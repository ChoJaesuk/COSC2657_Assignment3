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

        Button nextButton = findViewById(R.id.toPaymentButton);
        nextButton.setOnClickListener(v -> {
            // When the button is clicked, start the BillSplittingActivity
            Intent intent = new Intent(BillSplittingActivity.this, CheckoutActivity.class);
            startActivity(intent);
        });
    }
}
