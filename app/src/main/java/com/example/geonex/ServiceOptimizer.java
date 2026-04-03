package com.example.geonex;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.LocationManager;
import android.os.BatteryManager;
import android.os.Build;
import android.os.PowerManager;
import android.util.Log;

import java.util.List;

public class ServiceOptimizer {

    private static final String TAG = "ServiceOptimizer";
    private final Context context;
    private final PowerManager powerManager;
    private final LocationManager locationManager;
    private final ActivityManager activityManager;

    // Optimization thresholds
    private static final long BATTERY_SAVER_THRESHOLD = 15; // 15% battery
    private static final long LOCATION_UPDATE_INTERVAL_NORMAL = 2 * 60 * 1000; // 2 minutes
    private static final long LOCATION_UPDATE_INTERVAL_BATTERY_SAVER = 5 * 60 * 1000; // 5 minutes
    private static final long LOCATION_UPDATE_INTERVAL_CHARGING = 1 * 60 * 1000; // 1 minute
    private static final long SERVICE_CLEANUP_INTERVAL = 30 * 60 * 1000; // 30 minutes

    public ServiceOptimizer(Context context) {
        this.context = context;
        this.powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        this.locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        this.activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
    }

    /**
     * Get optimal location update interval based on device state
     */
    public long getOptimalLocationInterval() {
        // If device is charging, use faster updates
        if (isDeviceCharging()) {
            Log.d(TAG, "Device charging - using fast updates");
            return LOCATION_UPDATE_INTERVAL_CHARGING;
        }

        // If battery saver is enabled or battery low, use slower updates
        if (isBatterySaverOn() || isBatteryLow()) {
            Log.d(TAG, "Battery saver mode - using slow updates");
            return LOCATION_UPDATE_INTERVAL_BATTERY_SAVER;
        }

        // Normal mode
        return LOCATION_UPDATE_INTERVAL_NORMAL;
    }

    /**
     * Check if device is charging
     */
    public boolean isDeviceCharging() {
        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = context.registerReceiver(null, ifilter);

        if (batteryStatus != null) {
            int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
            return status == BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == BatteryManager.BATTERY_STATUS_FULL;
        }
        return false;
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
     * Check if battery is low (simplified)
     */
    public boolean isBatteryLow() {
        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = context.registerReceiver(null, ifilter);

        if (batteryStatus != null) {
            int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
            float batteryPct = level * 100 / (float) scale;

            return batteryPct < BATTERY_SAVER_THRESHOLD;
        }
        return false;
    }

    /**
     * Check if GPS is enabled
     */
    public boolean isGpsEnabled() {
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }

    /**
     * Check if network location is enabled
     */
    public boolean isNetworkLocationEnabled() {
        return locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }

    /**
     * Get best available location provider
     */
    public String getBestLocationProvider() {
        if (isGpsEnabled()) {
            return LocationManager.GPS_PROVIDER;
        } else if (isNetworkLocationEnabled()) {
            return LocationManager.NETWORK_PROVIDER;
        }
        return null;
    }

    /**
     * Check if service is already running
     */
    public boolean isServiceRunning(Class<?> serviceClass) {
        List<ActivityManager.RunningServiceInfo> runningServices =
                activityManager.getRunningServices(Integer.MAX_VALUE);

        for (ActivityManager.RunningServiceInfo service : runningServices) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get memory info
     */
    public ActivityManager.MemoryInfo getMemoryInfo() {
        ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
        activityManager.getMemoryInfo(memoryInfo);
        return memoryInfo;
    }

    /**
     * Check if device has enough memory for services
     */
    public boolean hasEnoughMemory() {
        ActivityManager.MemoryInfo memoryInfo = getMemoryInfo();
        return !memoryInfo.lowMemory;
    }

    /**
     * Get recommended location priority based on state
     */
    public int getRecommendedLocationPriority() {
        if (isDeviceCharging()) {
            return com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY;
        } else if (isBatterySaverOn()) {
            return com.google.android.gms.location.Priority.PRIORITY_BALANCED_POWER_ACCURACY;
        } else {
            return com.google.android.gms.location.Priority.PRIORITY_BALANCED_POWER_ACCURACY;
        }
    }

    /**
     * Check if device is in doze mode
     */
    public boolean isInDozeMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return powerManager.isDeviceIdleMode();
        }
        return false;
    }

    /**
     * Get optimal geofence radius multiplier based on battery
     */
    public float getOptimalRadiusMultiplier() {
        if (isBatterySaverOn()) {
            return 1.5f; // Use larger radius in battery saver (less frequent triggers)
        } else if (isDeviceCharging()) {
            return 0.8f; // Use smaller radius when charging (more precise)
        } else {
            return 1.0f; // Normal
        }
    }
}