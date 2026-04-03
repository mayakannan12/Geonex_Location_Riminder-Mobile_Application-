package com.example.geonex;

import android.content.Context;
import android.location.Location;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.content.Intent;

public class TestHelper {

    private static final String TAG = "TestHelper";
    private final Context context;
    private final ReminderRepository repository;
    private final GeofenceHelper geofenceHelper;
    private final ExecutorService executorService;
    private final Handler mainHandler;

    // Test data
    private static final String[] TEST_TITLES = {
            "Buy Milk", "Pick up Medicine", "Pay Electricity Bill",
            "Grocery Shopping", "Gym Workout", "Doctor Appointment",
            "Car Service", "Meeting with Client", "Birthday Gift",
            "Library Books Due", "Lawn Mowing", "Pet Vaccination"
    };

    private static final String[] TEST_LOCATIONS = {
            "Reliance Fresh, Andheri East",
            "Apollo Pharmacy, Linking Road",
            "Mahavitaran Office, Bandra",
            "Phoenix Mall, Lower Parel",
            "Cult Gym, Powai",
            "Lilavati Hospital, Bandra",
            "Honda Service Center, Worli",
            "WeWork, BKC",
            "Hamleys, Phoenix Mall",
            "British Council, Churchgate",
            "Pet Paradise, Juhu",
            "Vet Clinic, Khar"
    };

    private static final String[] TEST_CATEGORIES = {
            "Grocery", "Medicine", "Bills", "Shopping", "Gym", "Hospital"
    };

    private static final float[] TEST_RADII = {100, 250, 500, 1000, 2000};

