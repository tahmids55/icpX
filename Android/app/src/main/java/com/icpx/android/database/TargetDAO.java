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
     * Create a new target (checks for duplicates first)
     */
    public long createTarget(Target target, String userEmail) {
        // Check for duplicate problem link
        if (target.getProblemLink() != null && !target.getProblemLink().isEmpty()) {
            Target existing = getTargetByProblemLink(target.getProblemLink(), userEmail);
            if (existing != null) {
                android.util.Log.w("TargetDAO", "Duplicate problem detected: " + target.getProblemLink() + ". Skipping creation.");
                return existing.getId(); // Return existing ID instead of creating duplicate
            }
        }
        
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
     * Check if a target with the same problem link already exists for this user
     */
    public Target getTargetByProblemLink(String problemLink, String userEmail) {
        if (problemLink == null || problemLink.isEmpty()) {
            return null;
        }
        
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(
                DatabaseHelper.TABLE_TARGETS,
                null,
                DatabaseHelper.COLUMN_PROBLEM_LINK + " = ? AND " + DatabaseHelper.COLUMN_USER_EMAIL + " = ? AND " + DatabaseHelper.COLUMN_DELETED + " = 0",
                new String[]{problemLink, userEmail},
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
     * Remove duplicate targets for a user, keeping the oldest entry for each problem link
     */
    public int removeDuplicateTargets(String userEmail) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int deletedCount = 0;
        
        // Get all targets for this user
        List<Target> targets = getAllTargets(userEmail);
        
        // Group targets by problem link
        java.util.Map<String, java.util.List<Target>> targetsByLink = new java.util.HashMap<>();
        for (Target target : targets) {
            String link = target.getProblemLink();
            if (link != null && !link.isEmpty()) {
                if (!targetsByLink.containsKey(link)) {
                    targetsByLink.put(link, new java.util.ArrayList<>());
                }
                targetsByLink.get(link).add(target);
            }
        }
        
        // For each problem link with duplicates, keep the oldest and delete the rest
        for (java.util.Map.Entry<String, java.util.List<Target>> entry : targetsByLink.entrySet()) {
            java.util.List<Target> duplicates = entry.getValue();
            if (duplicates.size() > 1) {
                // Sort by creation date (oldest first)
                java.util.Collections.sort(duplicates, new java.util.Comparator<Target>() {
                    @Override
                    public int compare(Target t1, Target t2) {
                        return t1.getCreatedAt().compareTo(t2.getCreatedAt());
                    }
                });
                
                // Delete all except the first (oldest) one
                for (int i = 1; i < duplicates.size(); i++) {
                    deleteTarget(duplicates.get(i).getId());
                    deletedCount++;
                    android.util.Log.i("TargetDAO", "Deleted duplicate: " + duplicates.get(i).getName() + " (ID: " + duplicates.get(i).getId() + ")");
                }
            }
        }
        
        return deletedCount;
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

    /**
     * Get count of unique solved problems (excluding duplicates)
     */
    public int getUniqueSolvedCount(String userEmail) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String query = "SELECT COUNT(DISTINCT " + DatabaseHelper.COLUMN_PROBLEM_LINK + ") FROM " + 
                DatabaseHelper.TABLE_TARGETS +
                " WHERE " + DatabaseHelper.COLUMN_USER_EMAIL + " = ? AND " +
                DatabaseHelper.COLUMN_STATUS + " = 'achieved' AND " +
                DatabaseHelper.COLUMN_TYPE + " = 'problem' AND " +
                DatabaseHelper.COLUMN_PROBLEM_LINK + " IS NOT NULL AND " +
                DatabaseHelper.COLUMN_PROBLEM_LINK + " != ''";
        
        Cursor cursor = db.rawQuery(query, new String[]{userEmail});
        int count = 0;
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0);
        }
        cursor.close();
        return count;
    }

    /**
     * Get max CF rating from solved problems
     */
    public int getMaxCfRating(String userEmail) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String query = "SELECT MAX(" + DatabaseHelper.COLUMN_RATING + ") FROM " + 
                DatabaseHelper.TABLE_TARGETS +
                " WHERE " + DatabaseHelper.COLUMN_USER_EMAIL + " = ? AND " +
                DatabaseHelper.COLUMN_STATUS + " = 'achieved' AND " +
                DatabaseHelper.COLUMN_TYPE + " = 'problem' AND " +
                DatabaseHelper.COLUMN_RATING + " IS NOT NULL";
        
        Cursor cursor = db.rawQuery(query, new String[]{userEmail});
        int maxRating = 0;
        if (cursor.moveToFirst()) {
            maxRating = cursor.getInt(0);
        }
        cursor.close();
        return maxRating;
    }

    /**
     * Get count of problems added today
     */
    public int getDailyAddedCount(String userEmail) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String today = dateFormat.format(new Date());
        
        String query = "SELECT COUNT(*) FROM " + DatabaseHelper.TABLE_TARGETS +
                " WHERE " + DatabaseHelper.COLUMN_USER_EMAIL + " = ? AND " +
                DatabaseHelper.COLUMN_TYPE + " = 'problem' AND " +
                "date(" + DatabaseHelper.COLUMN_CREATED_AT + "/1000, 'unixepoch', 'localtime') = ?";
        
        Cursor cursor = db.rawQuery(query, new String[]{userEmail, today});
        int count = 0;
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0);
        }
        cursor.close();
        return count;
    }

    /**
     * Get count of problems solved today
     */
    public int getDailySolvedCount(String userEmail) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String today = dateFormat.format(new Date());
        
        String query = "SELECT COUNT(*) FROM " + DatabaseHelper.TABLE_TARGETS +
                " WHERE " + DatabaseHelper.COLUMN_USER_EMAIL + " = ? AND " +
                DatabaseHelper.COLUMN_TYPE + " = 'problem' AND " +
                DatabaseHelper.COLUMN_STATUS + " = 'achieved' AND " +
                "date(" + DatabaseHelper.COLUMN_CREATED_AT + "/1000, 'unixepoch', 'localtime') = ?";
        
        Cursor cursor = db.rawQuery(query, new String[]{userEmail, today});
        int count = 0;
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0);
        }
        cursor.close();
        return count;
    }
}
