package com.icpx.view;

import com.icpx.database.TargetDAO;
import com.icpx.database.SettingsDAO;
import com.icpx.model.Target;
import com.icpx.service.CodeforcesService;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Text;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Target management view for tracking competitive programming goals
 */
public class TargetView {
    
    private static VBox targetListContainer;
    private static CodeforcesService codeforcesService = new CodeforcesService();

    public static VBox createTargetView() {
        VBox targetView = new VBox(20);
        targetView.getStyleClass().add("content-area");
        targetView.setPadding(new Insets(30));

        // Header
        HBox header = new HBox(15);
        header.setAlignment(Pos.CENTER_LEFT);

        Text title = new Text("🎯 Target Section");
        title.getStyleClass().add("title");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button addProblemBtn = new Button("➕ Add Problem");
        addProblemBtn.getStyleClass().addAll("button", "button-primary");
        addProblemBtn.setOnAction(e -> showAddProblemDialog());

        Button addTopicBtn = new Button("📚 Add Topic");
        addTopicBtn.getStyleClass().addAll("button", "button-secondary");
        addTopicBtn.setOnAction(e -> showAddTopicDialog());

        Button importContestBtn = new Button("📥 Import Contest");
        importContestBtn.getStyleClass().addAll("button", "button-secondary");
        importContestBtn.setOnAction(e -> showImportContestDialog());

        header.getChildren().addAll(title, spacer, addProblemBtn, addTopicBtn, importContestBtn);

        // Statistics Cards
        HBox statsContainer = createStatsSection();

        // Target List
        targetListContainer = new VBox(10);
        refreshTargetList();

        ScrollPane scrollPane = new ScrollPane(targetListContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.getStyleClass().add("scroll-pane");
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        targetView.getChildren().addAll(header, new Separator(), statsContainer, new Text("Your Targets:"), scrollPane);

        return targetView;
    }

    private static HBox createStatsSection() {
        HBox statsContainer = new HBox(20);
        statsContainer.setAlignment(Pos.CENTER_LEFT);

        int pendingCount = TargetDAO.getTargetCountByStatus("pending");
        int achievedCount = TargetDAO.getTargetCountByStatus("achieved");
        int failedCount = TargetDAO.getTargetCountByStatus("failed");

        VBox pendingCard = createStatCard("Pending", String.valueOf(pendingCount), "#FFA726");
        VBox achievedCard = createStatCard("Achieved", String.valueOf(achievedCount), "#4CAF50");
        VBox failedCard = createStatCard("Failed", String.valueOf(failedCount), "#F44336");

        statsContainer.getChildren().addAll(pendingCard, achievedCard, failedCard);
        return statsContainer;
    }

    private static VBox createStatCard(String title, String value, String color) {
        VBox card = new VBox(10);
        card.getStyleClass().add("card");
        card.setPadding(new Insets(20));
        card.setPrefWidth(180);
        card.setAlignment(Pos.CENTER);

        Text titleText = new Text(title);
        titleText.getStyleClass().add("subtitle");

        Text valueText = new Text(value);
        valueText.getStyleClass().add("title");
        valueText.setStyle("-fx-font-size: 42px; -fx-fill: " + color + ";");

        card.getChildren().addAll(titleText, valueText);
        return card;
    }

    private static void refreshTargetList() {
        targetListContainer.getChildren().clear();
        List<Target> targets = TargetDAO.getAllTargets();

        if (targets.isEmpty()) {
            Text emptyText = new Text("No targets yet. Add your first target!");
            emptyText.getStyleClass().add("subtitle");
            emptyText.setStyle("-fx-font-style: italic;");
            targetListContainer.getChildren().add(emptyText);
        } else {
            for (Target target : targets) {
                targetListContainer.getChildren().add(createTargetCard(target));
            }
        }
    }

    private static HBox createTargetCard(Target target) {
        HBox card = new HBox(15);
        card.getStyleClass().add("card");
        card.setPadding(new Insets(15, 20, 15, 20));
        card.setAlignment(Pos.CENTER_LEFT);

        // Icon and Name
        VBox nameSection = new VBox(5);
        String icon = "problem".equals(target.getType()) ? "📝" : "📚";
        Text nameText = new Text(icon + " " + target.getName());
        nameText.getStyleClass().add("label");
        nameText.setStyle("-fx-font-size: 16px;");

        if ("topic".equals(target.getType()) && target.getTopicName() != null) {
            Text topicText = new Text("Topic: " + target.getTopicName());
            topicText.getStyleClass().add("subtitle");
            nameSection.getChildren().addAll(nameText, topicText);
        } else {
            nameSection.getChildren().add(nameText);
        }
        HBox.setHgrow(nameSection, Priority.ALWAYS);

        // Status Label
        Label statusLabel = new Label(target.getStatus().toUpperCase());
        statusLabel.setPadding(new Insets(5, 10, 5, 10));
        statusLabel.setStyle("-fx-background-radius: 5px; -fx-font-weight: bold;");
        
        switch (target.getStatus().toLowerCase()) {
            case "achieved":
                statusLabel.setStyle(statusLabel.getStyle() + "-fx-background-color: #4CAF50; -fx-text-fill: white;");
                break;
            case "failed":
                statusLabel.setStyle(statusLabel.getStyle() + "-fx-background-color: #F44336; -fx-text-fill: white;");
                break;
            default:
                statusLabel.setStyle(statusLabel.getStyle() + "-fx-background-color: #FFA726; -fx-text-fill: white;");
        }

        // Action Buttons
        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);

        if ("problem".equals(target.getType())) {
            Button linkBtn = new Button("🔗 Link");
            linkBtn.getStyleClass().addAll("button", "button-secondary");
            linkBtn.setOnAction(e -> openLink(target.getProblemLink()));

            Button checkBtn = new Button("✓ Check");
            checkBtn.getStyleClass().addAll("button", "button-primary");
            if ("achieved".equals(target.getStatus())) {
                checkBtn.setDisable(true);
            }
            checkBtn.setOnAction(e -> checkProblemStatus(target));

            buttonBox.getChildren().addAll(linkBtn, checkBtn);
        }

        Button deleteBtn = new Button("🗑");
        deleteBtn.getStyleClass().addAll("button", "button-danger");
        deleteBtn.setOnAction(e -> deleteTarget(target));

        buttonBox.getChildren().add(deleteBtn);

        card.getChildren().addAll(nameSection, statusLabel, buttonBox);
        return card;
    }

