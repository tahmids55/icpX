package com.icpx.android.firebase;

import android.content.Context;

import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.icpx.android.database.TargetDAO;
import com.icpx.android.database.UserDAO;
import com.icpx.android.model.Target;
import com.icpx.android.model.User;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service to synchronize data between SQLite and Firebase
 */
public class FirebaseSyncService {
                /**
                 * Sync solved problems history (problem id and link) to Firebase
                 * Also deletes history items that were removed locally
                 */
                public void syncHistoryToFirebase(FirebaseManager.FirestoreCallback callback) {
                    FirebaseUser firebaseUser = firebaseManager.getCurrentUser();
                    if (firebaseUser == null) {
                        callback.onFailure(new Exception("User not authenticated"));
                        return;
                    }
                    
                    final String userId = firebaseUser.getUid();
                    List<Target> history = targetDAO.getAchievedTargetsForHistory(firebaseUser.getEmail());
                    
                    // First, get all existing Firebase history documents
                    firebaseManager.getHistory(userId, new FirebaseManager.DataCallback() {
                        @Override
                        public void onSuccess(java.util.List<com.google.firebase.firestore.DocumentSnapshot> documents) {
                            // Build set of document IDs that should exist (from local history)
                            java.util.Set<String> localHistoryDocIds = new java.util.HashSet<>();
                            for (Target t : history) {
                                String problemLink = t.getProblemLink();
                                String docId = (problemLink != null && !problemLink.trim().isEmpty())
                                    ? generateDocIdFromLink(problemLink)
                                    : String.valueOf(t.getId());
                                localHistoryDocIds.add(docId);
                            }
                            
                            // [DISABLED] Find documents to delete (exist in Firebase but not locally)
                            // Prevent data loss in bidirectional sync
                            /*
                            java.util.List<String> toDelete = new java.util.ArrayList<>();
                            for (com.google.firebase.firestore.DocumentSnapshot doc : documents) {
                                if (!localHistoryDocIds.contains(doc.getId())) {
                                    toDelete.add(doc.getId());
                                }
                            }
                            
                            // Delete obsolete history documents
                            if (!toDelete.isEmpty()) {
                                final int[] deletedCount = {0};
                                for (String docId : toDelete) {
                                    firebaseManager.deleteHistory(userId, docId, new FirebaseManager.FirestoreCallback() {
                                        @Override
                                        public void onSuccess() {
                                            deletedCount[0]++;
                                            if (deletedCount[0] == toDelete.size()) {
                                                uploadLocalHistory(userId, history, callback);
                                            }
                                        }
                                        
                                        @Override
                                        public void onFailure(Exception e) {
                                            deletedCount[0]++;
                                            if (deletedCount[0] == toDelete.size()) {
                                                uploadLocalHistory(userId, history, callback);
                                            }
                                        }
                                    });
                                }
                            } else {
                                uploadLocalHistory(userId, history, callback);
                            }
                            */
                            uploadLocalHistory(userId, history, callback);
                        }
                        
                        @Override
                        public void onFailure(Exception e) {
                            // If we can't get Firebase docs, just upload local data
                            uploadLocalHistory(userId, history, callback);
                        }
                    });
                }
                
                /**
                 * Helper method to upload local history to Firebase
                 * Matches Desktop fields: id, problem_link, name, rating
                 */
                private void uploadLocalHistory(String userId, List<Target> history, FirebaseManager.FirestoreCallback callback) {
                    List<Map<String, Object>> historyList = new java.util.ArrayList<>();
                    for (Target t : history) {
                        Map<String, Object> entry = new java.util.HashMap<>();
                        entry.put("id", t.getId());
                        entry.put("problem_link", t.getProblemLink());
                        entry.put("name", t.getName());
                        entry.put("rating", t.getRating() != null ? t.getRating() : 0);
                        historyList.add(entry);
                    }
                    firebaseManager.updateHistory(userId, historyList, callback);
                }
            public TargetDAO getTargetDAO() {
                return targetDAO;
            }
        /**
         * Sync all time stats to Firebase
         */
        public void syncAllTimeStatsToFirebase(int allTimeSolve, int allTimeHistory, FirebaseManager.FirestoreCallback callback) {
            com.google.firebase.auth.FirebaseUser firebaseUser = firebaseManager.getCurrentUser();
            if (firebaseUser == null) {
                callback.onFailure(new Exception("User not authenticated"));
                return;
            }
            firebaseManager.updateAllTimeStats(firebaseUser.getUid(), allTimeSolve, allTimeHistory, callback);
        }

