package com.icpx.service;

import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.cloud.firestore.SetOptions;
import com.icpx.database.TargetDAO;
import com.icpx.database.UserDAO;
import com.icpx.model.Target;
import com.icpx.model.User;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Service to synchronize data between SQLite and Firebase (Desktop version)
 */
public class FirebaseSyncService {

    private static FirebaseSyncService instance;

    public static FirebaseSyncService getInstance() {
        if (instance == null) {
            instance = new FirebaseSyncService();
        }
        return instance;
    }

    /**
     * Sync all local targets to Firebase
     */
    public CompletableFuture<String> syncTargetsToFirebase() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String userId = AuthService.getCurrentUserId();
                if (userId == null) {
                    throw new RuntimeException("User not authenticated");
                }

                Firestore db = FirebaseManager.getFirestore();
                if (db == null) {
                    throw new RuntimeException("Firebase not initialized");
                }

                List<Target> targets = TargetDAO.getAllTargetsForSync();
                int syncedCount = 0;

                for (Target target : targets) {
                    Map<String, Object> targetData = targetToMap(target);
                    
                    // Use the target ID as document ID to avoid duplicates
                    DocumentReference docRef = db.collection("users")
                            .document(userId)
                            .collection("targets")
                            .document(String.valueOf(target.getId()));
                    
                    docRef.set(targetData, SetOptions.merge()).get();
                    syncedCount++;
                }

