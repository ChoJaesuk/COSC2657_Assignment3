package com.example.chillpoint.repositories;

import android.net.Uri;
import android.util.Log;

import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.HashMap;
import java.util.Map;

public class UserRepository {
    private final FirebaseAuth auth;

    private final FirebaseFirestore firestore;

    public UserRepository() {
        this.firestore = FirebaseFirestore.getInstance();
        this.auth = FirebaseAuth.getInstance();
    }

    public Task<Boolean> addUser(String userId, String username, String fullName, String email, String phone, String role, String bio, String imageUrl) {
        // Create user data map
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("username", username);
        userMap.put("fullName", fullName);
        userMap.put("email", email);
        userMap.put("phone", phone);
        userMap.put("role", role);
        userMap.put("bio", bio); // Add bio field
        userMap.put("imageUrl", imageUrl);
        userMap.put("isValidated", false); // Automatically set isValidated to false

        // Add the user to Firestore
        return firestore.collection("Users").document(userId)
                .set(userMap)
                .continueWith(task -> task.isSuccessful());
    }

    public Task<Boolean> loginUser(String email, String password) {
        return auth.signInWithEmailAndPassword(email, password)
                .continueWith(task -> {
                    if (task.isSuccessful()) {
                        // If the login is successful, return true
                        return true;
                    } else {
                        // Throw the exception to propagate error handling
                        throw task.getException() != null
                                ? task.getException()
                                : new Exception("Login failed for unknown reasons.");
                    }
                });
    }

    // New method to retrieve user details
    public Task<DocumentSnapshot> getUserDetails(String userId) {
        return firestore.collection("Users").document(userId).get()
                .continueWith(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot snapshot = task.getResult();
                        if (snapshot != null && snapshot.exists()) {
                            // Return the snapshot if the user exists
                            return snapshot;
                        } else {
                            // Throw an exception if no user data is found
                            throw new Exception("No user data found for ID: " + userId);
                        }
                    } else {
                        // Throw the exception to propagate the failure
                        throw task.getException() != null
                                ? task.getException()
                                : new Exception("Failed to retrieve user details.");
                    }
                });
    }

    public void saveUserToDatabase(FirebaseUser user) {
        if (user == null) {
            Log.e("Error", "User is null, cannot save to database.");
            return; // Exit the method early if the user is null
        }

        String userId = user.getUid();
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("username", user.getDisplayName());
        userInfo.put("email", user.getEmail());
        userInfo.put("role", "User");
        userInfo.put("fullName", "");
        userInfo.put("phone", "");
        userInfo.put("bio", ""); // Add bio field with default value
        userInfo.put("isValidated", false); // Automatically set isValidated to false

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

    public Task<String> uploadUserProfileImage(String userId, Uri imageUri) {
        String imagePath = "users/" + userId + "/profile.jpg";
        StorageReference reference = FirebaseStorage.getInstance().getReference(imagePath);

        // Upload the file
        return reference.putFile(imageUri)
                .continueWithTask(task -> {
                    if (!task.isSuccessful() && task.getException() != null) {
                        throw task.getException();
                    }
                    // Get the download URL
                    return reference.getDownloadUrl();
                })
                .continueWith(task -> {
                    if (task.isSuccessful()) {
                        // Return the download URL as a String
                        return task.getResult().toString();
                    } else {
                        throw task.getException() != null
                                ? task.getException()
                                : new Exception("Failed to retrieve the download URL");
                    }
                });
    }
}
