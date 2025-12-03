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
}
