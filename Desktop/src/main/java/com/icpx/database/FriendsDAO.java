package com.icpx.database;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for Friends operations
 * One-way friendship: User A adds User B - A can see B's data
 */
public class FriendsDAO {

    /**
     * Add a friend by email
     * @param userEmail The current user's email
     * @param friendEmail The friend's email to add
     * @return true if successful
     */
    public static boolean addFriend(String userEmail, String friendEmail) {
        if (userEmail == null || friendEmail == null || 
            userEmail.trim().isEmpty() || friendEmail.trim().isEmpty()) {
            return false;
        }
        
        // Normalize emails to lowercase
        userEmail = userEmail.trim().toLowerCase();
        friendEmail = friendEmail.trim().toLowerCase();
        
        // Can't add yourself
        if (userEmail.equals(friendEmail)) {
            return false;
        }
        
        String sql = "INSERT OR IGNORE INTO friends (user_email, friend_email, added_at) VALUES (?, ?, CURRENT_TIMESTAMP)";
        
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, userEmail);
            pstmt.setString(2, friendEmail);
            
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error adding friend: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Remove a friend
     * @param userEmail The current user's email
     * @param friendEmail The friend's email to remove
     * @return true if successful
     */
    public static boolean removeFriend(String userEmail, String friendEmail) {
        String sql = "DELETE FROM friends WHERE user_email = ? AND friend_email = ?";
        
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, userEmail.trim().toLowerCase());
            pstmt.setString(2, friendEmail.trim().toLowerCase());
            
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error removing friend: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Get all friends for a user
     * @param userEmail The user's email
     * @return List of friend emails
     */
    public static List<String> getFriends(String userEmail) {
        List<String> friends = new ArrayList<>();
        
        if (userEmail == null || userEmail.trim().isEmpty()) {
            return friends;
        }
        
        String sql = "SELECT friend_email FROM friends WHERE user_email = ? ORDER BY added_at DESC";
        
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, userEmail.trim().toLowerCase());
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                friends.add(rs.getString("friend_email"));
            }
        } catch (SQLException e) {
            System.err.println("Error getting friends: " + e.getMessage());
            e.printStackTrace();
        }
        return friends;
    }

    /**
     * Check if a user has added someone as a friend
     * @param userEmail The user's email
     * @param friendEmail The potential friend's email
     * @return true if they are friends
     */
    public static boolean isFriend(String userEmail, String friendEmail) {
        String sql = "SELECT 1 FROM friends WHERE user_email = ? AND friend_email = ?";
        
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, userEmail.trim().toLowerCase());
            pstmt.setString(2, friendEmail.trim().toLowerCase());
            ResultSet rs = pstmt.executeQuery();
            
            return rs.next();
        } catch (SQLException e) {
            System.err.println("Error checking friend: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Get friend count for a user
     * @param userEmail The user's email
     * @return Number of friends
     */
    public static int getFriendCount(String userEmail) {
        String sql = "SELECT COUNT(*) FROM friends WHERE user_email = ?";
        
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, userEmail.trim().toLowerCase());
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("Error getting friend count: " + e.getMessage());
            e.printStackTrace();
        }
        return 0;
    }
}
