package com.example.chillpoint.views.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chillpoint.R;
import com.example.chillpoint.managers.SessionManager;
import com.example.chillpoint.utils.NavigationSetup;
import com.example.chillpoint.utils.NavigationUtils;
import com.example.chillpoint.views.adapters.BookingAdapter;
import com.example.chillpoint.views.models.Booking;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;

public class BookingActivity extends AppCompatActivity implements NavigationSetup {
    private RecyclerView bookingsRecyclerView;
    private BookingAdapter bookingAdapter;
    private ArrayList<Booking> bookingsList;
    private FirebaseFirestore firestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking);
        setupNavigationBar();

        // Initialize RecyclerView
        bookingsRecyclerView = findViewById(R.id.bookingsRecyclerView);
        bookingsRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Initialize BookingAdapter and list
        bookingsList = new ArrayList<>();
        bookingAdapter = new BookingAdapter(this, bookingsList);
        bookingsRecyclerView.setAdapter(bookingAdapter);

        // Set click listener for bookings
        bookingAdapter.setOnBookingClickListener(booking -> {
            String userId = new SessionManager(this).getUserId();
            String username = new SessionManager(this).getUsername(); // Assuming you have this method
            String propertyId = booking.getPropertyId();
            long totalPrice = booking.getTotalPrice();

            // Navigate to BillSplittingActivity
            navigateToBillSplitting(userId, username, propertyId, totalPrice);
        });

            // Initialize Firestore
        firestore = FirebaseFirestore.getInstance();

        // Load bookings
        loadBookings();
    }

    private void loadBookings() {
        // Fetch bookings for the logged-in user
        String userId = new SessionManager(this).getUserId();
        Log.e("loadBookings", "Fetching bookings for user ID: " + userId);

        firestore.collection("reservations")
                .whereEqualTo("userId", userId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.e("loadBookings", "Successfully fetched reservations for user ID: " + userId);
                        bookingsList.clear();
                        for (DocumentSnapshot document : task.getResult()) {
                            String bookingId = document.getId(); // Booking ID
                            String propertyId = document.getString("propertyId");
                            String startDate = document.getString("fromDate");
                            String endDate = document.getString("toDate");
                            String status = document.getString("status");
                            long totalPrice = document.contains("totalPrice") && document.getLong("totalPrice") != null
                                    ? document.getLong("totalPrice")
                                    : 0; // Default to 0 if totalPrice is null

                            Log.e("ReservationDetails", "Booking ID: " + bookingId);
                            Log.e("ReservationDetails", "Property ID: " + propertyId);
                            Log.e("ReservationDetails", "Start Date: " + startDate);
                            Log.e("ReservationDetails", "End Date: " + endDate);
                            Log.e("ReservationDetails", "Status: " + status);

                            // Fetch property details using propertyId
                            firestore.collection("Properties").document(propertyId).get()
                                    .addOnCompleteListener(propertyTask -> {
                                        if (propertyTask.isSuccessful() && propertyTask.getResult() != null) {
                                            DocumentSnapshot propertySnapshot = propertyTask.getResult();

                                            // Fetch data from Firestore
                                            String propertyName = propertySnapshot.getString("name");
                                            String propertyLocation = propertySnapshot.getString("address");
                                            ArrayList<String> images = (ArrayList<String>) propertySnapshot.get("images");
                                            String imageUrl = (images != null && !images.isEmpty()) ? images.get(0) : null;

                                            Log.e("PropertyDetails", "Property Name: " + propertyName);
                                            Log.e("PropertyDetails", "Property Location: " + propertyLocation);
                                            Log.e("PropertyDetails", "Image URL: " + imageUrl);

                                            // Handle missing fields
                                            if (propertyName == null) propertyName = "No Name Available";
                                            if (propertyLocation == null) propertyLocation = "No Location Available";
                                            if (imageUrl == null)
                                                imageUrl = "https://example.com/placeholder.jpg"; // Placeholder

                                            // Create Booking object and add to list
                                            Booking booking = new Booking(
                                                    bookingId,
                                                    propertyId,
                                                    propertyName,
                                                    propertyLocation,
                                                    imageUrl,
                                                    startDate,
                                                    endDate,
                                                    status,
                                                    totalPrice
                                            );
                                            bookingsList.add(booking);

                                            Log.e("BookingList", "Added booking to list: " + booking.toString());

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

    // Inside BookingActivity
    private void navigateToBillSplitting(String userId, String username, String propertyId, long totalPrice) {
        Intent intent = new Intent(BookingActivity.this, BillSplittingActivity.class);
        intent.putExtra("userId", userId);
        intent.putExtra("username", username);
        intent.putExtra("propertyId", propertyId);
        intent.putExtra("totalPrice", totalPrice);
        startActivity(intent);
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
