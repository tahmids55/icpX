package com.icpx.controller;

import com.icpx.database.SettingsDAO;
import com.icpx.database.TargetDAO;
import com.icpx.database.UserDAO;
import com.icpx.model.Target;
import com.icpx.model.User;
import com.icpx.service.AuthService;
import com.icpx.service.FirebaseManager;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.WriteBatch;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SettingsController {

    @FXML
    private TextField cfHandleField;

    @FXML
    private CheckBox startupPasswordCheckbox;
    
    @FXML
    private Text syncStatusText;
    
    @FXML
    private VBox loginBox;
    @FXML
    private VBox loggedInBox;
    @FXML
    private TextField emailField;
    @FXML
    private TextField passwordField;
    @FXML
    private Text userEmailText;
    @FXML
    private Button loginBtn;

    @FXML
    private Button googleLoginBtn;

    @FXML
    private Button logoutBtn;
    @FXML
    private Button fullSyncBtn;
    
    @FXML
    private CheckBox contestRemindersCheckbox;

    @FXML
    public void initialize() {
        // Load saved handle
        String savedHandle = SettingsDAO.getCodeforcesHandle();
        if (savedHandle != null && !savedHandle.isEmpty()) {
            cfHandleField.setText(savedHandle);
        }

        // Load startup password preference
        try {
            User user = UserDAO.getCurrentUser();
            if (user != null) {
                startupPasswordCheckbox.setSelected(user.isStartupPasswordEnabled());
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        // Load contest reminder preference
        contestRemindersCheckbox.setSelected(SettingsDAO.isContestReminderEnabled());
        
        updateLoginState();
    }
    
    private void updateLoginState() {
        boolean authenticated = AuthService.isAuthenticated();
        loginBox.setVisible(!authenticated);
        loginBox.setManaged(!authenticated);
        loggedInBox.setVisible(authenticated);
        loggedInBox.setManaged(authenticated);
        fullSyncBtn.setDisable(!authenticated);
        
        if (authenticated) {
            String lastSync = SettingsDAO.getLastSyncTime();
            userEmailText.setText("Logged in as: " + AuthService.getCurrentUserEmail());
            syncStatusText.setText("Status: Ready to sync" + (lastSync != null ? " (Last: " + lastSync + ")" : ""));
        } else {
            syncStatusText.setText("Status: Sign In Required");
        }
    }
    
    @FXML
    private void login() {
        String email = emailField.getText().trim();
        String password = passwordField.getText().trim();

        if (email.isEmpty() || password.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Login Error", "Please enter email and password");
            return;
        }

        new Thread(() -> {
            boolean success = AuthService.signIn(email, password);
            Platform.runLater(() -> {
                if (success) {
                    updateLoginState();
                    showAlert(Alert.AlertType.INFORMATION, "Login Successful", "Welcome back!");
                } else {
                    showAlert(Alert.AlertType.ERROR, "Login Fallied", "Invalid email or password");
                }
            });
        }).start();
    }

    @FXML
    private void loginWithGoogle() {
        new Thread(() -> {
            boolean success = AuthService.signInWithGoogle();
            Platform.runLater(() -> {
                if (success) {
                    updateLoginState();
                    showAlert(Alert.AlertType.INFORMATION, "Login Successful", "Welcome back!");
                } else {
                    showAlert(Alert.AlertType.ERROR, "Login Failed", "Google authentication failed or was cancelled.");
                }
            });
        }).start();
    }
    
    @FXML
    private void logout() {
        AuthService.signOut();
        updateLoginState();
    }

    @FXML
    private void toggleContestReminders() {
        boolean enabled = contestRemindersCheckbox.isSelected();
        SettingsDAO.setContestReminderEnabled(enabled);
        showAlert(Alert.AlertType.INFORMATION, "Settings Updated", "Contest reminders have been " + (enabled ? "enabled" : "disabled"));
    }

    @FXML
    private void saveHandle() {
        String handle = cfHandleField.getText().trim();
        if (!handle.isEmpty()) {
            if (SettingsDAO.setCodeforcesHandle(handle)) {
                showAlert(Alert.AlertType.INFORMATION, "Success", "Codeforces handle saved successfully!");
            } else {
                showAlert(Alert.AlertType.ERROR, "Error", "Failed to save Codeforces handle");
            }
        }
    }

    @FXML
    private void toggleStartupPassword() {
        try {
            boolean enabled = startupPasswordCheckbox.isSelected();
            UserDAO.toggleStartupPassword(enabled);
            
            showAlert(Alert.AlertType.INFORMATION, "Settings Updated", "Startup password has been " + (enabled ? "enabled" : "disabled"));
        } catch (Exception ex) {
            ex.printStackTrace();
            startupPasswordCheckbox.setSelected(!startupPasswordCheckbox.isSelected());
        }
    }
    
    @FXML
    private void fullSync() {
        if (!FirebaseManager.isInitialized()) FirebaseManager.initialize();
        if (!FirebaseManager.isInitialized()) {
             showAlert(Alert.AlertType.ERROR, "Error", "Firebase not initialized.");
             return;
        }
        if (!AuthService.isAuthenticated()) return;
        
        syncStatusText.setText("Status: Full sync in progress...");
        fullSyncBtn.setDisable(true);
        
        // First upload local changes to cloud
        new Thread(() -> {
            try {
                uploadToCloudSync();
                
                // Then download from cloud to get any changes from other devices
                Platform.runLater(() -> syncStatusText.setText("Status: Downloading from cloud..."));
                downloadFromCloudSync();
                
                Platform.runLater(() -> {
                    fullSyncBtn.setDisable(false);
                    syncStatusText.setText("Status: Full sync complete! (Last: " + SettingsDAO.getLastSyncTime() + ")");
                    showAlert(Alert.AlertType.INFORMATION, "Success", "Full sync complete!");
                });
                
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    fullSyncBtn.setDisable(false);
                    syncStatusText.setText("Status: Sync Failed");
                    showAlert(Alert.AlertType.ERROR, "Error", "Sync failed: " + e.getMessage());
                });
            }
        }).start();
    }
    
    private void uploadToCloudSync() throws Exception {
        String uid = AuthService.getCurrentUserId();
        Firestore db = FirebaseManager.getFirestore();
        List<Target> targets = TargetDAO.getAllTargetsForSync();
        
        // First, get all existing Firebase documents to find what needs to be deleted
        List<QueryDocumentSnapshot> firebaseDocs = db.collection("users")
            .document(uid)
            .collection("targets")
            .get()
            .get()
            .getDocuments();
        
        // Build set of document IDs that should exist (from local data)
        java.util.Set<String> localDocIds = new java.util.HashSet<>();
                for (Target t : targets) {
                    String docId = (t.getProblemLink() != null && !t.getProblemLink().trim().isEmpty())
                        ? generateDocIdFromLink(t.getProblemLink())
                        : String.valueOf(t.getId());
                    localDocIds.add(docId);
                }
                
                // [DISABLED] Delete Firebase documents that don't exist locally
                // This causes data loss in bi-directional sync (deletes items created on other devices)
                // Use archived/deleted flags for soft deletes instead.
                /*
                WriteBatch deleteBatch = db.batch();
                int deleteCount = 0;
                int deletedTotal = 0;
                for (QueryDocumentSnapshot doc : firebaseDocs) {
                    if (!localDocIds.contains(doc.getId())) {
                        deleteBatch.delete(doc.getReference());
                        deleteCount++;
                        deletedTotal++;
                        if (deleteCount >= 400) {
                            deleteBatch.commit().get();
                            deleteBatch = db.batch();
                            deleteCount = 0;
                        }
                    }
                }
                if (deleteCount > 0) {
                    deleteBatch.commit().get();
                }
                final int deleted = deletedTotal;
                */
                final int deleted = 0;
                
                // Now upload all local targets
                WriteBatch batch = db.batch();
                int count = 0;
                
                for(Target t : targets) {
                    Map<String, Object> data = new HashMap<>();
                    data.put("id", String.valueOf(t.getId())); // Store as String to match Android
                    data.put("type", t.getType());
                    data.put("name", t.getName());
                    data.put("problemLink", t.getProblemLink()); // camelCase to match Android
                    data.put("topicName", t.getTopicName());
                    data.put("websiteUrl", t.getWebsiteUrl());
                    data.put("status", t.getStatus());
                    data.put("rating", t.getRating() != null ? t.getRating() : 0);
                    data.put("archived", t.isArchived());
                    if (t.getCreatedAt() != null) {
                        data.put("createdAt", t.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli());
                    }
                    
                    // Use problem_link hash as document ID to prevent duplicates across devices
                    // If no problem_link, fallback to numeric ID
                    String docId = (t.getProblemLink() != null && !t.getProblemLink().trim().isEmpty()) 
                        ? generateDocIdFromLink(t.getProblemLink()) 
                        : String.valueOf(t.getId());
                    
                    // Path: users/{uid}/targets/{docId}
                    batch.set(db.collection("users").document(uid).collection("targets").document(docId), data);
                    count++;
                    
                    if (count >= 400) {
                        batch.commit().get();
                        batch = db.batch();
                        count = 0;
                    }
                }
                if (count > 0) batch.commit().get();
                
                // Sync history to subcollection (independent of targets)
                syncHistoryToFirebase(uid, db);
                
                // Sync all time stats
                int allTimeSolve = TargetDAO.getTargetCountByStatus("achieved");
                int allTimeHistory = TargetDAO.getAchievedProblems().size();
                
                Map<String, Object> statsData = new java.util.HashMap<>();
                statsData.put("all_time_solve", allTimeSolve);
                statsData.put("all_time_history", allTimeHistory);
                statsData.put("lastUpdated", System.currentTimeMillis());
                
                db.collection("users").document(uid).set(statsData, com.google.cloud.firestore.SetOptions.merge()).get();
                
                // Sync daily activity (heatmap data)
                syncDailyActivityToFirebase(uid, db);
                
                SettingsDAO.updateLastSyncTime();
    }
    
    private void downloadFromCloudSync() throws Exception {
        String uid = AuthService.getCurrentUserId();
        Firestore db = FirebaseManager.getFirestore();
        
        // Path: users/{uid}/targets
        List<QueryDocumentSnapshot> documents = db.collection("users").document(uid).collection("targets").get().get().getDocuments();
        
        System.out.println("Sync Download: Found " + documents.size() + " documents in cloud.");

        int imported = 0;
        int updated = 0;
        for (QueryDocumentSnapshot doc : documents) {
            
            String problemLink = doc.getString("problemLink");
            // String cloudIdStr = doc.getString("id"); // Not used for matching
            
            Target t = new Target();
            t.setType(doc.getString("type"));
            t.setName(doc.getString("name"));
            t.setProblemLink(problemLink);
            t.setTopicName(doc.getString("topicName"));
            t.setWebsiteUrl(doc.getString("websiteUrl"));
            t.setStatus(doc.getString("status"));
            Long rating = doc.getLong("rating");
            if (rating != null) t.setRating(rating.intValue());
            Boolean archived = doc.getBoolean("archived");
            if (archived != null) t.setArchived(archived);
            
            Long createdAt = doc.getLong("createdAt");
            if (createdAt != null) {
                t.setCreatedAt(java.time.Instant.ofEpochMilli(createdAt)
                        .atZone(java.time.ZoneId.systemDefault())
                        .toLocalDateTime());
            }

            // First check by problem link (most reliable for duplicates)
            Target existingByLink = null;
            if (problemLink != null && !problemLink.trim().isEmpty()) {
                existingByLink = TargetDAO.getTargetByProblemLink(problemLink);
            }
            
            if (existingByLink != null) {
                // Update existing target found by link
                t.setId(existingByLink.getId()); // Keep local ID
                
                boolean success = TargetDAO.updateTarget(t);
                if (success) {
                    updated++;
                    System.out.println("Sync: Updated existing target: " + t.getName());
                } else {
                    System.err.println("Sync: Failed to update target: " + t.getName());
                }
                continue;
            }
            
            // New target - insert with duplicate check (insertTarget has built-in duplicate check)
            boolean inserted = TargetDAO.insertTarget(t);
            if (inserted) {
                imported++;
                System.out.println("Sync: Imported new target: " + t.getName());
            } else {
                System.out.println("Sync: Failed to insert new target (likely duplicate): " + t.getName());
            }
        }
        
        System.out.println("Download sync complete: " + imported + " imported, " + updated + " updated");
        SettingsDAO.updateLastSyncTime();
    }
    
    /**
     * Sync solved problems history to Firebase subcollection (independent of targets)
     * Path: users/{uid}/history/{problemId}
     * Matches Android implementation - also deletes history items removed locally
     */
    private void syncHistoryToFirebase(String uid, Firestore db) throws Exception {
        List<Target> achievedTargets = TargetDAO.getAchievedProblems();
        
        // First, get all existing Firebase history documents to find what needs to be deleted
        List<QueryDocumentSnapshot> firebaseHistoryDocs = db.collection("users")
            .document(uid)
            .collection("history")
            .get()
            .get()
            .getDocuments();
        
        // Build set of document IDs that should exist (from local achieved targets)
        java.util.Set<String> localHistoryDocIds = new java.util.HashSet<>();
        for (Target t : achievedTargets) {
            String docId = (t.getProblemLink() != null && !t.getProblemLink().trim().isEmpty())
                ? generateDocIdFromLink(t.getProblemLink())
                : String.valueOf(t.getId());
            localHistoryDocIds.add(docId);
        }
        
        // [DISABLED] Delete Firebase history documents that don't exist locally (were deleted)
        // Prevent data loss during sync
        /*
        WriteBatch deleteBatch = db.batch();
        int deleteCount = 0;
        for (QueryDocumentSnapshot doc : firebaseHistoryDocs) {
            if (!localHistoryDocIds.contains(doc.getId())) {
                deleteBatch.delete(doc.getReference());
                deleteCount++;
                if (deleteCount >= 400) {
                    deleteBatch.commit().get();
                    deleteBatch = db.batch();
                    deleteCount = 0;
                }
            }
        }
        if (deleteCount > 0) {
            deleteBatch.commit().get();
        }
        */
        
        if (achievedTargets.isEmpty()) {
            return;
        }
        
        // Now upload all local history entries
        WriteBatch batch = db.batch();
        int count = 0;
        
        for (Target t : achievedTargets) {
            Map<String, Object> entry = new HashMap<>();
            entry.put("id", t.getId());
            entry.put("problem_link", t.getProblemLink());
            entry.put("name", t.getName());
            entry.put("rating", t.getRating() != null ? t.getRating() : 0);
            
            // Use problem_link hash as document ID for consistency across devices
            String docId = (t.getProblemLink() != null && !t.getProblemLink().trim().isEmpty())
                ? generateDocIdFromLink(t.getProblemLink())
                : String.valueOf(t.getId());
            
            // Store in subcollection: users/{uid}/history/{docId}
            batch.set(
                db.collection("users")
                    .document(uid)
                    .collection("history")
                    .document(docId),
                entry,
                com.google.cloud.firestore.SetOptions.merge()
            );
            
            count++;
            
            // Commit in batches of 400 (Firestore limit is 500)
            if (count >= 400) {
                batch.commit().get();
                batch = db.batch();
                count = 0;
            }
        }
        
        if (count > 0) {
            batch.commit().get();
        }
    }
    
    /**
     * Generate a consistent document ID from problem link
     * Uses hash to create a valid Firestore document ID that's consistent across devices
     */
    private String generateDocIdFromLink(String problemLink) {
        // Use SHA-256 hash truncated to 20 chars for Firestore doc ID
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
     * Sync daily activity (heatmap data) to Firebase
     * Path: users/{uid}/daily_activity/{date}
     * Matches Android implementation
     */
    private void syncDailyActivityToFirebase(String uid, Firestore db) throws Exception {
        // Get activity data from local database based on solved problems
        List<Target> achievedTargets = TargetDAO.getAchievedProblems();
        
        if (achievedTargets.isEmpty()) {
            return;
        }
        
        // Group solved problems by date
        Map<String, Integer> dailyProblemCounts = new HashMap<>();
        Map<String, Integer> dailyTopicCounts = new HashMap<>();
        
        for (Target t : achievedTargets) {
            if (t.getCreatedAt() != null) {
                String date = t.getCreatedAt().toLocalDate().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                dailyProblemCounts.merge(date, 1, Integer::sum);
                
                // Count unique topics per day
                if (t.getTopicName() != null && !t.getTopicName().trim().isEmpty()) {
                    dailyTopicCounts.merge(date, 1, Integer::sum);
                }
            }
        }
        
        if (dailyProblemCounts.isEmpty()) {
            return;
        }
        
        // Use batch write for efficiency (same as Android)
        WriteBatch batch = db.batch();
        int count = 0;
        
        for (Map.Entry<String, Integer> entry : dailyProblemCounts.entrySet()) {
            String date = entry.getKey();
            int problemCount = entry.getValue();
            int topicCount = dailyTopicCounts.getOrDefault(date, 0);
            
            Map<String, Object> activity = new HashMap<>();
            activity.put("problemCount", problemCount);
            activity.put("topicCount", topicCount);
            activity.put("timestamp", System.currentTimeMillis());
            
            // Path: users/{uid}/daily_activity/{date}
            batch.set(
                db.collection("users")
                    .document(uid)
                    .collection("daily_activity")
                    .document(date),
                activity,
                com.google.cloud.firestore.SetOptions.merge()
            );
            
            count++;
            
            if (count >= 400) {
                batch.commit().get();
                batch = db.batch();
                count = 0;
            }
        }
        
        if (count > 0) {
            batch.commit().get();
        }
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}