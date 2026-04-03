package com.example.geonex;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RecoveryManager {

    private static final String TAG = "RecoveryManager";
    private static final String PREFS_NAME = "recovery_prefs";
    private static final String KEY_LAST_BACKUP = "last_backup_time";

    private final Context context;
    private final ExecutorService executorService;
    private final SharedPreferences prefs;
    private final Handler mainHandler;

    public RecoveryManager(Context context) {
        this.context = context;
        this.executorService = Executors.newSingleThreadExecutor();
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    /**
     * Attempt to recover from crash - restart services
     */
    public void recoverFromCrash() {
        Log.d(TAG, "🔄 Attempting to recover from crash");

        executorService.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    // Restart geofence monitoring
                    GeofenceHelper geofenceHelper = new GeofenceHelper(context);
                    geofenceHelper.reregisterAllGeofences();

                    // Restart tracking services
                    LocationTrackingManager trackingManager = new LocationTrackingManager(context);
                    GeofenceMonitorManager monitorManager = new GeofenceMonitorManager(context);

                    trackingManager.updateTrackingState();
                    monitorManager.updateMonitoringState();

                    Log.d(TAG, "✅ Recovery completed");

                } catch (Exception e) {
                    Log.e(TAG, "Recovery failed: " + e.getMessage());
                }
            }
        });
    }

    /**
     * Backup reminders to SharedPreferences (simple backup)
     */
    public void backupReminders() {
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    ReminderRepository repository = ((GeonexApplication) context).getRepository();
                    List<Reminder> allReminders = repository.getAllReminders().getValue();

                    if (allReminders != null && !allReminders.isEmpty()) {
                        // Simple backup - just save count and timestamp
                        SharedPreferences.Editor editor = prefs.edit();
                        editor.putLong(KEY_LAST_BACKUP, System.currentTimeMillis());
                        editor.putInt("reminder_count", allReminders.size());
                        editor.apply();

                        Log.d(TAG, "Backed up " + allReminders.size() + " reminders");
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Backup failed: " + e.getMessage());
                }
            }
        });
    }

    /**
     * Check if app was force stopped
     */
    public boolean wasForceStopped() {
        return prefs.getBoolean("force_stopped", false);
    }

    /**
     * Handle service death - restart if needed
     */
    public void handleServiceDeath(Class<?> serviceClass) {
        Log.d(TAG, "Service death detected: " + serviceClass.getSimpleName());

        // Check if should be running
        ReminderRepository repository = ((GeonexApplication) context).getRepository();
        List<Reminder> activeReminders = repository.getActiveRecurringReminders();

        if (activeReminders != null && !activeReminders.isEmpty()) {
            Log.d(TAG, "Restarting service: " + serviceClass.getSimpleName());

            if (serviceClass == LocationTrackingService.class) {
                new LocationTrackingManager(context).startTracking();
            } else if (serviceClass == GeofenceMonitoringService.class) {
                new GeofenceMonitorManager(context).startMonitoring();
            }
        }
    }

    /**
     * Schedule periodic recovery checks
     */
    public void scheduleRecoveryChecks() {
        mainHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                performHealthCheck();
                mainHandler.postDelayed(this, 30 * 60 * 1000); // Every 30 minutes
            }
        }, 5 * 60 * 1000); // Start after 5 minutes
    }

    /**
     * Perform health check on all services
     */
    private void performHealthCheck() {
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    Log.d(TAG, "🏥 Performing health check");

                    ServiceOptimizer optimizer = new ServiceOptimizer(context);
                    ReminderRepository repository = ((GeonexApplication) context).getRepository();
                    List<Reminder> activeReminders = repository.getActiveRecurringReminders();

                    if (activeReminders == null || activeReminders.isEmpty()) {
                        Log.d(TAG, "No active reminders, skipping health check");
                        return;
                    }

                    // Check tracking service
                    boolean trackingRunning = optimizer.isServiceRunning(LocationTrackingService.class);
                    if (!trackingRunning) {
                        Log.w(TAG, "Tracking service not running, restarting");
                        new LocationTrackingManager(context).startTracking();
                    }

                    // Check monitoring service
                    boolean monitoringRunning = optimizer.isServiceRunning(GeofenceMonitoringService.class);
                    if (!monitoringRunning) {
                        Log.w(TAG, "Monitoring service not running, restarting");
                        new GeofenceMonitorManager(context).startMonitoring();
                    }

                    // Check geofences
                    GeofenceHelper geofenceHelper = new GeofenceHelper(context);
                    List<Integer> registeredIds = geofenceHelper.getAllGeofenceIds();

                    if (registeredIds.size() < activeReminders.size()) {
                        Log.w(TAG, "Missing geofences, re-registering");
                        geofenceHelper.reregisterAllGeofences();
                    }

                    Log.d(TAG, "✅ Health check completed");

                } catch (Exception e) {
                    Log.e(TAG, "Health check failed: " + e.getMessage());
                }
            }
        });
    }

    /**
     * Clear all recovery data
     */
    public void clearRecoveryData() {
        prefs.edit().clear().apply();
        Log.d(TAG, "Recovery data cleared");
    }
}