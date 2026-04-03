package com.example.geonex;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LocationTrackingManager {

    private static final String TAG = "LocationTrackingMgr";
    private final Context context;
    private final ExecutorService executorService;

    public LocationTrackingManager(Context context) {
        this.context = context;
        this.executorService = Executors.newSingleThreadExecutor();
    }

    /**
     * Start location tracking service
     */
    public void startTracking() {
        Log.d(TAG, "Starting location tracking service");

        Intent intent = new Intent(context, LocationTrackingService.class);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent);
        } else {
            context.startService(intent);
        }
    }

    /**
     * Stop location tracking service
     */
    public void stopTracking() {
        Log.d(TAG, "Stopping location tracking service");

        Intent intent = new Intent(context, LocationTrackingService.class);
        context.stopService(intent);
    }

    /**
     * Check if tracking should be running based on active reminders
     */
    public void updateTrackingState() {
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    ReminderRepository repository = ((GeonexApplication) context.getApplicationContext()).getRepository();
                    List<Reminder> activeReminders = repository.getActiveRecurringReminders();
                    int activeCount = activeReminders != null ? activeReminders.size() : 0;

                    if (activeCount > 0) {
                        Log.d(TAG, "Active reminders found: " + activeCount + ", starting tracking");
                        startTracking();
                    } else {
                        Log.d(TAG, "No active reminders, stopping tracking");
                        stopTracking();
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error updating tracking state: " + e.getMessage());
                }
            }
        });
    }
}