package com.example.chillpoint.views.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.bumptech.glide.Glide;
import com.example.chillpoint.R;
import com.example.chillpoint.managers.SessionManager;
import com.example.chillpoint.repositories.PropertyRepository;
import com.example.chillpoint.repositories.UserRepository;
import com.example.chillpoint.views.models.Property;
import com.example.chillpoint.views.models.Receipt;
import com.example.chillpoint.views.models.Voucher;
import com.example.chillpoint.views.models.Bill;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FirebaseFirestore;
import com.stripe.android.PaymentConfiguration;
import com.stripe.android.paymentsheet.PaymentSheet;
import com.stripe.android.paymentsheet.PaymentSheetResult;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.StringReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public class PaymentActivity extends AppCompatActivity {
    private LinearLayout paymentLinearLayout, billSplitLinearLayout, bookingSummaryLinearLayout, paymentMethodLinearLayout, emailContainer;
    private RadioGroup splitBillRadioGroup,titleRadioGroup;
    private RadioButton yesRadioButton, noRadioButton;
    private EditText userNameEditText, emailEditText, phoneEditText, addressEditText, zipCodeEditText, cityEditText, voucherCodeEditText, friendEmailEditText;
    private Spinner countrySpinner;
    private Button nextButton, splitBillNextButton, bookingSummaryNextButton,addNewFriendButton, processPaymentButton;
    private boolean isBillSplit = false; // Tracks if the user chooses to split the bill
    private ArrayList<String> friendEmailIds = new ArrayList<>();
    private FirebaseFirestore firestore = FirebaseFirestore.getInstance(); // Firestore instance
    private UserRepository userRepository = new UserRepository(); // Repository for Firestore calls
    private PropertyRepository propertyRepository = new PropertyRepository();
    private String startDate;
    private String toDate;
    private String propertyId;
    private String numberOfGuests;
    private String hostId;
    private String totalPrice;
    private double totalAmount;
    private String billId;
    private String PublishableKey = "pk_test_51QeHVRCEu9hglsZOJi2iypmyk0Pq3BEgh7HaSMf8GUdeXohp9diQnSLVZL511yw7ypAT1yx1xwN8pVnVxwkGI2cA00X1hScVXa";
    private String SecretKey = "sk_test_51QeHVRCEu9hglsZO3hULaZDemp0UQZZ3goIX1oxI4PjMyV7qEZ7WDVuHv5Nepbhs6RYG6uvvAPT7FJlxAYCeKqSR00JNalnVc6";
    private SessionManager sessionManager;
    private String EphemeralKey ;
    private String ClientSecret;
    private String CustomerId;
    private PaymentSheet paymentSheet;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment);
        sessionManager = new SessionManager(this);
        friendEmailIds.add(sessionManager.getUserId());
        // Initialize Stripe Payment configuration
        PaymentConfiguration.init(this, PublishableKey);

        // Initialize paymentSheet only when needed
        paymentSheet = new PaymentSheet(this, this::onPaymentResult);

        Intent intent = getIntent();
         startDate = (String) intent.getExtras().get("fromDate");
         toDate = (String) intent.getExtras().get("toDate");
         propertyId = (String) intent.getExtras().get("propertyId");
         numberOfGuests =(String) intent.getExtras().get("numberOfGuests");
        totalPrice = (String) intent.getExtras().get("totalPrice");
        hostId = (String) intent.getExtras().get("hostId");

