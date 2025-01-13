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
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class NotificationActivity extends AppCompatActivity {

    private FirebaseFirestore firestore;
    private SessionManager sessionManager;

    private ListView notificationListView;
    private ProgressBar progressBar;

    private NotificationAdapter notificationAdapter;
    private ArrayList<NotificationItem> notificationList;

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
        notificationListView.setAdapter(notificationAdapter);

        // Load notifications
        loadNotifications();
    }

    private void loadNotifications() {
        progressBar.setVisibility(View.VISIBLE);

        String userId = sessionManager.getUserId();
        if (userId == null) {
            Toast.makeText(this, "User session invalid. Cannot load notifications.", Toast.LENGTH_SHORT).show();
            progressBar.setVisibility(View.GONE);
            return;
        }

        firestore.collection("Notifications")
                .whereEqualTo("userId", userId)
                .orderBy("timestamp", Query.Direction.DESCENDING) // 최신순으로 정렬
                .get()
                .addOnCompleteListener(task -> {
                    progressBar.setVisibility(View.GONE);
                    if (task.isSuccessful() && task.getResult() != null) {
                        notificationList.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            NotificationItem item = new NotificationItem(
                                    document.getId(),
                                    document.getString("title"),
                                    document.getString("message"),
                                    document.getBoolean("isRead"),
                                    document.getTimestamp("timestamp")
                            );
                            notificationList.add(item);
                        }
                        notificationAdapter.notifyDataSetChanged();

                        // Update isRead status to true
                        updateNotificationsToRead(userId);
                    } else {
                        Log.e("NotificationActivity", "Error loading notifications", task.getException());
                        Toast.makeText(this, "Failed to load notifications.", Toast.LENGTH_SHORT).show();
                    }
                });
    }


    private void updateNotificationsToRead(String userId) {
        firestore.collection("Notifications")
                .whereEqualTo("userId", userId)
                .whereEqualTo("isRead", false)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Map<String, Object> update = new HashMap<>();
                            update.put("isRead", true);
                            firestore.collection("Notifications")
                                    .document(document.getId())
                                    .update(update);
                        }
                    } else {
                        Log.e("NotificationActivity", "Error updating isRead status", task.getException());
                    }
                });
    }
}
