package com.example.chillpoint.views.models;

import java.util.ArrayList;

public class Bill {
    String id;
    ArrayList<String> receiptIds;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public ArrayList<String> getReceiptIds() {
        return receiptIds;
    }

    public void setReceiptIds(ArrayList<String> receiptIds) {
        this.receiptIds = receiptIds;
    }
}
