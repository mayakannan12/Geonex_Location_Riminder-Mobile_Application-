package com.example.geonex;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import java.util.List;

public class GeofenceMonitorManager {

    private static final String TAG = "GeofenceMonitorMgr";
    private final Context context;
    private final GeofenceHelper geofenceHelper;

    public GeofenceMonitorManager(Context context) {
        this.context = context;
        this.geofenceHelper = new GeofenceHelper(context);
    }

    /**
     * Start geofence monitoring service
     */
    public void startMonitoring() {
        Log.d(TAG, "Starting geofence monitoring service");

        Intent intent = new Intent(context, GeofenceMonitoringService.class);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent);
        } else {
            context.startService(intent);
        }
    }

    /**
     * Stop geofence monitoring service
     */
    public void stopMonitoring() {
        Log.d(TAG, "Stopping geofence monitoring service");

        Intent intent = new Intent(context, GeofenceMonitoringService.class);
        context.stopService(intent);
    }

    /**
     * Update monitoring state based on active reminders
     */
    public void updateMonitoringState() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    ReminderRepository repository = ((GeonexApplication) context.getApplicationContext()).getRepository();
                    List<Reminder> activeReminders = repository.getActiveRecurringReminders();

                    if (activeReminders != null && !activeReminders.isEmpty()) {
                        Log.d(TAG, "Active reminders found: " + activeReminders.size() + ", starting monitoring");
                        startMonitoring();
                    } else {
                        Log.d(TAG, "No active reminders, stopping monitoring");
                        stopMonitoring();
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error updating monitoring state: " + e.getMessage());
                }
            }
        }).start();
    }

    /**
     * Refresh geofences (call when reminders are added/removed)
     */
    public void refreshGeofences() {
        // Re-register all geofences
        geofenceHelper.reregisterAllGeofences();
    }

    /**
     * Get count of active geofences
     */
    public int getActiveGeofenceCount() {
        return geofenceHelper.getAllGeofenceIds().size();
    }
}