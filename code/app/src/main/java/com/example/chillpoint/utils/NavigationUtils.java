package com.example.chillpoint.utils;

import android.app.Activity;
import android.content.Intent;

import com.example.chillpoint.R;
import com.example.chillpoint.views.activities.MainActivity;
import com.example.chillpoint.views.activities.UserMainActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class NavigationUtils {
    public static void handleBottomNavigation(Activity activity, BottomNavigationView bottomNavigationView) {
        bottomNavigationView.setOnItemSelectedListener(item -> {
            String title = item.getTitle().toString(); // Get the title of the clicked item
            Intent intent;

            switch (title) {
                case "Explore": // Match with the title defined in the menu
                    if (!(activity instanceof MainActivity)) {
                        intent = new Intent(activity, MainActivity.class);
                        activity.startActivity(intent);
                        activity.overridePendingTransition(0, 0);
                        activity.finish();
                    }
                    return true;

                case "Trips": // Match with the title defined in the menu
                    if (!(activity instanceof UserMainActivity)) {
                        intent = new Intent(activity, UserMainActivity.class);
                        activity.startActivity(intent);
                        activity.overridePendingTransition(0, 0);
                        activity.finish();
                    }
                    return true;
//                case "Trips":
//                    return true;
//                case "Wishlists":
//                    //check if no user authenticate, navigate to login activity
//                    return true;
//                case "Inbox":
//                    return true;
                default:
                    return false;
            }
        });
    }
}
