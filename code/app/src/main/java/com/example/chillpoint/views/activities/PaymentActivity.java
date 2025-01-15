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
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
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
    private Spinner voucherSpinner;
    private ArrayList<Voucher> userVouchers = new ArrayList<>(); // 유저의 바우처 목록
    private ArrayList<String> voucherDescriptions = new ArrayList<>(); // Spinner에 표시될 바우처 설명
    private double discountedTotalAmount;
    private double finalCalculatedTotalAmount;
    private String voucherCode;
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

        loadUserVouchers();

        voucherSpinner = findViewById(R.id.voucherSpinner);
        voucherSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Log.d("PaymentActivity", "Selected position: " + position);

                // 디폴트 옵션 확인
                if (position == 0) {
                    Log.d("PaymentActivity", "Default option selected.");
                    return; // 디폴트 옵션 선택 시 아무 동작도 하지 않음
                }

                // 실제 데이터 인덱스 매핑
                int actualPosition = position - 1; // 디폴트 항목을 제외한 인덱스
                if (actualPosition >= 0 && actualPosition < userVouchers.size()) {
                    Voucher selectedVoucher = userVouchers.get(actualPosition);
                    if (selectedVoucher != null) {
                        Log.d("PaymentActivity", "Selected voucher: " + selectedVoucher);
//                        applyVoucherDiscount(selectedVoucher); // 바우처 할인 적용
                    } else {
                        Log.e("PaymentActivity", "Selected voucher is null.");
                    }
                } else {
                    Log.e("PaymentActivity", "Invalid voucher position: " + actualPosition);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                Log.d("PaymentActivity", "No voucher selected.");
            }
        });



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
        TextView newTotalPriceTextView = findViewById(R.id.newTotalPriceTextView); // New TextView for the discounted price
        ImageView propertyImageView = findViewById(R.id.bookingPropertyImageView);

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

// 바우처 선택 적용
        int selectedPosition = voucherSpinner.getSelectedItemPosition();
        Log.d("PaymentActivity", "Selected voucher position: " + selectedPosition);

// 디폴트 옵션 확인
        if (selectedPosition == 0) { // 기본 옵션인 "Select a voucher"가 선택된 경우
            Log.d("PaymentActivity", "No voucher selected. Default total amount applied.");
            newTotalPriceTextView.setText("Total: " + totalAmountTemp);
            return; // 아무 동작도 하지 않음
        }

