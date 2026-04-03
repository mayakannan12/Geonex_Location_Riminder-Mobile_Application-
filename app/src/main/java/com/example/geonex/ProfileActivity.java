package com.example.geonex;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.util.List;

public class ProfileActivity extends AppCompatActivity {

    private MaterialToolbar toolbar;
    private TextView tvThemeValue, tvTotalStats, tvActiveStats, tvCompletedStats, tvSuccessRateStats;
    private TextView tvRecurringStats;
    private CardView cardTheme, cardAbout, cardBackup, cardDeveloper;
    private SwitchMaterial switchNotifications, switchLocation;
    private ThemeManager themeManager;
    private ReminderRepository repository;
    private View cardBiometric;
    private SwitchMaterial switchBiometric;
    private SecurityManager securityManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize ThemeManager before setting content view
        themeManager = ThemeManager.getInstance(this);

        setContentView(R.layout.activity_profile);

        initViews();
        setupToolbar();
        setupClickListeners();
        updateThemeDisplay();

        // Initialize repository
        repository = ((GeonexApplication) getApplication()).getRepository();
        securityManager = new SecurityManager(this);

        // Load all statistics
        loadAllStatistics();

        // Setup developer options visibility
        setupDeveloperOptions();

        // Setup biometric option
        setupBiometricOption();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        tvThemeValue = findViewById(R.id.tvThemeValue);
        cardTheme = findViewById(R.id.cardTheme);
        cardAbout = findViewById(R.id.cardAbout);
        cardBackup = findViewById(R.id.cardBackup);
        cardDeveloper = findViewById(R.id.cardDeveloper);
        cardBiometric = findViewById(R.id.cardBiometric);

        // Statistics views
        tvTotalStats = findViewById(R.id.tvTotalStats);
        tvActiveStats = findViewById(R.id.tvActiveStats);
        tvCompletedStats = findViewById(R.id.tvCompletedStats);
        tvSuccessRateStats = findViewById(R.id.tvSuccessRateStats);
        tvRecurringStats = findViewById(R.id.tvRecurringStats);

        // Switches
        switchNotifications = findViewById(R.id.switchNotifications);
        switchLocation = findViewById(R.id.switchLocation);
        switchBiometric = findViewById(R.id.switchBiometric);

        LinearLayout categoryStatsContainer = findViewById(R.id.categoryStatsContainer);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle("Profile & Statistics");
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupClickListeners() {
        cardTheme.setOnClickListener(v -> showThemeDialog());
        cardAbout.setOnClickListener(v -> showAboutDialog());
        cardBackup.setOnClickListener(v -> {
            startActivity(new Intent(ProfileActivity.this, BackupActivity.class));
            overridePendingTransition(R.anim.slide_in_right, R.anim.fade_out);
        });
        cardDeveloper.setOnClickListener(v -> {
            startActivity(new Intent(ProfileActivity.this, DeveloperActivity.class));
            overridePendingTransition(R.anim.slide_in_right, R.anim.fade_out);
        });

        switchNotifications.setOnCheckedChangeListener((buttonView, isChecked) -> {
            String status = isChecked ? "enabled" : "disabled";
            Toast.makeText(this, "Notifications " + status, Toast.LENGTH_SHORT).show();
        });

        switchLocation.setOnCheckedChangeListener((buttonView, isChecked) -> {
            String status = isChecked ? "enabled" : "disabled";
            Toast.makeText(this, "High accuracy location " + status, Toast.LENGTH_SHORT).show();
        });
    }

    private void setupDeveloperOptions() {

        if (BuildConfig.DEBUG) {
            cardDeveloper.setVisibility(View.VISIBLE);
        } else {
            cardDeveloper.setVisibility(View.GONE);
        }
    }