// Log for debugging
        Log.d("PaymentActivity", "StartDate: " + startDate + ", ToDate: " + toDate + ", NumberOfGuests: " + numberOfGuests + ", TotalPrice: " + totalPrice);

        // Initialize UI elements
        paymentLinearLayout = findViewById(R.id.paymentLinearLayout);
        billSplitLinearLayout = findViewById(R.id.billSplitLinearLayout);
        bookingSummaryLinearLayout = findViewById(R.id.bookingSummaryLinearLayout);
        paymentMethodLinearLayout = findViewById(R.id.paymentMethodLinearLayout);

        splitBillRadioGroup = findViewById(R.id.splitBillRadioGroup);
        yesRadioButton = findViewById(R.id.yesRadioButton);
        noRadioButton = findViewById(R.id.noRadioButton);

        nextButton = findViewById(R.id.nextButton);
        splitBillNextButton = findViewById(R.id.billSplittingNextButton);
        bookingSummaryNextButton = findViewById(R.id.bookingSummaryNextButton);
        processPaymentButton = findViewById(R.id.processPaymentButton);

        // Set the initial visibility
        paymentLinearLayout.setVisibility(View.VISIBLE);
        billSplitLinearLayout.setVisibility(View.GONE);
        bookingSummaryLinearLayout.setVisibility(View.GONE);
        paymentMethodLinearLayout.setVisibility(View.GONE);
        // Initialize views in paymentLinearLayout
        titleRadioGroup = findViewById(R.id.titleRadioGroup);
        userNameEditText = findViewById(R.id.userNameEditText);
        emailEditText = findViewById(R.id.emailEditText);
        phoneEditText = findViewById(R.id.phoneEditText);
        addressEditText = findViewById(R.id.addressEditText);
        zipCodeEditText = findViewById(R.id.zipCodeEditText);
        cityEditText = findViewById(R.id.cityEditText);
        countrySpinner = findViewById(R.id.countrySpinner);
        // List of countries
        String[] countries = {"United States", "Canada", "United Kingdom", "Australia", "Germany", "France", "Vietnam"};

        // Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, countries);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        countrySpinner.setAdapter(adapter);

        // Handle selecting an item from the spinner
        countrySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                String selectedCountry = countrySpinner.getSelectedItem().toString();
                Log.d("PaymentActivity", "Selected country: " + selectedCountry);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
                // Do something if no item is selected
            }
        });
        voucherCodeEditText = findViewById(R.id.voucherCodeEditText);
        friendEmailEditText = findViewById(R.id.friendEmailEditText);
        addNewFriendButton = findViewById(R.id.addNewFriendButton);

        // Set up price strike-through for TextView
