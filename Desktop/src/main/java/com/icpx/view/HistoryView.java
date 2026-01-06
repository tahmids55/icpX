package com.icpx.view;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;

import java.io.IOException;

/**
 * View class for history of solved problems
 */
public class HistoryView {
    private Scene scene;

    public HistoryView() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/icpx/view/HistoryView.fxml"));
        Parent root = loader.load();
        scene = new Scene(root, 1000, 700);
    }

    public Scene getScene() {
        return scene;
    }
}
