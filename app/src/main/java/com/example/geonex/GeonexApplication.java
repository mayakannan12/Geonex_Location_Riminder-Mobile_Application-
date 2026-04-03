package com.example.geonex;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

public class GeonexApplication extends Application {

    public static final String CHANNEL_ID = "geonex_channel";
    public static final String CHANNEL_NAME = "Geonex Notifications";

    private static GeonexApplication instance;
    private AppDatabase database;
    private ReminderRepository repository;
    private ThemeManager themeManager;
    private SafetyManager safetyManager;
    private RecoveryManager recoveryManager;
    private ErrorHandler errorHandler;
    private PerformanceOptimizer performanceOptimizer;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;

        // Set up uncaught exception handler
        setupExceptionHandler();

        // Initialize ThemeManager and apply saved theme
        themeManager = ThemeManager.getInstance(this);
        themeManager.applyTheme();

        // Initialize PerformanceOptimizer
        performanceOptimizer = new PerformanceOptimizer(this);

        // Initialize database
        database = AppDatabase.getInstance(this);
        repository = new ReminderRepository(this);

        // Initialize safety managers
        safetyManager = new SafetyManager(this);
        recoveryManager = new RecoveryManager(this);
        errorHandler = new ErrorHandler(this);

        createNotificationChannel();

        Log.d("GeonexApp", "Application created successfully");
    }

    public static GeonexApplication getInstance() {
        return instance;
    }

    public AppDatabase getDatabase() {
        return database;
    }

    public ReminderRepository getRepository() {
        return repository;
    }

    public ThemeManager getThemeManager() {
        return themeManager;
    }

    public SafetyManager getSafetyManager() {
        return safetyManager;
    }

    public RecoveryManager getRecoveryManager() {
        return recoveryManager;
    }

    public ErrorHandler getErrorHandler() {
        return errorHandler;
    }

    public PerformanceOptimizer getPerformanceOptimizer() {
        return performanceOptimizer;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Channel for Geonex location reminders");
            channel.enableVibration(true);
            channel.setVibrationPattern(new long[]{100, 200, 300, 400, 500});

            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private void setupExceptionHandler() {
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread thread, Throwable throwable) {
                Log.e("GeonexApp", "CRASH: " + throwable.getMessage());
                throwable.printStackTrace();

                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(GeonexApplication.this,
                                "App crashed. Restarting...",
                                Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
    }
}