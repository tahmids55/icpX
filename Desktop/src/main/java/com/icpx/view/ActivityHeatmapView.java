package com.icpx.view;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.time.temporal.WeekFields;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Custom component to display activity heatmap (GitHub-style) with Year Selection
 */
public class ActivityHeatmapView extends VBox {

    private static final double CELL_SIZE = 16.0; // Increased size
    private static final double GAP = 3.0;

    // Colors for different activity levels (0, 1-2, 3-5, 6+)
    private static final Color[] COLORS = {
        Color.web("#bac9e0a2"), // Level 0 (None)
        Color.web("#9be9a8"), // Level 1
        Color.web("#40c463"), // Level 2
        Color.web("#30a14e"), // Level 3
        Color.web("#216e39")  // Level 4
    };

    private ComboBox<Integer> yearSelector;
    private GridPane heatmapGrid;
    private Map<LocalDate, Integer> activityData = new HashMap<>();
    private int currentYear;

    public ActivityHeatmapView() {
        this.setSpacing(10);
        this.setPadding(new Insets(10));
        this.setAlignment(Pos.TOP_LEFT); // Align content to top-left

        // Header with Year Selector
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        
        Label titleLabel = new Label("Activity");
        titleLabel.setFont(Font.font("Arial", 14));
        
        yearSelector = new ComboBox<>();
        yearSelector.setOnAction(e -> {
            if (yearSelector.getValue() != null) {
                drawHeatmap(yearSelector.getValue());
            }
        });
        
        header.getChildren().addAll(titleLabel, yearSelector);
        this.getChildren().add(header);

        // Grid
        heatmapGrid = new GridPane();
        heatmapGrid.setHgap(GAP);
        heatmapGrid.setVgap(GAP);
        this.getChildren().add(heatmapGrid);
        
        this.currentYear = LocalDate.now().getYear();
    }

    public void setData(Map<LocalDate, Integer> data) {
        this.activityData = data;
        
        // Populate Years
        Set<Integer> years = new HashSet<>();
        years.add(LocalDate.now().getYear());
        for (LocalDate date : data.keySet()) {
            years.add(date.getYear());
        }
        
        List<Integer> sortedYears = new ArrayList<>(years);
        Collections.sort(sortedYears, Collections.reverseOrder());
        
        yearSelector.getItems().setAll(sortedYears);
        
        // Select current year or first available
        if (sortedYears.contains(currentYear)) {
            yearSelector.getSelectionModel().select(Integer.valueOf(currentYear));
        } else if (!sortedYears.isEmpty()) {
            yearSelector.getSelectionModel().select(0);
        }
        
        // Trigger draw
        if (yearSelector.getValue() != null) {
            drawHeatmap(yearSelector.getValue());
        }
    }

    private void drawHeatmap(int year) {
        heatmapGrid.getChildren().clear();
        
        LocalDate firstDay = LocalDate.of(year, 1, 1);
        LocalDate lastDay = LocalDate.of(year, 12, 31);
        
        // Month Labels Row
        // We'll place month labels roughly at the start column of each month
        
        // Day Labels Column (Mon, Wed, Fri) - Optional, user didn't explicitly ask but it's standard
        // Adding simple letters M, W, F at columns -1? 
        // Let's stick to Month names on top as requested.
        
        int currentColumn = 0;
        int currentRow = 0;
        
        // Align Jan 1st to correct day of week
        // Week starts on Sunday (row 0) or Monday? GitHub uses Sunday=0 usually.
        // Let's use Sunday = 0
        
        int startDayOfWeek = firstDay.getDayOfWeek().getValue(); // 1=Mon, 7=Sun
        if (startDayOfWeek == 7) startDayOfWeek = 0; // Make Sunday 0
        
        // Pre-fill labels for Months
        Month currentMonth = null;
        
        // Iterate through all days of the year
        LocalDate iterDate = firstDay;
        // Adjust start column based on first day offset?
        // Usually, the first column is partial.
        
        // Calculate column for Jan 1
        // If Jan 1 is Wed (3), then it's in column 0, row 3.
        
        while (!iterDate.isAfter(lastDay)) {
            int dayOfWeek = iterDate.getDayOfWeek().getValue(); // 1-7
            if (dayOfWeek == 7) dayOfWeek = 0; // Sunday = 0
            
            // Calculate column index (weeks)
            // Codeforces/GitHub usually groups by week.
            // DayOfYear / 7 approx?
            // Correct way: Maintain a 'weekIndex'. Increment when day wraps from Sat to Sun?
            // Actually, if we fill column by column (Sun->Sat), we just increment column after Sat.
            
            // But we iterate by day.
            
            // Check Month change for Label
            if (iterDate.getMonth() != currentMonth) {
                currentMonth = iterDate.getMonth();
                // Add Month Label at currentColumn
                // Only if we have space (e.g. not immediately after previous label) 
                // Simple logic: Add label at currentColumn index
                Label monthLabel = new Label(currentMonth.name().substring(0, 3));
                monthLabel.setFont(Font.font("Arial", 10));
                heatmapGrid.add(monthLabel, currentColumn, 0); // Row 0 is for labels
            }
            
            int count = activityData.getOrDefault(iterDate, 0);
            Rectangle rect = createCell(count, iterDate);
            
            // Grid Row: 1 + dayOfWeek (0-6) -> 1 to 7
            // Grid Col: currentColumn
            
            heatmapGrid.add(rect, currentColumn, dayOfWeek + 1);
            
            if (dayOfWeek == 6) { // Saturday, next day is Sunday (new column)
                currentColumn++;
            }
            
            iterDate = iterDate.plusDays(1);
        }
    }

    private Rectangle createCell(int count, LocalDate date) {
        Rectangle rect = new Rectangle(CELL_SIZE, CELL_SIZE);
        rect.setArcWidth(3);
        rect.setArcHeight(3);
        
        // Determine color based on count
        int colorIndex = 0;
        if (count > 0) {
            if (count <= 2) colorIndex = 1;
            else if (count <= 4) colorIndex = 2;
            else if (count <= 6) colorIndex = 3;
            else colorIndex = 4;
        }
        rect.setFill(COLORS[colorIndex]);
        
        // Add tooltip
        String tooltipText = String.format("%d problems on %s", 
            count, date.format(DateTimeFormatter.ofPattern("MMM dd, yyyy")));
        Tooltip.install(rect, new Tooltip(tooltipText));
        
        return rect;
    }
}
