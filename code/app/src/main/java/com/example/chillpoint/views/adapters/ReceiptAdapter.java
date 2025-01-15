package com.example.chillpoint.views.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chillpoint.R;
import com.example.chillpoint.views.models.Receipt;

import java.util.List;

public class ReceiptAdapter extends RecyclerView.Adapter<ReceiptAdapter.ReceiptViewHolder> {

    private List<Receipt> receiptList;
    private OnReceiptClickListener listener;

    public ReceiptAdapter(List<Receipt> receiptList, OnReceiptClickListener listener) {
        this.receiptList = receiptList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ReceiptViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_receipt, parent, false);
        return new ReceiptViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReceiptViewHolder holder, int position) {
        Receipt receipt = receiptList.get(position);
        holder.receiptIdTextView.setText("Receipt ID: " + receipt.getId());
        holder.usernameTextView.setText("Name: " + receipt.getUsername());
        holder.totalAmountTextView.setText("Total Amount: $" + receipt.getTotalAmount());
        holder.statusTextView.setText("Status: " + receipt.getStatus());

        holder.itemView.setOnClickListener(v -> listener.onReceiptClick(receipt));
    }

    @Override
    public int getItemCount() {
        return receiptList.size();
    }

    public interface OnReceiptClickListener {
        void onReceiptClick(Receipt receipt);
    }

    static class ReceiptViewHolder extends RecyclerView.ViewHolder {

        TextView receiptIdTextView, usernameTextView, totalAmountTextView, statusTextView;

        public ReceiptViewHolder(@NonNull View itemView) {
            super(itemView);
            receiptIdTextView = itemView.findViewById(R.id.receiptIdTextView);
            usernameTextView = itemView.findViewById(R.id.usernameTextView);
            totalAmountTextView = itemView.findViewById(R.id.totalAmountTextView);
            statusTextView = itemView.findViewById(R.id.statusTextView);
        }
    }
}
