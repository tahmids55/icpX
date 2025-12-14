package com.icpx.view;

import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import java.io.IOException;

/**
 * Main dashboard view with sidebar, topbar, and content area
 */
public class DashboardView {
    
    public static Scene createScene() {
        try {
            FXMLLoader loader = new FXMLLoader(DashboardView.class.getResource("/com/icpx/view/DashboardView.fxml"));
            BorderPane root = loader.load();
            
            Scene scene = new Scene(root, 1000, 700);
            scene.getStylesheets().add(DashboardView.class.getResource("/styles.css").toExternalForm());
            
            return scene;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
