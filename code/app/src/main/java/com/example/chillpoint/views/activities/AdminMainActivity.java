package com.example.chillpoint.views.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.example.chillpoint.R;

public class AdminMainActivity extends AppCompatActivity {

    private Button hostVerificationButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_main);

        // Initialize the button
        hostVerificationButton = findViewById(R.id.hostVerificationButton);

        // Set click listener for the button
        hostVerificationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Navigate to HostVerificationListActivity
                Intent intent = new Intent(AdminMainActivity.this, HostVerificationListActivity.class);
                startActivity(intent);
            }
        });
    }
}
