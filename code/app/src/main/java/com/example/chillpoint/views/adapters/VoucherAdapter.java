package com.example.chillpoint.views.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chillpoint.R;
import com.example.chillpoint.views.models.Voucher;

import java.util.List;

public class VoucherAdapter extends RecyclerView.Adapter<VoucherAdapter.ViewHolder> {
    private final Context context;
    private final List<Voucher> vouchers;
    private final OnVoucherClickListener onVoucherClickListener;

    public interface OnVoucherClickListener {
        void onCollectVoucher(Voucher voucher);
    }

    public VoucherAdapter(Context context, List<Voucher> vouchers, OnVoucherClickListener onVoucherClickListener) {
        this.context = context;
        this.vouchers = vouchers;
        this.onVoucherClickListener = onVoucherClickListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_voucher, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Voucher voucher = vouchers.get(position);

        // 할인율 변환 및 바인딩
        holder.discountBadge.setText(String.format("%d%% OFF", (int) (voucher.getAmountOfDiscount() * 100)));

        // 나머지 데이터 바인딩
        holder.voucherCode.setText("CODE: " + voucher.getId());
        holder.voucherDates.setText(String.format("Valid: %s to %s", voucher.getStartDate(), voucher.getEndDate()));
        holder.voucherContent.setText(voucher.getContent());

        // "Collect" 버튼 클릭 처리
        holder.collectVoucher.setOnClickListener(v -> {
            if (onVoucherClickListener != null) {
                onVoucherClickListener.onCollectVoucher(voucher);
            }
        });
    }


    @Override
    public int getItemCount() {
        return vouchers.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView discountBadge, voucherCode, voucherDates, voucherContent, voucherStatus, collectVoucher;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            discountBadge = itemView.findViewById(R.id.voucherDiscountBadge);
            voucherCode = itemView.findViewById(R.id.voucherCodeTextView);
            voucherDates = itemView.findViewById(R.id.voucherDatesTextView);
            voucherContent = itemView.findViewById(R.id.voucherContentTextView);
            collectVoucher = itemView.findViewById(R.id.collectVoucherTextView);
        }
    }
}
