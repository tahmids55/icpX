package com.icpx.database;

import com.icpx.model.Target;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for Target operations
 */
public class TargetDAO {

    /**
     * Insert a new target into the database
     */
    public static boolean insertTarget(Target target) {
        // Check for duplicate by problem link first
        if (target.getProblemLink() != null && !target.getProblemLink().trim().isEmpty()) {
            Target existing = getTargetByProblemLink(target.getProblemLink());
            if (existing != null) {
                System.out.println("Target with same problem link already exists, skipping insert: " + target.getProblemLink());
                return false; // Duplicate, don't insert
            }
        }
        
        String sql = "INSERT INTO targets (type, name, problem_link, topic_name, website_url, status, rating, archived, deadline) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            pstmt.setString(1, target.getType());
            pstmt.setString(2, target.getName());
            pstmt.setString(3, target.getProblemLink());
            pstmt.setString(4, target.getTopicName());
            pstmt.setString(5, target.getWebsiteUrl());
            pstmt.setString(6, target.getStatus());
            if (target.getRating() != null) {
                pstmt.setInt(7, target.getRating());
            } else {
                pstmt.setNull(7, Types.INTEGER);
            }
            pstmt.setInt(8, target.isArchived() ? 1 : 0);
            if (target.getDeadline() != null) {
                pstmt.setTimestamp(9, Timestamp.valueOf(target.getDeadline()));
            } else {
                pstmt.setTimestamp(9, Timestamp.valueOf(LocalDateTime.now().plusHours(24)));
            }
            
            int affectedRows = pstmt.executeUpdate();
            
            if (affectedRows > 0) {
                // Get the last inserted row id for SQLite
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery("SELECT last_insert_rowid()")) {
                    if (rs.next()) {
                        target.setId(rs.getInt(1));
                    }
                }
                return true;
            }
        } catch (SQLException e) {
            if (e.getMessage().contains("UNIQUE constraint failed")) {
                System.out.println("Duplicate problem_link detected, skipping insert");
                return false;
            }
            System.err.println("Error inserting target: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Get all targets from the database
     */
    /**
     * Get all targets including archived (for sync)
     */
    public static List<Target> getAllTargetsForSync() {
        List<Target> targets = new ArrayList<>();
        String sql = "SELECT * FROM targets"; // No filter
        
        try (Connection conn = DatabaseHelper.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                targets.add(extractTargetFromResultSet(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error getting all targets for sync: " + e.getMessage());
            e.printStackTrace();
        }
        return targets;
    }

    /**
     * Get all targets from the database (Active only)
     */
    public static List<Target> getAllTargets() {
        List<Target> targets = new ArrayList<>();
        String sql = "SELECT * FROM targets WHERE archived = 0 ORDER BY created_at DESC";
        
        try (Connection conn = DatabaseHelper.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                targets.add(extractTargetFromResultSet(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error getting all targets: " + e.getMessage());
            e.printStackTrace();
        }
        return targets;
    }

    /**
     * Get a target by ID
     */
    public static Target getTargetById(int id) {
        String sql = "SELECT * FROM targets WHERE id = ?";
        
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, id);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return extractTargetFromResultSet(rs);
            }
        } catch (SQLException e) {
            System.err.println("Error getting target by ID: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Get a target by problem link (to check for duplicates)
     */
    public static Target getTargetByProblemLink(String problemLink) {
        if (problemLink == null || problemLink.trim().isEmpty()) {
            return null;
        }
        
        String sql = "SELECT * FROM targets WHERE problem_link = ?";
        
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, problemLink.trim());
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return extractTargetFromResultSet(rs);
            }
        } catch (SQLException e) {
            System.err.println("Error getting target by problem link: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Update target status
     */
    public static boolean updateTargetStatus(int id, String status) {
        String sql = "UPDATE targets SET status = ? WHERE id = ?";
        
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, status);
            pstmt.setInt(2, id);
            
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error updating target status: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }
    
    /**
     * Update target status with rating adjustment
     * @return The rating change applied (positive or negative)
     */
    public static double updateTargetStatusWithRating(int id, String status) {
        Target target = getTargetById(id);
        if (target == null) {
            return 0;
        }
        
        double ratingChange = 0;
        
        // Calculate rating change when marking as achieved
        if ("achieved".equals(status) && !"achieved".equals(target.getStatus())) {
            LocalDateTime now = LocalDateTime.now();
            ratingChange = SettingsDAO.calculateRatingChange(target.getDeadline(), now);
            SettingsDAO.adjustUserRating(ratingChange);
            System.out.println("Rating change: " + ratingChange + ", new rating: " + SettingsDAO.getUserRating());
        }
        
        // Update the status
        updateTargetStatus(id, status);
        
        return ratingChange;
    }

    /**
     * Update a target completely (for sync from Firebase)
     */
    public static boolean updateTarget(Target target) {
        String sql = "UPDATE targets SET type = ?, name = ?, problem_link = ?, topic_name = ?, " +
                     "website_url = ?, status = ?, rating = ?, archived = ? WHERE id = ?";
        
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, target.getType());
            pstmt.setString(2, target.getName());
            pstmt.setString(3, target.getProblemLink());
            pstmt.setString(4, target.getTopicName());
            pstmt.setString(5, target.getWebsiteUrl());
            pstmt.setString(6, target.getStatus());
            if (target.getRating() != null) {
                pstmt.setInt(7, target.getRating());
            } else {
                pstmt.setNull(7, Types.INTEGER);
            }
            pstmt.setInt(8, target.isArchived() ? 1 : 0);
            pstmt.setInt(9, target.getId());
            
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error updating target: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Delete a target
     */
    /**
     * Delete a target (Soft delete if achieved, hard delete otherwise)
     */
    public static boolean deleteTarget(int id) {
        Target target = getTargetById(id);
        if (target != null && "achieved".equals(target.getStatus())) {
            // Soft delete - mark as archived
            String sql = "UPDATE targets SET archived = 1 WHERE id = ?";
            try (Connection conn = DatabaseHelper.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, id);
                return pstmt.executeUpdate() > 0;
            } catch (SQLException e) {
                System.err.println("Error archiving target: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            // Hard delete
            String sql = "DELETE FROM targets WHERE id = ?";
            try (Connection conn = DatabaseHelper.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, id);
                return pstmt.executeUpdate() > 0;
            } catch (SQLException e) {
                System.err.println("Error deleting target: " + e.getMessage());
                e.printStackTrace();
            }
        }
        return false;
    }

    /**
     * Get count of targets by status
     */
    public static int getTargetCountByStatus(String status) {
        String sql = "SELECT COUNT(*) FROM targets WHERE status = ?";
        
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, status);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("Error getting target count: " + e.getMessage());
            e.printStackTrace();
        }
        return 0;
    }

    /**
     * Get all achieved problem targets (solved problems)
     */
    public static List<Target> getAchievedProblems() {
        List<Target> targets = new ArrayList<>();
        String sql = "SELECT * FROM targets WHERE type = 'problem' AND status = 'achieved' ORDER BY created_at DESC";
        
        try (Connection conn = DatabaseHelper.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                targets.add(extractTargetFromResultSet(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error getting achieved problems: " + e.getMessage());
            e.printStackTrace();
        }
        return targets;
    }

    /**
     * Get all achieved topic targets (learned topics)
     */
    public static List<Target> getAchievedTopics() {
        List<Target> targets = new ArrayList<>();
        String sql = "SELECT * FROM targets WHERE type = 'topic' AND status = 'achieved' ORDER BY created_at DESC";
        
        try (Connection conn = DatabaseHelper.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                targets.add(extractTargetFromResultSet(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error getting achieved topics: " + e.getMessage());
            e.printStackTrace();
        }
        return targets;
    }

    /**
     * Extract Target object from ResultSet
     */
    private static Target extractTargetFromResultSet(ResultSet rs) throws SQLException {
        Target target = new Target();
        target.setId(rs.getInt("id"));
        target.setType(rs.getString("type"));
        target.setName(rs.getString("name"));
        target.setProblemLink(rs.getString("problem_link"));
        target.setTopicName(rs.getString("topic_name"));
        target.setWebsiteUrl(rs.getString("website_url"));
        target.setStatus(rs.getString("status"));
        
        int rating = rs.getInt("rating");
        if (!rs.wasNull()) {
            target.setRating(rating);
        }
        
        Timestamp timestamp = rs.getTimestamp("created_at");
        if (timestamp != null) {
            target.setCreatedAt(timestamp.toLocalDateTime());
        }
        
        // Archived status
        int archived = rs.getInt("archived");
        target.setArchived(archived == 1);
        
        // Deadline
        try {
            Timestamp deadlineTs = rs.getTimestamp("deadline");
            if (deadlineTs != null) {
                target.setDeadline(deadlineTs.toLocalDateTime());
            }
        } catch (SQLException e) {
            // Column may not exist yet
        }
        
        return target;
    }

    /**
     * Update rating for a specific target
     */
    public static boolean updateTargetRating(int targetId, Integer rating) {
        String sql = "UPDATE targets SET rating = ? WHERE id = ?";
        
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            if (rating != null) {
                pstmt.setInt(1, rating);
            } else {
                pstmt.setNull(1, Types.INTEGER);
            }
            pstmt.setInt(2, targetId);
            
            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            System.err.println("Error updating target rating: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Get count of targets added today
     */
    public static int getDailyAddedCount() {
        String sql = "SELECT COUNT(*) FROM targets WHERE DATE(created_at) = DATE('now', 'localtime')";
        
        try (Connection conn = DatabaseHelper.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("Error getting daily added count: " + e.getMessage());
            e.printStackTrace();
        }
        return 0;
    }

    /**
     * Get count of targets solved today
     */
    public static int getDailySolvedCount() {
        String sql = "SELECT COUNT(*) FROM targets WHERE status = 'achieved' AND DATE(created_at) = DATE('now', 'localtime')";
        
        try (Connection conn = DatabaseHelper.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("Error getting daily solved count: " + e.getMessage());
            e.printStackTrace();
        }
        return 0;
    }

    /**
     * Get activity data for heatmap (date -> count of solved problems)
     */
    public static java.util.Map<java.time.LocalDate, Integer> getActivityHeatmapData() {
        java.util.Map<java.time.LocalDate, Integer> activityMap = new java.util.HashMap<>();
        String sql = "SELECT DATE(created_at) as date, COUNT(*) as count FROM targets " +
                     "WHERE status = 'achieved' AND type = 'problem' " +
                     "GROUP BY DATE(created_at) ORDER BY date DESC LIMIT 365";
        
        try (Connection conn = DatabaseHelper.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                String dateStr = rs.getString("date");
                int count = rs.getInt("count");
                
                if (dateStr != null) {
                    java.time.LocalDate date = java.time.LocalDate.parse(dateStr);
                    activityMap.put(date, count);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error getting activity heatmap data: " + e.getMessage());
            e.printStackTrace();
        }
        return activityMap;
    }

    /**
     * Get recent targets (last N)
     */
    public static List<Target> getRecentTargets(int limit) {
        List<Target> targets = new ArrayList<>();
        String sql = "SELECT * FROM targets WHERE archived = 0 ORDER BY created_at DESC LIMIT ?";
        
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, limit);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                targets.add(extractTargetFromResultSet(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error getting recent targets: " + e.getMessage());
            e.printStackTrace();
        }
        return targets;
    }
}