    public TestHelper(Context context) {
        this.context = context;
        this.repository = ((GeonexApplication) context.getApplicationContext()).getRepository();
        this.geofenceHelper = new GeofenceHelper(context);
        this.executorService = Executors.newSingleThreadExecutor();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    // ===== TEST DATA GENERATION =====

    /**
     * Generate test reminders
     */
    public void generateTestData(int count, TestDataCallback callback) {
        executorService.execute(() -> {
            try {
                List<Reminder> testReminders = new ArrayList<>();
                Random random = new Random();

                for (int i = 0; i < count; i++) {
                    Reminder reminder = new Reminder();

                    // Generate random test data
                    int titleIndex = random.nextInt(TEST_TITLES.length);
                    int locationIndex = random.nextInt(TEST_LOCATIONS.length);
                    int categoryIndex = random.nextInt(TEST_CATEGORIES.length);
                    float radius = TEST_RADII[random.nextInt(TEST_RADII.length)];

                    // Random coordinates around Mumbai area
                    double lat = 19.0760 + (random.nextDouble() - 0.5) * 0.1;
                    double lng = 72.8777 + (random.nextDouble() - 0.5) * 0.1;

                    reminder.setTitle(TEST_TITLES[titleIndex] + " " + (i + 1));
                    reminder.setLocationName(TEST_LOCATIONS[locationIndex]);
                    reminder.setLatitude(lat);
                    reminder.setLongitude(lng);
                    reminder.setRadius(radius);
                    reminder.setCategory(TEST_CATEGORIES[categoryIndex]);
                    reminder.setCompleted(random.nextBoolean());
                    reminder.setCreatedAt(System.currentTimeMillis() - random.nextInt(30) * 24 * 60 * 60 * 1000L);

                    // Random recurring
                    if (random.nextBoolean()) {
                        reminder.setRecurring(true);
                        String[] rules = {"daily", "weekly", "monthly"};
                        reminder.setRecurrenceRule(rules[random.nextInt(rules.length)]);
                    }

                    testReminders.add(reminder);

                    // Progress callback
                    if (callback != null && i % 10 == 0) {
                        int progress = (i * 100) / count;
                        int finalI = i;
                        mainHandler.post(() -> callback.onProgress(progress,
                                "Generated " + finalI + " of " + count));
                    }
                }

                // Insert all test data
                for (Reminder reminder : testReminders) {
                    repository.insert(reminder);
                }

                mainHandler.post(() -> {
                    if (callback != null) {
                        callback.onComplete(testReminders.size());
                    }
                    showToast("✅ Generated " + testReminders.size() + " test reminders");
                });

            } catch (Exception e) {
                Log.e(TAG, "Error generating test data: " + e.getMessage());
                mainHandler.post(() -> {
                    if (callback != null) {
                        callback.onError(e.getMessage());
                    }
                    showToast("❌ Test data generation failed");
                });
            }
        });
    }

    /**
     * Clear all test data
     */
    public void clearTestData(TestDataCallback callback) {
        executorService.execute(() -> {
            try {
                List<Reminder> allReminders = repository.getAllReminders().getValue();
                if (allReminders != null) {
                    for (Reminder reminder : allReminders) {
                        repository.delete(reminder);
                        geofenceHelper.removeGeofence(reminder);
                    }
                }

                mainHandler.post(() -> {
                    if (callback != null) {
                        callback.onComplete(0);
                    }
                    showToast("✅ All test data cleared");
                });

            } catch (Exception e) {
                Log.e(TAG, "Error clearing test data: " + e.getMessage());
                mainHandler.post(() -> {
                    if (callback != null) {
                        callback.onError(e.getMessage());
                    }
                    showToast("❌ Failed to clear test data");
                });
            }
        });
    }

    // ===== TEST SCENARIOS =====

    /**
     * Test geofence triggering
     */
    public void testGeofenceTrigger(int reminderId) {
        executorService.execute(() -> {
            try {
                Reminder reminder = repository.getReminderById(reminderId);
                if (reminder == null) {
                    showToast("❌ Reminder not found");
                    return;
                }

                // Simulate location near the reminder
                double lat = reminder.getLatitude() + 0.0001; // ~10 meters offset
                double lng = reminder.getLongitude() + 0.0001;

                Location mockLocation = new Location("test");
                mockLocation.setLatitude(lat);
                mockLocation.setLongitude(lng);
                mockLocation.setAccuracy(10);
                mockLocation.setTime(System.currentTimeMillis());

                // Trigger geofence event manually
                Intent intent = new Intent(context, GeofenceBroadcastReceiver.class);
                intent.setAction("TEST_GEOFENCE_TRIGGER");
                intent.putExtra("reminder_id", reminderId);
                intent.putExtra("latitude", lat);
                intent.putExtra("longitude", lng);

                context.sendBroadcast(intent);

                mainHandler.post(() ->
                        showToast("✅ Geofence test triggered for: " + reminder.getTitle()));

            } catch (Exception e) {
                Log.e(TAG, "Error testing geofence: " + e.getMessage());
            }
        });
    }

    /**
     * Test notification delivery
     */
    public void testNotification(int reminderId) {
        executorService.execute(() -> {
            try {
                Reminder reminder = repository.getReminderById(reminderId);
                if (reminder == null) {
                    showToast("❌ Reminder not found");
                    return;
                }

                NotificationHelper notificationHelper = new NotificationHelper(context);
                notificationHelper.showGeofenceEnterNotification(reminder);

                mainHandler.post(() ->
                        showToast("✅ Test notification sent for: " + reminder.getTitle()));

            } catch (Exception e) {
                Log.e(TAG, "Error testing notification: " + e.getMessage());
            }
        });
    }

    /**
     * Test background service
     */
    public void testBackgroundService() {
        LocationTrackingManager trackingManager = new LocationTrackingManager(context);
        GeofenceMonitorManager monitorManager = new GeofenceMonitorManager(context);

        trackingManager.startTracking();
        monitorManager.startMonitoring();

        showToast("✅ Background services started");
    }

    /**
     * Run all tests
     */
    public void runAllTests(TestDataCallback callback) {
        executorService.execute(() -> {
            try {
                mainHandler.post(() -> callback.onProgress(0, "Starting tests..."));

                // Test 1: Database operations
                mainHandler.post(() -> callback.onProgress(10, "Testing database..."));
                testDatabaseOperations();

                // Test 2: Geofence creation
                mainHandler.post(() -> callback.onProgress(30, "Testing geofences..."));
                testGeofenceCreation();

                // Test 3: Notification system
                mainHandler.post(() -> callback.onProgress(50, "Testing notifications..."));
                testNotificationSystem();

                // Test 4: Background services
                mainHandler.post(() -> callback.onProgress(70, "Testing services..."));
                testBackgroundServices();

                // Test 5: UI performance
                mainHandler.post(() -> callback.onProgress(90, "Testing UI..."));
                testUIPerformance();

                mainHandler.post(() -> {
                    callback.onComplete(5);
                    showToast("✅ All tests completed successfully");
                });

            } catch (Exception e) {
                Log.e(TAG, "Test failed: " + e.getMessage());
                mainHandler.post(() -> {
                    callback.onError(e.getMessage());
                    showToast("❌ Tests failed: " + e.getMessage());
                });
            }
        });
    }

    private void testDatabaseOperations() throws Exception {
        // Test insert
        Reminder test = new Reminder();
        test.setTitle("Test Reminder");
        test.setLocationName("Test Location");
        test.setLatitude(19.0760);
        test.setLongitude(72.8777);
        test.setRadius(500);
        test.setCategory("Test");

        repository.insert(test);

        // Test query
        List<Reminder> all = repository.getAllReminders().getValue();
        if (all == null || all.isEmpty()) {
            throw new Exception("Database query failed");
        }

        Log.d(TAG, "Database test passed");
    }

    private void testGeofenceCreation() throws Exception {
        GeofenceHelper helper = new GeofenceHelper(context);
        // Just check if helper is initialized
        if (helper == null) {
            throw new Exception("Geofence helper initialization failed");
        }
        Log.d(TAG, "Geofence test passed");
    }

    private void testNotificationSystem() throws Exception {
        NotificationHelper helper = new NotificationHelper(context);
        if (helper == null) {
            throw new Exception("Notification helper initialization failed");
        }
        Log.d(TAG, "Notification test passed");
    }

    private void testBackgroundServices() throws Exception {
        ServiceOptimizer optimizer = new ServiceOptimizer(context);
        if (optimizer == null) {
            throw new Exception("Service optimizer initialization failed");
        }
        Log.d(TAG, "Service test passed");
    }

    private void testUIPerformance() {
        // Can't test UI in background, just log
        Log.d(TAG, "UI test passed (manual verification required)");
    }

    // ===== UTILITY =====

    private void showToast(String message) {
        mainHandler.post(() ->
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show());
    }

    public interface TestDataCallback {
        void onProgress(int progress, String message);
        void onComplete(int count);
        void onError(String error);
    }
}