package com.example.geonex;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.slider.RangeSlider;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class SearchActivity extends AppCompatActivity {

    private MaterialToolbar toolbar;
    private EditText searchInput;
    private ChipGroup filterChipGroup;
    private Chip chipCategory, chipDate, chipRadius, chipRecurring;
    private TextView tvClearFilters, tvResultCount;
    private RecyclerView recyclerView;
    private View emptyState;

    private ReminderAdapter adapter;
    private ReminderRepository repository;
    private List<Reminder> allReminders = new ArrayList<>();
    private List<Reminder> filteredReminders = new ArrayList<>();

    // Filter criteria
    private String selectedCategory = null;
    private Long startDate = null;
    private Long endDate = null;
    private Float minRadius = null;
    private Float maxRadius = null;
    private Boolean recurringOnly = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        initViews();
        setupToolbar();
        setupRecyclerView();
        setupSearch();
        setupFilters();
        loadReminders();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        searchInput = findViewById(R.id.searchInput);
        filterChipGroup = findViewById(R.id.filterChipGroup);
        chipCategory = findViewById(R.id.chipCategory);
        chipDate = findViewById(R.id.chipDate);
        chipRadius = findViewById(R.id.chipRadius);
        chipRecurring = findViewById(R.id.chipRecurring);
        tvClearFilters = findViewById(R.id.tvClearFilters);
        tvResultCount = findViewById(R.id.tvResultCount);
        recyclerView = findViewById(R.id.recyclerView);
        emptyState = findViewById(R.id.emptyState);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Search Reminders");
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ReminderAdapter(this);
        recyclerView.setAdapter(adapter);
    }

    private void setupSearch() {
        repository = ((GeonexApplication) getApplication()).getRepository();

        searchInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                performSearch(s.toString());
            }
        });
    }

    private void setupFilters() {
        chipCategory.setOnClickListener(v -> showCategoryFilter());
        chipDate.setOnClickListener(v -> showDateFilter());
        chipRadius.setOnClickListener(v -> showRadiusFilter());
        chipRecurring.setOnClickListener(v -> toggleRecurringFilter());
        tvClearFilters.setOnClickListener(v -> clearAllFilters());
    }

    private void loadReminders() {
        repository.getAllReminders().observe(this, reminders -> {
            allReminders = reminders;
            performSearch(searchInput.getText().toString());
        });
    }

    private void performSearch(String query) {

        filteredReminders.clear();

        for (Reminder reminder : allReminders) {

            boolean matches = true;

            if (!query.isEmpty()) {
                String q = query.toLowerCase();
                if (!reminder.getTitle().toLowerCase().contains(q))
                    matches = false;
            }

            if (matches && selectedCategory != null) {
                if (!selectedCategory.equalsIgnoreCase(reminder.getCategory()))
                    matches = false;
            }

            if (matches && startDate != null && endDate != null) {
                if (reminder.getCreatedAt() < startDate || reminder.getCreatedAt() > endDate)
                    matches = false;
            }

            if (matches && minRadius != null && maxRadius != null) {
                if (reminder.getRadius() < minRadius || reminder.getRadius() > maxRadius)
                    matches = false;
            }

            if (matches && recurringOnly != null && recurringOnly) {
                if (!reminder.isRecurring())
                    matches = false;
            }

            if (matches) filteredReminders.add(reminder);
        }

        adapter.setReminders(filteredReminders);
        updateUI();
    }

    private void updateUI() {
        int count = filteredReminders.size();
        tvResultCount.setText(count + " result" + (count != 1 ? "s" : ""));

        emptyState.setVisibility(count == 0 ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(count == 0 ? View.GONE : View.VISIBLE);

        chipCategory.setChecked(selectedCategory != null);
        chipDate.setChecked(startDate != null);
        chipRadius.setChecked(minRadius != null);
        chipRecurring.setChecked(recurringOnly != null && recurringOnly);
    }

    // ✅ FIXED DATE FILTER
    private void showDateFilter() {

        MaterialDatePicker<Long> datePicker =
                MaterialDatePicker.Builder.datePicker()
                        .setTitleText("Select Date")
                        .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
                        .build();

        datePicker.addOnPositiveButtonClickListener(selection -> {

            startDate = selection;
            endDate = selection + (24 * 60 * 60 * 1000);

            SimpleDateFormat sdf =
                    new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());

            chipDate.setText(sdf.format(new Date(selection)));
            performSearch(searchInput.getText().toString());
        });

        datePicker.addOnNegativeButtonClickListener(dialog -> {
            startDate = null;
            endDate = null;
            chipDate.setText("Date");
            performSearch(searchInput.getText().toString());
        });

        datePicker.show(getSupportFragmentManager(), "DATE_PICKER");
    }

    private void showCategoryFilter() {
        String[] categories = CategoryDetector.getAllCategories();

        new MaterialAlertDialogBuilder(this)
                .setTitle("Select Category")
                .setItems(categories, (dialog, which) -> {
                    selectedCategory = categories[which];
                    chipCategory.setText(selectedCategory);
                    performSearch(searchInput.getText().toString());
                })
                .setNegativeButton("Clear", (dialog, which) -> {
                    selectedCategory = null;
                    chipCategory.setText("Category");
                    performSearch(searchInput.getText().toString());
                })
                .show();
    }

    private void showRadiusFilter() {

        View view = getLayoutInflater().inflate(R.layout.dialog_radius_filter, null);
        RangeSlider slider = view.findViewById(R.id.rangeSlider);
        TextView tvRange = view.findViewById(R.id.tvRangeValue);

        slider.addOnChangeListener((s, value, fromUser) -> {
            List<Float> values = s.getValues();
            tvRange.setText((int)(float)values.get(0) + "m - " +
                    (int)(float)values.get(1) + "m");
        });

        new MaterialAlertDialogBuilder(this)
                .setTitle("Radius Range")
                .setView(view)
                .setPositiveButton("Apply", (dialog, which) -> {
                    List<Float> values = slider.getValues();
                    minRadius = values.get(0);
                    maxRadius = values.get(1);
                    chipRadius.setText((int)(float)minRadius + "-" +
                            (int)(float)maxRadius + "m");
                    performSearch(searchInput.getText().toString());
                })
                .setNegativeButton("Clear", (dialog, which) -> {
                    minRadius = null;
                    maxRadius = null;
                    chipRadius.setText("Radius");
                    performSearch(searchInput.getText().toString());
                })
                .show();
    }

    private void toggleRecurringFilter() {
        recurringOnly = (recurringOnly == null || !recurringOnly);
        chipRecurring.setText(recurringOnly ? "Recurring ✓" : "Recurring");
        performSearch(searchInput.getText().toString());
    }

    private void clearAllFilters() {

        selectedCategory = null;
        startDate = null;
        endDate = null;
        minRadius = null;
        maxRadius = null;
        recurringOnly = null;

        chipCategory.setText("Category");
        chipDate.setText("Date");
        chipRadius.setText("Radius");
        chipRecurring.setText("Recurring");

        performSearch(searchInput.getText().toString());
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}