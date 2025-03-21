package com.example.chillpoint.views.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.chillpoint.R;
import com.example.chillpoint.managers.SessionManager;
import com.example.chillpoint.repositories.UserRepository;
import com.example.chillpoint.views.activities.AdminMainActivity;
import com.example.chillpoint.views.activities.RegisterActivity;
import com.example.chillpoint.views.activities.UserMainActivity;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class LoginActivity extends AppCompatActivity {

    private EditText emailEditText, passwordEditText;
    private Button loginButton;
    private ProgressBar progressBar;
    private TextView registerLink;
    private FirebaseAuth auth;
    private UserRepository userRepository;
    private GoogleSignInClient googleSignInClient;
    private SignInButton signInButton;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        FirebaseApp.initializeApp(this);
        // Initialize UI components
        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        loginButton = findViewById(R.id.loginButton);
        progressBar = findViewById(R.id.progressBar);
        registerLink = findViewById(R.id.registerLink);
        signInButton = findViewById(R.id.signInGoogle);
        GoogleSignInOptions options = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.client_id))
                .requestEmail()
                .build();
        googleSignInClient = GoogleSignIn.getClient(LoginActivity.this, options);

        // Initialize Firebase Auth and Firestore
        auth = FirebaseAuth.getInstance();
        userRepository = new UserRepository();

        // Set login button click listener
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loginUser();
            }
        });
        signInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = googleSignInClient.getSignInIntent();
                activityResultLauncher.launch(intent);
            }
        });

        // Set register link click listener
        registerLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
                startActivity(intent);
            }
        });
    }

    private void loginUser() {
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            Toast.makeText(LoginActivity.this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);

        userRepository.loginUser(email, password)
                .addOnSuccessListener(isLoggedIn -> {
                    if (isLoggedIn) {
                        String userId = auth.getCurrentUser().getUid(); // Retrieve current user ID
                        userRepository.getUserDetails(userId)
                                .addOnSuccessListener(documentSnapshot -> {
                                    // Extract user details
                                    String role = documentSnapshot.getString("role");
                                    String username = documentSnapshot.getString("username");
                                    String userImageUrl = documentSnapshot.getString("imageUrl"); // User image URL

                                    Log.d("LoginActivity", "Retrieved user details: role=" + role +
                                            ", username=" + username + ", imageUrl=" + userImageUrl);

                                    // Save session information
                                    sessionManager = new SessionManager(LoginActivity.this);
                                    sessionManager.saveUserSession(userId, role, username, userImageUrl); // Save with image URL

                                    Log.d("SessionManager", "Session saved: userId=" + sessionManager.getUserId() +
                                            ", role=" + sessionManager.getRole() +
                                            ", username=" + sessionManager.getUsername() +
                                            ", imageUrl=" + sessionManager.getUserImageUrl());

                                    // Navigate based on role
                                    if (role != null) {
                                        if ("User".equals(role)) {
                                            startActivity(new Intent(LoginActivity.this, UserMainActivity.class));
                                        } else if ("Admin".equals(role)) {
                                            startActivity(new Intent(LoginActivity.this, AdminMainActivity.class));
                                        }
                                        finish();
                                    } else {
                                        progressBar.setVisibility(View.GONE);
                                        Toast.makeText(LoginActivity.this, "Failed to retrieve user role", Toast.LENGTH_SHORT).show();
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    progressBar.setVisibility(View.GONE);
                                    Toast.makeText(LoginActivity.this, "Failed to retrieve user details: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                });
                    } else {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(LoginActivity.this, "Unexpected login failure", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(LoginActivity.this, "Login failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }




    private final ActivityResultLauncher<Intent> activityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode() == RESULT_OK) {
                        Task<GoogleSignInAccount> accountTask = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                        try {
                            GoogleSignInAccount signInAccount = accountTask.getResult(ApiException.class);
                            if (signInAccount == null) {
                                Log.e("SignIn", "Sign-in account was null.");
                                return;
                            }
                            Log.d("SignIn", "ID Token: " + signInAccount.getIdToken());
                            AuthCredential authCredential = GoogleAuthProvider.getCredential(signInAccount.getIdToken(), null);
                            auth.signInWithCredential(authCredential).addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    Log.d("SignIn", "Firebase Authentication successful.");
                                    Log.d("SignIn", "signInWithCredential:success");
                                    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                                    if (user != null) {
                                        userRepository.addUser(
                                                user.getUid(),
                                                user.getDisplayName(),
                                                user.getDisplayName(),
                                                user.getEmail(),
                                                "", // Placeholder for phone
                                                "User",
                                                "", // Placeholder for bio
                                                user.getPhotoUrl() != null ? user.getPhotoUrl().toString() : ""
                                        ).addOnCompleteListener(addUserTask -> {
                                            if (addUserTask.isSuccessful()) {
                                                Log.d("SignIn", "User added to Firestore successfully.");

                                                // Save session information
                                                sessionManager = new SessionManager(LoginActivity.this);
                                                sessionManager.saveUserSession(
                                                        user.getUid(),
                                                        "User",
                                                        user.getDisplayName(),
                                                        user.getPhotoUrl() != null ? user.getPhotoUrl().toString() : ""
                                                );

                                                Log.d("SessionManager", "Session saved: userId=" + sessionManager.getUserId() +
                                                        ", role=" + sessionManager.getRole() +
                                                        ", username=" + sessionManager.getUsername() +
                                                        ", imageUrl=" + sessionManager.getUserImageUrl());

                                                Toast.makeText(LoginActivity.this, "Signed in successfully!", Toast.LENGTH_SHORT).show();
                                                startActivity(new Intent(LoginActivity.this, UserMainActivity.class));
                                                finish();
                                            } else {
                                                Log.e("SignIn", "Failed to add user to Firestore.", addUserTask.getException());
                                                Toast.makeText(LoginActivity.this, "Failed to save user: " + addUserTask.getException(), Toast.LENGTH_SHORT).show();
                                            }
                                        });
                                    }
                                } else {
                                    Log.e("SignIn", "Firebase Authentication failed.", task.getException());
                                    Toast.makeText(LoginActivity.this, "Failed to sign in: " + task.getException(), Toast.LENGTH_SHORT).show();
                                }
                            });
                        } catch (ApiException e) {
                            Log.e("SignIn", "Google Sign-in failed.", e);
                        }
                    }
                }
            });

    @Override
    protected void onDestroy() {
        super.onDestroy();
        FirebaseAuth.getInstance().addAuthStateListener(new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                if (firebaseAuth.getCurrentUser() == null) {
                    googleSignInClient.signOut().addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void unused) {
//                            Toast.makeText(LoginActivity.this, "Signed out successfully", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        });
        FirebaseAuth.getInstance().signOut();
//        sessionManager.clearSession();
    }
}
