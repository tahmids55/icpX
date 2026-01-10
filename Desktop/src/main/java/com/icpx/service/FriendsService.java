package com.icpx.service;

import com.google.cloud.firestore.*;
import com.google.firebase.auth.UserRecord;
import com.icpx.database.FriendsDAO;

import java.util.*;
import java.util.concurrent.ExecutionException;

/**
 * Service for Friends feature with Firebase sync
 * Allows users to add friends and view their ratings/heatmaps
 */
public class FriendsService {

    private final Firestore db;

    public FriendsService() {
        this.db = FirebaseManager.getFirestore();
    }

    /**
     * Add a friend and sync to Firebase
     * Also looks up and stores the friend's UID for later access
     * @param userEmail Current user's email
     * @param friendEmail Friend's email to add
     * @return true if successful
     */
    public boolean addFriend(String userEmail, String friendEmail) {
        // Add locally first
        boolean localSuccess = FriendsDAO.addFriend(userEmail, friendEmail);
        
        if (localSuccess && db != null && AuthService.isAuthenticated()) {
            try {
                String uid = AuthService.getCurrentUserId();
                String friendEmailLower = friendEmail.toLowerCase();
                
                Map<String, Object> friendData = new HashMap<>();
                friendData.put("friendEmail", friendEmailLower);
                friendData.put("addedAt", System.currentTimeMillis());
                
                // Try to look up friend's UID and rating from userProfiles
                String emailKey = friendEmailLower.replace(".", "_").replace("@", "_at_");
                boolean foundFriend = false;
                try {
                    DocumentSnapshot profileDoc = db.collection("userProfiles").document(emailKey).get().get();
                    if (profileDoc.exists()) {
                        String friendUid = profileDoc.getString("uid");
                        Double friendRating = profileDoc.getDouble("rating");
                        if (friendUid != null) {
                            friendData.put("friendUid", friendUid);
                            foundFriend = true;
                        }
                        if (friendRating != null) {
                            friendData.put("friendRating", friendRating);
                        }
                        System.out.println("Found friend profile: uid=" + friendUid + ", rating=" + friendRating);
                    }
                } catch (Exception e) {
                    System.err.println("Could not look up userProfiles: " + e.getMessage());
                }
                
                // Try querying users collection as fallback
                if (!foundFriend) {
                    try {
                        QuerySnapshot userQuery = db.collection("users")
                            .whereEqualTo("email", friendEmailLower)
                            .get().get();
                        if (!userQuery.isEmpty()) {
                            DocumentSnapshot userDoc = userQuery.getDocuments().get(0);
                            friendData.put("friendUid", userDoc.getId());
                            Double rating = userDoc.getDouble("rating");
                            if (rating != null) {
                                friendData.put("friendRating", rating);
                            }
                            foundFriend = true;
                            System.out.println("Found friend in users: uid=" + userDoc.getId());
                        }
                    } catch (Exception e) {
                        System.err.println("Could not query users: " + e.getMessage());
                    }
                }
                
                // Use Firebase Auth Admin SDK to lookup user by email
                if (!foundFriend) {
                    try {
                        UserRecord userRecord = FirebaseManager.getUserByEmail(friendEmailLower);
                        if (userRecord != null) {
                            friendData.put("friendUid", userRecord.getUid());
                            foundFriend = true;
                            System.out.println("Found friend in Firebase Auth: uid=" + userRecord.getUid());
                        }
                    } catch (Exception e) {
                        System.err.println("Could not look up Firebase Auth: " + e.getMessage());
                    }
                }
                
                db.collection("users").document(uid)
                  .collection("friends").document(friendEmailLower)
                  .set(friendData).get();
                  
                System.out.println("Friend synced to Firebase: " + friendEmail);
            } catch (Exception e) {
                System.err.println("Error syncing friend to Firebase: " + e.getMessage());
            }
        }
        
        return localSuccess;
    }

    /**
     * Remove a friend and sync to Firebase
     */
    public boolean removeFriend(String userEmail, String friendEmail) {
        boolean localSuccess = FriendsDAO.removeFriend(userEmail, friendEmail);
        
        if (localSuccess && db != null && AuthService.isAuthenticated()) {
            try {
                String uid = AuthService.getCurrentUserId();
                db.collection("users").document(uid)
                  .collection("friends").document(friendEmail.toLowerCase())
                  .delete().get();
                  
                System.out.println("Friend removed from Firebase: " + friendEmail);
            } catch (Exception e) {
                System.err.println("Error removing friend from Firebase: " + e.getMessage());
            }
        }
        
        return localSuccess;
    }

