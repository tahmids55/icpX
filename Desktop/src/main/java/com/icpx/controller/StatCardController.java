package com.icpx.controller;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.text.Text;

/**
 * Controller for reusable statistic cards on the dashboard.
 */
public class StatCardController {

    @FXML
    private Text titleText;

    @FXML
    private Text valueText;

    @FXML
    private Button actionButton;

    public void setContent(String title, String value, String buttonText, EventHandler<ActionEvent> action) {
        titleText.setText(title);
        valueText.setText(value);
        actionButton.setText(buttonText);
        actionButton.setOnAction(action);
    }
}
