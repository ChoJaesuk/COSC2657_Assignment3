package com.example.chillpoint.views.activities;

import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.chillpoint.R;
import com.example.chillpoint.managers.SessionManager;
import com.example.chillpoint.repositories.UserRepository;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class ChatDetailsActivity extends AppCompatActivity {
    private FirebaseFirestore db;
    private String chatId;
    private ListView messageListView;
    private EditText messageInput;
    private Button sendButton;
    private ArrayList<String> messages = new ArrayList<>();
    private ArrayAdapter<String> adapter;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_details);
        sessionManager = new SessionManager(this);

        db = FirebaseFirestore.getInstance();
        chatId = getIntent().getStringExtra("chatId");

        messageListView = findViewById(R.id.message_list);
        messageInput = findViewById(R.id.message_input);
        sendButton = findViewById(R.id.send_button);

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, messages);
        messageListView.setAdapter(adapter);

        loadMessages();

        sendButton.setOnClickListener(v -> sendMessage(sessionManager.getUserId()));
    }

    private void loadMessages() {
        db.collection("Chats")
                .document(chatId)
                .addSnapshotListener((documentSnapshot, error) -> {
                    if (error != null) {
                        Log.e("ChatDetailsActivity", "Error listening for messages", error);
                        Toast.makeText(ChatDetailsActivity.this, "Error loading messages", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (documentSnapshot != null && documentSnapshot.exists()) {
                        Log.d("ChatDetailsActivity", "Real-time update: " + documentSnapshot.getData());

                        messages.clear();
                        ArrayList<Map<String, Object>> messageArray = (ArrayList<Map<String, Object>>) documentSnapshot.get("messages");

                        if (messageArray != null) {
                            for (Map<String, Object> wrapper : messageArray) {
                                if (wrapper != null) {
                                    // Extract the "message" map from the wrapper
                                    Map<String, Object> message = (Map<String, Object>) wrapper.get("message");
                                    if (message != null) {
                                        String senderId = (String) message.get("senderId");
                                        String content = (String) message.get("content");

                                        if (senderId != null && content != null) {
                                            // Fetch user details for the senderId
                                            fetchUserName(senderId, content);
                                        } else {
                                            Log.d("ChatDetailsActivity", "Message fields are null: " + message);
                                        }
                                    }
                                }
                            }
                        } else {
                            Log.d("ChatDetailsActivity", "No messages found in document");
                        }
                        adapter.notifyDataSetChanged();
                    } else {
                        Log.d("ChatDetailsActivity", "Document does not exist");
                        Toast.makeText(ChatDetailsActivity.this, "Chat not found", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // Fetch user name using UserRepository
    private void fetchUserName(String senderId, String content) {
        UserRepository userRepository = new UserRepository();

        userRepository.getUserDetails(senderId).addOnSuccessListener(snapshot -> {
            if (snapshot != null && snapshot.exists()) {
                String username = snapshot.getString("username");
                if (username != null) {
                    messages.add(username + ": " + content);
                    adapter.notifyDataSetChanged();
                } else {
                    Log.e("ChatDetailsActivity", "Username not found for senderId: " + senderId);
                }
            }
        }).addOnFailureListener(e -> {
            Log.e("ChatDetailsActivity", "Failed to fetch user details for senderId: " + senderId, e);
        });
    }


    private void sendMessage(String userId) {
        String content = messageInput.getText().toString();
        if (content.isEmpty()) {
            Toast.makeText(this, "Message cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create a new message map
        Map<String, Object> newMessage = new HashMap<>();
        newMessage.put("content", content);
        newMessage.put("senderId", userId); // Replace with the actual user ID
        newMessage.put("createdAt", new Date());

        // Wrap the message in the structure used in Firestore
        Map<String, Object> messageWrapper = new HashMap<>();
        messageWrapper.put("message", newMessage);

        // Update the messages array in Firestore
        db.collection("Chats")
                .document(chatId)
                .update("messages", FieldValue.arrayUnion(messageWrapper))
                .addOnSuccessListener(unused -> {
                    messageInput.setText(""); // Clear the input field
                    Toast.makeText(ChatDetailsActivity.this, "Message sent", Toast.LENGTH_SHORT).show();
                    loadMessages();
                })
                .addOnFailureListener(e -> {
                    Log.e("ChatDetailsActivity", "Error sending message", e);
                    Toast.makeText(ChatDetailsActivity.this, "Error sending message", Toast.LENGTH_SHORT).show();
                });
    }

    private void contactUser(String currentLoginUserId, String hostId){
        //when clicking on the button "contact host" storing host Id
        //check in firestore if there is a chat between 2 users
        //if there is a chat, get the chat id and going straightforward to ChatDetailsAcitivity of that chat with loaded messages
        //if not, go to ChatActivityDetails to start conversation
    }
}
