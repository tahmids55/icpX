package com.icpx.util;

import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Centralized scene manager for navigation between views
 */
public class SceneManager {
    private static Stage primaryStage;
    private static String currentTheme = "light";

    public static void setPrimaryStage(Stage stage) {
        primaryStage = stage;
    }

    public static Stage getPrimaryStage() {
        return primaryStage;
    }

    public static void switchScene(Scene scene) {
        if (primaryStage != null) {
            primaryStage.setScene(scene);
            applyTheme(scene);
        }
    }

    public static String getCurrentTheme() {
        return currentTheme;
    }

    public static void setTheme(String theme) {
        currentTheme = theme;
        if (primaryStage != null && primaryStage.getScene() != null) {
            applyTheme(primaryStage.getScene());
        }
    }

    private static void applyTheme(Scene scene) {
        scene.getRoot().getStyleClass().removeAll("light-theme", "dark-theme");
        scene.getRoot().getStyleClass().add(currentTheme + "-theme");
    }
}
