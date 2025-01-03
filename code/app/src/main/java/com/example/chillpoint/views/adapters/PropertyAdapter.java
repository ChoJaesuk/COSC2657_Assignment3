package com.example.chillpoint.views.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.chillpoint.R;
import com.example.chillpoint.views.models.Property;

import java.util.ArrayList;

public class PropertyAdapter extends RecyclerView.Adapter<PropertyAdapter.PropertyViewHolder> {
    private Context context;
    private ArrayList<Property> propertyList;

    public PropertyAdapter(Context context, ArrayList<Property> propertyList) {
        this.context = context;
        this.propertyList = propertyList;
    }

    @NonNull
    @Override
    public PropertyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_property, parent, false);
        return new PropertyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PropertyViewHolder holder, int position) {
        Property property = propertyList.get(position);

        // Set property details
        holder.nameTextView.setText(property.getName());
        holder.addressTextView.setText(property.getAddress());
        holder.priceTextView.setText("$" + property.getPricePerNight() + " / night");

        // Set property description
        String description = property.getDescription();
        holder.descriptionTextView.setText(description != null && !description.isEmpty()
                ? description
                : "No description available");

        // Load first image
        if (!property.getImages().isEmpty()) {
            Glide.with(context)
                    .load(property.getImages().get(0))
                    .placeholder(R.drawable.image_placeholder) // Optional placeholder image
                    .into(holder.propertyImageView);
        } else {
            holder.propertyImageView.setImageResource(R.drawable.image_placeholder); // Default image
        }
    }

    @Override
    public int getItemCount() {
        return propertyList.size();
    }

    static class PropertyViewHolder extends RecyclerView.ViewHolder {
        TextView nameTextView, addressTextView, priceTextView, descriptionTextView;
        ImageView propertyImageView;

        public PropertyViewHolder(@NonNull View itemView) {
            super(itemView);

            // Bind views
            nameTextView = itemView.findViewById(R.id.propertyNameTextView);
            addressTextView = itemView.findViewById(R.id.propertyAddressTextView);
            priceTextView = itemView.findViewById(R.id.propertyPriceTextView);
            descriptionTextView = itemView.findViewById(R.id.propertyDescriptionTextView);
            propertyImageView = itemView.findViewById(R.id.propertyImageView);
        }
    }
}
