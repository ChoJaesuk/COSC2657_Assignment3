package com.example.chillpoint.repositories;

import android.util.Log;

import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class WishlistRepository {
    private final FirebaseAuth auth;
    private final FirebaseFirestore firestore;

    public WishlistRepository() {
        this.firestore = FirebaseFirestore.getInstance();
        this.auth = FirebaseAuth.getInstance();
    }

    public Task<List<String>> getAllWishlistItemsByUser(String userId) {
        return firestore.collection("Wishlists")
                .whereEqualTo("userId", userId)
                .get()
                .continueWith(task -> {
                    List<String> propertyIds = new ArrayList<>();
                    if (task.isSuccessful() && task.getResult() != null) {
                        task.getResult().getDocuments().forEach(document -> {
                            String propertyId = document.getString("propertyId");
                            if (propertyId != null) {
                                propertyIds.add(propertyId);
                                Log.d("WishlistRepository", "Property ID: " + propertyId);
                            }
                        });
                    } else if (task.getException() != null) {
                        throw task.getException();
                    } else {
                        Log.d("WishlistRepository", "No wishlist items found for user: " + userId);
                    }
                    return propertyIds;
                });
    }

    public Task<Void> deleteWishlistItem(String userId, String propertyId) {
        return firestore.collection("Wishlists")
                .whereEqualTo("userId", userId) // Filter by user ID
                .whereEqualTo("propertyId", propertyId) // Filter by property ID
                .get()
                .continueWithTask(queryTask -> {
                    if (queryTask.isSuccessful() && queryTask.getResult() != null && !queryTask.getResult().isEmpty()) {
                        // Get the first document matching the query
                        String documentId = queryTask.getResult().getDocuments().get(0).getId();

                        // Delete the document
                        return firestore.collection("Wishlists").document(documentId).delete();
                    } else if (queryTask.getException() != null) {
                        throw queryTask.getException();
                    } else {
                        throw new Exception("No matching wishlist item found to delete.");
                    }
                });
    }

}
