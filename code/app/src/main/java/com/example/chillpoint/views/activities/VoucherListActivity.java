package com.example.chillpoint.views.activities;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chillpoint.R;
import com.example.chillpoint.managers.SessionManager;
import com.example.chillpoint.views.adapters.VoucherAdapter;
import com.example.chillpoint.views.models.Voucher;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class VoucherListActivity extends AppCompatActivity {
    private RecyclerView voucherRecyclerView;
    private VoucherAdapter voucherAdapter;
    private List<Voucher> voucherList;
    private FirebaseFirestore firestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_voucher_list);

        // Firestore 초기화
        firestore = FirebaseFirestore.getInstance();

        // RecyclerView 설정
        voucherRecyclerView = findViewById(R.id.voucherRecyclerView);
        voucherRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Adapter와 리스트 초기화
        voucherList = new ArrayList<>();
        voucherAdapter = new VoucherAdapter(this, voucherList, this::collectVoucher);
        voucherRecyclerView.setAdapter(voucherAdapter);

        // 바우처 로드
        loadVouchers();
    }

    private void loadVouchers() {
        String userId = new SessionManager(this).getUserId();

        if (userId == null) {
            Toast.makeText(this, "Session expired. Please log in again.", Toast.LENGTH_SHORT).show();
            return;
        }

        firestore.collection("Users")
                .document(userId)
                .get()
                .addOnSuccessListener(userSnapshot -> {
                    List<String> collectedVouchers = (List<String>) userSnapshot.get("voucherIds");

                    firestore.collection("Vouchers")
                            .get()
                            .addOnSuccessListener(queryDocumentSnapshots -> {
                                voucherList.clear();
                                if (!queryDocumentSnapshots.isEmpty()) {
                                    queryDocumentSnapshots.forEach(documentSnapshot -> {
                                        Voucher voucher = documentSnapshot.toObject(Voucher.class);
                                        voucher.setId(documentSnapshot.getId());

                                        // 이미 수집한 바우처인지 확인
                                        if (collectedVouchers == null || !collectedVouchers.contains(voucher.getId())) {
                                            voucherList.add(voucher);
                                        }
                                    });
                                    voucherAdapter.notifyDataSetChanged();
                                } else {
                                    Toast.makeText(this, "No vouchers available", Toast.LENGTH_SHORT).show();
                                }
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(this, "Failed to load vouchers", Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to fetch user data", Toast.LENGTH_SHORT).show();
                });
    }


    private void collectVoucher(Voucher voucher) {
        String userId = new SessionManager(this).getUserId();

        if (userId == null) {
            Toast.makeText(this, "Session expired. Please log in again.", Toast.LENGTH_SHORT).show();
            return;
        }

        firestore.collection("Users")
                .document(userId)
                .update("voucherIds", FieldValue.arrayUnion(voucher.getId()))
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Collected Voucher: " + voucher.getId(), Toast.LENGTH_SHORT).show();
                    voucherList.remove(voucher);
                    voucherAdapter.notifyDataSetChanged(); // UI 업데이트
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to collect voucher: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

}
