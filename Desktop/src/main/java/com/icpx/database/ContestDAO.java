package com.icpx.database;

import com.icpx.model.Contest;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ContestDAO {

    public static boolean saveContests(List<Contest> contests) {
        String sql = "INSERT OR REPLACE INTO contests (id, name, type, phase, duration_seconds, start_time_seconds) VALUES (?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            conn.setAutoCommit(false);
            for (Contest contest : contests) {
                pstmt.setInt(1, contest.getId());
                pstmt.setString(2, contest.getName());
                pstmt.setString(3, contest.getType());
                pstmt.setString(4, contest.getPhase());
                pstmt.setLong(5, contest.getDurationSeconds());
                pstmt.setLong(6, contest.getStartTimeSeconds());
                pstmt.addBatch();
            }
            pstmt.executeBatch();
            conn.commit();
            return true;
        } catch (SQLException e) {
            System.err.println("Error saving contests: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    public static List<Contest> getUpcomingContests() {
        List<Contest> contests = new ArrayList<>();
        String sql = "SELECT * FROM contests WHERE phase = 'BEFORE' ORDER BY start_time_seconds ASC";
        
        try (Connection conn = DatabaseHelper.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                Contest contest = new Contest();
                contest.setId(rs.getInt("id"));
                contest.setName(rs.getString("name"));
                contest.setType(rs.getString("type"));
                contest.setPhase(rs.getString("phase"));
                contest.setDurationSeconds(rs.getLong("duration_seconds"));
                contest.setStartTimeSeconds(rs.getLong("start_time_seconds"));
                contests.add(contest);
            }
        } catch (SQLException e) {
            System.err.println("Error getting upcoming contests: " + e.getMessage());
        }
        return contests;
    }

    public static void markReminderSent(int contestId) {
        String sql = "UPDATE contests SET reminder_sent = 1 WHERE id = ?";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, contestId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error marking reminder sent: " + e.getMessage());
        }
    }

    public static boolean hasReminderBeenSent(int contestId) {
        String sql = "SELECT reminder_sent FROM contests WHERE id = ?";
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, contestId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("reminder_sent") == 1;
            }
        } catch (SQLException e) {
            System.err.println("Error checking reminder status: " + e.getMessage());
        }
        return false;
    }
}
