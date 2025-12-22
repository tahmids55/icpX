package com.icpx.util;

import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Centralized scene manager for navigation between views
 */
public class SceneManager {
    private static Stage primaryStage;

    public static void setPrimaryStage(Stage stage) {
        primaryStage = stage;
    }

    public static Stage getPrimaryStage() {
        return primaryStage;
    }

    public static void switchScene(Scene scene) {
        if (primaryStage != null) {
            primaryStage.setScene(scene);
        }
    }
}