                return "Synced " + syncedCount + " targets to cloud";
            } catch (Exception e) {
                throw new RuntimeException("Sync failed: " + e.getMessage(), e);
            }
        });
    }

    /**
     * Sync targets from Firebase to local SQLite
     */
    public CompletableFuture<String> syncTargetsFromFirebase() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String userId = AuthService.getCurrentUserId();
                if (userId == null) {
                    throw new RuntimeException("User not authenticated");
                }

                Firestore db = FirebaseManager.getFirestore();
                if (db == null) {
                    throw new RuntimeException("Firebase not initialized");
                }

                QuerySnapshot querySnapshot = db.collection("users")
                        .document(userId)
                        .collection("targets")
                        .get()
                        .get();

                int syncedCount = 0;

                for (QueryDocumentSnapshot doc : querySnapshot.getDocuments()) {
                    Target target = documentToTarget(doc);
                    if (target != null) {
                        // Check if target exists locally
                        Target existing = TargetDAO.getTargetById(target.getId());
                        if (existing == null) {
                            // Create new target
                            TargetDAO.insertTarget(target);
                        } else {
                            // Update existing target
                            TargetDAO.updateTarget(target);
                        }
                        syncedCount++;
                    }
                }

                return "Synced " + syncedCount + " targets from cloud";
            } catch (Exception e) {
                throw new RuntimeException("Sync failed: " + e.getMessage(), e);
            }
        });
    }

    /**
     * Sync solved problems history (problem id and link) to Firebase
     */
    public CompletableFuture<String> syncHistoryToFirebase() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String userId = AuthService.getCurrentUserId();
                if (userId == null) {
                    throw new RuntimeException("User not authenticated");
                }

                Firestore db = FirebaseManager.getFirestore();
                if (db == null) {
                    throw new RuntimeException("Firebase not initialized");
                }

                List<Target> achievedTargets = TargetDAO.getAchievedProblems();
                List<Map<String, Object>> historyList = new ArrayList<>();

                for (Target t : achievedTargets) {
                    Map<String, Object> entry = new HashMap<>();
                    entry.put("id", t.getId());
                    entry.put("problem_link", t.getProblemLink());
                    entry.put("name", t.getName());
                    entry.put("rating", t.getRating());
                    historyList.add(entry);
                }

                Map<String, Object> historyData = new HashMap<>();
                historyData.put("history", historyList);
                historyData.put("lastUpdated", System.currentTimeMillis());

                db.collection("users")
                        .document(userId)
                        .set(historyData, SetOptions.merge())
                        .get();

                return "Synced " + historyList.size() + " solved problems to history";
            } catch (Exception e) {
                throw new RuntimeException("History sync failed: " + e.getMessage(), e);
            }
        });
    }

    /**
     * Sync all time stats to Firebase
     */
    public CompletableFuture<String> syncAllTimeStatsToFirebase(int allTimeSolve, int allTimeHistory) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String userId = AuthService.getCurrentUserId();
                if (userId == null) {
                    throw new RuntimeException("User not authenticated");
                }

                Firestore db = FirebaseManager.getFirestore();
                if (db == null) {
                    throw new RuntimeException("Firebase not initialized");
                }

                Map<String, Object> statsData = new HashMap<>();
                statsData.put("allTimeSolve", allTimeSolve);
                statsData.put("allTimeHistory", allTimeHistory);
                statsData.put("lastUpdated", System.currentTimeMillis());

                db.collection("users")
                        .document(userId)
                        .set(statsData, SetOptions.merge())
                        .get();

                return "Stats synced successfully";
            } catch (Exception e) {
                throw new RuntimeException("Stats sync failed: " + e.getMessage(), e);
            }
        });
    }

    /**
     * Fetch all time stats from Firebase
     */
    public CompletableFuture<Map<String, Object>> fetchAllTimeStatsFromFirebase() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String userId = AuthService.getCurrentUserId();
                if (userId == null) {
                    throw new RuntimeException("User not authenticated");
                }

                Firestore db = FirebaseManager.getFirestore();
                if (db == null) {
                    throw new RuntimeException("Firebase not initialized");
                }

                DocumentSnapshot doc = db.collection("users")
                        .document(userId)
                        .get()
                        .get();

                if (doc.exists()) {
                    Map<String, Object> result = new HashMap<>();
                    Long allTimeSolve = doc.getLong("allTimeSolve");
                    Long allTimeHistory = doc.getLong("allTimeHistory");
                    result.put("allTimeSolve", allTimeSolve != null ? allTimeSolve.intValue() : 0);
                    result.put("allTimeHistory", allTimeHistory != null ? allTimeHistory.intValue() : 0);
                    return result;
                }

                return new HashMap<>();
            } catch (Exception e) {
                throw new RuntimeException("Fetch stats failed: " + e.getMessage(), e);
            }
        });
    }

    /**
     * Sync user data to Firebase
     */
    public CompletableFuture<String> syncUserDataToFirebase() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String userId = AuthService.getCurrentUserId();
                String userEmail = AuthService.getCurrentUserEmail();
                if (userId == null) {
                    throw new RuntimeException("User not authenticated");
                }

                Firestore db = FirebaseManager.getFirestore();
                if (db == null) {
                    throw new RuntimeException("Firebase not initialized");
                }

                User currentUser = UserDAO.getCurrentUser();
                if (currentUser == null) {
                    throw new RuntimeException("No local user data");
                }

                Map<String, Object> userData = new HashMap<>();
                userData.put("email", userEmail);
                userData.put("username", currentUser.getUsername());
                userData.put("startupPasswordEnabled", currentUser.isStartupPasswordEnabled());
                userData.put("lastSync", System.currentTimeMillis());
                userData.put("platform", "desktop");

                db.collection("users")
                        .document(userId)
                        .set(userData, SetOptions.merge())
                        .get();

                return "User data synced";
            } catch (Exception e) {
                throw new RuntimeException("User sync failed: " + e.getMessage(), e);
            }
        });
    }

    /**
     * Perform full bidirectional sync
     */
    public CompletableFuture<String> performFullSync() {
        return syncTargetsToFirebase()
                .thenCompose(result1 -> syncTargetsFromFirebase())
                .thenCompose(result2 -> syncHistoryToFirebase())
                .thenApply(result -> "Full sync completed successfully");
    }

    // Helper methods
    private Map<String, Object> targetToMap(Target target) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", String.valueOf(target.getId()));
        map.put("type", target.getType());
        map.put("name", target.getName());
        map.put("problemLink", target.getProblemLink());
        map.put("topicName", target.getTopicName());
        map.put("websiteUrl", target.getWebsiteUrl());
        map.put("status", target.getStatus());
        map.put("rating", target.getRating() != null ? target.getRating() : 0);
        map.put("archived", target.isArchived());
        
        if (target.getCreatedAt() != null) {
            map.put("createdAt", target.getCreatedAt()
                    .atZone(ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli());
        }
        
        return map;
    }

    private Target documentToTarget(QueryDocumentSnapshot doc) {
        try {
            Target target = new Target();
            
            String idStr = doc.getString("id");
            if (idStr != null) {
                target.setId(Integer.parseInt(idStr));
            }
            
            target.setType(doc.getString("type"));
            target.setName(doc.getString("name"));
            target.setProblemLink(doc.getString("problemLink"));
            target.setTopicName(doc.getString("topicName"));
            target.setWebsiteUrl(doc.getString("websiteUrl"));
            target.setStatus(doc.getString("status"));

            Long rating = doc.getLong("rating");
            target.setRating(rating != null ? rating.intValue() : 0);

            Boolean archived = doc.getBoolean("archived");
            target.setArchived(archived != null && archived);

            Long createdAt = doc.getLong("createdAt");
            if (createdAt != null) {
                target.setCreatedAt(java.time.Instant.ofEpochMilli(createdAt)
                        .atZone(ZoneId.systemDefault())
                        .toLocalDateTime());
            }

            return target;
        } catch (Exception e) {
            System.err.println("Error parsing target document: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    // Callback interface for sync operations
    public interface SyncCallback {
        void onSuccess(String message);
        void onFailure(Exception e);
    }
}
