package com.example.geonex;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GeofenceMonitoringService extends Service {

    private static final String TAG = "GeofenceMonitorSvc";
    private static final int NOTIFICATION_ID = 1002;
    private static final String CHANNEL_ID = "geofence_monitor_channel";

    private GeofencingClient geofencingClient;
    private ExecutorService executorService;
    private ReminderRepository repository;
    private GeofenceHelper geofenceHelper;
    private ServiceOptimizer optimizer;
    private boolean isMonitoring = false;
    private int refreshCount = 0;
    private static final int MAX_GEOFENCES_BEFORE_OPTIMIZE = 20;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Geofence monitoring service created");

        executorService = Executors.newSingleThreadExecutor();
        repository = ((GeonexApplication) getApplication()).getRepository();
        geofencingClient = LocationServices.getGeofencingClient(this);
        geofenceHelper = new GeofenceHelper(this);
        optimizer = new ServiceOptimizer(this);

        // Start as foreground service
        startForeground(NOTIFICATION_ID, createNotification());

        // Start monitoring
        startMonitoring();
    }

    private Notification createNotification() {
        // Create notification channel for Android O and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Geofence Monitor",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Geonex is monitoring your saved locations");

            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }

        // Create notification
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Geonex is monitoring")
                .setContentText("Watching for your reminder locations")
                .setSmallIcon(android.R.drawable.ic_dialog_map)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .build();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Geofence monitoring service started");

        if (!hasLocationPermission()) {
            Log.e(TAG, "No location permission, stopping service");
            stopSelf();
            return START_NOT_STICKY;
        }

        return START_STICKY;
    }

    private boolean hasLocationPermission() {
        return ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void startMonitoring() {
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    Log.d(TAG, "Starting geofence monitoring on background thread");

                    // Get all active reminders
                    List<Reminder> activeReminders = repository.getActiveRecurringReminders();

                    if (activeReminders == null || activeReminders.isEmpty()) {
                        Log.d(TAG, "No active reminders, stopping service");
                        stopSelf();
                        return;
                    }

                    Log.d(TAG, "Found " + activeReminders.size() + " active reminders");

                    // Register all geofences
                    registerAllGeofences(activeReminders);

                    // Periodically check and refresh geofences (every 30 minutes)
                    scheduleGeofenceRefresh();

                    isMonitoring = true;

                } catch (Exception e) {
                    Log.e(TAG, "Error starting monitoring: " + e.getMessage());
                }
            }
        });
    }

    /**
     * Optimize geofences based on device state
     */
    private void optimizeGeofences(List<Reminder> reminders) {
        if (reminders.size() > MAX_GEOFENCES_BEFORE_OPTIMIZE) {
            Log.d(TAG, "Many geofences (" + reminders.size() + "), applying optimizations");

            float radiusMultiplier = optimizer.getOptimalRadiusMultiplier();
            if (radiusMultiplier != 1.0f) {
                Log.d(TAG, "Adjusting geofence radii by multiplier: " + radiusMultiplier);

                // Note: We can't directly modify geofence radius, but we could
                // recreate them with adjusted values if needed
            }
        }
    }

    /**
     * Register all geofences with optimizations
     */
    private void registerAllGeofences(List<Reminder> reminders) {
        List<Geofence> geofenceList = new ArrayList<>();
        int completedCount = 0;

        for (Reminder reminder : reminders) {
            if (!reminder.isCompleted()) {
                Geofence geofence = geofenceHelper.createGeofence(reminder);
                geofenceList.add(geofence);
            } else {
                completedCount++;
            }
        }

        Log.d(TAG, "Found " + geofenceList.size() + " active geofences, " +
                completedCount + " completed");

        if (geofenceList.isEmpty()) {
            Log.d(TAG, "No geofences to register, stopping service");
            stopSelf();
            return;
        }

        // Apply optimizations for many geofences
        optimizeGeofences(reminders);

        // Check if we're exceeding the geofence limit (100 per app)
        if (geofenceList.size() > 100) {
            Log.w(TAG, "Too many geofences (" + geofenceList.size() + "), prioritizing");
            // Could prioritize by proximity or importance
        }

        GeofencingRequest request = geofenceHelper.createGeofencingRequest(geofenceList);

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        geofencingClient.addGeofences(request, geofenceHelper.getPublicGeofencePendingIntent())
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        refreshCount++;
                        Log.d(TAG, "Successfully registered " + geofenceList.size() +
                                " geofences (refresh #" + refreshCount + ")");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "Failed to register geofences: " + e.getMessage());

                        // If too many geofences error, try to register in batches
                        if (e.getMessage() != null && e.getMessage().contains("TOO_MANY_GEOFENCES")) {
                            registerGeofencesInBatches(geofenceList);
                        }
                    }
                });
    }

    /**
     * Register geofences in batches to avoid limit
     */
    private void registerGeofencesInBatches(List<Geofence> geofences) {
        Log.d(TAG, "Registering geofences in batches of 50");

        int batchSize = 50;
        for (int i = 0; i < geofences.size(); i += batchSize) {
            int end = Math.min(i + batchSize, geofences.size());
            List<Geofence> batch = geofences.subList(i, end);

            GeofencingRequest request = geofenceHelper.createGeofencingRequest(new ArrayList<>(batch));

            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            int finalI = i;
            geofencingClient.addGeofences(request, geofenceHelper.getPublicGeofencePendingIntent())
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            Log.d(TAG, "Registered batch " + (finalI / batchSize + 1) +
                                    " with " + batch.size() + " geofences");
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.e(TAG, "Failed to register batch: " + e.getMessage());
                        }
                    });

            // Small delay between batches
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private void scheduleGeofenceRefresh() {
        // This will be implemented with WorkManager in Step 3.5
        // For now, we'll just log
        Log.d(TAG, "Geofence refresh scheduled");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Geofence monitoring service destroyed");

        if (executorService != null) {
            executorService.shutdown();
        }

        // Remove geofences? No, they persist even without service
        isMonitoring = false;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /**
     * Check if service should be running
     */
    public static boolean shouldBeRunning(Context context) {
        ReminderRepository repo = ((GeonexApplication) context.getApplicationContext()).getRepository();
        List<Reminder> activeReminders = repo.getActiveRecurringReminders();
        return activeReminders != null && !activeReminders.isEmpty();
    }

    /**
     * Refresh geofences (call when reminders change)
     */
    public void refreshGeofences() {
        if (!isMonitoring) {
            return;
        }

        executorService.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    Log.d(TAG, "Refreshing geofences");

                    // Remove all existing geofences
                    geofencingClient.removeGeofences(geofenceHelper.getPublicGeofencePendingIntent())
                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {
                                    Log.d(TAG, "Removed old geofences");

                                    // Re-register with current reminders
                                    List<Reminder> activeReminders = repository.getActiveRecurringReminders();
                                    if (activeReminders != null && !activeReminders.isEmpty()) {
                                        registerAllGeofences(activeReminders);
                                    }
                                }
                            })
                            .addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    Log.e(TAG, "Failed to remove geofences: " + e.getMessage());
                                }
                            });

                } catch (Exception e) {
                    Log.e(TAG, "Error refreshing geofences: " + e.getMessage());
                }
            }
        });
    }
}