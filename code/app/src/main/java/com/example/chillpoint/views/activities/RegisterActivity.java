package com.example.chillpoint.views.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.chillpoint.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    private EditText usernameEditText, fullNameEditText, emailEditText, phoneEditText, passwordEditText, confirmPasswordEditText;
    private Spinner roleSpinner;
    private Button registerButton;
    private ProgressBar progressBar;
    private TextView loginLink;
    private FirebaseAuth auth;
    private FirebaseFirestore firestore;

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
        progressBar = findViewById(R.id.progressBar);
        loginLink = findViewById(R.id.loginLink);

        auth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        // Role 배열 정의
        String[] roles = {"User", "Admin"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, roles);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        roleSpinner.setAdapter(adapter);

        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                registerUser();
            }
        });

        // Set login link click listener
        loginLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                startActivity(intent);
            }
        });
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

        progressBar.setVisibility(View.VISIBLE);
        auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                String userId = auth.getCurrentUser().getUid();

                // Create a new user document in Firestore
                Map<String, Object> userMap = new HashMap<>();
                userMap.put("username", username);
                userMap.put("fullName", fullName);
                userMap.put("email", email);
                userMap.put("phone", phone);
                userMap.put("role", role);

                firestore.collection("Users").document(userId).set(userMap).addOnCompleteListener(dbTask -> {
                    progressBar.setVisibility(View.GONE);
                    if (dbTask.isSuccessful()) {
                        Toast.makeText(RegisterActivity.this, "Registration successful", Toast.LENGTH_SHORT).show();

                        // 자동으로 로그인 페이지로 이동
                        new android.os.Handler().postDelayed(() -> {
                            Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                            startActivity(intent);
                            finish();
                        }, 2000); // 2초 딜레이
                    } else {
                        Toast.makeText(RegisterActivity.this, "Database error: " + dbTask.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(RegisterActivity.this, "Registration failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
