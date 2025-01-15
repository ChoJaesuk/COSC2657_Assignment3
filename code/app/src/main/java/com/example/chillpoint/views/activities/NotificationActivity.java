package com.example.chillpoint.views.activities;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.chillpoint.R;
import com.example.chillpoint.managers.SessionManager;
import com.example.chillpoint.views.adapters.NotificationAdapter;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashSet;

import javax.annotation.Nullable;

public class NotificationActivity extends AppCompatActivity {

    private FirebaseFirestore firestore;
    private SessionManager sessionManager;

    private ListView notificationListView;
    private ProgressBar progressBar;

    private NotificationAdapter notificationAdapter;
    private ArrayList<NotificationItem> notificationList;

    // 초기 HashSet 보장
    private HashSet<String> initialUnreadNotifications = new HashSet<>();
    private boolean isActivityVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification);

        // Initialize UI components
        notificationListView = findViewById(R.id.notificationListView);
        progressBar = findViewById(R.id.progressBar);

        // Initialize Firestore and SessionManager
        firestore = FirebaseFirestore.getInstance();
        sessionManager = new SessionManager(this);

        // Initialize list and adapter
        notificationList = new ArrayList<>();
        notificationAdapter = new NotificationAdapter(this, notificationList);

        // Ensure initialUnreadNotifications is initialized
        initialUnreadNotifications = new HashSet<>();

        // Set adapter
        notificationListView.setAdapter(notificationAdapter);

        // Load notifications in real-time
        listenForNotifications();
    }

    @Override
    protected void onResume() {
        super.onResume();
        isActivityVisible = true; // Activity is visible
    }

    @Override
    protected void onPause() {
        super.onPause();
        isActivityVisible = false; // Activity is not visible
    }

    private void listenForNotifications() {
        progressBar.setVisibility(View.VISIBLE);

        String userId = sessionManager.getUserId();
        if (userId == null) {
            Toast.makeText(this, "User session invalid. Cannot load notifications.", Toast.LENGTH_SHORT).show();
            progressBar.setVisibility(View.GONE);
            return;
        }

        firestore.collection("Notifications")
                .whereEqualTo("userId", userId)
                .orderBy("timestamp", Query.Direction.ASCENDING) // 최신순으로 정렬
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot snapshots, @Nullable FirebaseFirestoreException e) {
                        progressBar.setVisibility(View.GONE);
                        if (e != null) {
                            Log.e("NotificationActivity", "Error listening for notifications", e);
                            return;
                        }

                        if (snapshots != null) {
                            for (DocumentChange docChange : snapshots.getDocumentChanges()) {
                                switch (docChange.getType()) {
                                    case ADDED:
                                    case MODIFIED:
                                        NotificationItem item = docChange.getDocument().toObject(NotificationItem.class);
                                        item.id = docChange.getDocument().getId();

                                        // If the activity is visible and the notification is unread, add to initialUnreadNotifications
                                        if (!item.isRead && isActivityVisible) {
                                            initialUnreadNotifications.add(item.id);
                                        }

                                        updateNotificationList(item);
                                        break;
                                }
                            }
                            notificationAdapter.notifyDataSetChanged();
                        }
                    }
                });
    }

    private void updateNotificationList(NotificationItem newItem) {
        // Check if the notification already exists in the list
        boolean exists = false;
        for (int i = 0; i < notificationList.size(); i++) {
            NotificationItem existingItem = notificationList.get(i);
            if (existingItem.id.equals(newItem.id)) {
                notificationList.set(i, newItem); // Update the existing item
                exists = true;
                break;
            }
        }

        if (!exists) {
            notificationList.add(0, newItem); // Add new item at the top
        }

        // Automatically mark notifications as read only if the activity is visible
        if (isActivityVisible && !newItem.isRead) {
            firestore.collection("Notifications")
                    .document(newItem.id)
                    .update("isRead", true)
                    .addOnSuccessListener(unused -> Log.d("NotificationActivity", "Notification marked as read"))
                    .addOnFailureListener(e -> Log.e("NotificationActivity", "Error updating notification to read", e));
        }
    }

    public HashSet<String> getInitialUnreadNotifications() {
        return initialUnreadNotifications;
    }
}
