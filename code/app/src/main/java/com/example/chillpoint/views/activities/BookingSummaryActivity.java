package com.example.chillpoint.views.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.chillpoint.R;

public class BookingSummaryActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking_summary);

        // Retrieve data from the intent
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            String title = extras.getString("TITLE");
            String name = extras.getString("NAME");
            String email = extras.getString("EMAIL");
            String phone = extras.getString("PHONE");
            String address = extras.getString("ADDRESS");
            String city = extras.getString("CITY");
            String zip = extras.getString("ZIP");

            // Set the data to the corresponding TextViews
            ((TextView) findViewById(R.id.summaryTitle)).setText("Title: " + title);
            ((TextView) findViewById(R.id.summaryName)).setText("Name: " + name);
            ((TextView) findViewById(R.id.summaryEmail)).setText("Email: " + email);
            ((TextView) findViewById(R.id.summaryPhone)).setText("Phone: " + phone);
            ((TextView) findViewById(R.id.summaryAddress)).setText("Address: " + address);
            ((TextView) findViewById(R.id.summaryCity)).setText("City: " + city);
            ((TextView) findViewById(R.id.summaryZip)).setText("Zip Code: " + zip);
        }

        Button nextButton = findViewById(R.id.nextButton);
        nextButton.setOnClickListener(v -> {
            // When the button is clicked, start the BillSplittingActivity
            Intent intent = new Intent(BookingSummaryActivity.this, BillSplittingActivity.class);
            startActivity(intent);
        });
    }
}

