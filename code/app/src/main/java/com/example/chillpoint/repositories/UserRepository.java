package com.example.chillpoint.repositories;

import android.net.Uri;
import android.util.Log;

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

    public void addUser(String userId, String username, String fullName, String email, String phone, String role, AddUserCallback callback) {
        // Create user data map
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("username", username);
        userMap.put("fullName", fullName);
        userMap.put("email", email);
        userMap.put("phone", phone);
        userMap.put("role", role);
        userMap.put("isValidated", false); // Automatically set isValidated to false

        // Add the user to Firestore
        firestore.collection("Users").document(userId).set(userMap)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        callback.onSuccess(); // Notify success
                    } else {
                        callback.onFailure(task.getException()); // Notify failure with exception
                    }
                });
    }

    public void loginUser(String email, String password, LoginCallback callback) {
        try {
            auth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            callback.onSuccess();  // Notify success
                        } else {
                            callback.onFailure(task.getException()); // Notify failure with exception
                        }
                    });
        } catch (Exception e) {
            e.printStackTrace();
            callback.onFailure(e);  // Notify failure in case of unexpected exception
        }
    }

    public void getUserRole(String userId, UserRoleCallback callback) {
        firestore.collection("Users").document(userId).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot snapshot = task.getResult();
                        if (snapshot.exists()) {
                            String role = snapshot.getString("role");
                            callback.onSuccess(role);  // Pass the role to the callback
                        } else {
                            callback.onFailure(new Exception("No user data found"));  // Notify if no user found
                        }
                    } else {
                        callback.onFailure(task.getException());  // Notify if task failed
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

    public void uploadUserProfileImage(String userId, Uri imageUri, ImageUploadCallback callback) {
        String imagePath = "users/" + userId + "/profile.jpg";
        StorageReference reference = FirebaseStorage.getInstance().getReference(imagePath);
        reference.putFile(imageUri).addOnSuccessListener(taskSnapshot ->
                        reference.getDownloadUrl().addOnSuccessListener(uri -> callback.onSuccess(uri.toString()))
                                .addOnFailureListener(callback::onFailure))
                .addOnFailureListener(callback::onFailure);
    }

    public void addUserWithImage(String userId, String username, String fullName, String email, String phone, String role, String imageUrl, AddUserCallback callback) {
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("username", username);
        userMap.put("fullName", fullName);
        userMap.put("email", email);
        userMap.put("phone", phone);
        userMap.put("role", role);
        userMap.put("imageUrl", imageUrl); // Add image URL
        userMap.put("isValidated", false);

        firestore.collection("Users").document(userId).set(userMap)
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(callback::onFailure);
    }

    // Callback interface for addUser
    public interface AddUserCallback {
        void onSuccess();

        void onFailure(Exception e);
    }

    // Callback interface for login result
    public interface LoginCallback {
        void onSuccess();

        void onFailure(Exception e);
    }

    // Callback interface for retrieving user role
    public interface UserRoleCallback {
        void onSuccess(String role);

        void onFailure(Exception e);
    }

    // Callback interface for image upload
    public interface ImageUploadCallback {
        void onSuccess(String imageUrl);

        void onFailure(Exception e);
    }
}
