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

    private final ActivityResultLauncher<Intent> activityResultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
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
                    auth.signInWithCredential(authCredential).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (task.isSuccessful()) {
                                Log.d("SignIn", "Firebase Authentication successful.");
                                Log.d("SignIn", "signInWithCredential: success");

                                // Get the current authenticated user
                                auth = FirebaseAuth.getInstance();
                                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

                                if (user != null) {
                                    // Prepare to add the user details to Firestore
                                    String userId = user.getUid();
                                    String username = user.getDisplayName();
                                    String email = user.getEmail();

                                    // Optionally, you can retrieve or set other details like fullName, phone, role
                                    String fullName = ""; // Empty or retrieve fullName if you have a way to fetch it
                                    String phone = ""; // Empty or retrieve phone if you have a way to fetch it
                                    String role = "User"; // Default role as "User"

                                    // Call addUser method to store user data in Firestore
                                    userRepository.addUser(userId, username, fullName, email, phone, role)
                                            .addOnCompleteListener(firestoreTask -> {
                                                if (firestoreTask.isSuccessful() && Boolean.TRUE.equals(firestoreTask.getResult())) {
                                                    // If adding user to Firestore is successful, navigate to the user main activity
                                                    Toast.makeText(LoginActivity.this, "User added successfully!", Toast.LENGTH_SHORT).show();
                                                    startActivity(new Intent(LoginActivity.this, UserMainActivity.class));
                                                    finish();
                                                } else {
                                                    // Handle failure if adding user to Firestore fails
                                                    Toast.makeText(LoginActivity.this, "Failed to add user to Firestore.", Toast.LENGTH_SHORT).show();
                                                    Log.e("Firestore", "Error adding user to Firestore", firestoreTask.getException());
                                                }
                                            });
                                }

                            } else {
                                // Handle failure of Firebase authentication
                                Log.e("SignIn", "Firebase Authentication failed.", task.getException());
                                Toast.makeText(LoginActivity.this, "Failed to sign in: " + task.getException(), Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                } catch (ApiException e) {
                    e.printStackTrace();
                }
            }
        }
    });

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

        // Use the updated loginUser method with a callback
        userRepository.loginUser(email, password)
                .addOnSuccessListener(loginSuccess -> {
                    if (loginSuccess) {
                        progressBar.setVisibility(View.GONE);
                        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

                        userRepository.getUserRole(userId)
                                .addOnSuccessListener(role -> {
                                    if ("User".equals(role)) {
                                        startActivity(new Intent(LoginActivity.this, UserMainActivity.class));
                                    } else if ("Admin".equals(role)) {
                                        startActivity(new Intent(LoginActivity.this, AdminMainActivity.class));
                                    }
                                    finish();
                                })
                                .addOnFailureListener(e -> {
                                    progressBar.setVisibility(View.GONE);
                                    Toast.makeText(LoginActivity.this, "Failed to retrieve user role: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(LoginActivity.this, "Login failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

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
                            Toast.makeText(LoginActivity.this, "Signed out successfully", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        });
        FirebaseAuth.getInstance().signOut();
    }
}
