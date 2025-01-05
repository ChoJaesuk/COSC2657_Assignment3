package com.example.chillpoint.views.activities;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chillpoint.R;
import com.example.chillpoint.managers.SessionManager;
import com.example.chillpoint.repositories.PropertyRepository;
import com.example.chillpoint.repositories.WishlistRepository;
import com.example.chillpoint.views.adapters.WishlistAdapter;
import com.example.chillpoint.views.models.Property;
import com.example.chillpoint.views.models.WishlistItem;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class WishlistActivity extends AppCompatActivity implements  WishlistAdapter.OnHeartClickListener {

    private RecyclerView recyclerWishlist;
    private WishlistAdapter wishlistAdapter;
    private ArrayList<WishlistItem> wishlistItems;
    private WishlistRepository wishlistRepository;
    private PropertyRepository propertyRepository;
    String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wishlist);

        wishlistRepository = new WishlistRepository();
        propertyRepository = new PropertyRepository();
        wishlistItems = new ArrayList<>();

        SessionManager sessionManager = new SessionManager(this);
        userId = sessionManager.getUserId();

        recyclerWishlist = findViewById(R.id.recycler_wishlist);

        // Setup RecyclerView
        wishlistAdapter = new WishlistAdapter(this, wishlistItems, this);
        recyclerWishlist.setLayoutManager(new GridLayoutManager(this, 2));
        recyclerWishlist.setAdapter(wishlistAdapter);

        // Fetch wishlist items
        if (userId != null) {
            fetchWishlistItems(userId);
        } else {
            Log.e("WishlistActivity", "User ID is null. Cannot fetch wishlist items.");
        }
    }

    private void fetchWishlistItems(String userId) {
        wishlistRepository.getAllWishlistItemsByUser(userId)
                .addOnSuccessListener(this::fetchProperties)
                .addOnFailureListener(e -> Log.e("WishlistActivity", "Error fetching wishlist items: " + e.getMessage()));
    }

    private void fetchProperties(List<String> propertyIds) {
        for (String propertyId : propertyIds) {
            Log.d("WishlistActivity", "Fetching property with ID: " + propertyId);
            propertyRepository.getPropertyById(propertyId)
                    .addOnSuccessListener(this::addPropertyToWishlist)
                    .addOnFailureListener(e -> Log.e("WishlistActivity", "Error fetching property: " + e.getMessage()));
        }
    }


    @SuppressLint("NotifyDataSetChanged")
    private void addPropertyToWishlist(Property property) {
        if (property != null) {
            Log.d("WishlistActivity", "Property ID: " + property.getId());
            Log.d("WishlistActivity", "Property Name: " + property.getName());
            Log.d("WishlistActivity", "Property Price: " + property.getPricePerNight());

            WishlistItem wishlistItem = new WishlistItem(
                    property.getId(),
                    property.getName(),
                    "$" + property.getPricePerNight() + "/night",
                    property.getImages().get(0) // Assuming the first image is available
            );
            wishlistItems.add(wishlistItem);
            wishlistAdapter.notifyDataSetChanged();
        }
    }


    @Override
    public void onHeartClick(WishlistItem item, int position) {
        showDislikeDialog(item, position);
    }
    private void showDislikeDialog(WishlistItem item, int position) {
        new AlertDialog.Builder(this)
                .setTitle("Remove from Wishlist")
                .setMessage("Are you sure you want to remove " + item.getTitle() + " from your wishlist?")
                .setPositiveButton("Yes", (dialog, which) -> deleteWishlistItem(item, position))
                .setNegativeButton("No", (dialog, which) -> dialog.dismiss())
                .create()
                .show();
    }
    private void deleteWishlistItem(WishlistItem item, int position) {
        Log.e("WishlistActivity", "Deleting wishlist item with ID: " + item.getItemId());
        wishlistRepository.deleteWishlistItem(userId, item.getItemId())
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Removed from wishlist", Toast.LENGTH_SHORT).show();
                    wishlistItems.remove(position);
                    wishlistAdapter.notifyItemRemoved(position);
                })
                .addOnFailureListener(e -> {
                    if (Objects.requireNonNull(e.getMessage()).contains("No matching wishlist item")) {
                        Toast.makeText(this, "Item not found in wishlist", Toast.LENGTH_SHORT).show();
                    }
                    Log.e("WishlistActivity", "Error: " + e.getMessage());
                });

    }
    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {
        super.onPointerCaptureChanged(hasCapture);
    }
}
