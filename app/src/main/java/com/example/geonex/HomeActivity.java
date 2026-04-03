package com.example.geonex;

import android.Manifest;
import android.animation.LayoutTransition;
import android.annotation.SuppressLint;
import android.app.ActivityOptions;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.transition.Fade;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.OvershootInterpolator;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;

public class HomeActivity extends AppCompatActivity {

    private static final String TAG = "HomeActivity";

    private MaterialToolbar toolbar;
    private RecyclerView recyclerView;
    private ReminderAdapter adapter;
    private ReminderRepository repository;
    private LinearLayout emptyState;
    private TextView tvProgressPercent, tvProgressText, tvReminderCount;
    private TextView tvSearchInfo, tvClearSearch;
    private View progressFill, searchCard;
    private EditText etSearch;
    private FloatingActionButton fabAdd;
    private LinearLayout categoryChipsContainer;
    private BottomNavigationView bottomNavigation;

    private GeofenceHelper geofenceHelper;
    private PermissionHelper permissionHelper;
    private LocationTrackingManager trackingManager;
    private GeofenceMonitorManager monitorManager;

    private Reminder deletedReminder = null;
    private int deletedPosition = -1;

    private LocationUpdateReceiver locationUpdateReceiver;
    private ServiceStateReceiver serviceStateReceiver;
    private boolean receiversRegistered = false;

    private static final int LOCATION_PERMISSION_REQUEST = 100;

