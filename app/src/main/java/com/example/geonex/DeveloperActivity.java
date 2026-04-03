package com.example.geonex;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

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
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class DeveloperActivity extends AppCompatActivity implements TestHelper.TestDataCallback {

    private MaterialToolbar toolbar;
    private TextView tvDatabaseStats, tvMemoryStats, tvServiceStats;
    private LinearProgressIndicator progressBar;
    private TextView tvProgressMessage;
    private View progressContainer;
    private RecyclerView recyclerView;
    private LogAdapter logAdapter;

    private TestHelper testHelper;
    private DatabaseOptimizer databaseOptimizer;
    private PerformanceOptimizer performanceOptimizer;
    private SecurityManager securityManager;
    private SafetyManager safetyManager;
    private Handler mainHandler;

    private boolean isTesting = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_developer);

        initViews();
        setupToolbar();

        testHelper = new TestHelper(this);
        databaseOptimizer = new DatabaseOptimizer(this);
        performanceOptimizer = ((GeonexApplication) getApplication()).getPerformanceOptimizer();
        securityManager = new SecurityManager(this);
        safetyManager = new SafetyManager(this);
        mainHandler = new Handler(Looper.getMainLooper());

        setupClickListeners();
        updateStats();
        setupLogAdapter();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        tvDatabaseStats = findViewById(R.id.tvDatabaseStats);
        tvMemoryStats = findViewById(R.id.tvMemoryStats);
        tvServiceStats = findViewById(R.id.tvServiceStats);
        progressBar = findViewById(R.id.progressBar);
        tvProgressMessage = findViewById(R.id.tvProgressMessage);
        progressContainer = findViewById(R.id.progressContainer);
        recyclerView = findViewById(R.id.recyclerView);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle("Developer Tools");
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupClickListeners() {
        findViewById(R.id.btnGenerateTestData).setOnClickListener(v ->
                showGenerateDataDialog());

        findViewById(R.id.btnClearTestData).setOnClickListener(v ->
                showClearDataDialog());

        findViewById(R.id.btnRunAllTests).setOnClickListener(v ->
                runAllTests());

        findViewById(R.id.btnTestGeofence).setOnClickListener(v ->
                showReminderSelector("Select reminder to test geofence",
                        id -> testHelper.testGeofenceTrigger(id)));

        findViewById(R.id.btnTestNotification).setOnClickListener(v ->
                showReminderSelector("Select reminder to test notification",
                        id -> testHelper.testNotification(id)));

        findViewById(R.id.btnTestServices).setOnClickListener(v -> {
            testHelper.testBackgroundService();
            addLog("✅ Background services started");
        });

        findViewById(R.id.btnCheckPermissions).setOnClickListener(v ->
                checkPermissions());

        findViewById(R.id.btnOptimizeDatabase).setOnClickListener(v ->
                optimizeDatabase());

        findViewById(R.id.btnClearCache).setOnClickListener(v ->
                clearCache());

        findViewById(R.id.btnCrashTest).setOnClickListener(v ->
                showCrashTestDialog());

        // Toggle switches
        SwitchMaterial switchDebugMode = findViewById(R.id.switchDebugMode);
        switchDebugMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            addLog(isChecked ? "🔧 Debug mode enabled" : "🔧 Debug mode disabled");
        });

        SwitchMaterial switchMockLocation = findViewById(R.id.switchMockLocation);
        switchMockLocation.setOnCheckedChangeListener((buttonView, isChecked) -> {
            addLog(isChecked ? "📍 Mock location enabled" : "📍 Mock location disabled");
        });
    }

    private void setupLogAdapter() {
        logAdapter = new LogAdapter();
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(logAdapter);
    }

    private void updateStats() {
        // Database stats
        DatabaseOptimizer.DatabaseStats dbStats = databaseOptimizer.getStats();
        tvDatabaseStats.setText(String.format(Locale.getDefault(),
                "Rows: %d | Size: %s | Queries: %d",
                dbStats.rowCount, dbStats.getFormattedSize(), dbStats.queryCount));

        // Memory stats
        Runtime runtime = Runtime.getRuntime();
        long usedMemory = runtime.totalMemory() - runtime.freeMemory();
        long maxMemory = runtime.maxMemory();
        tvMemoryStats.setText(String.format(Locale.getDefault(),
                "Used: %.2f MB | Max: %.2f MB | Free: %.2f MB",
                usedMemory / (1024.0 * 1024.0),
                maxMemory / (1024.0 * 1024.0),
                runtime.freeMemory() / (1024.0 * 1024.0)));

        // Service stats
        ServiceOptimizer optimizer = new ServiceOptimizer(this);
        boolean trackingRunning = optimizer.isServiceRunning(LocationTrackingService.class);
        boolean monitoringRunning = optimizer.isServiceRunning(GeofenceMonitoringService.class);
        tvServiceStats.setText(String.format(Locale.getDefault(),
                "Tracking: %s | Monitoring: %s | GPS: %s",
                trackingRunning ? "✅" : "❌",
                monitoringRunning ? "✅" : "❌",
                optimizer.isGpsEnabled() ? "✅" : "❌"));

        // Update every 2 seconds
        mainHandler.postDelayed(this::updateStats, 2000);
    }

    private void addLog(String message) {
        logAdapter.addLog(message);
        recyclerView.smoothScrollToPosition(logAdapter.getItemCount() - 1);
    }

    private void showGenerateDataDialog() {
        String[] options = {"10 reminders", "25 reminders", "50 reminders", "100 reminders"};

        new MaterialAlertDialogBuilder(this)
                .setTitle("Generate Test Data")
                .setItems(options, (dialog, which) -> {
                    int count = switch (which) {
                        case 0 -> 10;
                        case 1 -> 25;
                        case 2 -> 50;
                        case 3 -> 100;
                        default -> 10;
                    };
                    generateTestData(count);
                })
                .show();
    }

    private void generateTestData(int count) {
        if (isTesting) return;

        isTesting = true;
        progressContainer.setVisibility(View.VISIBLE);
        progressBar.setProgress(0);
        tvProgressMessage.setText("Generating test data...");

        testHelper.generateTestData(count, this);
    }

    private void showClearDataDialog() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Clear Test Data")
                .setMessage("Are you sure you want to delete ALL reminders?")
                .setPositiveButton("Clear", (dialog, which) -> {
                    progressContainer.setVisibility(View.VISIBLE);
                    progressBar.setProgress(0);
                    tvProgressMessage.setText("Clearing data...");
                    testHelper.clearTestData(this);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void runAllTests() {
        if (isTesting) return;

        new MaterialAlertDialogBuilder(this)
                .setTitle("Run All Tests")
                .setMessage("This will run comprehensive tests on all systems. Continue?")
                .setPositiveButton("Run Tests", (dialog, which) -> {
                    isTesting = true;
                    progressContainer.setVisibility(View.VISIBLE);
                    progressBar.setProgress(0);
                    tvProgressMessage.setText("Starting tests...");
                    addLog("🧪 Starting comprehensive tests...");
                    testHelper.runAllTests(this);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showReminderSelector(String title, ReminderAction action) {
        List<Reminder> reminders = ((GeonexApplication) getApplication())
                .getRepository().getAllReminders().getValue();

        if (reminders == null || reminders.isEmpty()) {
            Snackbar.make(toolbar, "No reminders found", Snackbar.LENGTH_SHORT).show();
            return;
        }

        String[] reminderTitles = new String[reminders.size()];
        for (int i = 0; i < reminders.size(); i++) {
            reminderTitles[i] = reminders.get(i).getTitle();
        }

        new MaterialAlertDialogBuilder(this)
                .setTitle(title)
                .setItems(reminderTitles, (dialog, which) -> {
                    int id = reminders.get(which).getId();
                    action.execute(id);
                })
                .show();
    }

    private void checkPermissions() {
        SafetyManager.SafetyStatus status = safetyManager.getSafetyStatus();
        addLog("🔍 Permission Check:");
        addLog(status.getSummary());

        String error = safetyManager.getErrorMessage();
        if (error != null) {
            addLog("⚠️ " + error);
        }
    }

    private void optimizeDatabase() {
        progressContainer.setVisibility(View.VISIBLE);
        tvProgressMessage.setText("Optimizing database...");

        new Thread(() -> {
            databaseOptimizer.vacuumDatabase();
            mainHandler.post(() -> {
                progressContainer.setVisibility(View.GONE);
                addLog("✅ Database optimized");
                updateStats();
            });
        }).start();
    }

    private void clearCache() {
        performanceOptimizer.clearCaches();
        addLog("✅ Cache cleared");
        updateStats();
    }

    private void showCrashTestDialog() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Crash Test")
                .setMessage("This will crash the app to test recovery. Continue?")
                .setPositiveButton("Crash", (dialog, which) -> {
                    addLog("💥 Crashing app...");
                    throw new RuntimeException("Crash test");
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // ===== TEST CALLBACK =====

    @Override
    public void onProgress(int progress, String message) {
        mainHandler.post(() -> {
            progressBar.setProgress(progress);
            tvProgressMessage.setText(message);
        });
    }

    @Override
    public void onComplete(int count) {
        mainHandler.post(() -> {
            progressContainer.setVisibility(View.GONE);
            isTesting = false;
            updateStats();
            addLog("✅ Operation completed: " + count + " items affected");
        });
    }

    @Override
    public void onError(String error) {
        mainHandler.post(() -> {
            progressContainer.setVisibility(View.GONE);
            isTesting = false;
            addLog("❌ Error: " + error);
            Snackbar.make(toolbar, "Error: " + error, Snackbar.LENGTH_LONG).show();
        });
    }

    // ===== INTERFACES =====

    interface ReminderAction {
        void execute(int reminderId);
    }

    // ===== LOG ADAPTER (FIXED - ViewGroup import added) =====

    private static class LogAdapter extends RecyclerView.Adapter<LogAdapter.LogViewHolder> {

        private final List<String> logs = new ArrayList<>();

        void addLog(String log) {
            logs.add(0, log); // Add to top
            if (logs.size() > 100) {
                logs.remove(logs.size() - 1);
            }
            notifyItemInserted(0);
        }

        @NonNull
        @Override
        public LogViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            TextView textView = new TextView(parent.getContext());
            textView.setPadding(16, 12, 16, 12);
            textView.setTextSize(12);
            return new LogViewHolder(textView);
        }

        @Override
        public void onBindViewHolder(@NonNull LogViewHolder holder, int position) {
            holder.textView.setText(logs.get(position));

            // Color coding
            String log = logs.get(position);
            if (log.contains("✅")) {
                holder.textView.setTextColor(holder.textView.getContext().getColor(R.color.success));
            } else if (log.contains("❌")) {
                holder.textView.setTextColor(holder.textView.getContext().getColor(R.color.error));
            } else if (log.contains("⚠️")) {
                holder.textView.setTextColor(holder.textView.getContext().getColor(R.color.warning));
            } else if (log.contains("🔧")) {
                holder.textView.setTextColor(holder.textView.getContext().getColor(R.color.primary));
            } else {
                holder.textView.setTextColor(holder.textView.getContext().getColor(R.color.text_secondary));
            }
        }

        @Override
        public int getItemCount() {
            return logs.size();
        }

        static class LogViewHolder extends RecyclerView.ViewHolder {
            TextView textView;

            LogViewHolder(TextView itemView) {
                super(itemView);
                textView = itemView;
            }
        }
    }
}