package com.example.chillpoint.views.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.example.chillpoint.R;

public class AdminMainActivity extends AppCompatActivity {

    private Button hostVerificationButton;
    private Button createVoucherButton;
    private Button viewVoucherListButton;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_main);

        // Initialize the button
        hostVerificationButton = findViewById(R.id.hostVerificationButton);
        createVoucherButton = findViewById(R.id.createVoucherButton); // 새 버튼 초기화
        viewVoucherListButton = findViewById(R.id.viewVoucherListButton);

        // Set click listener for the button
        hostVerificationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Navigate to HostVerificationListActivity
                Intent intent = new Intent(AdminMainActivity.this, HostVerificationListActivity.class);
                startActivity(intent);
            }
        });
        // Create Voucher 버튼 클릭 리스너
        createVoucherButton.setOnClickListener(v -> {
            Intent intent = new Intent(AdminMainActivity.this, CreateVoucherActivity.class);
            startActivity(intent);
        });

        // Set click listener for View Voucher List button
        viewVoucherListButton.setOnClickListener(v -> {
            Intent intent = new Intent(AdminMainActivity.this, VoucherListActivity.class);
            startActivity(intent);
        });

    }
}
