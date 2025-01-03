package com.example.chillpoint.views.activities;

import android.os.Bundle;
import android.os.Parcel;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Pair;
import com.bumptech.glide.Glide;
import com.example.chillpoint.R;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.DateValidatorPointForward;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import android.util.Pair;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import android.location.Address;
import android.location.Geocoder;

public class PropertyDetailActivity extends AppCompatActivity implements OnMapReadyCallback {
    private String address;
    private String propertyId;
    private String userId = "sampleUserId"; // Replace with actual user ID from authentication
    private String selectedStartDate;
    private String selectedEndDate;

    private FirebaseFirestore firestore;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_property_detail);

        // Initialize Firestore
        firestore = FirebaseFirestore.getInstance();

        // Initialize views
        ImageView propertyImageView = findViewById(R.id.propertyDetailImageView);
        TextView propertyNameTextView = findViewById(R.id.propertyDetailNameTextView);
        TextView propertyDescriptionTextView = findViewById(R.id.propertyDetailDescriptionTextView);
        TextView propertyAddressTextView = findViewById(R.id.propertyDetailAddressTextView);
        TextView propertyPriceTextView = findViewById(R.id.propertyDetailPriceTextView);
        Button selectDatesButton = findViewById(R.id.selectDatesButton);
        Button bookButton = findViewById(R.id.bookButton);

        // Get data from intent
        String name = getIntent().getStringExtra("name");
        String description = getIntent().getStringExtra("description");
        address = getIntent().getStringExtra("address");
        String price = getIntent().getStringExtra("price");
        String imageUrl = getIntent().getStringExtra("image");
        propertyId = getIntent().getStringExtra("propertyId");
        Log.d("PropertyDetailActivity", "Received propertyId: " + propertyId);

        // Bind data to views
        propertyNameTextView.setText(name);
        propertyDescriptionTextView.setText(description);
        propertyAddressTextView.setText(address);
        propertyPriceTextView.setText(price);

        // Load image using Glide
        if (imageUrl != null) {
            Glide.with(this).load(imageUrl).placeholder(R.drawable.image_placeholder).into(propertyImageView);
        } else {
            propertyImageView.setImageResource(R.drawable.image_placeholder);
        }

        // Initialize map fragment
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.propertyMapFragment);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        // Select Dates
        selectDatesButton.setOnClickListener(v -> openDatePicker());

        // Book Property
        bookButton.setOnClickListener(v -> {
            if (selectedStartDate != null && selectedEndDate != null) {
                bookProperty();
            } else {
                Toast.makeText(this, "Please select a date range first", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void openDatePicker() {
        // Fetch reserved dates for the current property
        firestore.collection("reservations")
                .whereEqualTo("propertyId", propertyId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<Long> reservedDates = new ArrayList<>();
                        for (DocumentSnapshot document : task.getResult()) {
                            try {
                                String fromDateStr = document.getString("fromDate");
                                String toDateStr = document.getString("toDate");
                                if (fromDateStr != null && toDateStr != null) {
                                    long fromDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(fromDateStr).getTime();
                                    long toDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(toDateStr).getTime();

                                    // Add all dates in the range to reservedDates
                                    for (long date = fromDate; date <= toDate; date += 24 * 60 * 60 * 1000) { // Increment by 1 day
                                        reservedDates.add(date);
                                    }
                                }
                            } catch (Exception e) {
                                Log.e("PropertyDetailActivity", "Error parsing dates", e);
                            }
                        }

                        // Create a custom validator to disable reserved dates
                        CalendarConstraints.DateValidator dateValidator = new CalendarConstraints.DateValidator() {
                            @Override
                            public boolean isValid(long date) {
                                return !reservedDates.contains(date); // Disable reserved dates
                            }

                            @Override
                            public int describeContents() {
                                return 0;
                            }

                            @Override
                            public void writeToParcel(Parcel dest, int flags) {
                            }
                        };

                        // Build the date picker with the custom validator
                        CalendarConstraints.Builder constraintsBuilder = new CalendarConstraints.Builder()
                                .setValidator(dateValidator);

                        MaterialDatePicker.Builder<androidx.core.util.Pair<Long, Long>> datePickerBuilder =
                                MaterialDatePicker.Builder.dateRangePicker()
                                        .setTitleText("Select Booking Dates")
                                        .setCalendarConstraints(constraintsBuilder.build());

                        MaterialDatePicker<androidx.core.util.Pair<Long, Long>> datePicker = datePickerBuilder.build();

                        datePicker.addOnPositiveButtonClickListener(selection -> {
                            androidx.core.util.Pair<Long, Long> dateRange = selection;
                            selectedStartDate = formatDate(dateRange.first);
                            selectedEndDate = formatDate(dateRange.second);

                            Toast.makeText(this, "Selected Dates: " + selectedStartDate + " to " + selectedEndDate, Toast.LENGTH_SHORT).show();
                        });

                        datePicker.show(getSupportFragmentManager(), "DATE_PICKER");
                    } else {
                        Log.e("PropertyDetailActivity", "Failed to fetch reservations", task.getException());
                    }
                });
    }

    private String formatDate(Long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }


    private void bookProperty() {
        HashMap<String, Object> reservation = new HashMap<>();
        reservation.put("propertyId", propertyId);
        reservation.put("userId", userId);
        reservation.put("fromDate", selectedStartDate);
        reservation.put("toDate", selectedEndDate);
        reservation.put("timestamp", System.currentTimeMillis());

        firestore.collection("reservations")
                .add(reservation)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(this, "Booking successful!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Booking failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e("PropertyDetailActivity", "Error booking property", e);
                });
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        // Convert address to LatLng using Geocoder
        Geocoder geocoder = new Geocoder(this);
        try {
            List<Address> addressList = geocoder.getFromLocationName(address, 1);
            if (addressList != null && !addressList.isEmpty()) {
                Address location = addressList.get(0);
                LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());

                // Add marker to map
                googleMap.addMarker(new MarkerOptions().position(latLng).title("Property Location"));
                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));
            } else {
                // Fallback message if location not found
                googleMap.addMarker(new MarkerOptions().position(new LatLng(0, 0)).title("Location not found"));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
