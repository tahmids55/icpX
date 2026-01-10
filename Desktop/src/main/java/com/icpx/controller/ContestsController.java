package com.icpx.controller;

import com.icpx.database.TargetDAO;
import com.icpx.model.Target;
import javafx.scene.layout.GridPane;
import javafx.util.Pair;
import com.icpx.MainApp;
import com.icpx.model.Contest;
import com.icpx.service.CodeforcesService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Controller for Contests view
 */
public class ContestsController {

    @FXML
    private TabPane contestTabPane;
    
    @FXML
    private TableView<Contest> upcomingTable;
    
    @FXML
    private TableView<Contest> runningTable;
    
    @FXML
    private TableView<Contest> finishedTable;
    
    @FXML
    private Button refreshButton;
    
    @FXML
    private Label statusLabel;
    
    @FXML
    private ProgressIndicator progressIndicator;

    private CodeforcesService codeforcesService;
    private ObservableList<Contest> allContests;

    @FXML
    public void initialize() {
        codeforcesService = new CodeforcesService();
        allContests = FXCollections.observableArrayList();
        
        setupTables();
        loadContests();
    }

    private void setupTables() {
        setupTable(upcomingTable);
        setupTable(runningTable);
        setupTable(finishedTable);
    }

    private void setupTable(TableView<Contest> table) {
        // Name column
        TableColumn<Contest, String> nameCol = new TableColumn<>("Contest Name");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        nameCol.setPrefWidth(400);
        
        // Type column
        TableColumn<Contest, String> typeCol = new TableColumn<>("Type");
        typeCol.setCellValueFactory(new PropertyValueFactory<>("type"));
        typeCol.setPrefWidth(100);
        
        // Start Time column
        TableColumn<Contest, String> startCol = new TableColumn<>("Start Time");
        startCol.setCellValueFactory(cellData -> {
            Contest contest = cellData.getValue();
            if (contest.getStartTimeSeconds() > 0) {
                LocalDateTime dateTime = LocalDateTime.ofInstant(
                    Instant.ofEpochSecond(contest.getStartTimeSeconds()),
                    ZoneId.systemDefault()
                );
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm");
                return new javafx.beans.property.SimpleStringProperty(dateTime.format(formatter));
            }
            return new javafx.beans.property.SimpleStringProperty("N/A");
        });
        startCol.setPrefWidth(150);
        
        // Duration column
        TableColumn<Contest, String> durationCol = new TableColumn<>("Duration");
        durationCol.setCellValueFactory(cellData -> {
            Contest contest = cellData.getValue();
            long hours = contest.getDurationSeconds() / 3600;
            long minutes = (contest.getDurationSeconds() % 3600) / 60;
            String duration = String.format("%dh %dm", hours, minutes);
            return new javafx.beans.property.SimpleStringProperty(duration);
        });
        durationCol.setPrefWidth(100);
        
        table.getColumns().addAll(nameCol, typeCol, startCol, durationCol);
        
        // Add double-click handler to open contest URL
        table.setRowFactory(tv -> {
            TableRow<Contest> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    Contest contest = row.getItem();
                    openContestUrl(contest);
                }
            });
            return row;
        });
        
        // Add context menu
        ContextMenu contextMenu = new ContextMenu();
        MenuItem openItem = new MenuItem("Open in Browser");
        MenuItem importItem = new MenuItem("Import Problems");
        
        openItem.setOnAction(e -> {
            Contest selected = table.getSelectionModel().getSelectedItem();
            if (selected != null) {
                openContestUrl(selected);
            }
        });
        
        importItem.setOnAction(e -> {
            Contest selected = table.getSelectionModel().getSelectedItem();
            if (selected != null) {
                importContestProblems(selected);
            }
        });
        
        contextMenu.getItems().addAll(openItem, importItem);
        table.setContextMenu(contextMenu);
    }

    @FXML
    private void handleRefresh() {
        loadContests();
    }

    private void loadContests() {
        progressIndicator.setVisible(true);
        statusLabel.setText("Loading contests...");
        refreshButton.setDisable(true);
        
        new Thread(() -> {
            try {
                List<Contest> contests = codeforcesService.fetchContests(false);
                
                Platform.runLater(() -> {
                    allContests.setAll(contests);
                    filterContests();
                    progressIndicator.setVisible(false);
                    statusLabel.setText("Loaded " + contests.size() + " contests");
                    refreshButton.setDisable(false);
                    
                    // Save upcoming contests to DAO for reminders
                    new Thread(() -> {
                        com.icpx.database.ContestDAO.saveContests(contests);
                    }).start();
                });
            } catch (IOException e) {
                Platform.runLater(() -> {
                    progressIndicator.setVisible(false);
                    statusLabel.setText("Error loading contests: " + e.getMessage());
                    refreshButton.setDisable(false);
                    
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Error");
                    alert.setHeaderText("Failed to load contests");
                    alert.setContentText(e.getMessage());
                    alert.showAndWait();
                });
            }
        }).start();
    }

    private void filterContests() {
        List<Contest> upcoming = allContests.stream()
            .filter(Contest::isUpcoming)
            .collect(Collectors.toList());
        
        List<Contest> running = allContests.stream()
            .filter(Contest::isRunning)
            .collect(Collectors.toList());
        
        List<Contest> finished = allContests.stream()
            .filter(Contest::isFinished)
            .limit(50) // Limit finished contests to 50
            .collect(Collectors.toList());
        
        upcomingTable.setItems(FXCollections.observableArrayList(upcoming));
        runningTable.setItems(FXCollections.observableArrayList(running));
        finishedTable.setItems(FXCollections.observableArrayList(finished));
    }

    private void openContestUrl(Contest contest) {
        if (MainApp.getInstance() != null) {
            MainApp.getInstance().openUrl(contest.getContestUrl());
        }
    }

    private void importContestProblems(Contest contest) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Import Contest");
        alert.setHeaderText("Import problems from: " + contest.getName());
        alert.setContentText("This feature will import all problems from this contest as targets.\n\n" +
                             "Contest ID: " + contest.getId() + "\n" +
                             "Would you like to proceed?");
        
        ButtonType yesButton = new ButtonType("Yes", ButtonBar.ButtonData.YES);
        ButtonType noButton = new ButtonType("No", ButtonBar.ButtonData.NO);
        alert.getButtonTypes().setAll(yesButton, noButton);
        
        alert.showAndWait().ifPresent(response -> {
            if (response == yesButton) {
                performImport(contest);
            }
        });
    }

    private void performImport(Contest contest) {
        progressIndicator.setVisible(true);
        statusLabel.setText("Importing problems from contest " + contest.getId() + "...");
        
        new Thread(() -> {
            try {
                List<CodeforcesService.ContestProblem> problems = 
                    codeforcesService.fetchContestProblems(String.valueOf(contest.getId()));
                
                Platform.runLater(() -> {
                    progressIndicator.setVisible(false);
                    
                    if (problems.isEmpty()) {
                        statusLabel.setText("No problems found in contest");
                        Alert alert = new Alert(Alert.AlertType.WARNING);
                        alert.setTitle("Import Result");
                        alert.setHeaderText("No problems found");
                        alert.setContentText("The contest may not have started yet or has no problems.");
                        alert.showAndWait();
                    } else {
                        statusLabel.setText("Found " + problems.size() + " problems");
                        showImportDialog(contest, problems);
                    }
                });
            } catch (IOException e) {
                Platform.runLater(() -> {
                    progressIndicator.setVisible(false);
                    statusLabel.setText("Error importing contest");
                    
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Import Error");
                    alert.setHeaderText("Failed to import contest");
                    alert.setContentText(e.getMessage());
                    alert.showAndWait();
                });
            }
        }).start();
    }

    private void showImportDialog(Contest contest, List<CodeforcesService.ContestProblem> problems) {
        Dialog<Pair<Integer, Integer>> dialog = new Dialog<>();
        dialog.setTitle("Import Problems");
        dialog.setHeaderText("Import Problems from " + contest.getName());

        ButtonType importButtonType = new ButtonType("Import", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(importButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new javafx.geometry.Insets(20, 150, 10, 10));

        ComboBox<String> fromCombo = new ComboBox<>();
        ComboBox<String> toCombo = new ComboBox<>();
        
        // Populate combos with indices
        for (CodeforcesService.ContestProblem p : problems) {
            String label = p.index + " - " + p.name;
            fromCombo.getItems().add(label);
            toCombo.getItems().add(label);
        }
        
        if (!problems.isEmpty()) {
            fromCombo.getSelectionModel().selectFirst();
            toCombo.getSelectionModel().selectLast();
        }

        grid.add(new Label("From Problem:"), 0, 0);
        grid.add(fromCombo, 1, 0);
        grid.add(new Label("To Problem:"), 0, 1);
        grid.add(toCombo, 1, 1);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == importButtonType) {
                return new Pair<>(fromCombo.getSelectionModel().getSelectedIndex(), 
                                  toCombo.getSelectionModel().getSelectedIndex());
            }
            return null;
        });

        java.util.Optional<Pair<Integer, Integer>> result = dialog.showAndWait();

        result.ifPresent(range -> {
            int start = range.getKey();
            int end = range.getValue();
            
            if (start > end) {
                // Swap if inverted
                int temp = start;
                start = end;
                end = temp;
            }
            
            List<CodeforcesService.ContestProblem> selectedProblems = new java.util.ArrayList<>();
            for (int i = start; i <= end; i++) {
                selectedProblems.add(problems.get(i));
            }
            
            saveImportedProblems(selectedProblems, contest);
        });
    }

    private void saveImportedProblems(List<CodeforcesService.ContestProblem> problems, Contest contest) {
        int count = 0;
        for (CodeforcesService.ContestProblem problem : problems) {
            Target target = new Target();
            target.setType("problem");
            // Standard format: "123A - Problem Name" or just "A - Problem Name" (User preference usually)
            // Let's use "123A - Problem Name" to be distinct
            target.setName(contest.getId() + problem.index + " - " + problem.name);
            target.setProblemLink(problem.getUrl());
            target.setWebsiteUrl("Codeforces");
            target.setCreatedAt(java.time.LocalDateTime.now());
            target.setStatus("pending");
            
            // Try to insert
            if (TargetDAO.insertTarget(target)) {
                count++;
            }
        }
        
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Import Complete");
        alert.setHeaderText(null);
        alert.setContentText("Successfully imported " + count + " problems to Targets.");
        alert.showAndWait();
    }
}
