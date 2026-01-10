package com.icpx.database;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Database migration utility to add rating column to existing databases
 */
public class DatabaseMigration {

    /**
     * Add rating column to targets table if it doesn't exist
     * @param conn Active database connection (will not be closed by this method)
     */
    public static void addRatingColumn(Connection conn) {
        String sql = "ALTER TABLE targets ADD COLUMN rating INTEGER";
        
        try (Statement stmt = conn.createStatement()) {
            // Check if column already exists
            try {
                stmt.execute(sql);
                System.out.println("Successfully added 'rating' column to targets table");
            } catch (SQLException e) {
                // Column might already exist, which is fine
                if (e.getMessage().contains("duplicate column name")) {
                    System.out.println("Column 'rating' already exists in targets table");
                } else {
                    throw e;
                }
            }
        } catch (SQLException e) {
            System.err.println("Error adding rating column: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Add archived column to targets table if it doesn't exist
     * @param conn Active database connection (will not be closed by this method)
     */
    public static void addArchivedColumn(Connection conn) {
        String sql = "ALTER TABLE targets ADD COLUMN archived INTEGER DEFAULT 0";
        
        try (Statement stmt = conn.createStatement()) {
            // Check if column already exists
            try {
                stmt.execute(sql);
                System.out.println("Successfully added 'archived' column to targets table");
            } catch (SQLException e) {
                // Column might already exist, which is fine
                if (e.getMessage().contains("duplicate column name")) {
                    System.out.println("Column 'archived' already exists in targets table");
                } else {
                    throw e;
                }
            }
        } catch (SQLException e) {
            System.err.println("Error adding archived column: " + e.getMessage());
            // Don't print stack trace for common migration errors to keep logs clean
        }
    }

    /**
     * Create contests table if it doesn't exist
     * @param conn Active database connection
     */
    public static void createContestsTable(Connection conn) {
        String sql = "CREATE TABLE IF NOT EXISTS contests (" +
                     "id INTEGER PRIMARY KEY, " +
                     "name TEXT NOT NULL, " +
                     "type TEXT, " +
                     "phase TEXT, " +
                     "duration_seconds INTEGER, " +
                     "start_time_seconds INTEGER, " +
                     "reminder_sent INTEGER DEFAULT 0)";
        
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
            System.out.println("Verified 'contests' table exists");
        } catch (SQLException e) {
            System.err.println("Error creating contests table: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Create index on problem_link to prevent duplicate problems
     * @param conn Active database connection
     */
    public static void createProblemLinkIndex(Connection conn) {
        String sql = "CREATE UNIQUE INDEX IF NOT EXISTS idx_targets_problem_link " +
                     "ON targets(problem_link) WHERE problem_link IS NOT NULL AND problem_link != ''";
        
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
            System.out.println("Created unique index on problem_link to prevent duplicates");
        } catch (SQLException e) {
            // Index might already exist or there might be duplicates in the data
            if (e.getMessage().contains("UNIQUE constraint failed") || e.getMessage().contains("already exists")) {
                System.out.println("Index on problem_link already exists or duplicates found");
            } else {
                System.err.println("Error creating problem_link index: " + e.getMessage());
            }
        }
    }

    /**
     * Run all migrations
     * @param conn Active database connection (will not be closed by this method)
     */
    public static void runMigrations(Connection conn) {
        System.out.println("Running database migrations...");
        addRatingColumn(conn);
        addArchivedColumn(conn);
        createContestsTable(conn);
        createProblemLinkIndex(conn);
        addDeadlineColumn(conn);
        createFriendsTable(conn);
        addUserRatingToSettings(conn);
        System.out.println("Database migrations completed");
    }
    
    /**
     * Add deadline column to targets table
     */
    public static void addDeadlineColumn(Connection conn) {
        String sql = "ALTER TABLE targets ADD COLUMN deadline TIMESTAMP";
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
            System.out.println("Successfully added 'deadline' column to targets table");
        } catch (SQLException e) {
            if (e.getMessage().contains("duplicate column name")) {
                System.out.println("Column 'deadline' already exists in targets table");
            }
        }
    }
    
    /**
     * Create friends table for one-way friend relationships
     */
    public static void createFriendsTable(Connection conn) {
        String sql = "CREATE TABLE IF NOT EXISTS friends (" +
                     "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                     "user_email TEXT NOT NULL, " +
                     "friend_email TEXT NOT NULL, " +
                     "added_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                     "UNIQUE(user_email, friend_email))";
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
            System.out.println("Verified 'friends' table exists");
        } catch (SQLException e) {
            System.err.println("Error creating friends table: " + e.getMessage());
        }
    }
    
    /**
     * Add user_rating to settings table
     */
    public static void addUserRatingToSettings(Connection conn) {
        String sql = "INSERT OR IGNORE INTO settings (key, value) VALUES ('user_rating', '0.0')";
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
            System.out.println("Initialized user_rating setting");
        } catch (SQLException e) {
            System.err.println("Error adding user_rating: " + e.getMessage());
        }
    }
}
