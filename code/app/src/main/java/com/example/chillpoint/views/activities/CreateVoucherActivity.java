package com.example.chillpoint.views.activities;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.chillpoint.R;
import com.example.chillpoint.views.models.Voucher;
import com.google.firebase.firestore.FirebaseFirestore;

public class CreateVoucherActivity extends AppCompatActivity {

    private EditText startDateEditText, endDateEditText, discountEditText, contentEditText;
    private Button createVoucherButton;
    private FirebaseFirestore firestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_voucher);

        // Initialize Firestore
        firestore = FirebaseFirestore.getInstance();

        // Initialize views
        startDateEditText = findViewById(R.id.startDateEditText);
        endDateEditText = findViewById(R.id.endDateEditText);
        discountEditText = findViewById(R.id.discountEditText);
        contentEditText = findViewById(R.id.contentEditText);
        createVoucherButton = findViewById(R.id.createVoucherButton);

        // Set button click listener
        createVoucherButton.setOnClickListener(v -> createVoucher());
    }

    private void createVoucher() {
        String startDate = startDateEditText.getText().toString().trim();
        String endDate = endDateEditText.getText().toString().trim();
        String content = contentEditText.getText().toString().trim();

        double discount;
        try {
            discount = Double.parseDouble(discountEditText.getText().toString().trim());
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid discount value", Toast.LENGTH_SHORT).show();
            return;
        }

        if (startDate.isEmpty() || endDate.isEmpty() || content.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        Voucher voucher = new Voucher();
        voucher.setStartDate(startDate);
        voucher.setEndDate(endDate);
        voucher.setAmountOfDiscount(discount);
        voucher.setContent(content);

        firestore.collection("Vouchers")
                .add(voucher)
                .addOnSuccessListener(documentReference -> {
                    String documentId = documentReference.getId(); // Firestore 문서 ID 가져오기
                    voucher.setId(documentId); // `id` 필드 설정
                    firestore.collection("Vouchers")
                            .document(documentId)
                            .update("id", documentId) // `id` 필드를 업데이트
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(this, "Voucher created successfully with ID!", Toast.LENGTH_SHORT).show();
                                finish();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(this, "Failed to update voucher ID: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to create voucher: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });

    }
}
