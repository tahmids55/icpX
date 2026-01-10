package com.icpx.view;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

public class PersonalRatingChart extends VBox {

    private static final double MAX_RATING = 10.0;
    // Tiers: Name, MaxValue, Color, TextColor
    private static final Tier[] TIERS = {
        new Tier("Newbie", 2.0, Color.web("#f0f0f06e"), Color.GRAY),
        new Tier("Pupil", 4.0, Color.web("#008f00ce"), Color.GREEN),
        new Tier("Specialist", 6.0, Color.web("#17afafce"), Color.web("#03a89e")),
        new Tier("Expert", 7.0, Color.web("#4545eeff"), Color.BLUE),
        new Tier("Candidate Master", 8.0, Color.web("#ba1bcfff"), Color.PURPLE),
        new Tier("Master", 8.5, Color.web("#f8c324ff"), Color.ORANGE),
        new Tier("Grand Master", 9.0, Color.web("#e70c0cff"), Color.RED),
        new Tier("King", 9.5, Color.web("#cf5a23ff"), Color.GOLD),
        new Tier("Eternity", 10.0, Color.web("#753112ff"), Color.MAGENTA)
    };

    private Pane chartPane;
    private Rectangle fillBar;
    private VBox floatingBox;
    private Label tierLabel;
    private Label valueLabel;
    
    // Config
    private double chartWidth = 600;
    private double chartHeight = 60; // Bar height
    private double topMargin = 100;   // Increased space for labels above bar

    public PersonalRatingChart() {
        this.setPadding(new Insets(20));
        this.setSpacing(10);
        this.setStyle("-fx-background-color: white; -fx-background-radius: 15; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 0);");
        this.setAlignment(Pos.CENTER_LEFT);

        // Title
        Label title = new Label("Personal Rating (0-10)");
        title.setFont(Font.font("Verdana", FontWeight.BOLD, 18));
        this.getChildren().add(title);

        // Chart Pane
        chartPane = new Pane();
        chartPane.setPrefSize(chartWidth, chartHeight + 30 + topMargin); 
        
        // Initial draw
        drawChart(chartWidth);
        
        this.getChildren().add(chartPane);
        
        // Make it responsive to width changes
        this.widthProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal.doubleValue() > 0) {
                updateWidth(newVal.doubleValue() - 40); // Minus padding
            }
        });
    }

    private void updateWidth(double width) {
        drawChart(width);
    }
    
    private double currentRating = 0.0;

    private void drawChart(double width) {
        chartPane.getChildren().clear();
        chartWidth = width;
        chartPane.setPrefWidth(width);
        
        double pixelPerUnit = width / MAX_RATING;
        double currentX = 0;

        // Tiers
        for (int i = 0; i < TIERS.length; i++) {
            Tier tier = TIERS[i];
            double prevMax = (i == 0) ? 0 : TIERS[i-1].max;
            double widthUnits = tier.max - prevMax;
            double widthPx = widthUnits * pixelPerUnit;
            
            Rectangle tierRect = new Rectangle(currentX, topMargin, widthPx, chartHeight);
            tierRect.setFill(tier.color);
            tierRect.setStroke(Color.TRANSPARENT);
            chartPane.getChildren().add(tierRect);
            
            // Add Label inside Tier
            // Logic for abbreviations
            String labelText = tier.name;
            if (widthPx < 80) {
                if (tier.name.equals("Candidate Master")) labelText = "CM";
                else if (tier.name.equals("Grand Master")) labelText = "GM";
                else if (tier.name.equals("Eternity")) labelText = "Eternity"; // Eternity is fine if space allows, but it's small? 0.5 units
                // 0.5 units of 600 width / 10 = 30px. "Eternity" won't fit.
                // Abbreviate short ones? 
                if (widthPx < 40 && labelText.length() > 3) labelText = labelText.substring(0, 1) + "."; // e.g. "M."
            }
            
            // Show even if somewhat small, as long as it's readable (>25px)
            if (widthPx > 25) { 
                Text tLabel = new Text(labelText);
                tLabel.setFont(Font.font("Arial", FontWeight.BOLD, 10)); // Bold for visibility
                tLabel.setFill(Color.BLACK); // Contrast
                
                // Position: Vertical Center of bar (topMargin + chartHeight/2)
                tLabel.setX(currentX + 5); 
                tLabel.setY(topMargin + chartHeight / 2 + 4); 
                
                chartPane.getChildren().add(tLabel);
            }
            
            currentX += widthPx;
        }

        // Fill Bar
        double fillWidth = currentRating * pixelPerUnit;
        fillBar = new Rectangle(0, topMargin, fillWidth, chartHeight);
        fillBar.setFill(Color.web("#00a896"));
        chartPane.getChildren().add(fillBar);
        
        // Axis
        Line axisLine = new Line(0, chartHeight + topMargin, width, chartHeight + topMargin);
        axisLine.setStrokeWidth(2);
        chartPane.getChildren().add(axisLine);

        // Ticks
        for (int i = 0; i <= 10; i++) {
            double x = i * pixelPerUnit;
            Line tick = new Line(x, chartHeight + topMargin, x, chartHeight + topMargin + 5);
            chartPane.getChildren().add(tick);
            
            Text tickLabel = new Text(String.valueOf(i));
            tickLabel.setFont(Font.font("Arial", FontWeight.BOLD, 12));
            tickLabel.setX(x - 5);
            tickLabel.setY(chartHeight + topMargin + 20);
            chartPane.getChildren().add(tickLabel);
        }
        
        // Floating Box (Above the chart)
        floatingBox = new VBox(-2);
        floatingBox.setAlignment(Pos.CENTER);
        floatingBox.setPrefWidth(200);
        
        String tierName = "Newbie";
        for (Tier t : TIERS) {
            if (currentRating <= t.max) {
                tierName = t.name;
                break;
            }
        }
        
        tierLabel = new Label(tierName);
        tierLabel.setFont(Font.font("Verdana", FontWeight.BOLD, 14));
        tierLabel.setTextFill(Color.BLACK); // Always black since it's outside
        
        valueLabel = new Label(String.format("%.1f", currentRating));
        valueLabel.setFont(Font.font("Verdana", FontWeight.BOLD, 18));
        valueLabel.setTextFill(Color.BLACK);
        
        floatingBox.getChildren().addAll(tierLabel, valueLabel);
        
        // Position:
        // Centered on the end of the fill bar
        double boxX = fillWidth - 100; // 100 is half of 200 pref width
        if (boxX < 0) boxX = 0;
        if (boxX > width - 200) boxX = width - 200;
        
        floatingBox.setLayoutX(boxX);
        floatingBox.setLayoutY(10); // Absolute top
        
        chartPane.getChildren().add(floatingBox);
    }

    public void setRating(double rating) {
        if (rating > MAX_RATING) rating = MAX_RATING;
        if (rating < 0) rating = 0;
        this.currentRating = rating;
        
        drawChart(chartWidth > 0 ? chartWidth : 600);
    }

    private static class Tier {
        String name;
        double max;
        Color color;
        Color textColor;

        Tier(String name, double max, Color color, Color textColor) {
            this.name = name;
            this.max = max;
            this.color = color;
            this.textColor = textColor;
        }
    }
}
