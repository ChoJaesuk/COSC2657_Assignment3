package com.example.chillpoint.views.models;

public class Booking {
    private String propertyId;
    private String propertyName;
    private String propertyLocation;
    private String imageUrl;
    private String startDate;
    private String endDate;
    private String status;
    private String bookingId;
    private long totalPrice;
    public Booking() {
    }

    public Booking(String bookingId, String propertyId, String propertyName, String propertyLocation, String imageUrl, String startDate, String endDate, String status, long totalPrice) {
        this.propertyId = propertyId;
        this.propertyName = propertyName;
        this.propertyLocation = propertyLocation;
        this.imageUrl = imageUrl;
        this.startDate = startDate;
        this.endDate = endDate;
        this.status = status;
        this.bookingId = bookingId;
        this.totalPrice = totalPrice;

    }

    public String getPropertyId() {
        return propertyId;
    }

    public void setPropertyId(String propertyId) {
        this.propertyId = propertyId;
    }

    public String getPropertyName() {
        return propertyName;
    }

    public void setPropertyName(String propertyName) {
        this.propertyName = propertyName;
    }

    public String getPropertyLocation() {
        return propertyLocation;
    }

    public void setPropertyLocation(String propertyLocation) {
        this.propertyLocation = propertyLocation;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getStartDate() {
        return startDate;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public String getEndDate() {
        return endDate;
    }

    public void setEndDate(String endDate) {
        this.endDate = endDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
    // Getters and Setters
    public String getBookingId() {
        return bookingId;
    }
    public void setBookingId(String bookingId) {
        this.bookingId = bookingId;
    }
    public long getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(long totalPrice) {
        this.totalPrice = totalPrice;
    }
}
