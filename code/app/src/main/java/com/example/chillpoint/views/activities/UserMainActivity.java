package com.example.chillpoint.views.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chillpoint.R;
import com.example.chillpoint.views.models.Property;
import com.example.chillpoint.views.adapters.PropertyAdapter;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;

public class UserMainActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private PropertyAdapter propertyAdapter;
    private ArrayList<Property> propertyList;
    private FirebaseFirestore firestore;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_main);

        Button createPropertyButton = findViewById(R.id.createPropertyButton);
        recyclerView = findViewById(R.id.recyclerView);
        progressBar = findViewById(R.id.progressBar);

        propertyList = new ArrayList<>();
        propertyAdapter = new PropertyAdapter(this, propertyList);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(propertyAdapter);

        firestore = FirebaseFirestore.getInstance();

        createPropertyButton.setOnClickListener(v -> {
            startActivity(new Intent(UserMainActivity.this, CreatePropertyActivity.class));
        });

        loadProperties();
    }

    private void loadProperties() {
        progressBar.setVisibility(View.VISIBLE);

        firestore.collection("Properties").get().addOnCompleteListener(task -> {
            progressBar.setVisibility(View.GONE);
            if (task.isSuccessful()) {
                QuerySnapshot querySnapshot = task.getResult();
                propertyList.clear();
                propertyList.addAll(querySnapshot.toObjects(Property.class));
                propertyAdapter.notifyDataSetChanged();
            } else {
                // Handle errors
            }
        });
    }
}
