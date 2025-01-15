package com.example.chillpoint.views.activities;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Parcel;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
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
import com.example.chillpoint.repositories.ChatRepository;
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
import java.util.TimeZone;
import java.util.Map;

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
    private HashMap<String, Integer> bedTypeIcons;
    private int selectedGuests = 1; // 유저가 선택한 게스트 수 (기본값 1)
    private long maxNumOfGuests = 0; // Firestore에서 가져올 값
    private long pricePerNight = 0;  // Firestore에서 가져올 값
    // UI 참조할 뷰들
    private TextView guestsCountTextView;
    private Button guestsMinusButton, guestsPlusButton;
    private TextView totalPriceOrErrorTextView;
    private Button contactHostButton;
    private long totalPrice;
    
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_property_detail);
        setupNavigationBar();
        // 세션 데이터 로드
        SessionManager sessionManager = new SessionManager(this);
        ChatRepository chatRepository = new ChatRepository();
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
        // 게스트 수 관련 뷰 찾아오기
        guestsCountTextView = findViewById(R.id.guestsCountTextView);
        guestsMinusButton = findViewById(R.id.guestsMinusButton);
        guestsPlusButton = findViewById(R.id.guestsPlusButton);
        totalPriceOrErrorTextView = findViewById(R.id.totalPriceOrErrorTextView);

        // 초기값
        guestsCountTextView.setText(String.valueOf(selectedGuests));

        // – 버튼 클릭 시
        guestsMinusButton.setOnClickListener(v -> {
            if (selectedGuests > 1) {
                selectedGuests--;
                guestsCountTextView.setText(String.valueOf(selectedGuests));
                updateBookingInfo(); // 날짜/게스트 변경 시마다 총 가격 or 오류메시지 갱신
            }
        });

        // + 버튼 클릭 시
        guestsPlusButton.setOnClickListener(v -> {
            selectedGuests++;
            guestsCountTextView.setText(String.valueOf(selectedGuests));
            updateBookingInfo();
        });

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
        setupBedTypeIcons();
        fetchPropertyDetails(propertyId);
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

        bookButton.setOnClickListener(v -> {
            // 최종 확인: 게스트 초과?
            if (selectedGuests > maxNumOfGuests) {
                Toast.makeText(this, "You have exceeded the maximum number of guests!", Toast.LENGTH_SHORT).show();
                return;
            }
            // 날짜 미선택?
            if (selectedStartDate == null || selectedEndDate == null) {
                Toast.makeText(this, "Please select a date range first", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent intent = new Intent(PropertyDetailActivity.this, PaymentActivity.class);
            String selectedGuestsStr = String.valueOf(selectedGuests);
            String totalPriceStr = String.valueOf(totalPrice);
            intent.putExtra("fromDate", selectedStartDate);
            intent.putExtra("toDate", selectedEndDate);
            intent.putExtra("numberOfGuests", selectedGuestsStr);
            intent.putExtra("totalPrice", totalPriceStr);
            intent.putExtra("propertyId", propertyId);

            Log.e("IntentData", "fromDate: " + selectedStartDate);
            Log.e("IntentData", "toDate: " + selectedEndDate);
            Log.e("IntentData", "numberOfGuests: " + selectedGuests);
            Log.e("IntentData", "totalPrice: " + totalPrice);
            Log.e("IntentData", "propertyId: " + propertyId);
            startActivity(intent);
            // 모두 정상 -> bookProperty
//            bookProperty();
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


        
        contactHostButton = findViewById(R.id.contactHostButton);
        contactHostButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String hostId = (String)contactHostButton.getTag();
                Log.d("contactHostButton", "contact button hostUserId: " + hostUserId);
                contactUser(userId,hostId);
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

                        // 파싱용 SimpleDateFormat 만들 때 UTC로 고정
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));

                        for (DocumentSnapshot document : task.getResult()) {
                            try {
                                String fromDateStr = document.getString("fromDate"); // "2025-01-10"
                                String toDateStr = document.getString("toDate");
                                if (fromDateStr != null && toDateStr != null) {
                                    long fromDate = sdf.parse(fromDateStr).getTime();
                                    long toDate = sdf.parse(toDateStr).getTime();

                                    for (long date = fromDate; date <= toDate; date += 24 * 60 * 60 * 1000) {
                                        reservedDates.add(date);
                                    }
                                }
                            } catch (Exception e) {
                                Log.e("PropertyDetailActivity", "Error parsing dates", e);
                            }
                        }

                        CalendarConstraints.DateValidator dateValidator = new CalendarConstraints.DateValidator() {
                            @Override
                            public boolean isValid(long date) {
                                // 여기서도 'date'(Picker가 주는 값)는 UTC 기준 0시 타임스탬프
                                return !reservedDates.contains(date);
                            }

                            @Override
                            public int describeContents() {
                                return 0;
                            }

                            @Override
                            public void writeToParcel(Parcel dest, int flags) {
                            }
                        };

                        CalendarConstraints.Builder constraintsBuilder =
                                new CalendarConstraints.Builder().setValidator(dateValidator);

                        MaterialDatePicker.Builder<androidx.core.util.Pair<Long, Long>> datePickerBuilder =
                                MaterialDatePicker.Builder.dateRangePicker()
                                        .setTitleText("Select Booking Dates")
                                        .setCalendarConstraints(constraintsBuilder.build());

                        MaterialDatePicker<androidx.core.util.Pair<Long, Long>> datePicker = datePickerBuilder.build();

                        datePicker.addOnPositiveButtonClickListener(selection -> {
                            long startDate = selection.first;
                            long endDate = selection.second;

                            // 선택한 날짜 범위와 예약된 날짜가 겹치는지 검증
                            boolean isConflict = false;
                            for (long reservedDate : reservedDates) {
                                if (startDate <= reservedDate && reservedDate <= endDate) {
                                    isConflict = true;
                                    break;
                                }
                            }

                            if (isConflict) {
                                // 겹치는 날짜가 있는 경우 사용자에게 알림
                                Toast.makeText(this,
                                        "Selected dates conflict with existing reservations. Please choose a different range.",
                                        Toast.LENGTH_SHORT
                                ).show();
                            } else {
                                // 겹치는 날짜가 없으면 예약 처리
                                String formattedStartDate = formatDate(startDate);
                                String formattedEndDate = formatDate(endDate);

                                Toast.makeText(this,
                                        "Selected Dates: " + formattedStartDate + " ~ " + formattedEndDate,
                                        Toast.LENGTH_SHORT
                                ).show();

                                selectedStartDate = formattedStartDate;
                                selectedEndDate = formattedEndDate;

                                updateBookingInfo();
                            }
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
        reservation.put("guestCount", selectedGuests); // 추가
        reservation.put("timestamp", System.currentTimeMillis());
        reservation.put("hostId", hostUserId);
        reservation.put("status","Confirmed");

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
                        contactHostButton.setTag(hostUserId);
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
    private void fetchPropertyDetails(String propertyId) {
        firestore.collection("Properties")
                .document(propertyId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String checkInTime = documentSnapshot.getString("checkInTime");
                        String checkOutTime = documentSnapshot.getString("checkOutTime");

                        // 최대 게스트 수와 가격 정보 가져오기
                        Long maxGuestsFromDB = documentSnapshot.getLong("maxNumOfGuests");
                        Long priceFromDB = documentSnapshot.getLong("pricePerNight");

                        // 침대 타입 데이터 가져오기
                        Map<String, Long> bedTypesMap = (Map<String, Long>) documentSnapshot.get("bedTypes");

                        // UI 업데이트
                        if (checkInTime != null) {
                            TextView checkInTimeTextView = findViewById(R.id.checkInTimeTextView);
                            checkInTimeTextView.setText("Check-in: " + checkInTime);
                        }

                        if (checkOutTime != null) {
                            TextView checkOutTimeTextView = findViewById(R.id.checkOutTimeTextView);
                            checkOutTimeTextView.setText("Check-out: " + checkOutTime);
                        }

                        if (maxGuestsFromDB != null) {
                            this.maxNumOfGuests = maxGuestsFromDB;
                            TextView maxNumOfGuestsTextView = findViewById(R.id.maxNumOfGuestsTextView);
                            maxNumOfGuestsTextView.setText("Maximum Guests: " + this.maxNumOfGuests);
                        }

                        if (priceFromDB != null) {
                            this.pricePerNight = priceFromDB;
                        }

                        if (bedTypesMap != null) {
                            // Long 타입을 Integer로 변환하여 전달
                            Map<String, Integer> convertedBedTypes = new HashMap<>();
                            for (Map.Entry<String, Long> entry : bedTypesMap.entrySet()) {
                                convertedBedTypes.put(entry.getKey(), entry.getValue().intValue());
                            }
                            displayBedTypes(convertedBedTypes);
                        }
                    } else {
                        Log.e("fetchPropertyDetails", "Property not found for ID: " + propertyId);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("fetchPropertyDetails", "Error fetching property details", e);
                });
    }


    private void contactUser(String currentLoginUserId, String hostId) {
        // Reference to the "Chats" collection
        firestore.collection("Chats")
                .whereArrayContains("participants", currentLoginUserId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    String existingChatId = null;
                    // Check if a chat with the hostId exists
                    for (var document : querySnapshot) {
                        ArrayList<String> participants = (ArrayList<String>) document.get("participants");
                        if (participants != null && participants.contains(hostId)) {
                            existingChatId = document.getId();
                            break;
                        }
                    }
                    if (existingChatId != null) {
                        // Chat already exists, navigate to ChatDetailsActivity
                        navigateToChatDetailsActivity(existingChatId);
                    } else {
                        // Chat does not exist, create a new one
                        createNewChat(currentLoginUserId, hostId);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("ChatDetailsActivity", "Error checking existing chats", e);
                    Toast.makeText(this, "Error checking chat", Toast.LENGTH_SHORT).show();
                });
    }
    private void createNewChat(String currentLoginUserId, String hostId) {
        // Prepare the initial chat document
        Map<String, Object> newChat = new HashMap<>();
        ArrayList<String> participants = new ArrayList<>();
        participants.add(currentLoginUserId);
        participants.add(hostId);
        newChat.put("participants", participants);
        newChat.put("createdAt", new Date());
        newChat.put("messages", new ArrayList<Map<String, Object>>());
        // Add the new chat to Firestore
        firestore.collection("Chats")
                .add(newChat)
                .addOnSuccessListener(documentReference -> {
                    String newChatId = documentReference.getId();
                    Toast.makeText(this, "Chat created successfully", Toast.LENGTH_SHORT).show();
                    navigateToChatDetailsActivity(newChatId);
                })
                .addOnFailureListener(e -> {
                    Log.e("ChatDetailsActivity", "Error creating chat", e);
                    Toast.makeText(this, "Error creating chat", Toast.LENGTH_SHORT).show();
                });
    }
    private void navigateToChatDetailsActivity(String chatId) {
        // Navigate to ChatDetailsActivity with the chatId
        Intent intent = new Intent(this, ChatDetailsActivity.class);
        intent.putExtra("chatId", chatId);
        startActivity(intent);
    }

    private void updateBookingInfo() {

// 날짜 선택 후 업데이트
        TextView selectedDatesDisplayTextView = findViewById(R.id.selectedDatesDisplayTextView);

        if (selectedStartDate != null && selectedEndDate != null) {
            selectedDatesDisplayTextView.setVisibility(View.VISIBLE);
            selectedDatesDisplayTextView.setText("Dates: " + selectedStartDate + " to " + selectedEndDate);
        }

        // 1) 게스트가 maxNumOfGuests 초과면 오류메시지 출력
        if (selectedGuests > maxNumOfGuests) {
            totalPriceOrErrorTextView.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
            totalPriceOrErrorTextView.setText("You have exceeded the maximum number of guests!");
            return; // 여기서 종료
        }

        // 2) 게스트 수는 정상 범위. 날짜 선택 여부도 확인
        if (selectedStartDate == null || selectedEndDate == null) {
            // 아직 날짜 선택 안 했으면, 굳이 가격 계산하지 않고 안내만
            totalPriceOrErrorTextView.setTextColor(getResources().getColor(android.R.color.black));
            totalPriceOrErrorTextView.setText("Please select your dates");
            return;
        }

        // 3) 날짜 범위를 일수로 계산
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date start = sdf.parse(selectedStartDate);
            Date end = sdf.parse(selectedEndDate);

            long diffInMillis = end.getTime() - start.getTime();
            long diffInDays = (diffInMillis / (24 * 60 * 60 * 1000)) + 1; // +1 해서 마지막날도 포함

            if (diffInDays < 1) {
                // 혹시나 날짜가 잘못되어 end < start 인 경우
                totalPriceOrErrorTextView.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                totalPriceOrErrorTextView.setText("Invalid date range selected");
                return;
            }

            // 4) 총 가격 계산
            totalPrice = diffInDays * pricePerNight;

            // 5) 화면에 표시
            totalPriceOrErrorTextView.setTextColor(getResources().getColor(android.R.color.black));
            totalPriceOrErrorTextView.setText("Total Price: $" + totalPrice);

        } catch (Exception e) {
            Log.e("updateBookingInfo", "Error parsing dates", e);
            totalPriceOrErrorTextView.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
            totalPriceOrErrorTextView.setText("Error calculating total price");
        }
    }

    private void displayBedTypes(Map<String, Integer> bedTypesMap) {
        LinearLayout bedTypesContainer = findViewById(R.id.bedTypesContainer);
        bedTypesContainer.removeAllViews(); // 기존 뷰 초기화

        for (Map.Entry<String, Integer> entry : bedTypesMap.entrySet()) {
            String bedType = entry.getKey();
            int count = entry.getValue();

            // 아이콘 설정
            ImageView bedIcon = new ImageView(this);
            bedIcon.setLayoutParams(new LinearLayout.LayoutParams(150, 150));

            Integer iconRes = bedTypeIcons.get(bedType);
            if (iconRes != null) {
                bedIcon.setImageResource(iconRes); // 매핑된 아이콘 사용
            } else {
                bedIcon.setImageResource(R.drawable.ic_bed_default); // 기본 아이콘
            }
            bedIcon.setPadding(16, 16, 16, 16);

            // 침대 타입과 개수 추가
            TextView bedLabel = new TextView(this);
            bedLabel.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            ));
            bedLabel.setText(bedType + " x " + count);
            bedLabel.setTextSize(16);
            bedLabel.setPadding(16, 16, 16, 16);

            // Icon과 Label을 묶는 Layout
            LinearLayout bedItemLayout = new LinearLayout(this);
            bedItemLayout.setOrientation(LinearLayout.VERTICAL);
            bedItemLayout.setGravity(Gravity.CENTER);
            bedItemLayout.setPadding(8, 8, 8, 8);

            bedItemLayout.addView(bedIcon);
            bedItemLayout.addView(bedLabel);

            // 최종 Layout에 추가
            bedTypesContainer.addView(bedItemLayout);
        }
    }




    private void setupBedTypeIcons() {
        bedTypeIcons = new HashMap<>();
        bedTypeIcons.put("King Size", R.drawable.ic_bed_king); // King Size 침대 아이콘
        bedTypeIcons.put("Queen Size", R.drawable.ic_bed_queen); // Queen Size 침대 아이콘
        bedTypeIcons.put("Double", R.drawable.ic_bed_queen); // Double 침대 아이콘
        bedTypeIcons.put("Single", R.drawable.ic_bed_single); // Single 침대 아이콘
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
