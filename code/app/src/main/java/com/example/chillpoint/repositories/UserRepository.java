package com.example.chillpoint.repositories;

import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class UserRepository {
    private final FirebaseAuth auth;

    private final FirebaseFirestore firestore;

    public UserRepository() {
        this.firestore = FirebaseFirestore.getInstance();
        this.auth = FirebaseAuth.getInstance();
    }

    public Task<Boolean> addUser(String userId, String username, String fullName, String email, String phone, String role) {
        // Create user data map
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("username", username);
        userMap.put("fullName", fullName);
        userMap.put("email", email);
        userMap.put("phone", phone);
        userMap.put("role", role);
        userMap.put("isValidated", false); // Automatically set isValidated to false

        // Return the Task directly
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
                        // If the login fails, throw the exception
                        throw task.getException();
                    }
                });
    }

    public Task<String> getUserRole(String userId) {
        return firestore.collection("Users").document(userId).get()
                .continueWith(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document != null && document.exists()) {
                            // Retrieve the "role" field from the document
                            String role = document.getString("role");
                            if (role != null) {
                                return role;
                            } else {
                                throw new Exception("Role field is missing for the user.");
                            }
                        } else {
                            throw new Exception("User not found.");
                        }
                    } else {
                        throw task.getException();
                    }
                });
    }

//    public void saveUserToDatabase(FirebaseUser user) {
//        if (user == null) {
//            Log.e("Error", "User is null, cannot save to database.");
//            return; // Exit the method early if the user is null
//        }
//
//        String userId = user.getUid();
//        Map<String, Object> userInfo = new HashMap<>();
//        userInfo.put("username", user.getDisplayName());
//        userInfo.put("email", user.getEmail());
//        userInfo.put("role", "User");
//        userInfo.put("fullName", "");
//        userInfo.put("phone", "");
//        userInfo.put("isValidated", false); // Automatically set isValidated to false
//
//        firestore.collection("Users").document(userId)
//                .set(userInfo)
//                .addOnCompleteListener(task -> {
//                    if (task.isSuccessful()) {
//                        Log.d("Success", "User successfully saved to database.");
//                    } else {
//                        Log.e("Error", "Failed to save user to database.", task.getException());
//                    }
//                });
//    }

    // Callback interface for addUser
//    public interface AddUserCallback {
//        void onSuccess();
//
//        void onFailure(Exception e);
//    }
//
//    // Callback interface for login result
//    public interface LoginCallback {
//        void onSuccess();
//
//        void onFailure(Exception e);
//    }
//
//    // Callback interface for retrieving user role
//    public interface UserRoleCallback {
//        void onSuccess(String role);
//
//        void onFailure(Exception e);
//    }
}
