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
        String sql = "INSERT INTO targets (type, name, problem_link, topic_name, website_url, status, rating) VALUES (?, ?, ?, ?, ?, ?, ?)";
        
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
            System.err.println("Error inserting target: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Get all targets from the database
     */
    public static List<Target> getAllTargets() {
        List<Target> targets = new ArrayList<>();
        String sql = "SELECT * FROM targets ORDER BY created_at DESC";
        
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
     * Delete a target
     */
    public static boolean deleteTarget(int id) {
        String sql = "DELETE FROM targets WHERE id = ?";
        
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, id);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error deleting target: " + e.getMessage());
            e.printStackTrace();
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
        
        return target;
    }
}
