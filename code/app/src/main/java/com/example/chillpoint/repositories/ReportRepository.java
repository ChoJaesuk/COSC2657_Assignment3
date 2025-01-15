package com.example.chillpoint.repositories;

import com.example.chillpoint.views.models.Report;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class ReportRepository {
    private final FirebaseAuth auth;

    private final FirebaseFirestore firestore;

    public ReportRepository() {
        this.firestore = FirebaseFirestore.getInstance();
        this.auth = FirebaseAuth.getInstance();}

    public Task<QuerySnapshot> getAllReportsOfUser(String userId) {
        return firestore.collection("Reports")
                .whereEqualTo("userId", userId)
                .get();
    }

    public Task<Boolean> deleteReportById(String reportId) {
        return firestore.collection("Reports")
                .document(reportId)
                .delete()
                .continueWith(task -> {
                    if (task.isSuccessful()) {
                        return true; // Report successfully deleted
                    } else {
                        throw task.getException() != null
                                ? task.getException()
                                : new Exception("Failed to delete report with ID: " + reportId);
                    }
                });
    }

    public Task<Boolean> addNewReport(String userId, Report report) {
        // Create a map to represent the new report
        Map<String, Object> newReport = new HashMap<>();
        newReport.put("userId", userId);
        newReport.put("category", report.getCategory()); // Use category from the Report object
        newReport.put("content", report.getContent()); // Use content from the Report object
        newReport.put("createdAt", report.getCreatedAt()); // Use createdAt from the Report object
        newReport.put("status", report.getStatus()); // Use status from the Report object
        newReport.put("imageUrl", report.getImageUrl() != null ? report.getImageUrl() : new ArrayList<>()); // Use image URLs or an empty list if null

        // Add the new report to Firestore
        return firestore.collection("Reports")
                .add(newReport)
                .continueWith(task -> {
                    if (task.isSuccessful()) {
                        return true; // Report added successfully
                    } else {
                        throw task.getException() != null
                                ? task.getException()
                                : new Exception("Failed to add new report for user: " + userId);
                    }
                });
    }

}
