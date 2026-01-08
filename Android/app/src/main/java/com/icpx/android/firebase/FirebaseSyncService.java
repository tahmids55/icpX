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
     */
    public void syncTargetsToFirebase(SyncCallback callback) {
        FirebaseUser firebaseUser = firebaseManager.getCurrentUser();
        if (firebaseUser == null) {
            callback.onFailure(new Exception("User not authenticated"));
            return;
        }
        
        List<Target> targets = targetDAO.getAllTargets(firebaseUser.getEmail());
        int totalTargets = targets.size();
        final int[] syncedCount = {0};
        
        if (totalTargets == 0) {
            callback.onSuccess("No targets to sync");
            return;
        }
        
        for (Target target : targets) {
            Map<String, Object> targetData = targetToMap(target);
            
            firebaseManager.saveTarget(firebaseUser.getUid(), targetData, new FirebaseManager.FirestoreCallback() {
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
                
                for (DocumentSnapshot doc : documents) {
                    Target target = documentToTarget(doc);
                    if (target != null) {
                        // First check by problem link to avoid duplicates
                        Target existingByLink = null;
                        if (target.getProblemLink() != null && !target.getProblemLink().isEmpty()) {
                            existingByLink = targetDAO.getTargetByProblemLink(target.getProblemLink(), firebaseUser.getEmail());
                        }
                        
                        if (existingByLink != null) {
                            // Update existing target by link
                            target.setId(existingByLink.getId()); // Keep the same local ID
                            targetDAO.updateTarget(target);
                            syncedCount++;
                        } else {
                            // Check by ID as fallback
                            Target existingById = targetDAO.getTargetById(target.getId());
                            if (existingById == null) {
                                // Create new target (duplicate check is built into createTarget)
                                targetDAO.createTarget(target, firebaseUser.getEmail());
                                syncedCount++;
                            } else {
                                // Update existing target by ID
                                targetDAO.updateTarget(target);
                                syncedCount++;
                            }
                        }
                    }
                }
                
                callback.onSuccess("Synced " + syncedCount + " targets from cloud");
            }
            
            @Override
            public void onFailure(Exception e) {
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
     * Perform full bidirectional sync
     */
    public void performFullSync(SyncCallback callback) {
        syncTargetsToFirebase(new SyncCallback() {
            @Override
            public void onSuccess(String message) {
                syncTargetsFromFirebase(callback);
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
        return map;
    }
    
    private Target documentToTarget(DocumentSnapshot doc) {
        try {
            Target target = new Target();
            target.setId(Integer.parseInt(doc.getString("id")));
            target.setType(doc.getString("type"));
            target.setName(doc.getString("name"));
            target.setProblemLink(doc.getString("problemLink"));
            target.setTopicName(doc.getString("topicName"));
            target.setWebsiteUrl(doc.getString("websiteUrl"));
            target.setStatus(doc.getString("status"));
            
            Long rating = doc.getLong("rating");
            target.setRating(rating != null ? rating.intValue() : 0);
            
            Long createdAt = doc.getLong("createdAt");
            if (createdAt != null) {
                target.setCreatedAt(new java.util.Date(createdAt));
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
