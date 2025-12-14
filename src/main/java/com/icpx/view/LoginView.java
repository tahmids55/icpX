package com.icpx.view;

import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import java.io.IOException;

/**
 * Login view for startup password verification
 */
public class LoginView {
    
    public static Scene createScene() {
        try {
            FXMLLoader loader = new FXMLLoader(LoginView.class.getResource("/com/icpx/view/LoginView.fxml"));
            VBox root = loader.load();
            
            Scene scene = new Scene(root);
            scene.getStylesheets().add(LoginView.class.getResource("/styles.css").toExternalForm());
            
            return scene;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
