package com.example.chillpoint.views.activities;

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
import com.example.chillpoint.views.adapters.PropertyAdapter;
import com.example.chillpoint.views.models.Property;
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

public class UserMainActivity extends AppCompatActivity {
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

        recyclerView = findViewById(R.id.recyclerView);
        progressBar = findViewById(R.id.progressBar);
        filterButton = findViewById(R.id.filterButton);
        searchButton = findViewById(R.id.searchButton);
        searchEditText = findViewById(R.id.searchEditText);

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
    }

    private void loadProperties(String startDate, String endDate, String rooms, String beds, String searchQuery) {
        progressBar.setVisibility(View.VISIBLE);

        Query query = firestore.collection("Properties");
        Log.d("UserMainActivity", "Initial Query: " + query);

        // 날짜 범위 필터
        if (startDate != null && endDate != null) {
            query = query.whereGreaterThanOrEqualTo("availableFrom", startDate)
                    .whereLessThanOrEqualTo("availableTo", endDate);
            Log.d("UserMainActivity", "Added date filter: Start Date = " + startDate + ", End Date = " + endDate);
        }

        // 방 개수 필터
        if (rooms != null) {
            try {
                int roomCount = Integer.parseInt(rooms); // 숫자로 변환
                query = query.whereEqualTo("numOfRooms", roomCount);
                Log.d("UserMainActivity", "Added rooms filter: " + roomCount);
            } catch (NumberFormatException e) {
                Log.e("UserMainActivity", "Invalid room number format: " + rooms, e);
            }
        }

        // 침대 개수 필터
        if (beds != null) {
            try {
                int bedCount = Integer.parseInt(beds); // 숫자로 변환
                query = query.whereEqualTo("numOfBeds", bedCount);
                Log.d("UserMainActivity", "Added beds filter: " + bedCount);
            } catch (NumberFormatException e) {
                Log.e("UserMainActivity", "Invalid bed number format: " + beds, e);
            }
        }

        // 검색어 필터
        if (searchQuery != null && !searchQuery.isEmpty()) {
            query = query.whereGreaterThanOrEqualTo("name", searchQuery)
                    .whereLessThanOrEqualTo("name", searchQuery + "\uf8ff");
            Log.d("UserMainActivity", "Added search query filter: " + searchQuery);
        }

        query.get().addOnCompleteListener(task -> {
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
                Log.d("UserMainActivity", "Properties loaded successfully: " + propertyList.size());
            } else {
                Toast.makeText(this, "Failed to load properties", Toast.LENGTH_SHORT).show();
                Log.e("UserMainActivity", "Error loading properties", task.getException());
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

}
