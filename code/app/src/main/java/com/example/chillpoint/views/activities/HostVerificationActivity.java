package com.example.chillpoint.views.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.GridView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.chillpoint.R;
import com.example.chillpoint.managers.SessionManager;
import com.example.chillpoint.utils.NavigationSetup;
import com.example.chillpoint.utils.NavigationUtils;
import com.example.chillpoint.views.adapters.ImageAdapter;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class HostVerificationActivity extends AppCompatActivity {

    private static final String TAG = "HostVerification";

    private FirebaseAuth auth;
    private FirebaseFirestore firestore;
    private StorageReference storageReference;

    private Button uploadImageButton, submitButton;
    private GridView imageGridView;
    private TextView infoTextView, instructionsTextView;
    private ImageAdapter imageAdapter;
    private ArrayList<Uri> imageUris;

    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_host_verification);


        // Initialize Firebase instances
        auth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        storageReference = FirebaseStorage.getInstance().getReference();

        // Initialize SessionManager
        sessionManager = new SessionManager(this);

        // Initialize UI components
        uploadImageButton = findViewById(R.id.uploadImageButton);
        submitButton = findViewById(R.id.submitButton);
        imageGridView = findViewById(R.id.imageGridView);
        infoTextView = findViewById(R.id.infoTextView);
        instructionsTextView = findViewById(R.id.instructionsTextView);

        imageUris = new ArrayList<>();
        imageAdapter = new ImageAdapter(this, imageUris);
        imageGridView.setAdapter(imageAdapter);

        // Set listener for image removal
        imageAdapter.setOnImageRemoveListener(position -> {
            imageUris.remove(position);
            imageAdapter.notifyDataSetChanged();
        });

        // Set click listener for image upload button
        uploadImageButton.setOnClickListener(v -> openGallery());

        // Set click listener for submit button
        submitButton.setOnClickListener(v -> submitHostVerification());
    }

    // Open gallery to pick images
    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        galleryLauncher.launch(intent);
    }

    private final ActivityResultLauncher<Intent> galleryLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    if (result.getData().getClipData() != null) {
                        int count = result.getData().getClipData().getItemCount();
                        for (int i = 0; i < count; i++) {
                            Uri imageUri = result.getData().getClipData().getItemAt(i).getUri();
                            imageUris.add(imageUri);
                        }
                    } else if (result.getData().getData() != null) {
                        Uri imageUri = result.getData().getData();
                        imageUris.add(imageUri);
                    }
                    imageAdapter.notifyDataSetChanged();
                }
            });

    // Submit host verification form
    private void submitHostVerification() {
        if (imageUris.isEmpty()) {
            Toast.makeText(this, "Please upload at least one image.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Retrieve user details from SessionManager
        String userId = sessionManager.getUserId();
        String username = sessionManager.getUsername();
        String phone = sessionManager.getPhone();

        if (userId == null || username == null || phone == null) {
            Toast.makeText(this, "Error retrieving user details. Please log in again.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check if there is already a pending verification
        firestore.collection("HostVerifications")
                .whereEqualTo("userId", userId)
                .whereEqualTo("status", "Pending")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && !task.getResult().isEmpty()) {
                        Toast.makeText(this, "You already have a pending verification.", Toast.LENGTH_SHORT).show();
                    } else {
                        uploadImagesAndSaveData(userId, username, phone);
                    }
                });
    }

    private void uploadImagesAndSaveData(String userId, String username, String phone) {
        ArrayList<String> imageUrls = new ArrayList<>();
        for (Uri uri : imageUris) {
            String fileName = "host_verifications/" + userId + "/" + System.currentTimeMillis() + ".jpg";
            StorageReference imageRef = storageReference.child(fileName);

            imageRef.putFile(uri)
                    .continueWithTask(task -> imageRef.getDownloadUrl())
                    .addOnCompleteListener(new OnCompleteListener<Uri>() {
                        @Override
                        public void onComplete(@NonNull Task<Uri> task) {
                            if (task.isSuccessful()) {
                                imageUrls.add(task.getResult().toString());
                                if (imageUrls.size() == imageUris.size()) {
                                    saveVerificationData(userId, username, phone, imageUrls);
                                }
                            } else {
                                Toast.makeText(HostVerificationActivity.this, "Error uploading images.", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
        }
    }

    private void saveVerificationData(String userId, String username, String phone, ArrayList<String> imageUrls) {
        // Create verification data
        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
        String status = "Pending";
        String adminNote = "";

        // Prepare data to save
        HostVerification verification = new HostVerification(
                userId, username, phone, imageUrls, status, timestamp, adminNote
        );

        firestore.collection("HostVerifications").add(verification)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(HostVerificationActivity.this, "Verification submitted successfully.", Toast.LENGTH_SHORT).show();
                        finish(); // Close the activity
                    } else {
                        Toast.makeText(HostVerificationActivity.this, "Error submitting verification.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // Data model for host verification
    private static class HostVerification {
        public String userId;
        public String username;
        public String phone;
        public ArrayList<String> imageUrls;
        public String status;
        public String timestamp;
        public String adminNote;

        public HostVerification(String userId, String username, String phone, ArrayList<String> imageUrls, String status, String timestamp, String adminNote) {
            this.userId = userId;
            this.username = username;
            this.phone = phone;
            this.imageUrls = imageUrls;
            this.status = status;
            this.timestamp = timestamp;
            this.adminNote = adminNote;
        }
    }

}
