package com.example.geonex;

import android.content.Context;
import android.util.Log;


import androidx.annotation.NonNull;
import androidx.work.ListenableWorker;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.util.List;

public class GeofenceRefreshWorker extends Worker {

    private static final String TAG = "GeofenceRefreshWorker";

    public GeofenceRefreshWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d(TAG, "🔄 Running geofence refresh");

        try {
            Context context = getApplicationContext();
            ReminderRepository repository = ((GeonexApplication) context).getRepository();
            GeofenceHelper geofenceHelper = new GeofenceHelper(context);
            ServiceOptimizer optimizer = new ServiceOptimizer(context);

            // Get active reminders
            List<Reminder> activeReminders = repository.getActiveRecurringReminders();

            if (activeReminders == null || activeReminders.isEmpty()) {
                Log.d(TAG, "No active reminders, skipping refresh");
                return Result.success();
            }

            Log.d(TAG, "Refreshing geofences for " + activeReminders.size() + " reminders");

            // Get currently registered geofences
            List<Integer> registeredIds = geofenceHelper.getAllGeofenceIds();

            // Check if we need to refresh
            boolean needsRefresh = false;

            // If counts don't match, refresh
            if (registeredIds.size() != activeReminders.size()) {
                needsRefresh = true;
                Log.d(TAG, "Count mismatch: registered=" + registeredIds.size() +
                        ", active=" + activeReminders.size());
            } else {
                // Check if all active reminders are registered
                for (Reminder reminder : activeReminders) {
                    if (!registeredIds.contains(reminder.getId())) {
                        needsRefresh = true;
                        Log.d(TAG, "Missing geofence for reminder: " + reminder.getId());
                        break;
                    }
                }
            }

            if (needsRefresh) {
                Log.d(TAG, "Refreshing geofences...");

                // Remove all geofences
                geofenceHelper.removeAllGeofences();

                // Small delay
                Thread.sleep(1000);

                // Re-register with optimal radius multiplier
                float multiplier = optimizer.getOptimalRadiusMultiplier();
                if (multiplier != 1.0f) {
                    Log.d(TAG, "Using radius multiplier: " + multiplier);
                    // Note: Would need to adjust each reminder's radius
                }

                // Re-register all geofences
                geofenceHelper.addGeofences(activeReminders);

                Log.d(TAG, "Geofence refresh completed");
            } else {
                Log.d(TAG, "Geofences are up to date");
            }

            return ListenableWorker.Result.success();

        } catch (Exception e) {
            Log.e(TAG, "Error refreshing geofences: " + e.getMessage());
            return ListenableWorker.Result.failure();
        }
    }
}