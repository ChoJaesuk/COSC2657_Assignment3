package com.example.chillpoint.views.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chillpoint.R;
import com.example.chillpoint.managers.SessionManager;
import com.example.chillpoint.utils.NavigationSetup;
import com.example.chillpoint.utils.NavigationUtils;
import com.example.chillpoint.views.adapters.BookingAdapter;
import com.example.chillpoint.views.models.Booking;
import com.example.chillpoint.views.models.Receipt;
import com.example.chillpoint.views.models.Bill;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

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
        TextView receiptTextView = findViewById(R.id.receiptTextView);
        receiptTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(BookingActivity.this, ReceiptActivity.class);
                startActivity(intent);
            }
        });
        // Load bookings
        loadBookings();
    }

    private void loadBookings() {
        String userId = new SessionManager(this).getUserId();
        Log.e("loadBookings", "Fetching bookings for user ID: " + userId);

        firestore.collection("Receipts")
                .whereEqualTo("payerId", userId)
                .get()
                .addOnCompleteListener(receiptTask -> {
                    if (receiptTask.isSuccessful() && receiptTask.getResult() != null) {
                        List<String> validBillIds = new ArrayList<>();

                        for (DocumentSnapshot receiptDocument : receiptTask.getResult().getDocuments()) {
                            String billId = receiptDocument.getString("billId");
                            if (billId != null) {
                                checkBillAndFetchReservations(billId, validBillIds);
                            }
                        }
                    } else {
                        Log.e("loadBookings", "Failed to fetch receipts: ", receiptTask.getException());
                    }
                });
    }

    private void checkBillAndFetchReservations(String billId, List<String> validBillIds) {
        firestore.collection("Bills").document(billId).get()
                .addOnSuccessListener(billSnapshot -> {
                    if (billSnapshot.exists()) {
                        Bill bill = billSnapshot.toObject(Bill.class);
                        if (bill != null && bill.getReceiptIds() != null) {
                            checkAllReceiptsCompleted(bill.getReceiptIds(), billId, validBillIds);
                        } else {
                            Log.e("checkBillAndFetch", "Bill has no receiptIds or is null for billId: " + billId);
                        }
                    } else {
                        Log.e("checkBillAndFetch", "Bill not found for billId: " + billId);
                    }
                })
                .addOnFailureListener(e -> Log.e("checkBillAndFetch", "Error fetching bill: " + e.getMessage()));
    }

    private void checkAllReceiptsCompleted(List<String> receiptIds, String billId, List<String> validBillIds) {
        firestore.collection("Receipts").whereIn(FieldPath.documentId(), receiptIds).get()
                .addOnSuccessListener(receiptsSnapshot -> {
                    boolean allCompleted = true;
                    for (DocumentSnapshot receiptDocument : receiptsSnapshot.getDocuments()) {
                        Receipt receipt = receiptDocument.toObject(Receipt.class);
                        if (receipt != null && !"Completed".equalsIgnoreCase(receipt.getStatus())) {
                            allCompleted = false;
                            break;
                        }
                    }

                    if (allCompleted) {
                        validBillIds.add(billId);
                        fetchReservationsForBillId(billId);
                    } else {
                        Log.e("checkAllReceipts", "Not all receipts are completed for billId: " + billId);
                    }
                })
                .addOnFailureListener(e -> Log.e("checkAllReceipts", "Error fetching receipts: " + e.getMessage()));
    }

    private void fetchReservationsForBillId(String billId) {
        firestore.collection("reservations")
                .whereEqualTo("billId", billId)
                .get()
                .addOnSuccessListener(reservationsSnapshot -> {
                    if (!reservationsSnapshot.isEmpty()) {
                        for (DocumentSnapshot reservationDocument : reservationsSnapshot.getDocuments()) {
                            String bookingId = reservationDocument.getId();
                            String propertyId = reservationDocument.getString("propertyId");
                            String startDate = reservationDocument.getString("fromDate");
                            String endDate = reservationDocument.getString("toDate");
                            String status = reservationDocument.getString("status");

                            Log.e("ReservationDetails", "Booking ID: " + bookingId);
                            Log.e("ReservationDetails", "Property ID: " + propertyId);
                            Log.e("ReservationDetails", "Start Date: " + startDate);
                            Log.e("ReservationDetails", "End Date: " + endDate);
                            Log.e("ReservationDetails", "Status: " + status);

                            fetchPropertyDetailsAndAddBooking(bookingId, propertyId, startDate, endDate, status);
                        }
                    } else {
                        Log.e("fetchReservations", "No reservations found for billId: " + billId);
                    }
                })
                .addOnFailureListener(e -> Log.e("fetchReservations", "Error fetching reservations: " + e.getMessage()));
    }

    private void fetchPropertyDetailsAndAddBooking(String bookingId, String propertyId, String startDate, String endDate, String status) {
        firestore.collection("Properties").document(propertyId).get()
                .addOnSuccessListener(propertySnapshot -> {
                    if (propertySnapshot.exists()) {
                        String propertyName = propertySnapshot.getString("name");
                        String propertyLocation = propertySnapshot.getString("address");
                        ArrayList<String> images = (ArrayList<String>) propertySnapshot.get("images");
                        String imageUrl = (images != null && !images.isEmpty()) ? images.get(0) : "https://example.com/placeholder.jpg";
                        String description = propertySnapshot.getString("description");
                        Double pricePerNight = propertySnapshot.getDouble("pricePerNight");

                        if (propertyName == null) propertyName = "No Name Available";
                        if (propertyLocation == null) propertyLocation = "No Location Available";
                        if (description == null) description = "No description available";
                        if (pricePerNight == null) pricePerNight = 0.0;

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
                        bookingAdapter.notifyDataSetChanged();

                        Log.e("BookingList", "Added booking to list: " + booking.toString());
                    } else {
                        Log.e("fetchPropertyDetails", "Property not found for propertyId: " + propertyId);
                    }
                })
                .addOnFailureListener(e -> Log.e("fetchPropertyDetails", "Error fetching property details: " + e.getMessage()));
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
