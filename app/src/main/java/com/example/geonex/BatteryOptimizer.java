package com.example.geonex;

import android.content.Context;
import android.content.Intent;
import android.os.BatteryManager;
import android.os.Build;
import android.os.PowerManager;
import android.util.Log;

public class BatteryOptimizer {

    private static final String TAG = "BatteryOptimizer";
    private final Context context;
    private final PowerManager powerManager;
    private final BatteryManager batteryManager;

    public BatteryOptimizer(Context context) {
        this.context = context;
        this.powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            this.batteryManager = (BatteryManager) context.getSystemService(Context.BATTERY_SERVICE);
        } else {
            this.batteryManager = null;
        }
    }

    /**
     * Get current battery level (percentage)
     */
    public int getBatteryLevel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && batteryManager != null) {
            return batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
        }

        // For older versions, use Intent
        Intent batteryIntent = context.registerReceiver(null,
                new android.content.IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        if (batteryIntent != null) {
            int level = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
            if (level >= 0 && scale > 0) {
                return (level * 100) / scale;
            }
        }

        return 50; // Default
    }

    /**
     * Check if device is charging
     */
    public boolean isCharging() {
        Intent batteryIntent = context.registerReceiver(null,
                new android.content.IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        if (batteryIntent != null) {
            int status = batteryIntent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
            return status == BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == BatteryManager.BATTERY_STATUS_FULL;
        }
        return false;
    }

    /**
     * Get charging method
     */
    public String getChargingMethod() {
        Intent batteryIntent = context.registerReceiver(null,
                new android.content.IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        if (batteryIntent != null) {
            int plugged = batteryIntent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
            if (plugged == BatteryManager.BATTERY_PLUGGED_AC) {
                return "AC";
            } else if (plugged == BatteryManager.BATTERY_PLUGGED_USB) {
                return "USB";
            } else if (plugged == BatteryManager.BATTERY_PLUGGED_WIRELESS) {
                return "Wireless";
            }
        }
        return "Not charging";
    }

    /**
     * Check if battery saver is on
     */
    public boolean isBatterySaverOn() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return powerManager.isPowerSaveMode();
        }
        return false;
    }

    /**
     * Get recommended location accuracy based on battery
     */
    public String getRecommendedAccuracy() {
        int batteryLevel = getBatteryLevel();

        if (batteryLevel < 15) {
            return "LOW"; // Critical battery
        } else if (batteryLevel < 30) {
            return "BALANCED"; // Low battery
        } else if (isCharging()) {
            return "HIGH"; // Charging - can use high accuracy
        } else {
            return "BALANCED"; // Normal
        }
    }

    /**
     * Get recommended update interval based on battery
     */
    public long getRecommendedInterval() {
        int batteryLevel = getBatteryLevel();

        if (batteryLevel < 15) {
            return 10 * 60 * 1000; // 10 minutes (critical)
        } else if (batteryLevel < 30) {
            return 5 * 60 * 1000; // 5 minutes (low)
        } else if (isCharging()) {
            return 60 * 1000; // 1 minute (charging)
        } else {
            return 2 * 60 * 1000; // 2 minutes (normal)
        }
    }

    /**
     * Check if device can handle high accuracy location
     */
    public boolean canUseHighAccuracy() {
        return isCharging() || getBatteryLevel() > 50;
    }

    /**
     * Log battery status
     */
    public void logBatteryStatus() {
        Log.d(TAG, "=== Battery Status ===");
        Log.d(TAG, "Level: " + getBatteryLevel() + "%");
        Log.d(TAG, "Charging: " + isCharging());
        Log.d(TAG, "Method: " + getChargingMethod());
        Log.d(TAG, "Battery Saver: " + isBatterySaverOn());
        Log.d(TAG, "Recommended Accuracy: " + getRecommendedAccuracy());
        Log.d(TAG, "Recommended Interval: " + getRecommendedInterval() + "ms");
    }
}