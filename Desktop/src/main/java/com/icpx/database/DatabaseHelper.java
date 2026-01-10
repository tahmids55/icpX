package com.icpx.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Database helper class for managing SQLite connection and initialization
 */
public class DatabaseHelper {
    private static final String DB_URL = "jdbc:sqlite:icpx.db";
    private static boolean isInitialized = false;

    /**
     * Get a new database connection.
     * Callers are responsible for closing this connection.
     */
    public static Connection getConnection() throws SQLException {
        Connection conn = DriverManager.getConnection(DB_URL);
        
        if (!isInitialized) {
            initializeDatabase(conn);
            isInitialized = true;
        }
        
        return conn;
    }

    private static synchronized void initializeDatabase(Connection conn) throws SQLException {
        if (isInitialized) return;

        String createUsersTable = "CREATE TABLE IF NOT EXISTS users (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "username TEXT NOT NULL UNIQUE, " +
                "password_hash TEXT NOT NULL, " +
                "startup_password_enabled INTEGER DEFAULT 0, " +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                ")";

        String createTargetsTable = "CREATE TABLE IF NOT EXISTS targets (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "type TEXT NOT NULL, " +
                "name TEXT NOT NULL, " +
                "problem_link TEXT, " +
                "topic_name TEXT, " +
                "website_url TEXT, " +
                "status TEXT DEFAULT 'pending', " +
                "rating INTEGER, " +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                ")";

        String createSettingsTable = "CREATE TABLE IF NOT EXISTS settings (" +
                "key TEXT PRIMARY KEY, " +
                "value TEXT" +
                ")";

        try (Statement stmt = conn.createStatement()) {
            stmt.execute(createUsersTable);
            stmt.execute(createTargetsTable);
            stmt.execute(createSettingsTable);
        }
        
        // Run migrations
        DatabaseMigration.runMigrations(conn);
    }

    /**
     * Close database connection (No-op in new design as connections are managed by caller)
     */
    public static void closeConnection() {
        // No-op
    }
}
