package com.example.chillpoint.views.activities;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.chillpoint.R;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Map;

public class HostVerificationDetailsActivity extends AppCompatActivity {

    private TextView usernameTextView, phoneTextView, statusTextView, timestampTextView;
    private ImageView imageView;
    private EditText adminNoteEditText;
    private Button approveButton, rejectButton;
    private ProgressBar progressBar;

    private FirebaseFirestore firestore;
    private String verificationId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_host_verification_details);

        // Initialize UI components
        usernameTextView = findViewById(R.id.usernameTextView);
        phoneTextView = findViewById(R.id.phoneTextView);
        statusTextView = findViewById(R.id.statusTextView);
        timestampTextView = findViewById(R.id.timestampTextView);
        imageView = findViewById(R.id.imageView);
        adminNoteEditText = findViewById(R.id.adminNoteEditText);
        approveButton = findViewById(R.id.approveButton);
        rejectButton = findViewById(R.id.rejectButton);
        progressBar = findViewById(R.id.progressBar);

        // Initialize Firebase Firestore
        firestore = FirebaseFirestore.getInstance();
        verificationId = getIntent().getStringExtra("verificationId");

        // Load host verification details
        loadVerificationDetails();

        // Set listeners for approve and reject buttons
        approveButton.setOnClickListener(v -> updateVerificationStatus("Approved"));
        rejectButton.setOnClickListener(v -> updateVerificationStatus("Rejected"));
    }

    private void loadVerificationDetails() {
        progressBar.setVisibility(View.VISIBLE);
        firestore.collection("HostVerifications").document(verificationId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    progressBar.setVisibility(View.GONE);
                    if (documentSnapshot.exists()) {
                        Map<String, Object> verificationData = documentSnapshot.getData();
                        if (verificationData != null) {
                            displayDetails(verificationData);
                        }
                    } else {
                        Toast.makeText(this, "Verification not found.", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Failed to load verification details: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void displayDetails(Map<String, Object> verificationData) {
        // Display username
        usernameTextView.setText(verificationData.containsKey("username") ? verificationData.get("username").toString() : "N/A");

        // Display phone
        phoneTextView.setText(verificationData.containsKey("phone") ? verificationData.get("phone").toString() : "N/A");

        // Display status
        statusTextView.setText(verificationData.containsKey("status") ? verificationData.get("status").toString() : "N/A");

        // Display timestamp
        timestampTextView.setText(verificationData.containsKey("timestamp") ? verificationData.get("timestamp").toString() : "N/A");

        // Display image (load the first image from the imageUrls array)
        if (verificationData.containsKey("imageUrls")) {
            try {
                @SuppressWarnings("unchecked")
                String imageUrl = ((java.util.List<String>) verificationData.get("imageUrls")).get(0);
                Glide.with(this).load(imageUrl).into(imageView);
            } catch (Exception e) {
                Glide.with(this).load(R.drawable.placeholder_image).into(imageView); // Fallback to placeholder image
            }
        } else {
            Glide.with(this).load(R.drawable.placeholder_image).into(imageView);
        }

        // Display admin note
        adminNoteEditText.setText(verificationData.containsKey("adminNote") ? verificationData.get("adminNote").toString() : "");
    }

    private void updateVerificationStatus(String status) {
        String adminNote = adminNoteEditText.getText().toString().trim();

        if (TextUtils.isEmpty(adminNote)) {
            Toast.makeText(this, "Please add an admin note before proceeding.", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);

        firestore.collection("HostVerifications").document(verificationId)
                .update("status", status, "adminNote", adminNote)
                .addOnSuccessListener(aVoid -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Verification updated successfully.", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Failed to update verification: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
