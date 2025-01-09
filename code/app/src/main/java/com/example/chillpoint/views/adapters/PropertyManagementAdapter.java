package com.example.chillpoint.views.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.chillpoint.R;

import java.util.ArrayList;
import java.util.Map;

public class PropertyManagementAdapter extends RecyclerView.Adapter<PropertyManagementAdapter.PropertyViewHolder> {

    private Context context;
    private ArrayList<Map<String, Object>> properties;
    private OnEditPropertyListener onEditPropertyListener;
    private OnDeletePropertyListener onDeletePropertyListener;

    public PropertyManagementAdapter(Context context, ArrayList<Map<String, Object>> properties, OnEditPropertyListener editListener, OnDeletePropertyListener deleteListener) {
        this.context = context;
        this.properties = properties;
        this.onEditPropertyListener = editListener;
        this.onDeletePropertyListener = deleteListener;
    }

    @NonNull
    @Override
    public PropertyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_property_management, parent, false);
        return new PropertyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PropertyViewHolder holder, int position) {
        Map<String, Object> property = properties.get(position);

        holder.nameTextView.setText((String) property.get("name"));
        holder.addressTextView.setText((String) property.get("address"));
        holder.priceTextView.setText("$" + property.get("pricePerNight") + " / night");

        // Load property image
        ArrayList<String> images = (ArrayList<String>) property.get("images");
        if (images != null && !images.isEmpty()) {
            Glide.with(context)
                    .load(images.get(0))
                    .placeholder(R.drawable.image_placeholder)
                    .into(holder.propertyImageView);
        } else {
            holder.propertyImageView.setImageResource(R.drawable.image_placeholder);
        }

        // Edit Button Listener
        holder.editButton.setOnClickListener(v -> onEditPropertyListener.onEditProperty(property));

        // Delete Button Listener
        holder.deleteButton.setOnClickListener(v -> onDeletePropertyListener.onDeleteProperty(property));
    }

    @Override
    public int getItemCount() {
        return properties.size();
    }

    public interface OnEditPropertyListener {
        void onEditProperty(Map<String, Object> property);
    }

    public interface OnDeletePropertyListener {
        void onDeleteProperty(Map<String, Object> property);
    }

    static class PropertyViewHolder extends RecyclerView.ViewHolder {
        TextView nameTextView, addressTextView, priceTextView;
        ImageView propertyImageView;
        Button editButton, deleteButton;

        public PropertyViewHolder(@NonNull View itemView) {
            super(itemView);

            nameTextView = itemView.findViewById(R.id.propertyNameTextView);
            addressTextView = itemView.findViewById(R.id.propertyAddressTextView);
            priceTextView = itemView.findViewById(R.id.propertyPriceTextView);
            propertyImageView = itemView.findViewById(R.id.propertyImageView);
            editButton = itemView.findViewById(R.id.editButton);
            deleteButton = itemView.findViewById(R.id.deleteButton);
        }
    }
}
