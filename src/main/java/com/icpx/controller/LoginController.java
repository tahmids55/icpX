package com.icpx.controller;

import com.icpx.database.UserDAO;
import com.icpx.util.SceneManager;
import com.icpx.view.DashboardView;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.layout.VBox;

public class LoginController {

    @FXML
    private VBox root;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Label errorLabel;

    @FXML
    public void initialize() {
        // Initialize theme
        root.getStyleClass().add(SceneManager.getCurrentTheme() + "-theme");
    }

    @FXML
    private void handleLogin() {
        String password = passwordField.getText();

        if (password.isEmpty()) {
            errorLabel.setText("Password is required");
            errorLabel.setVisible(true);
            return;
        }

        try {
            boolean valid = UserDAO.verifyPassword(password);
            if (valid) {
                SceneManager.switchScene(DashboardView.createScene());
            } else {
                errorLabel.setText("Incorrect password");
                errorLabel.setVisible(true);
                passwordField.clear();
            }
        } catch (Exception ex) {
            errorLabel.setText("Error: " + ex.getMessage());
            errorLabel.setVisible(true);
            ex.printStackTrace();
        }
    }
}