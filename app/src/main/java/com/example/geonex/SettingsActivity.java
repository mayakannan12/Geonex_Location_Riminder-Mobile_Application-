package com.example.geonex;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.switchmaterial.SwitchMaterial;

public class SettingsActivity extends AppCompatActivity {

    private MaterialToolbar toolbar;
    private SwitchMaterial switchDarkMode;
    private SwitchMaterial switchNotifications;
    private SwitchMaterial switchSound;
    private SwitchMaterial switchVibration;
    private SwitchMaterial switchAutoBackup;
    private TextView tvLanguage;
    private TextView tvBackupStatus;
    private MaterialButton btnBackupNow;
    private MaterialButton btnRestore;
    private MaterialButton btnClearData;
    private TextView tvVersion;
    private TextView tvStorageUsage;

    private ThemeManager themeManager;
    private SharedPreferences prefs;
    private BackupManager backupManager;
    private ReminderRepository repository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        initViews();
        setupToolbar();
        loadSettings();
        setupClickListeners();

        themeManager = ThemeManager.getInstance(this);
        backupManager = new BackupManager(this);
        repository = ((GeonexApplication) getApplication()).getRepository();
        prefs = getSharedPreferences("app_settings", MODE_PRIVATE);

        updateStorageInfo();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        switchDarkMode = findViewById(R.id.switchDarkMode);
        switchNotifications = findViewById(R.id.switchNotifications);
        switchSound = findViewById(R.id.switchSound);
        switchVibration = findViewById(R.id.switchVibration);
        switchAutoBackup = findViewById(R.id.switchAutoBackup);
        tvLanguage = findViewById(R.id.tvLanguage);
        tvBackupStatus = findViewById(R.id.tvBackupStatus);
        btnBackupNow = findViewById(R.id.btnBackupNow);
        btnRestore = findViewById(R.id.btnRestore);
        btnClearData = findViewById(R.id.btnClearData);
        tvVersion = findViewById(R.id.tvVersion);
        tvStorageUsage = findViewById(R.id.tvStorageUsage);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle("Settings");
        }
        toolbar.setNavigationOnClickListener(v -> {
            finish();
            overridePendingTransition(R.anim.fade_in, R.anim.slide_out_right);
        });
    }

    private void loadSettings() {
        // Load saved preferences
        boolean isDarkMode = prefs.getBoolean("dark_mode", false);
        boolean notifications = prefs.getBoolean("notifications", true);
        boolean sound = prefs.getBoolean("sound", true);
        boolean vibration = prefs.getBoolean("vibration", true);
        boolean autoBackup = prefs.getBoolean("auto_backup", false);
        String language = prefs.getString("language", "English");

        switchDarkMode.setChecked(isDarkMode);
        switchNotifications.setChecked(notifications);
        switchSound.setChecked(sound);
        switchVibration.setChecked(vibration);
        switchAutoBackup.setChecked(autoBackup);
        tvLanguage.setText(language);

        // Update backup status
        updateBackupStatus();

        // Set version
        tvVersion.setText("Version 1.0.0");
    }

    private void setupClickListeners() {
        switchDarkMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean("dark_mode", isChecked).apply();
            if (isChecked) {
                themeManager.setThemeMode(ThemeManager.THEME_MODE_DARK);
            } else {
                themeManager.setThemeMode(ThemeManager.THEME_MODE_LIGHT);
            }
            Toast.makeText(this, "Theme changed. Restart app to apply.", Toast.LENGTH_SHORT).show();
        });

        switchNotifications.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean("notifications", isChecked).apply();
            Toast.makeText(this, isChecked ? "Notifications enabled" : "Notifications disabled",
                    Toast.LENGTH_SHORT).show();
        });

        switchSound.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean("sound", isChecked).apply();
        });

        switchVibration.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean("vibration", isChecked).apply();
        });

        switchAutoBackup.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean("auto_backup", isChecked).apply();
            Toast.makeText(this, isChecked ? "Auto backup enabled" : "Auto backup disabled",
                    Toast.LENGTH_SHORT).show();
        });

        tvLanguage.setOnClickListener(v -> showLanguageDialog());

        btnBackupNow.setOnClickListener(v -> {
            backupManager.backupToInternalStorage();
            Toast.makeText(this, "Backup started...", Toast.LENGTH_SHORT).show();
        });

        btnRestore.setOnClickListener(v -> {
            showRestoreDialog();
        });

        btnClearData.setOnClickListener(v -> {
            showClearDataDialog();
        });
    }

    private void showLanguageDialog() {
        String[] languages = {"English", "தமிழ் (Tamil)", "हिन्दी (Hindi)", "中文 (Chinese)"};
        int currentLang = 0;
        String current = tvLanguage.getText().toString();

        for (int i = 0; i < languages.length; i++) {
            if (languages[i].equals(current)) {
                currentLang = i;
                break;
            }
        }

        new MaterialAlertDialogBuilder(this)
                .setTitle("Select Language")
                .setSingleChoiceItems(languages, currentLang, (dialog, which) -> {
                    String selected = languages[which];
                    tvLanguage.setText(selected);
                    prefs.edit().putString("language", selected).apply();
                    dialog.dismiss();
                    Toast.makeText(this, "Language changed. Restart app to apply.",
                            Toast.LENGTH_SHORT).show();
                })
                .show();
    }

    private void showRestoreDialog() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Restore Data")
                .setMessage("Choose backup source:")
                .setPositiveButton("Internal Storage", (dialog, which) -> {
                    Toast.makeText(this, "Select backup file...", Toast.LENGTH_SHORT).show();
                    // Implement file picker
                })
                .setNegativeButton("Cloud Backup", (dialog, which) -> {
                    Toast.makeText(this, "Cloud restore coming soon", Toast.LENGTH_SHORT).show();
                })
                .setNeutralButton("Cancel", null)
                .show();
    }

    private void showClearDataDialog() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Clear All Data")
                .setMessage("Are you sure? This will delete ALL reminders and settings. This action cannot be undone!")
                .setPositiveButton("DELETE", (dialog, which) -> {
                    // Clear database
                    new Thread(() -> {
                        List<Reminder> all = repository.getAllRemindersSync();
                        for (Reminder r : all) {
                            repository.delete(r);
                        }
                        runOnUiThread(() -> {
                            Toast.makeText(this, "All data cleared", Toast.LENGTH_LONG).show();
                            updateStorageInfo();
                        });
                    }).start();
                })
                .setNegativeButton("Cancel", null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    private void updateBackupStatus() {
        long lastBackup = backupManager.getLastBackupTime();
        if (lastBackup > 0) {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat(
                    "dd MMM yyyy, HH:mm", java.util.Locale.getDefault());
            tvBackupStatus.setText("Last backup: " + sdf.format(new java.util.Date(lastBackup)));
        } else {
            tvBackupStatus.setText("No backup yet");
        }
    }

    private void updateStorageInfo() {
        new Thread(() -> {
            int count = repository.getTotalCount();
            long size = getDatabaseSize();
            runOnUiThread(() -> {
                tvStorageUsage.setText(count + " reminders · " + formatSize(size));
            });
        }).start();
    }

    private long getDatabaseSize() {
        return getDatabasePath("geonex_database").length();
    }

    private String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        overridePendingTransition(R.anim.fade_in, R.anim.slide_out_right);
        return true;
    }
}