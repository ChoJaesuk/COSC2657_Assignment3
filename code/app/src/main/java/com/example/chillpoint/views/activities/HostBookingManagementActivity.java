package com.example.chillpoint.views.activities;

import android.os.Bundle;
import android.util.Log;

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
                                                    status
                                            );
                                            bookingsList.add(booking);

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
