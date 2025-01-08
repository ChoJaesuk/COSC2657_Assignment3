package com.example.chillpoint.views.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.chillpoint.R;
import com.example.chillpoint.managers.SessionManager;

public class ProfileActivity extends AppCompatActivity {

    private ImageView profileImageView;
    private TextView usernameTextView;
    private TextView hostVerificationTextView;

    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        // Initialize SessionManager
        sessionManager = new SessionManager(this);

        // Initialize UI components
        profileImageView = findViewById(R.id.profileImageView);
        usernameTextView = findViewById(R.id.usernameTextView);
        hostVerificationTextView = findViewById(R.id.hostVerification);

        // Load user profile using SessionManager
        loadUserProfile();

        // Set click listener for Host Verification
        hostVerificationTextView.setOnClickListener(v -> {
            Intent intent = new Intent(ProfileActivity.this, HostVerificationActivity.class);
            startActivity(intent);
        });
    }

    private void loadUserProfile() {
        // Get user data from SessionManager
        String username = sessionManager.getUsername();
        String imageUrl = sessionManager.getUserImageUrl();

        if (username != null && !username.isEmpty()) {
            usernameTextView.setText(username);
        } else {
            usernameTextView.setText("Unknown User");
        }

        // Load profile image
        if (imageUrl != null && !imageUrl.isEmpty()) {
            Glide.with(ProfileActivity.this)
                    .load(imageUrl)
                    .placeholder(R.drawable.defaultavatar) // Default image while loading
                    .error(R.drawable.defaultavatar) // Default image if loading fails
                    .into(profileImageView);
        } else {
            profileImageView.setImageResource(R.drawable.defaultavatar); // Default image
        }

        // Check if session data exists, else show an error
        if (username == null && imageUrl == null) {
            Toast.makeText(this, "Session data not found. Please log in again.", Toast.LENGTH_SHORT).show();
        }
    }
}
