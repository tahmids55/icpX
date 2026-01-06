package com.icpx.android.service;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

/**
 * Worker for sending contest reminder notifications
 */
public class ContestReminderWorker extends Worker {

    private static final String TAG = "ContestReminderWorker";

    public ContestReminderWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            int contestId = getInputData().getInt("contestId", 0);
            String contestName = getInputData().getString("contestName");
            long startTime = getInputData().getLong("startTime", 0);
            int hours = getInputData().getInt("hours", 24);

            Log.d(TAG, "Sending " + hours + " hour reminder for contest: " + contestName);

            NotificationHelper.showContestReminder(
                    getApplicationContext(),
                    contestId,
                    contestName,
                    startTime,
                    hours
            );

            return Result.success();
        } catch (Exception e) {
            Log.e(TAG, "Error sending reminder", e);
            return Result.failure();
        }
    }
}
