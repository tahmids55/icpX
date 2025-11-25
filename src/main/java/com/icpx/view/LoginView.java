package com.icpx.view;

import com.icpx.database.UserDAO;
import com.icpx.util.SceneManager;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;

/**
 * Login view for startup password verification
 */
public class LoginView {
    
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
        Text title = new Text("icpX");
        title.getStyleClass().add("title");

        Text subtitle = new Text("Enter your password to continue");
        subtitle.getStyleClass().add("subtitle");

        // Password field
        Label passwordLabel = new Label("Password");
        passwordLabel.getStyleClass().add("label");
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Enter your password");
        passwordField.getStyleClass().add("password-field");
        passwordField.setPrefWidth(340);

        // Error label
        Label errorLabel = new Label();
        errorLabel.getStyleClass().add("error-label");
        errorLabel.setVisible(false);

        // Login button
        Button loginButton = new Button("Login");
        loginButton.getStyleClass().add("button");
        loginButton.setPrefWidth(340);
        loginButton.setOnAction(e -> {
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
        });

        // Handle Enter key
        passwordField.setOnAction(e -> loginButton.fire());

        // Add all elements to card
        VBox passwordBox = new VBox(8, passwordLabel, passwordField);

        card.getChildren().addAll(
            title,
            subtitle,
            new VBox(10),
            passwordBox,
            errorLabel,
            new VBox(5),
            loginButton
        );

        root.getChildren().add(card);

        Scene scene = new Scene(root);
        scene.getStylesheets().add(LoginView.class.getResource("/styles.css").toExternalForm());
        
        return scene;
    }
}
