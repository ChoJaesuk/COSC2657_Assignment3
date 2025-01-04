package com.example.chillpoint.views.activities;

import android.content.Intent;
import android.os.Bundle;
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
    private TextView usernameTextView;
    private TextView hostVerificationTextView;

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
        hostVerificationTextView = findViewById(R.id.hostVerification);

        // Load user profile
        loadUserProfile();

        // Set click listener for Host Verification
        hostVerificationTextView.setOnClickListener(v -> {
            Intent intent = new Intent(ProfileActivity.this, HostVerificationActivity.class);
            startActivity(intent);
        });
    }

    private void loadUserProfile() {
        String userId = auth.getCurrentUser().getUid();

        firestore.collection("Users").document(userId).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot snapshot = task.getResult();
                        if (snapshot.exists()) {
                            String imageUrl = snapshot.getString("imageUrl");
                            String username = snapshot.getString("username");

                            // Display user info
                            usernameTextView.setText(username != null ? username : "Unknown User");

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
                        } else {
                            Toast.makeText(ProfileActivity.this, "User data not found.", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(ProfileActivity.this, "Error loading profile: " + task.getException(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
