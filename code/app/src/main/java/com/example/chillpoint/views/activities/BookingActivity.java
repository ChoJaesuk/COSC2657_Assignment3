package com.example.chillpoint.views.activities;

import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chillpoint.R;
import com.example.chillpoint.managers.SessionManager;
import com.example.chillpoint.views.adapters.BookingAdapter;
import com.example.chillpoint.views.models.Booking;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;

public class BookingActivity extends AppCompatActivity {
    private RecyclerView bookingsRecyclerView;
    private BookingAdapter bookingAdapter;
    private ArrayList<Booking> bookingsList;
    private FirebaseFirestore firestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking);

        // Initialize RecyclerView
        bookingsRecyclerView = findViewById(R.id.bookingsRecyclerView);
        bookingsRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Initialize BookingAdapter and list
        bookingsList = new ArrayList<>();
        bookingAdapter = new BookingAdapter(this, bookingsList);
        bookingsRecyclerView.setAdapter(bookingAdapter);

        // Initialize Firestore
        firestore = FirebaseFirestore.getInstance();

        // Load bookings
        loadBookings();
    }

    private void loadBookings() {
        // Fetch bookings for the logged-in user
        String userId = new SessionManager(this).getUserId();
        firestore.collection("reservations")
                .whereEqualTo("userId", userId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        bookingsList.clear();
                        for (DocumentSnapshot document : task.getResult()) {
                            String propertyId = document.getString("propertyId");
                            String startDate = document.getString("fromDate");
                            String endDate = document.getString("toDate");
                            String status = document.getString("status");

                            // Fetch property details using propertyId
                            firestore.collection("Properties").document(propertyId).get()
                                    .addOnCompleteListener(propertyTask -> {
                                        if (propertyTask.isSuccessful() && propertyTask.getResult() != null) {
                                            DocumentSnapshot propertySnapshot = propertyTask.getResult();

                                            // Fetch data from Firestore
                                            String propertyName = propertySnapshot.getString("name");
                                            String propertyLocation = propertySnapshot.getString("address");
                                            // Extract the first image from the 'images' list
                                            ArrayList<String> images = (ArrayList<String>) propertySnapshot.get("images");
                                            String imageUrl = (images != null && !images.isEmpty()) ? images.get(0) : null;

                                            // Log for debugging
                                            Log.d("PropertyDetails", "Fetched property: " +
                                                    "Name=" + propertyName +
                                                    ", Location=" + propertyLocation +
                                                    ", ImageUrl=" + imageUrl);

                                            // Handle missing fields
                                            if (propertyName == null) propertyName = "No Name Available";
                                            if (propertyLocation == null) propertyLocation = "No Location Available";
                                            if (imageUrl == null) imageUrl = "https://example.com/placeholder.jpg"; // Add a placeholder image URL

                                            // Create Booking object and add to list
                                            Booking booking = new Booking(
                                                    propertyId,
                                                    propertyName,
                                                    propertyLocation,
                                                    imageUrl,
                                                    startDate,
                                                    endDate,
                                                    status
                                            );
                                            bookingsList.add(booking);

                                            // Notify adapter of data changes
                                            bookingAdapter.notifyDataSetChanged();
                                        } else {
                                            Log.e("PropertyDetails", "Failed to fetch property details for ID: " + propertyId);
                                        }
                                    });
                        }
                    } else {
                        Log.e("BookingActivity", "Failed to fetch reservations: ", task.getException());
                    }
                });
    }

}
