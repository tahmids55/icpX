package com.icpx.controller;

import com.icpx.database.TargetDAO;
import com.icpx.model.Target;
import com.icpx.util.SceneManager;
import com.icpx.view.DashboardView;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.text.Text;

import java.time.format.DateTimeFormatter;
import java.util.List;

public class TopicHistoryController {

    @FXML
    private Button backButton;

    @FXML
    private Text totalLearnedText;

    @FXML
    private TableView<Target> topicsTable;

    @FXML
    private TableColumn<Target, String> topicColumn;

    @FXML
    private TableColumn<Target, String> descriptionColumn;

    @FXML
    private TableColumn<Target, String> urlColumn;

    @FXML
    private TableColumn<Target, String> dateColumn;

    private ObservableList<Target> learnedTopics;

    @FXML
    public void initialize() {
        setupTable();
        loadLearnedTopics();
    }

    private void setupTable() {
        // Setup topic column
        topicColumn.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getTopicName()));

        // Setup description column
        descriptionColumn.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getName()));

        // Setup URL column with clickable hyperlink
        urlColumn.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getWebsiteUrl()));
        
        urlColumn.setCellFactory(col -> new TableCell<Target, String>() {
            private final Hyperlink hyperlink = new Hyperlink();
            
            @Override
            protected void updateItem(String url, boolean empty) {
                super.updateItem(url, empty);
                if (empty || url == null || url.isEmpty()) {
                    setGraphic(null);
                    setText(url == null || url.isEmpty() ? "N/A" : null);
                } else {
                    hyperlink.setText(url);
                    hyperlink.setOnAction(event -> {
                        try {
                            java.awt.Desktop.getDesktop().browse(new java.net.URI(url));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    });
                    setGraphic(hyperlink);
                }
            }
        });

        // Setup date column
        dateColumn.setCellValueFactory(cellData -> {
            if (cellData.getValue().getCreatedAt() != null) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                return new SimpleStringProperty(cellData.getValue().getCreatedAt().format(formatter));
            }
            return new SimpleStringProperty("N/A");
        });
    }

    private void loadLearnedTopics() {
        List<Target> topics = TargetDAO.getAchievedTopics();
        learnedTopics = FXCollections.observableArrayList(topics);
        topicsTable.setItems(learnedTopics);

        // Update statistics
        totalLearnedText.setText(String.valueOf(topics.size()));
    }

    @FXML
    private void handleBack() {
        try {
            SceneManager.switchScene(DashboardView.createScene());
        } catch (Exception e) {
            e.printStackTrace();
            showError("Navigation Error", "Failed to return to dashboard");
        }
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
