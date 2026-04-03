package com.example.geonex;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BootReceiver extends BroadcastReceiver {

    private static final String TAG = "BootReceiver";
    private static final String PREFS_NAME = "GeofencePrefs";
    private static final String KEY_GEOFENCE_IDS = "geofence_ids";
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction() != null) {
            String action = intent.getAction();
            Log.d(TAG, "📱 Received broadcast: " + action);

            // Check for boot completed
            if (Intent.ACTION_BOOT_COMPLETED.equals(action) ||
                    Intent.ACTION_LOCKED_BOOT_COMPLETED.equals(action) ||
                    "android.intent.action.QUICKBOOT_POWERON".equals(action)) {

                Log.d(TAG, "✅ Device reboot detected - Restoring app state...");

                // Re-register all geofences after reboot
                reregisterGeofences(context);

                // Start background services if needed
                startBackgroundServices(context);

                // Show boot notification
                showBootNotification(context);

                // Schedule periodic checks
                schedulePeriodicChecks(context);
            }
        }
    }

    private void reregisterGeofences(Context context) {
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    Log.d(TAG, "Starting geofence re-registration on background thread");

                    // Get application instance
                    GeonexApplication application = (GeonexApplication) context.getApplicationContext();

                    // Get repository
                    ReminderRepository repository = application.getRepository();

                    // Get all active reminders from database
                    List<Reminder> allReminders = repository.getActiveRecurringReminders();
                    List<Reminder> activeReminders = new ArrayList<>();

                    // Filter out completed reminders
                    for (Reminder reminder : allReminders) {
                        if (!reminder.isCompleted()) {
                            activeReminders.add(reminder);
                        }
                    }

                    if (!activeReminders.isEmpty()) {
                        Log.d(TAG, "✅ Re-registering " + activeReminders.size() + " geofences after reboot");

                        // Re-register geofences
                        GeofenceHelper geofenceHelper = new GeofenceHelper(context);
                        geofenceHelper.addGeofences(activeReminders);

                        // Save geofence IDs to SharedPreferences
                        saveGeofenceIds(context, activeReminders);
                    } else {
                        Log.d(TAG, "No active reminders found to re-register");
                    }

                } catch (Exception e) {
                    Log.e(TAG, "Error re-registering geofences: " + e.getMessage());
                }
            }
        });
    }

    private void saveGeofenceIds(Context context, List<Reminder> reminders) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        StringBuilder idsBuilder = new StringBuilder();
        for (int i = 0; i < reminders.size(); i++) {
            if (i > 0) {
                idsBuilder.append(",");
            }
            idsBuilder.append(reminders.get(i).getId());
        }

        editor.putString(KEY_GEOFENCE_IDS, idsBuilder.toString());
        editor.apply();

        Log.d(TAG, "Saved " + reminders.size() + " geofence IDs to preferences");
    }

    private void startBackgroundServices(Context context) {
        Log.d(TAG, "Starting background services after reboot");

        // Start location tracking if needed
        LocationTrackingManager trackingManager = new LocationTrackingManager(context);
        trackingManager.updateTrackingState();

        // Start geofence monitoring if needed
        GeofenceMonitorManager monitorManager = new GeofenceMonitorManager(context);
        monitorManager.updateMonitoringState();
    }

    private void showBootNotification(Context context) {
        // Run on main thread for notification
        android.os.Handler mainHandler = new android.os.Handler(context.getMainLooper());
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    NotificationHelper notificationHelper = new NotificationHelper(context);
                    notificationHelper.showBootNotification();
                    Log.d(TAG, "Boot notification shown");
                } catch (Exception e) {
                    Log.e(TAG, "Error showing boot notification: " + e.getMessage());
                }
            }
        });
    }

    private void schedulePeriodicChecks(Context context) {
        Log.d(TAG, "Scheduling periodic checks");

        // This will be implemented with WorkManager in Step 3.5
        // For now, we'll just log
    }

    /**
     * Check if any geofences were active before reboot
     */
    private boolean hasActiveGeofences(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String idsString = prefs.getString(KEY_GEOFENCE_IDS, "");
        return !idsString.isEmpty();
    }
}