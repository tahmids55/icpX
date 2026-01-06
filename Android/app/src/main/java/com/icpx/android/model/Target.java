package com.icpx.android.model;

import java.util.Date;

/**
 * Model class representing a competitive programming target/problem
 */
public class Target {
    private int id;
    private String type; // "problem" or "topic"
    private String name;
    private String problemLink;
    private String topicName;
    private String websiteUrl;
    private Date createdAt;
    private String status; // "pending", "achieved", "failed"
    private Integer rating; // Problem rating (difficulty)
    private String description; // Additional description for topics or notes
    private boolean deleted; // Soft delete flag

    public Target() {
        this.status = "pending";
        this.createdAt = new Date();
        this.deleted = false;
    }

    public Target(String type, String name) {
        this.type = type;
        this.name = name;
        this.status = "pending";
        this.createdAt = new Date();
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getProblemLink() {
        return problemLink;
    }

    public void setProblemLink(String problemLink) {
        this.problemLink = problemLink;
    }

    public String getTopicName() {
        return topicName;
    }

    public void setTopicName(String topicName) {
        this.topicName = topicName;
    }

    public String getWebsiteUrl() {
        return websiteUrl;
    }

    public void setWebsiteUrl(String websiteUrl) {
        this.websiteUrl = websiteUrl;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getRating() {
        return rating;
    }

    public void setRating(Integer rating) {
        this.rating = rating;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    @Override
    public String toString() {
        return "Target{" +
                "id=" + id +
                ", type='" + type + '\'' +
                ", name='" + name + '\'' +
                ", status='" + status + '\'' +
                ", rating=" + rating +
                '}';
    }
}
