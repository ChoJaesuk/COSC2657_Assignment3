package com.example.chillpoint.views.activities;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ProgressBar;
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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class CreatePropertyActivity extends AppCompatActivity {

    private static final int IMAGE_PICKER_REQUEST = 100;

    private EditText nameEditText, descriptionEditText, addressEditText, priceEditText, roomsEditText, bedsEditText;
    private Button uploadImagesButton, savePropertyButton;
    private GridView imagesGridView;
    private ProgressBar progressBar;

    private FirebaseAuth auth;
    private FirebaseFirestore firestore;
    private FirebaseStorage storage;

    private ArrayList<Uri> imageUris; // To store selected image URIs
    private ArrayList<String> uploadedImageUrls; // To store uploaded image URLs
    private ImageAdapter imageAdapter;

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
        bedsEditText = findViewById(R.id.bedsEditText);
        uploadImagesButton = findViewById(R.id.uploadImagesButton);
        savePropertyButton = findViewById(R.id.savePropertyButton);
        imagesGridView = findViewById(R.id.imagesGridView);
        progressBar = findViewById(R.id.progressBar);

        // Initialize Firebase services
        auth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();

        // Initialize image lists
        imageUris = new ArrayList<>();
        uploadedImageUrls = new ArrayList<>();

        // Set up ImageAdapter
        imageAdapter = new ImageAdapter(this, imageUris);
        imagesGridView.setAdapter(imageAdapter);
        imageAdapter.setOnImageRemoveListener(position -> {
            imageUris.remove(position); // Remove image from the list
            imageAdapter.notifyDataSetChanged(); // Notify adapter of data change
        });

        // Set listeners
        uploadImagesButton.setOnClickListener(v -> openImagePicker());
        savePropertyButton.setOnClickListener(v -> saveProperty());
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
            if (data.getClipData() != null) { // Multiple images selected
                int count = data.getClipData().getItemCount();
                for (int i = 0; i < count; i++) {
                    Uri imageUri = data.getClipData().getItemAt(i).getUri();
                    imageUris.add(imageUri);
                }
            } else if (data.getData() != null) { // Single image selected
                Uri imageUri = data.getData();
                imageUris.add(imageUri);
            }
            imageAdapter.notifyDataSetChanged(); // Refresh the adapter
        }
    }

    private void saveProperty() {
        String name = nameEditText.getText().toString().trim();
        String description = descriptionEditText.getText().toString().trim();
        String address = addressEditText.getText().toString().trim();
        String price = priceEditText.getText().toString().trim();
        String rooms = roomsEditText.getText().toString().trim();
        String beds = bedsEditText.getText().toString().trim();

        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(description) || TextUtils.isEmpty(address) ||
                TextUtils.isEmpty(price) || TextUtils.isEmpty(rooms) || TextUtils.isEmpty(beds)) {
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
                        Integer.parseInt(rooms), Integer.parseInt(beds), urls);
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

    private void saveToFirestore(String name, String description, String address, double price, int rooms, int beds, ArrayList<String> imageUrls) {
        String userId = auth.getCurrentUser().getUid();
        String createdAt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());

        Map<String, Object> property = new HashMap<>();
        property.put("name", name);
        property.put("description", description);
        property.put("address", address);
        property.put("pricePerNight", price);
        property.put("numOfRooms", rooms);
        property.put("numOfBeds", beds);
        property.put("createdAt", createdAt);
        property.put("updatedAt", createdAt);
        property.put("userId", userId);
        property.put("validProperty", true);
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
