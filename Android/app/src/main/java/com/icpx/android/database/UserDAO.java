package com.icpx.android.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.icpx.android.model.User;

/**
 * Data Access Object for User operations
 */
public class UserDAO {
    
    private DatabaseHelper dbHelper;

    public UserDAO(Context context) {
        this.dbHelper = DatabaseHelper.getInstance(context);
    }

    /**
     * Check if any user exists in the database
     */
    public boolean userExists() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + DatabaseHelper.TABLE_USERS, null);
        boolean exists = false;
        if (cursor.moveToFirst()) {
            exists = cursor.getInt(0) > 0;
        }
        cursor.close();
        return exists;
    }

    /**
     * Create a new user
     */
    public long createUser(User user) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COLUMN_USERNAME, user.getUsername());
        values.put(DatabaseHelper.COLUMN_PASSWORD_HASH, user.getPasswordHash());
        values.put(DatabaseHelper.COLUMN_CODEFORCES_HANDLE, user.getCodeforcesHandle());
        values.put(DatabaseHelper.COLUMN_STARTUP_PASSWORD_ENABLED, user.isStartupPasswordEnabled() ? 1 : 0);
        values.put(DatabaseHelper.COLUMN_CREATED_AT, user.getCreatedAt().getTime());

        return db.insert(DatabaseHelper.TABLE_USERS, null, values);
    }

    /**
     * Get the current user (assumes single user system)
     */
    public User getCurrentUser() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(
                DatabaseHelper.TABLE_USERS,
                null,
                null,
                null,
                null,
                null,
                DatabaseHelper.COLUMN_ID + " ASC",
                "1"
        );

        User user = null;
        if (cursor.moveToFirst()) {
            user = cursorToUser(cursor);
        }
        cursor.close();
        return user;
    }

    /**
     * Get user by username
     */
    public User getUserByUsername(String username) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(
                DatabaseHelper.TABLE_USERS,
                null,
                DatabaseHelper.COLUMN_USERNAME + " = ?",
                new String[]{username},
                null,
                null,
                null
        );

        User user = null;
        if (cursor.moveToFirst()) {
            user = cursorToUser(cursor);
        }
        cursor.close();
        return user;
    }

    /**
     * Update user
     */
    public int updateUser(User user) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COLUMN_USERNAME, user.getUsername());
        values.put(DatabaseHelper.COLUMN_PASSWORD_HASH, user.getPasswordHash());
        values.put(DatabaseHelper.COLUMN_CODEFORCES_HANDLE, user.getCodeforcesHandle());
        values.put(DatabaseHelper.COLUMN_STARTUP_PASSWORD_ENABLED, user.isStartupPasswordEnabled() ? 1 : 0);

        return db.update(
                DatabaseHelper.TABLE_USERS,
                values,
                DatabaseHelper.COLUMN_ID + " = ?",
                new String[]{String.valueOf(user.getId())}
        );
    }

    /**
     * Convert cursor to User object
     */
    private User cursorToUser(Cursor cursor) {
        User user = new User();
        user.setId(cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_ID)));
        user.setUsername(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_USERNAME)));
        user.setPasswordHash(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_PASSWORD_HASH)));
        user.setCodeforcesHandle(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_CODEFORCES_HANDLE)));
        user.setStartupPasswordEnabled(cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_STARTUP_PASSWORD_ENABLED)) == 1);
        user.setCreatedAt(new java.util.Date(cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_CREATED_AT))));
        return user;
    }
}
