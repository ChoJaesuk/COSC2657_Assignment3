package com.example.chillpoint.views.activities;

import com.google.firebase.Timestamp;

public class NotificationItem {
    public String id;
    public String title;
    public String message;
    public boolean isRead;
    public Timestamp timestamp;

    // Default no-argument constructor required for Firestore
    public NotificationItem() {
    }

    public NotificationItem(String id, String title, String message, Boolean isRead, Timestamp timestamp) {
        this.id = id;
        this.title = title;
        this.message = message;
        this.isRead = isRead != null && isRead;
        this.timestamp = timestamp;
    }
}
