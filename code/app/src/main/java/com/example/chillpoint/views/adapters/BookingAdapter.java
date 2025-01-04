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
import com.example.chillpoint.views.models.Booking;

import java.util.List;

public class BookingAdapter extends RecyclerView.Adapter<BookingAdapter.ViewHolder> {
    private final Context context;
    private final List<Booking> bookings;

    public BookingAdapter(Context context, List<Booking> bookings) {
        this.context = context;
        this.bookings = bookings;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_booking, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Booking booking = bookings.get(position);

        // Bind data
        holder.propertyName.setText(booking.getPropertyName());
        holder.location.setText(booking.getPropertyLocation());
        holder.dates.setText(booking.getStartDate() + " - " + booking.getEndDate());
        holder.status.setText(booking.getStatus());

        // Load image using Glide
        Glide.with(context)
                .load(booking.getImageUrl())
                .into(holder.propertyImage);

        holder.manageButton.setOnClickListener(v -> {
            // Handle manage button click
        });
    }

    @Override
    public int getItemCount() {
        return bookings.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView propertyImage;
        TextView propertyName, location, dates, status;
        Button manageButton;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            propertyImage = itemView.findViewById(R.id.bookingImage);
            propertyName = itemView.findViewById(R.id.bookingPropertyName);
            location = itemView.findViewById(R.id.bookingPropertyLocation);
            dates = itemView.findViewById(R.id.bookingDates);
            status = itemView.findViewById(R.id.bookingStatus);
            manageButton = itemView.findViewById(R.id.manageBookingButton);
        }
    }
}
