package com.icpx.database;

import com.icpx.model.User;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.*;
import java.time.LocalDateTime;

/**
 * Data Access Object for User operations
 */
public class UserDAO {

    /**
     * Check if any user exists in the database
     */
    public static boolean userExists() throws SQLException {
        String query = "SELECT COUNT(*) as count FROM users";
        try (Connection conn = DatabaseHelper.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            if (rs.next()) {
                return rs.getInt("count") > 0;
            }
        }
        return false;
    }

    /**
     * Create a new user with hashed password
     */
    public static boolean createUser(String username, String password) throws SQLException {
        String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());
        String query = "INSERT INTO users (username, password_hash, startup_password_enabled) VALUES (?, ?, 0)";
        
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, username);
            pstmt.setString(2, hashedPassword);
            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;
        }
    }

    /**
     * Verify user password
     */
    public static boolean verifyPassword(String password) throws SQLException {
        User user = getCurrentUser();
        if (user != null) {
            return BCrypt.checkpw(password, user.getPasswordHash());
        }
        return false;
    }

    /**
     * Get the current user (single user app, so just get the first user)
     */
    public static User getCurrentUser() throws SQLException {
        String query = "SELECT * FROM users LIMIT 1";
        try (Connection conn = DatabaseHelper.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            if (rs.next()) {
                User user = new User();
                user.setId(rs.getInt("id"));
                user.setUsername(rs.getString("username"));
                user.setPasswordHash(rs.getString("password_hash"));
                user.setStartupPasswordEnabled(rs.getInt("startup_password_enabled") == 1);
                
                Timestamp timestamp = rs.getTimestamp("created_at");
                if (timestamp != null) {
                    user.setCreatedAt(timestamp.toLocalDateTime());
                }
                
                return user;
            }
        }
        return null;
    }

    /**
     * Toggle startup password setting
     */
    public static boolean toggleStartupPassword(boolean enabled) throws SQLException {
        String query = "UPDATE users SET startup_password_enabled = ? WHERE id = (SELECT id FROM users LIMIT 1)";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, enabled ? 1 : 0);
            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;
        }
    }
}
