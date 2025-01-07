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
import com.example.chillpoint.managers.SessionManager;
import com.example.chillpoint.repositories.ChatRepository;
import com.example.chillpoint.repositories.UserRepository;
import com.example.chillpoint.utils.NavigationSetup;
import com.example.chillpoint.utils.NavigationUtils;
import com.example.chillpoint.views.adapters.ChatAdapter;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatActivity extends AppCompatActivity implements NavigationSetup {
    private FirebaseAuth auth;
    private RecyclerView chatRecyclerView;
    private final List<Map<String, String>> chatList = new ArrayList<>();
    private final List<String> chatIds = new ArrayList<>();
    private ChatAdapter chatAdapter;
    private ChatRepository chatRepository;
    private UserRepository userRepository;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        setupNavigationBar();
        sessionManager = new SessionManager(this);
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
        loadChats(sessionManager.getUserId());
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
                        String lastParticipantId = participants.stream()
                                .filter(participantId -> !participantId.equals(userId))
                                .findFirst()
                                .orElse(null);

                        if (lastParticipantId != null) {
                            // Retrieve user details using the updated getUserDetails API
                            userRepository.getUserDetails(lastParticipantId)
                                    .addOnCompleteListener(userTask -> {
                                        if (userTask.isSuccessful() && userTask.getResult() != null) {
                                            String username = userTask.getResult().getString("username");
                                            chatItem.put("lastParticipant", username != null ? username : "Unknown User");
                                            chatAdapter.notifyDataSetChanged();
                                            Log.d("ChatActivity", "username: " + username);
                                        } else {
                                            chatItem.put("lastParticipant", "Unknown User");
                                            Log.e("ChatActivity", "Error retrieving username for participant: " + lastParticipantId, userTask.getException());
                                            chatAdapter.notifyDataSetChanged();
                                        }
                                    });
                        } else {
                            chatItem.put("lastParticipant", "Unknown User");
                        }
                    } else {
                        chatItem.put("lastParticipant", "Unknown User");
                    }

                    // Handle messages
                    List<Map<String, Object>> messages = (List<Map<String, Object>>) document.get("messages");
                    if (messages != null && !messages.isEmpty()) {
                        Map<String, Object> lastMessageMap = messages.get(messages.size() - 1);
                        Map<String, Object> lastMessage = (Map<String, Object>) lastMessageMap.get("message");
                        // Retrieve the content from the 'message' object
                        String content = lastMessage != null ? (String) lastMessage.get("content") : null;
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
                Log.e("ChatActivity", "Error loading chats", task.getException());
            }
        });
    }

    @Override
    public void setupNavigationBar() {
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setSelectedItemId(R.id.nav_inbox);
        NavigationUtils.handleBottomNavigation(this, bottomNavigationView);
    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {
        super.onPointerCaptureChanged(hasCapture);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadChats(sessionManager.getUserId());
    }
}
