package com.example.geonex;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ReminderDetailActivity extends AppCompatActivity {

    private MaterialToolbar toolbar;
    private TextView tvTitle, tvLocation, tvCategory, tvRadius, tvCreated, tvStatus, tvRecurringDetail;
    private CardView cardNavigate, cardShare, cardEdit, cardDelete, cardComplete;
    private FloatingActionButton fabEdit;

    private Reminder reminder;
    private ReminderRepository repository;
    private GeofenceHelper geofenceHelper;
    private int reminderId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reminder_detail);

        // Get reminder ID from intent
        reminderId = getIntent().getIntExtra("reminder_id", -1);

        if (reminderId == -1) {
            Toast.makeText(this, "Reminder not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        setupToolbar();

        repository = ((GeonexApplication) getApplication()).getRepository();
        geofenceHelper = new GeofenceHelper(this);

        loadReminder();
        setupClickListeners();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        tvTitle = findViewById(R.id.tvDetailTitle);
        tvLocation = findViewById(R.id.tvDetailLocation);
        tvCategory = findViewById(R.id.tvDetailCategory);
        tvRadius = findViewById(R.id.tvDetailRadius);
        tvCreated = findViewById(R.id.tvDetailCreated);
        tvStatus = findViewById(R.id.tvDetailStatus);
        tvRecurringDetail = findViewById(R.id.tvDetailRecurring);

        cardNavigate = findViewById(R.id.cardNavigate);
        cardShare = findViewById(R.id.cardShare);
        cardEdit = findViewById(R.id.cardEdit);
        cardDelete = findViewById(R.id.cardDelete);
        cardComplete = findViewById(R.id.cardComplete);
        fabEdit = findViewById(R.id.fabEdit);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle("Reminder Details");
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void loadReminder() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                reminder = repository.getReminderById(reminderId);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (reminder != null) {
                            displayReminder();
                        } else {
                            Toast.makeText(ReminderDetailActivity.this,
                                    "Reminder not found", Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    }
                });
            }
        }).start();
    }

    private void displayReminder() {
        tvTitle.setText(reminder.getTitle());
        tvLocation.setText(reminder.getLocationName());
        tvCategory.setText(reminder.getCategory());

        // Format radius
        float radius = reminder.getRadius();
        String radiusText;
        if (radius >= 1000) {
            radiusText = String.format(Locale.getDefault(), "%.1f km", radius / 1000);
        } else {
            radiusText = (int) radius + " meters";
        }
        tvRadius.setText(radiusText);

        // Format created date
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault());
        tvCreated.setText(sdf.format(new Date(reminder.getCreatedAt())));

        // Set status
        if (reminder.isCompleted()) {
            tvStatus.setText("✓ Completed");
            tvStatus.setTextColor(getColor(R.color.success));
            cardComplete.setVisibility(View.GONE);
        } else {
            tvStatus.setText("⚡ Active");
            tvStatus.setTextColor(getColor(R.color.primary));
            cardComplete.setVisibility(View.VISIBLE);
        }

        // Set recurring info
        if (reminder.isRecurring()) {
            String recurringText = getRecurringText();
            tvRecurringDetail.setText(recurringText);
            tvRecurringDetail.setVisibility(View.VISIBLE);
        } else {
            tvRecurringDetail.setVisibility(View.GONE);
        }
    }

    private String getRecurringText() {
        String rule = reminder.getRecurrenceRule();
        if ("daily".equals(rule)) {
            return "Repeats daily";
        } else if ("weekly".equals(rule)) {
            return "Repeats weekly";
        } else if ("monthly".equals(rule)) {
            return "Repeats monthly";
        } else if ("custom".equals(rule)) {
            return "Every " + reminder.getCustomInterval() + " " + reminder.getCustomIntervalUnit();
        }
        return "";
    }

    private void setupClickListeners() {
        cardNavigate.setOnClickListener(v -> navigateToLocation());
        cardShare.setOnClickListener(v -> shareReminder());
        cardEdit.setOnClickListener(v -> editReminder());
        cardDelete.setOnClickListener(v -> deleteReminder());
        cardComplete.setOnClickListener(v -> markAsCompleted());
        fabEdit.setOnClickListener(v -> editReminder());
    }

    private void navigateToLocation() {
        Uri gmmIntentUri = Uri.parse("geo:" + reminder.getLatitude() + "," +
                reminder.getLongitude() + "?q=" + reminder.getLatitude() + "," +
                reminder.getLongitude() + "(" + reminder.getLocationName() + ")");
        Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
        mapIntent.setPackage("com.google.android.apps.maps");

        if (mapIntent.resolveActivity(getPackageManager()) != null) {
            startActivity(mapIntent);
        } else {
            Toast.makeText(this, "Google Maps not installed", Toast.LENGTH_SHORT).show();
        }
    }

    private void shareReminder() {
        String shareText = "📍 Geonex Reminder\n\n" +
                "📌 " + reminder.getTitle() + "\n" +
                "📍 Location: " + reminder.getLocationName() + "\n" +
                "📏 Radius: " + tvRadius.getText() + "\n" +
                "📂 Category: " + reminder.getCategory() + "\n" +
                "📅 Created: " + tvCreated.getText();

        if (reminder.isRecurring()) {
            shareText += "\n🔄 " + getRecurringText();
        }

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);
        startActivity(Intent.createChooser(shareIntent, "Share Reminder"));
    }

    private void editReminder() {
        Intent intent = new Intent(ReminderDetailActivity.this, AddReminderActivity.class);
        intent.putExtra("mode", "edit");
        intent.putExtra("reminder_id", reminder.getId());
        startActivity(intent);
        finish();
    }

    private void deleteReminder() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Delete Reminder")
                .setMessage("Are you sure you want to delete \"" + reminder.getTitle() + "\"?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            repository.delete(reminder);
                            geofenceHelper.removeGeofence(reminder);
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(ReminderDetailActivity.this,
                                            "Reminder deleted", Toast.LENGTH_SHORT).show();
                                    finish();
                                }
                            });
                        }
                    }).start();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void markAsCompleted() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                reminder.setCompleted(true);
                repository.update(reminder);

                if (reminder.isRecurring()) {
                    // For recurring, we'll keep the geofence but reset later
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Snackbar.make(toolbar, "Recurring reminder will reset automatically",
                                    Snackbar.LENGTH_LONG).show();
                        }
                    });
                } else {
                    geofenceHelper.removeGeofence(reminder);
                }

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(ReminderDetailActivity.this,
                                "✓ Marked as completed", Toast.LENGTH_SHORT).show();
                        loadReminder(); // Refresh
                    }
                });
            }
        }).start();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_reminder_detail, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_edit) {
            editReminder();
            return true;
        } else if (id == R.id.action_share) {
            shareReminder();
            return true;
        } else if (id == R.id.action_delete) {
            deleteReminder();
            return true;
        } else if (id == R.id.action_duplicate) {
            duplicateReminder();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void duplicateReminder() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Reminder duplicate = new Reminder();
                duplicate.setTitle(reminder.getTitle() + " (Copy)");
                duplicate.setLocationName(reminder.getLocationName());
                duplicate.setLatitude(reminder.getLatitude());
                duplicate.setLongitude(reminder.getLongitude());
                duplicate.setRadius(reminder.getRadius());
                duplicate.setCategory(reminder.getCategory());
                duplicate.setRecurring(reminder.isRecurring());
                duplicate.setRecurrenceRule(reminder.getRecurrenceRule());
                duplicate.setCustomInterval(reminder.getCustomInterval());
                duplicate.setCustomIntervalUnit(reminder.getCustomIntervalUnit());
                duplicate.setCompleted(false);
                duplicate.setCreatedAt(System.currentTimeMillis());

                repository.insert(duplicate, new ReminderRepository.OnReminderInsertedListener() {
                    @Override
                    public void onInserted(Reminder insertedReminder) {
                        geofenceHelper.addGeofence(insertedReminder);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(ReminderDetailActivity.this,
                                        "Reminder duplicated", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                });
            }
        }).start();
    }
}