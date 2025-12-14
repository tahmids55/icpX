package com.icpx.controller;

import com.icpx.database.UserDAO;
import com.icpx.model.User;
import com.icpx.util.SceneManager;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;

import java.io.IOException;

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
        // Total Solved Problems Card
        VBox solvedCard = createStatCard("Total Problems Solved", "0", "Solved", e -> {
            System.out.println("Navigate to solved problems");
        });
        
        // Topics Learned Card
        VBox topicsCard = createStatCard("Topics Learned", "0", "Learned", e -> {
            System.out.println("Navigate to learned topics");
        });
        
        statsContainer.getChildren().addAll(solvedCard, topicsCard);
    }

    private VBox createStatCard(String title, String value, String buttonText, javafx.event.EventHandler<javafx.event.ActionEvent> action) {
        VBox card = new VBox(12);
        card.getStyleClass().add("card");
        card.setPadding(new Insets(20));
        card.setPrefWidth(240);
        card.setAlignment(Pos.CENTER);
        
        Text titleText = new Text(title);
        titleText.getStyleClass().add("subtitle");
        titleText.setWrappingWidth(220);
        titleText.setStyle("-fx-text-alignment: center;");
        
        Text valueText = new Text(value);
        valueText.getStyleClass().add("title");
        valueText.setStyle("-fx-font-size: 48px;");
        
        Button actionButton = new Button(buttonText);
        actionButton.getStyleClass().add("button");
        actionButton.setOnAction(action);
        actionButton.setPrefWidth(180);
        
        card.getChildren().addAll(titleText, valueText, actionButton);
        return card;
    }

    private void createRatingGraph() {
        boolean isDark = SceneManager.getCurrentTheme().equals("dark");
        
        Canvas canvas = new Canvas(800, 250);
        javafx.scene.canvas.GraphicsContext gc = canvas.getGraphicsContext2D();
        
        // Draw axes
        gc.setStroke(isDark ? Color.web("#5a5a5a") : Color.web("#e2e8f0"));
        gc.setLineWidth(2);
        
        // Y-axis
        gc.strokeLine(50, 20, 50, 230);
        // X-axis
        gc.strokeLine(50, 230, 780, 230);
        
        // Sample data
        gc.setStroke(isDark ? Color.web("#4fc3f7") : Color.web("#2563eb"));
        gc.setLineWidth(3);
        
        double[] xPoints = {50, 150, 250, 350, 450, 550, 650, 750};
        double[] yPoints = {200, 180, 160, 140, 120, 100, 90, 80};
        
        for (int i = 0; i < xPoints.length - 1; i++) {
            gc.strokeLine(xPoints[i], yPoints[i], xPoints[i + 1], yPoints[i + 1]);
            gc.setFill(isDark ? Color.web("#4fc3f7") : Color.web("#2563eb"));
            gc.fillOval(xPoints[i] - 4, yPoints[i] - 4, 8, 8);
        }
        gc.fillOval(xPoints[xPoints.length - 1] - 4, yPoints[yPoints.length - 1] - 4, 8, 8);
        
        // Draw grid lines
        gc.setStroke(isDark ? Color.web("#3e3e42") : Color.web("#f1f5f9"));
        gc.setLineWidth(1);
        for (int i = 1; i < 6; i++) {
            double y = 30 + i * 40;
            gc.strokeLine(50, y, 780, y);
        }
        
        // Add labels
        gc.setFill(isDark ? Color.web("#9d9d9d") : Color.web("#64748b"));
        gc.fillText("Rating: 0 → 1500", 60, 250);
        
        Text noDataText = new Text("Start solving problems to see your rating progress!");
        noDataText.getStyleClass().add("subtitle");
        noDataText.setStyle("-fx-font-style: italic;");
        
        ratingGraphContainer.getChildren().addAll(canvas, noDataText);
    }

    private void createHeatmap() {
        boolean isDark = SceneManager.getCurrentTheme().equals("dark");
        
        GridPane grid = new GridPane();
        grid.setHgap(4);
        grid.setVgap(4);
        
        String[] days = {"Mon", "Wed", "Fri"};
        int[] dayIndices = {1, 3, 5};
        
        for (int i = 0; i < days.length; i++) {
            Text dayLabel = new Text(days[i]);
            dayLabel.getStyleClass().add("subtitle");
            dayLabel.setStyle("-fx-font-size: 10px;");
            grid.add(dayLabel, 0, dayIndices[i]);
        }
        
        for (int week = 0; week < 52; week++) {
            for (int day = 0; day < 7; day++) {
                Rectangle cell = new Rectangle(12, 12);
                int activity = (int) (Math.random() * 5);
                
                String color;
                if (activity == 0) {
                    color = isDark ? "#1e1e1e" : "#f1f5f9";
                } else if (activity == 1) {
                    color = isDark ? "#0e4429" : "#9be9a8";
                } else if (activity == 2) {
                    color = isDark ? "#006d32" : "#40c463";
                } else if (activity == 3) {
                    color = isDark ? "#26a641" : "#30a14e";
                } else {
                    color = isDark ? "#39d353" : "#216e39";
                }
                
                cell.setFill(Color.web(color));
                cell.setStroke(isDark ? Color.web("#2d2d30") : Color.web("#e2e8f0"));
                
                grid.add(cell, week + 1, day);
            }
        }
        
        HBox legend = new HBox(8);
        legend.setAlignment(Pos.CENTER_LEFT);
        legend.setPadding(new Insets(10, 0, 0, 0));
        
        Text lessText = new Text("Less");
        lessText.getStyleClass().add("subtitle");
        lessText.setStyle("-fx-font-size: 11px;");
        
        HBox legendBoxes = new HBox(4);
        String[] legendColors = isDark 
            ? new String[]{"#1e1e1e", "#0e4429", "#006d32", "#26a641", "#39d353"}
            : new String[]{"#f1f5f9", "#9be9a8", "#40c463", "#30a14e", "#216e39"};
            
        for (String color : legendColors) {
            Rectangle legendCell = new Rectangle(12, 12);
            legendCell.setFill(Color.web(color));
            legendCell.setStroke(isDark ? Color.web("#2d2d30") : Color.web("#e2e8f0"));
            legendBoxes.getChildren().add(legendCell);
        }
        
        Text moreText = new Text("More");
        moreText.getStyleClass().add("subtitle");
        moreText.setStyle("-fx-font-size: 11px;");
        
        legend.getChildren().addAll(lessText, legendBoxes, moreText);
        
        Text noDataText = new Text("No activity data yet. Start solving to fill the heatmap!");
        noDataText.getStyleClass().add("subtitle");
        noDataText.setStyle("-fx-font-style: italic;");
        
        heatmapContainer.getChildren().addAll(grid, legend, new VBox(5), noDataText);
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
}