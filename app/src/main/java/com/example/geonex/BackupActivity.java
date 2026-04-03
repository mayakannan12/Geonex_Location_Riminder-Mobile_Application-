package com.example.geonex;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.snackbar.Snackbar;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class BackupActivity extends AppCompatActivity implements BackupManager.BackupListener {

    private MaterialToolbar toolbar;
    private MaterialButton btnBackup, btnRestore, btnAutoBackup;
    private TextView tvLastBackup, tvBackupInfo, tvProgressMessage;
    private LinearProgressIndicator progressBar;
    private LinearLayout progressContainer;
    private LinearLayout backupFilesContainer;
    private RecyclerView recyclerView;
    private BackupFilesAdapter adapter;

    private BackupManager backupManager;
    private ReminderRepository repository;
    private boolean isInitialized = false;

    private final ActivityResultLauncher<String[]> backupPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(),
            result -> {
                Boolean storageGranted = result.getOrDefault(android.Manifest.permission.WRITE_EXTERNAL_STORAGE, false);
                if (Boolean.TRUE.equals(storageGranted)) {
                    createBackupFile();
                } else {
                    Toast.makeText(this, "Storage permission required for backup", Toast.LENGTH_SHORT).show();
                }
            }
    );

    private final ActivityResultLauncher<String> createDocumentLauncher = registerForActivityResult(
            new ActivityResultContracts.CreateDocument("application/json"),
            uri -> {
                if (uri != null) {
                    if (backupManager != null) {
                        backupManager.backupToExternalStorage(uri);
                    } else {
                        Toast.makeText(this, "Backup manager not initialized", Toast.LENGTH_SHORT).show();
                    }
                }
            }
    );

    private final ActivityResultLauncher<String[]> openDocumentLauncher = registerForActivityResult(
            new ActivityResultContracts.OpenDocument(),
            uri -> {
                if (uri != null) {
                    showRestoreConfirmation(uri);
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            setContentView(R.layout.activity_backup);

            initViews();
            setupToolbar();

            backupManager = new BackupManager(this);
            backupManager.setBackupListener(this);
            repository = ((GeonexApplication) getApplication()).getRepository();

            if (repository != null) {
                isInitialized = true;
            }

            setupClickListeners();
            updateLastBackupInfo();
            loadBackupFiles();

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error initializing backup: " + e.getMessage(), Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void initViews() {
        try {
            toolbar = findViewById(R.id.toolbar);
            btnBackup = findViewById(R.id.btnBackup);
            btnRestore = findViewById(R.id.btnRestore);
            btnAutoBackup = findViewById(R.id.btnAutoBackup);
            tvLastBackup = findViewById(R.id.tvLastBackup);
            tvBackupInfo = findViewById(R.id.tvBackupInfo);
            tvProgressMessage = findViewById(R.id.tvProgressMessage);
            progressBar = findViewById(R.id.progressBar);
            progressContainer = findViewById(R.id.progressContainer);
            backupFilesContainer = findViewById(R.id.backupFilesContainer);
            recyclerView = findViewById(R.id.recyclerView);

            // Safety checks
            if (backupFilesContainer == null) {
                backupFilesContainer = new LinearLayout(this);
                backupFilesContainer.setVisibility(View.GONE);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setupToolbar() {
        if (toolbar == null) return;

        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle("Backup & Restore");
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupClickListeners() {
        if (btnBackup != null) {
            btnBackup.setOnClickListener(v -> showBackupOptions());
        }
        if (btnRestore != null) {
            btnRestore.setOnClickListener(v -> showRestoreOptions());
        }
        if (btnAutoBackup != null) {
            btnAutoBackup.setOnClickListener(v -> showAutoBackupDialog());
        }
    }

    private void updateLastBackupInfo() {
        if (tvLastBackup == null || backupManager == null) return;

        long lastBackupTime = backupManager.getLastBackupTime();
        if (lastBackupTime > 0) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault());
            String dateStr = sdf.format(new Date(lastBackupTime));
            tvLastBackup.setText("Last backup: " + dateStr);
        } else {
            tvLastBackup.setText("No backup yet");
        }
    }

    private void loadBackupFiles() {
        if (backupManager == null || backupFilesContainer == null || tvBackupInfo == null || recyclerView == null) {
            return;
        }

        try {
            List<BackupManager.BackupFileInfo> backupFiles = backupManager.getBackupFiles();

            if (backupFiles == null || backupFiles.isEmpty()) {
                backupFilesContainer.setVisibility(View.GONE);
                tvBackupInfo.setVisibility(View.VISIBLE);
                tvBackupInfo.setText("No backup files found");
            } else {
                backupFilesContainer.setVisibility(View.VISIBLE);
                tvBackupInfo.setVisibility(View.GONE);

                recyclerView.setLayoutManager(new LinearLayoutManager(this));
                adapter = new BackupFilesAdapter(backupFiles, new BackupFilesAdapter.OnBackupFileListener() {
                    @Override
                    public void onFileClick(BackupManager.BackupFileInfo file) {
                        showRestoreConfirmation(file);
                    }

                    @Override
                    public void onFileDelete(BackupManager.BackupFileInfo file) {
                        showDeleteConfirmation(file);
                    }

                    @Override
                    public void onFileShare(BackupManager.BackupFileInfo file) {
                        shareBackupFile(file);
                    }
                });
                recyclerView.setAdapter(adapter);
            }
        } catch (Exception e) {
            e.printStackTrace();
            tvBackupInfo.setVisibility(View.VISIBLE);
            tvBackupInfo.setText("Error loading backup files");
        }
    }

    private void showBackupOptions() {
        String[] options = {"Backup to Internal Storage", "Backup to External Storage", "Cancel"};

        new MaterialAlertDialogBuilder(this)
                .setTitle("Backup Options")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        if (backupManager != null) {
                            backupManager.backupToInternalStorage();
                        }
                    } else if (which == 1) {
                        createBackupFile();
                    }
                })
                .show();
    }

    private void showRestoreOptions() {
        String[] options = {"Restore from Internal Storage", "Restore from External Storage", "Cancel"};

        new MaterialAlertDialogBuilder(this)
                .setTitle("Restore Options")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        showInternalBackupFiles();
                    } else if (which == 1) {
                        openDocumentLauncher.launch(new String[]{"application/json"});
                    }
                })
                .show();
    }

    private void showInternalBackupFiles() {
        if (backupManager == null) return;

        List<BackupManager.BackupFileInfo> backupFiles = backupManager.getBackupFiles();

        if (backupFiles == null || backupFiles.isEmpty()) {
            Toast.makeText(this, "No backup files found", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] fileNames = new String[backupFiles.size()];
        for (int i = 0; i < backupFiles.size(); i++) {
            fileNames[i] = backupFiles.get(i).name + " (" + backupFiles.get(i).getFormattedDate() + ")";
        }

        new MaterialAlertDialogBuilder(this)
                .setTitle("Select Backup File")
                .setItems(fileNames, (dialog, which) -> {
                    BackupManager.BackupFileInfo selectedFile = backupFiles.get(which);
                    showRestoreConfirmation(selectedFile);
                })
                .show();
    }

    private void showRestoreConfirmation(BackupManager.BackupFileInfo file) {
        if (file == null) return;

        new MaterialAlertDialogBuilder(this)
                .setTitle("Restore Backup")
                .setMessage("Are you sure you want to restore from:\n\n" +
                        "File: " + file.name + "\n" +
                        "Date: " + file.getFormattedDate() + "\n" +
                        "Size: " + file.getFormattedSize() + "\n\n" +
                        "Current reminders will be merged with backup.")
                .setPositiveButton("Restore", (dialog, which) -> {
                    if (backupManager != null) {
                        backupManager.restoreFromInternalStorage(file.path);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showRestoreConfirmation(Uri uri) {
        if (uri == null) return;

        new MaterialAlertDialogBuilder(this)
                .setTitle("Restore Backup")
                .setMessage("Are you sure you want to restore from the selected file?\n\n" +
                        "Current reminders will be merged with backup.")
                .setPositiveButton("Restore", (dialog, which) -> {
                    if (backupManager != null) {
                        backupManager.restoreFromExternalStorage(uri);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showDeleteConfirmation(BackupManager.BackupFileInfo file) {
        if (file == null || backupManager == null) return;

        new MaterialAlertDialogBuilder(this)
                .setTitle("Delete Backup")
                .setMessage("Delete backup file:\n" + file.name + "?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    if (backupManager.deleteBackupFile(file.path)) {
                        Toast.makeText(this, "Backup deleted", Toast.LENGTH_SHORT).show();
                        loadBackupFiles();
                    } else {
                        Toast.makeText(this, "Failed to delete backup", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void shareBackupFile(BackupManager.BackupFileInfo file) {
        Toast.makeText(this, "Share feature coming soon", Toast.LENGTH_SHORT).show();
    }

    private void showAutoBackupDialog() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Auto Backup")
                .setMessage("Configure automatic backup settings")
                .setPositiveButton("OK", null)
                .show();
    }

    private void createBackupFile() {
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                .format(new Date());
        String filename = "geonex_backup_" + timestamp + ".json";
        createDocumentLauncher.launch(filename);
    }

    // ===== BACKUP LISTENER IMPLEMENTATION =====

    @Override
    public void onBackupStart() {
        runOnUiThread(() -> {
            if (progressContainer != null) {
                progressContainer.setVisibility(View.VISIBLE);
            }
            if (progressBar != null) {
                progressBar.setProgress(0);
            }
            if (tvProgressMessage != null) {
                tvProgressMessage.setText("Starting backup...");
            }
            if (btnBackup != null) {
                btnBackup.setEnabled(false);
            }
            if (btnRestore != null) {
                btnRestore.setEnabled(false);
            }
        });
    }

    @Override
    public void onBackupProgress(int progress, String message) {
        runOnUiThread(() -> {
            if (progressBar != null) {
                progressBar.setProgress(progress);
            }
            if (tvProgressMessage != null) {
                tvProgressMessage.setText(message);
            }
        });
    }

    @Override
    public void onBackupComplete(String filePath, int reminderCount) {
        runOnUiThread(() -> {
            if (progressContainer != null) {
                progressContainer.setVisibility(View.GONE);
            }
            if (btnBackup != null) {
                btnBackup.setEnabled(true);
            }
            if (btnRestore != null) {
                btnRestore.setEnabled(true);
            }
            updateLastBackupInfo();
            loadBackupFiles();

            if (toolbar != null) {
                Snackbar.make(toolbar,
                        "Backup complete! " + reminderCount + " reminders saved",
                        Snackbar.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void onBackupError(String error) {
        runOnUiThread(() -> {
            if (progressContainer != null) {
                progressContainer.setVisibility(View.GONE);
            }
            if (btnBackup != null) {
                btnBackup.setEnabled(true);
            }
            if (btnRestore != null) {
                btnRestore.setEnabled(true);
            }

            new MaterialAlertDialogBuilder(this)
                    .setTitle("Backup Failed")
                    .setMessage(error)
                    .setPositiveButton("OK", null)
                    .show();
        });
    }

    @Override
    public void onRestoreStart() {
        runOnUiThread(() -> {
            if (progressContainer != null) {
                progressContainer.setVisibility(View.VISIBLE);
            }
            if (progressBar != null) {
                progressBar.setProgress(0);
            }
            if (tvProgressMessage != null) {
                tvProgressMessage.setText("Starting restore...");
            }
            if (btnBackup != null) {
                btnBackup.setEnabled(false);
            }
            if (btnRestore != null) {
                btnRestore.setEnabled(false);
            }
        });
    }

    @Override
    public void onRestoreProgress(int progress, String message) {
        runOnUiThread(() -> {
            if (progressBar != null) {
                progressBar.setProgress(progress);
            }
            if (tvProgressMessage != null) {
                tvProgressMessage.setText(message);
            }
        });
    }

    @Override
    public void onRestoreComplete(int reminderCount) {
        runOnUiThread(() -> {
            if (progressContainer != null) {
                progressContainer.setVisibility(View.GONE);
            }
            if (btnBackup != null) {
                btnBackup.setEnabled(true);
            }
            if (btnRestore != null) {
                btnRestore.setEnabled(true);
            }

            if (toolbar != null) {
                Snackbar.make(toolbar,
                        "Restore complete! " + reminderCount + " reminders restored",
                        Snackbar.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void onRestoreError(String error) {
        runOnUiThread(() -> {
            if (progressContainer != null) {
                progressContainer.setVisibility(View.GONE);
            }
            if (btnBackup != null) {
                btnBackup.setEnabled(true);
            }
            if (btnRestore != null) {
                btnRestore.setEnabled(true);
            }

            new MaterialAlertDialogBuilder(this)
                    .setTitle("Restore Failed")
                    .setMessage(error)
                    .setPositiveButton("OK", null)
                    .show();
        });
    }

    // ===== ADAPTER FOR BACKUP FILES =====

    private static class BackupFilesAdapter extends RecyclerView.Adapter<BackupFilesAdapter.ViewHolder> {

        private final List<BackupManager.BackupFileInfo> files;
        private final OnBackupFileListener listener;

        interface OnBackupFileListener {
            void onFileClick(BackupManager.BackupFileInfo file);
            void onFileDelete(BackupManager.BackupFileInfo file);
            void onFileShare(BackupManager.BackupFileInfo file);
        }

        BackupFilesAdapter(List<BackupManager.BackupFileInfo> files, OnBackupFileListener listener) {
            this.files = files != null ? files : new ArrayList<>();
            this.listener = listener;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_backup_file, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            if (position >= 0 && position < files.size()) {
                BackupManager.BackupFileInfo file = files.get(position);

                if (holder.tvFileName != null) {
                    holder.tvFileName.setText(file.name);
                }
                if (holder.tvFileInfo != null) {
                    holder.tvFileInfo.setText(file.getFormattedDate() + " • " + file.getFormattedSize());
                }

                holder.itemView.setOnClickListener(v -> {
                    if (listener != null) listener.onFileClick(file);
                });
                if (holder.btnDelete != null) {
                    holder.btnDelete.setOnClickListener(v -> {
                        if (listener != null) listener.onFileDelete(file);
                    });
                }
                if (holder.btnShare != null) {
                    holder.btnShare.setOnClickListener(v -> {
                        if (listener != null) listener.onFileShare(file);
                    });
                }
            }
        }

        @Override
        public int getItemCount() {
            return files.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvFileName, tvFileInfo;
            View btnDelete, btnShare;

            ViewHolder(View itemView) {
                super(itemView);
                tvFileName = itemView.findViewById(R.id.tvFileName);
                tvFileInfo = itemView.findViewById(R.id.tvFileInfo);
                btnDelete = itemView.findViewById(R.id.btnDelete);
                btnShare = itemView.findViewById(R.id.btnShare);
            }
        }
    }
}