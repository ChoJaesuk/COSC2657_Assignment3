package com.example.chillpoint.views.adapters;

import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;

import com.example.chillpoint.R;

import java.util.ArrayList;

public class ImageAdapter extends BaseAdapter {

    private Context context;
    private ArrayList<Uri> imageUris;
    private OnImageRemoveListener onImageRemoveListener;

    public ImageAdapter(Context context, ArrayList<Uri> imageUris) {
        this.context = context;
        this.imageUris = imageUris;
    }

    // Listener interface for removing images
    public interface OnImageRemoveListener {
        void onImageRemoved(int position);
    }

    public void setOnImageRemoveListener(OnImageRemoveListener listener) {
        this.onImageRemoveListener = listener;
    }

    @Override
    public int getCount() {
        return imageUris.size();
    }

    @Override
    public Object getItem(int position) {
        return imageUris.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;

        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_image, parent, false);
            holder = new ViewHolder();
            holder.imageView = convertView.findViewById(R.id.imageView);
            holder.deleteButton = convertView.findViewById(R.id.deleteButton);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        Uri imageUri = imageUris.get(position);

        // Set the image using the URI
        holder.imageView.setImageURI(imageUri);

        // Handle delete button click
        holder.deleteButton.setOnClickListener(v -> {
            if (onImageRemoveListener != null) {
                onImageRemoveListener.onImageRemoved(position);
            }
        });

        return convertView;
    }

    private static class ViewHolder {
        ImageView imageView;
        ImageButton deleteButton;
    }
}
