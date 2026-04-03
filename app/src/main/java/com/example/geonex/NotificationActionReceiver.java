package com.example.geonex;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class NotificationActionReceiver extends BroadcastReceiver {

    private static final String TAG = "NotificationAction";
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    // Snooze durations in milliseconds
    private static final long SNOOZE_15_MIN = 15 * 60 * 1000;
    private static final long SNOOZE_30_MIN = 30 * 60 * 1000;
    private static final long SNOOZE_1_HOUR = 60 * 60 * 1000;
    private static final long SNOOZE_2_HOUR = 2 * 60 * 60 * 1000;
    private static final long SNOOZE_TILL_TOMORROW = 24 * 60 * 60 * 1000;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction() != null) {
            Log.d(TAG, "📬 Action received: " + intent.getAction());

            switch (intent.getAction()) {
                case "ACTION_COMPLETE":
                    handleCompleteAction(context, intent);
                    break;

                case "ACTION_SNOOZE":
                    handleSnoozeAction(context, intent);
                    break;

                case "ACTION_SNOOZE_15":
                    handleSnoozeWithDuration(context, intent, SNOOZE_15_MIN);
                    break;

                case "ACTION_SNOOZE_30":
                    handleSnoozeWithDuration(context, intent, SNOOZE_30_MIN);
                    break;

                case "ACTION_SNOOZE_1H":
                    handleSnoozeWithDuration(context, intent, SNOOZE_1_HOUR);
                    break;

                case "ACTION_SNOOZE_2H":
                    handleSnoozeWithDuration(context, intent, SNOOZE_2_HOUR);
                    break;

                case "ACTION_SNOOZE_TOMORROW":
                    handleSnoozeWithDuration(context, intent, SNOOZE_TILL_TOMORROW);
                    break;

                case "ACTION_DISMISS":
                    handleDismissAction(context, intent);
                    break;

                case "ACTION_NAVIGATE":
                    handleNavigateAction(context, intent);
                    break;

                case "ACTION_SHARE":
                    handleShareAction(context, intent);
                    break;
            }
        }
    }

    private void handleCompleteAction(Context context, Intent intent) {
        int reminderId = intent.getIntExtra("reminder_id", -1);
        int notificationId = intent.getIntExtra("notification_id", -1);

        Log.d(TAG, "✅ Complete action - Reminder: " + reminderId);

        if (reminderId != -1) {
            executorService.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        ReminderRepository repository = ((GeonexApplication) context.getApplicationContext()).getRepository();
                        Reminder reminder = repository.getReminderById(reminderId);

                        if (reminder != null) {
                            // Mark as completed
                            reminder.setCompleted(true);
                            repository.update(reminder);

                            // Cancel notification
                            NotificationHelper notificationHelper = new NotificationHelper(context);
                            notificationHelper.cancelNotification(notificationId);

                            // Show completion notification
                            notificationHelper.showReminderCompletedNotification(reminder);

                            // Remove geofence
                            GeofenceHelper geofenceHelper = new GeofenceHelper(context);
                            geofenceHelper.removeGeofence(reminder);

                            // Show toast on UI thread
                            showToast(context, "✓ Reminder completed!");

                            Log.d(TAG, "Reminder " + reminderId + " marked as completed");

                            // Handle recurring reminder
                            if (reminder.isRecurring()) {
                                handleRecurringReminder(context, reminder);
                            }

                            // Update services
                            updateServices(context);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error completing reminder: " + e.getMessage());
                    }
                }
            });
        }
    }

    private void handleSnoozeAction(Context context, Intent intent) {
        int reminderId = intent.getIntExtra("reminder_id", -1);
        int notificationId = intent.getIntExtra("notification_id", -1);

        Log.d(TAG, "⏰ Snooze action - Reminder: " + reminderId);

        // Cancel current notification
        NotificationHelper notificationHelper = new NotificationHelper(context);
        notificationHelper.cancelNotification(notificationId);

        // Show snooze options dialog
        showSnoozeOptionsDialog(context, reminderId, notificationId);
    }

    private void handleSnoozeWithDuration(Context context, Intent intent, long duration) {
        int reminderId = intent.getIntExtra("reminder_id", -1);

        Log.d(TAG, "⏰ Snooze for " + duration/60000 + " minutes - Reminder: " + reminderId);

        // Schedule reminder after snooze
        scheduleSnoozedReminder(context, reminderId, duration);

        // Show confirmation toast
        String durationText = getDurationText(duration);
        showToast(context, "⏰ Reminded in " + durationText);
    }

    private void handleDismissAction(Context context, Intent intent) {
        int reminderId = intent.getIntExtra("reminder_id", -1);
        int notificationId = intent.getIntExtra("notification_id", -1);

        Log.d(TAG, "❌ Dismiss action - Reminder: " + reminderId);

        // Just cancel the notification
        NotificationHelper notificationHelper = new NotificationHelper(context);
        notificationHelper.cancelNotification(notificationId);
    }

    private void handleNavigateAction(Context context, Intent intent) {
        int reminderId = intent.getIntExtra("reminder_id", -1);

        Log.d(TAG, "📍 Navigate action - Reminder: " + reminderId);

        // Open maps with the location
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    ReminderRepository repository = ((GeonexApplication) context.getApplicationContext()).getRepository();
                    Reminder reminder = repository.getReminderById(reminderId);

                    if (reminder != null) {
                        openMaps(context, reminder.getLatitude(), reminder.getLongitude(), reminder.getLocationName());
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error navigating: " + e.getMessage());
                }
            }
        });
    }

    private void handleShareAction(Context context, Intent intent) {
        int reminderId = intent.getIntExtra("reminder_id", -1);

        Log.d(TAG, "📤 Share action - Reminder: " + reminderId);

        executorService.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    ReminderRepository repository = ((GeonexApplication) context.getApplicationContext()).getRepository();
                    Reminder reminder = repository.getReminderById(reminderId);

                    if (reminder != null) {
                        shareReminder(context, reminder);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error sharing: " + e.getMessage());
                }
            }
        });
    }

    private void handleRecurringReminder(Context context, Reminder reminder) {
        String rule = reminder.getRecurrenceRule();
        long nextTriggerTime = System.currentTimeMillis();

        switch (rule) {
            case "daily":
                nextTriggerTime += TimeUnit.DAYS.toMillis(1);
                break;
            case "weekly":
                nextTriggerTime += TimeUnit.DAYS.toMillis(7);
                break;
            case "monthly":
                nextTriggerTime += TimeUnit.DAYS.toMillis(30);
                break;
            case "custom":
                int interval = reminder.getCustomInterval();
                String unit = reminder.getCustomIntervalUnit();

                if (unit.contains("Day")) {
                    nextTriggerTime += TimeUnit.DAYS.toMillis(interval);
                } else if (unit.contains("Week")) {
                    nextTriggerTime += TimeUnit.DAYS.toMillis(interval * 7);
                } else if (unit.contains("Month")) {
                    nextTriggerTime += TimeUnit.DAYS.toMillis(interval * 30);
                }
                break;
            default:
                return;
        }

        Log.d(TAG, "🔄 Next recurring at: " + new java.util.Date(nextTriggerTime));

        // Create a new reminder or reset the existing one
        reminder.setCompleted(false);
        reminder.setCreatedAt(nextTriggerTime);

        ReminderRepository repository = ((GeonexApplication) context.getApplicationContext()).getRepository();
        repository.update(reminder);

        // Re-register geofence
        GeofenceHelper geofenceHelper = new GeofenceHelper(context);
        geofenceHelper.addGeofence(reminder);
    }

    private void scheduleSnoozedReminder(Context context, int reminderId, long delay) {
        // This will be implemented with WorkManager in Step 3.5
        // For now, we'll use a simple handler
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                executorService.execute(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            ReminderRepository repository = ((GeonexApplication) context.getApplicationContext()).getRepository();
                            Reminder reminder = repository.getReminderById(reminderId);

                            if (reminder != null && !reminder.isCompleted()) {
                                NotificationHelper notificationHelper = new NotificationHelper(context);
                                notificationHelper.showGeofenceEnterNotification(reminder);
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error showing snoozed reminder: " + e.getMessage());
                        }
                    }
                });
            }
        }, delay);
    }

    private void showSnoozeOptionsDialog(Context context, int reminderId, int notificationId) {
        // Create snooze options dialog
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(context);
        builder.setTitle("Snooze for");

        String[] options = {"15 minutes", "30 minutes", "1 hour", "2 hours", "Tomorrow"};

        builder.setItems(options, (dialog, which) -> {
            long duration;
            switch (which) {
                case 0:
                    duration = SNOOZE_15_MIN;
                    break;
                case 1:
                    duration = SNOOZE_30_MIN;
                    break;
                case 2:
                    duration = SNOOZE_1_HOUR;
                    break;
                case 3:
                    duration = SNOOZE_2_HOUR;
                    break;
                case 4:
                    duration = SNOOZE_TILL_TOMORROW;
                    break;
                default:
                    duration = SNOOZE_30_MIN;
            }

            scheduleSnoozedReminder(context, reminderId, duration);
            showToast(context, "⏰ Reminded in " + options[which]);
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void openMaps(Context context, double lat, double lng, String locationName) {
        Uri gmmIntentUri = Uri.parse("geo:" + lat + "," + lng + "?q=" + lat + "," + lng + "(" + locationName + ")");
        Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
        mapIntent.setPackage("com.google.android.apps.maps");

        if (mapIntent.resolveActivity(context.getPackageManager()) != null) {
            mapIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(mapIntent);
        }
    }

    private void shareReminder(Context context, Reminder reminder) {
        @SuppressLint("SimpleDateFormat") String shareText = "📍 Reminder: " + reminder.getTitle() +
                "\n📍 Location: " + reminder.getLocationName() +
                "\n📏 Radius: " + reminder.getRadius() + "m" +
                "\n📅 Created: " + new java.text.SimpleDateFormat("dd MMM yyyy").format(new java.util.Date(reminder.getCreatedAt()));

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);
        shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        context.startActivity(Intent.createChooser(shareIntent, "Share Reminder"));
    }

    private void updateServices(Context context) {
        LocationTrackingManager trackingManager = new LocationTrackingManager(context);
        GeofenceMonitorManager monitorManager = new GeofenceMonitorManager(context);

        trackingManager.updateTrackingState();
        monitorManager.updateMonitoringState();
    }

    private void showToast(Context context, String message) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String getDurationText(long duration) {
        if (duration < TimeUnit.HOURS.toMillis(1)) {
            return (duration / 60000) + " minutes";
        } else if (duration < TimeUnit.DAYS.toMillis(1)) {
            return (duration / 3600000) + " hours";
        } else {
            return "tomorrow";
        }
    }
}