        /**
         * Fetch all time stats from Firebase
         */
        public void fetchAllTimeStatsFromFirebase(FirebaseManager.DataCallback callback) {
            com.google.firebase.auth.FirebaseUser firebaseUser = firebaseManager.getCurrentUser();
            if (firebaseUser == null) {
                callback.onFailure(new Exception("User not authenticated"));
                return;
            }
            firebaseManager.getAllTimeStats(firebaseUser.getUid(), callback);
        }
    
    private Context context;
    private FirebaseManager firebaseManager;
    private TargetDAO targetDAO;
    private UserDAO userDAO;
    
    public FirebaseSyncService(Context context) {
        this.context = context;
        this.firebaseManager = FirebaseManager.getInstance();
        this.targetDAO = new TargetDAO(context);
        this.userDAO = new UserDAO(context);
    }
    
    /**
     * Sync all local targets to Firebase
     * This includes deleting Firebase documents that no longer exist locally
     */
    public void syncTargetsToFirebase(SyncCallback callback) {
        FirebaseUser firebaseUser = firebaseManager.getCurrentUser();
        if (firebaseUser == null) {
            callback.onFailure(new Exception("User not authenticated"));
            return;
        }
        
        final String userId = firebaseUser.getUid();
        List<Target> targets = targetDAO.getAllTargets(firebaseUser.getEmail());
        
        // First, get all existing Firebase documents to find what needs to be deleted
        firebaseManager.getTargets(userId, new FirebaseManager.DataCallback() {
            @Override
            public void onSuccess(List<com.google.firebase.firestore.DocumentSnapshot> documents) {
                // Build set of document IDs that should exist (from local data)
                java.util.Set<String> localDocIds = new java.util.HashSet<>();
                for (Target target : targets) {
                    String problemLink = target.getProblemLink();
                    String docId = (problemLink != null && !problemLink.trim().isEmpty())
                        ? generateDocIdFromLink(problemLink)
                        : String.valueOf(target.getId());
                    localDocIds.add(docId);
                }
                
                // [DISABLED] Find documents to delete (exist in Firebase but not locally)
                // This logic is dangerous for bi-directional sync as it deletes items created on other devices
                /*
                java.util.List<String> toDelete = new java.util.ArrayList<>();
                for (com.google.firebase.firestore.DocumentSnapshot doc : documents) {
                    if (!localDocIds.contains(doc.getId())) {
                        toDelete.add(doc.getId());
                    }
                }
                
                // Delete obsolete documents
                final int[] deletedCount = {0};
                if (!toDelete.isEmpty()) {
                    for (String docId : toDelete) {
                        firebaseManager.deleteTarget(userId, docId, new FirebaseManager.FirestoreCallback() {
                            @Override
                            public void onSuccess() {
                                deletedCount[0]++;
                                // After all deletes, upload local targets
                                if (deletedCount[0] == toDelete.size()) {
                                    uploadLocalTargets(userId, targets, callback);
                                }
                            }
                            
                            @Override
                            public void onFailure(Exception e) {
                                // Continue even if delete fails
                                deletedCount[0]++;
                                if (deletedCount[0] == toDelete.size()) {
                                    uploadLocalTargets(userId, targets, callback);
                                }
                            }
                        });
                    }
                } else {
                    // No deletes needed, just upload
                    uploadLocalTargets(userId, targets, callback);
                }
                */
                
                // Just upload local targets without deleting anything from cloud
                uploadLocalTargets(userId, targets, callback);
            }
            
            @Override
            public void onFailure(Exception e) {
                // If we can't get Firebase docs, just upload local data
                uploadLocalTargets(userId, targets, callback);
            }
        });
    }
    
    /**
     * Helper method to upload local targets to Firebase
     */
    private void uploadLocalTargets(String userId, List<Target> targets, SyncCallback callback) {
        int totalTargets = targets.size();
        final int[] syncedCount = {0};
        
        if (totalTargets == 0) {
            callback.onSuccess("Synced 0 targets");
            return;
        }
        
        for (Target target : targets) {
            Map<String, Object> targetData = targetToMap(target);
            
            firebaseManager.saveTarget(userId, targetData, new FirebaseManager.FirestoreCallback() {
                @Override
                public void onSuccess() {
                    syncedCount[0]++;
                    if (syncedCount[0] == totalTargets) {
                        callback.onSuccess("Synced " + totalTargets + " targets");
                    }
                }
                
                @Override
                public void onFailure(Exception e) {
                    callback.onFailure(e);
                }
            });
        }
    }
    
