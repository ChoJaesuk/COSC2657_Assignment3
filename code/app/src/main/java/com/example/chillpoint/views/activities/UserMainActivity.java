package com.example.chillpoint.views.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.example.chillpoint.R;

public class UserMainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_main);

        // Initialize the Create Property button
        Button createPropertyButton = findViewById(R.id.createPropertyButton);
        createPropertyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Navigate to CreatePropertyActivity
                Intent intent = new Intent(UserMainActivity.this, CreatePropertyActivity.class);
                startActivity(intent);
            }
        });
    }
}
