package com.example.geonex;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.splashscreen.SplashScreen;

public class MainActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "GeonexPrefs";
    private static final String KEY_FIRST_LAUNCH = "first_launch";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Handle splash screen (Android 12+)
        SplashScreen splashScreen = SplashScreen.installSplashScreen(this);
        super.onCreate(savedInstanceState);

        // Keep splash visible for a moment
        splashScreen.setKeepOnScreenCondition(() -> true);

        // Delay navigation
        new Handler().postDelayed(() -> {
            splashScreen.setKeepOnScreenCondition(() -> false);
            checkFirstLaunch();
        }, 1500); // 1.5 seconds splash
    }

    private void checkFirstLaunch() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean isFirstLaunch = prefs.getBoolean(KEY_FIRST_LAUNCH, true);

        Intent intent;
        if (isFirstLaunch) {
            // First time - go to onboarding
            intent = new Intent(MainActivity.this, OnboardingActivity.class);
            // Mark that user has seen onboarding
            prefs.edit().putBoolean(KEY_FIRST_LAUNCH, false).apply();
        } else {
            // Not first time - check if biometric lock is enabled
            SecurityManager securityManager = new SecurityManager(this);
            boolean isBiometricEnabled = securityManager.getSecureBoolean("biometric_enabled", false);

            if (isBiometricEnabled) {
                // Biometric lock is enabled - go to app lock screen
                intent = new Intent(MainActivity.this, AppLockActivity.class);
            } else {
                // No biometric lock - go directly to home
                intent = new Intent(MainActivity.this, HomeActivity.class);
            }
        }

        startActivity(intent);
        finish();
    }
}