package com.icpx.controller;

import com.icpx.model.Target;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;

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
    private Button linkBtn;

    @FXML
    private Button checkBtn;

    @FXML
    private Button deleteBtn;

    private Runnable onLinkAction;
    private Runnable onCheckAction;
    private Runnable onDeleteAction;

    public void initialize(Target target, Runnable onLink, Runnable onCheck, Runnable onDelete) {
        this.onLinkAction = onLink;
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
        checkBtn.setVisible(isProblem);
        checkBtn.setManaged(isProblem);

        // Disable check button if already achieved
        if (isProblem && "achieved".equals(target.getStatus())) {
            checkBtn.setDisable(true);
        }
    }

    @FXML
    private void handleLink() {
        if (onLinkAction != null) {
            onLinkAction.run();
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
