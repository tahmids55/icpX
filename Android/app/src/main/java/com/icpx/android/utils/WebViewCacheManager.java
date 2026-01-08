package com.icpx.android.utils;

import android.content.Context;
import android.util.Log;
import android.webkit.WebView;

import java.io.File;

/**
 * Utility class to manage WebView cache
 * Provides methods to check cache size and clear cache when needed
 */
public class WebViewCacheManager {
    
    private static final String TAG = "WebViewCacheManager";
    
    /**
     * Get the size of WebView cache in bytes
     * @param context Application context
     * @return Cache size in bytes
     */
    public static long getCacheSize(Context context) {
        try {
            File cacheDir = context.getCacheDir();
            return getDirSize(cacheDir);
        } catch (Exception e) {
            Log.e(TAG, "Error getting cache size: " + e.getMessage(), e);
            return 0;
        }
    }
    
    /**
     * Get human-readable cache size
     * @param context Application context
     * @return Formatted cache size string (e.g., "15.2 MB")
     */
    public static String getFormattedCacheSize(Context context) {
        long bytes = getCacheSize(context);
        return formatBytes(bytes);
    }
    
    /**
     * Clear WebView cache
     * @param context Application context
     * @param webView WebView instance (optional, can be null)
     */
    public static void clearCache(Context context, WebView webView) {
        try {
            if (webView != null) {
                webView.clearCache(true);
                webView.clearHistory();
            }
            
            // Clear app cache directory
            File cacheDir = context.getCacheDir();
            deleteDir(cacheDir);
            
            Log.d(TAG, "Cache cleared successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error clearing cache: " + e.getMessage(), e);
        }
    }
    
    /**
     * Calculate directory size recursively
     * @param dir Directory to measure
     * @return Size in bytes
     */
    private static long getDirSize(File dir) {
        long size = 0;
        try {
            if (dir.exists()) {
                File[] files = dir.listFiles();
                if (files != null) {
                    for (File file : files) {
                        if (file.isFile()) {
                            size += file.length();
                        } else {
                            size += getDirSize(file);
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error calculating dir size: " + e.getMessage(), e);
        }
        return size;
    }
    
    /**
     * Delete directory and its contents
     * @param dir Directory to delete
     * @return true if successful
     */
    private static boolean deleteDir(File dir) {
        try {
            if (dir != null && dir.isDirectory()) {
                String[] children = dir.list();
                if (children != null) {
                    for (String child : children) {
                        boolean success = deleteDir(new File(dir, child));
                        if (!success) {
                            return false;
                        }
                    }
                }
            }
            // Don't delete the cache dir itself, just its contents
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error deleting dir: " + e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Format bytes to human-readable format
     * @param bytes Size in bytes
     * @return Formatted string (e.g., "15.2 MB")
     */
    private static String formatBytes(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.1f KB", bytes / 1024.0);
        } else if (bytes < 1024 * 1024 * 1024) {
            return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
        } else {
            return String.format("%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0));
        }
    }
    
    /**
     * Check if cache size exceeds a limit
     * @param context Application context
     * @param limitMB Limit in megabytes
     * @return true if cache exceeds limit
     */
    public static boolean isCacheExceedingLimit(Context context, int limitMB) {
        long cacheSize = getCacheSize(context);
        long limitBytes = limitMB * 1024L * 1024L;
        return cacheSize > limitBytes;
    }
}
