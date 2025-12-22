package com.icpx.controller;

import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;

public class ImportContestDialogController {

    @FXML
    private TextField contestIdField;

    @FXML
    private ComboBox<String> fromProblemCombo;

    @FXML
    private ComboBox<String> toProblemCombo;

    @FXML
    public void initialize() {
        fromProblemCombo.getItems().addAll("A", "B", "C", "D", "E", "F", "G", "H", "I", "J");
        fromProblemCombo.setValue("A");
        
        toProblemCombo.getItems().addAll("A", "B", "C", "D", "E", "F", "G", "H", "I", "J");
        toProblemCombo.setValue("E");
        
        javafx.application.Platform.runLater(() -> contestIdField.requestFocus());
    }

    public String getContestId() {
        return contestIdField.getText().trim();
    }

    public String getFromProblem() {
        return fromProblemCombo.getValue();
    }

    public String getToProblem() {
        return toProblemCombo.getValue();
    }

    public boolean isValid() {
        return !getContestId().isEmpty();
    }
}
