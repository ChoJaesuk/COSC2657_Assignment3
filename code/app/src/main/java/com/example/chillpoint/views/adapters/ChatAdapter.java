package com.example.chillpoint.views.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.Map;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ChatViewHolder> {

    private final List<Map<String, String>> chatList;
    private final OnChatClickListener listener;

    public interface OnChatClickListener {
        void onChatClick(int position);
    }

    public ChatAdapter(List<Map<String, String>> chatList, OnChatClickListener listener) {
        this.chatList = chatList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Use the built-in simple_list_item_2 layout
        View view = LayoutInflater.from(parent.getContext())
                .inflate(android.R.layout.simple_list_item_2, parent, false);
        return new ChatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        // Get the current item as a map
        Map<String, String> chatItem = chatList.get(position);

        // Set the text fields with the data from the map
        holder.lastParticipantTextView.setText(chatItem.get("lastParticipant"));
        holder.lastMessageTextView.setText(chatItem.get("lastMessage"));

        // Set the click listener
        holder.itemView.setOnClickListener(v -> listener.onChatClick(position));
    }

    @Override
    public int getItemCount() {
        return chatList.size();
    }

    static class ChatViewHolder extends RecyclerView.ViewHolder {
        TextView lastParticipantTextView;
        TextView lastMessageTextView;

        public ChatViewHolder(@NonNull View itemView) {
            super(itemView);
            lastParticipantTextView = itemView.findViewById(android.R.id.text1);
            lastMessageTextView = itemView.findViewById(android.R.id.text2);
        }
    }
}
