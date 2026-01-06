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
     * Run all migrations
     * @param conn Active database connection (will not be closed by this method)
     */
    public static void runMigrations(Connection conn) {
        System.out.println("Running database migrations...");
        addRatingColumn(conn);
        System.out.println("Database migrations completed");
    }
}
