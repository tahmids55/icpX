package com.icpx.controller;

import com.icpx.model.Target;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;

public class AddProblemDialogController {

    @FXML
    private TextField nameField;

    @FXML
    private TextField linkField;

    @FXML
    public void initialize() {
        // Focus on name field when dialog opens
        javafx.application.Platform.runLater(() -> nameField.requestFocus());
    }

    public Target getResult() {
        String name = nameField.getText().trim();
        String link = linkField.getText().trim();
        
        if (!name.isEmpty() && !link.isEmpty()) {
            Target target = new Target("problem", name);
            target.setProblemLink(link);
            return target;
        }
        return null;
    }

    public TextField getNameField() {
        return nameField;
    }

    public TextField getLinkField() {
        return linkField;
    }
}
