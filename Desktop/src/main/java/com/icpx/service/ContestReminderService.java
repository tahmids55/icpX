package com.icpx.service;

import com.icpx.database.ContestDAO;
import com.icpx.model.Contest;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ContestReminderService {

    private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private static final int REMINDER_MINUTES = 30;

    public static void start() {
        // Run every minute
        scheduler.scheduleAtFixedRate(ContestReminderService::checkReminders, 0, 1, TimeUnit.MINUTES);
    }

    private static void checkReminders() {
        if (!com.icpx.database.SettingsDAO.isContestReminderEnabled()) {
            return;
        }
        
        List<Contest> upcoming = ContestDAO.getUpcomingContests();
        long nowSeconds = Instant.now().getEpochSecond();
        
        for (Contest contest : upcoming) {
            long startTime = contest.getStartTimeSeconds();
            long diffSeconds = startTime - nowSeconds;
            
            // If less than REMINDER_MINUTES, show notification
            if (diffSeconds > 0 && diffSeconds <= REMINDER_MINUTES * 60) {
                if (!ContestDAO.hasReminderBeenSent(contest.getId())) {
                    NotificationService.showNotification(
                        "Contest Reminder",
                        contest.getName() + " is starting in " + (diffSeconds / 60) + " minutes!"
                    );
                    ContestDAO.markReminderSent(contest.getId());
                }
            }
        }
    }

    public static void stop() {
        scheduler.shutdown();
    }
}
