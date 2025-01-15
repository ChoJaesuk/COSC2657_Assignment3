package com.example.chillpoint.views.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.MenuItemCompat;

import com.example.chillpoint.R;
import com.example.chillpoint.managers.SessionManager;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;

import javax.annotation.Nullable;

public class BaseActivity extends AppCompatActivity {

    private TextView notificationBadge;
    private FirebaseFirestore firestore;
    private SessionManager sessionManager;
    private MenuItem notificationItem;

    @Override
    protected void onStart() {
        super.onStart();
        // Set up the custom action bar with back button, logo, and app title
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true); // Enable back button
            actionBar.setDisplayShowHomeEnabled(true); // Show home icon
            actionBar.setHomeAsUpIndicator(R.drawable.ic_back_black); // Set custom back button icon

            actionBar.setDisplayShowTitleEnabled(false); // Disable default title to use custom title
            actionBar.setDisplayShowCustomEnabled(true);

            // Create a custom layout for the action bar
            LinearLayout customActionBarLayout = new LinearLayout(this);
            customActionBarLayout.setOrientation(LinearLayout.HORIZONTAL);
            customActionBarLayout.setGravity(Gravity.CENTER_VERTICAL); // Center content vertically

            // Add the logo to the layout
            ImageView logoImageView = new ImageView(this);
            logoImageView.setImageResource(R.drawable.logo); // Replace with your logo
            LinearLayout.LayoutParams logoLayoutParams = new LinearLayout.LayoutParams(80, 80); // Adjust size
            logoLayoutParams.setMargins(32, 0, 16, 0); // Move logo slightly to the left
            customActionBarLayout.addView(logoImageView, logoLayoutParams);

            // Add the app title to the layout
            TextView appTitleTextView = new TextView(this);
            appTitleTextView.setText(R.string.app_name); // Replace with your app name
            appTitleTextView.setTextSize(20); // Increase text size
            appTitleTextView.setGravity(Gravity.CENTER_VERTICAL); // Vertically center the text
            appTitleTextView.setTextColor(getResources().getColor(R.color.black)); // Set text color
            customActionBarLayout.addView(appTitleTextView);

            // Set the custom layout to the action bar
            actionBar.setCustomView(customActionBarLayout);
        }

        // Initialize Firebase and SessionManager
        firestore = FirebaseFirestore.getInstance();
        sessionManager = new SessionManager(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate menu with notification icon
        getMenuInflater().inflate(R.menu.menu_notification, menu);

        notificationItem = menu.findItem(R.id.action_notifications);
        View actionView = MenuItemCompat.getActionView(notificationItem);

        // Set up notification badge
        notificationBadge = actionView.findViewById(R.id.notificationBadge);
        actionView.setOnClickListener(v -> {
            onOptionsItemSelected(notificationItem);
        });

        // Start listening for notifications after menu is created
        listenForNotifications();

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_notifications) {
            // Open NotificationActivity when the icon is clicked
            Intent intent = new Intent(this, NotificationActivity.class);
            startActivity(intent);
            return true;
        } else if (item.getItemId() == android.R.id.home) {
            // Handle back button action
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void listenForNotifications() {
        String userId = sessionManager.getUserId();
        if (userId == null) {
            return; // No user logged in
        }

        firestore.collection("Notifications")
                .whereEqualTo("userId", userId)
                .whereEqualTo("isRead", false)
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot snapshots, @Nullable FirebaseFirestoreException e) {
                        if (e != null) {
                            return; // Handle error if needed
                        }
                        if (snapshots != null && notificationBadge != null) {
                            int unreadCount = snapshots.size();
                            if (unreadCount > 0) {
                                notificationBadge.setVisibility(View.VISIBLE);
                                notificationBadge.setText(String.valueOf(unreadCount));
                            } else {
                                notificationBadge.setVisibility(View.GONE);
                            }
                        }
                    }
                });
    }
}
