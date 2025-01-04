package com.example.chillpoint.views.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Spinner;

import androidx.appcompat.app.AppCompatActivity;

import com.example.chillpoint.R;
import com.example.chillpoint.views.activities.BookingSummaryActivity;

public class BookingActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking);

        // Get form inputs
        RadioGroup radioGroup = findViewById(R.id.radioGroup);
        EditText nameEditText = findViewById(R.id.nameEditText);
        EditText emailEditText = findViewById(R.id.emailEditText);
        EditText phoneEditText = findViewById(R.id.phoneEditText);
        EditText addressEditText = findViewById(R.id.addressEditText);
        EditText zipEditText = findViewById(R.id.zipEditText);
        EditText cityEditText = findViewById(R.id.cityEditText);
//        Spinner countrySpinner = findViewById(R.id.countrySpinner);
//        Spinner stateSpinner = findViewById(R.id.stateSpinner);

        Button submitButton = findViewById(R.id.submitButton);
        submitButton.setOnClickListener(v -> {
            // Collect data
            String title = radioGroup.getCheckedRadioButtonId() == R.id.radioButton ? "Mr." : "Mrs.";
            String name = nameEditText.getText().toString();
            String email = emailEditText.getText().toString();
            String phone = phoneEditText.getText().toString();
            String address = addressEditText.getText().toString();
            String zip = zipEditText.getText().toString();
            String city = cityEditText.getText().toString();

            // Start SummaryActivity
            Intent intent = new Intent(BookingActivity.this, BookingSummaryActivity.class);
            intent.putExtra("TITLE", title);
            intent.putExtra("NAME", name);
            intent.putExtra("EMAIL", email);
            intent.putExtra("PHONE", phone);
            intent.putExtra("ADDRESS", address);
            intent.putExtra("ZIP", zip);
            intent.putExtra("CITY", city);
            startActivity(intent);
        });
    }
}
