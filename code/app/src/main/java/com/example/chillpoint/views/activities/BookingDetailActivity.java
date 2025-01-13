package com.example.chillpoint.views.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.example.chillpoint.R;
import com.example.chillpoint.managers.SessionManager;
import com.example.chillpoint.views.adapters.ImageSliderAdapter;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BookingDetailActivity extends AppCompatActivity {
    private FirebaseFirestore firestore;
    private String propertyId, bookingId, bookingDates, hostUserId, bookingStatus, userId;

    private TextView propertyNameTextView, propertyDescriptionTextView, propertyAddressTextView;
    private TextView hostNameTextView, hostDetailsTextView;
    private TextView bookingIdTextView, bookingDatesTextView, bookingStatusTextView;
    private ImageView hostImageView;
    private ViewPager2 propertyImageViewPager;
    private Map<String, Integer> bedTypeIcons;
    private Spinner statusDropdown;
    private Button confirmStatusButton;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking_detail);

        // Firestore 초기화
        firestore = FirebaseFirestore.getInstance();
        sessionManager = new SessionManager(this);
        userId = sessionManager.getUserId();
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
        bookingStatusTextView = findViewById(R.id.bookingStatusTextView);

        statusDropdown = findViewById(R.id.statusDropdown);
        confirmStatusButton = findViewById(R.id.confirmStatusButton);

        // 침대 타입 아이콘 초기화
        setupBedTypeIcons();

        // Intent 데이터 가져오기
        propertyId = getIntent().getStringExtra("propertyId");
        bookingId = getIntent().getStringExtra("bookingId");
        bookingDates = getIntent().getStringExtra("dates");
        bookingStatus = getIntent().getStringExtra("status");

        if (propertyId == null || propertyId.isEmpty()) {
            Toast.makeText(this, "Invalid Property ID", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Booking ID 및 Dates 데이터 바인딩
        bookingIdTextView.setText(bookingId != null ? "Booking ID: " + bookingId : "No Booking ID");
        bookingDatesTextView.setText(bookingDates != null ? "Booking Dates: " + bookingDates : "No Booking Dates");
        bookingStatusTextView.setText(bookingStatus != null ? "Booking Status: " + bookingStatus : "No Booking Status");

        // Fetch Property Details
        fetchPropertyDetails(propertyId);

        // Fetch Host Information
        fetchHostInformation(propertyId);

        checkHostVerification();
    }

    private void setupBedTypeIcons() {
        bedTypeIcons = new HashMap<>();
        bedTypeIcons.put("King Size", R.drawable.ic_bed_king); // King Size 침대 아이콘
        bedTypeIcons.put("Queen Size", R.drawable.ic_bed_queen); // Queen Size 침대 아이콘
        bedTypeIcons.put("Double", R.drawable.ic_bed_queen); // Double 침대 아이콘
        bedTypeIcons.put("Single", R.drawable.ic_bed_single); // Single 침대 아이콘
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

                        // 침대 타입 데이터 가져오기
                        Map<String, Long> bedTypesMap = (Map<String, Long>) documentSnapshot.get("bedTypes");

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

                        // 침대 타입 UI 업데이트
                        if (bedTypesMap != null && !bedTypesMap.isEmpty()) {
                            Map<String, Integer> convertedBedTypes = new HashMap<>();
                            for (Map.Entry<String, Long> entry : bedTypesMap.entrySet()) {
                                convertedBedTypes.put(entry.getKey(), entry.getValue().intValue());
                            }
                            displayBedTypes(convertedBedTypes);
                        } else {
                            Toast.makeText(this, "No bed type information available", Toast.LENGTH_SHORT).show();
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

    private void checkHostVerification() {
        if (userId == null) {
            showAlert("Error", "Session expired. Please log in again.");
            return;
        }

        firestore.collection("HostVerifications")
                .whereEqualTo("userId", userId)
                .whereEqualTo("status", "Approved")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        QuerySnapshot querySnapshot = task.getResult();
                        if (!querySnapshot.isEmpty()) {
                            setupHostControls(); // Host 전용 UI 설정
                        } else {
                            hideHostControls(); // 일반 사용자 UI 설정
                        }
                    } else {
                        showAlert("Error", "Error checking host verification status.");
                    }
                });
    }
    private void showAlert(String title, String message) {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                .show();
    }
    private void setupHostControls() {
        statusDropdown.setVisibility(View.VISIBLE);
        confirmStatusButton.setVisibility(View.VISIBLE);

        // 드롭다운 메뉴에 Status 목록 설정
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this,
                R.array.reservation_status_array, // strings.xml에 정의된 status 배열
                android.R.layout.simple_spinner_item
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        statusDropdown.setAdapter(adapter);

        // Confirm 버튼 클릭 리스너
        confirmStatusButton.setOnClickListener(v -> updateReservationStatus());
    }

    private void hideHostControls() {
        statusDropdown.setVisibility(View.GONE);
        confirmStatusButton.setVisibility(View.GONE);
    }

    private void updateReservationStatus() {
        String newStatus = statusDropdown.getSelectedItem().toString();
        Log.d("BookingDetailActivity", "Selected new status: " + newStatus);

        firestore.collection("reservations")
                .document(bookingId)
                .update("status", newStatus)
                .addOnSuccessListener(aVoid -> {
                    Log.d("BookingDetailActivity", "Status successfully updated in Firestore to: " + newStatus);
                    Toast.makeText(this, "Status updated to " + newStatus, Toast.LENGTH_SHORT).show();

                    Intent resultIntent = new Intent();
                    resultIntent.putExtra("updatedStatus", newStatus); // 변경된 상태 전달
                    setResult(RESULT_OK, resultIntent); // RESULT_OK로 설정
                    Log.d("BookingDetailActivity", "setResult called with RESULT_OK");
                    finish(); // 이전 액티비티로 돌아가기
                })
                .addOnFailureListener(e -> {
                    Log.e("BookingDetailActivity", "Error updating status", e);
                    Toast.makeText(this, "Failed to update status", Toast.LENGTH_SHORT).show();
                });
    }

}