    private void setupBiometricOption() {
        if (securityManager.isBiometricAvailable()) {
            cardBiometric.setVisibility(View.VISIBLE);

            boolean isEnabled = securityManager.getSecureBoolean("biometric_enabled", false);
            switchBiometric.setChecked(isEnabled);

            switchBiometric.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    enableBiometricAuth();
                } else {
                    disableBiometricAuth();
                }
            });
        } else {
            cardBiometric.setVisibility(View.GONE);
        }
    }

    private void enableBiometricAuth() {
        securityManager.authenticateWithBiometrics(this,
                "Enable Biometric Lock",
                "Authenticate to enable app lock",
                new SecurityManager.BiometricAuthCallback() {
                    @Override
                    public void onSuccess() {
                        securityManager.putSecureBoolean("biometric_enabled", true);
                        Toast.makeText(ProfileActivity.this,
                                "Biometric lock enabled", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError(int errorCode, String errorMessage) {
                        switchBiometric.setChecked(false);
                        Toast.makeText(ProfileActivity.this,
                                "Authentication failed: " + errorMessage,
                                Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onFailed() {
                        switchBiometric.setChecked(false);
                        Toast.makeText(ProfileActivity.this,
                                "Authentication failed", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void disableBiometricAuth() {
        securityManager.putSecureBoolean("biometric_enabled", false);
        Toast.makeText(this, "Biometric lock disabled", Toast.LENGTH_SHORT).show();
    }

    // ===== PHASE 2 - STEP 7: DARK MODE TOGGLE =====

    private void showThemeDialog() {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_theme, null);
        builder.setView(dialogView);

        RadioGroup radioGroup = dialogView.findViewById(R.id.themeRadioGroup);
        RadioButton radioLight = dialogView.findViewById(R.id.radioLight);
        RadioButton radioDark = dialogView.findViewById(R.id.radioDark);
        RadioButton radioSystem = dialogView.findViewById(R.id.radioSystem);

        MaterialButton btnCancel = dialogView.findViewById(R.id.btnCancel);
        MaterialButton btnApply = dialogView.findViewById(R.id.btnApply);

        // Set current selection
        int currentTheme = themeManager.getThemeMode();
        switch (currentTheme) {
            case ThemeManager.THEME_MODE_LIGHT -> radioLight.setChecked(true);
            case ThemeManager.THEME_MODE_DARK -> radioDark.setChecked(true);
            default -> radioSystem.setChecked(true);
        }

        AlertDialog dialog = builder.create();
        dialog.show();

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnApply.setOnClickListener(v -> {
            int selectedId = radioGroup.getCheckedRadioButtonId();
            int themeMode;

            if (selectedId == R.id.radioLight) {
                themeMode = ThemeManager.THEME_MODE_LIGHT;
            } else if (selectedId == R.id.radioDark) {
                themeMode = ThemeManager.THEME_MODE_DARK;
            } else {
                themeMode = ThemeManager.THEME_MODE_SYSTEM;
            }

            // Save and apply new theme
            themeManager.setThemeMode(themeMode);
            updateThemeDisplay();

            Toast.makeText(this, "Theme changed to " + themeManager.getThemeModeName(),
                    Toast.LENGTH_SHORT).show();

            // Recreate activity to apply theme changes
            recreate();

            dialog.dismiss();
        });
    }

    private void updateThemeDisplay() {
        tvThemeValue.setText(themeManager.getThemeModeName());
    }

    // ===== PHASE 2 - STEP 8: STATISTICS DASHBOARD =====

    private void loadAllStatistics() {
        loadBasicStatistics();
        loadCategoryStatistics();
        loadRecurringStatistics();
    }

    private void loadBasicStatistics() {
        repository.getStatistics(new ReminderRepository.OnStatisticsListener() {
            @Override
            public void onStatistics(int total, int completed, int active) {
                runOnUiThread(new Runnable() {
                    @SuppressLint("SetTextI18n")
                    @Override
                    public void run() {
                        if (tvTotalStats != null) {
                            tvTotalStats.setText(String.valueOf(total));
                        }
                        if (tvCompletedStats != null) {
                            tvCompletedStats.setText(String.valueOf(completed));
                        }
                        if (tvActiveStats != null) {
                            tvActiveStats.setText(String.valueOf(active));
                        }

                        int successRate = total == 0 ? 0 : (completed * 100 / total);
                        if (tvSuccessRateStats != null) {
                            tvSuccessRateStats.setText(successRate + "%");
                        }
                    }
                });
            }
        });
    }

    private void loadCategoryStatistics() {
        repository.getCategoryStatistics(new ReminderRepository.OnCategoryStatisticsListener() {
            @Override
            public void onCategoryStatistics(List<ReminderDao.CategoryCount> categoryCounts) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        // Update category stats - you'll need to add TextViews for each category
                        // This is a simplified version
                    }
                });
            }
        });
    }

    private void loadRecurringStatistics() {
        repository.getRecurringStatistics(new ReminderRepository.OnRecurringStatisticsListener() {
            @Override
            public void onRecurringStatistics(int totalRecurring) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (tvRecurringStats != null) {
                            tvRecurringStats.setText(String.valueOf(totalRecurring));
                        }
                    }
                });
            }
        });
    }

    private void showAboutDialog() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("About Geonex")
                .setMessage("""
                        Geonex v2.0

                        Smart Location Reminder App

                        ✓ Phase 1: MVP Complete
                        ✓ Phase 2: Enhanced Features
                        ✓ Phase 3: Background Automation
                        ✓ Phase 4: Final Polish & Security

                        © 2025 Geonex Team""")
                .setPositiveButton("OK", null)
                .show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_profile, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_refresh) {
            loadAllStatistics();
            Toast.makeText(this, "Statistics refreshed", Toast.LENGTH_SHORT).show();
            return true;
        } else if (item.getItemId() == R.id.action_edit_profile) {
            Toast.makeText(this, "Edit profile coming soon", Toast.LENGTH_SHORT).show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadAllStatistics(); // Refresh when returning to profile
    }
}