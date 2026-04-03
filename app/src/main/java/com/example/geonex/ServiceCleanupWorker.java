package com.example.geonex;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.util.List;

public class ServiceCleanupWorker extends Worker {

    private static final String TAG = "ServiceCleanupWorker";

    public ServiceCleanupWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d(TAG, "🔄 Running service cleanup");

        try {
            Context context = getApplicationContext();
            ReminderRepository repository = ((GeonexApplication) context).getRepository();
            ServiceOptimizer optimizer = new ServiceOptimizer(context);
            LocationTrackingManager trackingManager = new LocationTrackingManager(context);
            GeofenceMonitorManager monitorManager = new GeofenceMonitorManager(context);

            // Get active reminders
            List<Reminder> activeReminders = repository.getActiveRecurringReminders();
            int activeCount = activeReminders != null ? activeReminders.size() : 0;

            Log.d(TAG, "Active reminders: " + activeCount);

            // Check if services should be running
            boolean shouldRun = activeCount > 0;

            if (shouldRun) {
                // Check if services are actually running
                boolean trackingRunning = optimizer.isServiceRunning(LocationTrackingService.class);
                boolean monitoringRunning = optimizer.isServiceRunning(GeofenceMonitoringService.class);

                Log.d(TAG, "Tracking service running: " + trackingRunning);
                Log.d(TAG, "Monitoring service running: " + monitoringRunning);

                // Restart if not running
                if (!trackingRunning) {
                    Log.d(TAG, "Restarting tracking service");
                    trackingManager.startTracking();
                }

                if (!monitoringRunning) {
                    Log.d(TAG, "Restarting monitoring service");
                    monitorManager.startMonitoring();
                }

                // Check memory
                if (!optimizer.hasEnoughMemory()) {
                    Log.w(TAG, "Low memory detected, optimizing services");
                    // Could reduce frequency or stop non-critical services
                }

                // Check battery state
                if (optimizer.isBatterySaverOn()) {
                    Log.d(TAG, "Battery saver active, using optimized settings");
                    // Will be handled by services
                }

            } else {
                // No active reminders, stop services if running
                Log.d(TAG, "No active reminders, stopping services");
                trackingManager.stopTracking();
                monitorManager.stopMonitoring();
            }

            // Clean up old geofences
            GeofenceHelper geofenceHelper = new GeofenceHelper(context);
            List<Integer> registeredIds = geofenceHelper.getAllGeofenceIds();

            for (Integer id : registeredIds) {
                boolean stillActive = false;
                if (activeReminders != null) {
                    for (Reminder r : activeReminders) {
                        if (r.getId() == id && !r.isCompleted()) {
                            stillActive = true;
                            break;
                        }
                    }
                }

                if (!stillActive) {
                    Log.d(TAG, "Removing orphaned geofence: " + id);
                    geofenceHelper.removeGeofenceById(id);
                }
            }

            Log.d(TAG, "✅ Service cleanup completed");
            return Result.success();

        } catch (Exception e) {
            Log.e(TAG, "Error during service cleanup: " + e.getMessage());
            return Result.failure();
        }
    }
}