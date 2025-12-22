package com.icpx.controller;

import javafx.fxml.FXML;
import javafx.scene.text.Text;

/**
 * Controller for target statistic cards.
 */
public class TargetStatCardController {

    @FXML
    private Text titleText;

    @FXML
    private Text valueText;

    public void setContent(String title, String value, String colorHex) {
        titleText.setText(title);
        valueText.setText(value);
        valueText.setStyle("-fx-font-size: 42px; -fx-fill: " + colorHex + ";");
    }
}