    /**
     * Generate document ID from problem link (must match Desktop and FirebaseManager)
     */
    private String generateDocIdFromLink(String problemLink) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(problemLink.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 10; i++) {
                sb.append(String.format("%02x", hash[i]));
            }
            return sb.toString();
        } catch (Exception e) {
            return problemLink.replaceAll("[^a-zA-Z0-9]", "_");
        }
    }
    
    /**
     * Sync targets from Firebase to local SQLite
     */
    public void syncTargetsFromFirebase(SyncCallback callback) {
        FirebaseUser firebaseUser = firebaseManager.getCurrentUser();
        if (firebaseUser == null) {
            callback.onFailure(new Exception("User not authenticated"));
            return;
        }
        
        firebaseManager.getTargets(firebaseUser.getUid(), new FirebaseManager.DataCallback() {
            @Override
            public void onSuccess(List<DocumentSnapshot> documents) {
                int syncedCount = 0;
                int createdCount = 0;
                int updatedCount = 0;
                
                android.util.Log.d("FirebaseSyncService", "Received " + documents.size() + " documents from Firebase");
                
                for (DocumentSnapshot doc : documents) {
                    Target target = documentToTarget(doc);
                    if (target != null) {
                        // Primary check: by problem link (most reliable for cross-device sync)
                        Target existingByLink = null;
                        if (target.getProblemLink() != null && !target.getProblemLink().isEmpty()) {
                            existingByLink = targetDAO.getTargetByProblemLink(target.getProblemLink(), firebaseUser.getEmail());
                        }
                        
                        if (existingByLink != null) {
                            // Update existing target found by link
                            target.setId(existingByLink.getId()); // Keep the same local ID
                            targetDAO.updateTarget(target);
                            updatedCount++;
                            syncedCount++;
                        } else {
                            // New target from cloud - create it locally
                            // createTarget has built-in duplicate check
                            long newId = targetDAO.createTarget(target, firebaseUser.getEmail());
                            if (newId > 0) {
                                createdCount++;
                                syncedCount++;
                            }
                        }
                    } else {
                        android.util.Log.w("FirebaseSyncService", "Failed to parse document: " + doc.getId());
                    }
                }
                
                android.util.Log.d("FirebaseSyncService", "Sync complete: " + createdCount + " created, " + updatedCount + " updated");
                callback.onSuccess("Synced " + syncedCount + " targets from cloud (" + createdCount + " new, " + updatedCount + " updated)");
            }
            
            @Override
            public void onFailure(Exception e) {
                android.util.Log.e("FirebaseSyncService", "Failed to get targets from Firebase", e);
                callback.onFailure(e);
            }
        });
    }
    
    /**
     * Sync user data to Firebase
     */
    public void syncUserDataToFirebase(SyncCallback callback) {
        FirebaseUser firebaseUser = firebaseManager.getCurrentUser();
        if (firebaseUser == null) {
            callback.onFailure(new Exception("User not authenticated"));
            return;
        }
        
        User currentUser = userDAO.getCurrentUser();
        if (currentUser == null) {
            callback.onFailure(new Exception("No local user data"));
            return;
        }
        
        Map<String, Object> userData = new HashMap<>();
        userData.put("username", currentUser.getUsername());
        userData.put("codeforcesHandle", currentUser.getCodeforcesHandle());
        userData.put("startupPasswordEnabled", currentUser.isStartupPasswordEnabled());
        
        firebaseManager.saveUserData(firebaseUser.getUid(), userData, new FirebaseManager.FirestoreCallback() {
            @Override
            public void onSuccess() {
                callback.onSuccess("User data synced");
            }
            
            @Override
            public void onFailure(Exception e) {
                callback.onFailure(e);
            }
        });
    }
    
    /**
     * Sync history from Firebase to local SQLite
     * Downloads history items that exist in cloud but not locally
     */
    public void syncHistoryFromFirebase(SyncCallback callback) {
        FirebaseUser firebaseUser = firebaseManager.getCurrentUser();
        if (firebaseUser == null) {
            callback.onFailure(new Exception("User not authenticated"));
            return;
        }
        
        final String userId = firebaseUser.getUid();
        final String userEmail = firebaseUser.getEmail();
        
        firebaseManager.getHistory(userId, new FirebaseManager.DataCallback() {
            @Override
            public void onSuccess(java.util.List<com.google.firebase.firestore.DocumentSnapshot> documents) {
                int importedCount = 0;
                
                android.util.Log.d("FirebaseSyncService", "Received " + documents.size() + " history documents from Firebase");
                
                for (com.google.firebase.firestore.DocumentSnapshot doc : documents) {
                    String problemLink = doc.getString("problem_link");
                    if (problemLink == null || problemLink.trim().isEmpty()) {
                        continue;
                    }
                    
                    // Check if this item already exists locally (including deleted/archived)
                    Target existingIncludingDeleted = targetDAO.getTargetByProblemLinkIncludingDeleted(problemLink, userEmail);
                    
                    if (existingIncludingDeleted != null) {
                        // Item already exists locally - don't recreate it
                        // Whether it's active, achieved, or deleted - respect the local state
                        android.util.Log.d("FirebaseSyncService", "History item already exists locally (deleted=" + existingIncludingDeleted.isDeleted() + "): " + problemLink);
                        continue;
                    }
                    
                    // New history item from cloud - create it locally as achieved+archived
                    Target t = new Target();
                    t.setProblemLink(problemLink);
                    
                    String name = doc.getString("name");
                    t.setName(name != null ? name : "Synced History Problem");
                    
                    Long rating = doc.getLong("rating");
                    t.setRating(rating != null ? rating.intValue() : 0);
                    
                    // History items are achieved and archived
                    t.setStatus("achieved");
                    t.setType("Practice");
                    t.setTopicName("General");
                    t.setWebsiteUrl("");
                    t.setDeleted(true); // archived = true for history
                    t.setCreatedAt(new java.util.Date());
                    
                    long newId = targetDAO.createTarget(t, userEmail);
                    if (newId > 0) {
                        importedCount++;
                        android.util.Log.d("FirebaseSyncService", "Imported new history item: " + problemLink);
                    }
                }
                
                android.util.Log.d("FirebaseSyncService", "History sync complete: " + importedCount + " imported");
                callback.onSuccess("Synced " + importedCount + " history items from cloud");
            }
            
            @Override
            public void onFailure(Exception e) {
                android.util.Log.e("FirebaseSyncService", "Failed to get history from Firebase", e);
                callback.onFailure(e);
            }
        });
    }
    
    /**
     * Perform full bidirectional sync
     * Order: Upload targets -> Download targets -> Upload history -> Download history
     */
    public void performFullSync(SyncCallback callback) {
        syncTargetsToFirebase(new SyncCallback() {
            @Override
            public void onSuccess(String message) {
                syncTargetsFromFirebase(new SyncCallback() {
                    @Override
                    public void onSuccess(String msg2) {
                        // Also sync history bidirectionally
                        syncHistoryFromFirebase(callback);
                    }
                    
                    @Override
                    public void onFailure(Exception e) {
                        callback.onFailure(e);
                    }
                });
            }
            
            @Override
            public void onFailure(Exception e) {
                callback.onFailure(e);
            }
        });
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
        map.put("rating", target.getRating());
        map.put("createdAt", target.getCreatedAt().getTime());
        // Map deleted to archived for Desktop compatibility
        map.put("archived", target.isDeleted());
        return map;
    }
    
    private Target documentToTarget(DocumentSnapshot doc) {
        try {
            Target target = new Target();
            
            // ID from cloud is the source device's local ID - don't rely on it
            // Use -1 to indicate it needs a new local ID when inserting
            Object idObj = doc.get("id");
            if (idObj != null) {
                try {
                    if (idObj instanceof String) {
                        target.setId(Integer.parseInt((String) idObj));
                    } else if (idObj instanceof Long) {
                        target.setId(((Long) idObj).intValue());
                    } else if (idObj instanceof Number) {
                        target.setId(((Number) idObj).intValue());
                    }
                } catch (NumberFormatException e) {
                    target.setId(-1); // Will get new ID on insert
                }
            } else {
                target.setId(-1);
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
            if (archived != null) {
                target.setDeleted(archived);
            }
            
            Long createdAt = doc.getLong("createdAt");
            if (createdAt != null) {
                target.setCreatedAt(new java.util.Date(createdAt));
            } else {
                target.setCreatedAt(new java.util.Date());
            }
            
            return target;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    
    // Callback interface
    public interface SyncCallback {
        void onSuccess(String message);
        void onFailure(Exception e);
    }
}
