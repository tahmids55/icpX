package com.icpx.controller;

import com.icpx.model.Target;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;

public class AddTopicDialogController {

    @FXML
    private TextField topicField;

    @FXML
    private TextField descField;

    @FXML
    private TextField urlField;

    @FXML
    public void initialize() {
        javafx.application.Platform.runLater(() -> topicField.requestFocus());
    }

    public Target getResult() {
        String topic = topicField.getText().trim();
        String desc = descField.getText().trim();
        String url = urlField.getText().trim();
        
        if (!topic.isEmpty() && !desc.isEmpty()) {
            Target target = new Target("topic", desc);
            target.setTopicName(topic);
            target.setWebsiteUrl(url.isEmpty() ? null : url);
            return target;
        }
        return null;
    }
}
