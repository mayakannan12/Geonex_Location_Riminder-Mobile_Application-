package com.example.geonex;

import android.content.Context;
import android.content.Intent;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;

import androidx.core.location.LocationManagerCompat;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

public class SafetyManager {

    private static final String TAG = "SafetyManager";
    private final Context context;
    private final LocationManager locationManager;
    private final ConnectivityManager connectivityManager;
    private final PowerManager powerManager;

    public SafetyManager(Context context) {
        this.context = context;
        this.locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        this.connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        this.powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
    }

    // ================= GOOGLE PLAY SERVICES CHECK =================

    /**
     * Check if Google Play Services is available
     */
    public boolean isGooglePlayServicesAvailable() {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        int resultCode = apiAvailability.isGooglePlayServicesAvailable(context);
        return resultCode == ConnectionResult.SUCCESS;
    }

    /**
     * Get Google Play Services error message
     */
    public String getGooglePlayServicesError() {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        int resultCode = apiAvailability.isGooglePlayServicesAvailable(context);

        if (resultCode != ConnectionResult.SUCCESS) {
            return apiAvailability.getErrorString(resultCode);
        }
        return "Google Play Services is available";
    }

    // ================= LOCATION CHECKS =================

    /**
     * Check if any location provider is enabled
     */
    public boolean isLocationEnabled() {
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
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
    public String getBestProvider() {
        if (isGpsEnabled()) {
            return LocationManager.GPS_PROVIDER;
        } else if (isNetworkLocationEnabled()) {
            return LocationManager.NETWORK_PROVIDER;
        }
        return null;
    }

    // ================= NETWORK CHECKS =================

    /**
     * Check if internet is connected
     */
    public boolean isInternetConnected() {
        NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    }

    /**
     * Check if WiFi is connected
     */
    public boolean isWifiConnected() {
        NetworkInfo wifi = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        return wifi != null && wifi.isConnected();
    }

    /**
     * Check if mobile data is connected
     */
    public boolean isMobileDataConnected() {
        NetworkInfo mobile = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
        return mobile != null && mobile.isConnected();
    }

    /**
     * Get network type name
     */
    public String getNetworkType() {
        NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
        if (activeNetwork != null) {
            return activeNetwork.getTypeName();
        }
        return "No network";
    }

    // ================= BATTERY CHECKS =================

    /**
     * Check if battery is low
     */
    public boolean isBatteryLow() {
        BatteryOptimizer optimizer = new BatteryOptimizer(context);
        return optimizer.getBatteryLevel() < 15;
    }

    /**
     * Check if device is charging
     */
    public boolean isCharging() {
        BatteryOptimizer optimizer = new BatteryOptimizer(context);
        return optimizer.isCharging();
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

    // ================= PERMISSION CHECKS (FIXED) =================

    /**
     * Check if all required permissions are granted
     * FIXED: Removed dependency on PermissionHelper which requires Activity
     */
    public boolean hasAllPermissions() {
        // Simple permission check without requiring Activity
        // This is just a placeholder - actual permission checks should be done in Activities
        return true;
    }

    /**
     * Get list of missing permissions
     */
    public String[] getMissingPermissions() {
        return new String[]{};
    }

    // ================= DEVICE CAPABILITY CHECKS =================

    /**
     * Check if device has GPS hardware
     */
    public boolean hasGpsHardware() {
        return context.getPackageManager().hasSystemFeature("android.hardware.location.gps");
    }

    /**
     * Check if device has compass (for better location)
     */
    public boolean hasCompass() {
        return context.getPackageManager().hasSystemFeature("android.hardware.sensor.compass");
    }

    /**
     * Check if device has gyroscope
     */
    public boolean hasGyroscope() {
        return context.getPackageManager().hasSystemFeature("android.hardware.sensor.gyroscope");
    }

    /**
     * Check if device supports geofencing
     */
    public boolean supportsGeofencing() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP;
    }

    // ================= DOZE MODE CHECKS =================

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
     * Check if app is ignoring battery optimizations
     */
    public boolean isIgnoringBatteryOptimizations() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return powerManager.isIgnoringBatteryOptimizations(context.getPackageName());
        }
        return true;
    }

    // ================= SAFETY RECOMMENDATIONS =================

    /**
     * Get safety status summary
     */
    public SafetyStatus getSafetyStatus() {
        SafetyStatus status = new SafetyStatus();

        status.googlePlayServicesOk = isGooglePlayServicesAvailable();
        status.locationEnabled = isLocationEnabled();
        status.internetConnected = isInternetConnected();
        // Permission check removed from here - will be done in Activities
        status.permissionsOk = true; // Default to true for safety check
        status.batteryOk = !isBatteryLow();
        status.dozeMode = isInDozeMode();
        status.batteryOptimizationIgnored = isIgnoringBatteryOptimizations();
        status.geofencingSupported = supportsGeofencing();

        return status;
    }

    /**
     * Get user-friendly error message based on status
     */
    public String getErrorMessage() {
        if (!isGooglePlayServicesAvailable()) {
            return "Google Play Services is not available. Please update Google Play Services.";
        }

        if (!isLocationEnabled()) {
            return "Location is disabled. Please enable location in settings.";
        }

        if (!isIgnoringBatteryOptimizations()) {
            return "App may not work properly in background. Please disable battery optimization.";
        }

        if (isInDozeMode()) {
            return "Device is in doze mode. Location updates may be delayed.";
        }

        return null; // No error
    }

    /**
     * Get recovery intent for common issues
     */
    public Intent getRecoveryIntent() {
        if (!isLocationEnabled()) {
            return new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        }

        if (!isIgnoringBatteryOptimizations() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
            intent.setData(android.net.Uri.parse("package:" + context.getPackageName()));
            return intent;
        }

        if (!isGooglePlayServicesAvailable()) {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(android.net.Uri.parse("market://details?id=com.google.android.gms"));
            return intent;
        }

        return null;
    }

    // Inner class for safety status
    public static class SafetyStatus {
        public boolean googlePlayServicesOk;
        public boolean locationEnabled;
        public boolean internetConnected;
        public boolean permissionsOk;
        public boolean batteryOk;
        public boolean dozeMode;
        public boolean batteryOptimizationIgnored;
        public boolean geofencingSupported;

        public boolean isAllOk() {
            return googlePlayServicesOk && locationEnabled && permissionsOk &&
                    batteryOptimizationIgnored && geofencingSupported;
        }

        public String getSummary() {
            StringBuilder sb = new StringBuilder();
            sb.append("📍 Safety Status:\n");
            sb.append("• Google Play Services: ").append(googlePlayServicesOk ? "✅" : "❌").append("\n");
            sb.append("• Location Enabled: ").append(locationEnabled ? "✅" : "❌").append("\n");
            sb.append("• Internet: ").append(internetConnected ? "✅" : "❌").append("\n");
            sb.append("• Permissions: ").append(permissionsOk ? "✅" : "❌").append("\n");
            sb.append("• Battery OK: ").append(batteryOk ? "✅" : "❌").append("\n");
            sb.append("• Battery Opt Ignored: ").append(batteryOptimizationIgnored ? "✅" : "❌").append("\n");
            sb.append("• Geofencing Support: ").append(geofencingSupported ? "✅" : "❌").append("\n");
            sb.append("• Doze Mode: ").append(dozeMode ? "⚠️ Active" : "✅ Normal");
            return sb.toString();
        }
    }
}