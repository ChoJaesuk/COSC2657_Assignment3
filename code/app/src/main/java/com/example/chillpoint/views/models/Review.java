package com.example.chillpoint.views.models;

public class Review {
    private String username;
    private String userImageUrl;
    private String feedback;
    private float rating;
    private long timestamp;

    // Empty constructor for Firestore
    public Review() {}

    public Review(String username, String userImageUrl, String feedback, float rating, long timestamp) {
        this.username = username;
        this.userImageUrl = userImageUrl;
        this.feedback = feedback;
        this.rating = rating;
        this.timestamp = timestamp;
    }

    public String getUsername() {
        return username;
    }

    public String getUserImageUrl() {
        return userImageUrl;
    }

    public String getFeedback() {
        return feedback;
    }

    public float getRating() {
        return rating;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
