package com.example.chillpoint.views.adapters;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.chillpoint.R;
import com.example.chillpoint.views.activities.NotificationItem;

import java.util.ArrayList;

public class NotificationAdapter extends android.widget.ArrayAdapter<NotificationItem> {

    public NotificationAdapter(Context context, ArrayList<NotificationItem> notifications) {
        super(context, 0, notifications);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        NotificationItem item = getItem(position);

        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_notification, parent, false);
        }

        TextView titleTextView = convertView.findViewById(R.id.notificationTitle);
        TextView messageTextView = convertView.findViewById(R.id.notificationMessage);
        TextView timestampTextView = convertView.findViewById(R.id.notificationTimestamp);

        titleTextView.setText(item.title);
        messageTextView.setText(item.message);
        timestampTextView.setText(item.timestamp.toDate().toString());

        // Highlight unread notifications
        if (!item.isRead) {
            convertView.setBackgroundColor(Color.parseColor("#FFF6E5")); // 연한 주황색 배경
        } else {
            convertView.setBackgroundColor(Color.WHITE); // 흰색 배경
        }

        return convertView;
    }
}
