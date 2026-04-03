package com.example.geonex;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Update;
import androidx.room.Delete;
import androidx.room.Query;
import androidx.room.Ignore; // Add this import
import androidx.lifecycle.LiveData;

import java.util.List;

@Dao
public interface ReminderDao {

    // ===== BASIC CRUD OPERATIONS =====

    @Insert
    long insert(Reminder reminder);

    @Update
    void update(Reminder reminder);

    @Delete
    void delete(Reminder reminder);

    // ===== LIVE DATA QUERIES =====

    @Query("SELECT * FROM reminders ORDER BY created_at DESC")
    LiveData<List<Reminder>> getAllReminders();

    @Query("SELECT * FROM reminders WHERE is_completed = 0 ORDER BY created_at DESC")
    LiveData<List<Reminder>> getActiveReminders();

    @Query("SELECT * FROM reminders WHERE is_completed = 1 ORDER BY created_at DESC")
    LiveData<List<Reminder>> getCompletedReminders();

    @Query("SELECT * FROM reminders WHERE category = :category ORDER BY created_at DESC")
    LiveData<List<Reminder>> getRemindersByCategory(String category);

    @Query("SELECT * FROM reminders WHERE title LIKE '%' || :query || '%' OR location_name LIKE '%' || :query || '%' ORDER BY created_at DESC")
    LiveData<List<Reminder>> searchReminders(String query);

    // ===== SYNCHRONOUS QUERIES (for background threads) =====

    @Query("SELECT * FROM reminders WHERE id = :id")
    Reminder getReminderById(int id);

    @Query("SELECT * FROM reminders WHERE is_completed = 0 AND is_recurring = 1")
    List<Reminder> getActiveRecurringReminders();

    @Query("SELECT * FROM reminders WHERE is_recurring = 1 AND recurrence_rule = :rule")
    List<Reminder> getRemindersByRecurrenceRule(String rule);

    @Query("SELECT * FROM reminders")
    List<Reminder> getAllRemindersSync();

    // ===== COUNT QUERIES =====

    @Query("SELECT COUNT(*) FROM reminders")
    int getTotalCount();

    @Query("SELECT COUNT(*) FROM reminders WHERE is_completed = 1")
    int getCompletedCount();

    @Query("SELECT COUNT(*) FROM reminders WHERE is_completed = 0")
    int getActiveCount();

    @Query("SELECT COUNT(*) FROM reminders WHERE is_recurring = 1")
    int getTotalRecurringCount();

    // ===== UPDATE OPERATIONS =====

    @Query("UPDATE reminders SET is_completed = 1 WHERE id = :id")
    void markAsCompleted(int id);

    // ===== DELETE OPERATIONS =====

    @Query("DELETE FROM reminders WHERE is_completed = 1")
    void deleteCompletedReminders();

    // ===== STATISTICS QUERIES =====

    @Query("SELECT category, COUNT(*) as count FROM reminders GROUP BY category")
    List<CategoryCount> getCategoryCounts();

    @Query("SELECT strftime('%Y-%m', datetime(created_at/1000, 'unixepoch')) as month, COUNT(*) as count FROM reminders GROUP BY month ORDER BY month DESC")
    List<MonthlyCount> getMonthlyReminderCounts();

    // ===== BATCH OPERATIONS =====

    @Insert
    void insertAll(List<Reminder> reminders);

    // ===== INNER CLASSES =====

    class CategoryCount {
        public String category;
        public int count;

        // No-arg constructor (used by Room)
        public CategoryCount() {}

        @Ignore
        public CategoryCount(String category, int count) {
            this.category = category;
            this.count = count;
        }
    }

    class MonthlyCount {
        public String month;
        public int count;

        // No-arg constructor (used by Room)
        public MonthlyCount() {}

        @Ignore
        public MonthlyCount(String month, int count) {
            this.month = month;
            this.count = count;
        }
    }
}