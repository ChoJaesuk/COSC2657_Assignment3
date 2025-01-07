package com.example.chillpoint.views.activities;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.util.Pair;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chillpoint.R;
import com.example.chillpoint.managers.SessionManager;
import com.example.chillpoint.utils.NavigationSetup;
import com.example.chillpoint.utils.NavigationUtils;
import com.example.chillpoint.views.adapters.PropertyAdapter;
import com.example.chillpoint.views.models.Property;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.DateValidatorPointForward;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class UserMainActivity extends AppCompatActivity implements NavigationSetup {
    private static final String TAG = "UserMainActivity";

    private RecyclerView recyclerView;
    private PropertyAdapter propertyAdapter;
    private ArrayList<Property> propertyList;
    private FirebaseFirestore firestore;
    private ProgressBar progressBar;
    private ImageButton filterButton, searchButton;
    private EditText searchEditText;
    private TextView selectDateTextView;

    private String startDate = null;
    private String endDate = null;
    private String selectedRooms = null;
    private String selectedBeds = null;
    private String selectedDateRange = "None";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_main);
        setupNavigationBar();
        // Load user session data
        // 세션 데이터 로드
        SessionManager sessionManager = new SessionManager(this);
        String userId = sessionManager.getUserId();
        String role = sessionManager.getRole();
        String username = sessionManager.getUsername();
        // 디버깅 로그 추가
        Log.d("SessionManager", "Loaded session: userId=" + userId + ", role=" + role + ", username=" + username);

        // 세션 검증
        if (userId == null || role == null || username == null) {
            Toast.makeText(this, "Failed to load user session. Please log in again.", Toast.LENGTH_SHORT).show();
            // LoginActivity로 이동
            Intent intent = new Intent(UserMainActivity.this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
            return;
        }
        recyclerView = findViewById(R.id.recyclerView);
        progressBar = findViewById(R.id.progressBar);
        filterButton = findViewById(R.id.filterButton);
        searchButton = findViewById(R.id.searchButton);
        searchEditText = findViewById(R.id.searchEditText);
//        Button createPropertyButton = findViewById(R.id.createPropertyButton);
//        createPropertyButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                // Navigate to CreatePropertyActivity
//                Intent intent = new Intent(UserMainActivity.this, CreatePropertyActivity.class);
//                startActivity(intent);
//            }
//        });
        // Initialize 'My Bookings' button
//        Button checkBookingsButton = findViewById(R.id.checkBookingsButton);
//        checkBookingsButton.setOnClickListener(v -> {
//            Intent intent = new Intent(UserMainActivity.this, BookingActivity.class);
//            startActivity(intent);
//        });
        propertyList = new ArrayList<>();
        propertyAdapter = new PropertyAdapter(this, propertyList);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(propertyAdapter);

        firestore = FirebaseFirestore.getInstance();

        // Load all properties initially
        loadProperties(null, null, null, null, null);

        // Search button click listener
        searchButton.setOnClickListener(v -> {
            String queryText = searchEditText.getText().toString().trim();
            loadProperties(startDate, endDate, selectedRooms, selectedBeds, queryText);
        });

        // Filter button click listener
        filterButton.setOnClickListener(v -> openFilterBottomSheet());
        Button wishlistButton = findViewById(R.id.wishlistButton);
        wishlistButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(UserMainActivity.this, WishlistActivity.class);
                startActivity(intent);
                finish();
            }
        });
    }

    private void loadProperties(String startDate, String endDate, String rooms, String beds, String searchQuery) {
        progressBar.setVisibility(View.VISIBLE);

        // Step 1: 초기 모든 property 로드 (검색어와 필터가 없을 때)
        if ((startDate == null || startDate.isEmpty()) &&
                (endDate == null || endDate.isEmpty()) &&
                (searchQuery == null || searchQuery.isEmpty()) &&
                rooms == null && beds == null) {

            firestore.collection("Properties")
                    .get()
                    .addOnCompleteListener(task -> {
                        progressBar.setVisibility(View.GONE);
                        if (task.isSuccessful()) {
                            propertyList.clear();
                            for (DocumentSnapshot document : task.getResult()) {
                                Property property = document.toObject(Property.class);
                                if (property != null) {
                                    property.setId(document.getId()); // Firebase 문서 ID 설정
                                    propertyList.add(property);
                                }
                            }
                            propertyAdapter.notifyDataSetChanged();
                            Log.d(TAG, "All properties loaded successfully: " + propertyList.size());
                        } else {
                            Toast.makeText(this, "Failed to load properties", Toast.LENGTH_SHORT).show();
                            Log.e(TAG, "Error loading all properties", task.getException());
                        }
                    });
            return;
        }

        // Step 2: 특정 날짜가 선택된 경우, 예약된 property 제외
        firestore.collection("reservations")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        ArrayList<String> excludedPropertyIds = new ArrayList<>();
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                        try {
                            long startTimestamp = startDate != null ? sdf.parse(startDate).getTime() : 0;
                            long endTimestamp = endDate != null ? sdf.parse(endDate).getTime() : Long.MAX_VALUE;

                            for (DocumentSnapshot document : task.getResult()) {
                                String reservedStartDate = document.getString("fromDate");
                                String reservedEndDate = document.getString("toDate");
                                String propertyId = document.getString("propertyId");

                                if (reservedStartDate != null && reservedEndDate != null && propertyId != null) {
                                    long reservedStart = sdf.parse(reservedStartDate).getTime();
                                    long reservedEnd = sdf.parse(reservedEndDate).getTime();

                                    // Check if the selected range overlaps with the reserved range
                                    if (!(endTimestamp < reservedStart || startTimestamp > reservedEnd)) {
                                        excludedPropertyIds.add(propertyId); // Add propertyId to exclude list
                                    }
                                }
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing dates", e);
                        }

                        // Step 3: Query properties excluding reserved propertyIds
                        Query query = firestore.collection("Properties");

                        if (!excludedPropertyIds.isEmpty()) {
                            query = query.whereNotIn("id", excludedPropertyIds); // Exclude reserved properties
                        }

                        // Add additional filters
                        if (rooms != null) {
                            query = query.whereEqualTo("numOfRooms", Integer.parseInt(rooms));
                        }

                        if (beds != null) {
                            query = query.whereEqualTo("numOfBeds", Integer.parseInt(beds));
                        }

                        if (searchQuery != null && !searchQuery.isEmpty()) {
                            query = query.whereGreaterThanOrEqualTo("name", searchQuery)
                                    .whereLessThanOrEqualTo("name", searchQuery + "\uf8ff");
                        }

                        // Fetch properties
                        query.get().addOnCompleteListener(propertyTask -> {
                            progressBar.setVisibility(View.GONE);
                            if (propertyTask.isSuccessful()) {
                                propertyList.clear();
                                for (DocumentSnapshot document : propertyTask.getResult()) {
                                    Property property = document.toObject(Property.class);
                                    if (property != null) {
                                        property.setId(document.getId()); // Set Firebase document ID
                                        propertyList.add(property);
                                    }
                                }
                                propertyAdapter.notifyDataSetChanged();
                                Log.d(TAG, "Filtered properties loaded successfully: " + propertyList.size());
                            } else {
                                Toast.makeText(this, "Failed to load properties", Toast.LENGTH_SHORT).show();
                                Log.e(TAG, "Error loading filtered properties", propertyTask.getException());
                            }
                        });
                    } else {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(this, "Failed to check reservations", Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "Error loading reservations", task.getException());
                    }
                });
    }


    private void applyAdditionalFilters(Query query, String rooms, String beds, String searchQuery) {
        if (rooms != null) {
            query = query.whereEqualTo("numOfRooms", Integer.parseInt(rooms));
        }

        if (beds != null) {
            query = query.whereEqualTo("numOfBeds", Integer.parseInt(beds));
        }

        if (searchQuery != null && !searchQuery.isEmpty()) {
            query = query.whereGreaterThanOrEqualTo("name", searchQuery)
                    .whereLessThanOrEqualTo("name", searchQuery + "\uf8ff");
        }

        // Fetch properties
        query.get().addOnCompleteListener(propertyTask -> {
            progressBar.setVisibility(View.GONE);
            if (propertyTask.isSuccessful()) {
                propertyList.clear();
                for (DocumentSnapshot document : propertyTask.getResult()) {
                    Property property = document.toObject(Property.class);
                    if (property != null) {
                        property.setId(document.getId()); // Set Firebase document ID
                        propertyList.add(property);
                    }
                }
                propertyAdapter.notifyDataSetChanged();
                Log.d(TAG, "Properties loaded successfully: " + propertyList.size());
            } else {
                Toast.makeText(this, "Failed to load properties", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Error loading properties", propertyTask.getException());
            }
        });

        // Initialize the Profile button
        Button profileButton = findViewById(R.id.profileButton);
        profileButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Navigate to ProfileActivity
                Intent intent = new Intent(UserMainActivity.this, ProfileActivity.class);
                startActivity(intent);
            }
        });
    }



    private String getRadioText(RadioGroup radioGroup, int radioId) {
        if (radioId == -1) return null;
        RadioButton radioButton = radioGroup.findViewById(radioId);
        if (radioButton != null) {
            String text = radioButton.getText().toString();
            // 숫자만 추출
            return text.split(" ")[0];
        }
        return null;
    }


    private void openFilterBottomSheet() {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        bottomSheetDialog.setContentView(R.layout.filter_bottom_sheet);

        RadioGroup roomsRadioGroup = bottomSheetDialog.findViewById(R.id.roomsRadioGroup);
        RadioGroup bedsRadioGroup = bottomSheetDialog.findViewById(R.id.bedsRadioGroup);
        Button applyFiltersButton = bottomSheetDialog.findViewById(R.id.applyFiltersButton);
        selectDateTextView = bottomSheetDialog.findViewById(R.id.selectDateTextView);
        Button selectDateButton = bottomSheetDialog.findViewById(R.id.selectDateButton);

        selectDateButton.setOnClickListener(v -> openDatePicker());

        applyFiltersButton.setOnClickListener(v -> {
            int selectedRoomRadioId = roomsRadioGroup.getCheckedRadioButtonId();
            int selectedBedRadioId = bedsRadioGroup.getCheckedRadioButtonId();

            selectedRooms = getRadioText(roomsRadioGroup, selectedRoomRadioId);
            selectedBeds = getRadioText(bedsRadioGroup, selectedBedRadioId);

            Toast.makeText(this, "Filters Applied", Toast.LENGTH_SHORT).show();
            bottomSheetDialog.dismiss();

            // Reload properties with the selected filters
            loadProperties(startDate, endDate, selectedRooms, selectedBeds, null);
        });

        bottomSheetDialog.show();
    }

    private void openDatePicker() {
        CalendarConstraints.Builder constraintsBuilder = new CalendarConstraints.Builder()
                .setValidator(DateValidatorPointForward.now());

        MaterialDatePicker.Builder<Pair<Long, Long>> datePickerBuilder = MaterialDatePicker.Builder.dateRangePicker()
                .setTitleText("Select Date Range")
                .setCalendarConstraints(constraintsBuilder.build());

        MaterialDatePicker<Pair<Long, Long>> datePicker = datePickerBuilder.build();
        datePicker.addOnPositiveButtonClickListener(selection -> {
            Pair<Long, Long> dateRange = selection;
            startDate = formatDate(dateRange.first);
            endDate = formatDate(dateRange.second);

            selectedDateRange = startDate + " ~ " + endDate;
            selectDateTextView.setText(selectedDateRange);
        });

        datePicker.show(getSupportFragmentManager(), "DATE_PICKER");
    }

    private String formatDate(Long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        return sdf.format(new Date(timestamp));
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
