package com.example.chillpoint.views.activities;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ListView;
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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class UpdatePropertyActivity extends AppCompatActivity {

    private static final int IMAGE_PICKER_REQUEST = 100;
    private static final int LOCATION_PICKER_REQUEST = 200;

    private EditText nameEditText, descriptionEditText, addressEditText, priceEditText, roomsEditText, maxGuestsEditText;
    private Spinner checkInTimeSpinner, checkOutTimeSpinner;
    private Button uploadImagesButton, updatePropertyButton, pickLocationButton, addBedTypeButton;
    private GridView imagesGridView;
    private ProgressBar progressBar;

    private FirebaseFirestore firestore;
    private FirebaseStorage storage;

    private ArrayList<Uri> imageUris;
    private ArrayList<String> uploadedImageUrls;
    private ImageAdapter imageAdapter;

    private String selectedCheckInTime, selectedCheckOutTime;
    private SessionManager sessionManager;

    private HashMap<String, Integer> bedTypesMap = new HashMap<>();
    private ListView bedTypesListView;
    private BedTypeAdapter bedTypesListAdapter;
    private ArrayList<BedTypeItem> bedTypesList;

    private int totalBeds = 0;
    private String propertyId;

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
        maxGuestsEditText = findViewById(R.id.maxGuestsEditText);

        checkInTimeSpinner = findViewById(R.id.checkInTimeSpinner);
        checkOutTimeSpinner = findViewById(R.id.checkOutTimeSpinner);

        uploadImagesButton = findViewById(R.id.uploadImagesButton);
        updatePropertyButton = findViewById(R.id.savePropertyButton);
        pickLocationButton = findViewById(R.id.pickLocationButton);
        addBedTypeButton = findViewById(R.id.addBedTypeButton);

        imagesGridView = findViewById(R.id.imagesGridView);
        progressBar = findViewById(R.id.progressBar);

        bedTypesListView = findViewById(R.id.bedTypesListView);
        bedTypesList = new ArrayList<>();
        bedTypesListAdapter = new BedTypeAdapter();
        bedTypesListView.setAdapter(bedTypesListAdapter);

        // Initialize Firebase services
        firestore = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        sessionManager = new SessionManager(this);

        // Initialize image lists
        imageUris = new ArrayList<>();
        uploadedImageUrls = new ArrayList<>();

        // Get property ID from Intent
        propertyId = getIntent().getStringExtra("propertyId");
        loadPropertyData();

        // Set up ImageAdapter
        imageAdapter = new ImageAdapter(this, imageUris);
        imagesGridView.setAdapter(imageAdapter);

        // Set listeners
        uploadImagesButton.setOnClickListener(v -> openImagePicker());
        updatePropertyButton.setOnClickListener(v -> updateProperty());
        pickLocationButton.setOnClickListener(v -> openLocationPicker());
        addBedTypeButton.setOnClickListener(v -> openAddBedTypeDialog());

        setupCheckInTimeSpinner();
        setupCheckOutTimeSpinner();
    }

    private void loadPropertyData() {
        firestore.collection("Properties").document(propertyId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        nameEditText.setText(documentSnapshot.getString("name"));
                        descriptionEditText.setText(documentSnapshot.getString("description"));
                        addressEditText.setText(documentSnapshot.getString("address"));
                        priceEditText.setText(String.valueOf(documentSnapshot.getDouble("pricePerNight")));
                        roomsEditText.setText(String.valueOf(documentSnapshot.getLong("numOfRooms")));
                        maxGuestsEditText.setText(String.valueOf(documentSnapshot.getLong("maxNumOfGuests")));

                        Map<String, Long> bedTypes = (Map<String, Long>) documentSnapshot.get("bedTypes");
                        if (bedTypes != null) {
                            bedTypes.forEach((key, value) -> bedTypesMap.put(key, value.intValue()));
                            updateTotalBeds();
                            refreshBedTypesList();
                        }

                        ArrayList<String> images = (ArrayList<String>) documentSnapshot.get("images");
                        if (images != null) {
                            uploadedImageUrls.addAll(images);
                        }

                        imageAdapter.notifyDataSetChanged();
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
        Intent intent = new Intent(UpdatePropertyActivity.this, MapsActivity.class);
        startActivityForResult(intent, LOCATION_PICKER_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Handle image picker result
        if (requestCode == IMAGE_PICKER_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
            // ... 이미지 선택 결과 처리 (생략)
        }

        // Handle location picker result
        else if (requestCode == LOCATION_PICKER_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
            double lat = data.getDoubleExtra("latitude", 0);
            double lng = data.getDoubleExtra("longitude", 0);
            String address = data.getStringExtra("address");
            addressEditText.setText(address);
        }
    }

    private void updateProperty() {
        String name = nameEditText.getText().toString().trim();
        String description = descriptionEditText.getText().toString().trim();
        String address = addressEditText.getText().toString().trim();
        String price = priceEditText.getText().toString().trim();
        String rooms = roomsEditText.getText().toString().trim();
        String maxGuests = maxGuestsEditText.getText().toString().trim();

        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(description) || TextUtils.isEmpty(address) ||
                TextUtils.isEmpty(price) || TextUtils.isEmpty(rooms) || TextUtils.isEmpty(maxGuests)) {
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
        updatedData.put("maxNumOfGuests", Integer.parseInt(maxGuests));
        updatedData.put("checkInTime", selectedCheckInTime);
        updatedData.put("checkOutTime", selectedCheckOutTime);
        updatedData.put("bedTypes", bedTypesMap);
        updatedData.put("numOfBeds", totalBeds);
        updatedData.put("updatedAt", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date()));

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

    private void setupCheckInTimeSpinner() {
        String[] checkInTimes = {"12:00", "13:00", "14:00", "15:00", "16:00", "17:00"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, checkInTimes);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        checkInTimeSpinner.setAdapter(adapter);
        checkInTimeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedCheckInTime = checkInTimes[position];
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selectedCheckInTime = "14:00";
            }
        });
    }

    private void setupCheckOutTimeSpinner() {
        String[] checkOutTimes = {"06:00", "07:00", "08:00", "09:00", "10:00", "11:00"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, checkOutTimes);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        checkOutTimeSpinner.setAdapter(adapter);
        checkOutTimeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedCheckOutTime = checkOutTimes[position];
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selectedCheckOutTime = "10:00";
            }
        });
    }

    private void openAddBedTypeDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_bed_type, null);
        Spinner bedTypeSpinner = dialogView.findViewById(R.id.dialogBedTypeSpinner);
        EditText bedCountEditText = dialogView.findViewById(R.id.bedCountEditText);
        Button addButton = dialogView.findViewById(R.id.addButton);

        String[] bedTypesForDialog = {"King Size", "Queen Size", "Double", "Single", "Studio"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, bedTypesForDialog);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        bedTypeSpinner.setAdapter(adapter);

        android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(this)
                .setTitle("Add Bed Type")
                .setView(dialogView)
                .create();

        addButton.setOnClickListener(v -> {
            String selectedType = bedTypeSpinner.getSelectedItem().toString();
            String countStr = bedCountEditText.getText().toString();

            if (TextUtils.isEmpty(countStr)) {
                Toast.makeText(this, "Please enter bed count.", Toast.LENGTH_SHORT).show();
                return;
            }

            int count = Integer.parseInt(countStr);
            bedTypesMap.put(selectedType, bedTypesMap.getOrDefault(selectedType, 0) + count);
            updateTotalBeds();
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
    }

    private void refreshBedTypesList() {
        bedTypesList.clear();
        for (Map.Entry<String, Integer> entry : bedTypesMap.entrySet()) {
            bedTypesList.add(new BedTypeItem(entry.getKey(), entry.getValue()));
        }
        bedTypesListAdapter.notifyDataSetChanged();
    }

    // Custom adapter for Bed Types ListView
    private class BedTypeAdapter extends ArrayAdapter<BedTypeItem> {
        public BedTypeAdapter() {
            super(UpdatePropertyActivity.this, 0, bedTypesList);
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            BedTypeItem item = getItem(position);
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_bed_type, parent, false);
            }

            TextView bedTypeTextView = convertView.findViewById(R.id.bedTypeTextView);
            ImageButton deleteButton = convertView.findViewById(R.id.deleteButton);

            bedTypeTextView.setText(item.type + " x " + item.count);

            deleteButton.setOnClickListener(v -> {
                bedTypesMap.remove(item.type);
                updateTotalBeds();
                refreshBedTypesList();
            });

            return convertView;
        }
    }

    // Bed Type item class
    private static class BedTypeItem {
        String type;
        int count;

        BedTypeItem(String type, int count) {
            this.type = type;
            this.count = count;
        }
    }
}
