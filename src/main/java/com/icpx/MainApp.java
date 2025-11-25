package com.icpx;

import com.icpx.database.DatabaseHelper;
import com.icpx.database.UserDAO;
import com.icpx.model.User;
import com.icpx.util.SceneManager;
import com.icpx.view.DashboardView;
import com.icpx.view.LoginView;
import com.icpx.view.SetupView;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Main application entry point
 */
public class MainApp extends Application {

    @Override
    public void start(Stage primaryStage) {
        SceneManager.setPrimaryStage(primaryStage);
        primaryStage.setTitle("icpX");

        try {
            // Check if user exists
            boolean userExists = UserDAO.userExists();
            
            Scene initialScene;
            if (!userExists) {
                // First run - show setup
                initialScene = SetupView.createScene();
            } else {
                // User exists - check if startup password is enabled
                User user = UserDAO.getCurrentUser();
                if (user != null && user.isStartupPasswordEnabled()) {
                    // Show login screen
                    initialScene = LoginView.createScene();
                } else {
                    // Go directly to dashboard
                    initialScene = DashboardView.createScene();
                }
            }

            primaryStage.setScene(initialScene);
            primaryStage.setResizable(true);
            primaryStage.show();

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Failed to initialize application: " + e.getMessage());
        }
    }

    @Override
    public void stop() {
        DatabaseHelper.closeConnection();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
