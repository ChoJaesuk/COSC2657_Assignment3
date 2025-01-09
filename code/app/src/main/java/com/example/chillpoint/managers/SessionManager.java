package com.example.chillpoint.managers;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

public class SessionManager {
    private static final String PREF_NAME = "ChillPointSession";
    private static final String KEY_USER_ID = "userId";
    private static final String KEY_ROLE = "role";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_USER_IMAGE = "userImageUrl";
    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;

    public SessionManager(Context context) {
        sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();
    }

    public void saveUserSession(String userId, String role, String username, String imageUrl) {
        editor.putString(KEY_USER_ID, userId);
        editor.putString(KEY_ROLE, role);
        editor.putString(KEY_USERNAME, username);
        editor.putString(KEY_USER_IMAGE, imageUrl);
        editor.apply();
    }

    public String getUserId() {
        return sharedPreferences.getString(KEY_USER_ID, null);
    }

    public String getRole() {
        return sharedPreferences.getString(KEY_ROLE, null);
    }

    public String getUsername() {
        return sharedPreferences.getString(KEY_USERNAME, null);
    }

    public void clearSession() {
        if (!sharedPreferences.getAll().isEmpty()) { // Check if there are stored preferences
            editor.clear();
            editor.apply();
            Log.d("SessionManager", "Session cleared successfully.");
        } else {
            Log.d("SessionManager", "No session data to clear.");
        }
    }

    public String getUserImageUrl() {
        return sharedPreferences.getString(KEY_USER_IMAGE, null); // 유저 이미지 URL 가져오기
    }
}
