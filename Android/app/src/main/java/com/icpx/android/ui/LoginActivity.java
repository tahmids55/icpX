package com.icpx.android.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.icpx.android.R;
import com.icpx.android.database.UserDAO;
import com.icpx.android.model.User;

import org.mindrot.jbcrypt.BCrypt;

/**
 * Login activity
 */
public class LoginActivity extends AppCompatActivity {

    private TextInputLayout usernameLayout;
    private TextInputLayout passwordLayout;
    private TextInputEditText usernameEditText;
    private TextInputEditText passwordEditText;
    private MaterialButton loginButton;
    private ProgressBar progressBar;
    
    private UserDAO userDAO;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        initViews();
        userDAO = new UserDAO(this);
        
        loginButton.setOnClickListener(v -> handleLogin());
    }

    private void initViews() {
        usernameLayout = findViewById(R.id.usernameLayout);
        passwordLayout = findViewById(R.id.passwordLayout);
        usernameEditText = findViewById(R.id.usernameEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        loginButton = findViewById(R.id.loginButton);
        progressBar = findViewById(R.id.progressBar);
    }

    private void handleLogin() {
        String username = usernameEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString();

        // Validate inputs
        if (username.isEmpty()) {
            usernameLayout.setError(getString(R.string.field_required));
            return;
        }
        if (password.isEmpty()) {
            passwordLayout.setError(getString(R.string.field_required));
            return;
        }

        // Clear errors
        usernameLayout.setError(null);
        passwordLayout.setError(null);

        // Show progress
        progressBar.setVisibility(View.VISIBLE);
        loginButton.setEnabled(false);

        // Perform login in background
        new Thread(() -> {
            User user = userDAO.getUserByUsername(username);
            boolean loginSuccessful = user != null && 
                BCrypt.checkpw(password, user.getPasswordHash());

            runOnUiThread(() -> {
                progressBar.setVisibility(View.GONE);
                loginButton.setEnabled(true);

                if (loginSuccessful) {
                    Toast.makeText(this, R.string.login_successful, Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                    startActivity(intent);
                    finish();
                    overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
                } else {
                    Toast.makeText(this, R.string.login_failed, Toast.LENGTH_LONG).show();
                    passwordEditText.setText("");
                }
            });
        }).start();
    }
}
