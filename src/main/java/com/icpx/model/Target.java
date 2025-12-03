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
    private String status; // "pending", "achieved", "failed"

    public Target() {
        this.status = "pending";
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

    @Override
    public String toString() {
        if ("problem".equals(type)) {
            return name + " - " + status;
        } else {
            return topicName + " (Topic) - " + status;
        }
    }
}
