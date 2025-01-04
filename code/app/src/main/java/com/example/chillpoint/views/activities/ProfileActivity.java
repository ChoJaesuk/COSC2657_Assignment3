package com.example.chillpoint.views.activities;

import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.chillpoint.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class ProfileActivity extends AppCompatActivity {

    private ImageView profileImageView;
    private TextView usernameTextView, emailTextView;
    private Button hostingMenuButton, reportsMenuButton, settingsMenuButton;

    private FirebaseAuth auth;
    private FirebaseFirestore firestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        // Initialize Firebase
        auth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        // Initialize UI components
        profileImageView = findViewById(R.id.profileImageView);
        usernameTextView = findViewById(R.id.usernameTextView);
        emailTextView = findViewById(R.id.emailTextView);
        hostingMenuButton = findViewById(R.id.hostingMenuButton);
        reportsMenuButton = findViewById(R.id.reportsMenuButton);
        settingsMenuButton = findViewById(R.id.settingsMenuButton);

        // Load user profile
        loadUserProfile();

        // Set button click listeners
        hostingMenuButton.setOnClickListener(v -> Toast.makeText(this, "Hostings clicked", Toast.LENGTH_SHORT).show());
        reportsMenuButton.setOnClickListener(v -> Toast.makeText(this, "Reports clicked", Toast.LENGTH_SHORT).show());
        settingsMenuButton.setOnClickListener(v -> Toast.makeText(this, "Settings clicked", Toast.LENGTH_SHORT).show());
    }

    private void loadUserProfile() {
        String userId = auth.getCurrentUser().getUid();

        firestore.collection("Users").document(userId).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot snapshot = task.getResult();
                        if (snapshot.exists()) {
                            String avatarUrl = snapshot.getString("avatarUrl");
                            String username = snapshot.getString("username");
                            String email = snapshot.getString("email");

                            // Display user info
                            usernameTextView.setText(username != null ? username : "Unknown User");
                            emailTextView.setText(email != null ? email : "No Email Provided");

                            // Load profile image
                            if (avatarUrl != null && !avatarUrl.isEmpty()) {
                                Glide.with(ProfileActivity.this)
                                        .load(avatarUrl)
                                        .placeholder(R.drawable.defaultavatar) // Default image while loading
                                        .error(R.drawable.defaultavatar) // Default image if loading fails
                                        .into(profileImageView);
                            } else {
                                profileImageView.setImageResource(R.drawable.defaultavatar); // Default image
                            }
                        } else {
                            Toast.makeText(ProfileActivity.this, "User data not found.", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(ProfileActivity.this, "Error loading profile: " + task.getException(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
