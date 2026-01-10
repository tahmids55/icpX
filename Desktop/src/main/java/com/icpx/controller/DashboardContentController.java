package com.icpx.controller;

import com.icpx.MainApp;

import com.icpx.database.SettingsDAO;
import com.icpx.database.TargetDAO;
import com.icpx.database.UserDAO;
import com.icpx.model.Target;
import com.icpx.model.User;
import com.icpx.service.CodeforcesService;
import com.icpx.util.SceneManager;
import com.icpx.view.ActivityHeatmapView;
import com.icpx.view.HistoryView;
import com.icpx.view.PersonalRatingChart;
import com.icpx.view.TopicHistoryView;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class DashboardContentController {

    @FXML
    private Text welcomeText;

    @FXML
    private VBox recentTargetsContainer;
    
    @FXML
    private HBox dailyStatsContainer;

    @FXML
    private HBox allTimeStatsContainer;

    @FXML
    private VBox ratingGraphContainer;

    @FXML
    private VBox heatmapContainer;
    
    @FXML
    private VBox ratingBarContainer;

    private CodeforcesService codeforcesService = new CodeforcesService();

    @FXML
    public void initialize() {
        try {
            User user = UserDAO.getCurrentUser();
            welcomeText.setText("Welcome, " + (user != null ? user.getUsername() : "User") + "!");

            createUserRatingBar();
            createDailyStats();
            createAllTimeStats();
            createPersonalRatingChart();
            createHeatmap();
            displayRecentTargets();

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    
    private void createUserRatingBar() {
        ratingBarContainer.getChildren().clear();
        
        double rating = SettingsDAO.getUserRating();
        
        // Normalize rating if it's from the old scale (e.g. 1500)
        if (rating > 10.0) {
            rating = 5.0;
            SettingsDAO.setUserRating(rating);
        }
        
        HBox ratingBox = new HBox(15);
        ratingBox.setAlignment(Pos.CENTER_LEFT);
        ratingBox.setStyle("-fx-background-color: #1a1a2e; -fx-background-radius: 10; -fx-padding: 15 25 15 25;");
        
        Text ratingIcon = new Text("🏆");
        ratingIcon.setStyle("-fx-font-size: 24px;");
        
        VBox ratingInfo = new VBox(3);
        Text ratingTitle = new Text("Your icpX Rating");
        ratingTitle.setStyle("-fx-fill: #888; -fx-font-size: 12px;");
        
        Text ratingValue = new Text(String.format("%.1f / 10", rating));
        ratingValue.setStyle("-fx-fill: white; -fx-font-size: 28px; -fx-font-weight: bold;");
        
        ratingInfo.getChildren().addAll(ratingTitle, ratingValue);
        
        // Rating progress explanation
        VBox infoBox = new VBox(3);
        Text infoTitle = new Text("Rating System");
        infoTitle.setStyle("-fx-fill: #888; -fx-font-size: 11px;");
        Text infoText = new Text("✓ Complete on time: +0.02  |  ⚠ Late: -0.01/min");
        infoText.setStyle("-fx-fill: #aaa; -fx-font-size: 11px;");
        infoBox.getChildren().addAll(infoTitle, infoText);
        
        ratingBox.getChildren().addAll(ratingIcon, ratingInfo, infoBox);
        ratingBarContainer.getChildren().add(ratingBox);
    }

    private void displayRecentTargets() {
        recentTargetsContainer.getChildren().clear();
        List<Target> recent = TargetDAO.getRecentTargets(5);
        
        for (Target target : recent) {
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
                
                recentTargetsContainer.getChildren().add(card);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void openPdf(String url) {
        if (url == null || url.isEmpty()) return;
        String pdfUrl = url;
        if (url.contains("codeforces.com/contest/")) {
            String[] parts = url.split("/");
            for (int i = 0; i < parts.length; i++) {
                if (parts[i].equals("contest") && i + 1 < parts.length) {
                    pdfUrl = "https://codeforces.com/contest/" + parts[i+1] + "/problems.pdf";
                    break;
                }
            }
        }
        openLink(pdfUrl);
    }

    private void openLink(String url) {
        if (MainApp.getInstance() != null) {
            MainApp.getInstance().openUrl(url);
        }
    }

    private void checkProblemStatus(Target target) {
        String handle = SettingsDAO.getCodeforcesHandle();
        if (handle == null || handle.isEmpty()) return;

        new Thread(() -> {
            try {
                boolean accepted = codeforcesService.checkProblemAccepted(handle, target.getProblemLink());
                Platform.runLater(() -> {
                    if (accepted) {
                        TargetDAO.updateTargetStatusWithRating(target.getId(), "achieved");
                        displayRecentTargets();
                        createDailyStats();
                        createAllTimeStats();
                    }
                });
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void deleteTarget(Target target) {
        if (TargetDAO.deleteTarget(target.getId())) {
            displayRecentTargets();
            createDailyStats();
            createAllTimeStats();
        }
    }

    private void createDailyStats() {
        int addedToday = TargetDAO.getDailyAddedCount();
        int solvedToday = TargetDAO.getDailySolvedCount();
        
        VBox addedCard = createStatCard("Added Today", String.valueOf(addedToday), "View Targets", e -> {
            // Logic to switch to Targets tab in MainApp?
            // For now just refresh
        });
        
        VBox solvedCard = createStatCard("Solved Today", String.valueOf(solvedToday), "View History", e -> {
            navigateToHistory();
        });
        
        dailyStatsContainer.getChildren().setAll(addedCard, solvedCard);
    }

    private void createAllTimeStats() {
        // Get actual count of achieved problems
        int solvedCount = TargetDAO.getTargetCountByStatus("achieved");
        // Count only topics, not all achieved
        int topicsCount = TargetDAO.getAchievedTopics().size();
        
        // Total Solved Problems Card
        VBox solvedCard = createStatCard("Total Problems Solved", String.valueOf(solvedCount), "View History", e -> {
            navigateToHistory();
        });
        
        // Topics Learned Card
        VBox topicsCard = createStatCard("Topics Learned", String.valueOf(topicsCount), "View History", e -> {
            navigateToTopicHistory();
        });
        
        allTimeStatsContainer.getChildren().setAll(solvedCard, topicsCard);
    }

    private void navigateToHistory() {
        try {
            HistoryView historyView = new HistoryView();
            SceneManager.switchScene(historyView.getScene());
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Failed to navigate to history view: " + e.getMessage());
        }
    }

    private void navigateToTopicHistory() {
        try {
            TopicHistoryView topicHistoryView = new TopicHistoryView();
            SceneManager.switchScene(topicHistoryView.getScene());
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Failed to navigate to topic history view: " + e.getMessage());
        }
    }

    private VBox createStatCard(String title, String value, String buttonText, javafx.event.EventHandler<javafx.event.ActionEvent> action){
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/icpx/view/StatCard.fxml"));
            VBox card = loader.load();
            StatCardController controller = loader.getController();
            controller.setContent(title, value, buttonText, action);
            return card;
        } catch (IOException e) {
            e.printStackTrace();
            return new VBox();
        }
    }



    private void createPersonalRatingChart() {
        PersonalRatingChart ratingView = new PersonalRatingChart();
        
        // Calculate rating based on solved problems
        int solvedCount = TargetDAO.getTargetCountByStatus("achieved");
        double rating = Math.min(10.0, solvedCount / 10.0);
        
        ratingView.setRating(rating);
        ratingGraphContainer.getChildren().setAll(ratingView);
    }

    private void createHeatmap() {
        ActivityHeatmapView heatmapView = new ActivityHeatmapView();
        
        // Add loading indicator
        Text loadingText = new Text("Loading activity...");
        loadingText.setStyle("-fx-font-style: italic; -fx-fill: #666;");
        heatmapContainer.getChildren().setAll(loadingText);

        new Thread(() -> {
            try {
                String handle = SettingsDAO.getCodeforcesHandle();
                if (handle != null && !handle.isEmpty()) {
                    Map<java.time.LocalDate, Integer> activityData = TargetDAO.getActivityHeatmapData();
                    
                    // If local data is empty, try fetching from Codeforces
                    if (activityData.isEmpty()) {
                        CodeforcesService service = new CodeforcesService();
                        try {
                            activityData = service.fetchSubmissionActivity(handle);
                        } catch (IOException e) {
                            System.err.println("Failed to fetch CF activity: " + e.getMessage());
                        }
                    }
                    
                    final Map<java.time.LocalDate, Integer> finalData = activityData;
                    Platform.runLater(() -> {
                        heatmapView.setData(finalData);
                        heatmapContainer.getChildren().setAll(heatmapView);
                    });
                } else {
                    Platform.runLater(() -> {
                        Text placeholder = new Text("Set Codeforces handle in Settings to see activity");
                        placeholder.setStyle("-fx-font-style: italic; -fx-fill: #666;");
                        heatmapContainer.getChildren().setAll(placeholder);
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    Text errorText = new Text("Error loading activity");
                    errorText.setStyle("-fx-fill: red;");
                    heatmapContainer.getChildren().setAll(errorText);
                });
            }
        }).start();
    }
    
    // Remove old renderHeatmap method as it is now in ActivityHeatmapView


    @FXML
    private void goToSettings() {
        ScrollPane scrollPane = (ScrollPane) welcomeText.getScene().lookup(".scroll-pane");
        if (scrollPane != null) {
             try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/icpx/view/SettingsView.fxml"));
                Parent content = loader.load();
                scrollPane.setContent(content);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}
