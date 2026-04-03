package com.example.geonex;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class NotificationDismissedReceiver extends BroadcastReceiver {

    private static final String TAG = "NotificationDismiss";

    @Override
    public void onReceive(Context context, Intent intent) {
        int notificationId = intent.getIntExtra("notification_id", -1);
        int reminderId = intent.getIntExtra("reminder_id", -1);

        Log.d(TAG, "Notification dismissed - ID: " + notificationId + ", Reminder: " + reminderId);

        // You can track notification dismissals here if needed
        // For example, update analytics or reschedule if needed

        if (reminderId != -1) {
            // Optional: You could log this for analytics
            // AnalyticsHelper.trackNotificationDismissed(reminderId);
        }
    }
}