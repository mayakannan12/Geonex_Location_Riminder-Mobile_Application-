package com.example.geonex;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.splashscreen.SplashScreen;

public class SplashActivity extends AppCompatActivity {

    private static final String TAG = "SplashActivity";
    private static final String PREFS_NAME = "geonex_prefs";
    private static final String KEY_ONBOARDING_DONE = "onboarding_done";
    private static final long SPLASH_DURATION = 1800; // 1.8 seconds

    private SharedPreferences sharedPreferences;
    private Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Handle splash screen for Android 12+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            SplashScreen splashScreen = SplashScreen.installSplashScreen(this);
            splashScreen.setKeepOnScreenCondition(() -> false);
        }

        setContentView(R.layout.activity_splash);

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        // ===== DEBUG: FORCE RESET ONBOARDING FLAG =====
        // This will make the app show onboarding every time (REMOVE AFTER TESTING)
        sharedPreferences.edit().putBoolean(KEY_ONBOARDING_DONE, false).apply();
        Log.d(TAG, "DEBUG: Forced onboarding_done = false");
        // ==============================================

        // Check current state
        boolean onboardingDone = sharedPreferences.getBoolean(KEY_ONBOARDING_DONE, false);
        Log.d(TAG, "Current onboarding_done value: " + onboardingDone);

        // Start animations
        startAnimations();

        // Navigate after delay
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                navigateToNextScreen();
            }
        }, SPLASH_DURATION);
    }

    private void startAnimations() {
        ImageView logoImage = findViewById(R.id.logoImage);
        TextView appNameText = findViewById(R.id.appNameText);
        TextView taglineText = findViewById(R.id.taglineText);

        // Logo animation
        if (logoImage != null) {
            logoImage.setAlpha(0f);
            logoImage.animate()
                    .alpha(1f)
                    .setDuration(1000)
                    .setInterpolator(new DecelerateInterpolator())
                    .start();
        }

        // App name animation
        if (appNameText != null) {
            appNameText.setAlpha(0f);
            appNameText.animate()
                    .alpha(1f)
                    .setDuration(800)
                    .setStartDelay(400)
                    .setInterpolator(new DecelerateInterpolator())
                    .start();
        }

        // Tagline animation
        if (taglineText != null) {
            taglineText.setAlpha(0f);
            taglineText.animate()
                    .alpha(1f)
                    .setDuration(600)
                    .setStartDelay(800)
                    .setInterpolator(new DecelerateInterpolator())
                    .start();
        }
    }

    private void navigateToNextScreen() {
        // Check if onboarding is completed
        boolean onboardingDone = sharedPreferences.getBoolean(KEY_ONBOARDING_DONE, false);

        Log.d(TAG, "Navigating - onboardingDone = " + onboardingDone);

        Intent intent;
        if (!onboardingDone) {
            // First launch - show onboarding
            Log.d(TAG, "First launch - going to OnboardingActivity");
            intent = new Intent(SplashActivity.this, OnboardingActivity.class);
        } else {
            // Returning user - go directly to home
            Log.d(TAG, "Returning user - going to HomeActivity");
            intent = new Intent(SplashActivity.this, HomeActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        }

        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
    }

    @Override
    public void onBackPressed() {
        // Prevent back button during splash
        // Do nothing
        super.onBackPressed();
    }
}