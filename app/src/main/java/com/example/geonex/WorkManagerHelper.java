package com.example.geonex;

import android.content.Context;
import android.util.Log;

import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.util.concurrent.TimeUnit;

public class WorkManagerHelper {

    private static final String TAG = "WorkManagerHelper";
    private final Context context;
    private final WorkManager workManager;

    // Work tags
    public static final String TAG_SERVICE_CLEANUP = "service_cleanup";
    public static final String TAG_GEOFENCE_REFRESH = "geofence_refresh";
    public static final String TAG_LOCATION_CHECK = "location_check";
    public static final String TAG_REMINDER_CHECK = "reminder_check";

    public WorkManagerHelper(Context context) {
        this.context = context;
        this.workManager = WorkManager.getInstance(context);
    }

    /**
     * Schedule periodic service cleanup
     */
    public void scheduleServiceCleanup() {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                .setRequiresBatteryNotLow(true)
                .build();

        PeriodicWorkRequest cleanupRequest = new PeriodicWorkRequest.Builder(
                ServiceCleanupWorker.class,
                30, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .setInitialDelay(5, TimeUnit.MINUTES)
                .addTag(TAG_SERVICE_CLEANUP)
                .build();

        workManager.enqueueUniquePeriodicWork(
                TAG_SERVICE_CLEANUP,
                ExistingPeriodicWorkPolicy.KEEP,
                cleanupRequest
        );

        Log.d(TAG, "Scheduled periodic service cleanup every 30 minutes");
    }

    /**
     * Schedule geofence refresh (every hour)
     */
    public void scheduleGeofenceRefresh() {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                .setRequiresBatteryNotLow(true)
                .build();

        PeriodicWorkRequest refreshRequest = new PeriodicWorkRequest.Builder(
                GeofenceRefreshWorker.class,
                1, TimeUnit.HOURS)
                .setConstraints(constraints)
                .setInitialDelay(10, TimeUnit.MINUTES)
                .addTag(TAG_GEOFENCE_REFRESH)
                .build();

        workManager.enqueueUniquePeriodicWork(
                TAG_GEOFENCE_REFRESH,
                ExistingPeriodicWorkPolicy.KEEP,
                refreshRequest
        );

        Log.d(TAG, "Scheduled geofence refresh every hour");
    }

    /**
     * Schedule one-time delayed task
     */
    public void scheduleOneTimeTask(Class<? extends androidx.work.Worker> workerClass,
                                    String tag, long delay, TimeUnit unit) {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                .setRequiresBatteryNotLow(true)
                .build();

        OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(workerClass)
                .setConstraints(constraints)
                .setInitialDelay(delay, unit)
                .addTag(tag)
                .build();

        workManager.enqueueUniqueWork(
                tag,
                ExistingWorkPolicy.REPLACE,
                workRequest
        );

        Log.d(TAG, "Scheduled one-time task: " + tag + " in " + delay + " " + unit);
    }

    /**
     * Cancel all work with tag
     */
    public void cancelWorkByTag(String tag) {
        workManager.cancelAllWorkByTag(tag);
        Log.d(TAG, "Cancelled work with tag: " + tag);
    }

    /**
     * Cancel all periodic work
     */
    public void cancelAllPeriodicWork() {
        workManager.cancelUniqueWork(TAG_SERVICE_CLEANUP);
        workManager.cancelUniqueWork(TAG_GEOFENCE_REFRESH);
        Log.d(TAG, "Cancelled all periodic work");
    }

    /**
     * Check if work is scheduled
     */
    public void getWorkInfosByTag(String tag) {
        workManager.getWorkInfosByTagLiveData(tag).observeForever(workInfos -> {
            for (androidx.work.WorkInfo workInfo : workInfos) {
                Log.d(TAG, "Work: " + workInfo.getId() + " State: " + workInfo.getState());
            }
        });
    }
}