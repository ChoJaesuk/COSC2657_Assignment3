package com.example.chillpoint.managers;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;

public class SessionManager {
    private static final String PREF_NAME = "ChillPointSession";
    private static final String KEY_USER_ID = "userId";
    private static final String KEY_ROLE = "role";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_USER_IMAGE = "userImageUrl";

    // 새로운 필드 추가
    private static final String KEY_EMAIL = "email";
    private static final String KEY_PHONE = "phone";
    private static final String KEY_BIO = "bio";

    // **FCM 관련 필드**
    private static final String KEY_FCM_TOKEN = "fcmToken";

    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;

    public SessionManager(Context context) {
        sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();
    }

    // 기존 메서드: 세션 저장
    public void saveUserSession(String userId, String role, String username, String imageUrl) {
        editor.putString(KEY_USER_ID, userId);
        editor.putString(KEY_ROLE, role);
        editor.putString(KEY_USERNAME, username);
        editor.putString(KEY_USER_IMAGE, imageUrl);
        editor.apply();
    }

    public void saveUserToken(String userId) {
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        String fcmToken = task.getResult();
                        FirebaseFirestore.getInstance().collection("FCMUsers")
                                .document(userId)
                                .update("fcmToken", fcmToken)
                                .addOnSuccessListener(aVoid -> Log.d("FCM", "Token saved successfully."))
                                .addOnFailureListener(e -> Log.e("FCM", "Failed to save token: " + e.getMessage()));

                        // FCM 토큰을 SharedPreferences에 저장
                        saveFcmToken(fcmToken);
                    } else {
                        Log.e("FCM", "Failed to fetch FCM token", task.getException());
                    }
                });
    }

    // **새로운 메서드**: FCM 토큰 저장
    public void saveFcmToken(String token) {
        editor.putString(KEY_FCM_TOKEN, token);
        editor.apply();
    }

    // **새로운 메서드**: 저장된 FCM 토큰 가져오기
    public String getFcmToken() {
        return sharedPreferences.getString(KEY_FCM_TOKEN, null);
    }

    // 새로운 메서드: 이메일, 전화번호, 바이오 추가 저장
    public void saveAdditionalUserInfo(String email, String phone, String bio) {
        editor.putString(KEY_EMAIL, email);
        editor.putString(KEY_PHONE, phone);
        editor.putString(KEY_BIO, bio);
        editor.apply();
    }

    // 기존 메서드: 유저 ID 가져오기
    public String getUserId() {
        return sharedPreferences.getString(KEY_USER_ID, null);
    }

    // 기존 메서드: 역할 가져오기
    public String getRole() {
        return sharedPreferences.getString(KEY_ROLE, null);
    }

    // 기존 메서드: 유저네임 가져오기
    public String getUsername() {
        return sharedPreferences.getString(KEY_USERNAME, null);
    }

    // 기존 메서드: 이미지 URL 가져오기
    public String getUserImageUrl() {
        return sharedPreferences.getString(KEY_USER_IMAGE, null);
    }

    // 새로운 메서드: 이메일 가져오기
    public String getEmail() {
        return sharedPreferences.getString(KEY_EMAIL, null);
    }

    // 새로운 메서드: 전화번호 가져오기
    public String getPhone() {
        return sharedPreferences.getString(KEY_PHONE, null);
    }

    // 새로운 메서드: 바이오 가져오기
    public String getBio() {
        return sharedPreferences.getString(KEY_BIO, null);
    }

    // 기존 메서드: 세션 클리어
    public void clearSession() {
        if (!sharedPreferences.getAll().isEmpty()) { // Check if there are stored preferences
            editor.clear();
            editor.apply();
            Log.d("SessionManager", "Session cleared successfully.");
        } else {
            Log.d("SessionManager", "No session data to clear.");
        }
    }
}
