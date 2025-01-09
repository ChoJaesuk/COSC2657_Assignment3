package com.example.chillpoint.views.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chillpoint.R;
import com.example.chillpoint.managers.SessionManager;
import com.example.chillpoint.views.adapters.PropertyManagementAdapter;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Map;

public class PropertyManagementActivity extends AppCompatActivity {

    private RecyclerView propertyRecyclerView;
    private ProgressBar progressBar;
    private Button createPropertyButton;

    private ArrayList<Map<String, Object>> properties;
    private PropertyManagementAdapter propertyManagementAdapter;

    private FirebaseFirestore firestore;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_property_management);

        // Initialize UI components
        propertyRecyclerView = findViewById(R.id.propertyRecyclerView);
        progressBar = findViewById(R.id.progressBar);
        createPropertyButton = findViewById(R.id.createPropertyButton);

        // Initialize Firebase services
        firestore = FirebaseFirestore.getInstance();

        // Initialize SessionManager
        sessionManager = new SessionManager(this);

        // Initialize data structures
        properties = new ArrayList<>();
        propertyManagementAdapter = new PropertyManagementAdapter(this, properties, this::onEditPropertyClicked, this::onDeletePropertyClicked);

        // Set up RecyclerView
        propertyRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        propertyRecyclerView.setAdapter(propertyManagementAdapter);

        // Load properties
        loadProperties();

        // Set button listeners
        createPropertyButton.setOnClickListener(v -> navigateToCreateProperty());
    }

    private void loadProperties() {
        String userId = sessionManager.getUserId(); // Get user ID from SessionManager

        if (userId == null) {
            Toast.makeText(this, "User not logged in.", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);

        firestore.collection("Properties")
                .whereEqualTo("userId", userId)
                .get()
                .addOnCompleteListener(task -> {
                    progressBar.setVisibility(View.GONE);
                    if (task.isSuccessful() && task.getResult() != null) {
                        properties.clear();
                        for (var document : task.getResult().getDocuments()) {
                            Map<String, Object> data = document.getData();
                            if (data != null) {
                                data.put("id", document.getId()); // Add document ID to the map
                                properties.add(data);
                            }
                        }
                        propertyManagementAdapter.notifyDataSetChanged();
                    } else {
                        Toast.makeText(this, "Failed to load properties.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void navigateToCreateProperty() {
        Intent intent = new Intent(this, CreatePropertyActivity.class);
        startActivity(intent);
    }

    private void onEditPropertyClicked(Map<String, Object> property) {
        Intent intent = new Intent(this, UpdatePropertyActivity.class);
        intent.putExtra("propertyId", (String) property.get("id"));
        startActivity(intent);
    }

    private void onDeletePropertyClicked(Map<String, Object> property) {
        String propertyName = (String) property.get("name");
        new AlertDialog.Builder(this)
                .setTitle("Delete Property")
                .setMessage("Are you sure you want to delete the property: " + propertyName + "?")
                .setPositiveButton("Yes", (dialog, which) -> deleteProperty(property))
                .setNegativeButton("No", null)
                .show();
    }

    private void deleteProperty(Map<String, Object> property) {
        String propertyId = (String) property.get("id");

        progressBar.setVisibility(View.VISIBLE);

        firestore.collection("Properties").document(propertyId).delete()
                .addOnSuccessListener(aVoid -> {
                    progressBar.setVisibility(View.GONE);
                    properties.remove(property);
                    propertyManagementAdapter.notifyDataSetChanged();
                    Toast.makeText(this, "Property deleted successfully.", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Error deleting property: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
