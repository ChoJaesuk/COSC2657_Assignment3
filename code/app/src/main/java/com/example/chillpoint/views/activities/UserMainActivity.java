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
        String userId = getIntent().getStringExtra("userId");

        // Initialize the Create Property button
        Button createPropertyButton = findViewById(R.id.createPropertyButton);
        Button chatBox = findViewById(R.id.chatBox);
        createPropertyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Navigate to CreatePropertyActivity
                Intent intent = new Intent(UserMainActivity.this, CreatePropertyActivity.class);
                startActivity(intent);
            }
        });
        chatBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(UserMainActivity.this, ChatActivity.class);
                startActivity(intent);
            }
        });
    }
}
