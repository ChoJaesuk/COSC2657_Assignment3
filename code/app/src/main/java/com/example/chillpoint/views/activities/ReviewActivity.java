package com.example.chillpoint.views.activities;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RatingBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.chillpoint.R;
import com.example.chillpoint.utils.NavigationSetup;
import com.example.chillpoint.utils.NavigationUtils;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class ReviewActivity extends BaseActivity implements NavigationSetup {
    private RatingBar ratingBar;
    private EditText feedbackEditText;
    private Button submitButton;
    private FirebaseFirestore firestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_review);

        // Initialize views
        ratingBar = findViewById(R.id.reviewRatingBar);
        feedbackEditText = findViewById(R.id.reviewFeedbackEditText);
        submitButton = findViewById(R.id.submitReviewButton);

        // Initialize Firestore
        firestore = FirebaseFirestore.getInstance();

        // Get bookingId, propertyId, userId, username, and imageUrl from intent
        String bookingId = getIntent().getStringExtra("bookingId");
        String propertyId = getIntent().getStringExtra("propertyId");
        String userId = getIntent().getStringExtra("userId");
        String username = getIntent().getStringExtra("username");
        String imageUrl = getIntent().getStringExtra("imageUrl"); // Retrieve imageUrl

        // Validate received data
        if (bookingId == null || propertyId == null || userId == null || username == null || imageUrl == null) {
            Toast.makeText(this, "Invalid data received. Please try again.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Submit button click listener
        submitButton.setOnClickListener(v -> submitReview(bookingId, propertyId, userId, username, imageUrl));
    }

    private void submitReview(String bookingId, String propertyId, String userId, String username, String imageUrl) {
        float rating = ratingBar.getRating();
        String feedback = feedbackEditText.getText().toString().trim();

        if (rating == 0) {
            Toast.makeText(this, "Please select a rating", Toast.LENGTH_SHORT).show();
            return;
        }

        if (feedback.isEmpty()) {
            Toast.makeText(this, "Please write your feedback", Toast.LENGTH_SHORT).show();
            return;
        }

        // Save review to Firestore
        Map<String, Object> review = new HashMap<>();
        review.put("bookingId", bookingId);
        review.put("propertyId", propertyId);
        review.put("userId", userId);
        review.put("username", username);
        review.put("rating", rating);
        review.put("feedback", feedback);
        review.put("imageUrl", imageUrl); // Save imageUrl
        review.put("timestamp", System.currentTimeMillis());

        firestore.collection("reviews")
                .add(review)
                .addOnSuccessListener(documentReference -> {
                    updatePropertyReviewStats(propertyId, rating); // Update property stats
                    Toast.makeText(this, "Review submitted successfully!", Toast.LENGTH_SHORT).show();
                    finish(); // Close the activity
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to submit review", Toast.LENGTH_SHORT).show();
                    Log.e("ReviewActivity", "Error submitting review", e);
                });
    }

    private void updatePropertyReviewStats(String propertyId, float newRating) {
        firestore.collection("Properties").document(propertyId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        double currentAverage = documentSnapshot.contains("averageRating")
                                ? documentSnapshot.getDouble("averageRating") : 0.0;
                        long currentCount = documentSnapshot.contains("reviewCount")
                                ? documentSnapshot.getLong("reviewCount") : 0;

                        // Calculate new average and count
                        double updatedAverage = ((currentAverage * currentCount) + newRating) / (currentCount + 1);
                        long updatedCount = currentCount + 1;

                        // Update Firestore
                        Map<String, Object> updates = new HashMap<>();
                        updates.put("averageRating", updatedAverage);
                        updates.put("reviewCount", updatedCount);

                        firestore.collection("Properties").document(propertyId).update(updates)
                                .addOnSuccessListener(aVoid -> Log.d("ReviewStats", "Property stats updated successfully"))
                                .addOnFailureListener(e -> Log.e("ReviewStats", "Failed to update property stats", e));
                    }
                })
                .addOnFailureListener(e -> Log.e("ReviewStats", "Failed to fetch property details", e));
    }
    @Override
    public void setupNavigationBar() {
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setSelectedItemId(R.id.nav_booking);
        NavigationUtils.handleBottomNavigation(this, bottomNavigationView);
    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {
        super.onPointerCaptureChanged(hasCapture);
    }
}
