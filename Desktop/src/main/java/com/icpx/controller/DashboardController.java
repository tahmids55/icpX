package com.icpx.controller;

import com.icpx.database.UserDAO;
import com.icpx.model.User;
import com.icpx.util.SceneManager;
import com.icpx.view.LoginView;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.ScrollPane;
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
    private StackPane logoContainer;

    @FXML
    public void initialize() {
        createLogo();
        showDashboard();
    }

    private void createLogo() {
        logoContainer.getChildren().clear();
        
        // Background circle
        Circle background = new Circle(16);
        background.setFill(Color.web("#2563eb"));
        
        // Create "X" shape using two rectangles rotated
        Rectangle bar1 = new Rectangle(18, 3);
        bar1.setFill(Color.WHITE);
        bar1.setRotate(45);
        
        Rectangle bar2 = new Rectangle(18, 3);
        bar2.setFill(Color.WHITE);
        bar2.setRotate(-45);
        
        // Create connection point (small circle at center)
        Circle centerDot = new Circle(3);
        centerDot.setFill(Color.web("#60a5fa"));
        
        logoContainer.getChildren().addAll(background, bar1, bar2, centerDot);
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
            
            // Increase scroll speed for the loaded content
            setupScrollSpeed(content);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Setup faster scroll speed for content
     */
    private void setupScrollSpeed(Parent content) {
        content.setOnScroll(event -> {
            double deltaY = event.getDeltaY() * 3; // Multiply scroll speed by 3
            double contentHeight = content.getBoundsInLocal().getHeight();
            double viewportHeight = contentScrollPane.getViewportBounds().getHeight();
            
            if (contentHeight > viewportHeight) {
                double scrollAmount = deltaY / (contentHeight - viewportHeight);
                double newValue = contentScrollPane.getVvalue() - scrollAmount;
                // Clamp between 0 and 1
                newValue = Math.max(0, Math.min(1, newValue));
                contentScrollPane.setVvalue(newValue);
            }
            event.consume();
        });
    }

    @FXML
    private void showContests() {
        loadContent("/com/icpx/view/ContestsView.fxml");
    }

    @FXML
    private void showHistory() {
        loadContent("/com/icpx/view/HistoryView.fxml");
    }
    
    @FXML
    private void showFriends() {
        loadContent("/com/icpx/view/FriendsView.fxml");
    }
}
