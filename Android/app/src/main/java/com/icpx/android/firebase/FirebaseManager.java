package com.icpx.android.firebase;

import android.util.Log;

import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

/**
 * Firebase manager for authentication and Firestore operations
 */
public class FirebaseManager {
    private static final String TAG = "FirebaseManager";
    private static FirebaseManager instance;
    private FirebaseAuth auth;
    private FirebaseFirestore firestore;
    
    private FirebaseManager() {
        auth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
    }
    
    public static synchronized FirebaseManager getInstance() {
        if (instance == null) {
            instance = new FirebaseManager();
        }
        return instance;
    }
    
    /**
     * Get current Firebase user
     */
    public FirebaseUser getCurrentUser() {
        return auth.getCurrentUser();
    }
    
    /**
     * Sign in with email and password
     */
    public void signIn(String email, String password, AuthCallback callback) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener(authResult -> callback.onSuccess(authResult.getUser()))
            .addOnFailureListener(callback::onFailure);
    }
    
    /**
     * Create new user with email and password
     */
    public void signUp(String email, String password, AuthCallback callback) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener(authResult -> callback.onSuccess(authResult.getUser()))
            .addOnFailureListener(callback::onFailure);
    }
    
    /**
     * Sign out current user
     */
    public void signOut() {
        auth.signOut();
    }
    
    /**
     * Sign in with Google account
     */
    public void signInWithGoogle(GoogleSignInAccount account, AuthCallback callback) {
        AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
        auth.signInWithCredential(credential)
            .addOnSuccessListener(authResult -> {
                // Sync user profile on successful login
                syncUserProfile();
                callback.onSuccess(authResult.getUser());
            })
            .addOnFailureListener(callback::onFailure);
    }
    
    /**
     * Update solved problems history as a subcollection 'history' under the user document
     * Uses problem_link hash as document ID for consistency across devices
     */
    public void updateHistory(String userId, java.util.List<java.util.Map<String, Object>> historyList, FirestoreCallback callback) {
        if (historyList == null || historyList.isEmpty()) {
            callback.onSuccess();
            return;
        }
        // Use a batch write for efficiency
        com.google.firebase.firestore.WriteBatch batch = firestore.batch();
        for (java.util.Map<String, Object> entry : historyList) {
            String problemLink = (String) entry.get("problem_link");
            String docId;
            
            // Use problem_link hash as document ID for consistency across devices
            if (problemLink != null && !problemLink.trim().isEmpty()) {
                docId = generateDocIdFromLink(problemLink);
            } else {
                // Fallback to numeric ID
                Object idObj = entry.get("id");
                docId = (idObj != null) ? String.valueOf(idObj) : java.util.UUID.randomUUID().toString();
            }
            
            batch.set(
                firestore.collection("users")
                    .document(userId)
                    .collection("history")
                    .document(docId),
                entry,
                SetOptions.merge()
            );
        }
        batch.commit()
            .addOnSuccessListener(aVoid -> callback.onSuccess())
            .addOnFailureListener(callback::onFailure);
    }
    
    /**
     * Update all time stats (solve/history) in Firestore user document
     */
    public void updateAllTimeStats(String userId, int allTimeSolve, int allTimeHistory, FirestoreCallback callback) {
        Map<String, Object> stats = new HashMap<>();
        stats.put("all_time_solve", allTimeSolve);
        stats.put("all_time_history", allTimeHistory);
        firestore.collection("users")
                .document(userId)
                .set(stats, SetOptions.merge())
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * Get all time stats from Firestore user document
     */
    public void getAllTimeStats(String userId, DataCallback callback) {
        firestore.collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    java.util.List<com.google.firebase.firestore.DocumentSnapshot> list = new java.util.ArrayList<>();
                    list.add(documentSnapshot);
                    callback.onSuccess(list);
                })
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * Save daily activity data to Firestore
     * @param userId User ID
     * @param date Date in format "yyyy-MM-dd"
     * @param problemCount Count of problems solved on this day
     * @param topicCount Count of topics learned on this day
     */
    public void saveDailyActivity(String userId, String date, int problemCount, int topicCount, FirestoreCallback callback) {
        Map<String, Object> activity = new HashMap<>();
        activity.put("problemCount", problemCount);
        activity.put("topicCount", topicCount);
        activity.put("timestamp", System.currentTimeMillis());
        
        firestore.collection("users")
                .document(userId)
                .collection("daily_activity")
                .document(date)
                .set(activity, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Daily activity saved: " + date);
                    callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to save daily activity: " + date, e);
                    callback.onFailure(e);
                });
    }

    /**
     * Batch save daily activity data for multiple dates
     * @param userId User ID
     * @param activityMap Map of date -> Map with "problemCount" and "topicCount"
     */
    public void batchSaveDailyActivity(String userId, Map<String, Map<String, Integer>> activityMap, FirestoreCallback callback) {
        if (activityMap == null || activityMap.isEmpty()) {
            callback.onSuccess();
            return;
        }
        
        com.google.firebase.firestore.WriteBatch batch = firestore.batch();
        for (Map.Entry<String, Map<String, Integer>> entry : activityMap.entrySet()) {
            String date = entry.getKey();
            Map<String, Integer> counts = entry.getValue();
            
            Map<String, Object> activity = new HashMap<>();
            activity.put("problemCount", counts.getOrDefault("problemCount", 0));
            activity.put("topicCount", counts.getOrDefault("topicCount", 0));
            activity.put("timestamp", System.currentTimeMillis());
            
            batch.set(
                firestore.collection("users")
                    .document(userId)
                    .collection("daily_activity")
                    .document(date),
                activity,
                SetOptions.merge()
            );
        }
        
        batch.commit()
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "Batch saved " + activityMap.size() + " daily activities");
                callback.onSuccess();
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Failed to batch save daily activities", e);
                callback.onFailure(e);
            });
    }

    /**
     * Get daily activity data from Firestore for a date range
     * @param userId User ID
     * @param callback Callback with list of activity documents
     */
    public void getDailyActivity(String userId, DataCallback callback) {
        firestore.collection("users")
                .document(userId)
                .collection("daily_activity")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    callback.onSuccess(querySnapshot.getDocuments());
                })
                .addOnFailureListener(callback::onFailure);
    }
    
    /**
     * Save target to Firestore
     * Uses problem_link hash as document ID to prevent duplicates across devices
     */
    public void saveTarget(String userId, Map<String, Object> targetData, FirestoreCallback callback) {
        String problemLink = (String) targetData.get("problemLink");
        String docId;
        
        // Use problem_link hash as document ID for consistency across devices
        if (problemLink != null && !problemLink.trim().isEmpty()) {
            docId = generateDocIdFromLink(problemLink);
        } else {
            // Fallback to numeric ID if no problem link
            docId = (String) targetData.get("id");
        }
        
        firestore.collection("users")
                .document(userId)
                .collection("targets")
                .document(docId)
                .set(targetData, SetOptions.merge())
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(callback::onFailure);
    }
    
    /**
     * Generate a consistent document ID from problem link
     * Uses SHA-256 hash truncated to 20 chars for Firestore doc ID
     * Must match Desktop implementation exactly
     */
    private String generateDocIdFromLink(String problemLink) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(problemLink.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 10; i++) { // 10 bytes = 20 hex chars
                sb.append(String.format("%02x", hash[i]));
            }
            return sb.toString();
        } catch (Exception e) {
            // Fallback: sanitize the URL to make it a valid doc ID
            return problemLink.replaceAll("[^a-zA-Z0-9]", "_");
        }
    }
    
    /**
     * Get all targets for a user
     */
    public void getTargets(String userId, DataCallback callback) {
        firestore.collection("users")
                .document(userId)
                .collection("targets")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    callback.onSuccess(queryDocumentSnapshots.getDocuments());
                })
                .addOnFailureListener(callback::onFailure);
    }
    
    /**
     * Get all history for a user
     */
    public void getHistory(String userId, DataCallback callback) {
        firestore.collection("users")
                .document(userId)
                .collection("history")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    callback.onSuccess(queryDocumentSnapshots.getDocuments());
                })
                .addOnFailureListener(callback::onFailure);
    }
    
    /**
     * Delete a history entry from Firestore
     */
    public void deleteHistory(String userId, String historyId, FirestoreCallback callback) {
        firestore.collection("users")
                .document(userId)
                .collection("history")
                .document(historyId)
                .delete()
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(callback::onFailure);
    }
    
    /**
     * Save user data to Firestore
     */
    public void saveUserData(String userId, Map<String, Object> userData, FirestoreCallback callback) {
        Log.d(TAG, "saveUserData called for userId: " + userId);
        firestore.collection("users")
                .document(userId)
                .set(userData, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "saveUserData successful");
                    callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "saveUserData failed", e);
                    callback.onFailure(e);
                });
    }
    
    /**
     * Get user data from Firestore
     */
    public void getUserData(String userId, DataCallback callback) {
        firestore.collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        java.util.List<com.google.firebase.firestore.DocumentSnapshot> list = new java.util.ArrayList<>();
                        list.add(documentSnapshot);
                        callback.onSuccess(list);
                    } else {
                        callback.onFailure(new Exception("User document not found"));
                    }
                })
                .addOnFailureListener(callback::onFailure);
    }
    
    /**
     * Delete target from Firestore
     */
    public void deleteTarget(String userId, String targetId, FirestoreCallback callback) {
        firestore.collection("users")
                .document(userId)
                .collection("targets")
                .document(targetId)
                .delete()
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(callback::onFailure);
    }
    
    /**
     * Sync user's public profile to userProfiles collection
     * This allows friends to find and view this user's stats
     */
    public void syncUserProfile() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null || user.getEmail() == null) return;
        
        String email = user.getEmail().toLowerCase();
        String uid = user.getUid();
        
        // Sanitize email for document ID
        String emailKey = email.replace(".", "_").replace("@", "_at_");
        
        Map<String, Object> profileData = new HashMap<>();
        profileData.put("uid", uid);
        profileData.put("email", email);
        profileData.put("lastUpdated", System.currentTimeMillis());
        
        firestore.collection("userProfiles").document(emailKey)
            .set(profileData, SetOptions.merge())
            .addOnSuccessListener(aVoid -> Log.d(TAG, "User profile synced: " + email))
            .addOnFailureListener(e -> Log.e(TAG, "Error syncing profile: " + e.getMessage()));
    }
    
    /**
     * Sync user profile with rating
     */
    public void syncUserProfileWithRating(double rating) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null || user.getEmail() == null) return;
        
        String email = user.getEmail().toLowerCase();
        String uid = user.getUid();
        
        // Sanitize email for document ID
        String emailKey = email.replace(".", "_").replace("@", "_at_");
        
        Map<String, Object> profileData = new HashMap<>();
        profileData.put("uid", uid);
        profileData.put("email", email);
        profileData.put("rating", rating);
        profileData.put("lastUpdated", System.currentTimeMillis());
        
        firestore.collection("userProfiles").document(emailKey)
            .set(profileData, SetOptions.merge())
            .addOnSuccessListener(aVoid -> Log.d(TAG, "User profile synced with rating: " + rating))
            .addOnFailureListener(e -> Log.e(TAG, "Error syncing profile: " + e.getMessage()));
    }
    
    // Callback interfaces
    public interface AuthCallback {
        void onSuccess(FirebaseUser user);
        void onFailure(Exception e);
    }
    
    public interface FirestoreCallback {
        void onSuccess();
        void onFailure(Exception e);
    }
    
    public interface DataCallback {
        void onSuccess(java.util.List<com.google.firebase.firestore.DocumentSnapshot> documents);
        void onFailure(Exception e);
    }
}
