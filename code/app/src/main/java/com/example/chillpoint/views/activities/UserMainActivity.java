package com.example.chillpoint.views.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.chillpoint.R;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class UserMainActivity extends AppCompatActivity {
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_main);
        db = FirebaseFirestore.getInstance();
        String userId = getIntent().getStringExtra("userId");

        // Initialize the Create Property button
        Button createPropertyButton = findViewById(R.id.createPropertyButton);
        Button chatBox = findViewById(R.id.chatBox);
        Button contactHost = findViewById(R.id.createChat);
        CharSequence contentDescriptionHostId = contactHost.getContentDescription();

        createPropertyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Navigate to CreatePropertyActivity
                Intent intent = new Intent(UserMainActivity.this, CreatePropertyActivity.class);
                startActivity(intent);
            }
        });
        chatBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(UserMainActivity.this, ChatActivity.class);
                startActivity(intent);
            }
        });
        contactHost.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                contactUser("4Iz89vGyqtVzMd3Tc0gT9aaCkGG3", (String) contentDescriptionHostId);
            }
        });
    }

    private void contactUser(String currentLoginUserId, String hostId) {
        // Reference to the "Chats" collection
        db.collection("Chats")
                .whereArrayContains("participants", currentLoginUserId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    String existingChatId = null;

                    // Check if a chat with the hostId exists
                    for (var document : querySnapshot) {
                        ArrayList<String> participants = (ArrayList<String>) document.get("participants");
                        if (participants != null && participants.contains(hostId)) {
                            existingChatId = document.getId();
                            break;
                        }
                    }

                    if (existingChatId != null) {
                        // Chat already exists, navigate to ChatDetailsActivity
                        navigateToChatDetailsActivity(existingChatId);
                    } else {
                        // Chat does not exist, create a new one
                        createNewChat(currentLoginUserId, hostId);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("ChatDetailsActivity", "Error checking existing chats", e);
                    Toast.makeText(this, "Error checking chat", Toast.LENGTH_SHORT).show();
                });
    }

    private void createNewChat(String currentLoginUserId, String hostId) {
        // Prepare the initial chat document
        Map<String, Object> newChat = new HashMap<>();
        ArrayList<String> participants = new ArrayList<>();
        participants.add(currentLoginUserId);
        participants.add(hostId);
        newChat.put("participants", participants);
        newChat.put("createdAt", new Date());
        newChat.put("messages", new ArrayList<Map<String, Object>>());

        // Add the new chat to Firestore
        db.collection("Chats")
                .add(newChat)
                .addOnSuccessListener(documentReference -> {
                    String newChatId = documentReference.getId();
                    Toast.makeText(this, "Chat created successfully", Toast.LENGTH_SHORT).show();
                    navigateToChatDetailsActivity(newChatId);
                })
                .addOnFailureListener(e -> {
                    Log.e("ChatDetailsActivity", "Error creating chat", e);
                    Toast.makeText(this, "Error creating chat", Toast.LENGTH_SHORT).show();
                });
    }

    private void navigateToChatDetailsActivity(String chatId) {
        // Navigate to ChatDetailsActivity with the chatId
        Intent intent = new Intent(this, ChatDetailsActivity.class);
        intent.putExtra("chatId", chatId);
        startActivity(intent);
    }

}
