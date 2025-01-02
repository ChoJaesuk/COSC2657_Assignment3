package com.example.chillpoint.repositories;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;

import java.util.HashMap;
import java.util.Map;

public class UserRepository {

    private final FirebaseFirestore firestore;

    public UserRepository() {
        this.firestore = FirebaseFirestore.getInstance();
    }

    public boolean addUser(String userId, String username, String fullName, String email, String phone, String role) {
        // Create user data map
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("username", username);
        userMap.put("fullName", fullName);
        userMap.put("email", email);
        userMap.put("phone", phone);
        userMap.put("role", role);

        // Create a document reference
        TaskCompletionSource<Boolean> taskCompletionSource = new TaskCompletionSource<>();

        // Add the user to Firestore
        firestore.collection("Users").document(userId).set(userMap)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        taskCompletionSource.setResult(true); // Success
                    } else {
                        taskCompletionSource.setResult(false); // Failure
                    }
                });

        // Get the result of the Task
        try {
            return taskCompletionSource.getTask().getResult();
        } catch (Exception e) {
            e.printStackTrace();
            return false; // In case of an error
        }
    }
}
