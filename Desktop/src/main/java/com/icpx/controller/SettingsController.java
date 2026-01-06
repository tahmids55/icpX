package com.icpx.controller;

import com.icpx.database.SettingsDAO;
import com.icpx.database.UserDAO;
import com.icpx.model.User;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;

public class SettingsController {

    @FXML
    private TextField cfHandleField;

    @FXML
    private CheckBox startupPasswordCheckbox;

    @FXML
    public void initialize() {
        // Load saved handle
        String savedHandle = SettingsDAO.getCodeforcesHandle();
        if (savedHandle != null && !savedHandle.isEmpty()) {
            cfHandleField.setText(savedHandle);
        }

        // Load startup password preference
        try {
            User user = UserDAO.getCurrentUser();
            if (user != null) {
                startupPasswordCheckbox.setSelected(user.isStartupPasswordEnabled());
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @FXML
    private void saveHandle() {
        String handle = cfHandleField.getText().trim();
        if (!handle.isEmpty()) {
            if (SettingsDAO.setCodeforcesHandle(handle)) {
                showAlert(Alert.AlertType.INFORMATION, "Success", "Codeforces handle saved successfully!");
            } else {
                showAlert(Alert.AlertType.ERROR, "Error", "Failed to save Codeforces handle");
            }
        }
    }

    @FXML
    private void toggleStartupPassword() {
        try {
            boolean enabled = startupPasswordCheckbox.isSelected();
            UserDAO.toggleStartupPassword(enabled);
            
            showAlert(Alert.AlertType.INFORMATION, "Settings Updated", "Startup password has been " + (enabled ? "enabled" : "disabled"));
        } catch (Exception ex) {
            ex.printStackTrace();
            startupPasswordCheckbox.setSelected(!startupPasswordCheckbox.isSelected());
        }
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}