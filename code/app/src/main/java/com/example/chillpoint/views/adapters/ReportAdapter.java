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
import com.example.chillpoint.views.models.Report;

import java.util.ArrayList;

public class ReportAdapter extends RecyclerView.Adapter<ReportAdapter.ViewHolder> {
    private final Context context;
    private final ArrayList<Report> reportList;

    public ReportAdapter(Context context, ArrayList<Report> reportList) {
        this.context = context;
        this.reportList = reportList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_report, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Report report = reportList.get(position);

        holder.categoryTextView.setText(report.getCategory());
        holder.contentTextView.setText(report.getContent());
        holder.statusTextView.setText("Status: " + report.getStatus());
        holder.dateTextView.setText(report.getCreatedAt());

        // Load image if available
        if (!report.getImageUrl().isEmpty()) {
            Glide.with(context)
                    .load(report.getImageUrl().get(0)) // Load the first image
                    .into(holder.reportImageView);
        } else {
            holder.reportImageView.setImageResource(R.drawable.ic_profile);
        }
    }

    @Override
    public int getItemCount() {
        return reportList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView categoryTextView, contentTextView, statusTextView, dateTextView;
        ImageView reportImageView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            categoryTextView = itemView.findViewById(R.id.categoryTextView);
            contentTextView = itemView.findViewById(R.id.contentTextView);
            statusTextView = itemView.findViewById(R.id.statusTextView);
            dateTextView = itemView.findViewById(R.id.dateTextView);
            reportImageView = itemView.findViewById(R.id.reportImageView);
        }
    }
}
