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
import com.example.chillpoint.views.models.Review;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class ReviewAdapter extends RecyclerView.Adapter<ReviewAdapter.ViewHolder> {
    private final Context context;
    private List<Review> reviews;

    public ReviewAdapter(Context context, List<Review> reviews) {
        this.context = context;
        this.reviews = reviews;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_review, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Review review = reviews.get(position);

        // Set username and rating
        holder.userNameTextView.setText(review.getUsername() + " ★" + review.getRating());

        // Set feedback
        holder.feedbackTextView.setText(review.getFeedback());

        // Format and set review date
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String formattedDate = sdf.format(review.getTimestamp());
        holder.dateTextView.setText(formattedDate);

        // Load user profile image
        Glide.with(context)
                .load(review.getUserImageUrl())
                .placeholder(R.drawable.default_host_image)
                .into(holder.userImageView);
    }

    @Override
    public int getItemCount() {
        return reviews.size();
    }

    public void updateReviews(List<Review> newReviews) {
        this.reviews = newReviews;
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView userImageView;
        TextView userNameTextView, dateTextView, feedbackTextView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            userImageView = itemView.findViewById(R.id.reviewUserImageView);
            userNameTextView = itemView.findViewById(R.id.reviewUserTextView);
            dateTextView = itemView.findViewById(R.id.reviewDateTextView);
            feedbackTextView = itemView.findViewById(R.id.reviewContentTextView);
        }
    }
}
