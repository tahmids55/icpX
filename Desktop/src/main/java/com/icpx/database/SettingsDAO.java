package com.icpx.database;

import java.sql.*;

/**
 * Data Access Object for Settings operations
 */
public class SettingsDAO {

    /**
     * Get a setting value by key
     */
    public static String getSetting(String key) {
        String sql = "SELECT value FROM settings WHERE key = ?";
        
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, key);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return rs.getString("value");
            }
        } catch (SQLException e) {
            System.err.println("Error getting setting: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Set a setting value
     */
    public static boolean setSetting(String key, String value) {
        String sql = "INSERT OR REPLACE INTO settings (key, value) VALUES (?, ?)";
        
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, key);
            pstmt.setString(2, value);
            
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error setting setting: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Delete a setting
     */
    public static boolean deleteSetting(String key) {
        String sql = "DELETE FROM settings WHERE key = ?";
        
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, Integer.parseInt(key));
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error deleting setting: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    // Convenience methods for specific settings
    
    public static String getCodeforcesHandle() {
        return getSetting("codeforces_handle");
    }

    public static boolean setCodeforcesHandle(String handle) {
        return setSetting("codeforces_handle", handle);
    }

    public static boolean isContestReminderEnabled() {
        String value = getSetting("contest_reminders_enabled");
        return value == null || Boolean.parseBoolean(value); // Default to enabled
    }

    public static boolean setContestReminderEnabled(boolean enabled) {
        return setSetting("contest_reminders_enabled", String.valueOf(enabled));
    }

    public static String getLastSyncTime() {
        return getSetting("last_sync_time");
    }

    public static void updateLastSyncTime() {
        String now = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("MMM dd, HH:mm"));
        setSetting("last_sync_time", now);
    }
    
    /**
     * Get current user rating
     */
    public static double getUserRating() {
        String value = getSetting("user_rating");
        if (value == null) {
            return 5.0; // Default starting rating (scale 1-10)
        }
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return 5.0;
        }
    }
    
    /**
     * Set user rating
     */
    public static boolean setUserRating(double rating) {
        return setSetting("user_rating", String.valueOf(rating));
    }
    
    /**
     * Add to user rating (can be negative for penalty)
     */
    public static double adjustUserRating(double delta) {
        double current = getUserRating();
        double newRating = Math.max(0, current + delta); // Minimum 0
        setUserRating(newRating);
        
        // Sync to Firebase for friends to see
        try {
            com.icpx.service.FriendsService.syncUserRatingToFirebase(newRating);
        } catch (Exception e) {
            // Ignore sync errors
        }
        
        return newRating;
    }
    
    /**
     * Calculate rating change when completing a target
     * @param targetDeadline The deadline of the target
     * @param completedAt The time when the target was completed
     * @return The rating change (positive if on time, negative if late)
     */
    public static double calculateRatingChange(java.time.LocalDateTime targetDeadline, java.time.LocalDateTime completedAt) {
        if (targetDeadline == null || completedAt == null) {
            return 0.02; // Default reward if no deadline set
        }
        
        if (completedAt.isBefore(targetDeadline) || completedAt.isEqual(targetDeadline)) {
            // Completed on time - reward
            return 0.02;
        } else {
            // Late - penalty based on minutes exceeded
            long minutesLate = java.time.Duration.between(targetDeadline, completedAt).toMinutes();
            double penalty = minutesLate * 0.01;
            return -penalty;
        }
    }
}
