package com.example.geonex;

import android.content.Context;
import android.util.Log;
import com.example.geonex.DatabaseOptimizer;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

@Database(entities = {Reminder.class}, version = 2, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    private static final String TAG = "AppDatabase";
    private static volatile AppDatabase INSTANCE;
    private static final String DATABASE_NAME = "geonex_database";

    public abstract ReminderDao reminderDao();

    // Migration from version 1 to 2
    static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            Log.d(TAG, "Running migration from version 1 to 2");

            // Add new columns for recurring reminders
            database.execSQL("ALTER TABLE reminders ADD COLUMN is_recurring INTEGER NOT NULL DEFAULT 0");
            database.execSQL("ALTER TABLE reminders ADD COLUMN recurrence_rule TEXT DEFAULT 'never'");
            database.execSQL("ALTER TABLE reminders ADD COLUMN custom_interval INTEGER NOT NULL DEFAULT 0");
            database.execSQL("ALTER TABLE reminders ADD COLUMN custom_interval_unit TEXT DEFAULT ''");

            Log.d(TAG, "Migration completed successfully");
        }
    };

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    Log.d(TAG, "Creating new database instance");
                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    AppDatabase.class,
                                    DATABASE_NAME
                            )
                            .addMigrations(MIGRATION_1_2)
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}