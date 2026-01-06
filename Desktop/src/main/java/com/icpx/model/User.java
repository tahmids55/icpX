package com.icpx.model;

import java.time.LocalDateTime;

/**
 * User entity representing a user in the system
 */
public class User {
    private int id;
    private String username;
    private String passwordHash;
    private boolean startupPasswordEnabled;
    private LocalDateTime createdAt;

    public User() {
    }

    public User(int id, String username, String passwordHash, boolean startupPasswordEnabled, LocalDateTime createdAt) {
        this.id = id;
        this.username = username;
        this.passwordHash = passwordHash;
        this.startupPasswordEnabled = startupPasswordEnabled;
        this.createdAt = createdAt;
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public boolean isStartupPasswordEnabled() {
        return startupPasswordEnabled;
    }

    public void setStartupPasswordEnabled(boolean startupPasswordEnabled) {
        this.startupPasswordEnabled = startupPasswordEnabled;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", startupPasswordEnabled=" + startupPasswordEnabled +
                ", createdAt=" + createdAt +
                '}';
    }
}
