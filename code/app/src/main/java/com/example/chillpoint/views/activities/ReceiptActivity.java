package com.example.chillpoint.views.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.example.chillpoint.R;
import com.example.chillpoint.managers.SessionManager;
import com.example.chillpoint.utils.NavigationSetup;
import com.example.chillpoint.utils.NavigationUtils;
import com.example.chillpoint.views.adapters.ReceiptAdapter;
import com.example.chillpoint.views.models.Receipt;
import com.example.chillpoint.views.models.Bill;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FirebaseFirestore;
import com.stripe.android.PaymentConfiguration;
import com.stripe.android.paymentsheet.PaymentSheet;
import com.stripe.android.paymentsheet.PaymentSheetResult;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReceiptActivity extends BaseActivity implements ReceiptAdapter.OnReceiptClickListener, NavigationSetup {

    private String PublishableKey = "pk_test_51QeHVRCEu9hglsZOJi2iypmyk0Pq3BEgh7HaSMf8GUdeXohp9diQnSLVZL511yw7ypAT1yx1xwN8pVnVxwkGI2cA00X1hScVXa";
    private String SecretKey = "sk_test_51QeHVRCEu9hglsZO3hULaZDemp0UQZZ3goIX1oxI4PjMyV7qEZ7WDVuHv5Nepbhs6RYG6uvvAPT7FJlxAYCeKqSR00JNalnVc6";
    private SessionManager sessionManager;
    private String EphemeralKey ;
    private String ClientSecret;
    private String CustomerId;
    private PaymentSheet paymentSheet;
    private RecyclerView receiptsRecyclerView;
    private ReceiptAdapter receiptAdapter;
    private List<Receipt> receiptList = new ArrayList<>();
    private FirebaseFirestore firestore = FirebaseFirestore.getInstance();

    // Receipt Details UI
    private View receiptDetailsLayout;
    private TextView receiptId, propertyId, username, email, phoneNumber, address, numberOfPayers, totalAmount, status, billId;
    private ImageButton closeButton;
    private Button bookNowButton,processPaymentButton;
    private LinearLayout paymentMethodLinearLayout;
    private double receiptTotalAmount;
    private String receiptBillId, receiptFromDate, receiptToDate, receiptHostId, receiptNumberOfGuests,receiptPropertyId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_receipt);
        setupNavigationBar();
        sessionManager = new SessionManager(this);
        // Initialize Stripe Payment configuration
        PaymentConfiguration.init(this, PublishableKey);

        // Initialize paymentSheet only when needed
        paymentSheet = new PaymentSheet(this, this::onPaymentResult);
        receiptsRecyclerView = findViewById(R.id.receiptsRecyclerView);
        receiptDetailsLayout = findViewById(R.id.receiptDetailsLayout);
        receiptDetailsLayout.setVisibility(View.GONE);
        paymentMethodLinearLayout = findViewById(R.id.paymentMethodLinearLayout);
        paymentMethodLinearLayout.setVisibility(View.GONE);

        // Initialize receipt details UI elements
        receiptId = findViewById(R.id.receiptId);
        propertyId = findViewById(R.id.propertyId);
        username = findViewById(R.id.username);
        email = findViewById(R.id.email);
        phoneNumber = findViewById(R.id.phoneNumber);
        address = findViewById(R.id.address);
        numberOfPayers = findViewById(R.id.numberOfPayers);
        totalAmount = findViewById(R.id.totalAmount);
        status = findViewById(R.id.status);
        billId = findViewById(R.id.billId);
        closeButton = findViewById(R.id.closeButton);
        bookNowButton = findViewById(R.id.bookNowButton);
        processPaymentButton = findViewById(R.id.processPaymentButton);


        // Initialize RecyclerView and Adapter
        receiptAdapter = new ReceiptAdapter(receiptList, this); // Pass `this` for the click listener
        receiptsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        receiptsRecyclerView.setAdapter(receiptAdapter);

        // Close button listener
        closeButton.setOnClickListener(v -> receiptDetailsLayout.setVisibility(View.GONE));
        loadReceipts();
        processPaymentButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                paymentFlow();
            }
        });
    }

    private void loadReceipts() {
        String userId = sessionManager.getUserId();
        if (userId == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        firestore.collection("Receipts")
                .whereEqualTo("payerId", userId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        receiptList.clear();
                        for (var document : querySnapshot.getDocuments()) {
                            Receipt receipt = document.toObject(Receipt.class);
                            if (receipt != null) {
                                receipt.setId(document.getId()); // Set the document ID
                                receiptList.add(receipt);
                            }
                        }
                        receiptAdapter.notifyDataSetChanged();
                    } else {
                        Toast.makeText(this, "No receipts found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("ReceiptActivity", "Error loading receipts", e);
                    Toast.makeText(this, "Error loading receipts", Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public void onReceiptClick(Receipt receipt) {
        onPaymentAction();
        // Populate receipt details UI
        receiptId.setText("Receipt ID: " + receipt.getId());
        propertyId.setText("Property ID: " + receipt.getPropertyId());
        username.setText("Name: " + receipt.getUsername());
        email.setText("Email: " + receipt.getEmail());
        phoneNumber.setText("Phone: " + receipt.getPhoneNumber());
        address.setText("Address: " + receipt.getAddress());
        numberOfPayers.setText("Number of Payers: " + receipt.getNumberOfPayers());
        totalAmount.setText("Total Amount: $" + receipt.getTotalAmount());
        receiptTotalAmount = receipt.getTotalAmount();
        status.setText("Status: " + receipt.getStatus());
        billId.setText("Bill ID: " + receipt.getBillId());
        receiptBillId = receipt.getBillId();
        Log.e("receiptBillId", "receiptBillId: " + receiptBillId);
        receiptFromDate = receipt.getFromDate();
        Log.e("receiptFromDate", "receiptFromDate: " + receiptFromDate);
        receiptToDate = receipt.getToDate();
        Log.e("receiptToDate", "receiptToDate: " + receiptToDate);
        receiptHostId = receipt.getHostId();
        Log.e("receiptHostId", "receiptHostId: " + receiptHostId);
        receiptNumberOfGuests = receipt.getNumberOfGuests();
        Log.e("receiptNumberOfGuests", "receiptNumberOfGuests: " + receiptNumberOfGuests);
        receiptPropertyId = receipt.getPropertyId();

        // Hide "Book Now" button for completed receipts
        if ("Completed".equalsIgnoreCase(receipt.getStatus())) {
            bookNowButton.setVisibility(View.GONE);
        } else {
            bookNowButton.setVisibility(View.VISIBLE);
            // Set up the click listener for "Book Now"
            bookNowButton.setOnClickListener(v -> {
                receiptDetailsLayout.setVisibility(View.GONE);
                paymentMethodLinearLayout.setVisibility(View.VISIBLE); // Show the payment method layout
            });
        }

        // Show receipt details layout
        receiptDetailsLayout.setVisibility(View.VISIBLE);

        // Hide the payment method layout initially
        paymentMethodLinearLayout.setVisibility(View.GONE);
    }

    private void onPaymentAction(){
        StringRequest request = new StringRequest(Request.Method.POST, "https://api.stripe.com/v1/customers", new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                try {
                    JSONObject object = new JSONObject(response);
                    CustomerId = object.getString("id");
                    Toast.makeText(ReceiptActivity.this,"Customer ID: " + CustomerId, Toast.LENGTH_SHORT).show();
                    getEphemeralKey();
                }catch (JSONException e){
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                String errorMessage = error.getLocalizedMessage();
                if (errorMessage == null) {
                    errorMessage = "An unexpected error occurred.";  // Fallback error message
                }

                // Check for 401 Unauthorized error
                if (error.networkResponse != null && error.networkResponse.statusCode == 401) {
                    errorMessage = "Authorization error: Invalid API key or permissions.";
                }

                Toast.makeText(ReceiptActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
            }

        }){
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headers = new HashMap<>();
                headers.put("Authorization", "Bearer " + SecretKey);
                return headers;
            }
        };

        RequestQueue requestQueue = Volley.newRequestQueue(this);
        requestQueue.add(request);
    }

    private void getEphemeralKey(){
        StringRequest request = new StringRequest(Request.Method.POST, "https://api.stripe.com/v1/ephemeral_keys", new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                try {
                    JSONObject object = new JSONObject(response);
                    EphemeralKey = object.getString("id");
                    Toast.makeText(ReceiptActivity.this,"EphemeralKey id: " + EphemeralKey, Toast.LENGTH_SHORT).show();
                    getClientSecret(CustomerId,EphemeralKey);
                }catch (JSONException e){
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(ReceiptActivity.this, error.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
            }
        }){
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headers = new HashMap<>();
                headers.put("Authorization", "Bearer " + SecretKey);
                headers.put("Stripe-Version", "2022-08-01");
                return headers;
            }

            @Nullable
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> params = new HashMap<>();
                params.put("customer", CustomerId);
                return params;
            }
        };

        RequestQueue requestQueue = Volley.newRequestQueue(this);
        requestQueue.add(request);
    }

    private void getClientSecret(String CustomerId, String EphemeralKey){
        StringRequest request = new StringRequest(Request.Method.POST, "https://api.stripe.com/v1/payment_intents", new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                try {
                    JSONObject object = new JSONObject(response);
                    ClientSecret = object.getString("client_secret");
                    Log.d("Stripe", "Client Secret: " + ClientSecret);
                    Toast.makeText(ReceiptActivity.this,"ClientSecret: " + ClientSecret, Toast.LENGTH_SHORT).show();

                }catch (JSONException e){
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(ReceiptActivity.this, error.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
            }
        }){
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headers = new HashMap<>();
                headers.put("Authorization", "Bearer " + SecretKey);
                return headers;
            }

            @Nullable
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> params = new HashMap<>();
                params.put("customer", CustomerId);
                int totalAmountInt = (int) Math.ceil(receiptTotalAmount);
                String totalAmountStr = String.valueOf(totalAmountInt);
                Log.e("totalAmountStr", "totalAmountStr: " + totalAmountStr);
                params.put("amount", totalAmountStr +"00");
                params.put("currency", "USD");
                params.put("automatic_payment_methods[enabled]", "true");
                return params;
            }
        };

        RequestQueue requestQueue = Volley.newRequestQueue(this);
        requestQueue.add(request);
    }

    private void onPaymentResult(PaymentSheetResult paymentSheetResult){
        if(paymentSheetResult instanceof PaymentSheetResult.Completed){
            Toast.makeText(this, "Payment completed", Toast.LENGTH_SHORT).show();
            updateStatusReceipt(sessionManager.getUserId(), receiptBillId);
            checkReceiptsAndCreateReservation(receiptBillId);
            paymentMethodLinearLayout.setVisibility(View.GONE);
            receiptDetailsLayout.setVisibility(View.GONE);
            loadReceipts();
        }else if(paymentSheetResult instanceof PaymentSheetResult.Canceled){
            Toast.makeText(this, "Payment canceled", Toast.LENGTH_SHORT).show();
        }
    }

    private void paymentFlow(){
        if (CustomerId == null || EphemeralKey == null || ClientSecret == null) {
            // Show an error message if any of the required values are missing
            Toast.makeText(this, "Customer ID, Ephemeral Key, or Client Secret is missing.", Toast.LENGTH_SHORT).show();
            return; // Prevent the flow if the required values are null
        }
        paymentSheet.presentWithPaymentIntent(ClientSecret, new PaymentSheet.Configuration("RMIT Froyo Group", new PaymentSheet.CustomerConfiguration(CustomerId,EphemeralKey)));
    }

    private void updateStatusReceipt(String userId, String billId) {
        // Query the Receipts collection with payerId and billId conditions
        firestore.collection("Receipts")
                .whereEqualTo("payerId", userId) // Check if payerId matches
                .whereEqualTo("billId", billId)  // Check if billId matches
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        // Iterate over the documents that match the query
                        for (DocumentSnapshot documentSnapshot : querySnapshot.getDocuments()) {
                            // Get the receipt document ID
                            String receiptId = documentSnapshot.getId();

                            // Update the status of the matching receipt document
                            firestore.collection("Receipts")
                                    .document(receiptId)
                                    .update("status", "Completed") // Update the status field to "Completed"
                                    .addOnSuccessListener(aVoid -> {
                                        // Successfully updated the status
                                        Log.d("ReceiptActivity", "Receipt status updated to 'Completed' for receipt ID: " + receiptId);
                                        Toast.makeText(ReceiptActivity.this, "Receipt payment status updated.", Toast.LENGTH_SHORT).show();

                                        // Optionally, you can also log or perform other actions like notifying the user, etc.
                                    })
                                    .addOnFailureListener(e -> {
                                        // Handle the failure (e.g., network issue, document not found)
                                        Log.e("ReceiptActivity", "Failed to update receipt status: " + e.getMessage());
                                        Toast.makeText(ReceiptActivity.this, "Failed to update receipt status.", Toast.LENGTH_SHORT).show();
                                    });
                        }
                    } else {
                        // No matching receipts found
                        Log.e("ReceiptActivity", "No matching receipt found for userId: " + userId + " and billId: " + receiptBillId);
                        Toast.makeText(ReceiptActivity.this, "No matching receipt found.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    // Handle any errors in the query
                    Log.e("ReceiptActivity", "Error fetching receipts: " + e.getMessage());
                    Toast.makeText(ReceiptActivity.this, "Error fetching receipts.", Toast.LENGTH_SHORT).show();
                });
    }
    private void checkReceiptsAndCreateReservation(String billId) {
        firestore.collection("Bills")
                .document(billId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Bill bill = documentSnapshot.toObject(Bill.class);
                        if (bill != null && bill.getReceiptIds() != null && !bill.getReceiptIds().isEmpty()) {
                            checkReceiptStatuses(bill.getReceiptIds(), billId);
                        } else {
                            Toast.makeText(this, "No receipts found for this bill.", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(this, "Bill not found.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to fetch bill: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e("checkReceipts", "Error fetching bill", e);
                });
    }

    private void checkReceiptStatuses(ArrayList<String> receiptIds, String billId) {
        firestore.collection("Receipts")
                .whereIn(FieldPath.documentId(), receiptIds)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        boolean allCompleted = true;

                        for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                            Receipt receipt = document.toObject(Receipt.class);
                            if (receipt != null && !"Completed".equalsIgnoreCase(receipt.getStatus())) {
                                allCompleted = false;
                                break;
                            }
                        }

                        if (allCompleted) {
                            createReservation(billId);
                        } else {
                            Toast.makeText(this, "All receipts are not completed yet.", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(this, "No matching receipts found.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to fetch receipts: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e("checkReceiptStatuses", "Error fetching receipts", e);
                });
    }

    private void createReservation(String billId) {
        HashMap<String, Object> reservation = new HashMap<>();
        reservation.put("propertyId", receiptPropertyId);
        reservation.put("billId", billId);
        reservation.put("fromDate", receiptFromDate);
        reservation.put("toDate", receiptToDate);
        reservation.put("guestCount", receiptNumberOfGuests);
        reservation.put("timestamp", System.currentTimeMillis());
        reservation.put("hostId", receiptHostId);
        reservation.put("status", "Confirmed");

        firestore.collection("reservations")
                .add(reservation)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(this, "Booking successful!", Toast.LENGTH_SHORT).show();
//                    Intent intent = new Intent(this, ReceiptActivity.class);
//                    startActivity(intent);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Booking failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e("createReservation", "Error booking property", e);
                });
    }
    @Override
    public void setupNavigationBar() {
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setSelectedItemId(R.id.nav_trips);
        NavigationUtils.handleBottomNavigation(this, bottomNavigationView);
    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {
        super.onPointerCaptureChanged(hasCapture);
    }
}
