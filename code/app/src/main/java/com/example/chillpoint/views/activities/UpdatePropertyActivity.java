package com.example.chillpoint.views.activities;

import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;

import com.example.chillpoint.R;
import com.example.chillpoint.managers.SessionManager;
import com.example.chillpoint.views.adapters.ImageAdapter;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class UpdatePropertyActivity extends AppCompatActivity {

    private static final int IMAGE_PICKER_REQUEST = 100;
    private static final int LOCATION_PICKER_REQUEST = 200;
    private static final String NOTIFICATION_CHANNEL_ID = "property_update_channel";

    private EditText nameEditText, descriptionEditText, addressEditText, priceEditText, roomsEditText, maxGuestsEditText;
    private Spinner checkInTimeSpinner, checkOutTimeSpinner;
    private Button uploadImagesButton, updatePropertyButton, pickLocationButton, addBedTypeButton;
    private GridView imagesGridView;
    private ProgressBar progressBar;

    private FirebaseFirestore firestore;
    private FirebaseStorage storage;

    private ArrayList<Uri> imageUris;
    private ArrayList<String> uploadedImageUrls;
    private ArrayList<String> removedImageUrls; // 삭제된 이미지를 임시 저장하는 리스트
    private ImageAdapter imageAdapter;

    private String selectedCheckInTime, selectedCheckOutTime;
    private SessionManager sessionManager;

    private HashMap<String, Integer> bedTypesMap = new HashMap<>();
    private ListView bedTypesListView;
    private BedTypeAdapter bedTypesListAdapter;
    private ArrayList<BedTypeItem> bedTypesList;

    private int totalBeds = 0;
    private String propertyId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_update_property);

        // UI 컴포넌트 초기화
        nameEditText = findViewById(R.id.nameEditText);
        descriptionEditText = findViewById(R.id.descriptionEditText);
        addressEditText = findViewById(R.id.addressEditText);
        priceEditText = findViewById(R.id.priceEditText);
        roomsEditText = findViewById(R.id.roomsEditText);
        maxGuestsEditText = findViewById(R.id.maxGuestsEditText);

        checkInTimeSpinner = findViewById(R.id.checkInTimeSpinner);
        checkOutTimeSpinner = findViewById(R.id.checkOutTimeSpinner);

        uploadImagesButton = findViewById(R.id.uploadImagesButton);
        updatePropertyButton = findViewById(R.id.savePropertyButton);
        pickLocationButton = findViewById(R.id.pickLocationButton);
        addBedTypeButton = findViewById(R.id.addBedTypeButton);

        imagesGridView = findViewById(R.id.imagesGridView);
        progressBar = findViewById(R.id.progressBar);

        bedTypesListView = findViewById(R.id.bedTypesListView);
        bedTypesList = new ArrayList<>();
        bedTypesListAdapter = new BedTypeAdapter();
        bedTypesListView.setAdapter(bedTypesListAdapter);

        // Firebase 서비스 초기화
        firestore = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        sessionManager = new SessionManager(this);

        // 이미지 리스트 초기화
        imageUris = new ArrayList<>();
        uploadedImageUrls = new ArrayList<>();
        removedImageUrls = new ArrayList<>(); // 삭제된 이미지 리스트 초기화

        // Intent에서 Property ID 가져오기
        propertyId = getIntent().getStringExtra("propertyId");
        loadPropertyData();

        // ImageAdapter 설정
        imageAdapter = new ImageAdapter(this, imageUris, uploadedImageUrls);
        imagesGridView.setAdapter(imageAdapter);

        imageAdapter.setOnImageRemoveListener(position -> {
            handleImageRemoval(position);
            Toast.makeText(this, "Image removed at position: " + position, Toast.LENGTH_SHORT).show();
        });
        // 리스너 설정
        uploadImagesButton.setOnClickListener(v -> openImagePicker());
        updatePropertyButton.setOnClickListener(v -> updateProperty());
        pickLocationButton.setOnClickListener(v -> openLocationPicker());
        addBedTypeButton.setOnClickListener(v -> openAddBedTypeDialog());

        setupCheckInTimeSpinner();
        setupCheckOutTimeSpinner();

        // 알림 채널 생성
        createNotificationChannel();
    }

    // Loads property data from Firestore
    private void loadPropertyData() {
        firestore.collection("Properties").document(propertyId).get()
                .addOnSuccessListener(documentSnapshot -> {

                    if (documentSnapshot.exists()) {
                        // Load property details
                        nameEditText.setText(documentSnapshot.getString("name"));
                        descriptionEditText.setText(documentSnapshot.getString("description"));
                        addressEditText.setText(documentSnapshot.getString("address"));
                        priceEditText.setText(String.valueOf(documentSnapshot.getDouble("pricePerNight")));
                        roomsEditText.setText(String.valueOf(documentSnapshot.getLong("numOfRooms")));
                        maxGuestsEditText.setText(String.valueOf(documentSnapshot.getLong("maxNumOfGuests")));

                        // Load bed types
                        Map<String, Long> bedTypes = (Map<String, Long>) documentSnapshot.get("bedTypes");
                        if (bedTypes != null) {
                            bedTypes.forEach((key, value) -> bedTypesMap.put(key, value.intValue()));
                            updateTotalBeds();
                            refreshBedTypesList();
                        }

                        // Load images
                        ArrayList<String> images = (ArrayList<String>) documentSnapshot.get("images");
                        if (images != null) {
                            uploadedImageUrls.clear(); // Clear existing list to avoid duplicates
                            uploadedImageUrls.addAll(images);
                        }
                        imageAdapter.notifyDataSetChanged(); // Notify adapter of data changes
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Error loading property data: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }



    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        startActivityForResult(Intent.createChooser(intent, "Select Images"), IMAGE_PICKER_REQUEST);
    }

    private void openLocationPicker() {
        Intent intent = new Intent(UpdatePropertyActivity.this, MapsActivity.class);
        startActivityForResult(intent, LOCATION_PICKER_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Handle image picker result
        if (requestCode == IMAGE_PICKER_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
            // [새로 추가됨] 새로 선택된 이미지를 처리
            if (data.getClipData() != null) {
                int count = data.getClipData().getItemCount();
                for (int i = 0; i < count; i++) {
                    Uri imageUri = data.getClipData().getItemAt(i).getUri();
                    imageUris.add(imageUri);
                }
            } else if (data.getData() != null) {
                imageUris.add(data.getData());
            }
            imageAdapter.notifyDataSetChanged();
        } else if (requestCode == LOCATION_PICKER_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
            double lat = data.getDoubleExtra("latitude", 0);
            double lng = data.getDoubleExtra("longitude", 0);
            String address = data.getStringExtra("address");
            addressEditText.setText(address);
        }
    }

    private void updateProperty() {
        String name = nameEditText.getText().toString().trim();
        String description = descriptionEditText.getText().toString().trim();
        String address = addressEditText.getText().toString().trim();
        String price = priceEditText.getText().toString().trim();
        String rooms = roomsEditText.getText().toString().trim();
        String maxGuests = maxGuestsEditText.getText().toString().trim();

        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(description) || TextUtils.isEmpty(address) ||
                TextUtils.isEmpty(price) || TextUtils.isEmpty(rooms) || TextUtils.isEmpty(maxGuests)) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);

        // Firebase Storage에서 삭제된 이미지 처리
        for (String removedImageUrl : removedImageUrls) {
            deleteImageFromFirebaseStorage(removedImageUrl);
        }
        removedImageUrls.clear(); // 삭제 목록 초기화

        uploadImages(new UploadImagesCallback() {
            @Override
            public void onUploadComplete(ArrayList<String> newImageUrls) {
                // 새로운 이미지 URL을 기존 이미지 목록에 추가
                uploadedImageUrls.addAll(newImageUrls);

                Map<String, Object> updatedData = new HashMap<>();
                updatedData.put("name", name);
                updatedData.put("description", description);
                updatedData.put("address", address);
                updatedData.put("pricePerNight", Double.parseDouble(price));
                updatedData.put("numOfRooms", Integer.parseInt(rooms));
                updatedData.put("maxNumOfGuests", Integer.parseInt(maxGuests));
                updatedData.put("checkInTime", selectedCheckInTime);
                updatedData.put("checkOutTime", selectedCheckOutTime);
                updatedData.put("bedTypes", bedTypesMap);
                updatedData.put("numOfBeds", totalBeds);
                updatedData.put("updatedAt", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date()));
                updatedData.put("images", uploadedImageUrls);

                firestore.collection("Properties").document(propertyId).update(updatedData)
                        .addOnSuccessListener(aVoid -> {
                            progressBar.setVisibility(View.GONE);
                            Toast.makeText(UpdatePropertyActivity.this, "Property updated successfully.", Toast.LENGTH_SHORT).show();

                            saveNotification("Property Updated", "Your property has been successfully updated.", name);
                            showNotification("Property Updated: " + name, "Your property has been successfully updated.");

                            // 작업 성공 시 RESULT_OK 전달
                            setResult(RESULT_OK);
                            finish();
                        })
                        .addOnFailureListener(e -> {
                            progressBar.setVisibility(View.GONE);
                            Toast.makeText(UpdatePropertyActivity.this, "Error updating property: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
            }

            @Override
            public void onUploadFailed(String errorMessage) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(UpdatePropertyActivity.this, "Failed to upload images: " + errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }





    private void uploadImages(UploadImagesCallback callback) {
        ArrayList<String> newImageUrls = new ArrayList<>();
        int uploadCount = 0;

        for (Uri uri : imageUris) {
            if (!uri.toString().startsWith("http")) { // Firebase에서 로드된 이미지는 다시 업로드하지 않음
                String fileName = "property_images/" + System.currentTimeMillis() + "_" + uri.getLastPathSegment();
                StorageReference reference = storage.getReference(fileName);
                int finalUploadCount = ++uploadCount; // 로컬 업로드 수 카운트
                reference.putFile(uri).addOnSuccessListener(taskSnapshot ->
                        reference.getDownloadUrl().addOnSuccessListener(url -> {
                            newImageUrls.add(url.toString());
                            if (newImageUrls.size() == finalUploadCount) {
                                callback.onUploadComplete(newImageUrls);
                            }
                        }).addOnFailureListener(e -> callback.onUploadFailed(e.getMessage()))
                ).addOnFailureListener(e -> callback.onUploadFailed(e.getMessage()));
            } else {
                uploadCount++;
                if (newImageUrls.size() == uploadCount) {
                    callback.onUploadComplete(newImageUrls);
                }
            }
        }

        if (imageUris.isEmpty() || uploadCount == 0) { // 이미지가 없을 경우 바로 콜백 처리
            callback.onUploadComplete(newImageUrls);
        }
    }


    interface UploadImagesCallback {
        void onUploadComplete(ArrayList<String> urls);

        void onUploadFailed(String errorMessage);
    }

    private void saveNotification(String title, String message, String relatedData) {
        String userId = sessionManager.getUserId();
        if (userId == null) {
            Toast.makeText(this, "User session invalid. Notification not saved.", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> notification = new HashMap<>();
        notification.put("userId", userId);
        notification.put("title", title);
        notification.put("message", message);
        notification.put("relatedData", relatedData);
        notification.put("timestamp", new Date());
        notification.put("isRead", false);

        firestore.collection("Notifications").add(notification)
                .addOnSuccessListener(documentReference -> Log.d("Notification", "Notification saved successfully"))
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to save notification: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }


    private void showNotification(String title, String message) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.notify((int) System.currentTimeMillis(), builder.build());
        }
    }


    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    NOTIFICATION_CHANNEL_ID,
                    "Property Update Notifications",
                    NotificationManager.IMPORTANCE_HIGH);

            channel.setDescription("Channel for property update notifications");

            // 소리 설정
            channel.setSound(
                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
                    new AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
            );

            // 진동 패턴 설정 (밀리초 단위: 대기, 진동, 대기, 진동...)
            channel.setVibrationPattern(new long[]{0, 500, 250, 500});
            channel.enableVibration(true);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void setupCheckInTimeSpinner() {
        String[] checkInTimes = {"12:00", "13:00", "14:00", "15:00", "16:00", "17:00"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, checkInTimes);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        checkInTimeSpinner.setAdapter(adapter);
        checkInTimeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedCheckInTime = checkInTimes[position];
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selectedCheckInTime = "14:00";
            }
        });
    }

    private void handleImageRemoval(int position) {
        if (position < uploadedImageUrls.size()) {
            // If the image is from Firebase
            String removedImageUrl = uploadedImageUrls.get(position);
            removedImageUrls.add(removedImageUrl); // Add to list of removed images
            uploadedImageUrls.remove(position); // Remove from displayed list
        } else {
            // If the image is locally added
            int localPosition = position - uploadedImageUrls.size();
            imageUris.remove(localPosition); // Remove from local list
        }

        imageAdapter.notifyDataSetChanged(); // Notify adapter of data changes
    }


    private void deleteImageFromFirebaseStorage(String imageUrl) {
        StorageReference storageReference = FirebaseStorage.getInstance().getReferenceFromUrl(imageUrl);
        storageReference.delete()
                .addOnSuccessListener(aVoid -> Log.d("FirebaseStorage", "Image deleted: " + imageUrl))
                .addOnFailureListener(e -> Log.e("FirebaseStorage", "Failed to delete image: " + e.getMessage()));
    }




    private void setupCheckOutTimeSpinner() {
        String[] checkOutTimes = {"06:00", "07:00", "08:00", "09:00", "10:00", "11:00"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, checkOutTimes);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        checkOutTimeSpinner.setAdapter(adapter);
        checkOutTimeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedCheckOutTime = checkOutTimes[position];
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selectedCheckOutTime = "10:00";
            }
        });
    }

    private void openAddBedTypeDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_bed_type, null);
        Spinner bedTypeSpinner = dialogView.findViewById(R.id.dialogBedTypeSpinner);
        EditText bedCountEditText = dialogView.findViewById(R.id.bedCountEditText);
        Button addButton = dialogView.findViewById(R.id.addButton);

        String[] bedTypesForDialog = {"King Size", "Queen Size", "Double", "Single", "Studio"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, bedTypesForDialog);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        bedTypeSpinner.setAdapter(adapter);

        android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(this)
                .setTitle("Add Bed Type")
                .setView(dialogView)
                .create();

        addButton.setOnClickListener(v -> {
            String selectedType = bedTypeSpinner.getSelectedItem().toString();
            String countStr = bedCountEditText.getText().toString();

            if (TextUtils.isEmpty(countStr)) {
                Toast.makeText(this, "Please enter bed count.", Toast.LENGTH_SHORT).show();
                return;
            }

            int count = Integer.parseInt(countStr);
            bedTypesMap.put(selectedType, bedTypesMap.getOrDefault(selectedType, 0) + count);
            updateTotalBeds();
            refreshBedTypesList();
            dialog.dismiss();
        });

        dialog.show();
    }

    private void updateTotalBeds() {
        totalBeds = 0;
        for (int count : bedTypesMap.values()) {
            totalBeds += count;
        }
    }

    private void refreshBedTypesList() {
        bedTypesList.clear();
        for (Map.Entry<String, Integer> entry : bedTypesMap.entrySet()) {
            bedTypesList.add(new BedTypeItem(entry.getKey(), entry.getValue()));
        }
        bedTypesListAdapter.notifyDataSetChanged();
    }

    // Custom adapter for Bed Types ListView
    private class BedTypeAdapter extends ArrayAdapter<BedTypeItem> {
        public BedTypeAdapter() {
            super(UpdatePropertyActivity.this, 0, bedTypesList);
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            BedTypeItem item = getItem(position);
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_bed_type, parent, false);
            }

            TextView bedTypeTextView = convertView.findViewById(R.id.bedTypeTextView);
            ImageButton deleteButton = convertView.findViewById(R.id.deleteButton);

            bedTypeTextView.setText(item.type + " x " + item.count);

            deleteButton.setOnClickListener(v -> {
                bedTypesMap.remove(item.type);
                updateTotalBeds();
                refreshBedTypesList();
            });

            return convertView;
        }
    }

    // Bed Type item class
    private static class BedTypeItem {
        String type;
        int count;

        BedTypeItem(String type, int count) {
            this.type = type;
            this.count = count;
        }
    }
}
