package com.icpx.android.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Data Access Object for Friends operations
 * One-way friendship: User A adds User B - A can see B's data
 */
public class FriendsDAO {

    private DatabaseHelper dbHelper;

    public FriendsDAO(Context context) {
        this.dbHelper = DatabaseHelper.getInstance(context);
    }

    /**
     * Add a friend by email
     * @param userEmail The current user's email
     * @param friendEmail The friend's email to add
     * @return true if successful
     */
    public boolean addFriend(String userEmail, String friendEmail) {
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

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COLUMN_USER_EMAIL, userEmail);
        values.put(DatabaseHelper.COLUMN_FRIEND_EMAIL, friendEmail);
        values.put(DatabaseHelper.COLUMN_ADDED_AT, System.currentTimeMillis());

        long result = db.insertWithOnConflict(DatabaseHelper.TABLE_FRIENDS, null, values,
                SQLiteDatabase.CONFLICT_IGNORE);
        return result != -1;
    }

    /**
     * Remove a friend
     * @param userEmail The current user's email
     * @param friendEmail The friend's email to remove
     * @return true if successful
     */
    public boolean removeFriend(String userEmail, String friendEmail) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int deleted = db.delete(
                DatabaseHelper.TABLE_FRIENDS,
                DatabaseHelper.COLUMN_USER_EMAIL + " = ? AND " + DatabaseHelper.COLUMN_FRIEND_EMAIL + " = ?",
                new String[]{userEmail.trim().toLowerCase(), friendEmail.trim().toLowerCase()}
        );
        return deleted > 0;
    }

    /**
     * Get all friends for a user
     * @param userEmail The user's email
     * @return List of friend emails
     */
    public List<String> getFriends(String userEmail) {
        List<String> friends = new ArrayList<>();

        if (userEmail == null || userEmail.trim().isEmpty()) {
            return friends;
        }

        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(
                DatabaseHelper.TABLE_FRIENDS,
                new String[]{DatabaseHelper.COLUMN_FRIEND_EMAIL},
                DatabaseHelper.COLUMN_USER_EMAIL + " = ?",
                new String[]{userEmail.trim().toLowerCase()},
                null, null,
                DatabaseHelper.COLUMN_ADDED_AT + " DESC"
        );

        while (cursor.moveToNext()) {
            friends.add(cursor.getString(0));
        }
        cursor.close();
        return friends;
    }

    /**
     * Check if a user has added someone as a friend
     * @param userEmail The user's email
     * @param friendEmail The potential friend's email
     * @return true if they are friends
     */
    public boolean isFriend(String userEmail, String friendEmail) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(
                DatabaseHelper.TABLE_FRIENDS,
                new String[]{"1"},
                DatabaseHelper.COLUMN_USER_EMAIL + " = ? AND " + DatabaseHelper.COLUMN_FRIEND_EMAIL + " = ?",
                new String[]{userEmail.trim().toLowerCase(), friendEmail.trim().toLowerCase()},
                null, null, null
        );

        boolean isFriend = cursor.getCount() > 0;
        cursor.close();
        return isFriend;
    }

    /**
     * Get friend count for a user
     * @param userEmail The user's email
     * @return Number of friends
     */
    public int getFriendCount(String userEmail) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT COUNT(*) FROM " + DatabaseHelper.TABLE_FRIENDS +
                        " WHERE " + DatabaseHelper.COLUMN_USER_EMAIL + " = ?",
                new String[]{userEmail.trim().toLowerCase()}
        );

        int count = 0;
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0);
        }
        cursor.close();
        return count;
    }
}
