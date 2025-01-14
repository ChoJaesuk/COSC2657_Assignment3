package com.example.chillpoint.views.activities;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.chillpoint.R;
import com.example.chillpoint.managers.SessionManager;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class EditProfileActivity extends AppCompatActivity {
    private static final String TAG = "EditProfileActivity";
    private static final int PICK_IMAGE_REQUEST = 1;

    private ImageView profileImageView;
    private EditText fullNameEditText, usernameEditText, emailEditText, phoneEditText, bioEditText;
    private Button changeImageButton, saveButton;

    private Uri imageUri;
    private SessionManager sessionManager;
    private FirebaseFirestore firestore;
    private StorageReference storageReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        sessionManager = new SessionManager(this);
        firestore = FirebaseFirestore.getInstance();
        storageReference = FirebaseStorage.getInstance().getReference();

        profileImageView = findViewById(R.id.profileImageView);
        fullNameEditText = findViewById(R.id.fullNameEditText);
        usernameEditText = findViewById(R.id.usernameEditText);
        emailEditText = findViewById(R.id.emailEditText);
        phoneEditText = findViewById(R.id.phoneEditText);
        bioEditText = findViewById(R.id.bioEditText);
        changeImageButton = findViewById(R.id.changeImageButton);
        saveButton = findViewById(R.id.saveButton);

        // Load user data from Firestore
        loadUserData();

        // Change image button logic
        changeImageButton.setOnClickListener(v -> openImagePicker());

        // Save button logic
        saveButton.setOnClickListener(v -> saveUserData());
    }

    private void loadUserData() {
        String userId = sessionManager.getUserId();
        firestore.collection("Users").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String imageUrl = documentSnapshot.getString("imageUrl");
                        String fullName = documentSnapshot.getString("fullName");
                        String username = documentSnapshot.getString("username");
                        String email = documentSnapshot.getString("email");
                        String phone = documentSnapshot.getString("phone");
                        String bio = documentSnapshot.getString("bio");

                        // Log user data
                        Log.d(TAG, "Loading user data from Firestore: " + fullName + ", " + username);

                        // Load image using Glide
                        if (imageUrl != null && !imageUrl.isEmpty()) {
                            Glide.with(this).load(imageUrl).into(profileImageView);
                        }

                        // Set placeholders
                        fullNameEditText.setText(fullName);
                        usernameEditText.setText(username);
                        emailEditText.setText(email);
                        phoneEditText.setText(phone);
                        bioEditText.setText(bio);
                    } else {
                        Log.e(TAG, "No user data found in Firestore for userId: " + userId);
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error loading user data from Firestore", e));
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null && data.getData() != null) {
            imageUri = data.getData();
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
                profileImageView.setImageBitmap(bitmap);
            } catch (IOException e) {
                Log.e(TAG, "Failed to load image", e);
                Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void saveUserData() {
        String fullName = fullNameEditText.getText().toString();
        String username = usernameEditText.getText().toString();
        String email = emailEditText.getText().toString();
        String phone = phoneEditText.getText().toString();
        String bio = bioEditText.getText().toString();

        if (imageUri != null) {
            // Upload image to Firebase Storage
            StorageReference imageRef = storageReference.child("users/" + sessionManager.getUserId() + "/profile.jpg");
            imageRef.putFile(imageUri)
                    .addOnSuccessListener(taskSnapshot -> imageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                        String imageUrl = uri.toString();
                        updateFirestore(fullName, username, email, phone, bio, imageUrl);
                    }))
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to upload image", e);
                        Toast.makeText(this, "Failed to upload image", Toast.LENGTH_SHORT).show();
                    });
        } else {
            updateFirestore(fullName, username, email, phone, bio, sessionManager.getUserImageUrl());
        }
    }

    private void updateFirestore(String fullName, String username, String email, String phone, String bio, String imageUrl) {
        String userId = sessionManager.getUserId();
        Map<String, Object> userUpdates = new HashMap<>();
        userUpdates.put("fullName", fullName);
        userUpdates.put("username", username);
        userUpdates.put("email", email);
        userUpdates.put("phone", phone);
        userUpdates.put("bio", bio);
        userUpdates.put("imageUrl", imageUrl);

        firestore.collection("Users").document(userId)
                .set(userUpdates, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Profile updated successfully for userId: " + userId);
                    Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show();

                    // Update SessionManager with new data
                    sessionManager.saveUserSession(userId, sessionManager.getRole(), username, imageUrl);
                    sessionManager.saveAdditionalUserInfo(email, phone, bio);

                    setResult(Activity.RESULT_OK);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to update profile", e);
                    Toast.makeText(this, "Failed to update profile", Toast.LENGTH_SHORT).show();
                });
    }
}
