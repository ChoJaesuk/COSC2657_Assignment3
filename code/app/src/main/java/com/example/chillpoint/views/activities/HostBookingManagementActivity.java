package com.example.chillpoint.views.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chillpoint.R;
import com.example.chillpoint.views.adapters.BookingAdapter;
import com.example.chillpoint.views.models.Booking;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;

public class HostBookingManagementActivity extends AppCompatActivity {
    private RecyclerView bookingsRecyclerView;
    private BookingAdapter bookingAdapter;
    private ArrayList<Booking> bookingsList;
    private FirebaseFirestore firestore;
    private String hostId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_host_booking_management);

        // Retrieve hostId from intent
        hostId = getIntent().getStringExtra("hostId");
        if (hostId == null || hostId.isEmpty()) {
            Log.e("HostBookingManagement", "No hostId provided");
            finish(); // Exit activity if hostId is missing
            return;
        }

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
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        Log.d("BookingActivity", "onActivityResult called with requestCode: " + requestCode + ", resultCode: " + resultCode);

        if (requestCode == 1001 && resultCode == RESULT_OK) {
            if (data != null && data.hasExtra("updatedStatus")) {
                String updatedStatus = data.getStringExtra("updatedStatus");
                Log.d("BookingActivity", "Received updated status: " + updatedStatus);
                loadBookings(); // 데이터 새로 로드
            } else {
                Log.d("BookingActivity", "No data or updatedStatus extra found");
            }
        } else {
            Log.d("BookingActivity", "Unexpected resultCode or requestCode");
        }
    }
    private void loadBookings() {
        Log.e("HostBookingManagement", "Loading bookings for hostId: " + hostId);

        firestore.collection("reservations")
                .whereEqualTo("hostId", hostId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
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
                                        }
                                    });
                        }
                    } else {
                        Log.e("HostBookingManagement", "Failed to load bookings: ", task.getException());
                    }
                });
    }

}
