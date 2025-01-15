package com.example.chillpoint.views.activities;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chillpoint.R;
import com.example.chillpoint.managers.SessionManager;
import com.example.chillpoint.repositories.ReportRepository;
import com.example.chillpoint.utils.NavigationSetup;
import com.example.chillpoint.utils.NavigationUtils;
import com.example.chillpoint.views.adapters.ImageSliderAdapter;
import com.example.chillpoint.views.adapters.ReportAdapter;
import com.example.chillpoint.views.models.Report;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CustomerSupportActivity extends AppCompatActivity implements NavigationSetup {
    private RecyclerView reportsRecyclerView;
    private ReportAdapter reportAdapter;
    private ArrayList<Report> reportList;
    private ReportRepository reportRepository;

    private View feedbackItemLayout, addFeedbackLayout;
    private TextView closeTextView, feedbackCategoryTextView, feedbackContentTextView, feedbackStatusTextView, feedbackDateTextView,removeFeedbackTextView;
    private EditText feedbackCategoryEditText, feedbackContentEditText;
    private TextView uploadImagesTextView, cancelTextView, feedbackButton;
    private RecyclerView feedbackImagesRecyclerView, uploadedImagesRecyclerView;
    private List<String> selectedImageUris = new ArrayList<>();
    private ImageSliderAdapter uploadedImagesAdapter;
    private String currentReportId;

    @SuppressLint("NotifyDataSetChanged")
    private final ActivityResultLauncher<Intent> pickImagesLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    if (result.getData().getClipData() != null) {
                        int count = result.getData().getClipData().getItemCount();
                        for (int i = 0; i < count; i++) {
                            Uri imageUri = result.getData().getClipData().getItemAt(i).getUri();
                            selectedImageUris.add(imageUri.toString());
                        }
                    } else if (result.getData().getData() != null) {
                        Uri imageUri = result.getData().getData();
                        selectedImageUris.add(imageUri.toString());
                    }
                    uploadedImagesAdapter.notifyDataSetChanged();
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_support);

        setupNavigationBar();

        // Initialize layouts and views
        feedbackItemLayout = findViewById(R.id.feedbackItemLinearLayout);
        feedbackItemLayout.setVisibility(View.GONE);

        addFeedbackLayout = findViewById(R.id.addFeedbackLinearLayout);
        addFeedbackLayout.setVisibility(View.GONE);

        closeTextView = findViewById(R.id.closeTextView);
        closeTextView.setOnClickListener(v -> feedbackItemLayout.setVisibility(View.GONE));
        feedbackCategoryTextView = findViewById(R.id.feedbackCategoryTextView);
        feedbackContentTextView = findViewById(R.id.feedbackContentTextView);
        feedbackStatusTextView = findViewById(R.id.feedbackStatusTextView);
        feedbackDateTextView = findViewById(R.id.feedbackDateTextView);
        feedbackCategoryEditText = findViewById(R.id.feedbackCategoryEditText);
        feedbackContentEditText = findViewById(R.id.feedbackContentEditText);
        uploadImagesTextView = findViewById(R.id.uploadImagesTextView);
        cancelTextView = findViewById(R.id.cancelTextView);
        feedbackButton = findViewById(R.id.feedbackButton);
        removeFeedbackTextView = findViewById(R.id.removeFeedbackTextView);

        feedbackImagesRecyclerView = findViewById(R.id.feedbackImagesRecyclerView);
        feedbackImagesRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));

        uploadedImagesRecyclerView = findViewById(R.id.uploadedImagesRecyclerView);
        uploadedImagesRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        uploadedImagesAdapter = new ImageSliderAdapter(this, selectedImageUris);
        uploadedImagesRecyclerView.setAdapter(uploadedImagesAdapter);

        uploadImagesTextView.setOnClickListener(v -> openImagePicker());

        cancelTextView.setOnClickListener(v -> addFeedbackLayout.setVisibility(View.GONE));

        feedbackButton.setOnClickListener(v -> {
            addFeedbackLayout.setVisibility(View.VISIBLE);
            clearFeedbackForm();
        });

        Button addNewReport = findViewById(R.id.addNewReport);
        addNewReport.setOnClickListener(v -> saveNewReport());

        reportsRecyclerView = findViewById(R.id.reportsRecyclerView);
        reportsRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        reportList = new ArrayList<>();
        reportAdapter = new ReportAdapter(this, reportList, this::showReportDetails);
        reportsRecyclerView.setAdapter(reportAdapter);

        reportRepository = new ReportRepository();
        String userId = new SessionManager(this).getUserId();
        if (userId != null) {
            fetchReports(userId);
        } else {
            Toast.makeText(this, "User ID not found. Please log in.", Toast.LENGTH_SHORT).show();
        }
        removeFeedbackTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentReportId == null || currentReportId.isEmpty()) {
                    Toast.makeText(CustomerSupportActivity.this, "No report selected to delete.", Toast.LENGTH_SHORT).show();
                    return;
                }

                reportRepository.deleteReportById(currentReportId)
                        .addOnSuccessListener(success -> {
                            if (success) {
                                Toast.makeText(CustomerSupportActivity.this, "Report deleted successfully!", Toast.LENGTH_SHORT).show();
                                feedbackItemLayout.setVisibility(View.GONE); // Hide the feedback details layout
                                String userId = new SessionManager(CustomerSupportActivity.this).getUserId();
                                if (userId != null) {
                                    fetchReports(userId); // Refresh the reports list
                                }
                            }
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(CustomerSupportActivity.this, "Failed to delete report: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
            }
        });

    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        pickImagesLauncher.launch(intent);
    }

    @SuppressLint("NotifyDataSetChanged")
    private void fetchReports(String userId) {
        reportRepository.getAllReportsOfUser(userId).addOnSuccessListener(querySnapshot -> {
            reportList.clear();
            for (var document : querySnapshot) {
                Report report = new Report(
                        document.getId(),
                        document.getString("category"),
                        document.getString("content"),
                        document.getString("createdAt"),
                        document.getString("status"),
                        document.getString("userId"),
                        (ArrayList<String>) document.get("imageUrl")
                );
                reportList.add(report);
            }
            reportAdapter.notifyDataSetChanged();
        }).addOnFailureListener(e -> Toast.makeText(this, "Failed to fetch reports: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void saveNewReport() {
        String category = feedbackCategoryEditText.getText().toString().trim();
        String content = feedbackContentEditText.getText().toString().trim();
        String userId = new SessionManager(this).getUserId();
        String createdAt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());

        if (category.isEmpty() || content.isEmpty()) {
            Toast.makeText(this, "Please fill out all fields.", Toast.LENGTH_SHORT).show();
            return;
        }

        Report newReport = new Report(
                null,
                category,
                content,
                createdAt,
                "Pending",
                userId,
                new ArrayList<>(selectedImageUris)
        );

        reportRepository.addNewReport(userId, newReport).addOnSuccessListener(success -> {
            if (success) {
                Toast.makeText(this, "Report added successfully!", Toast.LENGTH_SHORT).show();
                addFeedbackLayout.setVisibility(View.GONE);
                fetchReports(userId);
            }
        }).addOnFailureListener(e -> Toast.makeText(this, "Failed to add report: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void clearFeedbackForm() {
        feedbackCategoryEditText.setText("");
        feedbackContentEditText.setText("");
        selectedImageUris.clear();
        uploadedImagesAdapter.notifyDataSetChanged();
    }

    private void showReportDetails(Report report) {
        currentReportId = report.getId();
        // Populate the feedback details layout with the selected report data
        feedbackCategoryTextView.setText("Category: " + report.getCategory());
        feedbackContentTextView.setText("Content: " + report.getContent());
        feedbackStatusTextView.setText("Status: " + report.getStatus());
        feedbackDateTextView.setText("Date: " + report.getCreatedAt());

        // Handle image display
        if (report.getImageUrl() != null && !report.getImageUrl().isEmpty()) {
            ImageSliderAdapter imageSliderAdapter = new ImageSliderAdapter(this, report.getImageUrl());
            feedbackImagesRecyclerView.setAdapter(imageSliderAdapter);
        } else {
            feedbackImagesRecyclerView.setAdapter(null); // Clear previous images if no images exist
            Toast.makeText(this, "No images available", Toast.LENGTH_SHORT).show();
        }

        // Show the feedback details layout
        feedbackItemLayout.setVisibility(View.VISIBLE);
    }


    @Override
    public void setupNavigationBar() {
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setSelectedItemId(R.id.nav_profile);
        NavigationUtils.handleBottomNavigation(this, bottomNavigationView);
    }
}
