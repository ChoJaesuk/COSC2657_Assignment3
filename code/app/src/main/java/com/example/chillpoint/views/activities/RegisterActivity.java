package com.example.chillpoint.views.activities;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.chillpoint.R;
import com.example.chillpoint.repositories.UserRepository;
import com.example.chillpoint.views.adapters.ImageAdapter;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;

public class RegisterActivity extends AppCompatActivity {

    private static final int IMAGE_PICKER_REQUEST = 100;

    private EditText usernameEditText, fullNameEditText, emailEditText, phoneEditText, passwordEditText, confirmPasswordEditText;
    private Spinner roleSpinner;
    private Button registerButton, uploadImageButton;
    private ProgressBar progressBar;
    private TextView loginLink;
    private GridView imagesGridView;

    private FirebaseAuth auth;
    private UserRepository userRepository;

    private ArrayList<Uri> imageUris; // To store selected image URIs
    private ImageAdapter imageAdapter; // Adapter for displaying selected images

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // Initialize UI components
        usernameEditText = findViewById(R.id.usernameEditText);
        fullNameEditText = findViewById(R.id.fullNameEditText);
        emailEditText = findViewById(R.id.emailEditText);
        phoneEditText = findViewById(R.id.phoneEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        confirmPasswordEditText = findViewById(R.id.confirmPasswordEditText);
        roleSpinner = findViewById(R.id.roleSpinner);
        registerButton = findViewById(R.id.registerButton);
        uploadImageButton = findViewById(R.id.uploadImagesButton); // Button for image upload
        progressBar = findViewById(R.id.progressBar);
        loginLink = findViewById(R.id.loginLink);
        imagesGridView = findViewById(R.id.imagesGridView); // GridView for displaying selected image

        auth = FirebaseAuth.getInstance();
        userRepository = new UserRepository();

        // Role array definition
        String[] roles = {"User", "Admin"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, roles);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        roleSpinner.setAdapter(adapter);

        // Initialize image list and adapter
        imageUris = new ArrayList<>();
        imageAdapter = new ImageAdapter(this, imageUris);
        imagesGridView.setAdapter(imageAdapter);

        // Set up remove image functionality
        imageAdapter.setOnImageRemoveListener(position -> {
            imageUris.clear(); // Allow only one image
            imageAdapter.notifyDataSetChanged();
        });

        uploadImageButton.setOnClickListener(v -> openImagePicker());
        registerButton.setOnClickListener(v -> registerUser());

        // Set login link click listener
        loginLink.setOnClickListener(v -> {
            Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
            startActivity(intent);
        });
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(Intent.createChooser(intent, "Select an Image"), IMAGE_PICKER_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == IMAGE_PICKER_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
            Uri imageUri = data.getData();
            if (imageUri != null) {
                imageUris.clear(); // Allow only one image
                imageUris.add(imageUri);
                imageAdapter.notifyDataSetChanged(); // Refresh the adapter
            }
        }
    }

    private void registerUser() {
        String username = usernameEditText.getText().toString().trim();
        String fullName = fullNameEditText.getText().toString().trim();
        String email = emailEditText.getText().toString().trim();
        String phone = phoneEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();
        String confirmPassword = confirmPasswordEditText.getText().toString().trim();
        String role = roleSpinner.getSelectedItem().toString();

        if (TextUtils.isEmpty(username) || TextUtils.isEmpty(fullName) || TextUtils.isEmpty(email) ||
                TextUtils.isEmpty(phone) || TextUtils.isEmpty(password) || TextUtils.isEmpty(confirmPassword)) {
            Toast.makeText(RegisterActivity.this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!password.equals(confirmPassword)) {
            Toast.makeText(RegisterActivity.this, "Passwords do not match", Toast.LENGTH_SHORT).show();
            return;
        }

        if (imageUris.isEmpty()) {
            Toast.makeText(RegisterActivity.this, "Please upload a profile image", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                String userId = auth.getCurrentUser().getUid();

                // Upload image to storage
                Uri imageUri = imageUris.get(0); // Get the single image
                userRepository.uploadUserProfileImage(userId, imageUri, new UserRepository.ImageUploadCallback() {
                    @Override
                    public void onSuccess(String imageUrl) {
                        // Save user details to Firestore
                        userRepository.addUserWithImage(userId, username, fullName, email, phone, role, imageUrl, new UserRepository.AddUserCallback() {
                            @Override
                            public void onSuccess() {
                                progressBar.setVisibility(View.GONE);
                                Toast.makeText(RegisterActivity.this, "Registration successful", Toast.LENGTH_SHORT).show();
                                Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                                startActivity(intent);
                                finish();
                            }

                            @Override
                            public void onFailure(Exception e) {
                                progressBar.setVisibility(View.GONE);
                                Toast.makeText(RegisterActivity.this, "Database error: User not added", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }

                    @Override
                    public void onFailure(Exception e) {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(RegisterActivity.this, "Image upload failed", Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(RegisterActivity.this, "Registration failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
