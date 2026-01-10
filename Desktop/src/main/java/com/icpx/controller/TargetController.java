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
                () -> openPdf(target.getProblemLink()),
                () -> checkProblemStatus(target),
                () -> deleteTarget(target)
            );
            
            return card;
        } catch (IOException e) {
            e.printStackTrace();
            return new HBox();
        }
    }

    private void openPdf(String url) {
        if (url == null || url.isEmpty()) return;

        String pdfUrl = url;
        // Codeforces logic: contest/123/problem/A -> contest/123/problems.pdf
        if (url.contains("codeforces.com/contest/")) {
            String[] parts = url.split("/");
            for (int i = 0; i < parts.length; i++) {
                if (parts[i].equals("contest") && i + 1 < parts.length) {
                    pdfUrl = "https://codeforces.com/contest/" + parts[i+1] + "/problems.pdf";
                    break;
                }
            }
        } else if (url.contains("codeforces.com/problemset/problem/")) {
            // Problemset logic: problemset/problem/123/A -> contest/123/problems.pdf
             String[] parts = url.split("/");
             for (int i = 0; i < parts.length; i++) {
                if (parts[i].equals("problem") && i + 1 < parts.length) {
                    pdfUrl = "https://codeforces.com/contest/" + parts[i+1] + "/problems.pdf";
                    break;
                }
            }
        }

        openLink(pdfUrl);
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
                        double ratingChange = TargetDAO.updateTargetStatusWithRating(target.getId(), "achieved");
                        String ratingMsg = ratingChange >= 0 ? 
                            String.format(" Rating +%.2f", ratingChange) : 
                            String.format(" Rating %.2f (late penalty)", ratingChange);
                        showAlert(Alert.AlertType.INFORMATION, "Success!", 
                            "Problem accepted! Target achieved! 🎉" + ratingMsg);
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

    @FXML
    private void fetchRatings() {
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Fetch Ratings");
        confirmAlert.setHeaderText("Fetch problem ratings from Codeforces?");
        confirmAlert.setContentText("This will update ratings for all Codeforces problems. Continue?");
        
        Optional<ButtonType> result = confirmAlert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            new Thread(() -> {
                List<Target> targets = TargetDAO.getAllTargets();
                int updated = 0;
                int total = 0;
                
                for (Target target : targets) {
                    if ("problem".equals(target.getType()) && target.getProblemLink() != null 
                        && target.getProblemLink().contains("codeforces.com")) {
                        total++;
                        Integer rating = codeforcesService.fetchProblemRatingFromUrl(target.getProblemLink());
                        if (rating != null && rating > 0) {
                            if (TargetDAO.updateTargetRating(target.getId(), rating)) {
                                updated++;
                            }
                        }
                        // Small delay to avoid rate limiting
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }
                }
                
                final int finalUpdated = updated;
                final int finalTotal = total;
                Platform.runLater(() -> {
                    refreshTargetList();
                    showAlert(Alert.AlertType.INFORMATION, "Success", 
                        String.format("Updated %d/%d Codeforces problems with ratings", finalUpdated, finalTotal));
                });
            }).start();
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