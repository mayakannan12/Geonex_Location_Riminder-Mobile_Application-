package com.example.geonex;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FamilyActivity extends AppCompatActivity {

    private MaterialToolbar toolbar;
    private RecyclerView recyclerView;
    private FamilyMemberAdapter adapter;
    private TextView tvMemberCount, tvTotalReminders;
    private MaterialButton btnInvite;
    private FloatingActionButton fabAddMember;

    private List<FamilyMember> familyMembers = new ArrayList<>();
    private ReminderRepository repository;
    private ExecutorService executorService = Executors.newSingleThreadExecutor();
    private Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_family);

        initViews();
        setupToolbar();
        setupRecyclerView();
        loadFamilyMembers();
        setupClickListeners();

        repository = ((GeonexApplication) getApplication()).getRepository();
        updateStatistics(); // This now runs in background thread
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        recyclerView = findViewById(R.id.recyclerView);
        tvMemberCount = findViewById(R.id.tvMemberCount);
        tvTotalReminders = findViewById(R.id.tvTotalReminders);
        btnInvite = findViewById(R.id.btnInvite);
        fabAddMember = findViewById(R.id.fabAddMember);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle("Family");
        }
        toolbar.setNavigationOnClickListener(v -> {
            // Go back to Home with bottom navigation
            startActivity(new Intent(this, HomeActivity.class));
            finish();
        });
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new FamilyMemberAdapter(familyMembers, new FamilyMemberAdapter.OnFamilyMemberClickListener() {
            @Override
            public void onMemberClick(FamilyMember member, int position) {
                showMemberDetails(member);
            }

            @Override
            public void onMemberLongClick(FamilyMember member, int position) {
                showMemberOptions(member, position);
            }

            @Override
            public void onAssignReminderClick(FamilyMember member, int position) {
                assignReminderToMember(member);
            }
        });
        recyclerView.setAdapter(adapter);
    }

    private void loadFamilyMembers() {
        // Sample data - In real app, this would come from database/cloud
        familyMembers.clear();
        familyMembers.add(new FamilyMember(
                1,
                "Rajesh Kumar",
                "Father",
                "rajesh@example.com",
                "9123456789",
                FamilyMember.ROLE_ADMIN,
                3,
                "https://i.pravatar.cc/150?img=1"
        ));

        familyMembers.add(new FamilyMember(
                2,
                "Priya Kumar",
                "Mother",
                "priya@example.com",
                "9876543210",
                FamilyMember.ROLE_MEMBER,
                5,
                "https://i.pravatar.cc/150?img=2"
        ));

        familyMembers.add(new FamilyMember(
                3,
                "Arjun Kumar",
                "Son",
                "arjun@example.com",
                "9988776655",
                FamilyMember.ROLE_MEMBER,
                2,
                "https://i.pravatar.cc/150?img=3"
        ));

        familyMembers.add(new FamilyMember(
                4,
                "Ananya Kumar",
                "Daughter",
                "ananya@example.com",
                "8877665544",
                FamilyMember.ROLE_MEMBER,
                1,
                "https://i.pravatar.cc/150?img=4"
        ));

        adapter.notifyDataSetChanged();
        updateCounts();
    }

    private void updateCounts() {
        tvMemberCount.setText(familyMembers.size() + " members");

        // Calculate total reminders across all members
        int total = 0;
        for (FamilyMember member : familyMembers) {
            total += member.getReminderCount();
        }
        tvTotalReminders.setText("Total: " + total + " reminders");
    }

    // ===== FIXED: Database access moved to background thread =====
    private void updateStatistics() {
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    // Get total reminders from database (background thread)
                    int totalCount = repository.getTotalCount();

                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            // Update UI on main thread
                            // You can use this to show total reminders if needed
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void setupClickListeners() {
        btnInvite.setOnClickListener(v -> {
            showInviteDialog();
        });

        fabAddMember.setOnClickListener(v -> {
            showAddMemberDialog();
        });
    }

    private void showMemberDetails(FamilyMember member) {
        String details = "👤 Name: " + member.getName() + "\n" +
                "📌 Role: " + member.getRole() + "\n" +
                "📧 Email: " + member.getEmail() + "\n" +
                "📞 Phone: " + member.getPhone() + "\n" +
                "📋 Reminders: " + member.getReminderCount();

        new MaterialAlertDialogBuilder(this)
                .setTitle("Member Details")
                .setMessage(details)
                .setPositiveButton("OK", null)
                .setNegativeButton("Assign Reminder", (dialog, which) -> {
                    assignReminderToMember(member);
                })
                .show();
    }

    private void showMemberOptions(FamilyMember member, int position) {
        String[] options = {"Edit Member", "Assign Reminder", "Remove Member", "Cancel"};

        new MaterialAlertDialogBuilder(this)
                .setTitle(member.getName())
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            editMember(member, position);
                            break;
                        case 1:
                            assignReminderToMember(member);
                            break;
                        case 2:
                            removeMember(member, position);
                            break;
                    }
                })
                .show();
    }

    private void showInviteDialog() {
        // Create invite link/share
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Join my Geonex Family");
        shareIntent.putExtra(Intent.EXTRA_TEXT,
                "Join my family on Geonex! Download the app and use code: FAMILY123");
        startActivity(Intent.createChooser(shareIntent, "Invite via"));
    }

    private void showAddMemberDialog() {
        // In a real app, this would open a form
        Toast.makeText(this, "Add Member form will open here", Toast.LENGTH_SHORT).show();
    }

    private void editMember(FamilyMember member, int position) {
        Toast.makeText(this, "Edit " + member.getName(), Toast.LENGTH_SHORT).show();
    }

    private void assignReminderToMember(FamilyMember member) {
        Toast.makeText(this, "Assign reminder to " + member.getName(), Toast.LENGTH_SHORT).show();
    }

    private void removeMember(FamilyMember member, int position) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Remove Member")
                .setMessage("Are you sure you want to remove " + member.getName() + " from family?")
                .setPositiveButton("Remove", (dialog, which) -> {
                    familyMembers.remove(position);
                    adapter.notifyItemRemoved(position);
                    updateCounts();
                    Toast.makeText(this, member.getName() + " removed", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public boolean onSupportNavigateUp() {
        startActivity(new Intent(this, HomeActivity.class));
        finish();
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executorService != null) {
            executorService.shutdown();
        }
    }
}