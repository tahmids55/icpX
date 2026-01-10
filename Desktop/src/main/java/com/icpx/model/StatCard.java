package com.icpx.model;

/**
 * Model class for statistics cards on the dashboard
 */
public class StatCard {
    private String title;
    private String value;
    private String icon;
    private String color;

    public StatCard(String title, String value, String icon, String color) {
        this.title = title;
        this.value = value;
        this.icon = icon;
        this.color = color;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }
}
