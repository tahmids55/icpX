package com.icpx.controller;

import com.icpx.database.UserDAO;
import com.icpx.model.User;
import com.icpx.util.SceneManager;
import com.icpx.view.LoginView;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;

import java.io.IOException;

public class DashboardController {

    @FXML
    private BorderPane mainRoot;

    @FXML
    private ScrollPane contentScrollPane;

    @FXML
    private ToggleButton themeToggle;

    @FXML
    private StackPane logoContainer;

    @FXML
    public void initialize() {
        // Initialize theme
        mainRoot.getStyleClass().add(SceneManager.getCurrentTheme() + "-theme");
        themeToggle.setSelected(SceneManager.getCurrentTheme().equals("dark"));
        themeToggle.setText(themeToggle.isSelected() ? "☀️ Light" : "🌙 Dark");

        createLogo();
        showDashboard();
    }

    private void createLogo() {
        logoContainer.getChildren().clear();
        // Determine current theme
        boolean isDark = SceneManager.getCurrentTheme().equals("dark");
        
        // Background circle
        Circle background = new Circle(16);
        background.setFill(isDark ? Color.web("#0e639c") : Color.web("#2563eb"));
        
        // Create "X" shape using two rectangles rotated
        Rectangle bar1 = new Rectangle(18, 3);
        bar1.setFill(Color.WHITE);
        bar1.setRotate(45);
        
        Rectangle bar2 = new Rectangle(18, 3);
        bar2.setFill(Color.WHITE);
        bar2.setRotate(-45);
        
        // Create connection point (small circle at center)
        Circle centerDot = new Circle(3);
        centerDot.setFill(isDark ? Color.web("#4fc3f7") : Color.web("#60a5fa"));
        
        logoContainer.getChildren().addAll(background, bar1, bar2, centerDot);
    }

    @FXML
    private void handleThemeToggle() {
        String newTheme = themeToggle.isSelected() ? "dark" : "light";
        SceneManager.setTheme(newTheme);
        themeToggle.setText(themeToggle.isSelected() ? "☀️ Light" : "🌙 Dark");
        
        // Re-create logo
        createLogo();
        
        // Reload current content to refresh its theme-dependent elements
        // Ideally we track current view. For now, just reload dashboard.
        showDashboard();
    }

    @FXML
    private void handleLogout() {
        try {
            User user = UserDAO.getCurrentUser();
            if (user != null && user.isStartupPasswordEnabled()) {
                SceneManager.switchScene(LoginView.createScene());
            } else {
                SceneManager.getPrimaryStage().close();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @FXML
    private void showDashboard() {
        loadContent("/com/icpx/view/DashboardContent.fxml");
    }

    @FXML
    private void showTargets() {
        loadContent("/com/icpx/view/TargetView.fxml");
    }

    @FXML
    private void showSettings() {
        loadContent("/com/icpx/view/SettingsView.fxml");
    }

    private void loadContent(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent content = loader.load();
            contentScrollPane.setContent(content);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}