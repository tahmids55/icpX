package com.icpx.view;

import javafx.fxml.FXMLLoader;
import javafx.scene.layout.VBox;
import java.io.IOException;

/**
 * Target management view for tracking competitive programming goals
 */
public class TargetView {
    
    public static VBox createTargetView() {
        try {
            FXMLLoader loader = new FXMLLoader(TargetView.class.getResource("/com/icpx/view/TargetView.fxml"));
            return loader.load();
        } catch (IOException e) {
            e.printStackTrace();
            return new VBox();
        }
    }
}
