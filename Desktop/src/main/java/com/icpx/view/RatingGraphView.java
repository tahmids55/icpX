package com.icpx.view;

import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.layout.BorderPane;

import java.util.List;

/**
 * Custom component for displaying rating history graph
 */
public class RatingGraphView extends BorderPane {

    private LineChart<String, Number> lineChart;
    private XYChart.Series<String, Number> series;

    public RatingGraphView() {
        // Axes
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        xAxis.setLabel("Contest");
        yAxis.setLabel("Rating");
        yAxis.setAutoRanging(false);
        yAxis.setLowerBound(0);
        yAxis.setUpperBound(3000);
        yAxis.setTickUnit(200);

        // Chart
        lineChart = new LineChart<>(xAxis, yAxis);
        lineChart.setTitle("Rating History");
        lineChart.setLegendVisible(false);
        lineChart.setCreateSymbols(true); // Show dots on line

        series = new XYChart.Series<>();
        lineChart.getData().add(series);

        setCenter(lineChart);
    }

    public void setData(List<Integer> ratings) {
        series.getData().clear();

        // If no data, clear and return
        if (ratings == null || ratings.isEmpty()) {
            return;
        }

        // Add data points
        // Limit to last 20 contests to avoid clutter
        int start = Math.max(0, ratings.size() - 20);
        for (int i = start; i < ratings.size(); i++) {
            series.getData().add(new XYChart.Data<>(String.valueOf(i + 1), ratings.get(i)));
        }
        
        // Dynamic y-axis scaling
        int min = ratings.stream().min(Integer::compare).orElse(0);
        int max = ratings.stream().max(Integer::compare).orElse(10);
        NumberAxis yAxis = (NumberAxis) lineChart.getYAxis();
        yAxis.setLowerBound(Math.max(0, min - 200));
        yAxis.setUpperBound(max + 200);
    }
}
