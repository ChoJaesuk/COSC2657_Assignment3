package com.example.chillpoint.views.activities;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.chillpoint.R;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_details);

        db = FirebaseFirestore.getInstance();
        chatId = getIntent().getStringExtra("chatId");

        messageListView = findViewById(R.id.message_list);
        messageInput = findViewById(R.id.message_input);
        sendButton = findViewById(R.id.send_button);

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, messages);
        messageListView.setAdapter(adapter);

        loadMessages();

        sendButton.setOnClickListener(v -> sendMessage());
    }

    private void loadMessages() {
        db.collection("chats")
                .document(chatId)
                .collection("messages")
                .orderBy("createdAt", Query.Direction.ASCENDING)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        Toast.makeText(ChatDetailsActivity.this, "Error loading messages", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    messages.clear();
                    for (var document : snapshots) {
                        String content = document.getString("sender") + ": " + document.getString("content");
                        messages.add(content);
                    }
                    adapter.notifyDataSetChanged();
                });
    }

    private void sendMessage() {
        String content = messageInput.getText().toString();
        if (content.isEmpty()) {
            Toast.makeText(this, "Message cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> message = new HashMap<>();
        message.put("sender", "current_user"); // Replace with the actual username
        message.put("content", content);
        message.put("createdAt", new Date());

        db.collection("chats")
                .document(chatId)
                .collection("messages")
                .add(message)
                .addOnSuccessListener(documentReference -> {
                    messageInput.setText("");
                    Toast.makeText(ChatDetailsActivity.this, "Message sent", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> Toast.makeText(ChatDetailsActivity.this, "Error sending message", Toast.LENGTH_SHORT).show());
    }
}