    private static void showAddProblemDialog() {
        Dialog<Target> dialog = new Dialog<>();
        dialog.setTitle("Add Problem Target");
        dialog.setHeaderText("Add a new problem to track");

        ButtonType addButtonType = new ButtonType("Add", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(addButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        TextField nameField = new TextField();
        nameField.setPromptText("Problem name");
        TextField linkField = new TextField();
        linkField.setPromptText("https://codeforces.com/...");

        grid.add(new Label("Problem Name:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Problem Link:"), 0, 1);
        grid.add(linkField, 1, 1);

        dialog.getDialogPane().setContent(grid);

        Platform.runLater(nameField::requestFocus);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == addButtonType) {
                String name = nameField.getText().trim();
                String link = linkField.getText().trim();
                
                if (!name.isEmpty() && !link.isEmpty()) {
                    Target target = new Target("problem", name);
                    target.setProblemLink(link);
                    return target;
                }
            }
            return null;
        });

        Optional<Target> result = dialog.showAndWait();
        result.ifPresent(target -> {
            if (TargetDAO.insertTarget(target)) {
                refreshTargetList();
                showAlert(Alert.AlertType.INFORMATION, "Success", "Problem target added!");
            } else {
                showAlert(Alert.AlertType.ERROR, "Error", "Failed to add target");
            }
        });
    }

    private static void showAddTopicDialog() {
        Dialog<Target> dialog = new Dialog<>();
        dialog.setTitle("Add Topic Target");
        dialog.setHeaderText("Add a new topic to learn");

        ButtonType addButtonType = new ButtonType("Add", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(addButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        TextField topicField = new TextField();
        topicField.setPromptText("e.g., Dynamic Programming");
        TextField descField = new TextField();
        descField.setPromptText("Description");
        TextField urlField = new TextField();
        urlField.setPromptText("Resource URL (optional)");

        grid.add(new Label("Topic Name:"), 0, 0);
        grid.add(topicField, 1, 0);
        grid.add(new Label("Description:"), 0, 1);
        grid.add(descField, 1, 1);
        grid.add(new Label("Resource URL:"), 0, 2);
        grid.add(urlField, 1, 2);

        dialog.getDialogPane().setContent(grid);

        Platform.runLater(topicField::requestFocus);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == addButtonType) {
                String topic = topicField.getText().trim();
                String desc = descField.getText().trim();
                String url = urlField.getText().trim();
                
                if (!topic.isEmpty() && !desc.isEmpty()) {
                    Target target = new Target("topic", desc);
                    target.setTopicName(topic);
                    target.setWebsiteUrl(url.isEmpty() ? null : url);
                    return target;
                }
            }
            return null;
        });

        Optional<Target> result = dialog.showAndWait();
        result.ifPresent(target -> {
            if (TargetDAO.insertTarget(target)) {
                refreshTargetList();
                showAlert(Alert.AlertType.INFORMATION, "Success", "Topic target added!");
            } else {
                showAlert(Alert.AlertType.ERROR, "Error", "Failed to add target");
            }
        });
    }

    private static void showImportContestDialog() {
        Dialog<ContestImportConfig> dialog = new Dialog<>();
        dialog.setTitle("Import Contest");
        dialog.setHeaderText("Import problems from a Codeforces contest");

        ButtonType importButtonType = new ButtonType("Import", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(importButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(15);
        grid.setPadding(new Insets(20));

        TextField contestIdField = new TextField();
        contestIdField.setPromptText("e.g., 1234");

        // Problem range selection
        ComboBox<String> fromProblemCombo = new ComboBox<>();
        fromProblemCombo.getItems().addAll("A", "B", "C", "D", "E", "F", "G", "H", "I", "J");
        fromProblemCombo.setValue("A");
        fromProblemCombo.getStyleClass().add("combo-box");

        ComboBox<String> toProblemCombo = new ComboBox<>();
        toProblemCombo.getItems().addAll("A", "B", "C", "D", "E", "F", "G", "H", "I", "J");
        toProblemCombo.setValue("E");
        toProblemCombo.getStyleClass().add("combo-box");

        HBox rangeBox = new HBox(10);
        rangeBox.setAlignment(Pos.CENTER_LEFT);
        rangeBox.getChildren().addAll(fromProblemCombo, new Label("to"), toProblemCombo);

        grid.add(new Label("Contest ID:"), 0, 0);
        grid.add(contestIdField, 1, 0);
        grid.add(new Label("Problem Range:"), 0, 1);
        grid.add(rangeBox, 1, 1);

        Text infoText = new Text("Select which problems to import (e.g., A to E)");
        infoText.setStyle("-fx-font-size: 11px; -fx-fill: gray;");
        grid.add(infoText, 1, 2);

        dialog.getDialogPane().setContent(grid);

        Platform.runLater(contestIdField::requestFocus);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == importButtonType) {
                String contestId = contestIdField.getText().trim();
                String fromProblem = fromProblemCombo.getValue();
                String toProblem = toProblemCombo.getValue();
                
                if (!contestId.isEmpty()) {
                    return new ContestImportConfig(contestId, fromProblem, toProblem);
                }
            }
            return null;
        });

        Optional<ContestImportConfig> result = dialog.showAndWait();
        result.ifPresent(config -> importContest(config.contestId, config.fromProblem, config.toProblem));
    }

    private static void importContest(String contestId, String fromProblem, String toProblem) {
        new Thread(() -> {
            try {
                List<CodeforcesService.ContestProblem> problems = codeforcesService.fetchContestProblems(contestId);
                
                Platform.runLater(() -> {
                    if (problems.isEmpty()) {
                        showAlert(Alert.AlertType.ERROR, "Error", "No problems found or invalid contest ID");
                    } else {
                        // Filter problems based on selected range
                        List<CodeforcesService.ContestProblem> filteredProblems = filterProblemsByRange(problems, fromProblem, toProblem);
                        
                        if (filteredProblems.isEmpty()) {
                            showAlert(Alert.AlertType.WARNING, "No Problems", 
                                "No problems found in the range " + fromProblem + " to " + toProblem);
                            return;
                        }
                        
                        int count = 0;
                        for (CodeforcesService.ContestProblem problem : filteredProblems) {
                            Target target = new Target("problem", problem.name);
                            target.setProblemLink(problem.getUrl());
                            if (TargetDAO.insertTarget(target)) {
                                count++;
                            }
                        }
                        refreshTargetList();
                        showAlert(Alert.AlertType.INFORMATION, "Success", 
                            "Imported " + count + " problems (" + fromProblem + " to " + toProblem + ") from contest " + contestId);
                    }
                });
            } catch (IOException e) {
                Platform.runLater(() -> 
                    showAlert(Alert.AlertType.ERROR, "Error", "Failed to fetch contest: " + e.getMessage())
                );
            }
        }).start();
    }

    private static List<CodeforcesService.ContestProblem> filterProblemsByRange(
            List<CodeforcesService.ContestProblem> problems, String fromProblem, String toProblem) {
        List<CodeforcesService.ContestProblem> filtered = new ArrayList<>();
        
        char fromChar = fromProblem.charAt(0);
        char toChar = toProblem.charAt(0);
        
        // Ensure from is less than or equal to to
        if (fromChar > toChar) {
            char temp = fromChar;
            fromChar = toChar;
            toChar = temp;
        }
        
        for (CodeforcesService.ContestProblem problem : problems) {
            if (problem.index.length() > 0) {
                char problemChar = problem.index.charAt(0);
                if (problemChar >= fromChar && problemChar <= toChar) {
                    filtered.add(problem);
                }
            }
        }
        
        return filtered;
    }

    private static void checkProblemStatus(Target target) {
        String handle = SettingsDAO.getCodeforcesHandle();
        if (handle == null || handle.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Missing Handle", 
                "Please set your Codeforces handle in Settings first!");
            return;
        }

        new Thread(() -> {
            try {
                boolean accepted = codeforcesService.checkProblemAccepted(handle, target.getProblemLink());
                
                Platform.runLater(() -> {
                    if (accepted) {
                        target.setStatus("achieved");
                        TargetDAO.updateTargetStatus(target.getId(), "achieved");
                        showAlert(Alert.AlertType.INFORMATION, "Success!", 
                            "Problem accepted! Target achieved! 🎉");
                    } else {
                        target.setStatus("failed");
                        TargetDAO.updateTargetStatus(target.getId(), "failed");
                        showAlert(Alert.AlertType.WARNING, "Not Accepted", 
                            "Problem not accepted yet. Try again!");
                    }
                    refreshTargetList();
                });
            } catch (IOException e) {
                Platform.runLater(() -> 
                    showAlert(Alert.AlertType.ERROR, "Error", 
                        "Failed to check status: " + e.getMessage())
                );
            }
        }).start();
    }

    private static void deleteTarget(Target target) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Target");
        alert.setHeaderText("Are you sure you want to delete this target?");
        alert.setContentText(target.getName());

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            if (TargetDAO.deleteTarget(target.getId())) {
                refreshTargetList();
                showAlert(Alert.AlertType.INFORMATION, "Success", "Target deleted!");
            } else {
                showAlert(Alert.AlertType.ERROR, "Error", "Failed to delete target");
            }
        }
    }

    private static void openLink(String url) {
        if (url == null || url.isEmpty()) {
            return;
        }

        String os = System.getProperty("os.name").toLowerCase();
        try {
            if (os.contains("nix") || os.contains("nux")) {
                Runtime.getRuntime().exec(new String[]{"xdg-open", url});
            } else if (os.contains("mac")) {
                Runtime.getRuntime().exec(new String[]{"open", url});
            } else if (os.contains("win")) {
                Runtime.getRuntime().exec(new String[]{"rundll32", "url.dll,FileProtocolHandler", url});
            } else {
                if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                    Desktop.getDesktop().browse(new URI(url));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to open link");
        }
    }

    private static void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // Helper class for contest import configuration
    private static class ContestImportConfig {
        String contestId;
        String fromProblem;
        String toProblem;
        
        ContestImportConfig(String contestId, String fromProblem, String toProblem) {
            this.contestId = contestId;
            this.fromProblem = fromProblem;
            this.toProblem = toProblem;
        }
    }
}
