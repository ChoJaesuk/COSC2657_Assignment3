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
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.chillpoint.R;
import com.example.chillpoint.managers.SessionManager;
import com.example.chillpoint.views.adapters.ImageAdapter;
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

public class CreatePropertyActivity extends AppCompatActivity {

    private static final int IMAGE_PICKER_REQUEST = 100;
    private static final int LOCATION_PICKER_REQUEST = 200; // For map location picking

    private EditText nameEditText, descriptionEditText, addressEditText, priceEditText, roomsEditText, numOfBedsEditText, maxGuestsEditText;
    private Spinner bedTypeSpinner, checkInTimeSpinner, checkOutTimeSpinner;
    private Button uploadImagesButton, savePropertyButton, pickLocationButton;
    private GridView imagesGridView;
    private ProgressBar progressBar;

    private FirebaseFirestore firestore;
    private FirebaseStorage storage;

    private ArrayList<Uri> imageUris; // To store selected image URIs
    private ArrayList<String> uploadedImageUrls; // To store uploaded image URLs
    private ImageAdapter imageAdapter;

    private String selectedBedType, selectedCheckInTime, selectedCheckOutTime; // To store selected options

    private SessionManager sessionManager; // SessionManager for user data

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_property);

        // Initialize UI components
        nameEditText = findViewById(R.id.nameEditText);
        descriptionEditText = findViewById(R.id.descriptionEditText);
        addressEditText = findViewById(R.id.addressEditText);
        priceEditText = findViewById(R.id.priceEditText);
        roomsEditText = findViewById(R.id.roomsEditText);
        numOfBedsEditText = findViewById(R.id.numOfBedsEditText); // Number of Beds Input
        maxGuestsEditText = findViewById(R.id.maxGuestsEditText);
        bedTypeSpinner = findViewById(R.id.bedTypeSpinner);
        checkInTimeSpinner = findViewById(R.id.checkInTimeSpinner);
        checkOutTimeSpinner = findViewById(R.id.checkOutTimeSpinner);
        uploadImagesButton = findViewById(R.id.uploadImagesButton);
        savePropertyButton = findViewById(R.id.savePropertyButton);
        pickLocationButton = findViewById(R.id.pickLocationButton);
        imagesGridView = findViewById(R.id.imagesGridView);
        progressBar = findViewById(R.id.progressBar);

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

        // Setup spinners for bed types, check-in, and check-out times
        setupBedTypeSpinner();
        setupCheckInTimeSpinner();
        setupCheckOutTimeSpinner();
    }

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
        String numOfBeds = numOfBedsEditText.getText().toString().trim();
        String maxGuests = maxGuestsEditText.getText().toString().trim();

        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(description) || TextUtils.isEmpty(address) ||
                TextUtils.isEmpty(price) || TextUtils.isEmpty(rooms) || TextUtils.isEmpty(numOfBeds) || TextUtils.isEmpty(maxGuests)) {
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
                saveToFirestore(name, description, address, Double.parseDouble(price),
                        Integer.parseInt(rooms), Integer.parseInt(numOfBeds), Integer.parseInt(maxGuests), selectedCheckInTime, selectedCheckOutTime, selectedBedType, urls);
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

    private void saveToFirestore(String name, String description, String address, double price, int rooms, int numOfBeds, int maxGuests, String checkInTime, String checkOutTime, String bedType, ArrayList<String> imageUrls) {
        String userId = sessionManager.getUserId(); // Changed to SessionManager
        String createdAt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());

        Map<String, Object> property = new HashMap<>();
        property.put("name", name);
        property.put("description", description);
        property.put("address", address);
        property.put("pricePerNight", price);
        property.put("numOfRooms", rooms);
        property.put("numOfBeds", numOfBeds);
        property.put("maxNumOfGuests", maxGuests);
        property.put("checkInTime", checkInTime);
        property.put("checkOutTime", checkOutTime);
        property.put("bedType", bedType);
        property.put("createdAt", createdAt);
        property.put("updatedAt", createdAt);
        property.put("userId", userId);
        property.put("images", imageUrls);

        firestore.collection("Properties").add(property).addOnSuccessListener(documentReference -> {
            progressBar.setVisibility(View.GONE);
            Toast.makeText(CreatePropertyActivity.this, "Property created successfully", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(CreatePropertyActivity.this, UserMainActivity.class);
            startActivity(intent);
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
}
