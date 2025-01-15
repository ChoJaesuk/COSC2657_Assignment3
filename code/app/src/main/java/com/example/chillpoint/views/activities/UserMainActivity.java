package com.example.chillpoint.views.activities;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
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

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.util.Pair;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chillpoint.R;
import com.example.chillpoint.managers.SessionManager;
import com.example.chillpoint.utils.NavigationSetup;
import com.example.chillpoint.utils.NavigationUtils;
import com.example.chillpoint.views.adapters.PropertyAdapter;
import com.example.chillpoint.views.models.Property;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Tasks;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.DateValidatorPointForward;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserMainActivity extends AppCompatActivity implements NavigationSetup {
    private static final String TAG = "UserMainActivity";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 100;

    private RecyclerView recyclerView;
    private PropertyAdapter propertyAdapter;
    private ArrayList<Property> propertyList;
    private FirebaseFirestore firestore;
    private ProgressBar progressBar;
    private ImageButton filterButton, searchButton;
    private EditText searchEditText;
    private TextView selectDateTextView;

    // 날짜/필터 선택값
    private String startDate = null; // "yyyy-MM-dd"
    private String endDate = null;   // "yyyy-MM-dd"
    private String selectedRooms = null;
    private String selectedBeds = null;
    private String selectedDateRange = "None";
    private Button viewVoucherListButton;
    private double userLat = 0.0;
    private double userLng = 0.0;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_main);

        setupNavigationBar();
        checkLocationPermission();

        // 세션 데이터 로드
        SessionManager sessionManager = new SessionManager(this);
        String userId = sessionManager.getUserId();
        String role = sessionManager.getRole();
        String username = sessionManager.getUsername();
        Log.d("SessionManager", "Loaded session: userId=" + userId + ", role=" + role + ", username=" + username);

        // 세션 검증
        if (userId == null || role == null || username == null) {
            Toast.makeText(this, "Failed to load user session. Please log in again.", Toast.LENGTH_SHORT).show();
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
        viewVoucherListButton = findViewById(R.id.viewVoucherListButton);

        propertyList = new ArrayList<>();
        propertyAdapter = new PropertyAdapter(this, propertyList);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(propertyAdapter);

        firestore = FirebaseFirestore.getInstance();

        // 처음에는 모든 Properties 불러오기
        loadProperties(null, null, null, null, null);

        // ========== 검색 버튼 ==========
        searchButton.setOnClickListener(v -> {
            String queryText = searchEditText.getText().toString().trim();
            loadProperties(startDate, endDate, selectedRooms, selectedBeds, queryText);

            // [옵션] 검색 완료 후 필터 옵션 초기화
            startDate = null;
            endDate = null;
            selectedRooms = null;
            selectedBeds = null;
            selectedDateRange = "None";
            // 검색창도 초기화할지 여부는 자유
            //searchEditText.setText("");
        });

        // Set click listener for View Voucher List button
        viewVoucherListButton.setOnClickListener(v -> {
            Intent intent = new Intent(UserMainActivity.this, VoucherListActivity.class);
            startActivity(intent);
        });

        // ========== 필터 버튼 (BottomSheetDialog) ==========
        filterButton.setOnClickListener(v -> openFilterBottomSheet());
    }

    /**
     * 숙소 목록 불러오기
     * - 날짜 필터 -> reservations 콜렉션
     * - excludedPropertyIds -> Properties 콜렉션에서 제외
     * - 방/침대 필터 + (name,address) 검색
     */
    private void loadProperties(String startDate, String endDate,
                                String rooms, String beds,
                                String searchText) {
        progressBar.setVisibility(View.VISIBLE);

        ArrayList<String> excludedPropertyIds = new ArrayList<>();

        // 날짜 필터가 있는 경우: (fromDate <= endDate) && (toDate >= startDate)
        if (startDate != null && endDate != null) {
            firestore.collection("reservations")
                    .whereLessThanOrEqualTo("fromDate", endDate)
                    .whereGreaterThanOrEqualTo("toDate", startDate)
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            for (DocumentSnapshot doc : task.getResult()) {
                                String propertyId = doc.getString("propertyId");
                                if (propertyId != null) {
                                    excludedPropertyIds.add(propertyId);
                                }
                            }
                            Log.d(TAG, "Excluded property IDs: " + excludedPropertyIds);

                            // Properties 전체
                            Query propertyQuery = firestore.collection("Properties");
                            // 문서 ID와 excludedPropertyIds가 일치하는 건 제외
                            if (!excludedPropertyIds.isEmpty()) {
                                propertyQuery = propertyQuery.whereNotIn(FieldPath.documentId(), excludedPropertyIds);
                            }

                            applyAdditionalFilters(propertyQuery, rooms, beds, searchText);
                        } else {
                            progressBar.setVisibility(View.GONE);
                            Toast.makeText(this, "Failed to fetch reservations", Toast.LENGTH_SHORT).show();
                            Log.e(TAG, "Error fetching reservations", task.getException());
                        }
                    });
        } else {
            // 날짜 필터가 없으면 전체
            Query propertyQuery = firestore.collection("Properties");
            applyAdditionalFilters(propertyQuery, rooms, beds, searchText);
        }
    }

    /**
     * - 방 개수/침대 개수 필터
     * - name or address 검색
     */
    private void applyAdditionalFilters(Query baseQuery,
                                        String rooms, String beds,
                                        String searchText) {
        // 방 개수 필터
        if (rooms != null) {
            try {
                int rCount = Integer.parseInt(rooms);
                baseQuery = baseQuery.whereEqualTo("numOfRooms", rCount);
            } catch (NumberFormatException e) {
                Log.e(TAG, "Invalid rooms", e);
            }
        }
        // 침대 개수 필터
        if (beds != null) {
            try {
                int bCount = Integer.parseInt(beds);
                baseQuery = baseQuery.whereEqualTo("numOfBeds", bCount);
            } catch (NumberFormatException e) {
                Log.e(TAG, "Invalid beds", e);
            }
        }

        // 이제 name OR address 검색을 위해 2번 쿼리
        if (searchText != null && !searchText.isEmpty()) {
            searchByNameOrAddress(baseQuery, searchText, results -> {
                progressBar.setVisibility(View.GONE);
                if (results == null) {
                    Toast.makeText(this, "Failed to load properties for search", Toast.LENGTH_SHORT).show();
                    return;
                }
                propertyList.clear();
                for (DocumentSnapshot doc : results) {
                    Property property = doc.toObject(Property.class);
                    if (property != null) {
                        property.setId(doc.getId());
                        propertyList.add(property);
                    }
                }
                propertyAdapter.notifyDataSetChanged();
            });
        } else {
            // name/address 검색어가 없으면 => 그대로 1번 쿼리만
            baseQuery.get().addOnCompleteListener(task -> {
                progressBar.setVisibility(View.GONE);
                if (task.isSuccessful()) {
                    propertyList.clear();
                    for (DocumentSnapshot doc : task.getResult()) {
                        Property property = doc.toObject(Property.class);
                        if (property != null) {
                            property.setId(doc.getId());
                            propertyList.add(property);
                        }
                    }
                    geocodeAndSortProperties(propertyList);
                    propertyAdapter.notifyDataSetChanged();
                } else {
                    Toast.makeText(this, "Failed to load properties", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Error loading properties", task.getException());
                }
            });
        }
    }

    /**
     * "이 baseQuery를 만족" + ( name LIKE query OR address LIKE query )
     * => 2번 쿼리(name, address) => 합침
     */
    private void searchByNameOrAddress(Query baseQuery,
                                       String queryText,
                                       OnSearchCompleteListener listener) {
        Query nameQuery = baseQuery.whereGreaterThanOrEqualTo("name", queryText)
                .whereLessThanOrEqualTo("name", queryText + "\uf8ff");

        Query addressQuery = baseQuery.whereGreaterThanOrEqualTo("address", queryText)
                .whereLessThanOrEqualTo("address", queryText + "\uf8ff");

        // 2개의 쿼리를 병렬로 실행
        nameQuery.get().continueWithTask(task1 -> {
            QuerySnapshot nameSnap = task1.getResult();
            return addressQuery.get().continueWith(task2 -> {
                QuerySnapshot addressSnap = task2.getResult();

                // 두 결과 합치기
                List<DocumentSnapshot> combined = new ArrayList<>();
                if (nameSnap != null) {
                    combined.addAll(nameSnap.getDocuments());
                }
                if (addressSnap != null) {
                    combined.addAll(addressSnap.getDocuments());
                }

                // 중복 제거
                Map<String, DocumentSnapshot> uniqueMap = new HashMap<>();
                for (DocumentSnapshot doc : combined) {
                    uniqueMap.put(doc.getId(), doc);
                }
                return new ArrayList<>(uniqueMap.values());
            });
        }).addOnSuccessListener(list -> {
            // 검색 성공 -> callback
            listener.onSearchComplete(list);
        }).addOnFailureListener(e -> {
            // 검색 실패 -> null 로 전달
            listener.onSearchComplete(null);
        });
    }

    // 콜백 인터페이스
    interface OnSearchCompleteListener {
        void onSearchComplete(List<DocumentSnapshot> results);
    }

    /**
     * 필터 BottomSheet
     */
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

            Toast.makeText(this, "Filters saved. Press Search to apply.", Toast.LENGTH_SHORT).show();
            bottomSheetDialog.dismiss();
        });

        bottomSheetDialog.show();
    }

    private void openDatePicker() {
        CalendarConstraints.Builder constraintsBuilder =
                new CalendarConstraints.Builder().setValidator(DateValidatorPointForward.now());

        MaterialDatePicker.Builder<Pair<Long, Long>> datePickerBuilder =
                MaterialDatePicker.Builder.dateRangePicker()
                        .setTitleText("Select Date Range")
                        .setCalendarConstraints(constraintsBuilder.build());

        MaterialDatePicker<Pair<Long, Long>> datePicker = datePickerBuilder.build();
        datePicker.addOnPositiveButtonClickListener(selection -> {
            if (selection != null) {
                long startMillis = selection.first;
                long endMillis = selection.second;
                startDate = formatDate(startMillis);
                endDate = formatDate(endMillis);

                selectedDateRange = startDate + " ~ " + endDate;
                selectDateTextView.setText(selectedDateRange);
            }
        });

        datePicker.show(getSupportFragmentManager(), "DATE_PICKER");
    }

    private String formatDate(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }

    private String getRadioText(RadioGroup radioGroup, int radioId) {
        if (radioId == -1) return null;
        RadioButton radioButton = radioGroup.findViewById(radioId);
        if (radioButton != null) {
            String text = radioButton.getText().toString();
            return text.split(" ")[0]; // e.g. "2 Rooms" -> "2"
        }
        return null;
    }

    // 위치 권한 확인 및 요청
    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            // 권한이 부여되지 않은 경우 요청
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            // 권한이 이미 부여된 경우
            fetchUserLocation();
        }
    }

    // 권한 요청 결과 처리
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 권한이 허용된 경우
                fetchUserLocation();
            } else {
                // 권한이 거부된 경우
                Toast.makeText(this, "Location permission is required to use this feature", Toast.LENGTH_SHORT).show();
            }
        }
    }


    private void fetchUserLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "Location permission not granted");
            return;
        }

        FusedLocationProviderClient fusedLocationClient =
                LocationServices.getFusedLocationProviderClient(this);

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        userLat = location.getLatitude();
                        userLng = location.getLongitude();
                        Log.d(TAG, "User location: " + userLat + ", " + userLng);
                    } else {
                        Log.e(TAG, "Location is null. Make sure GPS is on.");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to get user location", e);
                });
    }


    private void geocodeAndSortProperties(ArrayList<Property> properties) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());

        for (Property prop : properties) {
            String addressStr = prop.getAddress();
            if (addressStr != null && !addressStr.isEmpty()) {
                try {
                    List<Address> addresses = geocoder.getFromLocationName(addressStr, 1);
                    if (addresses != null && !addresses.isEmpty()) {
                        Address location = addresses.get(0);
                        double lat = location.getLatitude();
                        double lng = location.getLongitude();
                        prop.setLatitude(lat);
                        prop.setLongitude(lng);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Geocoding failed for address: " + addressStr, e);
                }
            }
        }

        // 이제 각 property에 lat/lng가 세팅되었으니, 사용자 위치와의 거리 계산
        Collections.sort(properties, (p1, p2) -> {
            double dist1 = computeDistance(userLat, userLng, p1.getLatitude(), p1.getLongitude());
            double dist2 = computeDistance(userLat, userLng, p2.getLatitude(), p2.getLongitude());
            return Double.compare(dist1, dist2); // ASC
        });
    }

    private double computeDistance(double lat1, double lng1, double lat2, double lng2) {
        // 보통 Haversine formula 등 사용
        // 간단히 Android의 Location.distanceBetween() 써도 됨
        float[] results = new float[1];
        Location.distanceBetween(lat1, lng1, lat2, lng2, results);
        return results[0]; // meters
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
