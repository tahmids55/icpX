package com.icpx.android.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Database helper class for managing SQLite database
 */
public class DatabaseHelper extends SQLiteOpenHelper {
    
    private static final String DATABASE_NAME = "icpx.db";
    private static final int DATABASE_VERSION = 3;

    // Table names
    public static final String TABLE_USERS = "users";
    public static final String TABLE_TARGETS = "targets";
    public static final String TABLE_SETTINGS = "settings";

    // User table columns
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_USERNAME = "username";
    public static final String COLUMN_PASSWORD_HASH = "password_hash";
    public static final String COLUMN_CODEFORCES_HANDLE = "codeforces_handle";
    public static final String COLUMN_STARTUP_PASSWORD_ENABLED = "startup_password_enabled";
    public static final String COLUMN_CREATED_AT = "created_at";

    // Target table columns
    public static final String COLUMN_USER_EMAIL = "user_email";
    public static final String COLUMN_TYPE = "type";
    public static final String COLUMN_NAME = "name";
    public static final String COLUMN_PROBLEM_LINK = "problem_link";
    public static final String COLUMN_TOPIC_NAME = "topic_name";
    public static final String COLUMN_WEBSITE_URL = "website_url";
    public static final String COLUMN_STATUS = "status";
    public static final String COLUMN_RATING = "rating";
    public static final String COLUMN_DELETED = "deleted";

    // Settings table columns
    public static final String COLUMN_KEY = "key";
    public static final String COLUMN_VALUE = "value";

    private static DatabaseHelper instance;

    private DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public static synchronized DatabaseHelper getInstance(Context context) {
        if (instance == null) {
            instance = new DatabaseHelper(context.getApplicationContext());
        }
        return instance;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Create users table
        String createUsersTable = "CREATE TABLE " + TABLE_USERS + " (" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_USERNAME + " TEXT NOT NULL UNIQUE, " +
                COLUMN_PASSWORD_HASH + " TEXT NOT NULL, " +
                COLUMN_CODEFORCES_HANDLE + " TEXT, " +
                COLUMN_STARTUP_PASSWORD_ENABLED + " INTEGER DEFAULT 1, " +
                COLUMN_CREATED_AT + " INTEGER NOT NULL" +
                ")";
        db.execSQL(createUsersTable);

        // Create targets table
        String createTargetsTable = "CREATE TABLE " + TABLE_TARGETS + " (" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_USER_EMAIL + " TEXT NOT NULL, " +
                COLUMN_TYPE + " TEXT NOT NULL, " +
                COLUMN_NAME + " TEXT NOT NULL, " +
                COLUMN_PROBLEM_LINK + " TEXT, " +
                COLUMN_TOPIC_NAME + " TEXT, " +
                COLUMN_WEBSITE_URL + " TEXT, " +
                COLUMN_STATUS + " TEXT DEFAULT 'pending', " +
                COLUMN_RATING + " INTEGER, " +
                COLUMN_DELETED + " INTEGER DEFAULT 0, " +
                COLUMN_CREATED_AT + " INTEGER NOT NULL" +
                ")";
        db.execSQL(createTargetsTable);

        // Create settings table
        String createSettingsTable = "CREATE TABLE " + TABLE_SETTINGS + " (" +
                COLUMN_KEY + " TEXT PRIMARY KEY, " +
                COLUMN_VALUE + " TEXT" +
                ")";
        db.execSQL(createSettingsTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Handle database upgrades
        if (oldVersion < 2 && newVersion >= 2) {
            // Add deleted column to targets table
            db.execSQL("ALTER TABLE " + TABLE_TARGETS + " ADD COLUMN " + COLUMN_DELETED + " INTEGER DEFAULT 0");
        }
        if (oldVersion < 3 && newVersion >= 3) {
            // Add user_email column to targets table
            db.execSQL("ALTER TABLE " + TABLE_TARGETS + " ADD COLUMN " + COLUMN_USER_EMAIL + " TEXT DEFAULT ''");
        }
        if (oldVersion < newVersion && newVersion > 3) {
            // For future upgrades, handle here
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_TARGETS);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_SETTINGS);
            onCreate(db);
        }
    }
}
