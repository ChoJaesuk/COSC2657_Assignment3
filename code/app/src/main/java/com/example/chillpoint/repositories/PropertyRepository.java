package com.example.chillpoint.repositories;

import com.example.chillpoint.views.models.Property;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class PropertyRepository {
    private final FirebaseAuth auth;
    private final FirebaseFirestore firestore;

    public PropertyRepository() {
        this.firestore = FirebaseFirestore.getInstance();
        this.auth = FirebaseAuth.getInstance();
    }

    public Task<Property> getPropertyById(String propertyId) {
        return firestore.collection("Properties")
                .document(propertyId)
                .get()
                .continueWith(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot documentSnapshot = task.getResult();
                        if (documentSnapshot != null && documentSnapshot.exists()) {
                            Property property = documentSnapshot.toObject(Property.class);
                            if (property != null) {
                                property.setId(documentSnapshot.getId()); // Set the ID manually
                            }
                            return property;
                        } else {
                            throw new Exception("Property with ID " + propertyId + " does not exist.");
                        }
                    } else {
                        throw task.getException() != null
                                ? task.getException()
                                : new Exception("Failed to fetch property.");
                    }
                });
    }
}