    /**
     * Get friend's public data (rating and activity)
     * First checks if we have stored UID in our friends collection
     * @param friendEmail Friend's email
     * @return Map with rating, heatmap data, and other public info
     */
    public Map<String, Object> getFriendData(String friendEmail) {
        Map<String, Object> result = new HashMap<>();
        
        if (db == null || !AuthService.isAuthenticated()) {
            result.put("error", "Not authenticated");
            return result;
        }
        
        try {
            String friendEmailLower = friendEmail.toLowerCase();
            String currentUid = AuthService.getCurrentUserId();
            String friendUid = null;
            Double rating = 5.0; // Default rating (scale 1-10)
            
            // FIRST: Check if we already have the friend's UID stored in our friends collection
            try {
                DocumentSnapshot friendDoc = db.collection("users").document(currentUid)
                    .collection("friends").document(friendEmailLower).get().get();
                
                if (friendDoc.exists()) {
                    friendUid = friendDoc.getString("friendUid");
                    Double storedRating = friendDoc.getDouble("friendRating");
                    if (storedRating != null) {
                        rating = storedRating;
                    }
                    System.out.println("Got friend UID from stored data: " + friendUid);
                }
            } catch (Exception e) {
                System.err.println("Error reading friend doc: " + e.getMessage());
            }
            
            // SECOND: If no stored UID, try userProfiles collection
            if (friendUid == null) {
                String emailKey = friendEmailLower.replace(".", "_").replace("@", "_at_");
                System.out.println("Looking up userProfiles with key: " + emailKey);
                try {
                    DocumentSnapshot profileDoc = db.collection("userProfiles").document(emailKey).get().get();
                    System.out.println("userProfiles lookup result - exists: " + profileDoc.exists());
                    if (profileDoc.exists()) {
                        friendUid = profileDoc.getString("uid");
                        Double ratingVal = profileDoc.getDouble("rating");
                        if (ratingVal != null) rating = ratingVal;
                        System.out.println("Got friend from userProfiles: " + friendUid + ", rating: " + rating);
                        
                        // Update stored friend data with UID for next time
                        if (friendUid != null) {
                            try {
                                Map<String, Object> updateData = new HashMap<>();
                                updateData.put("friendUid", friendUid);
                                updateData.put("friendRating", rating);
                                db.collection("users").document(currentUid)
                                    .collection("friends").document(friendEmailLower)
                                    .update(updateData);
                                System.out.println("Updated stored friend with UID: " + friendUid);
                            } catch (Exception ex) {
                                System.err.println("Could not update friend doc: " + ex.getMessage());
                            }
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Error reading userProfiles: " + e.getMessage());
                    e.printStackTrace();
                }
            }
            
            // THIRD: Fallback - Query users collection by email (may fail due to permissions)
            if (friendUid == null) {
                System.out.println("Trying users collection query for: " + friendEmailLower);
                try {
                    QuerySnapshot userQuery = db.collection("users")
                        .whereEqualTo("email", friendEmailLower)
                        .get().get();
                    
                    System.out.println("Users query returned: " + userQuery.size() + " results");
                    if (!userQuery.isEmpty()) {
                        DocumentSnapshot userDoc = userQuery.getDocuments().get(0);
                        friendUid = userDoc.getId();
                        Double ratingVal = userDoc.getDouble("rating");
                        if (ratingVal != null) rating = ratingVal;
                        System.out.println("Got friend from users query: " + friendUid + ", rating: " + rating);
                        
                        // Update stored friend data with UID for next time
                        try {
                            Map<String, Object> updateData = new HashMap<>();
                            updateData.put("friendUid", friendUid);
                            updateData.put("friendRating", rating);
                            db.collection("users").document(currentUid)
                                .collection("friends").document(friendEmailLower)
                                .update(updateData);
                            System.out.println("Updated stored friend with UID: " + friendUid);
                        } catch (Exception ex) {
                            System.err.println("Could not update friend doc: " + ex.getMessage());
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Error querying users: " + e.getMessage());
                    e.printStackTrace();
                }
            }
            
            // FOURTH: Use Firebase Auth Admin SDK to lookup user by email directly
            if (friendUid == null) {
                System.out.println("Trying Firebase Auth lookup for: " + friendEmailLower);
                try {
                    UserRecord userRecord = FirebaseManager.getUserByEmail(friendEmailLower);
                    if (userRecord != null) {
                        friendUid = userRecord.getUid();
                        System.out.println("Got friend from Firebase Auth: " + friendUid);
                        
                        // Update stored friend data with UID for next time
                        try {
                            Map<String, Object> updateData = new HashMap<>();
                            updateData.put("friendUid", friendUid);
                            updateData.put("friendRating", rating);
                            db.collection("users").document(currentUid)
                                .collection("friends").document(friendEmailLower)
                                .update(updateData);
                            System.out.println("Updated stored friend with UID: " + friendUid);
                        } catch (Exception ex) {
                            System.err.println("Could not update friend doc: " + ex.getMessage());
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Error looking up user in Firebase Auth: " + e.getMessage());
                }
            }
            
            if (friendUid == null) {
                result.put("error", "User not found. Make sure they have signed in to icpX with this email.");
                return result;
            }
            
            result.put("email", friendEmail);
            result.put("rating", rating);
            
            // Get activity data (heatmap) - with error handling for permissions
            Map<String, Integer> activity = new HashMap<>();
            try {
                QuerySnapshot dailyQuery = db.collection("users").document(friendUid)
                    .collection("dailyActivity")
                    .orderBy("date", Query.Direction.DESCENDING)
                    .limit(365)
                    .get().get();
                
                for (DocumentSnapshot doc : dailyQuery.getDocuments()) {
                    String date = doc.getString("date");
                    Long count = doc.getLong("count");
                    if (date != null && count != null) {
                        activity.put(date, count.intValue());
                    }
                }
            } catch (Exception e) {
                System.err.println("Could not fetch activity (permission issue): " + e.getMessage());
            }
            result.put("activity", activity);
            
            // Get total solved count - with error handling for permissions
            int totalSolved = 0;
            try {
                QuerySnapshot targetsQuery = db.collection("users").document(friendUid)
                    .collection("targets")
                    .whereEqualTo("status", "achieved")
                    .get().get();
                totalSolved = targetsQuery.size();
            } catch (Exception e) {
                System.err.println("Could not fetch targets (permission issue): " + e.getMessage());
            }
            result.put("totalSolved", totalSolved);
            
        } catch (Exception e) {
            System.err.println("Error getting friend data: " + e.getMessage());
            result.put("error", e.getMessage());
        }
        
        return result;
    }

    /**
     * Get list of all friends with their basic data
     */
    public List<Map<String, Object>> getAllFriendsData(String userEmail) {
        List<Map<String, Object>> friendsData = new ArrayList<>();
        List<String> friends = FriendsDAO.getFriends(userEmail);
        
        for (String friendEmail : friends) {
            Map<String, Object> data = getFriendData(friendEmail);
            if (!data.containsKey("error")) {
                friendsData.add(data);
            }
        }
        
        return friendsData;
    }

    /**
     * Sync friends from Firebase to local
     */
    public void syncFriendsFromFirebase(String userEmail) {
        if (db == null || !AuthService.isAuthenticated()) {
            return;
        }
        
        try {
            String uid = AuthService.getCurrentUserId();
            QuerySnapshot friendsQuery = db.collection("users").document(uid)
                .collection("friends")
                .get().get();
            
            for (DocumentSnapshot doc : friendsQuery.getDocuments()) {
                String friendEmail = doc.getString("friendEmail");
                if (friendEmail != null) {
                    FriendsDAO.addFriend(userEmail.toLowerCase(), friendEmail.toLowerCase());
                }
            }
            
            System.out.println("Synced " + friendsQuery.size() + " friends from Firebase");
        } catch (Exception e) {
            System.err.println("Error syncing friends from Firebase: " + e.getMessage());
        }
    }

    /**
     * Sync user's rating to Firebase for friends to see
     * Also creates/updates public profile in userProfiles collection
     */
    public static void syncUserRatingToFirebase(double rating) {
        Firestore db = FirebaseManager.getFirestore();
        if (db == null || !AuthService.isAuthenticated()) {
            return;
        }
        
        try {
            String uid = AuthService.getCurrentUserId();
            String email = AuthService.getCurrentUserEmail();
            if (email == null) return;
            
            email = email.toLowerCase();
            
            // Update private user document
            Map<String, Object> userData = new HashMap<>();
            userData.put("rating", rating);
            userData.put("email", email);
            userData.put("lastUpdated", System.currentTimeMillis());
            
            db.collection("users").document(uid).set(userData, SetOptions.merge()).get();
            
            // Also update public profile (accessible by friends)
            String emailKey = email.replace(".", "_").replace("@", "_at_");
            Map<String, Object> profileData = new HashMap<>();
            profileData.put("uid", uid);
            profileData.put("email", email);
            profileData.put("rating", rating);
            profileData.put("lastUpdated", System.currentTimeMillis());
            
            db.collection("userProfiles").document(emailKey).set(profileData, SetOptions.merge()).get();
            
            System.out.println("User rating synced to Firebase: " + rating);
        } catch (Exception e) {
            System.err.println("Error syncing rating to Firebase: " + e.getMessage());
        }
    }
}
