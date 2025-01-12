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

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.chillpoint.R;
import com.example.chillpoint.managers.SessionManager;
import com.example.chillpoint.repositories.PropertyRepository;
import com.example.chillpoint.repositories.UserRepository;
import com.example.chillpoint.views.models.Property;
import com.example.chillpoint.views.models.Voucher;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public class PaymentActivity extends AppCompatActivity {
    private LinearLayout paymentLinearLayout, billSplitLinearLayout, bookingSummaryLinearLayout, paymentMethodLinearLayout, emailContainer;
    private RadioGroup splitBillRadioGroup,titleRadioGroup;
    private RadioButton yesRadioButton, noRadioButton;
    private EditText userNameEditText, emailEditText, phoneEditText, addressEditText, zipCodeEditText, cityEditText, voucherCodeEditText, friendEmailEditText;
    private Spinner countrySpinner;
    private Button nextButton, splitBillNextButton, bookingSummaryNextButton,addNewFriendButton;
    private boolean isBillSplit = false; // Tracks if the user chooses to split the bill
    private ArrayList<String> friendEmailIds = new ArrayList<>();
    private FirebaseFirestore firestore = FirebaseFirestore.getInstance(); // Firestore instance
    private UserRepository userRepository = new UserRepository(); // Repository for Firestore calls
    private PropertyRepository propertyRepository = new PropertyRepository();
    private String startDate;
    private String toDate;
    private String propertyId;
    private String numberOfGuests;
    private String totalPrice;
    private double totalAmount;
    private Spinner voucherSpinner;
    private ArrayList<Voucher> userVouchers = new ArrayList<>(); // 유저의 바우처 목록
    private ArrayList<String> voucherDescriptions = new ArrayList<>(); // Spinner에 표시될 바우처 설명
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment);
        SessionManager sessionManager = new SessionManager(this);
        friendEmailIds.add(sessionManager.getUserId());

        Intent intent = getIntent();
         startDate = (String) intent.getExtras().get("fromDate");
         toDate = (String) intent.getExtras().get("toDate");
         propertyId = (String) intent.getExtras().get("propertyId");
         numberOfGuests =(String) intent.getExtras().get("numberOfGuests");
        totalPrice = (String) intent.getExtras().get("totalPrice");

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
                        applyVoucherDiscount(selectedVoucher); // 바우처 할인 적용
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
        double totalAmount = Double.parseDouble(totalPrice) / friendEmailIds.size();
        Log.d("PaymentActivity", "Calculated totalAmount: " + totalAmount);
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
                double discountedTotalAmount = totalAmount - (totalAmount * discount); // 할인 적용
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
                ? userVouchers.get(selectedPosition - 1).getId() // 디폴트 옵션 제외
                : null;

        // Add the bill to Firestore with auto-generated ID
        firestore.collection("Bills")
                .add(billMap) // Automatically generates a document ID
                .addOnSuccessListener(documentReference -> {
                    // Get the generated billId
                    String billId = documentReference.getId();
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
                        receiptMap.put("voucherId", voucherId); // Selected voucher ID
                        receiptMap.put("numberOfPayers", friendEmailIds.size());
                        receiptMap.put("payerId", payerId);  // Payer's ID for this receipt
                        receiptMap.put("billId", billId);    // Associate the receipt with the billId

                        // Parse and add totalAmount
                        try {
                            double totalAmount = Double.parseDouble(totalPrice) / friendEmailIds.size();

                            // Apply discount if voucher is selected
                            if (voucherId != null && selectedPosition > 0) {
                                Voucher selectedVoucher = userVouchers.get(selectedPosition - 1); // 디폴트 옵션 제외
                                if (selectedVoucher != null) {
                                    // Check if the voucher is within the valid date range
                                    String currentDate = getCurrentDate(); // Get current date
                                    if (isValidVoucher(selectedVoucher, currentDate)) {
                                        // Apply the discount to the total amount
                                        double discount = selectedVoucher.getAmountOfDiscount();
                                        totalAmount -= totalAmount * discount; // 할인 적용
                                        receiptMap.put("totalAmount", totalAmount); // 할인 후 금액 저장
                                    } else {
                                        Log.e("Voucher", "Voucher is expired or invalid");
                                        receiptMap.put("totalAmount", totalAmount); // Save without discount
                                    }
                                }
                            } else {
                                receiptMap.put("totalAmount", totalAmount); // Save without discount
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
// Log after adding the email
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



    private void applyVoucherDiscount(Voucher selectedVoucher) {
        if (selectedVoucher != null) {
            String currentDate = getCurrentDate(); // Get the current date
            if (isValidVoucher(selectedVoucher, currentDate)) {
                double discount = selectedVoucher.getAmountOfDiscount(); // Get the discount percentage
                double discountedTotalAmount = totalAmount - (totalAmount * discount); // Apply the discount

                Log.d("applyVoucherDiscount", "Discount applied: " + discount + ", Final Price: " + discountedTotalAmount);

                // Update the UI with the discounted price
                TextView newTotalPriceTextView = findViewById(R.id.newTotalPriceTextView);
                newTotalPriceTextView.setText("Total after discount: $" + String.format("%.2f", discountedTotalAmount));
            } else {
                // If the voucher is expired or invalid
                Log.e("applyVoucherDiscount", "Invalid or expired voucher selected.");
                TextView newTotalPriceTextView = findViewById(R.id.newTotalPriceTextView);
                newTotalPriceTextView.setText("Voucher expired or invalid.");
            }
        } else {
            Log.e("applyVoucherDiscount", "Voucher is null.");
        }
    }


}
