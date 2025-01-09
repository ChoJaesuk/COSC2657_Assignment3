package com.example.chillpoint.repositories;

import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

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
}
