package com.icpx.android.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.icpx.android.model.Target;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Data Access Object for Target operations
 */
public class TargetDAO {
    
    private DatabaseHelper dbHelper;

    public TargetDAO(Context context) {
        this.dbHelper = DatabaseHelper.getInstance(context);
    }

    /**
     * Create a new target
     */
    public long createTarget(Target target, String userEmail) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COLUMN_USER_EMAIL, userEmail);
        values.put(DatabaseHelper.COLUMN_TYPE, target.getType());
        values.put(DatabaseHelper.COLUMN_NAME, target.getName());
        values.put(DatabaseHelper.COLUMN_PROBLEM_LINK, target.getProblemLink());
        values.put(DatabaseHelper.COLUMN_TOPIC_NAME, target.getTopicName());
        values.put(DatabaseHelper.COLUMN_WEBSITE_URL, target.getWebsiteUrl());
        values.put(DatabaseHelper.COLUMN_STATUS, target.getStatus());
        values.put(DatabaseHelper.COLUMN_RATING, target.getRating());
        values.put(DatabaseHelper.COLUMN_DELETED, target.isDeleted() ? 1 : 0);
        values.put(DatabaseHelper.COLUMN_CREATED_AT, target.getCreatedAt().getTime());

        return db.insert(DatabaseHelper.TABLE_TARGETS, null, values);
    }

    /**
     * Get all targets for a specific user
     */
    public List<Target> getAllTargets(String userEmail) {
        android.util.Log.d("TargetDAO", "getAllTargets called with userEmail: " + userEmail);
        List<Target> targets = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(
                DatabaseHelper.TABLE_TARGETS,
                null,
                DatabaseHelper.COLUMN_DELETED + " = 0 AND " + DatabaseHelper.COLUMN_USER_EMAIL + " = ?",
                new String[]{userEmail},
                null,
                null,
                DatabaseHelper.COLUMN_CREATED_AT + " DESC"
        );
        android.util.Log.d("TargetDAO", "Query returned " + cursor.getCount() + " rows");

        while (cursor.moveToNext()) {
            targets.add(cursorToTarget(cursor));
        }
        cursor.close();
        return targets;
    }

    /**
     * Get targets by status for a specific user
     */
    public List<Target> getTargetsByStatus(String status, String userEmail) {
        List<Target> targets = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(
                DatabaseHelper.TABLE_TARGETS,
                null,
                DatabaseHelper.COLUMN_STATUS + " = ? AND " + DatabaseHelper.COLUMN_DELETED + " = 0 AND " + DatabaseHelper.COLUMN_USER_EMAIL + " = ?",
                new String[]{status, userEmail},
                null,
                null,
                DatabaseHelper.COLUMN_CREATED_AT + " DESC"
        );

        while (cursor.moveToNext()) {
            targets.add(cursorToTarget(cursor));
        }
        cursor.close();
        return targets;
    }

    /**
     * Get targets by type
     */
    public List<Target> getTargetsByType(String type) {
        List<Target> targets = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(
                DatabaseHelper.TABLE_TARGETS,
                null,
                DatabaseHelper.COLUMN_TYPE + " = ?",
                new String[]{type},
                null,
                null,
                DatabaseHelper.COLUMN_CREATED_AT + " DESC"
        );

        while (cursor.moveToNext()) {
            targets.add(cursorToTarget(cursor));
        }
        cursor.close();
        return targets;
    }

    /**
     * Get target by ID
     */
    public Target getTargetById(int id) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(
                DatabaseHelper.TABLE_TARGETS,
                null,
                DatabaseHelper.COLUMN_ID + " = ?",
                new String[]{String.valueOf(id)},
                null,
                null,
                null
        );

        Target target = null;
        if (cursor.moveToFirst()) {
            target = cursorToTarget(cursor);
        }
        cursor.close();
        return target;
    }

    /**
     * Get achieved targets for history (includes deleted items)
     */
    public List<Target> getAchievedTargetsForHistory() {
        List<Target> targets = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(
                DatabaseHelper.TABLE_TARGETS,
                null,
                DatabaseHelper.COLUMN_STATUS + " = ?",
                new String[]{"achieved"},
                null,
                null,
                DatabaseHelper.COLUMN_CREATED_AT + " DESC"
        );

        while (cursor.moveToNext()) {
            targets.add(cursorToTarget(cursor));
        }
        cursor.close();
        return targets;
    }

    /**
     * Update target status
     */
    public int updateTargetStatus(int targetId, String status) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COLUMN_STATUS, status);

        return db.update(
                DatabaseHelper.TABLE_TARGETS,
                values,
                DatabaseHelper.COLUMN_ID + " = ?",
                new String[]{String.valueOf(targetId)}
        );
    }

    /**
     * Update target
     */
    public int updateTarget(Target target) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COLUMN_TYPE, target.getType());
        values.put(DatabaseHelper.COLUMN_NAME, target.getName());
        values.put(DatabaseHelper.COLUMN_PROBLEM_LINK, target.getProblemLink());
        values.put(DatabaseHelper.COLUMN_TOPIC_NAME, target.getTopicName());
        values.put(DatabaseHelper.COLUMN_WEBSITE_URL, target.getWebsiteUrl());
        values.put(DatabaseHelper.COLUMN_STATUS, target.getStatus());
        values.put(DatabaseHelper.COLUMN_RATING, target.getRating());
        values.put(DatabaseHelper.COLUMN_DELETED, target.isDeleted() ? 1 : 0);

        return db.update(
                DatabaseHelper.TABLE_TARGETS,
                values,
                DatabaseHelper.COLUMN_ID + " = ?",
                new String[]{String.valueOf(target.getId())}
        );
    }

    /**
     * Delete target
     */
    public int deleteTarget(int targetId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        return db.delete(
                DatabaseHelper.TABLE_TARGETS,
                DatabaseHelper.COLUMN_ID + " = ?",
                new String[]{String.valueOf(targetId)}
        );
    }

    /**
     * Get count by status for a specific user
     */
    public int getCountByStatus(String status, String userEmail) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT COUNT(*) FROM " + DatabaseHelper.TABLE_TARGETS +
                        " WHERE " + DatabaseHelper.COLUMN_STATUS + " = ? AND " +
                        DatabaseHelper.COLUMN_USER_EMAIL + " = ?",
                new String[]{status, userEmail}
        );
        int count = 0;
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0);
        }
        cursor.close();
        return count;
    }

    /**
     * Get average rating of solved problems
     */
    public double getAverageRating() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT AVG(" + DatabaseHelper.COLUMN_RATING + ") FROM " + DatabaseHelper.TABLE_TARGETS +
                        " WHERE " + DatabaseHelper.COLUMN_STATUS + " = 'achieved' AND " +
                        DatabaseHelper.COLUMN_RATING + " IS NOT NULL",
                null
        );
        double avg = 0;
        if (cursor.moveToFirst()) {
            avg = cursor.getDouble(0);
        }
        cursor.close();
        return avg;
    }

    /**
     * Convert cursor to Target object
     */
    private Target cursorToTarget(Cursor cursor) {
        Target target = new Target();
        target.setId(cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_ID)));
        target.setType(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_TYPE)));
        target.setName(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_NAME)));
        target.setProblemLink(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_PROBLEM_LINK)));
        target.setTopicName(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_TOPIC_NAME)));
        target.setWebsiteUrl(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_WEBSITE_URL)));
        target.setStatus(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_STATUS)));
        
        int ratingIndex = cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_RATING);
        if (!cursor.isNull(ratingIndex)) {
            target.setRating(cursor.getInt(ratingIndex));
        }
        
        target.setDeleted(cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_DELETED)) == 1);
        target.setCreatedAt(new Date(cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_CREATED_AT))));
        return target;
    }

    /**
     * Get activity data for heatmap - returns count of problems solved per date
     */
    public Map<String, Integer> getProblemActivityByDate(String userEmail) {
        Map<String, Integer> activityMap = new HashMap<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

        String query = "SELECT " + DatabaseHelper.COLUMN_CREATED_AT + " FROM " + DatabaseHelper.TABLE_TARGETS +
                " WHERE " + DatabaseHelper.COLUMN_USER_EMAIL + " = ? AND " +
                DatabaseHelper.COLUMN_TYPE + " = 'problem' AND " +
                DatabaseHelper.COLUMN_STATUS + " = 'achieved' AND " +
                DatabaseHelper.COLUMN_DELETED + " = 0";

        Cursor cursor = db.rawQuery(query, new String[]{userEmail});

        if (cursor.moveToFirst()) {
            do {
                long timestamp = cursor.getLong(0);
                String dateStr = dateFormat.format(new Date(timestamp));
                activityMap.put(dateStr, activityMap.getOrDefault(dateStr, 0) + 1);
            } while (cursor.moveToNext());
        }

        cursor.close();
        return activityMap;
    }

    /**
     * Get activity data for heatmap - returns count of topics learned per date
     */
    public Map<String, Integer> getTopicActivityByDate(String userEmail) {
        Map<String, Integer> activityMap = new HashMap<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

        String query = "SELECT " + DatabaseHelper.COLUMN_CREATED_AT + " FROM " + DatabaseHelper.TABLE_TARGETS +
                " WHERE " + DatabaseHelper.COLUMN_USER_EMAIL + " = ? AND " +
                DatabaseHelper.COLUMN_TYPE + " = 'topic' AND " +
                DatabaseHelper.COLUMN_DELETED + " = 0";

        Cursor cursor = db.rawQuery(query, new String[]{userEmail});

        if (cursor.moveToFirst()) {
            do {
                long timestamp = cursor.getLong(0);
                String dateStr = dateFormat.format(new Date(timestamp));
                activityMap.put(dateStr, activityMap.getOrDefault(dateStr, 0) + 1);
            } while (cursor.moveToNext());
        }

        cursor.close();
        return activityMap;
    }
}
