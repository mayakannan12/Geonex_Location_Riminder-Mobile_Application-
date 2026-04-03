package com.example.geonex;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class ServiceStateReceiver extends BroadcastReceiver {

    private static final String TAG = "ServiceStateRcvr";
    private static final String ACTION_CHECK_SERVICES = "com.example.geonex.CHECK_SERVICES";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction() != null) {
            switch (intent.getAction()) {
                case ACTION_CHECK_SERVICES:
                    checkServices(context);
                    break;

                case Intent.ACTION_SCREEN_ON:
                    Log.d(TAG, "Screen turned on");
                    // Optional: Increase location frequency when screen is on
                    break;

                case Intent.ACTION_SCREEN_OFF:
                    Log.d(TAG, "Screen turned off");
                    // Optional: Decrease location frequency when screen is off
                    break;

                case Intent.ACTION_POWER_CONNECTED:
                    Log.d(TAG, "Power connected");
                    // Optional: Use higher accuracy when charging
                    break;

                case Intent.ACTION_POWER_DISCONNECTED:
                    Log.d(TAG, "Power disconnected");
                    // Optional: Use lower accuracy on battery
                    break;
            }
        }
    }

    private void checkServices(Context context) {
        Log.d(TAG, "Checking service states");

        // Update both services
        LocationTrackingManager trackingManager = new LocationTrackingManager(context);
        GeofenceMonitorManager monitorManager = new GeofenceMonitorManager(context);

        trackingManager.updateTrackingState();
        monitorManager.updateMonitoringState();
    }
}