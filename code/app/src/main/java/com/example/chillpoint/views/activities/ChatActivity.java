package com.example.chillpoint.views.activities;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chillpoint.R;
import com.example.chillpoint.repositories.ChatRepository;
import com.example.chillpoint.repositories.UserRepository;
import com.example.chillpoint.views.adapters.ChatAdapter;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatActivity extends AppCompatActivity {
    private FirebaseAuth auth;
    private RecyclerView chatRecyclerView;
    private final List<Map<String, String>> chatList = new ArrayList<>();
    private final List<String> chatIds = new ArrayList<>();
    private ChatAdapter chatAdapter;
    private ChatRepository chatRepository;
    private UserRepository userRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        chatRepository = new ChatRepository();
        userRepository = new UserRepository();
        chatRecyclerView = findViewById(R.id.chatRecyclerView);
        chatRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Add dividers
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(
                chatRecyclerView.getContext(),
                DividerItemDecoration.VERTICAL
        );
        chatRecyclerView.addItemDecoration(dividerItemDecoration);

        chatAdapter = new ChatAdapter(chatList, position -> {
            Intent intent = new Intent(ChatActivity.this, ChatDetailsActivity.class);
            intent.putExtra("chatId", chatIds.get(position));
            startActivity(intent);
        });

        chatRecyclerView.setAdapter(chatAdapter);
        loadChats("4Iz89vGyqtVzMd3Tc0gT9aaCkGG3");
    }

    @SuppressLint("NotifyDataSetChanged")
    private void loadChats(String userId) {
        chatRepository.getAllChatsByUserId(userId).addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                chatList.clear();
                chatIds.clear();

                task.getResult().forEach(document -> {
                    chatIds.add(document.getId());
                    Map<String, String> chatItem = new HashMap<>();

                    // Retrieve participant(s) from the document
                    List<String> participants = (List<String>) document.get("participants");
                    if (participants != null && !participants.isEmpty()) {
                        String lastParticipantId = participants.stream().filter(participantId -> !participantId.equals(userId)).findFirst().orElse(null);
                        // Get the last participant who is not the current user
                        // Asynchronously retrieve the username using the getUsernameByUserId method
                        userRepository.getUsernameByUserId(lastParticipantId)
                                .addOnSuccessListener(username -> {
                                    chatAdapter.notifyDataSetChanged();
                                    Log.d("ChatActivity", "username: " + username);
                                    // Once the username is retrieved, update the chatItem with the last participant's username
                                    chatItem.put("lastParticipant", username != null ? username : "Unknown User");
                                })
                                .addOnFailureListener(e -> {
                                    // Handle failure case
                                    chatItem.put("lastParticipant", "Unknown User");
                                    Log.e("ChatActivity", "Error retrieving username for participant: " + lastParticipantId, e);
                                });
                    } else {
                        chatItem.put("lastParticipant", "Unknown User");
                    }



                    // Handle messages
                    List<Map<String, Object>> messages = (List<Map<String, Object>>) document.get("messages");
                    if (messages != null && !messages.isEmpty()) {
                        Map<String, Object> lastMessageMap = messages.get(messages.size() - 1);
                        Map<String, Object> lastMessage = (Map<String, Object>) lastMessageMap.get("message");
                        // Retrieve the content from the 'message' object
                        String content = (String) lastMessage.get("content");
                        // Add the message content to the chat item
                        chatItem.put("lastMessage", content != null ? content : "No messages yet");
                    } else {
                        Log.d("ChatActivity", "No messages available");
                        chatItem.put("lastMessage", "No messages yet");
                    }

                    chatList.add(chatItem);
                });

                chatAdapter.notifyDataSetChanged();
            } else {
                Toast.makeText(ChatActivity.this, "Error loading chats", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
