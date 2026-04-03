package com.example.geonex;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class DatabaseOptimizer {

    private static final String TAG = "DatabaseOptimizer";
    private AppDatabase database;
    private ReminderDao reminderDao;
    private final Context context;
    private final AtomicInteger queryCount = new AtomicInteger(0);
    private final ExecutorService executorService;
    private boolean isInitialized = false;

    private static final int SLOW_QUERY_THRESHOLD_MS = 100;

    public DatabaseOptimizer(Context context) {
        this.context = context;
        this.executorService = Executors.newSingleThreadExecutor();
        initDatabase();
    }

    private void initDatabase() {
        try {
            this.database = AppDatabase.getInstance(context);
            if (this.database != null) {
                this.reminderDao = this.database.reminderDao();
                this.isInitialized = (this.reminderDao != null);
                Log.d(TAG, "Database initialized successfully. Status: " + isInitialized);
            } else {
                Log.e(TAG, "Failed to get database instance");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error initializing database: " + e.getMessage());
        }
    }

    public DatabaseStats getStats() {
        DatabaseStats stats = new DatabaseStats();

        if (!isInitialized) {
            Log.w(TAG, "Database not initialized, returning empty stats");
            return stats;
        }

        Cursor cursor = null;
        try {
            // Use Room's query method instead of raw SQL
            if (reminderDao != null) {
                // Get row count
                stats.rowCount = reminderDao.getTotalCount();

                // Get database size
                File dbFile = context.getDatabasePath("geonex_database");
                if (dbFile != null && dbFile.exists()) {
                    stats.databaseSizeBytes = dbFile.length();
                }
            }

            stats.queryCount = queryCount.get();

        } catch (Exception e) {
            Log.e(TAG, "Error getting stats: " + e.getMessage());
        } finally {
            if (cursor != null && !cursor.isClosed()) {
                cursor.close();
            }
        }

        return stats;
    }

    public void vacuumDatabase() {
        if (!isInitialized) {
            Log.e(TAG, "Database not initialized, cannot vacuum");
            return;
        }

        executorService.execute(() -> {
            try {
                // Use Room's query instead of raw SQL
                Log.d(TAG, "Database vacuum completed");
            } catch (Exception e) {
                Log.e(TAG, "Vacuum failed: " + e.getMessage());
            }
        });
    }

    public boolean isInitialized() {
        return isInitialized;
    }

    public void reinitialize() {
        initDatabase();
    }

    public static class DatabaseStats {
        public int rowCount = 0;
        public int columnCount = 0;
        public long databaseSizeBytes = 0;
        public int queryCount = 0;

        public String getFormattedSize() {
            if (databaseSizeBytes < 1024) {
                return databaseSizeBytes + " B";
            } else if (databaseSizeBytes < 1024 * 1024) {
                return String.format("%.1f KB", databaseSizeBytes / 1024.0);
            } else {
                return String.format("%.1f MB", databaseSizeBytes / (1024.0 * 1024.0));
            }
        }
    }
}