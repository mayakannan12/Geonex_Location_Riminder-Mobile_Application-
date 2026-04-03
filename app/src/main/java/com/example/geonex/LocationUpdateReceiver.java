package com.example.geonex;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.util.Log;

public class LocationUpdateReceiver extends BroadcastReceiver {

    private static final String TAG = "LocationUpdateRcvr";
    private static final String ACTION_LOCATION_UPDATE = "com.example.geonex.LOCATION_UPDATE";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction() != null && intent.getAction().equals(ACTION_LOCATION_UPDATE)) {

            double latitude = intent.getDoubleExtra("latitude", 0);
            double longitude = intent.getDoubleExtra("longitude", 0);
            float accuracy = intent.getFloatExtra("accuracy", 0);
            long time = intent.getLongExtra("time", 0);

            Log.d(TAG, "📍 Location update received: " + latitude + ", " + longitude);

            // You can use this for debugging or additional processing
            // For example, update UI in HomeActivity if needed

            // Check proximity to reminders
            checkProximityToReminders(context, latitude, longitude);
        }
    }

    private void checkProximityToReminders(Context context, double lat, double lng) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    ReminderRepository repository = ((GeonexApplication) context.getApplicationContext()).getRepository();
                    java.util.List<Reminder> activeReminders = repository.getActiveRecurringReminders();

                    for (Reminder reminder : activeReminders) {
                        float[] results = new float[1];
                        android.location.Location.distanceBetween(
                                lat, lng,
                                reminder.getLatitude(), reminder.getLongitude(),
                                results
                        );

                        float distance = results[0];
                        float radius = reminder.getRadius();

                        // If within 150% of radius, log it
                        if (distance < radius * 1.5) {
                            Log.d(TAG, "⚠️ Near reminder: " + reminder.getTitle() +
                                    " - Distance: " + distance + "m, Radius: " + radius + "m");
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error checking proximity: " + e.getMessage());
                }
            }
        }).start();
    }
}