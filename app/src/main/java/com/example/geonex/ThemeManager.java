package com.example.geonex;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Build;

import androidx.appcompat.app.AppCompatDelegate;

public class ThemeManager {

    private static final String PREFS_NAME = "theme_prefs";
    private static final String KEY_THEME_MODE = "theme_mode";

    // Theme modes
    public static final int THEME_MODE_LIGHT = 0;
    public static final int THEME_MODE_DARK = 1;
    public static final int THEME_MODE_SYSTEM = 2;

    private static ThemeManager instance;
    private final SharedPreferences prefs;
    private Context context; // Store context reference

    private ThemeManager(Context context) {
        this.context = context.getApplicationContext(); // Store application context
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static synchronized ThemeManager getInstance(Context context) {
        if (instance == null) {
            instance = new ThemeManager(context.getApplicationContext());
        }
        return instance;
    }

    /**
     * Apply saved theme mode
     */
    public void applyTheme() {
        int themeMode = getThemeMode();
        switch (themeMode) {
            case THEME_MODE_LIGHT:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
            case THEME_MODE_DARK:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
            case THEME_MODE_SYSTEM:
            default:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                } else {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY);
                }
                break;
        }
    }

    /**
     * Set theme mode and apply
     */
    public void setThemeMode(int themeMode) {
        prefs.edit().putInt(KEY_THEME_MODE, themeMode).apply();
        applyTheme();
    }

    /**
     * Get current theme mode
     */
    public int getThemeMode() {
        return prefs.getInt(KEY_THEME_MODE, THEME_MODE_SYSTEM); // Default to system
    }

    /**
     * Get theme mode name for display
     */
    public String getThemeModeName() {
        int themeMode = getThemeMode();
        switch (themeMode) {
            case THEME_MODE_LIGHT:
                return "Light";
            case THEME_MODE_DARK:
                return "Dark";
            case THEME_MODE_SYSTEM:
            default:
                return "Follow System";
        }
    }

    /**
     * Check if dark mode is currently active
     * FIXED: Now uses stored context, not passed parameter
     */
    public boolean isDarkModeActive() {
        if (context == null) {
            // Fallback if context is null
            return false;
        }
        int currentNightMode = context.getResources().getConfiguration().uiMode &
                Configuration.UI_MODE_NIGHT_MASK;
        return currentNightMode == Configuration.UI_MODE_NIGHT_YES;
    }

    /**
     * Get all theme options for display
     */
    public static String[] getThemeOptions() {
        return new String[]{"Light", "Dark", "Follow System"};
    }

    /**
     * Toggle between light and dark (for quick toggle)
     */
    public void toggleTheme() {
        int currentMode = getThemeMode();
        if (currentMode == THEME_MODE_LIGHT) {
            setThemeMode(THEME_MODE_DARK);
        } else if (currentMode == THEME_MODE_DARK) {
            setThemeMode(THEME_MODE_LIGHT);
        } else {
            // If system mode, check current and toggle opposite
            if (isDarkModeActive()) {
                setThemeMode(THEME_MODE_LIGHT);
            } else {
                setThemeMode(THEME_MODE_DARK);
            }
        }
    }

    public boolean isNightMode() {
        int nightModeFlags = context.getResources().getConfiguration().uiMode &
                Configuration.UI_MODE_NIGHT_MASK;
        return nightModeFlags == Configuration.UI_MODE_NIGHT_YES;
    }
}