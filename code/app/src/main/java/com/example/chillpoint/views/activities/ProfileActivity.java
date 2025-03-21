package com.example.chillpoint.views.activities;

import static androidx.core.content.ContextCompat.startActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import com.bumptech.glide.Glide;
import com.example.chillpoint.R;
import com.example.chillpoint.managers.SessionManager;
import com.example.chillpoint.utils.NavigationUtils;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

public class ProfileActivity extends BaseActivity {

    private static final int EDIT_PROFILE_REQUEST = 1001;

    private ImageView profileImageView;
    private TextView usernameTextView;
    private TextView hostVerificationTextView;
    private TextView bookingManagementTextView;
    private TextView propertyManagementTextView;
    private Button editProfileButton;
    private TextView customerSupportTv;

    private SessionManager sessionManager;
    private FirebaseFirestore firestore;
    private Button logoutButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        setupNavigationBar();

        // Initialize SessionManager and Firestore
        sessionManager = new SessionManager(this);
        firestore = FirebaseFirestore.getInstance();

        // Initialize UI components
        profileImageView = findViewById(R.id.profileImageView);
        usernameTextView = findViewById(R.id.usernameTextView);
        hostVerificationTextView = findViewById(R.id.hostVerification);
        bookingManagementTextView = findViewById(R.id.bookingManagement);
        propertyManagementTextView = findViewById(R.id.propertyManagement);
        customerSupportTv = findViewById(R.id.customerSupportTv);
        editProfileButton = findViewById(R.id.editProfileButton);
        logoutButton = findViewById(R.id.logoutButton);

        // Load user profile using SessionManager
        loadUserProfile();

        // Set click listeners
        hostVerificationTextView.setOnClickListener(v -> handleHostVerification());
        bookingManagementTextView.setOnClickListener(v -> handleRestrictedActions("Booking Management"));
        propertyManagementTextView.setOnClickListener(v -> handlePropertyManagement());

        // Edit profile button click listener
        editProfileButton.setOnClickListener(v -> navigateToEditProfile());
        propertyManagementTextView.setOnClickListener(v -> handleRestrictedActions("Property Management"));
        customerSupportTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ProfileActivity.this, CustomerSupportActivity.class);
                startActivity(intent);
            }
        });

        logoutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();

                if (firebaseAuth.getCurrentUser() != null) {
                    GoogleSignInClient googleSignInClient = GoogleSignIn.getClient(ProfileActivity.this, GoogleSignInOptions.DEFAULT_SIGN_IN);

                    firebaseAuth.signOut();
                    googleSignInClient.signOut().addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (task.isSuccessful()) {
                                sessionManager.clearSession();
                                Toast.makeText(ProfileActivity.this, "Signed out successfully", Toast.LENGTH_SHORT).show();
                                Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
                                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(intent);
                                finish();
                            } else {
                                Toast.makeText(ProfileActivity.this, "Failed to sign out", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                } else {
                    sessionManager.clearSession();

                    Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                }
            }
        });


    }


    private void loadUserProfile() {
        // Get user data from SessionManager
        String username = sessionManager.getUsername();
        String imageUrl = sessionManager.getUserImageUrl();

        if (username != null && !username.isEmpty()) {
            usernameTextView.setText(username);
        } else {
            usernameTextView.setText("Unknown User");
        }

        // Load profile image
        if (imageUrl != null && !imageUrl.isEmpty()) {
            Glide.with(ProfileActivity.this)
                    .load(imageUrl)
                    .placeholder(R.drawable.defaultavatar) // Default image while loading
                    .error(R.drawable.defaultavatar) // Default image if loading fails
                    .into(profileImageView);
        } else {
            profileImageView.setImageResource(R.drawable.defaultavatar); // Default image
        }

        // Check if session data exists, else show an error
        if (username == null && imageUrl == null) {
            Toast.makeText(this, "Session data not found. Please log in again.", Toast.LENGTH_SHORT).show();
        }
    }

    private void navigateToEditProfile() {
        Intent intent = new Intent(ProfileActivity.this, EditProfileActivity.class);
        startActivityForResult(intent, EDIT_PROFILE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == EDIT_PROFILE_REQUEST && resultCode == RESULT_OK) {
            // Reload updated user profile
            loadUserProfile();
        }
    }

    private void handleHostVerification() {
        String userId = sessionManager.getUserId();

        if (userId == null) {
            showAlert("Error", "Session expired. Please log in again.");
            return;
        }

        firestore.collection("HostVerifications")
                .whereEqualTo("userId", userId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        QuerySnapshot querySnapshot = task.getResult();
                        if (!querySnapshot.isEmpty()) {
                            String status = querySnapshot.getDocuments().get(0).getString("status");

                            switch (status) {
                                case "Approved":
                                    showAlert("Already Verified", "You have already been verified as a host.");
                                    break;
                                case "Pending":
                                    showAlert("Pending Approval", "Your verification request is still pending. Please wait for admin approval.");
                                    break;
                                default: // Rejected or other cases
                                    navigateToHostVerification();
                                    break;
                            }
                        } else {
                            // No verification record found
                            navigateToHostVerification();
                        }
                    } else {
                        // Firestore query failed
                        Log.e("FirestoreError", "Error checking host verification status", task.getException());
                        Toast.makeText(this, "Error checking host verification status.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void navigateToHostVerification() {
        Intent intent = new Intent(ProfileActivity.this, HostVerificationActivity.class);
        startActivity(intent);
    }

    private void handleRestrictedActions(String actionName) {
        String userId = sessionManager.getUserId();

        if (userId == null) {
            showAlert("Error", "Session expired. Please log in again.");
            return;
        }

        firestore.collection("HostVerifications")
                .whereEqualTo("userId", userId)
                .whereEqualTo("status", "Approved")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        QuerySnapshot querySnapshot = task.getResult();
                        if (!querySnapshot.isEmpty()) {
                            if ("Booking Management".equals(actionName)) {
                                Intent intent = new Intent(ProfileActivity.this, HostBookingManagementActivity.class);
                                intent.putExtra("hostId", userId);
                                startActivity(intent);
                            } else if ("Property Management".equals(actionName)) {
                                Intent intent = new Intent(ProfileActivity.this, PropertyManagementActivity.class);
                                startActivity(intent);
                            } else {
                                Toast.makeText(this, actionName + " is not yet implemented.", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            showAlert("Host Verification Required",
                                    "To perform this action, you need to submit a host verification request and get approved by the admin.");
                        }
                    } else {
                        showAlert("Error", "Error checking host verification status.");
                    }
                });
    }

    private void handlePropertyManagement() {
        String userId = sessionManager.getUserId();

        if (userId == null) {
            showAlert("Error", "Session expired. Please log in again.");
            return;
        }

        firestore.collection("HostVerifications")
                .whereEqualTo("userId", userId)
                .whereEqualTo("status", "Approved")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        QuerySnapshot querySnapshot = task.getResult();
                        if (!querySnapshot.isEmpty()) {
                            // User is verified, navigate to PropertyManagementActivity
                            Intent intent = new Intent(this, PropertyManagementActivity.class);
                            startActivity(intent);
                        } else {
                            showAlert("Host Verification Required",
                                    "To manage properties, you need to be an approved host. Please submit a host verification request.");
                        }
                    } else {
                        showAlert("Error", "Error checking host verification status.");
                    }
                });
    }

    private void showAlert(String title, String message) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                .show();
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
