package com.icpx.view;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

/**
 * Custom component for Personal Rating Bar (Simplified)
 */
public class PersonalRatingBarView extends VBox {

    private static final double MAX_RATING = 10.0;
    private static final double BAR_WIDTH = 300.0;
    private static final double BAR_HEIGHT = 20.0;

    private Rectangle progressBar;
    private Label ratingLabel;

    public PersonalRatingBarView() {
        setSpacing(5);
        
        // Label on top
        ratingLabel = new Label("Personal Rating: 0.0/10");
        ratingLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #333;");
        
        // Bar container (background)
        StackPane barContainer = new StackPane();
        barContainer.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        
        Rectangle background = new Rectangle(BAR_WIDTH, BAR_HEIGHT);
        background.setFill(Color.web("#e0e0e0")); // Light gray
        background.setArcWidth(10);
        background.setArcHeight(10);
        
        // Progress bar (foreground)
        progressBar = new Rectangle(0, BAR_HEIGHT); // Start with 0 width
        progressBar.setFill(Color.web("#2563eb")); // Default blue
        progressBar.setArcWidth(10);
        progressBar.setArcHeight(10);
        
        barContainer.getChildren().addAll(background, progressBar);
        
        getChildren().addAll(ratingLabel, barContainer);
    }

    public void setRating(double rating) {
        // Clamp rating between 0 and 10
        final double finalRating = Math.max(0, Math.min(rating, MAX_RATING));
        
        ratingLabel.setText(String.format("Personal Rating: %.1f/10", finalRating));
        
        // Update color based on rating
        Color barColor;
        if (finalRating < 4.0) barColor = Color.web("#ef4444");      // Red
        else if (finalRating < 7.0) barColor = Color.web("#f59e0b"); // Orange/Yellow
        else barColor = Color.web("#10b981");                        // Green
        
        progressBar.setFill(barColor);
        
        // Animate width
        double targetWidth = (finalRating / MAX_RATING) * BAR_WIDTH;
        
        Timeline timeline = new Timeline();
        timeline.getKeyFrames().add(
            new KeyFrame(Duration.millis(1000), 
                new KeyValue(progressBar.widthProperty(), targetWidth))
        );
        timeline.play();
    }
}
