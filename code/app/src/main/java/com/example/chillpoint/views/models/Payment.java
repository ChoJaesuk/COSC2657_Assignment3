package com.example.chillpoint.views.models;

public class Payment {
    private String paymentId;
    private String userId;
    private String propertyId;
    private double amount;
    private String transactionDate;

    public Payment() {}

    public Payment(String paymentId, String userId, String propertyId, double amount, String transactionDate) {
        this.paymentId = paymentId;
        this.userId = userId;
        this.propertyId = propertyId;
        this.amount = amount;
        this.transactionDate = transactionDate;
    }

    // Getters and Setters
    public String getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(String paymentId) {
        this.paymentId = paymentId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getPropertyId() {
        return propertyId;
    }

    public void setPropertyId(String propertyId) {
        this.propertyId = propertyId;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getTransactionDate() {
        return transactionDate;
    }

    public void setTransactionDate(String transactionDate) {
        this.transactionDate = transactionDate;
    }
}

