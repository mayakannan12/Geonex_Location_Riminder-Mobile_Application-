package com.example.geonex;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;
import android.provider.Settings;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;

public class PermissionHelper {

    private final Activity activity;

    // Permission constants
    public static final int PERMISSION_REQUEST_CODE = 100;
    public static final String[] REQUIRED_PERMISSIONS;

    static {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            REQUIRED_PERMISSIONS = new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION,
                    Manifest.permission.POST_NOTIFICATIONS
            };
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            REQUIRED_PERMISSIONS = new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION
            };
        } else {
            REQUIRED_PERMISSIONS = new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            };
        }
    }

    public PermissionHelper(Activity activity) {
        this.activity = activity;
    }

    // Check if all permissions are granted
    public boolean hasAllPermissions() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(activity, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    // Check specific permission
    public boolean hasPermission(String permission) {
        return ContextCompat.checkSelfPermission(activity, permission)
                == PackageManager.PERMISSION_GRANTED;
    }

    // Show rationale dialog for permissions
    public void showPermissionRationale(String permission, Runnable onConfirm) {
        String message;
        switch (permission) {
            case Manifest.permission.ACCESS_FINE_LOCATION:
            case Manifest.permission.ACCESS_COARSE_LOCATION:
                message = "Location permission is needed to detect when you enter a reminder area.";
                break;
            case Manifest.permission.ACCESS_BACKGROUND_LOCATION:
                message = "Background location permission is needed to trigger reminders even when the app is closed.";
                break;
            case Manifest.permission.POST_NOTIFICATIONS:
                message = "Notification permission is needed to alert you when you reach a reminder location.";
                break;
            default:
                message = "This permission is needed for the app to function properly.";
        }

        new AlertDialog.Builder(activity)
                .setTitle("Permission Required")
                .setMessage(message)
                .setPositiveButton("OK", (dialog, which) -> onConfirm.run())
                .setNegativeButton("Cancel", null)
                .show();
    }

    // Show settings dialog if user permanently denied
    public void showSettingsDialog(String permission) {
        new AlertDialog.Builder(activity)
                .setTitle("Permission Required")
                .setMessage("You have permanently denied this permission. Please enable it in app settings.")
                .setPositiveButton("Open Settings", (dialog, which) -> {
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    Uri uri = Uri.fromParts("package", activity.getPackageName(), null);
                    intent.setData(uri);
                    activity.startActivity(intent);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // Check if battery optimization is disabled
    @SuppressLint("ObsoleteSdkInt")
    public boolean isBatteryOptimizationDisabled() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PowerManager powerManager = (PowerManager) activity.getSystemService(Context.POWER_SERVICE);
            return powerManager.isIgnoringBatteryOptimizations(activity.getPackageName());
        }
        return true; // Not needed for older versions
    }

    // Request to ignore battery optimization
    @SuppressLint("ObsoleteSdkInt")
    public void requestDisableBatteryOptimization() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
            intent.setData(Uri.parse("package:" + activity.getPackageName()));
            activity.startActivity(intent);
        }
    }

    // Show GPS settings dialog
    public void showGPSSettingsDialog() {
        new AlertDialog.Builder(activity)
                .setTitle("GPS Required")
                .setMessage("GPS is turned off. Please enable GPS for accurate location detection.")
                .setPositiveButton("Open Settings", (dialog, which) -> {
                    Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    activity.startActivity(intent);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
    /**
     * Check if background location permission is granted
     */
    public boolean hasBackgroundLocationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return hasPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION);
        }
        return true; // Not needed for older versions
    }

}
