package com.example.geonex;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.app.Person;
import androidx.core.graphics.drawable.IconCompat;

import java.util.Date;
import java.util.concurrent.TimeUnit;

public class NotificationHelper {

    private static final String TAG = "NotificationHelper";
    private final Context context;
    private final NotificationManager notificationManager;

    // Notification Channel IDs
    public static final String CHANNEL_GEOFENCE_ENTER = "geofence_enter_channel";
    public static final String CHANNEL_GEOFENCE_EXIT = "geofence_exit_channel";
    public static final String CHANNEL_GEOFENCE_DWELL = "geofence_dwell_channel";
    public static final String CHANNEL_REMINDER_COMPLETE = "reminder_complete_channel";
    public static final String CHANNEL_REMINDER_DUE = "reminder_due_channel";
    public static final String CHANNEL_BOOT = "boot_channel";
    public static final String CHANNEL_SERVICE = "service_channel";
    public static final String CHANNEL_URGENT = "urgent_channel";

    // Notification ID ranges
    private static final int BOOT_NOTIFICATION_ID = 9999;
    private static final int GEOFENCE_ENTER_BASE = 1000;
    private static final int GEOFENCE_EXIT_BASE = 2000;
    private static final int GEOFENCE_DWELL_BASE = 3000;
    private static final int REMINDER_COMPLETE_BASE = 4000;
    private static final int REMINDER_DUE_BASE = 5000;

    public NotificationHelper(Context context) {
        this.context = context;
        this.notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        createNotificationChannels();
    }

    /**
     * Create all notification channels (Android 8+)
     */
    private void createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            // Geofence Enter Channel (High importance)
            NotificationChannel enterChannel = new NotificationChannel(
                    CHANNEL_GEOFENCE_ENTER,
                    "Location Enter Alerts",
                    NotificationManager.IMPORTANCE_HIGH
            );
            enterChannel.setDescription("Notifications when you enter a reminder location");
            enterChannel.enableVibration(true);
            enterChannel.setVibrationPattern(new long[]{0, 500, 200, 500});
            enterChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            enterChannel.setBypassDnd(true);
            notificationManager.createNotificationChannel(enterChannel);

            // Geofence Exit Channel (Low importance)
            NotificationChannel exitChannel = new NotificationChannel(
                    CHANNEL_GEOFENCE_EXIT,
                    "Location Exit Alerts",
                    NotificationManager.IMPORTANCE_LOW
            );
            exitChannel.setDescription("Notifications when you leave a reminder location");
            exitChannel.enableVibration(false);
            exitChannel.setSound(null, null);
            exitChannel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
            notificationManager.createNotificationChannel(exitChannel);

            // Geofence Dwell Channel (Medium importance)
            NotificationChannel dwellChannel = new NotificationChannel(
                    CHANNEL_GEOFENCE_DWELL,
                    "Location Dwell Alerts",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            dwellChannel.setDescription("Reminders while you're at a location");
            dwellChannel.enableVibration(true);
            dwellChannel.setVibrationPattern(new long[]{0, 300});
            notificationManager.createNotificationChannel(dwellChannel);

            // Reminder Complete Channel (Low importance)
            NotificationChannel completeChannel = new NotificationChannel(
                    CHANNEL_REMINDER_COMPLETE,
                    "Completion Alerts",
                    NotificationManager.IMPORTANCE_LOW
            );
            completeChannel.setDescription("Notifications when reminders are completed");
            notificationManager.createNotificationChannel(completeChannel);

            // Reminder Due Channel (High importance)
            NotificationChannel dueChannel = new NotificationChannel(
                    CHANNEL_REMINDER_DUE,
                    "Due Reminders",
                    NotificationManager.IMPORTANCE_HIGH
            );
            dueChannel.setDescription("Notifications for upcoming reminders");
            dueChannel.enableVibration(true);
            dueChannel.setVibrationPattern(new long[]{0, 500, 200, 500});
            notificationManager.createNotificationChannel(dueChannel);

            // Boot Channel (Low importance)
            NotificationChannel bootChannel = new NotificationChannel(
                    CHANNEL_BOOT,
                    "System Events",
                    NotificationManager.IMPORTANCE_LOW
            );
            bootChannel.setDescription("Notifications about app status");
            notificationManager.createNotificationChannel(bootChannel);

            // Service Channel (Min importance)
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_SERVICE,
                    "Background Service",
                    NotificationManager.IMPORTANCE_MIN
            );
            serviceChannel.setDescription("Shows when app is running in background");
            serviceChannel.setSound(null, null);
            serviceChannel.enableVibration(false);
            notificationManager.createNotificationChannel(serviceChannel);

