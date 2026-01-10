package com.icpx.controller;

import com.icpx.database.FriendsDAO;
import com.icpx.service.AuthService;
import com.icpx.service.FriendsService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;

import java.util.List;
import java.util.Map;

/**
 * Controller for Friends view
 * Allows adding/removing friends and viewing their stats
 */
public class FriendsController {

    @FXML
    private TextField friendEmailField;
    
    @FXML
    private Text statusText;
    
    @FXML
    private VBox friendsContainer;
    
    @FXML
    private Button addFriendBtn;
    
    @FXML
    private Button refreshBtn;
    
    private FriendsService friendsService;

    @FXML
    public void initialize() {
        friendsService = new FriendsService();
        
        if (!AuthService.isAuthenticated()) {
            statusText.setText("Please sign in to use Friends feature");
            addFriendBtn.setDisable(true);
        }
        
        loadFriends();
    }

    @FXML
    private void addFriend() {
        String email = friendEmailField.getText().trim();
        
        if (email.isEmpty()) {
            statusText.setText("Please enter an email address");
            return;
        }
        
        if (!email.contains("@")) {
            statusText.setText("Please enter a valid email address");
            return;
        }
        
        String currentEmail = AuthService.getCurrentUserEmail();
        if (currentEmail == null) {
            statusText.setText("Please sign in first");
            return;
        }
        
        if (email.equalsIgnoreCase(currentEmail)) {
            statusText.setText("You cannot add yourself as a friend");
            return;
        }
        
        statusText.setText("Adding friend...");
        addFriendBtn.setDisable(true);
        
        new Thread(() -> {
            boolean success = friendsService.addFriend(currentEmail, email);
            
            Platform.runLater(() -> {
                addFriendBtn.setDisable(false);
                if (success) {
                    statusText.setText("Friend added: " + email);
                    friendEmailField.clear();
                    loadFriends();
                } else {
                    statusText.setText("Failed to add friend. They may already be added.");
                }
            });
        }).start();
    }

    @FXML
    private void refreshFriends() {
        String currentEmail = AuthService.getCurrentUserEmail();
        if (currentEmail == null) {
            return;
        }
        
        statusText.setText("Syncing friends...");
        refreshBtn.setDisable(true);
        
        new Thread(() -> {
            friendsService.syncFriendsFromFirebase(currentEmail);
            
            Platform.runLater(() -> {
                refreshBtn.setDisable(false);
                statusText.setText("Friends synced!");
                loadFriends();
            });
        }).start();
    }

    private void loadFriends() {
        friendsContainer.getChildren().clear();
        
        String currentEmail = AuthService.getCurrentUserEmail();
        if (currentEmail == null) {
            Text loginText = new Text("Sign in to view friends");
            loginText.setStyle("-fx-fill: #888;");
            friendsContainer.getChildren().add(loginText);
            return;
        }
        
        List<String> friends = FriendsDAO.getFriends(currentEmail);
        
        if (friends.isEmpty()) {
            Text emptyText = new Text("No friends added yet. Add a friend by their email address above!");
            emptyText.setStyle("-fx-fill: #888;");
            friendsContainer.getChildren().add(emptyText);
            return;
        }
        
        for (String friendEmail : friends) {
            friendsContainer.getChildren().add(createFriendCard(friendEmail));
        }
    }

    private VBox createFriendCard(String friendEmail) {
        VBox card = new VBox(10);
        card.setStyle("-fx-background-color: #252540; -fx-background-radius: 10; -fx-padding: 15;");
        
        // Header with email and remove button
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        
        Text emailText = new Text("📧 " + friendEmail);
        emailText.setStyle("-fx-fill: white; -fx-font-size: 14px;");
        HBox.setHgrow(emailText, Priority.ALWAYS);
        
        Button viewBtn = new Button("View Stats");
        viewBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;");
        viewBtn.setOnAction(e -> viewFriendStats(friendEmail));
        
        Button removeBtn = new Button("Remove");
        removeBtn.setStyle("-fx-background-color: #F44336; -fx-text-fill: white;");
        removeBtn.setOnAction(e -> removeFriend(friendEmail));
        
        header.getChildren().addAll(emailText, viewBtn, removeBtn);
        
        // Stats container (initially empty, populated on "View Stats")
        VBox statsContainer = new VBox(8);
        statsContainer.setId("stats_" + friendEmail.hashCode());
        
        card.getChildren().addAll(header, statsContainer);
        return card;
    }