    // ===== PERFORMANCE OPTIMIZATIONS =====
    private PerformanceOptimizer performanceOptimizer;
    private long lastLoadTime = 0;
    private static final long LOAD_COOLDOWN_MS = 500;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Enable window transitions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setEnterTransition(new Fade());
            getWindow().setExitTransition(new Fade());
        }

        setContentView(R.layout.activity_home);

        // Initialize views
        initViews();

        // Setup toolbar
        setupToolbar();

        // Setup bottom navigation
        setupBottomNavigation();

        // Initialize helpers
        repository = ((GeonexApplication) getApplication()).getRepository();
        performanceOptimizer = ((GeonexApplication) getApplication()).getPerformanceOptimizer();
        geofenceHelper = new GeofenceHelper(this);
        permissionHelper = new PermissionHelper(this);
        trackingManager = new LocationTrackingManager(this);
        monitorManager = new GeofenceMonitorManager(this);
        WorkManagerHelper workManagerHelper = new WorkManagerHelper(this);
        BatteryOptimizer batteryOptimizer = new BatteryOptimizer(this);

        // Setup RecyclerView with optimizations
        setupRecyclerView();

        // Setup category chips
        setupCategoryChips();

        // Setup enhanced search
        setupEnhancedSearch();

        // Setup swipe actions
        setupSwipeActions();

        // Setup FAB with animation
        setupFabWithAnimation();

        // Check permissions
        checkPermissions();

        // Load reminders with optimization
        loadReminders();

        // Schedule background tasks
        workManagerHelper.scheduleServiceCleanup();
        workManagerHelper.scheduleGeofenceRefresh();

        // Check and re-register geofences (for boot)
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                geofenceHelper.reregisterAllGeofences();
            }
        }, 1000);

        // Log battery status for debugging
        batteryOptimizer.logBatteryStatus();

        // Set up layout transitions for container
        setupLayoutTransitions();

        // Setup scroll listener for performance optimization
        setupScrollOptimization();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        recyclerView = findViewById(R.id.recyclerView);
        emptyState = findViewById(R.id.emptyState);
        tvProgressPercent = findViewById(R.id.tvProgressPercent);
        tvProgressText = findViewById(R.id.tvProgressText);
        tvReminderCount = findViewById(R.id.tvReminderCount);
        progressFill = findViewById(R.id.progressFill);
        etSearch = findViewById(R.id.etSearch);
        fabAdd = findViewById(R.id.fabAdd);
        categoryChipsContainer = findViewById(R.id.categoryChipsContainer);
        bottomNavigation = findViewById(R.id.bottomNavigation);

        // Search views
        tvSearchInfo = findViewById(R.id.tvSearchInfo);
        tvClearSearch = findViewById(R.id.tvClearSearch);
        searchCard = findViewById(R.id.searchCard);

        // Debug check for fabAdd
        if (fabAdd == null) {
            Log.e(TAG, "fabAdd is null - check layout ID in activity_home.xml");
        }
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Geonex");
        }
    }

    private void setupBottomNavigation() {
        bottomNavigation.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.navigation_home) {
                // Already in Home
                Log.d(TAG, "Home selected - already in Home");
                return true;
            }
            else if (id == R.id.navigation_family) {
                // Phase 2: Family screen
                Log.d(TAG, "Family selected - opening FamilyActivity");
                startActivity(new Intent(HomeActivity.this, FamilyActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            }
            else if (id == R.id.navigation_add) {
                // Phase 3: Add Reminder screen
                Log.d(TAG, "Add selected - opening AddReminderActivity");
                startActivity(new Intent(HomeActivity.this, AddReminderActivity.class));
                overridePendingTransition(R.anim.slide_in_right, R.anim.fade_out);
                return true;
            }
            else if (id == R.id.navigation_search) {
                // Phase 4: Search screen
                Log.d(TAG, "Search selected - opening SearchActivity");
                Toast.makeText(this, "Search screen coming in Phase 4", Toast.LENGTH_SHORT).show();
                // Uncomment when SearchActivity is created
                // startActivity(new Intent(HomeActivity.this, SearchActivity.class));
                // overridePendingTransition(0, 0);
                // finish();
                return true;
            }
            else if (id == R.id.navigation_settings) {
                // Phase 5: Settings screen
                Log.d(TAG, "Settings selected - opening SettingsActivity");
                Toast.makeText(this, "Settings screen coming in Phase 5", Toast.LENGTH_SHORT).show();
                // Uncomment when SettingsActivity is created
                // startActivity(new Intent(HomeActivity.this, SettingsActivity.class));
                // overridePendingTransition(0, 0);
                // finish();
                return true;
            }
            else if (id == R.id.navigation_search) {
                // Phase 4: Search screen
                Log.d(TAG, "Search selected - opening SearchActivity");
                startActivity(new Intent(HomeActivity.this, SearchActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            }
            else if (id == R.id.navigation_settings) {
                // Phase 5: Settings screen
                Log.d(TAG, "Settings selected - opening SettingsActivity");
                startActivity(new Intent(HomeActivity.this, SettingsActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            }

            return false;
        });

        // Set Home as default selected
        bottomNavigation.setSelectedItemId(R.id.navigation_home);
    }

    private void setupLayoutTransitions() {
        LayoutTransition transition = new LayoutTransition();
        transition.enableTransitionType(LayoutTransition.CHANGING);
        if (categoryChipsContainer != null) {
            ((ViewGroup) categoryChipsContainer.getParent()).setLayoutTransition(transition);
        }
    }

    // ===== PERFORMANCE OPTIMIZATION: Scroll optimization =====
    private void setupScrollOptimization() {
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
                    // Pause heavy operations while scrolling
                    if (adapter != null) {
                        adapter.setScrolling(true);
                    }
                } else if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    // Resume operations when scrolling stops
                    if (adapter != null) {
                        adapter.setScrolling(false);
                    }
                }
            }
        });
    }

    private void setupRecyclerView() {
        // ===== RECYCLERVIEW OPTIMIZATIONS =====

        // Create and configure RecycledViewPool for better performance
        RecyclerView.RecycledViewPool viewPool = new RecyclerView.RecycledViewPool();
        viewPool.setMaxRecycledViews(0, 10); // Cache up to 10 views
        recyclerView.setRecycledViewPool(viewPool);

        // Enable prefetching and caching
        recyclerView.setItemViewCacheSize(10);
        recyclerView.setHasFixedSize(true); // Set fixed size if possible

        // Create and configure LayoutManager with prefetch
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setInitialPrefetchItemCount(4); // Prefetch 4 items
        recyclerView.setLayoutManager(layoutManager);

        // Initialize adapter
        adapter = new ReminderAdapter(this);

        adapter.setOnReminderClickListener(new ReminderAdapter.OnReminderClickListener() {
            @Override
            public void onReminderClick(Reminder reminder) {
                onReminderClickHandler(reminder);
            }

            @Override
            public void onReminderMenuClick(Reminder reminder, View view) {
                showReminderMenu(reminder, view);
            }
        });

        adapter.setOnReminderLongClickListener(new ReminderAdapter.OnReminderLongClickListener() {
            @Override
            public void onReminderLongClick(Reminder reminder) {
                showReminderOptions(reminder);
            }
        });

        // Setup swipe action listener
        adapter.setOnSwipeActionListener(new ReminderAdapter.OnSwipeActionListener() {
            @Override
            public void onMarkAsCompleted(Reminder reminder, int position) {
                markAsCompletedFromSwipe(reminder, position);
            }

            @Override
            public void onDelete(Reminder reminder, int position) {
                showDeleteConfirmation(reminder, position);
            }
        });

        recyclerView.setAdapter(adapter);
    }

    private void setupCategoryChips() {
        String[] categories = CategoryDetector.getAllCategories();

        // Add "All" at the beginning
        String[] allCategories = new String[categories.length + 1];
        String[] allDisplayNames = new String[categories.length + 1];

        allCategories[0] = "all";
        allDisplayNames[0] = "All";

        for (int i = 0; i < categories.length; i++) {
            allCategories[i + 1] = categories[i];
            allDisplayNames[i + 1] = categories[i];
        }

        for (int i = 0; i < allCategories.length; i++) {
            MaterialButton chip = new MaterialButton(this);
            chip.setText(allDisplayNames[i]);
            chip.setBackgroundColor(ContextCompat.getColor(this, R.color.surface));
            chip.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
            chip.setCornerRadius(50);
            chip.setPadding(16, 8, 16, 8);
            chip.setElevation(0f);
            chip.setAllCaps(false);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(0, 0, 8, 0);
            chip.setLayoutParams(params);

            final String category = allCategories[i];
            chip.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // Reset all chips with animation
                    for (int j = 0; j < categoryChipsContainer.getChildCount(); j++) {
                        MaterialButton btn = (MaterialButton) categoryChipsContainer.getChildAt(j);
                        btn.animate()
                                .scaleX(1f)
                                .scaleY(1f)
                                .setDuration(200)
                                .start();
                        btn.setBackgroundColor(ContextCompat.getColor(HomeActivity.this, R.color.surface));
                        btn.setTextColor(ContextCompat.getColor(HomeActivity.this, R.color.text_secondary));
                    }

                    // Highlight selected with animation
                    chip.animate()
                            .scaleX(1.1f)
                            .scaleY(1.1f)
                            .setDuration(200)
                            .withEndAction(new Runnable() {
                                @Override
                                public void run() {
                                    chip.animate()
                                            .scaleX(1f)
                                            .scaleY(1f)
                                            .setDuration(200)
                                            .start();
                                }
                            })
                            .start();

                    chip.setBackgroundColor(ContextCompat.getColor(HomeActivity.this, R.color.primary));
                    chip.setTextColor(ContextCompat.getColor(HomeActivity.this, R.color.white));

                    // Filter reminders
                    adapter.filterByCategory(category);
                    updateEmptyState();
                    updateSearchInfo(adapter.getCurrentSearchQuery());
                }
            });

            categoryChipsContainer.addView(chip);
        }

        // Set first chip as selected
        if (categoryChipsContainer.getChildCount() > 0) {
            MaterialButton firstChip = (MaterialButton) categoryChipsContainer.getChildAt(0);
            firstChip.setBackgroundColor(ContextCompat.getColor(this, R.color.primary));
            firstChip.setTextColor(ContextCompat.getColor(this, R.color.white));
        }
    }

    // ===== FAB SETUP WITH ANIMATION =====
    private void setupFabWithAnimation() {
        if (fabAdd != null) {
            fabAdd.setOnClickListener(v -> {
                Log.d(TAG, "FAB clicked - opening AddReminderActivity");

                // Add scale animation to FAB
                v.animate()
                        .scaleX(0.9f)
                        .scaleY(0.9f)
                        .setDuration(100)
                        .withEndAction(new Runnable() {
                            @Override
                            public void run() {
                                v.animate()
                                        .scaleX(1.0f)
                                        .scaleY(1.0f)
                                        .setDuration(100)
                                        .start();

                                // Start AddReminderActivity with animation
                                Intent intent = new Intent(HomeActivity.this, AddReminderActivity.class);
                                startActivity(intent);
                                overridePendingTransition(R.anim.slide_in_right, R.anim.fade_out);
                            }
                        })
                        .start();
            });
            Log.d(TAG, "FAB click listener set successfully");
        } else {
            Log.e(TAG, "fabAdd is null - cannot set click listener");
        }
    }

    // ===== PHASE 2 - STEP 5: ENHANCED SEARCH =====
    private void setupEnhancedSearch() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s.toString();
                adapter.filter(query);
                updateSearchInfo(query);
                updateEmptyState();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Clear search button
        tvClearSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                etSearch.setText("");
                adapter.clearFilters();
                updateSearchInfo("");
                updateEmptyState();
            }
        });

        // Add search focus listener
        etSearch.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    searchCard.animate()
                            .scaleX(1.02f)
                            .scaleY(1.02f)
                            .setDuration(200)
                            .start();
                    searchCard.setBackgroundResource(R.drawable.search_focused_bg);
                } else {
                    searchCard.animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(200)
                            .start();
                    searchCard.setBackgroundResource(R.drawable.search_bg);
                }
            }
        });
    }

    @SuppressLint("SetTextI18n")
    private void updateSearchInfo(String query) {
        if (!query.isEmpty()) {
            int resultCount = adapter.getFilteredCount();
            tvSearchInfo.setVisibility(View.VISIBLE);
            tvSearchInfo.setText("Found " + resultCount + " result" + (resultCount != 1 ? "s" : ""));
            tvClearSearch.setVisibility(View.VISIBLE);

            // Animate clear button
            tvClearSearch.animate()
                    .alpha(1f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(300)
                    .start();
        } else {
            tvSearchInfo.setVisibility(View.GONE);

            // Animate clear button out
            tvClearSearch.animate()
                    .alpha(0f)
                    .scaleX(0.5f)
                    .scaleY(0.5f)
                    .setDuration(300)
                    .withEndAction(new Runnable() {
                        @Override
                        public void run() {
                            tvClearSearch.setVisibility(View.GONE);
                        }
                    })
                    .start();
        }
    }

    // ===== PHASE 2 - STEP 6: SWIPE ACTIONS =====
    private void setupSwipeActions() {
        SwipeToDeleteCallback swipeToDeleteCallback = new SwipeToDeleteCallback(
                this,
                adapter,
                new SwipeToDeleteCallback.OnSwipeListener() {
                    @Override
                    public void onSwipedLeft(Reminder reminder, int position) {
                        if (reminder != null) {
                            // Swipe left - Delete
                            showDeleteConfirmation(reminder, position);
                        } else {
                            adapter.notifyItemChanged(position);
                            Toast.makeText(HomeActivity.this,
                                    "Error: Reminder not found", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onSwipedRight(Reminder reminder, int position) {
                        if (reminder != null) {
                            // Swipe right - Mark as completed
                            markAsCompletedFromSwipe(reminder, position);
                        } else {
                            adapter.notifyItemChanged(position);
                            Toast.makeText(HomeActivity.this,
                                    "Error: Reminder not found", Toast.LENGTH_SHORT).show();
                        }
                    }
                });

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(swipeToDeleteCallback);
        itemTouchHelper.attachToRecyclerView(recyclerView);
    }

    private void showDeleteConfirmation(Reminder reminder, int position) {
        if (reminder == null) return;

        new MaterialAlertDialogBuilder(this)
                .setTitle("Delete Reminder")
                .setMessage("Are you sure you want to delete \"" + reminder.getTitle() + "\"?")
                .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        deleteReminderFromSwipe(reminder, position);
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        adapter.notifyItemChanged(position);
                    }
                })
                .setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        adapter.notifyItemChanged(position);
                    }
                })
                .show();
    }

    private void markAsCompletedFromSwipe(Reminder reminder, int position) {
        if (reminder == null) {
            adapter.notifyItemChanged(position);
            return;
        }

        try {
            // Mark as completed
            reminder.setCompleted(true);
            repository.update(reminder);

            // Remove from current list with animation
            adapter.removeReminderAt(position);

            // Show snackbar with undo option
            Snackbar.make(recyclerView, "✓ Reminder completed", Snackbar.LENGTH_LONG)
                    .setAction("Undo", new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            undoMarkAsCompleted(reminder, position);
                        }
                    })
                    .addCallback(new Snackbar.Callback() {
                        @Override
                        public void onDismissed(Snackbar transientBottomBar, int event) {
                            if (event != Snackbar.Callback.DISMISS_EVENT_ACTION) {
                                // Snackbar dismissed without undo - remove geofence
                                geofenceHelper.removeGeofence(reminder);
                            }
                        }
                    })
                    .show();

            updateStatistics(null);

            if (reminder.isRecurring()) {
                Toast.makeText(this, "Recurring reminder will reset automatically",
                        Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error marking as completed: " + e.getMessage());
            adapter.notifyItemChanged(position);
            Toast.makeText(this, "Error completing reminder", Toast.LENGTH_SHORT).show();
        }
    }

    private void undoMarkAsCompleted(Reminder reminder, int originalPosition) {
        if (reminder == null) return;

        try {
            reminder.setCompleted(false);
            repository.update(reminder);
            adapter.addReminder(reminder);
            updateStatistics(null);
            Toast.makeText(this, "Reminder restored", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e(TAG, "Error undoing completion: " + e.getMessage());
        }
    }

    private void deleteReminderFromSwipe(Reminder reminder, int position) {
        if (reminder == null) {
            adapter.notifyItemChanged(position);
            return;
        }

        try {
            // Save for undo
            deletedReminder = reminder;
            deletedPosition = position;

            // Remove from database and geofence
            repository.delete(reminder);
            geofenceHelper.removeGeofence(reminder);

            // Remove from adapter
            adapter.removeReminderAt(position);

            // Show snackbar with undo option
            Snackbar.make(recyclerView, "Reminder deleted", Snackbar.LENGTH_LONG)
                    .setAction("Undo", new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            undoDelete(reminder, position);
                        }
                    })
                    .addCallback(new Snackbar.Callback() {
                        @Override
                        public void onDismissed(Snackbar transientBottomBar, int event) {
                            if (event != Snackbar.Callback.DISMISS_EVENT_ACTION) {
                                // Snackbar dismissed without undo - cleanup
                                deletedReminder = null;
                                deletedPosition = -1;
                            }
                        }
                    })
                    .show();

            updateStatistics(null);

        } catch (Exception e) {
            Log.e(TAG, "Error deleting reminder: " + e.getMessage());
            adapter.notifyItemChanged(position);
            Toast.makeText(this, "Error deleting reminder", Toast.LENGTH_SHORT).show();
        }
    }

    private void undoDelete(Reminder reminder, int originalPosition) {
        if (reminder == null) return;

        try {
            // Restore reminder
            repository.insert(reminder, new ReminderRepository.OnReminderInsertedListener() {
                @Override
                public void onInserted(Reminder insertedReminder) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            geofenceHelper.addGeofence(insertedReminder);
                            adapter.addReminder(insertedReminder);
                            updateStatistics(null);
                            Toast.makeText(HomeActivity.this,
                                    "Reminder restored", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            });

            deletedReminder = null;
            deletedPosition = -1;

        } catch (Exception e) {
            Log.e(TAG, "Error undoing delete: " + e.getMessage());
        }
    }

    // ===== ANIMATIONS =====
    private void animateEmptyState() {
        emptyState.setAlpha(0f);
        emptyState.setScaleX(0.9f);
        emptyState.setScaleY(0.9f);
        emptyState.setVisibility(View.VISIBLE);

        emptyState.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(300)
                .setInterpolator(new OvershootInterpolator())
                .start();
    }

    // ===== SAFETY CHECK =====
    private void performSafetyCheck() {
        SafetyManager safetyManager = ((GeonexApplication) getApplication()).getSafetyManager();
        if (safetyManager == null) return;

        SafetyManager.SafetyStatus status = safetyManager.getSafetyStatus();

        if (!status.isAllOk()) {
            String errorMessage = safetyManager.getErrorMessage();
            if (errorMessage != null) {
                new MaterialAlertDialogBuilder(this)
                        .setTitle("⚠️ Safety Check")
                        .setMessage(errorMessage + "\n\n" + status.getSummary())
                        .setPositiveButton("Fix", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Intent recoveryIntent = safetyManager.getRecoveryIntent();
                                if (recoveryIntent != null) {
                                    startActivity(recoveryIntent);
                                }
                            }
                        })
                        .setNegativeButton("Ignore", null)
                        .show();
            }
        } else {
            Log.d(TAG, "Safety check passed: " + status.getSummary());
        }
    }

    // ===== PHASE 3 - BACKGROUND SERVICES =====
    private void checkAndStartTracking() {
        if (permissionHelper.hasPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION)) {
            trackingManager.updateTrackingState();
        }
    }

    private void updateGeofenceMonitoring() {
        if (permissionHelper.hasBackgroundLocationPermission()) {
            monitorManager.updateMonitoringState();
        }
    }

    // ===== BROADCAST RECEIVERS =====
    private void registerBroadcastReceivers() {
        if (!receiversRegistered) {
            // Register location update receiver
            locationUpdateReceiver = new LocationUpdateReceiver();
            IntentFilter locationFilter = new IntentFilter("com.example.geonex.LOCATION_UPDATE");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(locationUpdateReceiver, locationFilter, Context.RECEIVER_NOT_EXPORTED);
            } else {
                registerReceiver(locationUpdateReceiver, locationFilter);
            }

            // Register service state receiver
            serviceStateReceiver = new ServiceStateReceiver();
            IntentFilter serviceFilter = new IntentFilter();
            serviceFilter.addAction("com.example.geonex.CHECK_SERVICES");
            serviceFilter.addAction(Intent.ACTION_SCREEN_ON);
            serviceFilter.addAction(Intent.ACTION_SCREEN_OFF);
            serviceFilter.addAction(Intent.ACTION_POWER_CONNECTED);
            serviceFilter.addAction(Intent.ACTION_POWER_DISCONNECTED);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(serviceStateReceiver, serviceFilter, Context.RECEIVER_NOT_EXPORTED);
            } else {
                registerReceiver(serviceStateReceiver, serviceFilter);
            }

            receiversRegistered = true;
            Log.d(TAG, "Broadcast receivers registered");
        }
    }

    private void unregisterBroadcastReceivers() {
        if (receiversRegistered) {
            unregisterReceiver(locationUpdateReceiver);
            unregisterReceiver(serviceStateReceiver);
            receiversRegistered = false;
            Log.d(TAG, "Broadcast receivers unregistered");
        }
    }

    // ===== OPTIMIZED REMINDER LOADING WITH DEBUG LOGS =====
    private void loadReminders() {
        Log.d("DEBUG", "🏠 HomeActivity: loadReminders() called");

        // Use direct observation without performance optimizer for now to ensure it works
        repository.getActiveReminders().observe(this, new Observer<List<Reminder>>() {
            @Override
            public void onChanged(List<Reminder> reminders) {
                Log.d("DEBUG", "📋 LiveData onChanged: " +
                        (reminders != null ? reminders.size() : 0) + " reminders");

                runOnUiThread(() -> {
                    if (reminders != null && !reminders.isEmpty()) {
                        Log.d("DEBUG", "   • First reminder: " + reminders.get(0).getId() + " - " + reminders.get(0).getTitle());
                        for (Reminder r : reminders) {
                            Log.d("DEBUG", "   • Reminder in list: ID=" + r.getId() + ", Title=" + r.getTitle() + ", Completed=" + r.isCompleted());
                        }
                        adapter.setReminders(reminders);
                        tvReminderCount.setText(reminders.size() + " items");
                    } else {
                        Log.d("DEBUG", "   • No reminders (empty or null)");
                        adapter.setReminders(new ArrayList<>());
                        tvReminderCount.setText("0 items");
                    }
                    updateStatistics(reminders);
                    updateEmptyState();
                });
            }
        });
    }

    private void updateStatistics(List<Reminder> reminders) {
        repository.getStatistics(new ReminderRepository.OnStatisticsListener() {
            @Override
            public void onStatistics(int total, int completed, int active) {
                runOnUiThread(() -> {
                    int percent = total == 0 ? 0 : (completed * 100 / total);

                    // Animate percentage change
                    tvProgressPercent.animate()
                            .alpha(0f)
                            .setDuration(150)
                            .withEndAction(new Runnable() {
                                @Override
                                public void run() {
                                    tvProgressPercent.setText(percent + "%");
                                    tvProgressPercent.animate()
                                            .alpha(1f)
                                            .setDuration(150)
                                            .start();
                                }
                            })
                            .start();

                    tvProgressText.animate()
                            .alpha(0f)
                            .setDuration(150)
                            .withEndAction(new Runnable() {
                                @Override
                                public void run() {
                                    tvProgressText.setText(completed + "/" + total + " reminders completed");
                                    tvProgressText.animate()
                                            .alpha(1f)
                                            .setDuration(150)
                                            .start();
                                }
                            })
                            .start();

                    // Update progress bar width with animation
                    progressFill.post(new Runnable() {
                        @Override
                        public void run() {
                            ViewGroup.LayoutParams params = progressFill.getLayoutParams();
                            View parent = (View) progressFill.getParent();
                            if (parent != null) {
                                int newWidth = (int) (parent.getWidth() * percent / 100f);

                                // Animate width change
                                progressFill.animate()
                                        .scaleX(newWidth / (float) params.width)
                                        .setDuration(300)
                                        .start();

                                params.width = newWidth;
                                progressFill.setLayoutParams(params);
                            }
                        }
                    });
                });
            }
        });
    }

    private void updateEmptyState() {
        if (adapter.getItemCount() == 0) {
            if (emptyState.getVisibility() != View.VISIBLE) {
                animateEmptyState();
            }

            recyclerView.animate()
                    .alpha(0f)
                    .setDuration(200)
                    .withEndAction(new Runnable() {
                        @Override
                        public void run() {
                            recyclerView.setVisibility(View.GONE);
                        }
                    })
                    .start();
        } else {
            emptyState.animate()
                    .alpha(0f)
                    .setDuration(200)
                    .withEndAction(new Runnable() {
                        @Override
                        public void run() {
                            emptyState.setVisibility(View.GONE);
                        }
                    })
                    .start();

            recyclerView.setVisibility(View.VISIBLE);
            recyclerView.setAlpha(0f);
            recyclerView.animate()
                    .alpha(1f)
                    .setDuration(300)
                    .start();
        }
    }

    // ===== SHIMMER LOADING EFFECT =====
    private void showShimmerLoading() {
        View shimmerContainer = findViewById(R.id.shimmerContainer);
        if (shimmerContainer != null) {
            shimmerContainer.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);

            // Fade in shimmer
            shimmerContainer.animate()
                    .alpha(1f)
                    .setDuration(300)
                    .start();
        }
    }

    private void hideShimmerLoading() {
        View shimmerContainer = findViewById(R.id.shimmerContainer);
        if (shimmerContainer != null) {
            shimmerContainer.animate()
                    .alpha(0f)
                    .setDuration(300)
                    .withEndAction(new Runnable() {
                        @Override
                        public void run() {
                            shimmerContainer.setVisibility(View.GONE);
                            recyclerView.setVisibility(View.VISIBLE);
                            recyclerView.setAlpha(0f);
                            recyclerView.animate().alpha(1f).setDuration(300).start();
                        }
                    })
                    .start();
        }
    }

    // ===== PERMISSION CHECKS =====
    private void checkPermissions() {
        if (!permissionHelper.hasAllPermissions()) {
            new MaterialAlertDialogBuilder(this)
                    .setTitle("Permissions Required")
                    .setMessage("Location and notification permissions are needed for the app to work properly.")
                    .setPositiveButton("Grant", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            requestPermissions();
                        }
                    })
                    .setNegativeButton("Skip", null)
                    .show();
        }

        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (locationManager != null && !locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            showGPSDialog();
        }
    }

    private void requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(this,
                    PermissionHelper.REQUIRED_PERMISSIONS,
                    LOCATION_PERMISSION_REQUEST);
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.ACCESS_BACKGROUND_LOCATION},
                    LOCATION_PERMISSION_REQUEST);
        }
    }

    private void showGPSDialog() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("GPS Required")
                .setMessage("Please enable GPS for accurate location detection.")
                .setPositiveButton("Open Settings", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        startActivity(intent);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        boolean allGranted = true;
        for (int result : grantResults) {
            if (result != PackageManager.PERMISSION_GRANTED) {
                allGranted = false;
                break;
            }
        }

        if (allGranted) {
            Toast.makeText(this, "All permissions granted!", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Some permissions are missing", Toast.LENGTH_SHORT).show();
        }
    }

    // ===== REMINDER ACTIONS =====
    private void showReminderDetails(Reminder reminder) {
        String details = "Title: " + reminder.getTitle() + "\n" +
                "Location: " + reminder.getLocationName() + "\n" +
                "Radius: " + reminder.getRadius() + "m\n" +
                "Category: " + reminder.getCategory();

        if (reminder.isRecurring()) {
            String rule = reminder.getRecurrenceRule();
            if ("custom".equals(rule)) {
                details += "\nRecurring: Every " + reminder.getCustomInterval() +
                        " " + reminder.getCustomIntervalUnit();
            } else {
                details += "\nRecurring: " + rule.substring(0, 1).toUpperCase() +
                        rule.substring(1);
            }
        }

        new MaterialAlertDialogBuilder(this)
                .setTitle("Reminder Details")
                .setMessage(details)
                .setPositiveButton("OK", null)
                .show();
    }

    private void showReminderMenu(Reminder reminder, View view) {
        String[] options = {"Mark as Completed", "Edit", "Delete"};

        new MaterialAlertDialogBuilder(this)
                .setItems(options, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case 0:
                                markAsCompleted(reminder);
                                break;
                            case 1:
                                Toast.makeText(HomeActivity.this,
                                        "Edit coming in Phase 4",
                                        Toast.LENGTH_SHORT).show();
                                break;
                            case 2:
                                deleteReminder(reminder);
                                break;
                        }
                    }
                })
                .show();
    }

    private void showReminderOptions(Reminder reminder) {
        showReminderMenu(reminder, null);
    }

    private void markAsCompleted(Reminder reminder) {
        reminder.setCompleted(true);
        repository.update(reminder);

        if (reminder.isRecurring()) {
            Toast.makeText(this, "Recurring reminder will reset automatically",
                    Toast.LENGTH_SHORT).show();
        }

        Toast.makeText(this, "Reminder marked as completed", Toast.LENGTH_SHORT).show();
        loadReminders();
    }

    private void deleteReminder(Reminder reminder) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Delete Reminder")
                .setMessage("Are you sure you want to delete this reminder?")
                .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        repository.delete(reminder);
                        geofenceHelper.removeGeofence(reminder);
                        Toast.makeText(HomeActivity.this, "Reminder deleted",
                                Toast.LENGTH_SHORT).show();
                        loadReminders();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // ===== REMINDER CLICK HANDLER =====
    private void onReminderClickHandler(Reminder reminder) {
        Intent intent = new Intent(HomeActivity.this, ReminderDetailActivity.class);
        intent.putExtra("reminder_id", reminder.getId());
        startActivity(intent);
        overridePendingTransition(R.anim.slide_in_right, R.anim.fade_out);
    }

    // ===== LIFECYCLE =====
    @Override
    protected void onResume() {
        super.onResume();
        Log.d("DEBUG", "🏠 HomeActivity: onResume() called");
        loadReminders();
        checkAndStartTracking();
        updateGeofenceMonitoring();
        registerBroadcastReceivers();
        performSafetyCheck();

        // Ensure home is selected when returning to this activity
        if (bottomNavigation != null) {
            bottomNavigation.setSelectedItemId(R.id.navigation_home);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterBroadcastReceivers();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Clean up if needed
        if (performanceOptimizer != null) {
            // Clear cache if needed
            repository.clearCache();
        }
    }

    // ===== MENU =====
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_home, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_notifications) {
            Toast.makeText(this, "Notifications center coming in Phase 4",
                    Toast.LENGTH_SHORT).show();
            return true;
        } else if (id == R.id.action_profile) {
            Intent intent = new Intent(HomeActivity.this, ProfileActivity.class);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right, R.anim.fade_out);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


}