package com.example.chillpoint.views.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;
import com.example.chillpoint.R;

public class BillSplittingActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bill_splitting);

        // Retrieve data from Intent
        Intent incomingIntent = getIntent();
        String userId = incomingIntent.getStringExtra("userId");
        String username = incomingIntent.getStringExtra("username");
        String propertyId = incomingIntent.getStringExtra("propertyId");
        double propertyPrice = incomingIntent.getDoubleExtra("propertyPrice", 0.0);

        Button nextButton = findViewById(R.id.toPaymentButton);
        nextButton.setOnClickListener(v -> {
            // Pass data to CheckoutActivity
            Intent intent = new Intent(BillSplittingActivity.this, CheckoutActivity.class);
            intent.putExtra("userId", userId);
            intent.putExtra("username", username);
            intent.putExtra("propertyId", propertyId);
            intent.putExtra("propertyPrice", propertyPrice);
            startActivity(intent);
        });
    }
}