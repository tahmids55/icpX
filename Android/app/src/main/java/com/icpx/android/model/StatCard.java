package com.icpx.android.model;

/**
 * Model class for statistics data
 */
public class StatCard {
    private String title;
    private String value;
    private String icon;
    private int colorResId;

    public StatCard(String title, String value, String icon, int colorResId) {
        this.title = title;
        this.value = value;
        this.icon = icon;
        this.colorResId = colorResId;
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

    public int getColorResId() {
        return colorResId;
    }

    public void setColorResId(int colorResId) {
        this.colorResId = colorResId;
    }
}
