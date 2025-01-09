package com.example.chillpoint.views.models;

import java.util.ArrayList;

public class Report {
    private String id;
    private String category;
    private String content;
    private String createdAt;
    private String status;
    private String userId;
    private ArrayList<String> imageUrl;

    public Report(String id, String category, String content, String createdAt, String status, String userId, ArrayList<String> imageUrl) {
        this.id = id;
        this.category = category;
        this.content = content;
        this.createdAt = createdAt;
        this.status = status;
        this.userId = userId;
        this.imageUrl = imageUrl;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public String getCategory() {
        return category;
    }

    public String getContent() {
        return content;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public String getStatus() {
        return status;
    }

    public String getUserId() {
        return userId;
    }

    public ArrayList<String> getImageUrl() {
        return imageUrl;
    }
}
