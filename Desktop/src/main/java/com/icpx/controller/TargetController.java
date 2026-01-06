package com.icpx.controller;

import com.icpx.database.SettingsDAO;
import com.icpx.database.TargetDAO;
import com.icpx.model.Target;
import com.icpx.service.CodeforcesService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class TargetController {

    @FXML
    private HBox statsContainer;

    @FXML
    private VBox targetListContainer;

    private CodeforcesService codeforcesService = new CodeforcesService();

    @FXML
    public void initialize() {
        refreshStats();
        refreshTargetList();
    }
    
    private void refreshStats() {
        statsContainer.getChildren().clear();
        int pendingCount = TargetDAO.getTargetCountByStatus("pending");
        int achievedCount = TargetDAO.getTargetCountByStatus("achieved");
        int failedCount = TargetDAO.getTargetCountByStatus("failed");

        VBox pendingCard = createStatCard("Pending", String.valueOf(pendingCount), "#FFA726");
        VBox achievedCard = createStatCard("Achieved", String.valueOf(achievedCount), "#4CAF50");
        VBox failedCard = createStatCard("Failed", String.valueOf(failedCount), "#F44336");

        statsContainer.getChildren().addAll(pendingCard, achievedCard, failedCard);
    }

    private VBox createStatCard(String title, String value, String color) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/icpx/view/TargetStatCard.fxml"));
            VBox card = loader.load();
            TargetStatCardController controller = loader.getController();
            controller.setContent(title, value, color);
            return card;
        } catch (IOException e) {
            e.printStackTrace();
            return new VBox();
        }
    }

    private void refreshTargetList() {
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

    private HBox createTargetCard(Target target) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/icpx/view/TargetCard.fxml"));
            HBox card = loader.load();
            TargetCardController controller = loader.getController();
            
            controller.initialize(
                target,
                () -> openLink(target.getProblemLink()),
                () -> checkProblemStatus(target),
                () -> deleteTarget(target)
            );
            
            return card;
        } catch (IOException e) {
            e.printStackTrace();
            return new HBox();
        }
    }

    @FXML
    private void showAddProblemDialog() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/icpx/view/AddProblemDialog.fxml"));
            GridPane grid = loader.load();
            AddProblemDialogController controller = loader.getController();

            Dialog<Target> dialog = new Dialog<>();
            dialog.setTitle("Add Problem Target");
            dialog.setHeaderText("Add a new problem to track");

            ButtonType addButtonType = new ButtonType("Add", ButtonBar.ButtonData.OK_DONE);
            dialog.getDialogPane().getButtonTypes().addAll(addButtonType, ButtonType.CANCEL);
            dialog.getDialogPane().setContent(grid);

            dialog.setResultConverter(dialogButton -> {
                if (dialogButton == addButtonType) {
                    return controller.getResult();
                }
                return null;
            });

            Optional<Target> result = dialog.showAndWait();
            result.ifPresent(target -> {
                if (TargetDAO.insertTarget(target)) {
                    refreshTargetList();
                    refreshStats();
                    showAlert(Alert.AlertType.INFORMATION, "Success", "Problem target added!");
                } else {
                    showAlert(Alert.AlertType.ERROR, "Error", "Failed to add target");
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to load dialog");
        }
    }

    @FXML
    private void showAddTopicDialog() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/icpx/view/AddTopicDialog.fxml"));
            GridPane grid = loader.load();
            AddTopicDialogController controller = loader.getController();

            Dialog<Target> dialog = new Dialog<>();
            dialog.setTitle("Add Topic Target");
            dialog.setHeaderText("Add a new topic to learn");

            ButtonType addButtonType = new ButtonType("Add", ButtonBar.ButtonData.OK_DONE);
            dialog.getDialogPane().getButtonTypes().addAll(addButtonType, ButtonType.CANCEL);
            dialog.getDialogPane().setContent(grid);

            dialog.setResultConverter(dialogButton -> {
                if (dialogButton == addButtonType) {
                    return controller.getResult();
                }
                return null;
            });

            Optional<Target> result = dialog.showAndWait();
            result.ifPresent(target -> {
                if (TargetDAO.insertTarget(target)) {
                    refreshTargetList();
                    refreshStats();
                    showAlert(Alert.AlertType.INFORMATION, "Success", "Topic target added!");
                } else {
                    showAlert(Alert.AlertType.ERROR, "Error", "Failed to add target");
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to load dialog");
        }
    }

    @FXML
    private void showImportContestDialog() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/icpx/view/ImportContestDialog.fxml"));
            GridPane grid = loader.load();
            ImportContestDialogController controller = loader.getController();

            Dialog<ContestImportConfig> dialog = new Dialog<>();
            dialog.setTitle("Import Contest");
            dialog.setHeaderText("Import problems from a Codeforces contest");

            ButtonType importButtonType = new ButtonType("Import", ButtonBar.ButtonData.OK_DONE);
            dialog.getDialogPane().getButtonTypes().addAll(importButtonType, ButtonType.CANCEL);
            dialog.getDialogPane().setContent(grid);

            dialog.setResultConverter(dialogButton -> {
                if (dialogButton == importButtonType && controller.isValid()) {
                    return new ContestImportConfig(
                        controller.getContestId(),
                        controller.getFromProblem(),
                        controller.getToProblem()
                    );
                }
                return null;
            });

            Optional<ContestImportConfig> result = dialog.showAndWait();
            result.ifPresent(config -> importContest(config.contestId, config.fromProblem, config.toProblem));
        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "Failed to load dialog");
        }
    }

    private void importContest(String contestId, String fromProblem, String toProblem) {
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
                        refreshStats();
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

    private List<CodeforcesService.ContestProblem> filterProblemsByRange(
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

    private void checkProblemStatus(Target target) {
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
                    refreshStats();
                });
            } catch (IOException e) {
                Platform.runLater(() -> 
                    showAlert(Alert.AlertType.ERROR, "Error", 
                        "Failed to check status: " + e.getMessage())
                );
            }
        }).start();
    }

    private void deleteTarget(Target target) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Target");
        alert.setHeaderText("Are you sure you want to delete this target?");
        alert.setContentText(target.getName());

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            if (TargetDAO.deleteTarget(target.getId())) {
                refreshTargetList();
                refreshStats();
                showAlert(Alert.AlertType.INFORMATION, "Success", "Target deleted!");
            } else {
                showAlert(Alert.AlertType.ERROR, "Error", "Failed to delete target");
            }
        }
    }

    private void openLink(String url) {
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

    private void showAlert(Alert.AlertType type, String title, String message) {
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