package com.icpx.android.ui;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import androidx.appcompat.app.AppCompatActivity;

import com.icpx.android.R;
import com.icpx.android.database.UserDAO;

/**
 * Splash screen activity
 */
public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_DELAY = 2000; // 2 seconds

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        new Handler().postDelayed(() -> {
            // Check if user exists
            UserDAO userDAO = new UserDAO(this);
            boolean userExists = userDAO.userExists();

            Intent intent;
            if (!userExists) {
                // First run - show setup
                intent = new Intent(SplashActivity.this, SetupActivity.class);
            } else {
                // Check if login is required from settings
                boolean requireLogin = getSharedPreferences("user_prefs", 0)
                        .getBoolean("require_login", true);
                
                if (requireLogin) {
                    // Show login screen
                    intent = new Intent(SplashActivity.this, LoginActivity.class);
                } else {
                    // Skip login - go directly to dashboard
                    intent = new Intent(SplashActivity.this, MainActivity.class);
                }
            }

            startActivity(intent);
            finish();
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        }, SPLASH_DELAY);
    }
}
