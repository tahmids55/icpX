package com.icpx.controller;

import com.icpx.database.SettingsDAO;
import com.icpx.database.TargetDAO;
import com.icpx.database.UserDAO;
import com.icpx.model.Target;
import com.icpx.model.User;
import com.icpx.service.CodeforcesService;
import com.icpx.util.SceneManager;
import com.icpx.view.HistoryView;
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
    private HBox statsContainer;

    @FXML
    private VBox ratingGraphContainer;

    @FXML
    private VBox heatmapContainer;

    @FXML
    public void initialize() {
        try {
            User user = UserDAO.getCurrentUser();
            welcomeText.setText("Welcome, " + (user != null ? user.getUsername() : "User") + "!");

            createStats();
            createRatingGraph();
            createHeatmap();

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void createStats() {
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
        
        statsContainer.getChildren().addAll(solvedCard, topicsCard);
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

    private void createRatingGraph() {
        // A canvas to draw the rating-over-time graph
        Canvas canvas = new Canvas(840, 280);
        javafx.scene.canvas.GraphicsContext gc = canvas.getGraphicsContext2D();

        // Axis positioning within the canvas
        double axisLeft = 100;
        double axisRight = 720;
        double axisTop = 40;
        double axisBottom = 240;
        double axisHeight = axisBottom - axisTop;
        double axisWidth = axisRight - axisLeft;

        // Rating range we display on the vertical axis
        double minRating = 0;
        double maxRating = 600;

        // Define visual tiers (bands) for rating ranges with colors and labels
        RatingTier[] tiers = {
                new RatingTier(0, 100, "Newbie", "#808080"),
                new RatingTier(100, 200, "Pupil", "#008000"),
                new RatingTier(200, 300, "Specialist", "#03a89e"),
                new RatingTier(300, 400, "Expert", "#0000ff"),
                new RatingTier(400, 500, "CandidateMaster", "#aa00ff"),
                new RatingTier(500, 550, "Master", "#ff8c00"),
                new RatingTier(550, 600, "Grandmaster", "#ff0000")
        };

        // Draw background bands for each rating tier
        for (RatingTier tier : tiers) {
            double bandStart = Math.max(tier.start, minRating);
            double bandEnd = Math.min(tier.end, maxRating);
            if (bandEnd <= bandStart) {
                // Skip tiers outside of range
                continue;
            }

            // Convert rating values to y coordinates on the canvas
            double yStart = axisBottom - ((bandStart - minRating) / (maxRating - minRating)) * axisHeight;
            double yEnd = axisBottom - ((bandEnd - minRating) / (maxRating - minRating)) * axisHeight;
            double bandHeight = yStart - yEnd;

            // Fill a semi-transparent colored rectangle for the tier band
            gc.setFill(javafx.scene.paint.Color.web(tier.colorHex, 0.2));
            gc.fillRect(axisLeft, yEnd, axisWidth, bandHeight);

            // Draw the tier label to the right of the axis band
            gc.setFill(javafx.scene.paint.Color.web(tier.colorHex));
            gc.fillText(tier.label, axisRight + 20, yEnd + (bandHeight / 2) + 4);
        }

        // Y-axis tick values to show on the left
        double[] yTicks = {0, 100, 200, 300, 400, 450, 500, 550, 600};

        // Example X axis labels (months) and example data (ratings) for the plot
        String[] months = {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug"};
        double[] ratings = {40, 120, 180, 260, 340, 430, 510, 580};

        // Draw axes lines
        gc.setLineWidth(2);
        gc.setStroke(Color.web("#e2e8f0"));
        gc.strokeLine(axisLeft, axisTop, axisLeft, axisBottom);   // Y axis
        gc.strokeLine(axisLeft, axisBottom, axisRight, axisBottom); // X axis

        // Draw horizontal grid lines for each Y tick
        gc.setStroke(Color.web("#f1f5f9"));
        gc.setLineWidth(1);
        for (double tick : yTicks) {
            double y = axisBottom - ((tick - minRating) / (maxRating - minRating)) * axisHeight;
            gc.strokeLine(axisLeft, y, axisRight, y);
        }

        // Draw vertical grid lines for each month tick
        double xStep = axisWidth / (months.length - 1);
        for (int i = 0; i < months.length; i++) {
            double x = axisLeft + i * xStep;
            gc.strokeLine(x, axisBottom, x, axisTop);
        }

        // Draw Y-axis labels (ratings)
        gc.setFill(Color.web("#1f2937"));
        for (double tick : yTicks) {
            double y = axisBottom - ((tick - minRating) / (maxRating - minRating)) * axisHeight;
            gc.fillText(String.valueOf((int) tick), axisLeft - 50, y + 4);
        }

        // Draw X-axis labels (months)
        gc.setFill(Color.web("#1f2937"));
        for (int i = 0; i < months.length; i++) {
            double x = axisLeft + i * xStep;
            gc.fillText(months[i], x - 12, axisBottom + 20);
        }

        // Draw the line representing rating progression
        gc.setStroke(Color.web("#2563eb"));
        gc.setLineWidth(3);
        for (int i = 0; i < ratings.length - 1; i++) {
            double x1 = axisLeft + i * xStep;
            double y1 = axisBottom - ((ratings[i] - minRating) / (maxRating - minRating)) * axisHeight;
            double x2 = axisLeft + (i + 1) * xStep;
            double y2 = axisBottom - ((ratings[i + 1] - minRating) / (maxRating - minRating)) * axisHeight;
            gc.strokeLine(x1, y1, x2, y2);
        }

        // Draw markers (dots) at each data point
        gc.setFill(Color.web("#2563eb"));
        for (int i = 0; i < ratings.length; i++) {
            double x = axisLeft + i * xStep;
            double y = axisBottom - ((ratings[i] - minRating) / (maxRating - minRating)) * axisHeight;
            gc.fillOval(x - 4, y - 4, 8, 8);
        }

        // Axis titles
        gc.setFill(Color.web("#1f2937"));
        gc.fillText("Rating", axisLeft - 60, axisTop - 10);
        gc.fillText("Months", (axisLeft + axisRight) / 2 - 30, axisBottom + 40);

        // If there is no real data, show an informative text below the canvas
        Text noDataText = new Text("Start solving problems to see your rating progress!");
        noDataText.getStyleClass().add("subtitle");
        noDataText.setStyle("-fx-font-style: italic;");

        // Add the canvas and the hint text to the container in the UI
        ratingGraphContainer.getChildren().addAll(canvas, noDataText);
    }

    private void createHeatmap() {
        boolean isDark = false;
        
        // Create loading text
        Text loadingText = new Text("Loading activity data...");
        loadingText.getStyleClass().add("subtitle");
        loadingText.setStyle("-fx-font-style: italic;");
        heatmapContainer.getChildren().add(loadingText);
        
        // Fetch data in background
        new Thread(() -> {
            try {
                String handle = SettingsDAO.getCodeforcesHandle();
                Map<java.time.LocalDate, Integer> activityMap;
                
                if (handle == null || handle.isEmpty()) {
                    // No handle set, show empty heatmap
                    activityMap = new java.util.HashMap<>();
                } else {
                    // Fetch real data from Codeforces
                    CodeforcesService service = new CodeforcesService();
                    activityMap = service.fetchSubmissionActivity(handle);
                }
                
                // Update UI on JavaFX thread
                final Map<java.time.LocalDate, Integer> finalActivityMap = activityMap;
                Platform.runLater(() -> {
                    heatmapContainer.getChildren().clear();
                    renderHeatmap(finalActivityMap, isDark);
                });
            } catch (java.net.SocketException e) {
                Platform.runLater(() -> {
                    heatmapContainer.getChildren().clear();
                    Text errorText = new Text("Network unavailable. Heatmap requires internet connection.");
                    errorText.getStyleClass().add("subtitle");
                    errorText.setStyle("-fx-fill: #ef4444; -fx-font-style: italic;");
                    
                    // Show empty heatmap instead of just error
                    renderHeatmap(new java.util.HashMap<>(), isDark);
                    heatmapContainer.getChildren().add(0, errorText);
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    heatmapContainer.getChildren().clear();
                    VBox errorBox = new VBox(10);
                    
                    Text errorText = new Text("Failed to load activity data");
                    errorText.getStyleClass().add("subtitle");
                    errorText.setStyle("-fx-fill: #ef4444; -fx-font-weight: bold;");
                    
                    Text detailText = new Text(e.getMessage() != null ? e.getMessage() : "Unknown error");
                    detailText.getStyleClass().add("subtitle");
                    detailText.setStyle("-fx-font-size: 11px; -fx-font-style: italic;");
                    
                    errorBox.getChildren().addAll(errorText, detailText);
                    
                    // Show empty heatmap
                    renderHeatmap(new java.util.HashMap<>(), isDark);
                    heatmapContainer.getChildren().add(0, errorBox);
                });
                e.printStackTrace();
            }
        }).start();
    }
    
    private void renderHeatmap(Map<java.time.LocalDate, Integer> activityMap, boolean isDark) {
        GridPane grid = new GridPane();
        grid.setHgap(3);
        grid.setVgap(3);
        
        // Add month labels at the top
        String[] months = {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
        java.time.LocalDate today = java.time.LocalDate.now();
        java.time.LocalDate startDate = today.minusWeeks(52);
        
        // Find the most recent Monday
        while (startDate.getDayOfWeek() != java.time.DayOfWeek.MONDAY) {
            startDate = startDate.minusDays(1);
        }
        
        // Track which week each month starts
        java.time.LocalDate currentDate = startDate;
        int lastMonth = -1;
        for (int week = 0; week < 52; week++) {
            int month = currentDate.getMonthValue();
            if (month != lastMonth && week % 4 == 0) {
                Text monthLabel = new Text(months[month - 1]);
                monthLabel.getStyleClass().add("subtitle");
                monthLabel.setStyle("-fx-font-size: 10px;");
                grid.add(monthLabel, week + 1, 0);
                lastMonth = month;
            }
            currentDate = currentDate.plusWeeks(1);
        }
        
        // Day labels
        String[] days = {"Mon", "", "Wed", "", "Fri", "", ""};
        for (int i = 0; i < days.length; i++) {
            if (!days[i].isEmpty()) {
                Text dayLabel = new Text(days[i]);
                dayLabel.getStyleClass().add("subtitle");
                dayLabel.setStyle("-fx-font-size: 10px;");
                grid.add(dayLabel, 0, i + 1);
            }
        }
        
        // Reset date
        currentDate = startDate;
        
        // Create cells for each day
        for (int week = 0; week < 52; week++) {
            for (int day = 0; day < 7; day++) {
                Rectangle cell = new Rectangle(11, 11);
                
                // Get activity count for this date
                int activity = activityMap.getOrDefault(currentDate, 0);
                
                // Determine color based on activity level (Codeforces style)
                String color;
                if (activity == 0) {
                    color = isDark ? "#1e1e1e" : "#ebedf0";
                } else {
                    // Scale colors based on activity (1-4+ levels)
                    double intensity = Math.min(activity, 4) / 4.0;
                    if (intensity <= 0.25) {
                        color = isDark ? "#0e4429" : "#9be9a8";
                    } else if (intensity <= 0.5) {
                        color = isDark ? "#006d32" : "#40c463";
                    } else if (intensity <= 0.75) {
                        color = isDark ? "#26a641" : "#30a14e";
                    } else {
                        color = isDark ? "#39d353" : "#216e39";
                    }
                }
                
                cell.setFill(Color.web(color));
                cell.setStroke(isDark ? Color.web("#2d2d30") : Color.web("#e5e5e5"));
                cell.setStrokeWidth(0.5);
                cell.setArcWidth(2);
                cell.setArcHeight(2);
                
                // Add tooltip
                final java.time.LocalDate dateForTooltip = currentDate;
                final int activityForTooltip = activity;
                Tooltip tooltip = new Tooltip(
                    activityForTooltip + " submission" + (activityForTooltip != 1 ? "s" : "") + 
                    " on " + dateForTooltip.toString()
                );
                Tooltip.install(cell, tooltip);
                
                grid.add(cell, week + 1, day + 1);
                currentDate = currentDate.plusDays(1);
            }
        }
        
        // Legend
        HBox legend = new HBox(8);
        legend.setAlignment(Pos.CENTER_LEFT);
        legend.setPadding(new Insets(10, 0, 0, 0));
        
        Text lessText = new Text("Less");
        lessText.getStyleClass().add("subtitle");
        lessText.setStyle("-fx-font-size: 11px;");
        
        HBox legendBoxes = new HBox(3);
        String[] legendColors = isDark 
            ? new String[]{"#1e1e1e", "#0e4429", "#006d32", "#26a641", "#39d353"}
            : new String[]{"#ebedf0", "#9be9a8", "#40c463", "#30a14e", "#216e39"};
            
        for (String color : legendColors) {
            Rectangle legendCell = new Rectangle(11, 11);
            legendCell.setFill(Color.web(color));
            legendCell.setStroke(isDark ? Color.web("#2d2d30") : Color.web("#e5e5e5"));
            legendCell.setStrokeWidth(0.5);
            legendCell.setArcWidth(2);
            legendCell.setArcHeight(2);
            legendBoxes.getChildren().add(legendCell);
        }
        
        Text moreText = new Text("More");
        moreText.getStyleClass().add("subtitle");
        moreText.setStyle("-fx-font-size: 11px;");
        
        legend.getChildren().addAll(lessText, legendBoxes, moreText);
        
        // Show activity summary
        int totalActivity = activityMap.values().stream().mapToInt(Integer::intValue).sum();
        String handle = SettingsDAO.getCodeforcesHandle();
        
        Text summaryText = new Text();
        if (handle == null || handle.isEmpty()) {
            summaryText.setText("Set your Codeforces handle in Settings to see your activity");
        } else if (totalActivity > 0) {
            summaryText.setText(totalActivity + " submission" + (totalActivity != 1 ? "s" : "") + " in the last year");
        } else {
            summaryText.setText("No submissions found. Start solving on Codeforces!");
        }
        summaryText.getStyleClass().add("subtitle");
        summaryText.setStyle("-fx-font-style: italic;");
        
        heatmapContainer.getChildren().addAll(grid, legend, new VBox(5), summaryText);
    }

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

    private static class RatingTier {
        final double start;
        final double end;
        final String label;
        final String colorHex;

        RatingTier(double start, double end, String label, String colorHex) {
            this.start = start;
            this.end = end;
            this.label = label;
            this.colorHex = colorHex;
        }
    }
}