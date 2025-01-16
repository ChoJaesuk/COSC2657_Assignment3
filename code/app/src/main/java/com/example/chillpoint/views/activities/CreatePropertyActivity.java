package com.example.chillpoint.views.activities;

import android.app.Activity;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ListView; // [새로 추가됨]
import android.widget.ProgressBar;
//import android.widget.Spinner; // [주석 처리됨 - 메인 화면에서 BedType Spinner 제거]
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.chillpoint.R;
import com.example.chillpoint.managers.SessionManager;
import com.example.chillpoint.utils.NavigationSetup;
import com.example.chillpoint.utils.NavigationUtils;
import com.example.chillpoint.views.adapters.ImageAdapter;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.android.gms.maps.model.LatLng;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CreatePropertyActivity extends BaseActivity implements NavigationSetup {

    private static final int IMAGE_PICKER_REQUEST = 100;
    private static final int LOCATION_PICKER_REQUEST = 200; // For map location picking

    private EditText nameEditText, descriptionEditText, addressEditText, priceEditText, roomsEditText, /*numOfBedsEditText,*/ maxGuestsEditText;
    //private Spinner bedTypeSpinner, checkInTimeSpinner, checkOutTimeSpinner; // [주석 처리됨 - 메인에서 bedTypeSpinner 제거]
    private Spinner checkInTimeSpinner, checkOutTimeSpinner; // [새로 정의 - bedTypeSpinner 제외]
    private Button uploadImagesButton, savePropertyButton, pickLocationButton, addBedTypeButton;
    private GridView imagesGridView;
    private ProgressBar progressBar;

    private FirebaseFirestore firestore;
    private FirebaseStorage storage;

    private ArrayList<Uri> imageUris; // To store selected image URIs
    private ArrayList<String> uploadedImageUrls; // To store uploaded image URLs
    private ImageAdapter imageAdapter;

    //private String selectedBedType, selectedCheckInTime, selectedCheckOutTime; // To store selected options (bedType는 팝업에서만 선택)
    private String selectedCheckInTime, selectedCheckOutTime; // [수정됨] bedType는 팝업에서만 관리
    private SessionManager sessionManager; // SessionManager for user data

    private HashMap<String, Integer> bedTypesMap = new HashMap<>(); // To store bed types and their counts

    // [새로 추가됨] 배드 타입 목록을 보여줄 ListView + Adapter
    private ListView bedTypesListView;
    private ArrayAdapter<String> bedTypesListAdapter;
    private ArrayList<String> bedTypesList; // 실제로 화면에 "침대타입 x 개수" 형태로 보여줄 리스트

    // [새로 추가됨] 총 침대 개수를 계산하여 보관할 변수
    private int totalBeds = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_property);
        setupNavigationBar();

        // Initialize UI components
        nameEditText = findViewById(R.id.nameEditText);
        descriptionEditText = findViewById(R.id.descriptionEditText);
        addressEditText = findViewById(R.id.addressEditText);
        priceEditText = findViewById(R.id.priceEditText);
        roomsEditText = findViewById(R.id.roomsEditText);
        //numOfBedsEditText = findViewById(R.id.numOfBedsEditText); // [주석 처리됨 - numOfBedsEditText는 제거됨]
        maxGuestsEditText = findViewById(R.id.maxGuestsEditText);

        //bedTypeSpinner = findViewById(R.id.bedTypeSpinner); // [주석 처리됨 - 메인 화면 bedTypeSpinner 제거]
        checkInTimeSpinner = findViewById(R.id.checkInTimeSpinner);
        checkOutTimeSpinner = findViewById(R.id.checkOutTimeSpinner);

        uploadImagesButton = findViewById(R.id.uploadImagesButton);
        savePropertyButton = findViewById(R.id.savePropertyButton);
        pickLocationButton = findViewById(R.id.pickLocationButton);
        addBedTypeButton = findViewById(R.id.addBedTypeButton);
        imagesGridView = findViewById(R.id.imagesGridView);
        progressBar = findViewById(R.id.progressBar);

        // [새로 추가됨] ListView 초기화
        bedTypesListView = findViewById(R.id.bedTypesListView);
        bedTypesList = new ArrayList<>();
        bedTypesListAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, bedTypesList);
        bedTypesListView.setAdapter(bedTypesListAdapter);

        // Disable addressEditText to make it non-editable
        addressEditText.setFocusable(false);
        addressEditText.setClickable(false);

        // Initialize Firebase services
        firestore = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();

        // Initialize SessionManager
        sessionManager = new SessionManager(this);

        // Initialize image lists
        imageUris = new ArrayList<>();
        uploadedImageUrls = new ArrayList<>();

        // Set up ImageAdapter
        imageAdapter = new ImageAdapter(this, imageUris);
        imagesGridView.setAdapter(imageAdapter);
        imageAdapter.setOnImageRemoveListener(position -> {
            imageUris.remove(position);
            imageAdapter.notifyDataSetChanged();
        });

        // Set listeners
        uploadImagesButton.setOnClickListener(v -> openImagePicker());
        savePropertyButton.setOnClickListener(v -> saveProperty());
        pickLocationButton.setOnClickListener(v -> openLocationPicker());
        addBedTypeButton.setOnClickListener(v -> openAddBedTypeDialog());

        // [주석 처리됨] 메인 화면의 bedTypeSpinner 설정 로직은 제거
        //setupBedTypeSpinner();

        setupCheckInTimeSpinner();
        setupCheckOutTimeSpinner();
    }

    // [주석 처리됨 - 기존 bedType 스피너 초기화 로직(메인화면)은 삭제 대신 주석 보존]
    /*
    private void setupBedTypeSpinner() {
        // Define bed types locally
        String[] bedTypes = {"King Size", "Queen Size", "Double", "Single", "Studio"};

        // Setup ArrayAdapter
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, bedTypes);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        bedTypeSpinner.setAdapter(adapter);

        // Set item selected listener
        bedTypeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedBedType = bedTypes[position];
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selectedBedType = "Unknown";
            }
        });
    }
    */

    private void setupCheckInTimeSpinner() {
        // Define extended check-in times
        String[] checkInTimes = {
                "12:00", "13:00", "14:00", "15:00", "16:00", "17:00", "18:00",
                "19:00", "20:00", "21:00", "22:00", "23:00"
        };

        // Setup ArrayAdapter
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, checkInTimes);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        checkInTimeSpinner.setAdapter(adapter);

        // Set item selected listener
        checkInTimeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedCheckInTime = checkInTimes[position];
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selectedCheckInTime = "14:00"; // Default value
            }
        });
    }

    private void setupCheckOutTimeSpinner() {
        // Define extended check-out times
        String[] checkOutTimes = {
                "06:00", "07:00", "08:00", "09:00", "10:00", "11:00", "12:00"
        };

        // Setup ArrayAdapter
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, checkOutTimes);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        checkOutTimeSpinner.setAdapter(adapter);

        // Set item selected listener
        checkOutTimeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedCheckOutTime = checkOutTimes[position];
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selectedCheckOutTime = "10:00"; // Default value
            }
        });
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        startActivityForResult(Intent.createChooser(intent, "Select Images"), IMAGE_PICKER_REQUEST);
    }

    private void openLocationPicker() {
        Intent intent = new Intent(CreatePropertyActivity.this, MapsActivity.class);
        startActivityForResult(intent, LOCATION_PICKER_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == IMAGE_PICKER_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
            if (data.getClipData() != null) {
                int count = data.getClipData().getItemCount();
                for (int i = 0; i < count; i++) {
                    Uri imageUri = data.getClipData().getItemAt(i).getUri();
                    imageUris.add(imageUri);
                }
            } else if (data.getData() != null) {
                Uri imageUri = data.getData();
                imageUris.add(imageUri);
            }
            imageAdapter.notifyDataSetChanged();
        } else if (requestCode == LOCATION_PICKER_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
            double lat = data.getDoubleExtra("latitude", 0);
            double lng = data.getDoubleExtra("longitude", 0);
            String address = getAddressFromLatLng(new LatLng(lat, lng));
            addressEditText.setText(address);
        }
    }

    private String getAddressFromLatLng(LatLng latLng) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1);
            if (addresses != null && !addresses.isEmpty()) {
                return addresses.get(0).getAddressLine(0);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "Unknown Location";
    }

    private void saveProperty() {
        String name = nameEditText.getText().toString().trim();
        String description = descriptionEditText.getText().toString().trim();
        String address = addressEditText.getText().toString().trim();
        String price = priceEditText.getText().toString().trim();
        String rooms = roomsEditText.getText().toString().trim();
        String maxGuests = maxGuestsEditText.getText().toString().trim();

        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(description) || TextUtils.isEmpty(address) ||
                TextUtils.isEmpty(price) || TextUtils.isEmpty(rooms) || TextUtils.isEmpty(maxGuests)) {
            Toast.makeText(CreatePropertyActivity.this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (imageUris.isEmpty()) {
            Toast.makeText(CreatePropertyActivity.this, "Please upload at least one image", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        uploadImages(new UploadImagesCallback() {
            @Override
            public void onUploadComplete(ArrayList<String> urls) {
                // [수정됨] numOfBedsEditText 대신, 자동 계산된 totalBeds를 사용
                saveToFirestore(
                        name,
                        description,
                        address,
                        Double.parseDouble(price),
                        Integer.parseInt(rooms),
                        Integer.parseInt(maxGuests),
                        selectedCheckInTime,
                        selectedCheckOutTime,
                        urls
                );
            }

            @Override
            public void onUploadFailed(String errorMessage) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(CreatePropertyActivity.this, "Image upload failed: " + errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void uploadImages(UploadImagesCallback callback) {
        uploadedImageUrls.clear();
        for (Uri uri : imageUris) {
            String fileName = "property_images/" + System.currentTimeMillis() + "_" + uri.getLastPathSegment();
            StorageReference reference = storage.getReference(fileName);
            reference.putFile(uri).addOnSuccessListener(taskSnapshot ->
                    reference.getDownloadUrl().addOnSuccessListener(url -> {
                        uploadedImageUrls.add(url.toString());
                        if (uploadedImageUrls.size() == imageUris.size()) {
                            callback.onUploadComplete(uploadedImageUrls);
                        }
                    }).addOnFailureListener(e -> callback.onUploadFailed(e.getMessage()))
            ).addOnFailureListener(e -> callback.onUploadFailed(e.getMessage()));
        }
    }

    private void openAddBedTypeDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_bed_type, null);

        // [주석 처리됨] 기존 EditText bedTypeEditText는 제거 또는 주석
//        EditText bedTypeEditText = dialogView.findViewById(R.id.bedTypeEditText);
        // [새로 추가됨] 팝업에 뜨는 Spinner
        Spinner dialogBedTypeSpinner = dialogView.findViewById(R.id.dialogBedTypeSpinner);

        EditText bedCountEditText = dialogView.findViewById(R.id.bedCountEditText);
        Button addButton = dialogView.findViewById(R.id.addButton);

        // [새로 추가됨] 팝업에 표시할 침대 타입 리스트
        String[] bedTypesForDialog = {"King Size", "Queen Size", "Double", "Single", "Studio"};

        // [새로 추가됨] 팝업 스피너 세팅
        ArrayAdapter<String> bedTypeDialogAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                bedTypesForDialog
        );
        bedTypeDialogAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        dialogBedTypeSpinner.setAdapter(bedTypeDialogAdapter);

        // [새로 추가됨] EditText는 주석만 유지(아래를 참고).
        // 실제로는 free text 입력 대신 Spinner에서 선택하여 사용.
        /*
        bedTypeEditText.setVisibility(View.GONE); // 필요하다면 이렇게 숨길 수도 있습니다.
        */

        android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(this)
                .setTitle("Add Bed Type")
                .setView(dialogView)
                .create();

        addButton.setOnClickListener(v -> {
            // [수정됨] bedTypeEditText 대신 dialogBedTypeSpinner로 bedType을 선택
            String selectedBedType = dialogBedTypeSpinner.getSelectedItem().toString().trim();

            // 만약 기존의 EditText를 활용하고 싶다면 아래처럼 사용 가능(현재는 미사용):
            // String bedType = bedTypeEditText.getText().toString().trim();

            String bedCountStr = bedCountEditText.getText().toString().trim();

            if (TextUtils.isEmpty(selectedBedType) || TextUtils.isEmpty(bedCountStr)) {
                Toast.makeText(CreatePropertyActivity.this, "Please enter bed type and count", Toast.LENGTH_SHORT).show();
                return;
            }

            int bedCount = Integer.parseInt(bedCountStr);

            // HashMap 업데이트
            bedTypesMap.put(selectedBedType, bedTypesMap.getOrDefault(selectedBedType, 0) + bedCount);

            // 총 침대수 업데이트
            updateTotalBeds();

            // [새로 추가됨] ListView에 "침대타입 x 개수" 형태로 표시
            // 만약 이미 같은 타입이 들어갔다면 누적된 개수가 표시됨
            refreshBedTypesList();

            dialog.dismiss();
        });

        dialog.show();
    }

    private void updateTotalBeds() {
        totalBeds = 0;
        for (int count : bedTypesMap.values()) {
            totalBeds += count;
        }
        // [주석 처리됨] numOfBedsEditText.setText(String.valueOf(totalBeds));
        // 더 이상 EditText로 표시하지 않고, totalBeds만 내부적으로 관리
    }

    // [새로 추가됨] 현재 bedTypesMap을 바탕으로 ListView에 반영
    private void refreshBedTypesList() {
        bedTypesList.clear();
        for (Map.Entry<String, Integer> entry : bedTypesMap.entrySet()) {
            String type = entry.getKey();
            int count = entry.getValue();
            bedTypesList.add(type + " x " + count);
        }
        bedTypesListAdapter.notifyDataSetChanged();
    }

    private void saveToFirestore(String name, String description, String address, double price, int rooms,
                                 int maxGuests, String checkInTime, String checkOutTime, ArrayList<String> imageUrls) {
        String userId = sessionManager.getUserId(); // Changed to SessionManager
        String createdAt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());

        Map<String, Object> property = new HashMap<>();
        property.put("name", name);
        property.put("description", description);
        property.put("address", address);
        property.put("pricePerNight", price);
        property.put("numOfRooms", rooms);

        // [수정됨] 자동 계산된 totalBeds를 저장
        //property.put("numOfBeds", Integer.parseInt(numOfBedsEditText.getText().toString())); // [주석 처리됨]
        property.put("numOfBeds", totalBeds); // [새로 추가됨]

        property.put("maxNumOfGuests", maxGuests);
        property.put("checkInTime", checkInTime);
        property.put("checkOutTime", checkOutTime);
        property.put("bedTypes", bedTypesMap); // Save bed types map
        property.put("createdAt", createdAt);
        property.put("updatedAt", createdAt);
        property.put("userId", userId);
        property.put("images", imageUrls);

        firestore.collection("Properties").add(property).addOnSuccessListener(documentReference -> {
            progressBar.setVisibility(View.GONE);
            Toast.makeText(CreatePropertyActivity.this, "Property created successfully", Toast.LENGTH_SHORT).show();

            // 명시적으로 UserMainActivity로 가지 않고, 현재 Activity를 종료하고 이전 Activity로 돌아감
            finish();
        }).addOnFailureListener(e -> {
            progressBar.setVisibility(View.GONE);
            Toast.makeText(CreatePropertyActivity.this, "Failed to create property: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    interface UploadImagesCallback {
        void onUploadComplete(ArrayList<String> urls);

        void onUploadFailed(String errorMessage);
    }
    @Override
    public void setupNavigationBar() {
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setSelectedItemId(R.id.nav_profile);
        NavigationUtils.handleBottomNavigation(this, bottomNavigationView);
    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {
        super.onPointerCaptureChanged(hasCapture);
    }
}
