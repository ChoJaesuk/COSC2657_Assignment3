package com.example.chillpoint.views.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.chillpoint.R;
import com.example.chillpoint.repositories.UserRepository;
import com.example.chillpoint.views.activities.AdminMainActivity;
import com.example.chillpoint.views.activities.RegisterActivity;
import com.example.chillpoint.views.activities.UserMainActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class LoginActivity extends AppCompatActivity {

    private EditText emailEditText, passwordEditText;
    private Button loginButton;
    private ProgressBar progressBar;
    private TextView registerLink;
    private FirebaseAuth auth;
    private UserRepository userRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Initialize UI components
        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        loginButton = findViewById(R.id.loginButton);
        progressBar = findViewById(R.id.progressBar);
        registerLink = findViewById(R.id.registerLink);

        // Initialize Firebase Auth and Firestore
        auth = FirebaseAuth.getInstance();

        // Set login button click listener
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loginUser();
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
        boolean isLoginSuccessful = userRepository.loginUser(email, password);

        progressBar.setVisibility(View.GONE);

        if (isLoginSuccessful) {
            String userId = auth.getCurrentUser().getUid();
            String role = userRepository.getUserRole(userId);  // Now returns the role as a string

            if (role != null) {
                if ("User".equals(role)) {
                    startActivity(new Intent(LoginActivity.this, UserMainActivity.class));
                } else if ("Admin".equals(role)) {
                    startActivity(new Intent(LoginActivity.this, AdminMainActivity.class));
                }
                finish();
            } else {
                Toast.makeText(LoginActivity.this, "Failed to retrieve user role", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(LoginActivity.this, "Login failed", Toast.LENGTH_SHORT).show();
        }
    }
}
