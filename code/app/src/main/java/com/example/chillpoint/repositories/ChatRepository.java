package com.example.chillpoint.repositories;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

public class ChatRepository {
    private final FirebaseFirestore firestore;

    public ChatRepository() {
        firestore = FirebaseFirestore.getInstance();
    }

    public Task<QuerySnapshot> getAllChatsByUserId(String userId) {
        return firestore.collection("Chats")
                .whereArrayContains("participants", userId)
                .get();
    }
}
