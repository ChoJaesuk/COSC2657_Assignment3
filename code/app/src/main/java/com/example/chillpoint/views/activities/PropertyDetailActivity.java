package com.example.chillpoint.views.activities;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Parcel;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import android.util.Pair;
import com.bumptech.glide.Glide;
import com.example.chillpoint.R;
import com.example.chillpoint.managers.SessionManager;
import com.example.chillpoint.repositories.WishlistRepository;
import com.example.chillpoint.utils.NavigationSetup;
import com.example.chillpoint.utils.NavigationUtils;
import com.example.chillpoint.views.adapters.ImageSliderAdapter;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.DateValidatorPointForward;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

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

public class PropertyDetailActivity extends AppCompatActivity implements OnMapReadyCallback, NavigationSetup {
    private String address;
    private String propertyId;
    private String selectedStartDate;
    private String selectedEndDate;
    private String hostUserId;
    private String userId; // From session
    private String username; // From session
    private FirebaseFirestore firestore;
    private WishlistRepository wishlistRepository;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_property_detail);
        setupNavigationBar();
        // 세션 데이터 로드
        SessionManager sessionManager = new SessionManager(this);
        userId = sessionManager.getUserId();
        String role = sessionManager.getRole();
        username = sessionManager.getUsername();
        // 디버깅 로그 추가
        Log.d("SessionManager", "Loaded session: userId=" + userId + ", role=" + role + ", username=" + username);

        wishlistRepository = new WishlistRepository();
        // 세션 검증
        if (userId == null || role == null || username == null) {
            Toast.makeText(this, "Failed to load user session. Please log in again.", Toast.LENGTH_SHORT).show();
            // LoginActivity로 이동
            Intent intent = new Intent(PropertyDetailActivity.this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
            return;
        }
        // Initialize Firestore
        firestore = FirebaseFirestore.getInstance();

        // Initialize views
        ViewPager2 imageViewPager = findViewById(R.id.propertyImageViewPager);
        TextView propertyNameTextView = findViewById(R.id.propertyDetailNameTextView);
        TextView propertyDescriptionTextView = findViewById(R.id.propertyDetailDescriptionTextView);
        TextView propertyAddressTextView = findViewById(R.id.propertyDetailAddressTextView);
        TextView propertyPriceTextView = findViewById(R.id.propertyDetailPriceTextView);
        Button selectDatesButton = findViewById(R.id.selectDatesButton);
        Button bookButton = findViewById(R.id.bookButton);
        TextView propertyAddressTextViewTop = findViewById(R.id.propertyDetailAddressTextViewTop);
// Inside onCreate method
        TextView hostNameTextView = findViewById(R.id.hostNameTextView);
        TextView hostDetailsTextView = findViewById(R.id.hostDetailsTextView);
        ImageView hostImageView = findViewById(R.id.hostImageView);
        // Get data from intent
        String name = getIntent().getStringExtra("name");
        String description = getIntent().getStringExtra("description");
        address = getIntent().getStringExtra("address");
        String price = getIntent().getStringExtra("price");
        List<String> images = getIntent().getStringArrayListExtra("images");
        // Get propertyId from Intent
        propertyId = getIntent().getStringExtra("propertyId");

        // Debug log to check if propertyId is correctly received
        Log.d("PropertyDetailActivity", "Received propertyId: " + propertyId);
        // propertyId 가져오기
//        propertyId = getIntent().getStringExtra("propertyId");
        TextView addToWishlist = findViewById(R.id.addToWishlist);
        if (userId != null && propertyId != null) {
            wishlistRepository.isExistWishlistItem(userId, propertyId)
                    .addOnSuccessListener(exists -> {
                        if (exists) {
                            // If the wishlist item exists, hide the button
                            addToWishlist.setBackgroundTintList(getResources().getColorStateList(R.color.red));
                            Log.d("WishlistCheck", "Wishlist item exists. Button hidden.");
                        } else {
                            // If the wishlist item does not exist, make the button visible
                            addToWishlist.setVisibility(View.VISIBLE);
                            Log.d("WishlistCheck", "Wishlist item does not exist. Button visible.");
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e("WishlistCheck", "Error checking wishlist item existence", e);
                        Toast.makeText(PropertyDetailActivity.this, "Error checking wishlist status", Toast.LENGTH_SHORT).show();
                    });
        } else {
            Log.e("WishlistCheck", "Invalid user or property information.");
            Toast.makeText(this, "Invalid user or property information", Toast.LENGTH_SHORT).show();
        }
//        if (propertyId == null || propertyId.isEmpty()) {
//            Toast.makeText(this, "Invalid Property ID", Toast.LENGTH_SHORT).show();
//            finish();
//            return;
//        }

        // 리뷰 통계 불러오기
        fetchPropertyReviewStats(propertyId);
        if (propertyId == null || propertyId.isEmpty()) {
            Toast.makeText(this, "Property ID is missing!", Toast.LENGTH_SHORT).show();
            finish(); // Close the activity to prevent further issues
            return;
        }
// Fetch host information
        fetchHostInformation(propertyId, hostNameTextView, hostDetailsTextView, hostImageView);

        // Bind data to views
        propertyNameTextView.setText(name);
        propertyDescriptionTextView.setText(description);
        propertyAddressTextView.setText(address);
        propertyPriceTextView.setText(price);
        propertyAddressTextViewTop.setText(address);

        // Set up image slider
        if (images != null && !images.isEmpty()) {
            ImageSliderAdapter sliderAdapter = new ImageSliderAdapter(this, images);
            imageViewPager.setAdapter(sliderAdapter);
        } else {
            Toast.makeText(this, "No images available", Toast.LENGTH_SHORT).show();
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

        // 기타 초기화 코드
        fetchSingleReview();
        Button seeAllReviewsButton = findViewById(R.id.seeAllReviewsButton);
        seeAllReviewsButton.setOnClickListener(v -> {
            Intent intent = new Intent(PropertyDetailActivity.this, AllReviewsActivity.class);
            intent.putExtra("propertyId", propertyId);
            startActivity(intent);
        });



        addToWishlist.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Assuming you have a userId and propertyId available
                if (userId != null && propertyId != null) {
                    // Check if the item already exists in the wishlist
                    wishlistRepository.isExistWishlistItem(userId, propertyId)
                            .addOnSuccessListener(exists -> {
                                if (exists) {
                                    // If the item exists, show a toast message
                                    Toast.makeText(PropertyDetailActivity.this, "Already in Wishlist!", Toast.LENGTH_SHORT).show();
                                } else {
                                    // If the item does not exist, add it to the wishlist
                                    wishlistRepository.addWishlistItem(userId, propertyId)
                                            .addOnSuccessListener(result -> {
                                                if (result) {
                                                    // Show success message
                                                    Toast.makeText(PropertyDetailActivity.this, "Added to Wishlist!", Toast.LENGTH_SHORT).show();
                                                    // Change the button background tint to red
                                                    addToWishlist.setBackgroundTintList(getResources().getColorStateList(R.color.red));
                                                    // Optionally hide the button
                                                }
                                            })
                                            .addOnFailureListener(e -> {
                                                // Show error message
                                                Toast.makeText(PropertyDetailActivity.this, "Failed to add to Wishlist: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                                Log.e("Wishlist", "Error adding to wishlist", e);
                                            });
                                }
                            })
                            .addOnFailureListener(e -> {
                                // Show error message for the `isExistWishlistItem` call
                                Toast.makeText(PropertyDetailActivity.this, "Error checking wishlist: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                Log.e("Wishlist", "Error checking wishlist existence", e);
                            });
                } else {
                    Toast.makeText(PropertyDetailActivity.this, "Invalid user or property information", Toast.LENGTH_SHORT).show();
                }
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

    private void fetchHostInformation(String propertyId, TextView hostNameTextView, TextView hostDetailsTextView, ImageView hostImageView) {
        // Fetch the property data to get the userId
        firestore.collection("Properties")
                .document(propertyId)
                .get()
                .addOnSuccessListener(propertySnapshot -> {
                    if (propertySnapshot.exists()) {
                        hostUserId = propertySnapshot.getString("userId"); // 호스트 ID 저장
                        Log.d("PropertyDetailActivity", "Fetched hostUserId: " + hostUserId);
                        if (hostUserId != null) {
                            // Fetch the user data from Users collection
                            firestore.collection("Users")
                                    .document(hostUserId)
                                    .get()
                                    .addOnSuccessListener(userSnapshot -> {
                                        if (userSnapshot.exists()) {
                                            String username = userSnapshot.getString("username");
                                            String bio = userSnapshot.getString("bio");
                                            String imageUrl = userSnapshot.getString("imageUrl");

                                            // Update UI with host information
                                            hostNameTextView.setText("Name: " + (username != null ? username : "Unknown"));
                                            hostDetailsTextView.setText(bio != null ? bio : "Details not available");

                                            // Load image using Glide
                                            if (imageUrl != null && !imageUrl.isEmpty()) {
                                                Glide.with(this)
                                                        .load(imageUrl)
                                                        .placeholder(R.drawable.default_host_image) // 디폴트 이미지
                                                        .into(hostImageView);
                                            } else {
                                                // Load default image if imageUrl is null or empty
                                                hostImageView.setImageResource(R.drawable.default_host_image);
                                            }
                                        } else {
                                            Log.e("PropertyDetailActivity", "User not found in Users collection.");
                                        }
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e("PropertyDetailActivity", "Error fetching user data", e);
                                    });
                        } else {
                            Log.e("PropertyDetailActivity", "userId not found in property data.");
                        }
                    } else {
                        Log.e("PropertyDetailActivity", "Property not found in Properties collection.");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("PropertyDetailActivity", "Error fetching property data", e);
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
    private void fetchSingleReview() {
        firestore.collection("reviews")
                .whereEqualTo("propertyId", propertyId)
                .orderBy("timestamp", Query.Direction.DESCENDING) // 최신 순으로 정렬
                .limit(1) // 하나의 리뷰만 가져오기
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        DocumentSnapshot document = querySnapshot.getDocuments().get(0);

                        // Firestore에서 데이터 가져오기
                        String username = document.getString("username");
                        String userImageUrl = document.getString("imageUrl");
                        String feedback = document.getString("feedback");
                        float rating = (document.contains("rating")) ? document.getDouble("rating").floatValue() : 0f;
                        long timestamp = document.getLong("timestamp");

                        // 리뷰 섹션 UI에 데이터 반영
                        TextView reviewUserTextView = findViewById(R.id.reviewUserTextView);
                        TextView reviewDateTextView = findViewById(R.id.reviewDateTextView);
                        TextView reviewContentTextView = findViewById(R.id.reviewContentTextView);
                        ImageView reviewUserImageView = findViewById(R.id.reviewUserImageView);

                        reviewUserTextView.setText(username + " ★" + rating);
                        reviewDateTextView.setText(formatDate(timestamp));
                        reviewContentTextView.setText(feedback);

                        // Glide로 유저 이미지 로드
                        Glide.with(this)
                                .load(userImageUrl)
                                .placeholder(R.drawable.default_host_image)
                                .into(reviewUserImageView);
                    } else {
                        // 리뷰가 없는 경우 기본값 설정
                        TextView reviewTitleTextView = findViewById(R.id.reviewTitleTextView);
                        reviewTitleTextView.setText("No reviews available");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("PropertyDetailActivity", "Failed to fetch single review", e);
                    Toast.makeText(this, "Failed to load review", Toast.LENGTH_SHORT).show();
                });
    }


    private String formatDate(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }
    private void fetchPropertyReviewStats(String propertyId) {
        firestore.collection("Properties")
                .document(propertyId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        double averageRating = documentSnapshot.contains("averageRating")
                                ? documentSnapshot.getDouble("averageRating") : 0.0;
                        long reviewCount = documentSnapshot.contains("reviewCount")
                                ? documentSnapshot.getLong("reviewCount") : 0;

                        // Update the TextView with fetched data
                        TextView reviewTitleTextView = findViewById(R.id.reviewTitleTextView);
                        reviewTitleTextView.setText("★ " + String.format("%.1f", averageRating) + " reviews (" + reviewCount + ")");
                        TextView averageRatingTv = findViewById(R.id.averageRating);
                        averageRatingTv.setText("★ " + String.format("%.1f", averageRating) + " reviews (" + reviewCount + ")");
                    } else {
                        Log.e("PropertyDetailActivity", "Property not found in database.");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("PropertyDetailActivity", "Failed to fetch property review stats", e);
                    Toast.makeText(this, "Failed to load review stats", Toast.LENGTH_SHORT).show();
                });
    }


    @Override
    public void setupNavigationBar() {
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setSelectedItemId(R.id.nav_trips);
        NavigationUtils.handleBottomNavigation(this, bottomNavigationView);
    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {
        super.onPointerCaptureChanged(hasCapture);
    }
}
