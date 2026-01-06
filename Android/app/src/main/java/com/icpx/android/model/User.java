package com.icpx.android.model;

import java.util.Date;

/**
 * Model class representing a User
 */
public class User {
    private int id;
    private String username;
    private String passwordHash;
    private String codeforcesHandle;
    private boolean startupPasswordEnabled;
    private Date createdAt;

    public User() {
        this.startupPasswordEnabled = true;
        this.createdAt = new Date();
    }

    public User(String username, String passwordHash, String codeforcesHandle) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.codeforcesHandle = codeforcesHandle;
        this.startupPasswordEnabled = true;
        this.createdAt = new Date();
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

    public String getCodeforcesHandle() {
        return codeforcesHandle;
    }

    public void setCodeforcesHandle(String codeforcesHandle) {
        this.codeforcesHandle = codeforcesHandle;
    }

    public boolean isStartupPasswordEnabled() {
        return startupPasswordEnabled;
    }

    public void setStartupPasswordEnabled(boolean startupPasswordEnabled) {
        this.startupPasswordEnabled = startupPasswordEnabled;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }
}
