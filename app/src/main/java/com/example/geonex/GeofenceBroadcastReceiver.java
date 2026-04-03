package com.example.geonex;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofenceStatusCodes;
import com.google.android.gms.location.GeofencingEvent;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GeofenceBroadcastReceiver extends BroadcastReceiver {

    private static final String TAG = "GeofenceReceiver";
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "📡 Geofence broadcast received");

        GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);

        if (geofencingEvent == null) {
            Log.e(TAG, "Geofencing event is null");
            return;
        }

        // Check for errors
        if (geofencingEvent.hasError()) {
            String errorMessage = getErrorString(geofencingEvent.getErrorCode());
            Log.e(TAG, "Geofence error: " + errorMessage);

            // If too many geofences error, try to re-register
            if (geofencingEvent.getErrorCode() == GeofenceStatusCodes.GEOFENCE_TOO_MANY_GEOFENCES) {
                handleTooManyGeofencesError(context);
            }
            return;
        }

        // Get transition type
        int transitionType = geofencingEvent.getGeofenceTransition();

        // Get triggered geofences
        List<Geofence> triggeredGeofences = geofencingEvent.getTriggeringGeofences();

        if (triggeredGeofences == null || triggeredGeofences.isEmpty()) {
            Log.e(TAG, "No triggered geofences");
            return;
        }

        // Process each triggered geofence on background thread
        for (Geofence geofence : triggeredGeofences) {
            String geofenceId = geofence.getRequestId();
            Log.d(TAG, "🎯 Geofence triggered: " + geofenceId + ", transition: " + getTransitionTypeString(transitionType));

            // Process in background
            processGeofenceEvent(context, geofenceId, transitionType);
        }

        // Ensure services are running after geofence trigger
        ensureServicesRunning(context);
    }

    private void processGeofenceEvent(Context context, String geofenceId, int transitionType) {
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    Log.d(TAG, "Processing geofence on background thread: " + geofenceId);

                    // Get repository
                    ReminderRepository repository = ((GeonexApplication) context.getApplicationContext()).getRepository();

                    try {
                        int reminderId = Integer.parseInt(geofenceId);
                        Reminder reminder = repository.getReminderById(reminderId);

                        if (reminder != null) {
                            // Check if reminder is completed
                            if (reminder.isCompleted()) {
                                Log.d(TAG, "Reminder already completed: " + reminderId);

                                // If it's recurring, reset it
                                if (reminder.isRecurring()) {
                                    handleRecurringReminder(context, reminder);
                                }
                                return;
                            }

                            switch (transitionType) {
                                case Geofence.GEOFENCE_TRANSITION_ENTER:
                                    Log.d(TAG, "🚶 Entered geofence: " + reminder.getTitle());
                                    handleEnterEvent(context, reminder);
                                    break;

                                case Geofence.GEOFENCE_TRANSITION_EXIT:
                                    Log.d(TAG, "🚪 Exited geofence: " + reminder.getTitle());
                                    handleExitEvent(context, reminder);
                                    break;

                                case Geofence.GEOFENCE_TRANSITION_DWELL:
                                    Log.d(TAG, "⏱️ Dwelling in geofence: " + reminder.getTitle());
                                    handleDwellEvent(context, reminder);
                                    break;
                            }
                        } else {
                            Log.e(TAG, "Reminder not found for id: " + geofenceId);
                        }
                    } catch (NumberFormatException e) {
                        Log.e(TAG, "Invalid geofence ID format: " + geofenceId);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error processing geofence: " + e.getMessage());
                }
            }
        });
    }

    private void handleEnterEvent(Context context, Reminder reminder) {
        // Show notification on UI thread
        showNotificationOnUiThread(context, reminder, "enter");

        // Log the event for analytics
        Log.d(TAG, "✅ Enter event handled for: " + reminder.getTitle());

        // If it's a one-time reminder, optionally mark as completed
        if (!reminder.isRecurring()) {
            // You can choose to auto-complete or leave as is
            // For now, we'll just notify
        }
    }

    private void handleExitEvent(Context context, Reminder reminder) {
        // Optionally show exit notification
        if (shouldShowExitNotifications()) {
            showNotificationOnUiThread(context, reminder, "exit");
        }

        Log.d(TAG, "👋 Exit event handled for: " + reminder.getTitle());
    }

    private void handleDwellEvent(Context context, Reminder reminder) {
        // Dwell means user stayed in area for some time
        // Could be used for "Don't forget to buy milk while you're here"

        Log.d(TAG, "⏲️ Dwell event for: " + reminder.getTitle());

        // Show a different notification for dwell
        showNotificationOnUiThread(context, reminder, "dwell");
    }

    private void handleRecurringReminder(Context context, Reminder reminder) {
        // Reset recurring reminder for next cycle
        reminder.setCompleted(false);

        ReminderRepository repository = ((GeonexApplication) context.getApplicationContext()).getRepository();
        repository.update(reminder);

        // Re-register geofence
        GeofenceHelper geofenceHelper = new GeofenceHelper(context);
        geofenceHelper.addGeofence(reminder);

        Log.d(TAG, "🔄 Recurring reminder reset: " + reminder.getTitle());
    }

    private void handleTooManyGeofencesError(Context context) {
        Log.w(TAG, "⚠️ Too many geofences error, attempting to re-register");

        executorService.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    // Remove all geofences and re-register
                    GeofenceHelper geofenceHelper = new GeofenceHelper(context);
                    geofenceHelper.removeAllGeofences();

                    // Small delay before re-registering
                    Thread.sleep(2000);

                    // Re-register all active geofences
                    geofenceHelper.reregisterAllGeofences();

                } catch (Exception e) {
                    Log.e(TAG, "Error handling too many geofences: " + e.getMessage());
                }
            }
        });
    }

    private void showNotificationOnUiThread(Context context, Reminder reminder, String type) {
        android.os.Handler mainHandler = new android.os.Handler(context.getMainLooper());
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    NotificationHelper notificationHelper = new NotificationHelper(context);

                    switch (type) {
                        case "enter":
                            notificationHelper.showGeofenceEnterNotification(reminder);
                            break;
                        case "exit":
                            notificationHelper.showGeofenceExitNotification(reminder);
                            break;
                        case "dwell":
                            notificationHelper.showHeadsUpNotification(
                                    "Don't forget!",
                                    reminder.getTitle() + " at " + reminder.getLocationName()
                            );
                            break;
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error showing notification: " + e.getMessage());
                }
            }
        });
    }

    private void ensureServicesRunning(Context context) {
        // Make sure our background services are running after geofence trigger
        LocationTrackingManager trackingManager = new LocationTrackingManager(context);
        GeofenceMonitorManager monitorManager = new GeofenceMonitorManager(context);

        trackingManager.updateTrackingState();
        monitorManager.updateMonitoringState();
    }

    private boolean shouldShowExitNotifications() {
        // This could be a user preference
        // For now, return false to avoid spam
        return false;
    }

    private String getTransitionTypeString(int transitionType) {
        switch (transitionType) {
            case Geofence.GEOFENCE_TRANSITION_ENTER:
                return "ENTER";
            case Geofence.GEOFENCE_TRANSITION_EXIT:
                return "EXIT";
            case Geofence.GEOFENCE_TRANSITION_DWELL:
                return "DWELL";
            default:
                return "UNKNOWN";
        }
    }

    private String getErrorString(int errorCode) {
        switch (errorCode) {
            case GeofenceStatusCodes.GEOFENCE_NOT_AVAILABLE:
                return "Geofence not available";
            case GeofenceStatusCodes.GEOFENCE_TOO_MANY_GEOFENCES:
                return "Too many geofences";
            case GeofenceStatusCodes.GEOFENCE_TOO_MANY_PENDING_INTENTS:
                return "Too many pending intents";
            default:
                return "Unknown error: " + errorCode;
        }
    }
}