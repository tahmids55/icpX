package com.icpx.view;

import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import java.io.IOException;

/**
 * First-time setup view for user registration
 */
public class SetupView {
    
    public static Scene createScene() {
        try {
            FXMLLoader loader = new FXMLLoader(SetupView.class.getResource("/com/icpx/view/SetupView.fxml"));
            VBox root = loader.load();
            
            Scene scene = new Scene(root);
            // scene.getStylesheets().add(SetupView.class.getResource("/styles.css").toExternalForm());
            
            return scene;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