    private void viewFriendStats(String friendEmail) {
        String currentEmail = AuthService.getCurrentUserEmail();
        if (currentEmail == null) return;
        
        new Thread(() -> {
            Map<String, Object> data = friendsService.getFriendData(friendEmail);
            
            Platform.runLater(() -> {
                // Find the stats container
                for (javafx.scene.Node node : friendsContainer.getChildren()) {
                    if (node instanceof VBox) {
                        VBox card = (VBox) node;
                        for (javafx.scene.Node child : card.getChildren()) {
                            if (child instanceof VBox && child.getId() != null && 
                                child.getId().equals("stats_" + friendEmail.hashCode())) {
                                
                                VBox statsBox = (VBox) child;
                                statsBox.getChildren().clear();
                                
                                if (data.containsKey("error")) {
                                    Text errorText = new Text("Error: " + data.get("error"));
                                    errorText.setStyle("-fx-fill: #F44336;");
                                    statsBox.getChildren().add(errorText);
                                    return;
                                }
                                
                                // Rating display with bar chart
                                Double rating = (Double) data.get("rating");
                                if (rating != null) {
                                    VBox ratingContainer = new VBox(5);
                                    
                                    HBox ratingBox = new HBox(10);
                                    ratingBox.setAlignment(Pos.CENTER_LEFT);
                                    
                                    Text ratingLabel = new Text("🏆 Rating:");
                                    ratingLabel.setStyle("-fx-fill: #888;");
                                    
                                    Text ratingValue = new Text(String.format("%.1f / 10", rating));
                                    ratingValue.setStyle("-fx-fill: #FFD700; -fx-font-size: 18px; -fx-font-weight: bold;");
                                    
                                    ratingBox.getChildren().addAll(ratingLabel, ratingValue);
                                    ratingContainer.getChildren().add(ratingBox);
                                    
                                    // Rating bar chart (visual representation)
                                    HBox barContainer = new HBox(0);
                                    barContainer.setAlignment(Pos.CENTER_LEFT);
                                    barContainer.setMaxWidth(200);
                                    
                                    // Filled portion (rating out of 10)
                                    double barWidth = 200.0;
                                    double fillWidth = Math.min(10.0, Math.max(0.0, rating)) / 10.0 * barWidth;
                                    
                                    javafx.scene.layout.Region filledBar = new javafx.scene.layout.Region();
                                    filledBar.setPrefSize(fillWidth, 16);
                                    filledBar.setStyle("-fx-background-color: linear-gradient(to right, #4CAF50, #8BC34A); -fx-background-radius: 8 0 0 8;");
                                    
                                    javafx.scene.layout.Region emptyBar = new javafx.scene.layout.Region();
                                    emptyBar.setPrefSize(barWidth - fillWidth, 16);
                                    emptyBar.setStyle("-fx-background-color: #333; -fx-background-radius: 0 8 8 0;");
                                    
                                    barContainer.getChildren().addAll(filledBar, emptyBar);
                                    ratingContainer.getChildren().add(barContainer);
                                    
                                    statsBox.getChildren().add(ratingContainer);
                                }
                                
                                // Total solved
                                Integer totalSolved = (Integer) data.get("totalSolved");
                                if (totalSolved != null && totalSolved >= 0) {
                                    Text solvedText = new Text("✓ Total Solved: " + totalSolved);
                                    solvedText.setStyle("-fx-fill: #4CAF50;");
                                    statsBox.getChildren().add(solvedText);
                                }
                                
                                return;
                            }
                        }
                    }
                }
            });
        }).start();
    }

    private void removeFriend(String friendEmail) {
        String currentEmail = AuthService.getCurrentUserEmail();
        if (currentEmail == null) return;
        
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Remove Friend");
        confirm.setHeaderText("Remove " + friendEmail + "?");
        confirm.setContentText("You will no longer be able to view their stats.");
        
        confirm.showAndWait().ifPresent(response -> {
            if (response == javafx.scene.control.ButtonType.OK) {
                new Thread(() -> {
                    boolean success = friendsService.removeFriend(currentEmail, friendEmail);
                    
                    Platform.runLater(() -> {
                        if (success) {
                            statusText.setText("Friend removed: " + friendEmail);
                            loadFriends();
                        } else {
                            statusText.setText("Failed to remove friend");
                        }
                    });
                }).start();
            }
        });
    }
}
