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
import com.example.chillpoint.views.models.WishlistItem;

import java.util.List;

public class WishlistAdapter extends RecyclerView.Adapter<WishlistAdapter.WishlistViewHolder> {

    private Context context;
    private List<WishlistItem> wishlistItems;
    private final OnHeartClickListener heartClickListener;
    public WishlistAdapter(Context context, List<WishlistItem> wishlistItems, OnHeartClickListener heartClickListener) {
        this.context = context;
        this.wishlistItems = wishlistItems;
        this.heartClickListener = heartClickListener;
    }

    @NonNull
    @Override
    public WishlistViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_wishlist, parent, false);
        return new WishlistViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull WishlistViewHolder holder, int position) {
        WishlistItem item = wishlistItems.get(position);
        // Load the image from the URL using Glide
        Glide.with(holder.imgWishlistItem.getContext())
                .load(item.getImageResId())
                .placeholder(R.drawable.ic_launcher_background)
                .error(R.drawable.ic_launcher_background)
                .into(holder.imgWishlistItem);
        holder.tvRoomTitle.setText(item.getTitle());
        holder.tvRoomPrice.setText(item.getPrice());

        holder.imgWishlistHeart.setOnClickListener(v -> heartClickListener.onHeartClick(item, position));
    }

    @Override
    public int getItemCount() {
        return wishlistItems.size();
    }

    static class WishlistViewHolder extends RecyclerView.ViewHolder {
        ImageView imgWishlistItem, imgWishlistHeart;
        TextView tvRoomTitle, tvRoomPrice;

        public WishlistViewHolder(@NonNull View itemView) {
            super(itemView);
            imgWishlistItem = itemView.findViewById(R.id.img_wishlist_item);
            imgWishlistHeart = itemView.findViewById(R.id.img_wishlist_heart);
            tvRoomTitle = itemView.findViewById(R.id.tv_room_title);
            tvRoomPrice = itemView.findViewById(R.id.tv_room_price);
        }
    }

    public interface OnHeartClickListener {
        void onHeartClick(WishlistItem item, int position);
    }
}