// 실제 데이터 인덱스 매핑
        int actualPosition = selectedPosition - 1; // 기본 옵션을 제외한 실제 위치
        if (actualPosition >= 0 && actualPosition < userVouchers.size()) {
            Voucher selectedVoucher = userVouchers.get(actualPosition);
            Log.d("PaymentActivity", "Selected voucher details: " + selectedVoucher);

            // 바우처 할인 계산
            String currentDate = getCurrentDate(); // 현재 날짜 가져오기
            Log.d("PaymentActivity", "Current date: " + currentDate);
            if (selectedVoucher != null && isValidVoucher(selectedVoucher, currentDate)) {
                double discount = selectedVoucher.getAmountOfDiscount(); // 바우처의 할인율
                Log.d("PaymentActivity", "Voucher discount amount: " + discount);
                discountedTotalAmount = totalAmount - (totalAmount * discount); // 할인 적용
                Log.d("PaymentActivity", "Discounted totalAmount: " + discountedTotalAmount);
                String discountedPrice = String.format("%.2f", discountedTotalAmount);

                // 할인 적용된 가격 업데이트
                newTotalPriceTextView.setText("Total after discount: " + discountedPrice);
            } else {
                // 바우처가 만료되었거나 유효하지 않을 경우
                Log.d("PaymentActivity", "Voucher expired or invalid.");
                newTotalPriceTextView.setText("Voucher expired or invalid.");
            }
        } else {
            // 잘못된 인덱스
            Log.e("PaymentActivity", "Invalid voucher position: " + actualPosition);
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

        // Get selected voucher from Spinner
        int selectedPosition = voucherSpinner.getSelectedItemPosition();
        String voucherId = (selectedPosition > 0 && selectedPosition - 1 < userVouchers.size())
                ? userVouchers.get(selectedPosition - 1).getId() // Default option excluded
                : null;

        double calculatedTotalAmount = totalAmount; // Base total amount
        if (voucherId != null) {
            // Apply discount synchronously based on selected voucher
            Voucher selectedVoucher = userVouchers.get(selectedPosition - 1);
            if (selectedVoucher != null && isValidVoucher(selectedVoucher, getCurrentDate())) {
                double discount = selectedVoucher.getAmountOfDiscount();
                calculatedTotalAmount -= (calculatedTotalAmount * discount);
            }
        }

        // Update bill map with the totalAmount (discounted if applicable)
        billMap.put("totalAmount", calculatedTotalAmount);
        billMap.put("voucherId", voucherId);

        // Add the bill to Firestore
        finalCalculatedTotalAmount = calculatedTotalAmount;
        firestore.collection("Bills")
                .add(billMap) // Automatically generates a document ID
                .addOnSuccessListener(documentReference -> {
                    billId = documentReference.getId(); // Save bill ID for reference
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
                        receiptMap.put("voucherId", voucherId);
                        receiptMap.put("numberOfPayers", friendEmailIds.size());
                        receiptMap.put("payerId", payerId);
                        receiptMap.put("billId", billId);
                        receiptMap.put("fromDate", startDate);
                        receiptMap.put("toDate", toDate);
                        receiptMap.put("numberOfGuests", numberOfGuests);
                        receiptMap.put("hostId", hostId);
                        receiptMap.put("totalAmount", finalCalculatedTotalAmount);
                        receiptMap.put("status", "Pending");

                        // Add receipt to Firestore
                        firestore.collection("Receipts")
                                .add(receiptMap)
                                .addOnSuccessListener(receiptDocRef -> {
                                    receiptIds.add(receiptDocRef.getId()); // Collect receipt ID
                                    Toast.makeText(this, "Receipt saved successfully: " + receiptDocRef.getId(), Toast.LENGTH_SHORT).show();

                                    // After saving all receipts, update the bill
                                    if (receiptIds.size() == friendEmailIds.size()) {
                                        Map<String, Object> billUpdateMap = new HashMap<>();
                                        billUpdateMap.put("receiptIds", receiptIds);

                                        firestore.collection("Bills")
                                                .document(billId)
                                                .update(billUpdateMap)
                                                .addOnSuccessListener(aVoid -> {
                                                    Toast.makeText(this, "Bill updated with receipt IDs.", Toast.LENGTH_SHORT).show();
                                                })
                                                .addOnFailureListener(e -> {
                                                    Log.e("saveBillAndReceipt", "Failed to update bill: " + e.getMessage());
                                                });
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    Log.e("saveBillAndReceipt", "Failed to save receipt: " + e.getMessage());
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
                int finalCalculatedTotalAmountInt = (int) Math.ceil(finalCalculatedTotalAmount);
                String finalCalculatedTotalAmountStr = String.valueOf(finalCalculatedTotalAmountInt);
                params.put("amount",finalCalculatedTotalAmountStr + "00");
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


    private void loadUserVouchers() {
        Log.d("PaymentActivity", "Starting to load vouchers...");

        SessionManager sessionManager = new SessionManager(this);
        String userId = sessionManager.getUserId();
        Log.d("PaymentActivity", "Fetched User ID from SessionManager: " + userId);

        firestore.collection("Users")
                .document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    Log.d("PaymentActivity", "User document fetched: " + documentSnapshot.getId());

                    if (documentSnapshot.exists() && documentSnapshot.contains("voucherIds")) { // 필드 이름 수정
                        List<String> voucherIds = (List<String>) documentSnapshot.get("voucherIds"); // 필드 이름 수정
                        Log.d("PaymentActivity", "Fetched voucher IDs: " + voucherIds);

                        if (voucherIds != null && !voucherIds.isEmpty()) {
                            Log.d("PaymentActivity", "Voucher IDs are valid. Fetching vouchers from Firestore...");

                            firestore.collection("Vouchers")
                                    .whereIn(FieldPath.documentId(), voucherIds)
                                    .get()
                                    .addOnSuccessListener(queryDocumentSnapshots -> {
                                        Log.d("PaymentActivity", "Vouchers fetched successfully: " + queryDocumentSnapshots.size() + " vouchers.");

                                        voucherDescriptions.clear();
                                        voucherDescriptions.add("Select a voucher"); // Default option
                                        userVouchers.clear();

                                        for (DocumentSnapshot doc : queryDocumentSnapshots) {
                                            try {
                                                Voucher voucher = doc.toObject(Voucher.class);
                                                if (voucher != null) {
                                                    voucher.setId(doc.getId());
                                                    userVouchers.add(voucher);
                                                    voucherDescriptions.add(String.format("%.0f%% OFF - %s",
                                                            voucher.getAmountOfDiscount() * 100, voucher.getContent()));
                                                    Log.d("PaymentActivity", "Voucher added: " + voucher.getContent());
                                                }
                                            } catch (Exception e) {
                                                Log.e("PaymentActivity", "Failed to parse voucher document: " + doc.getId(), e);
                                            }
                                        }

                                        // 어댑터 설정
                                        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                                                this, android.R.layout.simple_spinner_item, voucherDescriptions);
                                        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                                        voucherSpinner.setAdapter(adapter);

                                        Log.d("PaymentActivity", "Voucher Spinner updated. Descriptions: " + voucherDescriptions);
                                    })
                                    .addOnFailureListener(e -> Log.e("PaymentActivity", "Failed to load vouchers: " + e.getMessage()));

                        } else {
                            Log.e("PaymentActivity", "Voucher IDs are null or empty for this user.");
                            showNoVouchersMessage();
                        }
                    } else {
                        Log.e("PaymentActivity", "No 'voucherIds' field found in user document.");
                        showNoVouchersMessage();
                    }
                })
                .addOnFailureListener(e -> Log.e("PaymentActivity", "Failed to fetch user document: " + e.getMessage()));
    }

    private void showNoVouchersMessage() {
        Toast.makeText(this, "No vouchers available for this user.", Toast.LENGTH_SHORT).show();
        voucherDescriptions.clear();
        voucherDescriptions.add("No vouchers available");
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, voucherDescriptions);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        voucherSpinner.setAdapter(adapter);
        Log.d("PaymentActivity", "Voucher Spinner updated with no vouchers.");
    }



//    private void applyVoucherDiscount(Voucher selectedVoucher) {
//        if (selectedVoucher != null) {
//            String currentDate = getCurrentDate(); // Get the current date
//            if (isValidVoucher(selectedVoucher, currentDate)) {
//                double discount = selectedVoucher.getAmountOfDiscount(); // Get the discount percentage
//                double discountedTotalAmount = totalAmount - (totalAmount * discount); // Apply the discount
//
//                Log.d("applyVoucherDiscount", "Discount applied: " + discount + ", Final Price: " + discountedTotalAmount);
//
//                // Update the UI with the discounted price
//                TextView newTotalPriceTextView = findViewById(R.id.newTotalPriceTextView);
//                newTotalPriceTextView.setText("Total after discount: $" + String.format("%.2f", discountedTotalAmount));
//            } else {
//                // If the voucher is expired or invalid
//                Log.e("applyVoucherDiscount", "Invalid or expired voucher selected.");
//                TextView newTotalPriceTextView = findViewById(R.id.newTotalPriceTextView);
//                newTotalPriceTextView.setText("Voucher expired or invalid.");
//            }
//        } else {
//            Log.e("applyVoucherDiscount", "Voucher is null.");
//        }
//    }


}
