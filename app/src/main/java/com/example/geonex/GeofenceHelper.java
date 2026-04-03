package com.example.geonex;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import java.util.ArrayList;
import java.util.List;

public class GeofenceHelper {

    private static final String TAG = "GeofenceHelper";
    private static final float GEOFENCE_RADIUS = 500; // Default radius in meters
    private static final long GEOFENCE_EXPIRATION = Geofence.NEVER_EXPIRE;
    private static final String PREFS_NAME = "GeofencePrefs";
    private static final String KEY_GEOFENCE_IDS = "geofence_ids";

    private Context context;
    private GeofencingClient geofencingClient;
    private PendingIntent geofencePendingIntent;

    public GeofenceHelper(Context context) {
        this.context = context;
        this.geofencingClient = LocationServices.getGeofencingClient(context);
    }

    // Create a geofence for a reminder
    public Geofence createGeofence(Reminder reminder) {
        String geofenceId = String.valueOf(reminder.getId());

        float radius = reminder.getRadius();
        if (radius <= 0) {
            radius = GEOFENCE_RADIUS;
        }

        return new Geofence.Builder()
                .setRequestId(geofenceId)
                .setCircularRegion(
                        reminder.getLatitude(),
                        reminder.getLongitude(),
                        radius
                )
                .setExpirationDuration(GEOFENCE_EXPIRATION)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER |
                        Geofence.GEOFENCE_TRANSITION_EXIT)
                .setLoiteringDelay(30000) // 30 seconds
                .build();
    }

    // Create geofencing request
    public GeofencingRequest createGeofencingRequest(Geofence geofence) {
        List<Geofence> geofenceList = new ArrayList<>();
        geofenceList.add(geofence);

        return new GeofencingRequest.Builder()
                .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
                .addGeofences(geofenceList)
                .build();
    }

    // Create geofencing request for multiple geofences
    public GeofencingRequest createGeofencingRequest(List<Geofence> geofences) {
        return new GeofencingRequest.Builder()
                .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
                .addGeofences(geofences)
                .build();
    }

    // Get pending intent for geofence transitions
    PendingIntent getGeofencePendingIntent() {
        if (geofencePendingIntent != null) {
            return geofencePendingIntent;
        }

        Intent intent = new Intent(context, GeofenceBroadcastReceiver.class);

        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            flags |= PendingIntent.FLAG_MUTABLE;
        }

        geofencePendingIntent = PendingIntent.getBroadcast(
                context,
                0,
                intent,
                flags
        );

        return geofencePendingIntent;
    }

    /**
     * Get pending intent for geofence transitions (public version)
     */
    public PendingIntent getPublicGeofencePendingIntent() {
        return getGeofencePendingIntent();
    }

    /**
     * Check if geofence is already registered
     */
    public boolean isGeofenceRegistered(int reminderId) {
        List<Integer> ids = getAllGeofenceIds();
        return ids.contains(reminderId);
    }

    /**
     * Get all registered geofence IDs (public version)
     */
    public List<Integer> getAllRegisteredGeofenceIds() {
        return getAllGeofenceIds();
    }

    /**
     * Remove geofence by ID
     */
    public void removeGeofenceById(int reminderId) {
        List<String> geofenceIds = new ArrayList<>();
        geofenceIds.add(String.valueOf(reminderId));

        geofencingClient.removeGeofences(geofenceIds)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d(TAG, "Geofence removed: " + reminderId);
                        removeGeofenceId(reminderId);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(Exception e) {
                        Log.e(TAG, "Failed to remove geofence: " + e.getMessage());
                    }
                });
    }

    // Add a single geofence
    public void addGeofence(Reminder reminder) {
        if (!checkLocationPermission()) {
            Log.e(TAG, "Location permission not granted");
            return;
        }

        Geofence geofence = createGeofence(reminder);
        GeofencingRequest request = createGeofencingRequest(geofence);
        PendingIntent pendingIntent = getGeofencePendingIntent();

        geofencingClient.addGeofences(request, pendingIntent)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d(TAG, "Geofence added successfully for reminder: " + reminder.getId());

                        // Save geofence ID to SharedPreferences for tracking
                        saveGeofenceId(reminder.getId());
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(Exception e) {
                        Log.e(TAG, "Failed to add geofence: " + e.getMessage());
                    }
                });
    }

    // Add multiple geofences
    public void addGeofences(List<Reminder> reminders) {
        if (!checkLocationPermission()) {
            Log.e(TAG, "Location permission not granted");
            return;
        }

        if (reminders == null || reminders.isEmpty()) {
            return;
        }

        List<Geofence> geofenceList = new ArrayList<>();
        for (Reminder reminder : reminders) {
            if (!reminder.isCompleted()) {
                geofenceList.add(createGeofence(reminder));
            }
        }

        if (geofenceList.isEmpty()) {
            return;
        }

        GeofencingRequest request = createGeofencingRequest(geofenceList);
        PendingIntent pendingIntent = getGeofencePendingIntent();

        geofencingClient.addGeofences(request, pendingIntent)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d(TAG, geofenceList.size() + " geofences added successfully");

                        // Save all geofence IDs
                        for (Reminder reminder : reminders) {
                            saveGeofenceId(reminder.getId());
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(Exception e) {
                        Log.e(TAG, "Failed to add geofences: " + e.getMessage());
                    }
                });
    }

    // Remove a single geofence
    public void removeGeofence(Reminder reminder) {
        List<String> geofenceIds = new ArrayList<>();
        geofenceIds.add(String.valueOf(reminder.getId()));

        geofencingClient.removeGeofences(geofenceIds)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d(TAG, "Geofence removed successfully for reminder: " + reminder.getId());

                        // Remove from SharedPreferences
                        removeGeofenceId(reminder.getId());
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(Exception e) {
                        Log.e(TAG, "Failed to remove geofence: " + e.getMessage());
                    }
                });
    }

    // Remove all geofences
    public void removeAllGeofences() {
        geofencingClient.removeGeofences(getGeofencePendingIntent())
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d(TAG, "All geofences removed successfully");

                        // Clear all geofence IDs from SharedPreferences
                        clearAllGeofenceIds();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(Exception e) {
                        Log.e(TAG, "Failed to remove all geofences: " + e.getMessage());
                    }
                });
    }

    // Re-register all geofences from database (used after reboot) - FIXED VERSION
    public void reregisterAllGeofences() {
        // Run on background thread to avoid main thread violation
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Log.d(TAG, "Re-registering geofences on background thread");

                    ReminderRepository repository = ((GeonexApplication) context.getApplicationContext()).getRepository();

                    // Get saved geofence IDs from SharedPreferences
                    SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                    String idsString = prefs.getString(KEY_GEOFENCE_IDS, "");

                    List<Reminder> activeReminders = new ArrayList<>();

                    if (!idsString.isEmpty()) {
                        String[] idArray = idsString.split(",");
                        Log.d(TAG, "Found " + idArray.length + " saved geofence IDs");

                        for (String idStr : idArray) {
                            try {
                                int id = Integer.parseInt(idStr);
                                Reminder reminder = repository.getReminderById(id);

                                if (reminder != null && !reminder.isCompleted()) {
                                    activeReminders.add(reminder);
                                    Log.d(TAG, "Loaded reminder: " + reminder.getTitle());
                                }
                            } catch (NumberFormatException e) {
                                Log.e(TAG, "Invalid ID format: " + idStr);
                            }
                        }
                    }

                    if (!activeReminders.isEmpty()) {
                        Log.d(TAG, "Re-registering " + activeReminders.size() + " geofences after reboot");

                        // Re-register geofences
                        addGeofences(activeReminders);
                    } else {
                        Log.d(TAG, "No active reminders found to re-register");
                    }

                } catch (Exception e) {
                    Log.e(TAG, "Error re-registering geofences: " + e.getMessage());
                }
            }
        }).start();
    }

    // Check location permission
    private boolean checkLocationPermission() {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    // Save geofence ID to SharedPreferences
    private void saveGeofenceId(int reminderId) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        // Get existing IDs
        String idsString = prefs.getString(KEY_GEOFENCE_IDS, "");
        if (!idsString.isEmpty()) {
            idsString += "," + reminderId;
        } else {
            idsString = String.valueOf(reminderId);
        }

        editor.putString(KEY_GEOFENCE_IDS, idsString);
        editor.apply();
    }

    // Remove geofence ID from SharedPreferences
    private void removeGeofenceId(int reminderId) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String idsString = prefs.getString(KEY_GEOFENCE_IDS, "");

        if (!idsString.isEmpty()) {
            String[] ids = idsString.split(",");
            StringBuilder newIds = new StringBuilder();

            for (String id : ids) {
                if (!id.equals(String.valueOf(reminderId))) {
                    if (newIds.length() > 0) {
                        newIds.append(",");
                    }
                    newIds.append(id);
                }
            }

            prefs.edit().putString(KEY_GEOFENCE_IDS, newIds.toString()).apply();
        }
    }

    // Clear all geofence IDs
    private void clearAllGeofenceIds() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().remove(KEY_GEOFENCE_IDS).apply();
    }

    // Get all geofence IDs from SharedPreferences
    List<Integer> getAllGeofenceIds() {
        List<Integer> ids = new ArrayList<>();
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String idsString = prefs.getString(KEY_GEOFENCE_IDS, "");

        if (!idsString.isEmpty()) {
            String[] idArray = idsString.split(",");
            for (String id : idArray) {
                try {
                    ids.add(Integer.parseInt(id));
                } catch (NumberFormatException e) {
                    Log.e(TAG, "Invalid geofence ID: " + id);
                }
            }
        }

        return ids;
    }

    /**
     * Remove geofence by ID
     */

}