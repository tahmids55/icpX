package com.icpx.android.worker;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.icpx.android.firebase.FirebaseSyncService;
import com.icpx.android.utils.NotificationHelper;

import java.util.concurrent.CountDownLatch;

public class SyncWorker extends Worker {

    public SyncWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        FirebaseSyncService syncService = new FirebaseSyncService(getApplicationContext());
        final CountDownLatch latch = new CountDownLatch(1);
        final Result[] result = {Result.success()};

        syncService.performFullSync(new FirebaseSyncService.SyncCallback() {
            @Override
            public void onSuccess(String message) {
                NotificationHelper.sendSyncNotification(getApplicationContext(), "Auto-Sync Complete", "Successfully synced with cloud.");
                latch.countDown();
            }

            @Override
            public void onFailure(Exception e) {
                NotificationHelper.sendSyncNotification(getApplicationContext(), "Auto-Sync Failed", e.getMessage());
                result[0] = Result.retry();
                latch.countDown();
            }
        });

        try {
            latch.await();
        } catch (InterruptedException e) {
            return Result.failure();
        }

        return result[0];
    }
}
