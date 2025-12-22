package com.icpx.controller;

import com.icpx.database.UserDAO;
import com.icpx.util.SceneManager;
import com.icpx.view.DashboardView;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

public class SetupController {

    @FXML
    private VBox root;

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private PasswordField confirmPasswordField;

    @FXML
    private Label errorLabel;

    @FXML
    public void initialize() {
        // Theme initialization removed
    }

    @FXML
    private void handleCreateAccount() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();

        // Validation
        if (username.isEmpty() || password.isEmpty()) {
            errorLabel.setText("Username and password are required");
            errorLabel.setVisible(true);
            return;
        }

        if (password.length() < 4) {
            errorLabel.setText("Password must be at least 4 characters");
            errorLabel.setVisible(true);
            return;
        }

        if (!password.equals(confirmPassword)) {
            errorLabel.setText("Passwords do not match");
            errorLabel.setVisible(true);
            return;
        }

        // Create user
        try {
            boolean success = UserDAO.createUser(username, password);
            if (success) {
                SceneManager.switchScene(DashboardView.createScene());
            } else {
                errorLabel.setText("Failed to create account");
                errorLabel.setVisible(true);
            }
        } catch (Exception ex) {
            errorLabel.setText("Error: " + ex.getMessage());
            errorLabel.setVisible(true);
            ex.printStackTrace();
        }
    }
}