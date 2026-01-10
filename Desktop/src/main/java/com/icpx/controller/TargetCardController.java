package com.icpx.controller;

import com.icpx.model.Target;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Controller for individual target card items.
 */
public class TargetCardController {

    @FXML
    private Text nameText;

    @FXML
    private VBox nameSection;

    @FXML
    private Label statusLabel;

    @FXML
    private Label ratingLabel;
    
    @FXML
    private Label deadlineLabel;

    @FXML
    private Button pdfBtn;

    @FXML
    private Button linkBtn;

    @FXML
    private Button checkBtn;

    @FXML
    private Button deleteBtn;

    private Runnable onLinkAction;
    private Runnable onPdfAction;
    private Runnable onCheckAction;
    private Runnable onDeleteAction;

    public void initialize(Target target, Runnable onLink, Runnable onPdf, Runnable onCheck, Runnable onDelete) {
        this.onLinkAction = onLink;
        this.onPdfAction = onPdf;
        this.onCheckAction = onCheck;
        this.onDeleteAction = onDelete;

        // Set icon and name
        String icon = "problem".equals(target.getType()) ? "📝" : "📚";
        nameText.setText(icon + " " + target.getName());

        // Add topic text if it's a topic type
        if ("topic".equals(target.getType()) && target.getTopicName() != null) {
            Text topicText = new Text("Topic: " + target.getTopicName());
            topicText.getStyleClass().add("subtitle");
            nameSection.getChildren().add(topicText);
        }

        // Set status label color
        statusLabel.setText(target.getStatus().toUpperCase());
        switch (target.getStatus().toLowerCase()) {
            case "achieved":
                statusLabel.setStyle(statusLabel.getStyle() + "-fx-background-color: #4CAF50; -fx-text-fill: white;");
                break;
            case "failed":
                statusLabel.setStyle(statusLabel.getStyle() + "-fx-background-color: #F44336; -fx-text-fill: white;");
                break;
            default:
                statusLabel.setStyle(statusLabel.getStyle() + "-fx-background-color: #FFA726; -fx-text-fill: white;");
        }

        // Show/hide buttons based on target type
        boolean isProblem = "problem".equals(target.getType());
        linkBtn.setVisible(isProblem);
        linkBtn.setManaged(isProblem);
        pdfBtn.setVisible(isProblem);
        pdfBtn.setManaged(isProblem);
        checkBtn.setVisible(isProblem);
        checkBtn.setManaged(isProblem);

        // Disable check button if already achieved
        if (isProblem && "achieved".equals(target.getStatus())) {
            checkBtn.setDisable(true);
        }

        // Set rating label and color
        if (target.getRating() != null && target.getRating() > 0) {
            ratingLabel.setText("★ " + target.getRating());
            int rating = target.getRating();
            String color;
            if (rating < 1300) {
                color = "#43a047"; // Green - Easy
            } else if (rating < 1800) {
                color = "#fb8c00"; // Orange - Medium
            } else {
                color = "#e53935"; // Red - Hard
            }
            ratingLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-padding: 2 8 2 8; -fx-background-radius: 4px; -fx-background-color: " + color + "; -fx-text-fill: white;");
            ratingLabel.setVisible(true);
            ratingLabel.setManaged(true);
        } else {
            ratingLabel.setVisible(false);
            ratingLabel.setManaged(false);
        }
        
        // Set deadline countdown
        if (target.getDeadline() != null && !"achieved".equals(target.getStatus())) {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime deadline = target.getDeadline();
            
            if (now.isBefore(deadline)) {
                // Time remaining
                Duration remaining = Duration.between(now, deadline);
                long hours = remaining.toHours();
                long minutes = remaining.toMinutes() % 60;
                
                String timeText;
                String bgColor;
                if (hours >= 12) {
                    timeText = "⏰ " + hours + "h left";
                    bgColor = "#4CAF50"; // Green
                } else if (hours >= 1) {
                    timeText = "⏰ " + hours + "h " + minutes + "m left";
                    bgColor = "#FF9800"; // Orange
                } else {
                    timeText = "⏰ " + minutes + "m left";
                    bgColor = "#F44336"; // Red
                }
                
                deadlineLabel.setText(timeText);
                deadlineLabel.setStyle("-fx-font-size: 11px; -fx-padding: 2 8 2 8; -fx-background-radius: 4px; -fx-background-color: " + bgColor + "; -fx-text-fill: white;");
                deadlineLabel.setVisible(true);
                deadlineLabel.setManaged(true);
            } else {
                // Overdue
                Duration overdue = Duration.between(deadline, now);
                long hours = overdue.toHours();
                long minutes = overdue.toMinutes() % 60;
                
                String timeText;
                if (hours >= 1) {
                    timeText = "⚠ " + hours + "h " + minutes + "m late (-" + String.format("%.2f", (hours * 60 + minutes) * 0.01) + ")";
                } else {
                    timeText = "⚠ " + minutes + "m late (-" + String.format("%.2f", minutes * 0.01) + ")";
                }
                
                deadlineLabel.setText(timeText);
                deadlineLabel.setStyle("-fx-font-size: 11px; -fx-padding: 2 8 2 8; -fx-background-radius: 4px; -fx-background-color: #B71C1C; -fx-text-fill: white;");
                deadlineLabel.setVisible(true);
                deadlineLabel.setManaged(true);
            }
        } else {
            deadlineLabel.setVisible(false);
            deadlineLabel.setManaged(false);
        }
    }

    @FXML
    private void handleLink() {
        if (onLinkAction != null) {
            onLinkAction.run();
        }
    }

    @FXML
    private void handlePdf() {
        if (onPdfAction != null) {
            onPdfAction.run();
        }
    }

    @FXML
    private void handleCheck() {
        if (onCheckAction != null) {
            onCheckAction.run();
        }
    }

    @FXML
    private void handleDelete() {
        if (onDeleteAction != null) {
            onDeleteAction.run();
        }
    }
}
