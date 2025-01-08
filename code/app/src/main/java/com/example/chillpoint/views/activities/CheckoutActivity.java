package com.example.chillpoint.views.activities;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.util.Log;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.chillpoint.R;
import com.example.chillpoint.managers.SessionManager;
import com.stripe.android.PaymentConfiguration;
import com.stripe.android.paymentsheet.PaymentSheet;
import com.stripe.android.paymentsheet.PaymentSheetResult;
import okhttp3.*;
import com.google.firebase.firestore.FirebaseFirestore;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

public class CheckoutActivity extends AppCompatActivity {
    private static final String TAG = "CheckoutActivity";
    private static final String BACKEND_URL = "http://10.0.2.2:4242";

    private String paymentIntentClientSecret;
    private PaymentSheet paymentSheet;

    private Button payButton;
    private FirebaseFirestore db;
    private String propertyId;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_checkout);
        PaymentConfiguration.init(
                getApplicationContext(),
                "pk_test_51QeHVRCEu9hglsZOJi2iypmyk0Pq3BEgh7HaSMf8GUdeXohp9diQnSLVZL511yw7ypAT1yx1xwN8pVnVxwkGI2cA00X1hScVXa"
        );

        db = FirebaseFirestore.getInstance();

        SessionManager sessionManager = new SessionManager(this);

        // Retrieve data from Intent
        Intent incomingIntent = getIntent();
        String userId = incomingIntent.getStringExtra("userId");
        String username = incomingIntent.getStringExtra("username");
        propertyId = incomingIntent.getStringExtra("propertyId");

        if (userId == null || username == null) {
            Toast.makeText(this, "Failed to load session. Please log in again.", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(CheckoutActivity.this, LoginActivity.class));
            finish();
            return;
        }

        // Hook up the pay button
        payButton = findViewById(R.id.pay_button);
        payButton.setOnClickListener(this::onPayClicked);
        payButton.setEnabled(false);

        paymentSheet = new PaymentSheet(this, this::onPaymentSheetResult);

        fetchPaymentIntent();
    }

    private void showAlert(String title, @Nullable String message) {
        runOnUiThread(() -> {
            AlertDialog dialog = new AlertDialog.Builder(this)
                    .setTitle(title)
                    .setMessage(message)
                    .setPositiveButton("Ok", null)
                    .create();
            dialog.show();
        });
    }

    private void showToast(String message) {
        runOnUiThread(() -> Toast.makeText(this, message, Toast.LENGTH_LONG).show());
    }

    private void fetchPaymentIntent() {
        final String shoppingCartContent = "{\"items\": [{\"id\":\"xl-tshirt\", \"amount\": 5000}]}";

        final RequestBody requestBody = RequestBody.create(
                shoppingCartContent,
                MediaType.get("application/json; charset=utf-8")
        );

        Request request = new Request.Builder()
                .url(BACKEND_URL + "/create-payment-intent")
                .post(requestBody)
                .build();

        new OkHttpClient()
                .newCall(request)
                .enqueue(new Callback() {
                    @Override
                    public void onFailure(@NonNull Call call, @NonNull IOException e) {
                        showAlert("Failed to load data", "Error: " + e.toString());
                    }

                    @Override
                    public void onResponse(
                            @NonNull Call call,
                            @NonNull Response response
                    ) throws IOException {
                        if (!response.isSuccessful()) {
                            showAlert(
                                    "Failed to load page",
                                    "Error: " + response.toString()
                            );
                        } else {
                            final JSONObject responseJson = parseResponse(response.body());
                            paymentIntentClientSecret = responseJson.optString("clientSecret");
                            runOnUiThread(() -> payButton.setEnabled(true));
                            Log.i(TAG, "Retrieved PaymentIntent");
                        }
                    }
                });
    }

    private JSONObject parseResponse(ResponseBody responseBody) {
        if (responseBody != null) {
            try {
                return new JSONObject(responseBody.string());
            } catch (IOException | JSONException e) {
                Log.e(TAG, "Error parsing response", e);
            }
        }

        return new JSONObject();
    }

    private void onPayClicked(View view) {
        PaymentSheet.Configuration configuration = new PaymentSheet.Configuration.Builder("Example, Inc.")
                .build();

        // Present Payment Sheet
        paymentSheet.presentWithPaymentIntent(paymentIntentClientSecret, configuration);
    }


    private void onPaymentSheetResult(final PaymentSheetResult paymentSheetResult) {
        if (paymentSheetResult instanceof PaymentSheetResult.Completed) {
            showToast("Payment complete!");
            recordPaymentInFirestore();
        } else if (paymentSheetResult instanceof PaymentSheetResult.Canceled) {
            Log.i(TAG, "Payment canceled!");
        } else if (paymentSheetResult instanceof PaymentSheetResult.Failed) {
            Throwable error = ((PaymentSheetResult.Failed) paymentSheetResult).getError();
            showAlert("Payment failed", error.getLocalizedMessage());
        }
    }

    private void recordPaymentInFirestore() {
        // Create a payment record
        HashMap<String, Object> paymentRecord = new HashMap<>();
        paymentRecord.put("propertyId", propertyId); // The property being paid for
        paymentRecord.put("userId", new SessionManager(this).getUserId());
        paymentRecord.put("username", new SessionManager(this).getUsername());
        paymentRecord.put("amount", 5000);
        paymentRecord.put("status", "success");
        paymentRecord.put("timestamp", System.currentTimeMillis());

        db.collection("Payments")
                .add(paymentRecord)
                .addOnSuccessListener(documentReference -> {
                    Log.d(TAG, "Payment recorded with ID: " + documentReference.getId());
                    showToast("Payment recorded successfully!");
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Error adding payment record", e);
                    showToast("Failed to record payment. Please contact support.");
                });
    }
}