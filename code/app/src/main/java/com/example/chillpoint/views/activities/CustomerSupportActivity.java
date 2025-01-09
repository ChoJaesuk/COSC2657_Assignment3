package com.example.chillpoint.views.activities;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.chillpoint.R;
import com.example.chillpoint.managers.SessionManager;
import com.example.chillpoint.repositories.ReportRepository;
import com.example.chillpoint.views.adapters.ReportAdapter;
import com.example.chillpoint.views.models.Report;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;

public class CustomerSupportActivity extends AppCompatActivity {
    private ListView reportsListView;
    private ReportAdapter reportAdapter;
    private ArrayList<Report> reportList;
    private ReportRepository reportRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_support);

        // Initialize the views
        reportsListView = findViewById(R.id.reportsListView);
        reportList = new ArrayList<>();
        reportAdapter = new ReportAdapter(this, reportList);
        reportsListView.setAdapter((ListAdapter) reportAdapter);

        // Initialize ReportRepository
        reportRepository = new ReportRepository();

        // Fetch reports
        String userId = new SessionManager(this).getUserId(); // Assumes SessionManager is available
        if (userId != null) {
            fetchReports(userId);
        } else {
            Toast.makeText(this, "User ID not found. Please log in.", Toast.LENGTH_SHORT).show();
        }
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
}
