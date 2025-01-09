package com.example.chillpoint.views.adapters;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
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
import com.example.chillpoint.managers.SessionManager;
import com.example.chillpoint.views.activities.BookingDetailActivity;
import com.example.chillpoint.views.activities.PropertyDetailActivity;
import com.example.chillpoint.views.activities.ReviewActivity;
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

        // Bind booking details
        holder.propertyName.setText(booking.getPropertyName());
        holder.location.setText(booking.getPropertyLocation());
        holder.dates.setText(booking.getStartDate() + " - " + booking.getEndDate());
        holder.status.setText(booking.getStatus());

        Log.e("BookingAdapter", "Start Date: " + booking.getStartDate());
        Log.e("BookingAdapter", "End Date: " + booking.getEndDate());
        // Load property image
        Glide.with(context)
                .load(booking.getImageUrl())
                .placeholder(R.drawable.image_placeholder)
                .into(holder.propertyImage);

        // Manage button click listener
        holder.manageButton.setOnClickListener(v -> {
            // Pass all required property details to PropertyDetailActivity
            Intent intent = new Intent(context, BookingDetailActivity.class);
            intent.putExtra("propertyId", booking.getPropertyId());
            intent.putExtra("name", booking.getPropertyName());
            intent.putExtra("address", booking.getPropertyLocation());
            intent.putExtra("price", "$" + booking.getPricePerNight() + " / night");
            intent.putExtra("description", booking.getDescription());
            intent.putExtra("bookingId", booking.getBookingId());
            intent.putExtra("dates", booking.getStartDate() + " - " + booking.getEndDate());
            intent.putStringArrayListExtra("images", booking.getImages());
            intent.putExtra("isFromMyBookings", true); // My Booking에서 호출됨을 알리는 플래그
            context.startActivity(intent);
        });


        // Enable or disable Review button based on status
        if ("Completed".equalsIgnoreCase(booking.getStatus())) {
            holder.reviewButton.setEnabled(true);
            holder.reviewButton.setAlpha(1.0f); // Fully visible
            holder.reviewButton.setOnClickListener(v -> {
                // Get user session details
                SessionManager sessionManager = new SessionManager(context);
                String userId = sessionManager.getUserId();
                String username = sessionManager.getUsername();
                String imageUrl = sessionManager.getUserImageUrl();
                // Navigate to ReviewActivity
                Intent intent = new Intent(context, ReviewActivity.class);
                intent.putExtra("bookingId", booking.getBookingId()); // Pass bookingId
                intent.putExtra("userId", userId); // Pass userId
                intent.putExtra("username", username); // Pass username
                intent.putExtra("propertyId", booking.getPropertyId());
                intent.putExtra("imageUrl", imageUrl);
                context.startActivity(intent);
            });
        } else {
            holder.reviewButton.setEnabled(false);
            holder.reviewButton.setAlpha(0.5f); // Dim the button to indicate it's disabled
            holder.reviewButton.setOnClickListener(null); // Remove click listener
        }
    }


    @Override
    public int getItemCount() {
        return bookings.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView propertyImage;
        TextView propertyName, location, dates, status;
        Button manageButton, reviewButton;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            propertyImage = itemView.findViewById(R.id.bookingImage);
            propertyName = itemView.findViewById(R.id.bookingPropertyName);
            location = itemView.findViewById(R.id.bookingPropertyLocation);
            dates = itemView.findViewById(R.id.bookingDates);
            status = itemView.findViewById(R.id.bookingStatus);
            manageButton = itemView.findViewById(R.id.manageBookingButton);
            reviewButton = itemView.findViewById(R.id.reviewBookingButton); // Add reference to review button
        }
    }
}
