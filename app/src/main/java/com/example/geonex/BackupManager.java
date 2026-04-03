package com.example.geonex;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.util.Log;

import androidx.documentfile.provider.DocumentFile;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BackupManager {

    private static final String TAG = "BackupManager";
    private static final String PREFS_NAME = "backup_prefs";
    private static final String KEY_LAST_BACKUP = "last_backup_time";
    private static final String BACKUP_FILE_NAME = "geonex_backup_%s.json";
    private static final String BACKUP_MIME_TYPE = "application/json";

    private final Context context;
    private final ExecutorService executorService;
    private final SharedPreferences prefs;
    private BackupListener backupListener;

    public interface BackupListener {
        void onBackupStart();
        void onBackupProgress(int progress, String message);
        void onBackupComplete(String filePath, int reminderCount);
        void onBackupError(String error);

        void onRestoreStart();
        void onRestoreProgress(int progress, String message);
        void onRestoreComplete(int reminderCount);
        void onRestoreError(String error);
    }

    public BackupManager(Context context) {
        this.context = context;
        this.executorService = Executors.newSingleThreadExecutor();
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public void setBackupListener(BackupListener listener) {
        this.backupListener = listener;
    }

    // ===== BACKUP METHODS =====

    /**
     * Backup reminders to internal storage
     */
    public void backupToInternalStorage() {
        executorService.execute(() -> {
            if (backupListener != null) {
                backupListener.onBackupStart();
            }

            try {
                // Get all reminders
                ReminderRepository repository = ((GeonexApplication) context).getRepository();
                List<Reminder> allReminders = repository.getAllReminders().getValue();

                if (allReminders == null || allReminders.isEmpty()) {
                    if (backupListener != null) {
                        backupListener.onBackupError("No reminders to backup");
                    }
                    return;
                }

                // Create backups directory
                File backupDir = new File(context.getFilesDir(), "backups");
                if (!backupDir.exists()) {
                    backupDir.mkdirs();
                }

                // Create backup file with timestamp
                String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                        .format(new Date());
                String fileName = String.format(BACKUP_FILE_NAME, timestamp);
                File backupFile = new File(backupDir, fileName);

                // Convert reminders to JSON
                JSONArray jsonArray = new JSONArray();
                int total = allReminders.size();

                for (int i = 0; i < total; i++) {
                    Reminder reminder = allReminders.get(i);
                    jsonArray.put(reminderToJson(reminder));

                    if (backupListener != null && i % 10 == 0) {
                        int progress = (i * 100) / total;
                        backupListener.onBackupProgress(progress,
                                "Processing reminder " + (i + 1) + " of " + total);
                    }
                }

                JSONObject backupObject = new JSONObject();
                backupObject.put("version", 1);
                backupObject.put("timestamp", System.currentTimeMillis());
                backupObject.put("count", total);
                backupObject.put("reminders", jsonArray);
                backupObject.put("app_name", "Geonex");
                backupObject.put("export_date", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss",
                        Locale.getDefault()).format(new Date()));

                // Write to file
                try (FileOutputStream fos = new FileOutputStream(backupFile);
                     OutputStreamWriter osw = new OutputStreamWriter(fos, StandardCharsets.UTF_8);
                     BufferedWriter writer = new BufferedWriter(osw)) {

                    writer.write(backupObject.toString(2)); // Pretty print with 2 spaces
                    writer.flush();
                }

                // Save last backup time
                prefs.edit().putLong(KEY_LAST_BACKUP, System.currentTimeMillis()).apply();

                if (backupListener != null) {
                    backupListener.onBackupComplete(backupFile.getAbsolutePath(), total);
                }

                Log.d(TAG, "Backup completed: " + backupFile.getAbsolutePath() +
                        " with " + total + " reminders");

            } catch (Exception e) {
                Log.e(TAG, "Backup error: " + e.getMessage(), e);
                if (backupListener != null) {
                    backupListener.onBackupError("Backup failed: " + e.getMessage());
                }
            }
        });
    }

    /**
     * Backup reminders to external storage using Storage Access Framework
     */
    public void backupToExternalStorage(Uri targetUri) {
        executorService.execute(() -> {
            if (backupListener != null) {
                backupListener.onBackupStart();
            }

            try {
                // Get all reminders
                ReminderRepository repository = ((GeonexApplication) context).getRepository();
                List<Reminder> allReminders = repository.getAllReminders().getValue();

                if (allReminders == null || allReminders.isEmpty()) {
                    if (backupListener != null) {
                        backupListener.onBackupError("No reminders to backup");
                    }
                    return;
                }

                // Convert reminders to JSON
                JSONArray jsonArray = new JSONArray();
                int total = allReminders.size();

                for (int i = 0; i < total; i++) {
                    Reminder reminder = allReminders.get(i);
                    jsonArray.put(reminderToJson(reminder));

                    if (backupListener != null && i % 10 == 0) {
                        int progress = (i * 100) / total;
                        backupListener.onBackupProgress(progress,
                                "Processing reminder " + (i + 1) + " of " + total);
                    }
                }

                JSONObject backupObject = new JSONObject();
                backupObject.put("version", 1);
                backupObject.put("timestamp", System.currentTimeMillis());
                backupObject.put("count", total);
                backupObject.put("reminders", jsonArray);
                backupObject.put("app_name", "Geonex");
                backupObject.put("export_date", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss",
                        Locale.getDefault()).format(new Date()));

                // Write to external storage
                try (OutputStream outputStream = context.getContentResolver().openOutputStream(targetUri);
                     OutputStreamWriter osw = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8);
                     BufferedWriter writer = new BufferedWriter(osw)) {

                    writer.write(backupObject.toString(2));
                    writer.flush();
                }

                // Save last backup time
                prefs.edit().putLong(KEY_LAST_BACKUP, System.currentTimeMillis()).apply();

                if (backupListener != null) {
                    backupListener.onBackupComplete(targetUri.toString(), total);
                }

                Log.d(TAG, "Backup completed to external storage with " + total + " reminders");

            } catch (Exception e) {
                Log.e(TAG, "Backup error: " + e.getMessage(), e);
                if (backupListener != null) {
                    backupListener.onBackupError("Backup failed: " + e.getMessage());
                }
            }
        });
    }

    // ===== RESTORE METHODS =====

    /**
     * Restore reminders from internal storage file
     */
    public void restoreFromInternalStorage(String filePath) {
        executorService.execute(() -> {
            if (backupListener != null) {
                backupListener.onRestoreStart();
            }

            File backupFile = new File(filePath);
            if (!backupFile.exists()) {
                if (backupListener != null) {
                    backupListener.onRestoreError("Backup file not found");
                }
                return;
            }

            try {
                // Read JSON file
                StringBuilder jsonString = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new FileReader(backupFile))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        jsonString.append(line);
                    }
                }

                restoreFromJson(jsonString.toString());

            } catch (Exception e) {
                Log.e(TAG, "Restore error: " + e.getMessage(), e);
                if (backupListener != null) {
                    backupListener.onRestoreError("Restore failed: " + e.getMessage());
                }
            }
        });
    }

    /**
     * Restore reminders from external storage using Storage Access Framework
     */
    public void restoreFromExternalStorage(Uri sourceUri) {
        executorService.execute(() -> {
            if (backupListener != null) {
                backupListener.onRestoreStart();
            }

            try {
                // Read JSON from URI
                StringBuilder jsonString = new StringBuilder();
                try (InputStream inputStream = context.getContentResolver().openInputStream(sourceUri);
                     BufferedReader reader = new BufferedReader(
                             new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {

                    String line;
                    while ((line = reader.readLine()) != null) {
                        jsonString.append(line);
                    }
                }

                restoreFromJson(jsonString.toString());

            } catch (Exception e) {
                Log.e(TAG, "Restore error: " + e.getMessage(), e);
                if (backupListener != null) {
                    backupListener.onRestoreError("Restore failed: " + e.getMessage());
                }
            }
        });
    }

    /**
     * Restore reminders from JSON string
     */
    private void restoreFromJson(String jsonString) throws JSONException {
        JSONObject backupObject = new JSONObject(jsonString);
        int version = backupObject.getInt("version");
        long timestamp = backupObject.getLong("timestamp");
        int count = backupObject.getInt("count");
        JSONArray jsonArray = backupObject.getJSONArray("reminders");

        if (backupListener != null) {
            backupListener.onRestoreProgress(10, "Validating backup file...");
        }

        // Validate backup
        if (version != 1) {
            throw new JSONException("Unsupported backup version: " + version);
        }

        // Parse reminders
        List<Reminder> restoredReminders = new ArrayList<>();
        int total = jsonArray.length();

        for (int i = 0; i < total; i++) {
            JSONObject reminderJson = jsonArray.getJSONObject(i);
            Reminder reminder = jsonToReminder(reminderJson);
            restoredReminders.add(reminder);

            if (backupListener != null && i % 10 == 0) {
                int progress = 10 + (i * 80 / total); // 10% to 90%
                backupListener.onRestoreProgress(progress,
                        "Processing reminder " + (i + 1) + " of " + total);
            }
        }

        if (backupListener != null) {
            backupListener.onRestoreProgress(90, "Saving to database...");
        }

        // Save to database
        ReminderRepository repository = ((GeonexApplication) context).getRepository();
        for (Reminder reminder : restoredReminders) {
            repository.insert(reminder);
        }

        if (backupListener != null) {
            backupListener.onRestoreProgress(100, "Restore complete!");
            backupListener.onRestoreComplete(restoredReminders.size());
        }

        Log.d(TAG, "Restore completed: " + restoredReminders.size() + " reminders restored");
    }

    // ===== JSON CONVERSION =====

    /**
     * Convert Reminder to JSON object
     */
    private JSONObject reminderToJson(Reminder reminder) throws JSONException {
        JSONObject json = new JSONObject();
        json.put("id", reminder.getId());
        json.put("title", reminder.getTitle());
        json.put("location_name", reminder.getLocationName());
        json.put("latitude", reminder.getLatitude());
        json.put("longitude", reminder.getLongitude());
        json.put("radius", reminder.getRadius());
        json.put("category", reminder.getCategory());
        json.put("is_completed", reminder.isCompleted());
        json.put("created_at", reminder.getCreatedAt());
        json.put("is_recurring", reminder.isRecurring());
        json.put("recurrence_rule", reminder.getRecurrenceRule() != null ?
                reminder.getRecurrenceRule() : "");
        json.put("custom_interval", reminder.getCustomInterval());
        json.put("custom_interval_unit", reminder.getCustomIntervalUnit() != null ?
                reminder.getCustomIntervalUnit() : "");
        return json;
    }

    /**
     * Convert JSON object to Reminder
     */
    private Reminder jsonToReminder(JSONObject json) throws JSONException {
        Reminder reminder = new Reminder();
        reminder.setId(json.getInt("id"));
        reminder.setTitle(json.getString("title"));
        reminder.setLocationName(json.getString("location_name"));
        reminder.setLatitude(json.getDouble("latitude"));
        reminder.setLongitude(json.getDouble("longitude"));
        reminder.setRadius((float) json.getDouble("radius"));
        reminder.setCategory(json.getString("category"));
        reminder.setCompleted(json.getBoolean("is_completed"));
        reminder.setCreatedAt(json.getLong("created_at"));
        reminder.setRecurring(json.getBoolean("is_recurring"));
        reminder.setRecurrenceRule(json.optString("recurrence_rule", ""));
        reminder.setCustomInterval(json.optInt("custom_interval", 0));
        reminder.setCustomIntervalUnit(json.optString("custom_interval_unit", ""));
        return reminder;
    }

    // ===== UTILITY METHODS =====

    /**
     * Get list of backup files from internal storage
     */
    public List<BackupFileInfo> getBackupFiles() {
        List<BackupFileInfo> backupFiles = new ArrayList<>();
        File backupDir = new File(context.getFilesDir(), "backups");

        if (backupDir.exists()) {
            File[] files = backupDir.listFiles((dir, name) -> name.startsWith("geonex_backup_"));
            if (files != null) {
                for (File file : files) {
                    backupFiles.add(new BackupFileInfo(
                            file.getName(),
                            file.getAbsolutePath(),
                            file.length(),
                            file.lastModified()
                    ));
                }
            }
        }

        // Sort by date (newest first)
        backupFiles.sort((f1, f2) -> Long.compare(f2.lastModified, f1.lastModified));

        return backupFiles;
    }

    /**
     * Get last backup time
     */
    public long getLastBackupTime() {
        return prefs.getLong(KEY_LAST_BACKUP, 0);
    }

    /**
     * Delete backup file
     */
    public boolean deleteBackupFile(String filePath) {
        File file = new File(filePath);
        return file.exists() && file.delete();
    }

    /**
     * Backup file info class
     */
    public static class BackupFileInfo {
        public final String name;
        public final String path;
        public final long size;
        public final long lastModified;

        public BackupFileInfo(String name, String path, long size, long lastModified) {
            this.name = name;
            this.path = path;
            this.size = size;
            this.lastModified = lastModified;
        }

        public String getFormattedSize() {
            if (size < 1024) return size + " B";
            if (size < 1024 * 1024) return String.format(Locale.getDefault(), "%.1f KB", size / 1024.0);
            return String.format(Locale.getDefault(), "%.1f MB", size / (1024.0 * 1024.0));
        }

        public String getFormattedDate() {
            SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault());
            return sdf.format(new Date(lastModified));
        }
    }
}