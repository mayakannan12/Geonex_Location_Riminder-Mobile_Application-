package com.example.geonex;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ReminderAdapter extends RecyclerView.Adapter<ReminderAdapter.ReminderViewHolder> {

    private static final String TAG = "ReminderAdapter";

    private List<Reminder> reminders = new ArrayList<>();
    private List<Reminder> allReminders = new ArrayList<>(); // For filtering
    private final Context context;
    private OnReminderClickListener listener;
    private OnReminderLongClickListener longClickListener;
    private OnSwipeActionListener swipeActionListener;
    private String currentFilter = "all";
    private String currentSearchQuery = "";

    public void setScrolling(boolean b) {
    }

    // ===== INTERFACES =====
    public interface OnReminderClickListener {
        void onReminderClick(Reminder reminder); // Opens detail view
        void onReminderMenuClick(Reminder reminder, View view);
    }

    public interface OnReminderLongClickListener {
        void onReminderLongClick(Reminder reminder);
    }

    public interface OnSwipeActionListener {
        void onMarkAsCompleted(Reminder reminder, int position);
        void onDelete(Reminder reminder, int position);
    }

    // ===== CONSTRUCTOR =====
    public ReminderAdapter(Context context) {
        this.context = context;
    }

    // ===== SETTERS =====
    public void setOnReminderClickListener(OnReminderClickListener listener) {
        this.listener = listener;
    }

    public void setOnReminderLongClickListener(OnReminderLongClickListener listener) {
        this.longClickListener = listener;
    }

    public void setOnSwipeActionListener(OnSwipeActionListener listener) {
        this.swipeActionListener = listener;
    }

    // ===== VIEW HOLDER CREATION =====
    @NonNull
    @Override
    public ReminderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_reminder, parent, false);
        return new ReminderViewHolder(view);
    }

    // ===== BIND VIEW HOLDER =====
    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull ReminderViewHolder holder, int position) {
        if (position < 0 || position >= reminders.size()) {
            return;
        }

        Reminder reminder = reminders.get(position);
        if (reminder == null) {
            return;
        }

        // Set title
        if (holder.tvTitle != null) {
            holder.tvTitle.setText(reminder.getTitle());
        }

        // Set location
        if (holder.tvLocation != null) {
            holder.tvLocation.setText(reminder.getLocationName());
        }

        // Set category badge
        if (holder.tvCategoryBadge != null) {
            String category = reminder.getCategory();
            holder.tvCategoryBadge.setText(category);
            setCategoryBadgeColor(holder.tvCategoryBadge, category);
        }

        // Set radius
        if (holder.tvRadius != null) {
            float radius = reminder.getRadius();
            if (radius >= 1000) {
                holder.tvRadius.setText(String.format(Locale.getDefault(), "%.1f km", radius / 1000));
            } else {
                holder.tvRadius.setText(String.format(Locale.getDefault(), "%.0f m", radius));
            }
        }

        // Set time
        if (holder.tvTime != null) {
            long createdAt = reminder.getCreatedAt();
            holder.tvTime.setText(formatTime(createdAt));
        }

        // Set recurring badge
        if (holder.tvRecurringBadge != null) {
            if (reminder.isRecurring()) {
                holder.tvRecurringBadge.setVisibility(View.VISIBLE);
                String rule = reminder.getRecurrenceRule();
                if ("daily".equals(rule)) {
                    holder.tvRecurringBadge.setText("🔄 Daily");
                } else if ("weekly".equals(rule)) {
                    holder.tvRecurringBadge.setText("🔄 Weekly");
                } else if ("monthly".equals(rule)) {
                    holder.tvRecurringBadge.setText("🔄 Monthly");
                } else if ("custom".equals(rule)) {
                    int interval = reminder.getCustomInterval();
                    String unit = reminder.getCustomIntervalUnit();
                    holder.tvRecurringBadge.setText("🔄 Every " + interval + " " + unit);
                } else {
                    holder.tvRecurringBadge.setText("🔄 Recurring");
                }
            } else {
                holder.tvRecurringBadge.setVisibility(View.GONE);
            }
        }

        // Set completed overlay
        if (holder.completedOverlay != null && holder.cardReminder != null) {
            if (reminder.isCompleted()) {
                holder.completedOverlay.setVisibility(View.VISIBLE);
                holder.cardReminder.setAlpha(0.7f);
            } else {
                holder.completedOverlay.setVisibility(View.GONE);
                holder.cardReminder.setAlpha(1.0f);
            }
        }

        // ===== CLICK LISTENERS =====
        // Item click - opens detail view
        holder.itemView.setOnClickListener(v -> {
            if (listener != null && reminder != null) {
                listener.onReminderClick(reminder);
            }
        });

        // Long click - for additional options
        holder.itemView.setOnLongClickListener(v -> {
            if (longClickListener != null && reminder != null) {
                longClickListener.onReminderLongClick(reminder);
                return true;
            }
            return false;
        });

        // Menu button click
        if (holder.btnMenu != null) {
            holder.btnMenu.setOnClickListener(v -> {
                if (listener != null && reminder != null) {
                    listener.onReminderMenuClick(reminder, holder.btnMenu);
                }
            });
        }
    }

    // ===== ITEM COUNT =====
    @Override
    public int getItemCount() {
        return reminders.size();
    }

    // ===== DATA MANAGEMENT =====
    @SuppressLint("NotifyDataSetChanged")
    public void setReminders(List<Reminder> reminders) {
        this.reminders = new ArrayList<>(reminders);
        this.allReminders = new ArrayList<>(reminders);
        notifyDataSetChanged();
    }

    /**
     * Get reminder at specific position
     */
    public Reminder getReminderAt() {
        int position = 0;
        if (position >= 0 && position < reminders.size()) {
            return reminders.get(position);
        }
        return null;
    }

    /**
     * Remove reminder at position
     */
    @SuppressLint("NotifyDataSetChanged")
    public void removeReminderAt(int position) {
        if (position >= 0 && position < reminders.size()) {
            Reminder reminder = reminders.get(position);
            allReminders.remove(reminder);
            reminders.remove(position);
            notifyItemRemoved(position);
        }
    }

    /**
     * Update reminder at position
     */
    public void updateReminderAt(int position, Reminder updatedReminder) {
        if (position >= 0 && position < reminders.size()) {
            reminders.set(position, updatedReminder);
            for (int i = 0; i < allReminders.size(); i++) {
                if (allReminders.get(i).getId() == updatedReminder.getId()) {
                    allReminders.set(i, updatedReminder);
                    break;
                }
            }
            notifyItemChanged(position);
        }
    }

    // ===== FILTERING METHODS =====
    /**
     * Filter reminders based on search query
     * @param query The search text
     */
    public void filter(String query) {
        currentSearchQuery = query.toLowerCase().trim();
        performFiltering();
    }

    /**
     * Filter reminders based on category
     * @param category The category to filter by
     */
    public void filterByCategory(String category) {
        currentFilter = category;
        performFiltering();
    }

    /**
     * Perform combined filtering (category + search)
     */
    @SuppressLint("NotifyDataSetChanged")
    private void performFiltering() {
        List<Reminder> filteredList = new ArrayList<>();

        for (Reminder reminder : allReminders) {
            boolean matchesCategory = currentFilter.equals("all") ||
                    reminder.getCategory().equalsIgnoreCase(currentFilter);

            boolean matchesSearch = currentSearchQuery.isEmpty() ||
                    reminder.getTitle().toLowerCase().contains(currentSearchQuery) ||
                    reminder.getLocationName().toLowerCase().contains(currentSearchQuery);

            if (matchesCategory && matchesSearch) {
                filteredList.add(reminder);
            }
        }

        reminders.clear();
        reminders.addAll(filteredList);
        notifyDataSetChanged();
    }

    /**
     * Clear all filters and show all reminders
     */
    @SuppressLint("NotifyDataSetChanged")
    public void clearFilters() {
        currentFilter = "all";
        currentSearchQuery = "";
        reminders.clear();
        reminders.addAll(allReminders);
        notifyDataSetChanged();
    }

    /**
     * Get current filter state
     */
    public String getCurrentFilter() {
        return currentFilter;
    }

    /**
     * Get current search query
     */
    public String getCurrentSearchQuery() {
        return currentSearchQuery;
    }

    /**
     * Get count of filtered reminders
     */
    public int getFilteredCount() {
        return reminders.size();
    }

    /**
     * Get count of total reminders
     */
    public int getTotalCount() {
        return allReminders.size();
    }

    // ===== CRUD OPERATIONS =====
    /**
     * Update a single reminder in the list
     */
    public void updateReminder(Reminder reminder) {
        for (int i = 0; i < allReminders.size(); i++) {
            if (allReminders.get(i).getId() == reminder.getId()) {
                allReminders.set(i, reminder);
                break;
            }
        }
        performFiltering();
    }

    /**
     * Remove a reminder from the list
     */
    @SuppressLint("NotifyDataSetChanged")
    public void removeReminder(Reminder reminder) {
        allReminders.remove(reminder);
        reminders.remove(reminder);
        notifyDataSetChanged();
    }

    /**
     * Add a new reminder to the list
     */
    public void addReminder(Reminder reminder) {
        allReminders.add(0, reminder);
        performFiltering();
    }

    // ===== UTILITY METHODS =====
    private void setCategoryBadgeColor(TextView badge, String category) {
        int bgColor;
        int textColor;

        switch (category.toLowerCase()) {
            case "grocery":
                bgColor = context.getColor(R.color.grocery_bg);
                textColor = context.getColor(R.color.grocery_text);
                break;
            case "medicine":
                bgColor = context.getColor(R.color.medicine_bg);
                textColor = context.getColor(R.color.medicine_text);
                break;
            case "bills":
                bgColor = context.getColor(R.color.bills_bg);
                textColor = context.getColor(R.color.bills_text);
                break;
            case "shopping":
                bgColor = context.getColor(R.color.shopping_bg);
                textColor = context.getColor(R.color.shopping_text);
                break;
            case "work":
                bgColor = context.getColor(R.color.work_bg);
                textColor = context.getColor(R.color.work_text);
                break;
            default:
                bgColor = context.getColor(R.color.background);
                textColor = context.getColor(R.color.text_secondary);
                break;
        }

        badge.setBackgroundColor(bgColor);
        badge.setTextColor(textColor);
    }

    private String formatTime(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("MMM d, h:mm a", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }

    // ===== VIEW HOLDER CLASS =====
    public static class ReminderViewHolder extends RecyclerView.ViewHolder {
        CardView cardReminder;
        TextView tvCategoryBadge, tvRecurringBadge, tvTitle, tvLocation, tvTime, tvRadius, btnMenu;
        FrameLayout mapPreview, completedOverlay;

        @SuppressLint("WrongViewCast")
        public ReminderViewHolder(@NonNull View itemView) {
            super(itemView);

            try {
                cardReminder = itemView.findViewById(R.id.cardReminder);
                tvCategoryBadge = itemView.findViewById(R.id.tvCategoryBadge);
                tvRecurringBadge = itemView.findViewById(R.id.tvRecurringBadge);
                tvTitle = itemView.findViewById(R.id.tvTitle);
                tvLocation = itemView.findViewById(R.id.tvLocation);
                tvTime = itemView.findViewById(R.id.tvTime);
                tvRadius = itemView.findViewById(R.id.tvRadius);
                btnMenu = itemView.findViewById(R.id.btnMenu);
                mapPreview = itemView.findViewById(R.id.mapPreview);
                completedOverlay = itemView.findViewById(R.id.completedOverlay);

                // Debug logging
                if (tvTitle == null) Log.e(TAG, "tvTitle is null - check ID in layout");
                if (tvLocation == null) Log.e(TAG, "tvLocation is null - check ID in layout");
                if (tvCategoryBadge == null) Log.e(TAG, "tvCategoryBadge is null - check ID in layout");
                if (btnMenu == null) Log.e(TAG, "btnMenu is null - check ID in layout");
                if (tvRadius == null) Log.e(TAG, "tvRadius is null - check ID in layout");

            } catch (ClassCastException e) {
                Log.e(TAG, "ClassCastException in ViewHolder: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
}