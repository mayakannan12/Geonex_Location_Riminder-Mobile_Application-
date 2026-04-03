package com.example.geonex;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.Ignore;

@Entity(tableName = "reminders")
public class Reminder {

    @PrimaryKey(autoGenerate = true)
    private int id;

    @ColumnInfo(name = "title")
    private String title;

    @ColumnInfo(name = "location_name")
    private String locationName;

    @ColumnInfo(name = "latitude")
    private double latitude;

    @ColumnInfo(name = "longitude")
    private double longitude;

    @ColumnInfo(name = "radius")
    private float radius;

    @ColumnInfo(name = "category")
    private String category;

    @ColumnInfo(name = "is_completed")
    private boolean isCompleted;

    @ColumnInfo(name = "created_at")
    private long createdAt;

    @ColumnInfo(name = "is_recurring")
    private boolean isRecurring;

    @ColumnInfo(name = "recurrence_rule")
    private String recurrenceRule;

    @ColumnInfo(name = "custom_interval")
    private int customInterval;

    @ColumnInfo(name = "custom_interval_unit")
    private String customIntervalUnit;

    // No-arg constructor
    public Reminder() {
        this.createdAt = System.currentTimeMillis();
        this.isCompleted = false;
        this.radius = 500;
        this.isRecurring = false;
        this.recurrenceRule = "never";
        this.customInterval = 0;
        this.customIntervalUnit = "";
    }

    @Ignore
    public Reminder(String title, String locationName, double latitude,
                    double longitude, String category) {
        this.title = title;
        this.locationName = locationName;
        this.latitude = latitude;
        this.longitude = longitude;
        this.category = category;
        this.radius = 500;
        this.isCompleted = false;
        this.createdAt = System.currentTimeMillis();
        this.isRecurring = false;
        this.recurrenceRule = "never";
        this.customInterval = 0;
        this.customIntervalUnit = "";
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getLocationName() { return locationName; }
    public void setLocationName(String locationName) { this.locationName = locationName; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    public float getRadius() { return radius; }
    public void setRadius(float radius) { this.radius = radius; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public boolean isCompleted() { return isCompleted; }
    public void setCompleted(boolean completed) { isCompleted = completed; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public boolean isRecurring() { return isRecurring; }
    public void setRecurring(boolean recurring) { isRecurring = recurring; }

    public String getRecurrenceRule() { return recurrenceRule; }
    public void setRecurrenceRule(String recurrenceRule) { this.recurrenceRule = recurrenceRule; }

    public int getCustomInterval() { return customInterval; }
    public void setCustomInterval(int customInterval) { this.customInterval = customInterval; }

    public String getCustomIntervalUnit() { return customIntervalUnit; }
    public void setCustomIntervalUnit(String customIntervalUnit) { this.customIntervalUnit = customIntervalUnit; }
}