package com.icpx.model;

import java.time.LocalDateTime;

/**
 * Model class representing a competitive programming target
 */
public class Target {
    private int id;
    private String type; // "problem" or "topic"
    private String name;
    private String problemLink;
    private String topicName;
    private String websiteUrl;
    private LocalDateTime createdAt;
    private LocalDateTime deadline; // 24h deadline for rating penalty
    private String status; // "pending", "achieved", "failed"
    private Integer rating; // Problem rating (difficulty)
    private boolean archived; // Soft delete status

    public boolean isArchived() {
        return archived;
    }

    public void setArchived(boolean archived) {
        this.archived = archived;
    }

    public LocalDateTime getDeadline() {
        return deadline;
    }

    public void setDeadline(LocalDateTime deadline) {
        this.deadline = deadline;
    }

    public Target() {
        this.status = "pending";
        this.createdAt = LocalDateTime.now();
        this.deadline = LocalDateTime.now().plusHours(24); // Default 24h deadline
    }

    public Target(String type, String name) {
        this.type = type;
        this.name = name;
        this.status = "pending";
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
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

    @Override
    public String toString() {
        if ("problem".equals(type)) {
            return name + " - " + status;
        } else {
            return topicName + " (Topic) - " + status;
        }
    }
}
