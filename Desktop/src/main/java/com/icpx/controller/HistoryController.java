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

public class HistoryController {

    @FXML
    private Button backButton;

    @FXML
    private Text totalSolvedText;

    @FXML
    private Text avgRatingText;

    @FXML
    private TableView<Target> problemsTable;

    @FXML
    private TableColumn<Target, String> nameColumn;

    @FXML
    private TableColumn<Target, String> ratingColumn;

    @FXML
    private TableColumn<Target, String> linkColumn;

    @FXML
    private TableColumn<Target, String> dateColumn;

    private ObservableList<Target> solvedProblems;

    @FXML
    public void initialize() {
        setupTable();
        loadSolvedProblems();
    }

    private void setupTable() {
        // Setup name column
        nameColumn.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getName()));

        // Setup rating column
        ratingColumn.setCellValueFactory(cellData -> {
            Integer rating = cellData.getValue().getRating();
            return new SimpleStringProperty(rating != null ? String.valueOf(rating) : "N/A");
        });

        // Setup link column with clickable hyperlink
        linkColumn.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getProblemLink()));
        
        linkColumn.setCellFactory(col -> new TableCell<Target, String>() {
            private final Hyperlink hyperlink = new Hyperlink();
            
            @Override
            protected void updateItem(String link, boolean empty) {
                super.updateItem(link, empty);
                if (empty || link == null) {
                    setGraphic(null);
                } else {
                    hyperlink.setText(link);
                    hyperlink.setOnAction(event -> {
                        try {
                            java.awt.Desktop.getDesktop().browse(new java.net.URI(link));
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

    private void loadSolvedProblems() {
        List<Target> problems = TargetDAO.getAchievedProblems();
        solvedProblems = FXCollections.observableArrayList(problems);
        problemsTable.setItems(solvedProblems);

        // Update statistics
        totalSolvedText.setText(String.valueOf(problems.size()));

        // Calculate average rating
        if (!problems.isEmpty()) {
            double avgRating = problems.stream()
                    .filter(p -> p.getRating() != null)
                    .mapToInt(Target::getRating)
                    .average()
                    .orElse(0.0);
            avgRatingText.setText(String.format("%.0f", avgRating));
        } else {
            avgRatingText.setText("0");
        }
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
