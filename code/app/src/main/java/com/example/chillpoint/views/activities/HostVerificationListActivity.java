package com.example.chillpoint.views.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ImageView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.chillpoint.R;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class HostVerificationListActivity extends AppCompatActivity {

    private LinearLayout verificationListLayout;
    private ProgressBar progressBar;
    private Spinner statusFilterSpinner, dateFilterSpinner;
    private Button applyFiltersButton;

    private FirebaseFirestore firestore;
    private ActivityResultLauncher<Intent> detailsActivityLauncher;

    // Store verifications to update data dynamically
    private List<Map<String, Object>> verifications;
    private List<Map<String, Object>> filteredVerifications;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_host_verification_list);

        // Initialize UI components
        verificationListLayout = findViewById(R.id.verificationListLayout);
        progressBar = findViewById(R.id.progressBar);
        statusFilterSpinner = findViewById(R.id.statusFilterSpinner);
        dateFilterSpinner = findViewById(R.id.dateFilterSpinner);
        applyFiltersButton = findViewById(R.id.applyFiltersButton);

        // Initialize Firebase Firestore
        firestore = FirebaseFirestore.getInstance();

        // Initialize the verifications list
        verifications = new ArrayList<>();
        filteredVerifications = new ArrayList<>();

        // Set up spinners
        setUpSpinners();

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

        // Set up filter button
        applyFiltersButton.setOnClickListener(v -> applyFilters());
    }

    private void setUpSpinners() {
        // Status filter spinner
        ArrayAdapter<CharSequence> statusAdapter = ArrayAdapter.createFromResource(
                this, R.array.status_filter_options, android.R.layout.simple_spinner_item);
        statusAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        statusFilterSpinner.setAdapter(statusAdapter);

        // Date filter spinner
        ArrayAdapter<CharSequence> dateAdapter = ArrayAdapter.createFromResource(
                this, R.array.date_filter_options, android.R.layout.simple_spinner_item);
        dateAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        dateFilterSpinner.setAdapter(dateAdapter);
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
                    filteredVerifications.clear();
                    filteredVerifications.addAll(verifications);
                    displayVerificationList();
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Failed to load verifications: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void applyFilters() {
        String selectedStatus = statusFilterSpinner.getSelectedItem().toString();
        String selectedDateOrder = dateFilterSpinner.getSelectedItem().toString();

        // Filter by status
        filteredVerifications.clear();
        for (Map<String, Object> verification : verifications) {
            if (selectedStatus.equals("All") || verification.get("status").toString().equalsIgnoreCase(selectedStatus)) {
                filteredVerifications.add(verification);
            }
        }

        // Sort by date
        if (selectedDateOrder.equals("Date: Newest First")) {
            Collections.sort(filteredVerifications, (v1, v2) -> compareDates(v2.get("timestamp").toString(), v1.get("timestamp").toString()));
        } else if (selectedDateOrder.equals("Date: Oldest First")) {
            Collections.sort(filteredVerifications, (v1, v2) -> compareDates(v1.get("timestamp").toString(), v2.get("timestamp").toString()));
        }

        displayVerificationList();
    }

    private int compareDates(String date1, String date2) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            Date d1 = sdf.parse(date1);
            Date d2 = sdf.parse(date2);
            return d1.compareTo(d2);
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    private void displayVerificationList() {
        verificationListLayout.removeAllViews();
        for (Map<String, Object> verification : filteredVerifications) {
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
        applyFilters();
    }
}
