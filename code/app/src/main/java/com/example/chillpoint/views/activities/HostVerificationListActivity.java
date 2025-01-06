package com.example.chillpoint.views.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ImageView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.chillpoint.R;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class HostVerificationListActivity extends AppCompatActivity {

    private LinearLayout verificationListLayout;
    private ProgressBar progressBar;

    private FirebaseFirestore firestore;
    private ActivityResultLauncher<Intent> detailsActivityLauncher;

    // Store verifications to update data dynamically
    private List<Map<String, Object>> verifications;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_host_verification_list);

        // Initialize UI components
        verificationListLayout = findViewById(R.id.verificationListLayout);
        progressBar = findViewById(R.id.progressBar);

        // Initialize Firebase Firestore
        firestore = FirebaseFirestore.getInstance();

        // Initialize the verifications list
        verifications = new ArrayList<>();

        // Register for activity result
        detailsActivityLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        String verificationId = result.getData().getStringExtra("verificationId");
                        String status = result.getData().getStringExtra("status");
                        updateStatusInList(verificationId, status);
                    }
                });

        // Load verification list
        loadVerificationList();
    }

    private void loadVerificationList() {
        progressBar.setVisibility(View.VISIBLE);
        firestore.collection("HostVerifications")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    progressBar.setVisibility(View.GONE);
                    verifications.clear();
                    for (var doc : queryDocumentSnapshots.getDocuments()) {
                        Map<String, Object> verification = doc.getData();
                        verification.put("verificationId", doc.getId()); // Add document ID
                        verifications.add(verification);
                    }
                    displayVerificationList();
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Failed to load verifications: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void displayVerificationList() {
        verificationListLayout.removeAllViews();
        for (Map<String, Object> verification : verifications) {
            View cardView = getLayoutInflater().inflate(R.layout.card_host_verification, verificationListLayout, false);

            TextView usernameTextView = cardView.findViewById(R.id.usernameTextView);
            TextView timestampTextView = cardView.findViewById(R.id.timestampTextView);
            TextView statusTextView = cardView.findViewById(R.id.statusTextView);
            ImageView imageView = cardView.findViewById(R.id.verificationImageView);
            View seeDetailsButton = cardView.findViewById(R.id.seeDetailsButton);

            // Set data into views
            usernameTextView.setText(verification.containsKey("username") ? verification.get("username").toString() : "N/A");
            timestampTextView.setText(verification.containsKey("timestamp") ? verification.get("timestamp").toString() : "N/A");

            // Set status and color
            String status = verification.get("status").toString();
            statusTextView.setText(status);
            if ("Approved".equalsIgnoreCase(status)) {
                statusTextView.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
            } else if ("Rejected".equalsIgnoreCase(status)) {
                statusTextView.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
            }

            // Set image
            if (verification.containsKey("imageUrls")) {
                try {
                    @SuppressWarnings("unchecked")
                    String imageUrl = ((List<String>) verification.get("imageUrls")).get(0);
                    Glide.with(this).load(imageUrl).into(imageView);
                } catch (Exception e) {
                    Glide.with(this).load(R.drawable.placeholder_image).into(imageView); // Default placeholder
                }
            } else {
                Glide.with(this).load(R.drawable.placeholder_image).into(imageView); // Default placeholder
            }

            // Set click listener for See Details button
            seeDetailsButton.setOnClickListener(v -> {
                Intent intent = new Intent(HostVerificationListActivity.this, HostVerificationDetailsActivity.class);
                intent.putExtra("verificationId", verification.get("verificationId").toString());
                detailsActivityLauncher.launch(intent);
            });

            // Add card to the list
            verificationListLayout.addView(cardView);

            // Store the verification ID in the card view tag
            cardView.setTag(verification.get("verificationId"));
        }
    }

    private void updateStatusInList(String verificationId, String status) {
        // Update the status in the local verifications list
        for (Map<String, Object> verification : verifications) {
            if (verificationId.equals(verification.get("verificationId"))) {
                verification.put("status", status); // Update the status
                break;
            }
        }

        // Refresh the UI by redisplaying the list
        displayVerificationList();
    }
}
