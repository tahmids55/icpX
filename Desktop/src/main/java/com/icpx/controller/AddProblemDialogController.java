package com.icpx.controller;

import com.icpx.model.Target;
import com.icpx.service.CodeforcesService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

public class AddProblemDialogController {

    @FXML
    private TextField nameField;

    @FXML
    private TextField linkField;

    @FXML
    private TextField ratingField;

    @FXML
    private Label statusLabel;

    private CodeforcesService codeforcesService;
    private String problemName;
    private Integer problemRating;

    @FXML
    public void initialize() {
        codeforcesService = new CodeforcesService();
        
        // Focus on link field when dialog opens
        Platform.runLater(() -> linkField.requestFocus());
        
        // Add listener to link field to auto-fetch problem details
        linkField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null && !newValue.trim().isEmpty()) {
                fetchProblemDetails(newValue.trim());
            } else {
                clearProblemDetails();
            }
        });
    }

    private void fetchProblemDetails(String url) {
        statusLabel.setText("Fetching problem details...");
        statusLabel.setStyle("-fx-text-fill: #3b82f6;");
        
        // Fetch in background thread to avoid blocking UI
        new Thread(() -> {
            try {
                CodeforcesService.ProblemDetails details = codeforcesService.fetchProblemDetails(url);
                
                Platform.runLater(() -> {
                    problemName = details.name;
                    problemRating = details.rating;
                    
                    nameField.setText(problemName);
                    ratingField.setText(problemRating != null ? String.valueOf(problemRating) : "N/A");
                    statusLabel.setText("✓ Problem details loaded successfully");
                    statusLabel.setStyle("-fx-text-fill: #10b981;");
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    clearProblemDetails();
                    statusLabel.setText("⚠ Failed to fetch problem details: " + e.getMessage());
                    statusLabel.setStyle("-fx-text-fill: #ef4444;");
                });
            }
        }).start();
    }

    private void clearProblemDetails() {
        problemName = null;
        problemRating = null;
        nameField.setText("");
        ratingField.setText("");
        statusLabel.setText("");
    }

    public Target getResult() {
        String link = linkField.getText().trim();
        
        if (!link.isEmpty() && problemName != null) {
            Target target = new Target("problem", problemName);
            target.setProblemLink(link);
            target.setRating(problemRating);
            return target;
        }
        return null;
    }

    public TextField getNameField() {
        return nameField;
    }

    public TextField getLinkField() {
        return linkField;
    }
}
