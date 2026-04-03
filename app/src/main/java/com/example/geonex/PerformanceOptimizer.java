package com.example.geonex;

import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.LruCache;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class PerformanceOptimizer {

    private static final String TAG = "PerformanceOptimizer";
    private final Context context;
    private final ActivityManager activityManager;
    private final ExecutorService executorService;
    private final Handler mainHandler;

    // Cache for bitmap images
    private LruCache<String, Bitmap> memoryCache;

    // Cache for reminder data
    private LruCache<Integer, Reminder> reminderCache;

    // Memory thresholds
    private static final int MAX_MEMORY_CACHE_SIZE = 10 * 1024 * 1024; // 10MB
    private static final int MAX_REMINDER_CACHE_SIZE = 50; // 50 reminders

    // Optimization flags
    private boolean isLowMemoryDevice = false;
    private boolean isPowerSaveMode = false;

    public PerformanceOptimizer(Context context) {
        this.context = context;
        this.activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        this.executorService = Executors.newFixedThreadPool(3);
        this.mainHandler = new Handler(Looper.getMainLooper());

        detectDeviceCapabilities();
        setupMemoryCache();
    }

    /**
     * Detect device capabilities
     */
    private void detectDeviceCapabilities() {
        ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
        activityManager.getMemoryInfo(memoryInfo);

        // Check if device has low memory
        long totalMemory = memoryInfo.totalMem;
        isLowMemoryDevice = totalMemory < 2L * 1024 * 1024 * 1024; // Less than 2GB RAM

        Log.d(TAG, "Device RAM: " + (totalMemory / (1024 * 1024)) + "MB");
        Log.d(TAG, "Low memory device: " + isLowMemoryDevice);
    }

    /**
     * Setup memory cache
     */
    private void setupMemoryCache() {
        // Calculate cache size based on device capabilities
        int cacheSize = isLowMemoryDevice ?
                MAX_MEMORY_CACHE_SIZE / 2 : MAX_MEMORY_CACHE_SIZE;

        memoryCache = new LruCache<String, Bitmap>(cacheSize) {
            @Override
            protected int sizeOf(String key, Bitmap bitmap) {
                return bitmap.getByteCount();
            }
        };

        reminderCache = new LruCache<Integer, Reminder>(
                isLowMemoryDevice ? MAX_REMINDER_CACHE_SIZE / 2 : MAX_REMINDER_CACHE_SIZE);
    }

    // ===== BITMAP CACHE METHODS (Used by ImageLoader) =====

    /**
     * Cache a bitmap
     */
    public void cacheBitmap(String key, Bitmap bitmap) {
        if (memoryCache != null && bitmap != null) {
            memoryCache.put(key, bitmap);
            Log.d(TAG, "Cached bitmap: " + key);
        }
    }

    /**
     * Get cached bitmap
     */
    public Bitmap getCachedBitmap(String key) {
        Bitmap bitmap = memoryCache != null ? memoryCache.get(key) : null;
        if (bitmap != null) {
            Log.d(TAG, "Bitmap cache hit: " + key);
        }
        return bitmap;
    }

    // ===== REMINDER CACHE METHODS =====

    /**
     * Cache a reminder
     */
    public void cacheReminder(Reminder reminder) {
        if (reminderCache != null && reminder != null) {
            reminderCache.put(reminder.getId(), reminder);
        }
    }

    /**
     * Get cached reminder
     */
    public Reminder getCachedReminder(int id) {
        return reminderCache != null ? reminderCache.get(id) : null;
    }

    /**
     * Clear all caches
     */
    public void clearCaches() {
        if (memoryCache != null) {
            memoryCache.evictAll();
        }
        if (reminderCache != null) {
            reminderCache.evictAll();
        }
        Log.d(TAG, "Caches cleared");
    }

    // ===== IMAGE OPTIMIZATION METHODS (Used by ImageLoader) =====

    /**
     * Get optimal image sample size
     */
    public int getOptimalSampleSize(int imageWidth, int imageHeight, int targetWidth, int targetHeight) {
        int sampleSize = 1;

        if (imageHeight > targetHeight || imageWidth > targetWidth) {
            final int halfHeight = imageHeight / 2;
            final int halfWidth = imageWidth / 2;

            while ((halfHeight / sampleSize) >= targetHeight
                    && (halfWidth / sampleSize) >= targetWidth) {
                sampleSize *= 2;
            }
        }

        return sampleSize;
    }

    /**
     * Check if we should load high-resolution images
     */
    public boolean shouldLoadHighRes() {
        return !isLowMemoryDevice && !isPowerSaveMode;
    }

    /**
     * Load bitmap with optimal size
     */
    public Bitmap loadOptimizedBitmap(String filePath, int reqWidth, int reqHeight) {
        // First decode with inJustDecodeBounds=true to check dimensions
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(filePath, options);

        // Calculate inSampleSize
        options.inSampleSize = getOptimalSampleSize(
                options.outWidth, options.outHeight, reqWidth, reqHeight);

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        options.inPreferredConfig = shouldLoadHighRes() ?
                Bitmap.Config.ARGB_8888 : Bitmap.Config.RGB_565;

        return BitmapFactory.decodeFile(filePath, options);
    }

    // ===== POWER MANAGEMENT =====

    /**
     * Update power save mode
     */
    public void setPowerSaveMode(boolean enabled) {
        this.isPowerSaveMode = enabled;
        Log.d(TAG, "Power save mode: " + enabled);
    }

    // ===== THROTTLING =====

    private Runnable pendingScrollRunnable;
    private long lastScrollTime = 0;
    private static final long SCROLL_THROTTLE_MS = 100;

    /**
     * Throttle scroll events to prevent excessive processing
     */
    public void throttleScroll(Runnable runnable) {
        long now = System.currentTimeMillis();

        if (pendingScrollRunnable != null) {
            mainHandler.removeCallbacks(pendingScrollRunnable);
        }

        if (now - lastScrollTime > SCROLL_THROTTLE_MS) {
            runnable.run();
            lastScrollTime = now;
        } else {
            pendingScrollRunnable = runnable;
            mainHandler.postDelayed(() -> {
                runnable.run();
                lastScrollTime = System.currentTimeMillis();
                pendingScrollRunnable = null;
            }, SCROLL_THROTTLE_MS);
        }
    }

    // ===== BACKGROUND TASKS =====

    /**
     * Execute task in background
     */
    public void executeInBackground(Runnable runnable) {
        if (executorService != null) {
            executorService.execute(runnable);
        } else {
            new Thread(runnable).start();
        }
    }

    /**
     * Run task on UI thread
     */
    public void runOnUiThread(Runnable runnable) {
        if (mainHandler != null) {
            mainHandler.post(runnable);
        } else {
            runnable.run();
        }
    }

    /**
     * Execute task with delay
     */
    public void scheduleTask(Runnable runnable, long delayMs) {
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.schedule(() -> {
            runnable.run();
            scheduler.shutdown();
        }, delayMs, TimeUnit.MILLISECONDS);
    }

    /**
     * Shutdown executor service
     */
    public void shutdown() {
        if (executorService != null) {
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(800, TimeUnit.MILLISECONDS)) {
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
            }
        }
    }
}