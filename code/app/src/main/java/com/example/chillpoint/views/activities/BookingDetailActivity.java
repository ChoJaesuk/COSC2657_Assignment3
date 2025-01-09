package com.example.chillpoint.views.activities;

import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.example.chillpoint.R;
import com.example.chillpoint.views.adapters.ImageSliderAdapter;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class BookingDetailActivity extends AppCompatActivity {
    private FirebaseFirestore firestore;
    private String propertyId, bookingId, bookingDates, hostUserId;

    private TextView propertyNameTextView, propertyDescriptionTextView, propertyAddressTextView;
    private TextView hostNameTextView, hostDetailsTextView;
    private TextView bookingIdTextView, bookingDatesTextView;
    private ImageView hostImageView;
    private ViewPager2 propertyImageViewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking_detail);

        // Firestore 초기화
        firestore = FirebaseFirestore.getInstance();

        // View 초기화
        propertyNameTextView = findViewById(R.id.propertyDetailNameTextView);
        propertyDescriptionTextView = findViewById(R.id.propertyDetailDescriptionTextView);
        propertyAddressTextView = findViewById(R.id.propertyDetailAddressTextView);
        bookingIdTextView = findViewById(R.id.bookingIdTextView);
        bookingDatesTextView = findViewById(R.id.bookingDatesTextView);
        hostNameTextView = findViewById(R.id.hostNameTextView);
        hostDetailsTextView = findViewById(R.id.hostDetailsTextView);
        hostImageView = findViewById(R.id.hostImageView);
        propertyImageViewPager = findViewById(R.id.propertyImageViewPager);

        // Intent 데이터 가져오기
        propertyId = getIntent().getStringExtra("propertyId");
        bookingId = getIntent().getStringExtra("bookingId");
        bookingDates = getIntent().getStringExtra("dates");

        if (propertyId == null || propertyId.isEmpty()) {
            Toast.makeText(this, "Invalid Property ID", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Booking ID 및 Dates 데이터 바인딩
        bookingIdTextView.setText(bookingId != null ? "Booking ID: " + bookingId : "No Booking ID");
        bookingDatesTextView.setText(bookingDates != null ? "Booking Dates: " + bookingDates : "No Booking Dates");

        // Fetch Property Details
        fetchPropertyDetails(propertyId);

        // Fetch Host Information
        fetchHostInformation(propertyId);
    }

    private void fetchPropertyDetails(String propertyId) {
        firestore.collection("Properties")
                .document(propertyId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String name = documentSnapshot.getString("name");
                        String description = documentSnapshot.getString("description");
                        String address = documentSnapshot.getString("address");
                        String checkInTime = documentSnapshot.getString("checkInTime");
                        String checkOutTime = documentSnapshot.getString("checkOutTime");
                        List<String> images = (List<String>) documentSnapshot.get("images");

                        // View에 데이터 바인딩
                        propertyNameTextView.setText(name != null ? name : "No Name");
                        propertyDescriptionTextView.setText(description != null ? description : "No Description");
                        propertyAddressTextView.setText(address != null ? address : "No Address");

                        // 체크인 시간
                        if (checkInTime != null) {
                            TextView checkInTimeTextView = findViewById(R.id.checkInTimeTextView);
                            checkInTimeTextView.setText("Check-in: " + checkInTime);
                        }

                        // 체크아웃 시간
                        if (checkOutTime != null) {
                            TextView checkOutTimeTextView = findViewById(R.id.checkOutTimeTextView);
                            checkOutTimeTextView.setText("Check-out: " + checkOutTime);
                        }

                        // 이미지 슬라이더 설정
                        if (images != null && !images.isEmpty()) {
                            ImageSliderAdapter sliderAdapter = new ImageSliderAdapter(this, images);
                            propertyImageViewPager.setAdapter(sliderAdapter);
                        } else {
                            Toast.makeText(this, "No images available for this property", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(this, "Property not found!", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("BookingDetailActivity", "Error fetching property details", e);
                    Toast.makeText(this, "Failed to load property details", Toast.LENGTH_SHORT).show();
                });
    }

    private void fetchHostInformation(String propertyId) {
        firestore.collection("Properties")
                .document(propertyId)
                .get()
                .addOnSuccessListener(propertySnapshot -> {
                    if (propertySnapshot.exists()) {
                        hostUserId = propertySnapshot.getString("userId");

                        if (hostUserId != null) {
                            // Fetch Host Information
                            firestore.collection("Users")
                                    .document(hostUserId)
                                    .get()
                                    .addOnSuccessListener(userSnapshot -> {
                                        if (userSnapshot.exists()) {
                                            String username = userSnapshot.getString("username");
                                            String bio = userSnapshot.getString("bio");
                                            String imageUrl = userSnapshot.getString("imageUrl");

                                            // View에 데이터 바인딩
                                            hostNameTextView.setText("Name: " + (username != null ? username : "Unknown"));
                                            hostDetailsTextView.setText(bio != null ? bio : "No details available");

                                            // Glide로 호스트 이미지 로드
                                            if (imageUrl != null && !imageUrl.isEmpty()) {
                                                Glide.with(this)
                                                        .load(imageUrl)
                                                        .placeholder(R.drawable.default_host_image)
                                                        .into(hostImageView);
                                            } else {
                                                hostImageView.setImageResource(R.drawable.default_host_image);
                                            }
                                        } else {
                                            Log.e("BookingDetailActivity", "Host not found in Users collection.");
                                        }
                                    })
                                    .addOnFailureListener(e -> Log.e("BookingDetailActivity", "Error fetching host data", e));
                        } else {
                            Log.e("BookingDetailActivity", "Host userId is null.");
                        }
                    }
                })
                .addOnFailureListener(e -> Log.e("BookingDetailActivity", "Error fetching property data", e));
    }
}
