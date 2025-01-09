package com.example.chillpoint.views.activities;

import android.app.Activity;
import android.content.Intent;
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
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.chillpoint.R;
import com.example.chillpoint.views.adapters.ImageAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class UpdatePropertyActivity extends AppCompatActivity {

    private static final int IMAGE_PICKER_REQUEST = 100;

    private EditText nameEditText, descriptionEditText, addressEditText, priceEditText, roomsEditText, numOfBedsEditText, maxGuestsEditText;
    private Spinner bedTypeSpinner, checkInTimeSpinner, checkOutTimeSpinner;
    private Button uploadImagesButton, updatePropertyButton;
    private GridView imagesGridView;
    private ProgressBar progressBar;

    private FirebaseAuth auth;
    private FirebaseFirestore firestore;
    private FirebaseStorage storage;

    private ArrayList<Uri> imageUris; // New images to upload
    private ArrayList<String> uploadedImageUrls; // Previously uploaded images
    private ImageAdapter imageAdapter;

    private String selectedBedType, selectedCheckInTime, selectedCheckOutTime; // Selected options
    private String propertyId; // ID of the property to update

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_update_property);

        // Initialize UI components
        nameEditText = findViewById(R.id.nameEditText);
        descriptionEditText = findViewById(R.id.descriptionEditText);
        addressEditText = findViewById(R.id.addressEditText);
        priceEditText = findViewById(R.id.priceEditText);
        roomsEditText = findViewById(R.id.roomsEditText);
        numOfBedsEditText = findViewById(R.id.numOfBedsEditText);
        maxGuestsEditText = findViewById(R.id.maxGuestsEditText);
        bedTypeSpinner = findViewById(R.id.bedTypeSpinner);
        checkInTimeSpinner = findViewById(R.id.checkInTimeSpinner);
        checkOutTimeSpinner = findViewById(R.id.checkOutTimeSpinner);
        uploadImagesButton = findViewById(R.id.uploadImagesButton);
        updatePropertyButton = findViewById(R.id.savePropertyButton);
        imagesGridView = findViewById(R.id.imagesGridView); // Ensure this ID matches your XML
        progressBar = findViewById(R.id.progressBar);

        // Initialize Firebase services
        auth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();

        // Initialize image lists
        imageUris = new ArrayList<>();
        uploadedImageUrls = new ArrayList<>();

        // Get property ID and existing data from Intent
        propertyId = getIntent().getStringExtra("propertyId");
        loadPropertyData();

        // Set up ImageAdapter
        imageAdapter = new ImageAdapter(this, imageUris);
        imagesGridView.setAdapter(imageAdapter); // Fix for NullPointerException

        // Set listeners
        uploadImagesButton.setOnClickListener(v -> openImagePicker());
        updatePropertyButton.setOnClickListener(v -> updateProperty());

        // Setup spinners for bed types, check-in, and check-out times
        setupBedTypeSpinner();
        setupCheckInTimeSpinner();
        setupCheckOutTimeSpinner();
    }

    private void loadPropertyData() {
        // Load property data from Firestore
        firestore.collection("Properties").document(propertyId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // Populate UI with existing property data
                        nameEditText.setText(documentSnapshot.getString("name"));
                        descriptionEditText.setText(documentSnapshot.getString("description"));
                        addressEditText.setText(documentSnapshot.getString("address"));
                        priceEditText.setText(String.valueOf(documentSnapshot.getDouble("pricePerNight")));
                        roomsEditText.setText(String.valueOf(documentSnapshot.getLong("numOfRooms")));
                        numOfBedsEditText.setText(String.valueOf(documentSnapshot.getLong("numOfBeds")));
                        maxGuestsEditText.setText(String.valueOf(documentSnapshot.getLong("maxNumOfGuests")));

                        // Populate image list
                        ArrayList<String> images = (ArrayList<String>) documentSnapshot.get("images");
                        if (images != null) {
                            uploadedImageUrls.addAll(images);
                        }

                        // Notify adapter
                        imageAdapter.notifyDataSetChanged();
                    } else {
                        Toast.makeText(UpdatePropertyActivity.this, "Property not found.", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(UpdatePropertyActivity.this, "Error loading property: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void setupBedTypeSpinner() {
        String[] bedTypes = {"King Size", "Queen Size", "Double", "Single", "Studio"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, bedTypes);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        bedTypeSpinner.setAdapter(adapter);
    }

    private void setupCheckInTimeSpinner() {
        String[] checkInTimes = {"12:00", "13:00", "14:00", "15:00", "16:00", "17:00"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, checkInTimes);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        checkInTimeSpinner.setAdapter(adapter);
    }

    private void setupCheckOutTimeSpinner() {
        String[] checkOutTimes = {"06:00", "07:00", "08:00", "09:00", "10:00", "11:00"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, checkOutTimes);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        checkOutTimeSpinner.setAdapter(adapter);
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        startActivityForResult(Intent.createChooser(intent, "Select Images"), IMAGE_PICKER_REQUEST);
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
        }
    }

    private void updateProperty() {
        String name = nameEditText.getText().toString().trim();
        String description = descriptionEditText.getText().toString().trim();
        String address = addressEditText.getText().toString().trim();
        String price = priceEditText.getText().toString().trim();
        String rooms = roomsEditText.getText().toString().trim();
        String numOfBeds = numOfBedsEditText.getText().toString().trim();
        String maxGuests = maxGuestsEditText.getText().toString().trim();

        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(description) || TextUtils.isEmpty(address) ||
                TextUtils.isEmpty(price) || TextUtils.isEmpty(rooms) || TextUtils.isEmpty(numOfBeds) || TextUtils.isEmpty(maxGuests)) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);

        Map<String, Object> updatedData = new HashMap<>();
        updatedData.put("name", name);
        updatedData.put("description", description);
        updatedData.put("address", address);
        updatedData.put("pricePerNight", Double.parseDouble(price));
        updatedData.put("numOfRooms", Integer.parseInt(rooms));
        updatedData.put("numOfBeds", Integer.parseInt(numOfBeds));
        updatedData.put("maxNumOfGuests", Integer.parseInt(maxGuests));
        updatedData.put("updatedAt", System.currentTimeMillis());

        firestore.collection("Properties").document(propertyId).update(updatedData)
                .addOnSuccessListener(aVoid -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(UpdatePropertyActivity.this, "Property updated successfully.", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(UpdatePropertyActivity.this, "Error updating property: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
