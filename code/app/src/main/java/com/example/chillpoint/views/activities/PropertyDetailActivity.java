package com.example.chillpoint.views.activities;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.chillpoint.R;

public class PropertyDetailActivity extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_property_detail);

        // Initialize views
        ImageView propertyImageView = findViewById(R.id.propertyDetailImageView);
        TextView propertyNameTextView = findViewById(R.id.propertyDetailNameTextView);
        TextView propertyDescriptionTextView = findViewById(R.id.propertyDetailDescriptionTextView);
        TextView propertyAddressTextView = findViewById(R.id.propertyDetailAddressTextView);
        TextView propertyPriceTextView = findViewById(R.id.propertyDetailPriceTextView);

        // Get data from intent
        String name = getIntent().getStringExtra("name");
        String description = getIntent().getStringExtra("description");
        String address = getIntent().getStringExtra("address");
        String price = getIntent().getStringExtra("price");
        String imageUrl = getIntent().getStringExtra("image");

        // Bind data to views
        propertyNameTextView.setText(name);
        propertyDescriptionTextView.setText(description);
        propertyAddressTextView.setText(address);
        propertyPriceTextView.setText(price);

        // Load image using Glide
        if (imageUrl != null) {
            Glide.with(this).load(imageUrl).placeholder(R.drawable.image_placeholder).into(propertyImageView);
        } else {
            propertyImageView.setImageResource(R.drawable.image_placeholder);
        }
    }
}
