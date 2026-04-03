package com.example.geonex;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LocationTrackingService extends Service {

    private static final String TAG = "LocationTrackingSvc";
    private static final int NOTIFICATION_ID = 1001;
    private static final String CHANNEL_ID = "location_tracking_channel";

    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private LocationRequest locationRequest;
    private boolean isTracking = false;
    private ExecutorService executorService;
    private ReminderRepository repository;
    private ServiceOptimizer optimizer;

    // Tracking intervals (in milliseconds)
    private static final long UPDATE_INTERVAL = 2 * 60 * 1000; // 2 minutes normal
    private static final long FASTEST_INTERVAL = 1 * 60 * 1000; // 1 minute when near
    private static final long SMALLEST_DISPLACEMENT = 100; // 100 meters

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Location tracking service created");

        executorService = Executors.newSingleThreadExecutor();
        repository = ((GeonexApplication) getApplication()).getRepository();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        optimizer = new ServiceOptimizer(this);

        createLocationRequest();
        createLocationCallback();

        // Start as foreground service
        startForeground(NOTIFICATION_ID, createNotification());
    }

    private void createLocationRequest() {
        long interval = optimizer != null ? optimizer.getOptimalLocationInterval() : UPDATE_INTERVAL;
        int priority = optimizer != null ? optimizer.getRecommendedLocationPriority()
                : Priority.PRIORITY_BALANCED_POWER_ACCURACY;

        Log.d(TAG, "Creating location request with interval: " + interval + "ms, priority: " + priority);

        locationRequest = new LocationRequest.Builder(priority)
                .setIntervalMillis(interval)
                .setMinUpdateIntervalMillis(FASTEST_INTERVAL)
                .setMinUpdateDistanceMeters(SMALLEST_DISPLACEMENT)
                .setMaxUpdateAgeMillis(10 * 60 * 1000) // 10 minutes
                .build();
    }

    private void createLocationCallback() {
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }

                Location location = locationResult.getLastLocation();
                if (location != null) {
                    Log.d(TAG, "Location update: " + location.getLatitude() +
                            ", " + location.getLongitude());

                    // Process location in background
                    processLocation(location);
                }
            }
        };
    }

    private Notification createNotification() {
        // Create notification channel for Android O and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Location Tracking",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Geonex is tracking your location for reminders");

            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }

        // Create notification
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Geonex is running")
                .setContentText("Tracking location for your reminders")
                .setSmallIcon(android.R.drawable.ic_dialog_map)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .build();
    }

    private void processLocation(Location location) {
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    // Check if there are any active reminders - THIS IS NOW ON BACKGROUND THREAD
                    List<Reminder> activeReminders = repository.getActiveRecurringReminders();

                    if (activeReminders == null || activeReminders.isEmpty()) {
                        Log.d(TAG, "No active reminders, stopping service");
                        stopSelf();
                        return;
                    }

                    Log.d(TAG, "Processing location for " + activeReminders.size() + " reminders");

                } catch (Exception e) {
                    Log.e(TAG, "Error processing location: " + e.getMessage());
                }
            }
        });
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Location tracking service started");

        if (!hasLocationPermission()) {
            Log.e(TAG, "No location permission, stopping service");
            stopSelf();
            return START_NOT_STICKY;
        }

        // Check if service should run - DO THIS ON BACKGROUND THREAD
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                if (!shouldServiceRun()) {
                    Log.d(TAG, "No active reminders, stopping service");
                    stopSelf();
                    return;
                }

                // Start location updates on main thread (Location API requirement)
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        startLocationUpdates();
                    }
                });
            }
        });

        return START_STICKY;
    }

    private void runOnUiThread(Runnable runnable) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            runnable.run();
        } else {
            new android.os.Handler(Looper.getMainLooper()).post(runnable);
        }
    }

    private boolean hasLocationPermission() {
        return ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    @android.annotation.SuppressLint("MissingPermission")
    private void startLocationUpdates() {
        if (!hasLocationPermission()) {
            return;
        }

        try {
            fusedLocationClient.requestLocationUpdates(
                    locationRequest,
                    locationCallback,
                    Looper.getMainLooper() // Callback on main thread, we switch to background in processLocation
            );
            isTracking = true;
            Log.d(TAG, "Location updates started");
        } catch (SecurityException e) {
            Log.e(TAG, "Security exception: " + e.getMessage());
        }
    }

    private void stopLocationUpdates() {
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
            isTracking = false;
            Log.d(TAG, "Location updates stopped");
        }
    }

    /**
     * Check if service should be running - MUST BE CALLED FROM BACKGROUND THREAD
     */
    private boolean shouldServiceRun() {
        try {
            List<Reminder> activeReminders = repository.getActiveRecurringReminders();
            return activeReminders != null && !activeReminders.isEmpty();
        } catch (Exception e) {
            Log.e(TAG, "Error checking if service should run: " + e.getMessage());
            return false;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Location tracking service destroyed");
        stopLocationUpdates();

        if (executorService != null) {
            executorService.shutdown();
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}