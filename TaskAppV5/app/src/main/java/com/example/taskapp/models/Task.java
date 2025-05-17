package com.example.taskapp.models;

import java.util.Date;

public class Task {
    private int id;
    private int userId;
    private String username;
    private String groupName;
    private String title;
    private String description;
    private int priority;
    private Date dueDate;
    private boolean completed;
    private Date creationTimestamp;
    private Date lastModifiedTimestamp;

    // Constructeur
    public Task(int id, int userId, String username, String groupName, String title, 
                String description, int priority, Date dueDate, boolean completed,
                Date creationTimestamp, Date lastModifiedTimestamp) {
        this.id = id;
        this.userId = userId;
        this.username = username;
        this.groupName = groupName;
        this.title = title;
        this.description = description;
        this.priority = priority;
        this.dueDate = dueDate;
        this.completed = completed;
        this.creationTimestamp = creationTimestamp;
        this.lastModifiedTimestamp = lastModifiedTimestamp;
    }

    // Constructeur pour nouvelle tâche
    public Task(int userId, String groupName, String title, String description, 
                int priority, Date dueDate) {
        this.userId = userId;
        this.groupName = groupName;
        this.title = title;
        this.description = description;
        this.priority = priority;
        this.dueDate = dueDate;
        this.completed = false;
        this.creationTimestamp = new Date();
        this.lastModifiedTimestamp = new Date();
    }

    // Getters et Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public Date getDueDate() {
        return dueDate;
    }

    public void setDueDate(Date dueDate) {
        this.dueDate = dueDate;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
        this.lastModifiedTimestamp = new Date();
    }

    public Date getCreationTimestamp() {
        return creationTimestamp;
    }

    public void setCreationTimestamp(Date creationTimestamp) {
        this.creationTimestamp = creationTimestamp;
    }

    public Date getLastModifiedTimestamp() {
        return lastModifiedTimestamp;
    }

    public void setLastModifiedTimestamp(Date lastModifiedTimestamp) {
        this.lastModifiedTimestamp = lastModifiedTimestamp;
    }

    // Méthode pour mettre à jour la date de dernière modification
    public void updateLastModified() {
        this.lastModifiedTimestamp = new Date();
    }
}
