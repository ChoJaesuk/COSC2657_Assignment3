package com.example.chillpoint.views.activities;

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

                                            // Fetch property details
                                            String propertyName = propertySnapshot.getString("name");
                                            String propertyLocation = propertySnapshot.getString("address");
                                            ArrayList<String> images = (ArrayList<String>) propertySnapshot.get("images");
                                            String imageUrl = (images != null && !images.isEmpty()) ? images.get(0) : null;
                                            String description = propertySnapshot.getString("description");
                                            Double pricePerNight = propertySnapshot.getDouble("pricePerNight");

                                            Log.e("PropertyDetails", "Property Name: " + propertyName);
                                            Log.e("PropertyDetails", "Property Location: " + propertyLocation);
                                            Log.e("PropertyDetails", "Image URL: " + imageUrl);
                                            Log.e("PropertyDetails", "Price per Night: " + pricePerNight);
                                            Log.e("PropertyDetails", "Description: " + description);

                                            // Handle missing fields
                                            if (propertyName == null) propertyName = "No Name Available";
                                            if (propertyLocation == null) propertyLocation = "No Location Available";
                                            if (imageUrl == null)
                                                imageUrl = "https://example.com/placeholder.jpg"; // Placeholder
                                            if (description == null) description = "No description available";
                                            if (pricePerNight == null) pricePerNight = 0.0;

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
                                                    pricePerNight,
                                                    description,
                                                    images
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
