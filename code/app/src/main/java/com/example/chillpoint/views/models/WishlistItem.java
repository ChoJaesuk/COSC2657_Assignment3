package com.example.chillpoint.views.models;

public class WishlistItem {
    private String itemId;
    private String title;
    private String price;
    private String imageResId;

    public WishlistItem(String itemId,String title, String price, String imageResId) {
        this.itemId = itemId;
        this.title = title;
        this.price = price;
        this.imageResId = imageResId;
    }

    public String getItemId() { return itemId; }
    public String getTitle() {
        return title;
    }

    public String getPrice() {
        return price;
    }

    public String getImageResId() {
        return imageResId;
    }
}
