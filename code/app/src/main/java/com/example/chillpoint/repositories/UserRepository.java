package com.example.chillpoint.repositories;

import android.util.Log;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public class UserRepository {
    private final FirebaseAuth auth;

    private final FirebaseFirestore firestore;

    public UserRepository() {
        this.firestore = FirebaseFirestore.getInstance();
        this.auth = FirebaseAuth.getInstance();
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

    // Method to login a user (returns boolean indicating success or failure)
    public boolean loginUser(String email, String password) {
        // Create TaskCompletionSource to return result
        TaskCompletionSource<Boolean> taskCompletionSource = new TaskCompletionSource<>();

        try {
            auth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            taskCompletionSource.setResult(true);  // Success
                        } else {
                            taskCompletionSource.setResult(false); // Failure
                        }
                    });
        } catch (Exception e) {
            e.printStackTrace();
            taskCompletionSource.setResult(false);  // In case of exception, return false
        }

        // Return the result of the Task
        try {
            return taskCompletionSource.getTask().getResult();
        } catch (Exception e) {
            e.printStackTrace();
            return false;  // In case of error, return false
        }
    }


    // Synchronously retrieves the user role
    public String getUserRole(String userId) {
        TaskCompletionSource<String> taskCompletionSource = new TaskCompletionSource<>();

        firestore.collection("Users").document(userId).get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot snapshot = task.getResult();
                if (snapshot.exists()) {
                    String role = snapshot.getString("role");
                    taskCompletionSource.setResult(role);  // Set the role result
                } else {
                    taskCompletionSource.setException(new Exception("No user data found"));  // Set exception if no user found
                }
            } else {
                taskCompletionSource.setException(task.getException());  // Set exception if task failed
            }
        });

        // Wait and return the result or exception
        try {
            return taskCompletionSource.getTask().getResult();
        } catch (Exception e) {
            e.printStackTrace();
            return null; // In case of error, return null
        }
    }

    public void saveUserToDatabase(FirebaseUser user) {
        if (user == null) {
            Log.e("Error", "User is null, cannot save to database.");
            return; // Exit the method early if the user is null
        }

        String userId = user.getUid();
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("userId", userId);
        userInfo.put("username", user.getDisplayName());
        userInfo.put("email", user.getEmail());
        userInfo.put("role", "User");


        firestore.collection("Users").document(userId)
                .set(userInfo)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d("Success", "User successfully saved to database.");
                    } else {
                        Log.e("Error", "Failed to save user to database.", task.getException());
                    }
                });
    }

}
