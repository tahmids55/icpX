package com.icpx.view;

import com.icpx.database.UserDAO;
import com.icpx.util.SceneManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;

/**
 * First-time setup view for user registration
 */
public class SetupView {
    
    public static Scene createScene() {
        VBox root = new VBox(20);
        root.getStyleClass().addAll("container", SceneManager.getCurrentTheme() + "-theme");
        root.setAlignment(Pos.CENTER);
        root.setPrefSize(500, 600);

        // Card container
        VBox card = new VBox(20);
        card.getStyleClass().add("card");
        card.setMaxWidth(400);
        card.setAlignment(Pos.CENTER);

        // Title
        Text title = new Text("Welcome to icpX");
        title.getStyleClass().add("title");

        Text subtitle = new Text("Let's set up your account");
        subtitle.getStyleClass().add("subtitle");

        // Username field
        Label usernameLabel = new Label("Username");
        usernameLabel.getStyleClass().add("label");
        TextField usernameField = new TextField();
        usernameField.setPromptText("Enter your username");
        usernameField.getStyleClass().add("text-field");
        usernameField.setPrefWidth(340);

        // Password field
        Label passwordLabel = new Label("Password");
        passwordLabel.getStyleClass().add("label");
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Enter your password");
        passwordField.getStyleClass().add("password-field");
        passwordField.setPrefWidth(340);

        // Confirm password field
        Label confirmPasswordLabel = new Label("Confirm Password");
        confirmPasswordLabel.getStyleClass().add("label");
        PasswordField confirmPasswordField = new PasswordField();
        confirmPasswordField.setPromptText("Confirm your password");
        confirmPasswordField.getStyleClass().add("password-field");
        confirmPasswordField.setPrefWidth(340);

        // Error label
        Label errorLabel = new Label();
        errorLabel.getStyleClass().add("error-label");
        errorLabel.setVisible(false);

        // Create account button
        Button createButton = new Button("Create Account");
        createButton.getStyleClass().add("button");
        createButton.setPrefWidth(340);
        createButton.setOnAction(e -> {
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
        });

        // Add all elements to card
        VBox usernameBox = new VBox(8, usernameLabel, usernameField);
        VBox passwordBox = new VBox(8, passwordLabel, passwordField);
        VBox confirmPasswordBox = new VBox(8, confirmPasswordLabel, confirmPasswordField);

        card.getChildren().addAll(
            title,
            subtitle,
            new VBox(5),
            usernameBox,
            passwordBox,
            confirmPasswordBox,
            errorLabel,
            new VBox(5),
            createButton
        );

        root.getChildren().add(card);

        Scene scene = new Scene(root);
        scene.getStylesheets().add(SetupView.class.getResource("/styles.css").toExternalForm());
        
        return scene;
    }
}
