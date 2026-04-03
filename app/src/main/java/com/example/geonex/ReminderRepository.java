package com.example.geonex;

import android.content.Context;
import android.util.Log;

import androidx.lifecycle.LiveData;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ReminderRepository {

    private static final String TAG = "ReminderRepository";

    private final ReminderDao reminderDao;
    private final LiveData<List<Reminder>> allReminders;
    private final LiveData<List<Reminder>> activeReminders;
    private final LiveData<List<Reminder>> completedReminders;
    private final ExecutorService executorService;
    private PerformanceOptimizer performanceOptimizer;

    public ReminderRepository(Context context) {
        AppDatabase database = AppDatabase.getInstance(context);
        this.reminderDao = database.reminderDao();
        this.allReminders = reminderDao.getAllReminders();
        this.activeReminders = reminderDao.getActiveReminders();
        this.completedReminders = reminderDao.getCompletedReminders();
        this.executorService = Executors.newSingleThreadExecutor();
        this.performanceOptimizer = ((GeonexApplication) context.getApplicationContext()).getPerformanceOptimizer();

        Log.d(TAG, "ReminderRepository initialized");
    }

    // ===== INSERT OPERATIONS =====

    /**
     * Insert a reminder without callback
     */
    public void insert(Reminder reminder) {
        Log.d("DEBUG", "📦 Repository: inserting reminder without callback: " + reminder.getTitle());
        executorService.execute(() -> {
            long id = reminderDao.insert(reminder);
            Log.d("DEBUG", "📦 Repository: insert returned ID: " + id);
            if (performanceOptimizer != null) {
                performanceOptimizer.clearCaches();
            }
        });
    }

    /**
     * Insert a reminder with callback
     */
    public void insert(Reminder reminder, final OnReminderInsertedListener listener) {
        Log.d("DEBUG", "📦 Repository: inserting reminder with callback: " + reminder.getTitle());
        executorService.execute(() -> {
            long id = reminderDao.insert(reminder);
            Log.d("DEBUG", "📦 Repository: insert returned ID: " + id);
            if (listener != null) {
                reminder.setId((int) id);
                listener.onInserted(reminder);
            }
            if (performanceOptimizer != null) {
                performanceOptimizer.clearCaches();
            }
        });
    }

    /**
     * Batch insert multiple reminders
     */
    public void insertAll(List<Reminder> reminders) {
        Log.d(TAG, "Batch inserting " + (reminders != null ? reminders.size() : 0) + " reminders");
        executorService.execute(() -> {
            reminderDao.insertAll(reminders);
            if (performanceOptimizer != null) {
                performanceOptimizer.clearCaches();
            }
        });
    }

    // ===== UPDATE OPERATIONS =====

    /**
     * Update a reminder
     */
    public void update(Reminder reminder) {
        Log.d("DEBUG", "📦 Repository: updating reminder: " + reminder.getId() + " - " + reminder.getTitle());
        executorService.execute(() -> {
            reminderDao.update(reminder);
            if (performanceOptimizer != null) {
                performanceOptimizer.clearCaches();
            }
        });
    }

    /**
     * Mark a reminder as completed by ID
     */
    public void markAsCompleted(int id) {
        Log.d(TAG, "Marking reminder as completed: " + id);
        executorService.execute(() -> {
            reminderDao.markAsCompleted(id);
            if (performanceOptimizer != null) {
                performanceOptimizer.clearCaches();
            }
        });
    }

    // ===== DELETE OPERATIONS =====

    /**
     * Delete a reminder
     */
    public void delete(Reminder reminder) {
        Log.d("DEBUG", "📦 Repository: deleting reminder: " + reminder.getId() + " - " + reminder.getTitle());
        executorService.execute(() -> {
            reminderDao.delete(reminder);
            if (performanceOptimizer != null) {
                performanceOptimizer.clearCaches();
            }
        });
    }

    /**
     * Delete a reminder by ID
     */
    public void deleteById(int id) {
        Log.d(TAG, "Deleting reminder by ID: " + id);
        executorService.execute(() -> {
            Reminder reminder = reminderDao.getReminderById(id);
            if (reminder != null) {
                reminderDao.delete(reminder);
                if (performanceOptimizer != null) {
                    performanceOptimizer.clearCaches();
                }
            }
        });
    }

    /**
     * Delete all completed reminders
     */
    public void deleteCompletedReminders() {
        Log.d(TAG, "Deleting all completed reminders");
        executorService.execute(() -> {
            reminderDao.deleteCompletedReminders();
            if (performanceOptimizer != null) {
                performanceOptimizer.clearCaches();
            }
        });
    }

    // ===== LIVE DATA QUERIES (Auto-updating UI) =====

    /**
     * Get all reminders as LiveData
     */
    public LiveData<List<Reminder>> getAllReminders() {
        return allReminders;
    }

    /**
     * Get active (not completed) reminders as LiveData
     */
    public LiveData<List<Reminder>> getActiveReminders() {
        return activeReminders;
    }

    /**
     * Get completed reminders as LiveData
     */
    public LiveData<List<Reminder>> getCompletedReminders() {
        return completedReminders;
    }

    /**
     * Get reminders by category as LiveData
     */
    public LiveData<List<Reminder>> getRemindersByCategory(String category) {
        return reminderDao.getRemindersByCategory(category);
    }

    /**
     * Search reminders by title or location as LiveData
     */
    public LiveData<List<Reminder>> searchReminders(String query) {
        return reminderDao.searchReminders(query);
    }

    // ===== SYNCHRONOUS QUERIES (for background threads) =====

    /**
     * Get a reminder by ID (synchronous - use in background threads only)
     */
    public Reminder getReminderById(int id) {
        Log.d("DEBUG", "📦 Repository: getReminderById called for ID: " + id);

        // Check cache first
        if (performanceOptimizer != null) {
            Reminder cached = performanceOptimizer.getCachedReminder(id);
            if (cached != null) {
                Log.d("DEBUG", "📦 Repository: returning cached reminder: " + cached.getTitle());
                return cached;
            }
        }

        Reminder reminder = reminderDao.getReminderById(id);
        Log.d("DEBUG", "📦 Repository: database returned reminder: " + (reminder != null ? reminder.getTitle() : "null"));

        // Cache for future use
        if (reminder != null && performanceOptimizer != null) {
            performanceOptimizer.cacheReminder(reminder);
        }

        return reminder;
    }

    /**
     * Get all active recurring reminders (synchronous - use in background threads only)
     */
    public List<Reminder> getActiveRecurringReminders() {
        return reminderDao.getActiveRecurringReminders();
    }

    /**
     * Get all reminders synchronously
     */
    public List<Reminder> getAllRemindersSync() {
        return reminderDao.getAllRemindersSync();
    }

    // ===== COUNT QUERIES =====

    /**
     * Get total count of reminders
     */
    public int getTotalCount() {
        return reminderDao.getTotalCount();
    }

    /**
     * Get count of completed reminders
     */
    public int getCompletedCount() {
        return reminderDao.getCompletedCount();
    }

    /**
     * Get count of active reminders
     */
    public int getActiveCount() {
        return reminderDao.getActiveCount();
    }

    /**
     * Get count of recurring reminders
     */
    public int getTotalRecurringCount() {
        return reminderDao.getTotalRecurringCount();
    }

    // ===== STATISTICS METHODS =====

    /**
     * Get basic statistics (total, completed, active)
     */
    public void getStatistics(final OnStatisticsListener listener) {
        executorService.execute(() -> {
            int total = reminderDao.getTotalCount();
            int completed = reminderDao.getCompletedCount();
            int active = reminderDao.getActiveCount();

            if (listener != null) {
                listener.onStatistics(total, completed, active);
            }
        });
    }

    /**
     * Get category statistics
     */
    public void getCategoryStatistics(final OnCategoryStatisticsListener listener) {
        executorService.execute(() -> {
            List<ReminderDao.CategoryCount> categoryCounts = reminderDao.getCategoryCounts();
            if (listener != null) {
                listener.onCategoryStatistics(categoryCounts);
            }
        });
    }

    /**
     * Get monthly statistics
     */
    public void getMonthlyStatistics(final OnMonthlyStatisticsListener listener) {
        executorService.execute(() -> {
            List<ReminderDao.MonthlyCount> monthlyCounts = reminderDao.getMonthlyReminderCounts();
            if (listener != null) {
                listener.onMonthlyStatistics(monthlyCounts);
            }
        });
    }

    /**
     * Get recurring statistics
     */
    public void getRecurringStatistics(final OnRecurringStatisticsListener listener) {
        executorService.execute(() -> {
            int totalRecurring = reminderDao.getTotalRecurringCount();
            if (listener != null) {
                listener.onRecurringStatistics(totalRecurring);
            }
        });
    }

    /**
     * Get completion rate
     */
    public void getCompletionRate(final OnCompletionRateListener listener) {
        executorService.execute(() -> {
            int completed = reminderDao.getCompletedCount();
            int total = reminderDao.getTotalCount();
            float rate = total == 0 ? 0 : (completed * 100f / total);

            if (listener != null) {
                listener.onCompletionRate(rate);
            }
        });
    }

    // ===== CACHE MANAGEMENT =====

    /**
     * Clear the cache
     */
    public void clearCache() {
        if (performanceOptimizer != null) {
            performanceOptimizer.clearCaches();
            Log.d(TAG, "Cache cleared");
        }
    }

    // ===== INTERFACES =====

    public interface OnReminderInsertedListener {
        void onInserted(Reminder reminder);
    }

    public interface OnStatisticsListener {
        void onStatistics(int total, int completed, int active);
    }

    public interface OnCategoryStatisticsListener {
        void onCategoryStatistics(List<ReminderDao.CategoryCount> categoryCounts);
    }

    public interface OnMonthlyStatisticsListener {
        void onMonthlyStatistics(List<ReminderDao.MonthlyCount> monthlyCounts);
    }

    public interface OnRecurringStatisticsListener {
        void onRecurringStatistics(int totalRecurring);
    }

    public interface OnCompletionRateListener {
        void onCompletionRate(float rate);
    }
}