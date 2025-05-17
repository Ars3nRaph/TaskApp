package com.example.taskapp.models;

public class User {
    private int id;
    private String username;

    // Constructeur
    public User(int id, String username) {
        this.id = id;
        this.username = username;
    }

    // Getters et Setters
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
}
