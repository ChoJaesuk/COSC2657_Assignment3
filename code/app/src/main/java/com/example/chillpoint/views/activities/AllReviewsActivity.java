package com.example.chillpoint.views.activities;

import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chillpoint.R;
import com.example.chillpoint.views.adapters.ReviewAdapter;
import com.example.chillpoint.views.models.Review;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class AllReviewsActivity extends AppCompatActivity {
    private RecyclerView reviewsRecyclerView;
    private ReviewAdapter reviewAdapter;
    private FirebaseFirestore firestore;
    private String propertyId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_all_reviews);

        // Initialize Firestore
        firestore = FirebaseFirestore.getInstance();

        // Get propertyId from intent
        propertyId = getIntent().getStringExtra("propertyId");

        // Initialize RecyclerView
        reviewsRecyclerView = findViewById(R.id.reviewsRecyclerView);
        reviewsRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Initialize ReviewAdapter
        List<Review> reviews = new ArrayList<>();
        reviewAdapter = new ReviewAdapter(this, reviews);
        reviewsRecyclerView.setAdapter(reviewAdapter);

        // Fetch reviews
        fetchAllReviews();
    }

    private void fetchAllReviews() {
        firestore.collection("reviews")
                .whereEqualTo("propertyId", propertyId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Review> reviews = new ArrayList<>();
                    for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                        reviews.add(document.toObject(Review.class));
                    }
                    reviewAdapter.updateReviews(reviews); // Update data in adapter
                })
                .addOnFailureListener(e -> {
                    Log.e("AllReviewsActivity", "Failed to fetch reviews", e);
                });
    }
}