//        TextView priceTextView = findViewById(R.id.totalPriceTextView);
//        priceTextView.setPaintFlags(priceTextView.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);

        // RadioGroup listener to track user choice
        splitBillRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.yesRadioButton) {
                isBillSplit = true;
            } else if (checkedId == R.id.noRadioButton) {
                isBillSplit = false;
            }
        });

        // Handle "Next" button in paymentLinearLayout
        nextButton.setOnClickListener(v -> {
            if (areFieldsValid(paymentLinearLayout)) {
                if (isBillSplit) {
                    paymentLinearLayout.setVisibility(View.GONE);
                    billSplitLinearLayout.setVisibility(View.VISIBLE);
                } else {
                    paymentLinearLayout.setVisibility(View.GONE);
                    bookingSummaryLinearLayout.setVisibility(View.VISIBLE);
                    propertyRepository.getPropertyById(propertyId)
                            .addOnSuccessListener(property -> {
                                if (property != null) {
                                    updateBookingSummary(property, startDate, toDate, numberOfGuests, totalPrice);
                                } else {
                                    Toast.makeText(this, "Property details not available.", Toast.LENGTH_SHORT).show();
                                }
                            })
                            .addOnFailureListener(e -> {
                                Log.e("PaymentActivity", "Error fetching property details", e);
                                Toast.makeText(this, "Failed to load property details.", Toast.LENGTH_SHORT).show();
                            });
                }
            }
        });

        // Handle "Next" button in billSplitLinearLayout
        splitBillNextButton.setOnClickListener(v -> {
            if (areFieldsValid(billSplitLinearLayout)) {
                billSplitLinearLayout.setVisibility(View.GONE);
                bookingSummaryLinearLayout.setVisibility(View.VISIBLE);
                propertyRepository.getPropertyById(propertyId)
                        .addOnSuccessListener(property -> {
                            if (property != null) {
                                updateBookingSummary(property, startDate, toDate, numberOfGuests, totalPrice);
                            } else {
                                Toast.makeText(this, "Property details not available.", Toast.LENGTH_SHORT).show();
                            }
                        })
                        .addOnFailureListener(e -> {
                            Log.e("PaymentActivity", "Error fetching property details", e);
                            Toast.makeText(this, "Failed to load property details.", Toast.LENGTH_SHORT).show();
                        });
            }
        });


        // Handle "Next" button in bookingSummaryLinearLayout
        bookingSummaryNextButton.setOnClickListener(v -> {
            if (areFieldsValid(bookingSummaryLinearLayout)) {
                saveBillAndReceiptToFirestore();
                bookingSummaryLinearLayout.setVisibility(View.GONE);
                paymentMethodLinearLayout.setVisibility(View.VISIBLE);
                onPaymentAction();
            }
        });
        processPaymentButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                paymentFlow();
            }
        });
        // Initialize views
        emailContainer = findViewById(R.id.emailContainer);
        addNewFriendButton = findViewById(R.id.addNewFriendButton);

        // Handle "Add New Friend" button click
        addNewFriendButton.setOnClickListener(v -> {
            if (!friendEmailEditText.getText().toString().trim().isEmpty()) {
                verifyFriendEmail(friendEmailEditText.getText().toString().trim());
            } else {
                Toast.makeText(this, "Please enter a valid email.", Toast.LENGTH_SHORT).show();
            }
        });

    }
    private void updateBookingSummary(Property property, String startDate, String toDate, String numberOfGuests, String totalPrice) {
        // Initialize views
        TextView propertyNameTextView = findViewById(R.id.propertyDetailNameTextView);
        TextView propertyLocationTextView = findViewById(R.id.propertyDetailLocationTextView);
        TextView bookingDateTextView = findViewById(R.id.bookingDateTextView);
        TextView guestsTextView = findViewById(R.id.guestsTextView);
        TextView totalPriceTextView = findViewById(R.id.totalPriceTextView);
        TextView totalPriceTextViewNeedToPay = findViewById(R.id.totalPriceTextViewNeedToPay);
        TextView newTotalPriceTextView = findViewById(R.id.newTotalPriceTextView);  // New TextView for the discounted price

        ImageView propertyImageView = findViewById(R.id.bookingPropertyImageView);
        EditText voucherCodeEditText = findViewById(R.id.voucherCodeEditText);  // Voucher code EditText

        if (propertyNameTextView == null || propertyLocationTextView == null ||
                bookingDateTextView == null || guestsTextView == null ||
                totalPriceTextView == null || totalPriceTextViewNeedToPay == null ||
                propertyImageView == null || newTotalPriceTextView == null) {
            Log.e("PaymentActivity", "One or more TextViews/ImageView are null. Check layout XML IDs.");
            return;
        }

        // Populate property information
        propertyNameTextView.setText(property.getName());
        propertyLocationTextView.setText(property.getAddress());
        bookingDateTextView.setText(String.format("%s - %s", startDate, toDate));
        guestsTextView.setText(numberOfGuests);
        totalPriceTextView.setText(totalPrice);

        // Calculate the total amount (divide by the number of friends)
        totalAmount = Double.parseDouble(totalPrice) / friendEmailIds.size();
        String totalAmountTemp = String.format("%.2f", totalAmount);
        totalPriceTextViewNeedToPay.setText(totalAmountTemp);

        // Check if voucher code is entered and apply a discount
        String voucherCode = voucherCodeEditText.getText().toString().trim();
        Log.e("voucherCode", "Voucher Code: " + voucherCode);
        AtomicReference<Double> discountedTotalAmount = new AtomicReference<>(totalAmount);  // Default to original amount
        Log.e("discountedTotalAmount", "discountedTotalAmount: " + discountedTotalAmount);
        if (!voucherCode.isEmpty()) {
            // Fetch voucher details from Firestore
            firestore.collection("Vouchers")
                    .whereEqualTo(FieldPath.documentId(), voucherCode)
                    .whereEqualTo("status", true)  // Ensure voucher is active
                    .get()
                    .addOnSuccessListener(querySnapshot -> {
                        if (!querySnapshot.isEmpty()) {
                            // Voucher found, apply discount
                            DocumentSnapshot documentSnapshot = querySnapshot.getDocuments().get(0);
                            Voucher voucher = documentSnapshot.toObject(Voucher.class);

                            if (voucher != null) {
                                // Check if the voucher is within the valid date range
                                String currentDate = getCurrentDate(); // Implement this method to get the current date in the same format
                                if (isValidVoucher(voucher, currentDate)) {
                                    // Apply discount
                                    discountedTotalAmount.set(totalAmount - (totalAmount * (voucher.getAmountOfDiscount() / 100)));

                                    // Assuming discountedTotalAmount is an AtomicReference<Double>
                                    double discountedPriceValue = discountedTotalAmount.get(); // Extract the value

                                    // Now use the value to format it
                                    String discountedPrice = String.format("%.2f", discountedPriceValue);

                                    // Update the TextView
                                    newTotalPriceTextView.setText("Total after discount: " + discountedPrice);
                                } else {
                                    newTotalPriceTextView.setText("Voucher expired or invalid.");
                                }
                            }
                        } else {
                            // Voucher not found or inactive
                            newTotalPriceTextView.setText("Invalid or inactive voucher.");
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e("PaymentActivity", "Error fetching voucher: " + e.getMessage());
                        newTotalPriceTextView.setText("Error applying voucher.");
                    });
        } else {
            // If voucher code is empty, keep the original price
            newTotalPriceTextView.setText("Total: " + totalAmountTemp);
        }

        // Load property image
        Glide.with(this)
                .load(property.getImages().get(0))
                .placeholder(R.drawable.ic_launcher_background)
                .into(propertyImageView);
    }

    // Helper method to get the current date in the same format as the voucher's date
    private String getCurrentDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        return sdf.format(new Date());
    }

    // Helper method to check if the voucher is valid based on the current date
    private boolean isValidVoucher(Voucher voucher, String currentDate) {
        return currentDate.compareTo(voucher.getStartDate()) >= 0 && currentDate.compareTo(voucher.getEndDate()) <= 0;
    }

    private void saveBillAndReceiptToFirestore() {
        // Create a map to store the bill data
        Map<String, Object> billMap = new HashMap<>();
        ArrayList<String> receiptIds = new ArrayList<>(); // List to store receipt IDs

        // Add the bill to Firestore with auto-generated ID
        firestore.collection("Bills")
                .add(billMap) // Automatically generates a document ID
                .addOnSuccessListener(documentReference -> {
                    // Get the generated billId
                    billId = documentReference.getId();
                    Toast.makeText(this, "Bill saved successfully with ID: " + billId, Toast.LENGTH_SHORT).show();

                    // Iterate over each friend's email and create a receipt for each
                    for (String payerId : friendEmailIds) {
                        // Create a map to store receipt data
                        Map<String, Object> receiptMap = new HashMap<>();
                        receiptMap.put("propertyId", propertyId);
                        receiptMap.put("prefixTitle", getSelectedTitle());
                        receiptMap.put("username", userNameEditText.getText().toString());
                        receiptMap.put("email", emailEditText.getText().toString());
                        receiptMap.put("phoneNumber", phoneEditText.getText().toString());
                        receiptMap.put("address", addressEditText.getText().toString());
                        receiptMap.put("zipCode", zipCodeEditText.getText().toString());
                        receiptMap.put("cityName", cityEditText.getText().toString());
                        receiptMap.put("country", countrySpinner.getSelectedItem().toString());
                        receiptMap.put("voucherId", voucherCodeEditText.getText().toString());
                        receiptMap.put("numberOfPayers", friendEmailIds.size());
                        receiptMap.put("payerId", payerId);
                        receiptMap.put("billId", billId);
                        receiptMap.put("fromDate", startDate);
                        receiptMap.put("toDate", toDate);
                        receiptMap.put("numberOfGuests", numberOfGuests);
                        receiptMap.put("hostId", hostId);

                        // Initialize totalAmount and calculate
                        try {
                            totalAmount = Double.parseDouble(totalPrice) / friendEmailIds.size();

                            // Check if voucher code is provided
                            String voucherCode = voucherCodeEditText.getText().toString().trim();

                            // Apply discount if voucher code is provided
                            if (!voucherCode.isEmpty()) {
                                // Fetch voucher details from Firestore
                                firestore.collection("Vouchers")
                                        .whereEqualTo(FieldPath.documentId(), voucherCode)
                                        .whereEqualTo("status", true)  // Ensure voucher is active
                                        .get()
                                        .addOnSuccessListener(querySnapshot -> {
                                            if (!querySnapshot.isEmpty()) {
                                                // Voucher found, apply discount
                                                DocumentSnapshot documentSnapshot = querySnapshot.getDocuments().get(0);
                                                Voucher voucher = documentSnapshot.toObject(Voucher.class);

                                                if (voucher != null) {
                                                    // Check if the voucher is within the valid date range
                                                    String currentDate = getCurrentDate(); // Implement this method to get the current date in the same format
                                                    if (isValidVoucher(voucher, currentDate)) {
                                                        // Apply the discount to the total amount
                                                        double discount = voucher.getAmountOfDiscount();
                                                        totalAmount = totalAmount - (totalAmount * (discount / 100)); // Apply percentage discount
                                                        receiptMap.put("totalAmount", totalAmount);
                                                    } else {
                                                        // Voucher is expired or invalid
                                                        Log.e("Voucher", "Voucher is expired or invalid");
                                                    }
                                                }
                                            } else {
                                                // Voucher not found or inactive
                                                Log.e("Voucher", "Voucher is invalid or inactive");
                                            }
                                        })
                                        .addOnFailureListener(e -> {
                                            Log.e("Voucher Error", "Failed to fetch voucher: " + e.getMessage());
                                        });
                            } else {
                                receiptMap.put("totalAmount", totalAmount);
                            }

                        } catch (NumberFormatException e) {
                            Log.e("saveReceipt", "Invalid totalPrice: " + totalPrice, e);
                            Toast.makeText(this, "Invalid total price format", Toast.LENGTH_SHORT).show();
                            return; // Prevent further execution
                        }

                        receiptMap.put("status", "Pending");

                        // Add receipt to Firestore with auto-generated ID
                        firestore.collection("Receipts")
                                .add(receiptMap) // Automatically generates a document ID
                                .addOnSuccessListener(receiptDocRef -> {
                                    // Collect the receiptId and add it to the list
                                    receiptIds.add(receiptDocRef.getId()); // Store the receipt ID

                                    Toast.makeText(this, "Receipt saved successfully with ID: " + receiptDocRef.getId(), Toast.LENGTH_SHORT).show();

                                    // After saving all receipts, update the Bill with receiptIds
                                    if (receiptIds.size() == friendEmailIds.size()) {
                                        // Update the bill document with the list of receipt IDs
                                        Map<String, Object> billUpdateMap = new HashMap<>();
                                        billUpdateMap.put("receiptIds", receiptIds);

                                        firestore.collection("Bills")
                                                .document(billId)
                                                .update(billUpdateMap) // Update the existing bill with the receipt IDs
                                                .addOnSuccessListener(aVoid -> {
                                                    Toast.makeText(this, "Bill updated with receipt IDs.", Toast.LENGTH_SHORT).show();
                                                })
                                                .addOnFailureListener(e1 -> {
                                                    Toast.makeText(this, "Failed to update bill: " + e1.getMessage(), Toast.LENGTH_SHORT).show();
                                                });
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(this, "Failed to save receipt: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to save bill: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private String getSelectedTitle() {
        int selectedId = titleRadioGroup.getCheckedRadioButtonId();
        if (selectedId == R.id.mrRadioButton) {
            return "Mr.";
        } else if (selectedId == R.id.mrsRadioButton) {
            return "Mrs.";
        } else {
            return "";
        }
    }
    private void verifyFriendEmail(String email) {
        userRepository.checkUserExistByEmail(email)
                .addOnSuccessListener(userId -> {
                    if (userId != null) {
                        if (!friendEmailIds.contains(email)) {
                            friendEmailIds.add(userId);
                            Log.e("verifyFriendEmail","userId: "+ userId);
                            Log.e("verifyFriendEmail Size","Size: "+ friendEmailIds.size());
                            // Add new EditText auto when clicks button add
                            EditText newEmailEditText = new EditText(this);
                            int paddingInPixels = (int) (12 * getResources().getDisplayMetrics().density + 0.5f);
                            newEmailEditText.setLayoutParams(new LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.MATCH_PARENT,
                                    LinearLayout.LayoutParams.WRAP_CONTENT
                            ));
                            newEmailEditText.setHint("Friend's email");
                            newEmailEditText.setText(email); // Prepopulate with the validated email
                            newEmailEditText.setPadding(paddingInPixels, paddingInPixels, paddingInPixels, paddingInPixels);
                            newEmailEditText.setTextSize(14);
                            emailContainer.addView(newEmailEditText);
                            Log.e("verifyFriendEmail Size", "Size after adding email: " + friendEmailIds.size());
                            Toast.makeText(this, "Friend email added: " + email, Toast.LENGTH_SHORT).show();
                            friendEmailEditText.setText(""); // Clear input
                        } else {
                            Toast.makeText(this, "Email is already added.", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(this, "This email is not registered in the system.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to verify email: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private boolean areFieldsValid(LinearLayout layout) {
        // Iterate through all child views of the provided layout
        for (int i = 0; i < layout.getChildCount(); i++) {
            View view = layout.getChildAt(i);
            if (view instanceof EditText) {
                EditText editText = (EditText) view;
                if (editText.getText().toString().trim().isEmpty()) {
                    Toast.makeText(this, "Please fill out all fields.", Toast.LENGTH_SHORT).show();
                    return false;
                }
            }
        }
        return true;
    }

    private void onPaymentAction(){
        StringRequest request = new StringRequest(Request.Method.POST, "https://api.stripe.com/v1/customers", new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                try {
                    JSONObject object = new JSONObject(response);
                    CustomerId = object.getString("id");
                    Toast.makeText(PaymentActivity.this,"Customer ID: " + CustomerId, Toast.LENGTH_SHORT).show();
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

                Toast.makeText(PaymentActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
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
                    Toast.makeText(PaymentActivity.this,"EphemeralKey id: " + EphemeralKey, Toast.LENGTH_SHORT).show();
                    getClientSecret(CustomerId,EphemeralKey);
                }catch (JSONException e){
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(PaymentActivity.this, error.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
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
                    Toast.makeText(PaymentActivity.this,"ClientSecret: " + ClientSecret, Toast.LENGTH_SHORT).show();

                }catch (JSONException e){
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(PaymentActivity.this, error.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
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
                int totalAmountInt = (int) Math.ceil(totalAmount);
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

    private void onPaymentResult(PaymentSheetResult paymentSheetResult) {
        if (paymentSheetResult instanceof PaymentSheetResult.Completed) {
            Toast.makeText(this, "Payment completed", Toast.LENGTH_SHORT).show();
            updateStatusReceipt(sessionManager.getUserId(), billId);
            // Check if all receipts are completed before creating the reservation
            checkReceiptsAndCreateReservation(billId);
        } else if (paymentSheetResult instanceof PaymentSheetResult.Canceled) {
            Toast.makeText(this, "Payment canceled", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(this, ReceiptActivity.class);
            startActivity(intent);
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
                                        Log.d("PaymentActivity", "Receipt status updated to 'Completed' for receipt ID: " + receiptId);
                                        Toast.makeText(PaymentActivity.this, "Receipt payment status updated.", Toast.LENGTH_SHORT).show();

                                        // Optionally, you can also log or perform other actions like notifying the user, etc.
                                    })
                                    .addOnFailureListener(e -> {
                                        // Handle the failure (e.g., network issue, document not found)
                                        Log.e("PaymentActivity", "Failed to update receipt status: " + e.getMessage());
                                        Toast.makeText(PaymentActivity.this, "Failed to update receipt status.", Toast.LENGTH_SHORT).show();
                                    });
                        }
                    } else {
                        // No matching receipts found
                        Log.e("PaymentActivity", "No matching receipt found for userId: " + userId + " and billId: " + billId);
                        Toast.makeText(PaymentActivity.this, "No matching receipt found.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    // Handle any errors in the query
                    Log.e("PaymentActivity", "Error fetching receipts: " + e.getMessage());
                    Toast.makeText(PaymentActivity.this, "Error fetching receipts.", Toast.LENGTH_SHORT).show();
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
        reservation.put("propertyId", propertyId);
        reservation.put("billId", billId);
        reservation.put("fromDate", startDate);
        reservation.put("toDate", toDate);
        reservation.put("guestCount", numberOfGuests);
        reservation.put("timestamp", System.currentTimeMillis());
        reservation.put("hostId", hostId);
        reservation.put("status", "Confirmed");

        firestore.collection("reservations")
                .add(reservation)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(this, "Booking successful!", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(this, ReceiptActivity.class);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Booking failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e("createReservation", "Error booking property", e);
                });
    }

}