            // Urgent Channel (Critical importance)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                NotificationChannel urgentChannel = new NotificationChannel(
                        CHANNEL_URGENT,
                        "Urgent Alerts",
                        NotificationManager.IMPORTANCE_HIGH
                );
                urgentChannel.setDescription("Critical reminder notifications");
                urgentChannel.enableVibration(true);
                urgentChannel.setVibrationPattern(new long[]{0, 1000, 500, 1000});
                urgentChannel.setBypassDnd(true);
                urgentChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
                notificationManager.createNotificationChannel(urgentChannel);
            }

            Log.d(TAG, "Notification channels created");
        }
    }

    /**
     * Show geofence enter notification with action buttons
     */
    public void showGeofenceEnterNotification(Reminder reminder) {
        String title = reminder.getTitle();
        String message = "📍 You have arrived at " + reminder.getLocationName();

        Intent intent = new Intent(context, HomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.putExtra("reminder_id", reminder.getId());
        intent.putExtra("action", "geofence_enter");

        int notificationId = GEOFENCE_ENTER_BASE + reminder.getId();

        // Create notification with actions
        NotificationCompat.Builder builder = createBaseNotification(
                notificationId,
                title,
                message,
                intent,
                CHANNEL_GEOFENCE_ENTER,
                true
        );

        // Add action buttons
        addGeofenceActions(builder, reminder, notificationId);

        // Add styles
        builder.setStyle(new NotificationCompat.BigTextStyle()
                .bigText(message)
                .setBigContentTitle(title)
                .setSummaryText(reminder.getLocationName()));

        // Show notification
        notificationManager.notify(notificationId, builder.build());
        Log.d(TAG, "Enter notification sent for: " + reminder.getTitle());
    }

    /**
     * Show geofence exit notification
     */
    public void showGeofenceExitNotification(Reminder reminder) {
        String title = reminder.getTitle();
        String message = "👋 You have left " + reminder.getLocationName();

        Intent intent = new Intent(context, HomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        int notificationId = GEOFENCE_EXIT_BASE + reminder.getId();

        NotificationCompat.Builder builder = createBaseNotification(
                notificationId,
                title,
                message,
                intent,
                CHANNEL_GEOFENCE_EXIT,
                false
        );

        notificationManager.notify(notificationId, builder.build());
        Log.d(TAG, "Exit notification sent for: " + reminder.getTitle());
    }

    /**
     * Show geofence dwell notification (when user stays in area)
     */
    public void showGeofenceDwellNotification(Reminder reminder) {
        String title = "Still at " + reminder.getLocationName() + "?";
        String message = "Don't forget: " + reminder.getTitle();

        Intent intent = new Intent(context, HomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.putExtra("reminder_id", reminder.getId());

        int notificationId = GEOFENCE_DWELL_BASE + reminder.getId();

        NotificationCompat.Builder builder = createBaseNotification(
                notificationId,
                title,
                message,
                intent,
                CHANNEL_GEOFENCE_DWELL,
                true
        );

        // Add snooze button for dwell
        addSnoozeAction(builder, reminder, notificationId);

        notificationManager.notify(notificationId, builder.build());
        Log.d(TAG, "Dwell notification sent for: " + reminder.getTitle());
    }

    /**
     * Show reminder completed notification
     */
    public void showReminderCompletedNotification(Reminder reminder) {
        String title = "✅ Reminder Completed";
        String message = reminder.getTitle() + " - Well done!";

        Intent intent = new Intent(context, HomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        int notificationId = REMINDER_COMPLETE_BASE + reminder.getId();

        NotificationCompat.Builder builder = createBaseNotification(
                notificationId,
                title,
                message,
                intent,
                CHANNEL_REMINDER_COMPLETE,
                false
        );

        notificationManager.notify(notificationId, builder.build());
    }

    /**
     * Show reminder due soon notification (for upcoming reminders)
     */
    public void showReminderDueNotification(Reminder reminder, long minutesUntil) {
        String title = "⏰ Reminder Due Soon";
        String message = reminder.getTitle() + " at " + reminder.getLocationName();

        if (minutesUntil > 0) {
            message = message + " (in " + minutesUntil + " minutes)";
        }

        Intent intent = new Intent(context, HomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.putExtra("reminder_id", reminder.getId());

        int notificationId = REMINDER_DUE_BASE + reminder.getId();

        NotificationCompat.Builder builder = createBaseNotification(
                notificationId,
                title,
                message,
                intent,
                CHANNEL_REMINDER_DUE,
                true
        );

        // Add snooze button
        addSnoozeAction(builder, reminder, notificationId);

        notificationManager.notify(notificationId, builder.build());
    }

    /**
     * Show boot completed notification
     */
    public void showBootNotification() {
        String title = "🔄 Geonex Ready";
        String message = "Your reminders are active after reboot";

        Intent intent = new Intent(context, HomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        NotificationCompat.Builder builder = createBaseNotification(
                BOOT_NOTIFICATION_ID,
                title,
                message,
                intent,
                CHANNEL_BOOT,
                false
        );

        notificationManager.notify(BOOT_NOTIFICATION_ID, builder.build());
    }

    /**
     * Show service running notification (foreground service)
     */
    public Notification showServiceNotification() {
        return new NotificationCompat.Builder(context, CHANNEL_SERVICE)
                .setContentTitle("Geonex is active")
                .setContentText("Monitoring your reminder locations")
                .setSmallIcon(android.R.drawable.ic_dialog_map)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .build();
    }

    /**
     * Show urgent notification (critical reminders)
     */
    public void showUrgentNotification(Reminder reminder, String message) {
        String title = "🔴 URGENT: " + reminder.getTitle();

        Intent intent = new Intent(context, HomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.putExtra("reminder_id", reminder.getId());

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_URGENT)
                .setContentTitle(title)
                .setContentText(message)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setAutoCancel(true)
                .setContentIntent(PendingIntent.getActivity(
                        context,
                        reminder.getId(),
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                ))
                .setFullScreenIntent(PendingIntent.getActivity(
                        context,
                        reminder.getId() + 10000,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                ), true); // Heads-up notification

        notificationManager.notify(reminder.getId() + 10000, builder.build());
    }

    /**
     * Create base notification with common settings
     */
    private NotificationCompat.Builder createBaseNotification(
            int notificationId,
            String title,
            String message,
            Intent intent,
            String channelId,
            boolean highPriority) {

        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                notificationId,
                intent,
                flags
        );

        // Create delete intent
        Intent deleteIntent = new Intent(context, NotificationDismissedReceiver.class);
        deleteIntent.putExtra("notification_id", notificationId);

        PendingIntent deletePendingIntent = PendingIntent.getBroadcast(
                context,
                notificationId + 5000,
                deleteIntent,
                flags
        );

        Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
                .setContentTitle(title)
                .setContentText(message)
                .setSmallIcon(android.R.drawable.ic_dialog_map)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setDeleteIntent(deletePendingIntent)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setCategory(NotificationCompat.CATEGORY_REMINDER);

        if (highPriority) {
            builder.setPriority(NotificationCompat.PRIORITY_HIGH);
            builder.setSound(soundUri);
            builder.setVibrate(new long[]{0, 500, 200, 500});
        }

        return builder;
    }

    /**
     * Add geofence action buttons (complete, snooze)
     */
    private void addGeofenceActions(NotificationCompat.Builder builder, Reminder reminder, int notificationId) {
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }

        // Complete action
        Intent completeIntent = new Intent(context, NotificationActionReceiver.class);
        completeIntent.setAction("ACTION_COMPLETE");
        completeIntent.putExtra("reminder_id", reminder.getId());
        completeIntent.putExtra("notification_id", notificationId);

        PendingIntent completePendingIntent = PendingIntent.getBroadcast(
                context,
                notificationId + 10000,
                completeIntent,
                flags
        );

        builder.addAction(new NotificationCompat.Action.Builder(
                android.R.drawable.ic_menu_save,
                "✓ Complete",
                completePendingIntent
        ).build());

        // Snooze action
        Intent snoozeIntent = new Intent(context, NotificationActionReceiver.class);
        snoozeIntent.setAction("ACTION_SNOOZE");
        snoozeIntent.putExtra("reminder_id", reminder.getId());
        snoozeIntent.putExtra("notification_id", notificationId);

        PendingIntent snoozePendingIntent = PendingIntent.getBroadcast(
                context,
                notificationId + 20000,
                snoozeIntent,
                flags
        );

        builder.addAction(new NotificationCompat.Action.Builder(
                android.R.drawable.ic_menu_revert,
                "⏰ Snooze",
                snoozePendingIntent
        ).build());

        // Navigate action
        Intent navigateIntent = new Intent(context, HomeActivity.class);
        navigateIntent.putExtra("reminder_id", reminder.getId());
        navigateIntent.putExtra("action", "navigate");

        PendingIntent navigatePendingIntent = PendingIntent.getActivity(
                context,
                notificationId + 30000,
                navigateIntent,
                flags
        );

        builder.addAction(new NotificationCompat.Action.Builder(
                android.R.drawable.ic_dialog_map,
                "📍 View",
                navigatePendingIntent
        ).build());
    }

    /**
     * Add snooze action only
     */
    private void addSnoozeAction(NotificationCompat.Builder builder, Reminder reminder, int notificationId) {
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }

        Intent snoozeIntent = new Intent(context, NotificationActionReceiver.class);
        snoozeIntent.setAction("ACTION_SNOOZE");
        snoozeIntent.putExtra("reminder_id", reminder.getId());
        snoozeIntent.putExtra("notification_id", notificationId);

        PendingIntent snoozePendingIntent = PendingIntent.getBroadcast(
                context,
                notificationId + 20000,
                snoozeIntent,
                flags
        );

        builder.addAction(new NotificationCompat.Action.Builder(
                android.R.drawable.ic_menu_revert,
                "⏰ Remind me later",
                snoozePendingIntent
        ).build());
    }

    /**
     * Cancel a specific notification
     */
    public void cancelNotification(int notificationId) {
        notificationManager.cancel(notificationId);
        Log.d(TAG, "Notification cancelled: " + notificationId);
    }

    /**
     * Cancel all notifications for a reminder
     */
    public void cancelReminderNotifications(Reminder reminder) {
        cancelNotification(GEOFENCE_ENTER_BASE + reminder.getId());
        cancelNotification(GEOFENCE_EXIT_BASE + reminder.getId());
        cancelNotification(GEOFENCE_DWELL_BASE + reminder.getId());
        cancelNotification(REMINDER_COMPLETE_BASE + reminder.getId());
        cancelNotification(REMINDER_DUE_BASE + reminder.getId());
    }

    /**
     * Cancel all notifications
     */
    public void cancelAllNotifications() {
        notificationManager.cancelAll();
        Log.d(TAG, "All notifications cancelled");
    }

    /**
     * Check if notifications are enabled
     */
    public boolean areNotificationsEnabled() {
        NotificationManagerCompat manager = NotificationManagerCompat.from(context);
        return manager.areNotificationsEnabled();
    }

    public void showHeadsUpNotification(String s, String s1) {
    }
}