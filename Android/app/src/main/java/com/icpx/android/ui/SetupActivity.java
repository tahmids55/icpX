package com.icpx.android.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.icpx.android.R;
import com.icpx.android.database.UserDAO;
import com.icpx.android.model.User;

import org.mindrot.jbcrypt.BCrypt;

/**
 * Setup activity for first-time users
 */
public class SetupActivity extends AppCompatActivity {

    private TextInputLayout usernameLayout;
    private TextInputLayout passwordLayout;
    private TextInputLayout codeforcesHandleLayout;
    private TextInputEditText usernameEditText;
    private TextInputEditText passwordEditText;
    private TextInputEditText codeforcesHandleEditText;
    private MaterialCheckBox startupPasswordCheckbox;
    private MaterialButton createAccountButton;
    private ProgressBar progressBar;
    
    private UserDAO userDAO;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup);

        initViews();
        userDAO = new UserDAO(this);
        
        createAccountButton.setOnClickListener(v -> handleCreateAccount());
    }

    private void initViews() {
        usernameLayout = findViewById(R.id.usernameLayout);
        passwordLayout = findViewById(R.id.passwordLayout);
        codeforcesHandleLayout = findViewById(R.id.codeforcesHandleLayout);
        usernameEditText = findViewById(R.id.usernameEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        codeforcesHandleEditText = findViewById(R.id.codeforcesHandleEditText);
        startupPasswordCheckbox = findViewById(R.id.startupPasswordCheckbox);
        createAccountButton = findViewById(R.id.createAccountButton);
        progressBar = findViewById(R.id.progressBar);
    }

    private void handleCreateAccount() {
        String username = usernameEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString();
        String codeforcesHandle = codeforcesHandleEditText.getText().toString().trim();
        boolean startupPasswordEnabled = startupPasswordCheckbox.isChecked();

        // Validate inputs
        boolean isValid = true;
        
        if (username.isEmpty()) {
            usernameLayout.setError(getString(R.string.field_required));
            isValid = false;
        } else {
            usernameLayout.setError(null);
        }

        if (password.isEmpty()) {
            passwordLayout.setError(getString(R.string.field_required));
            isValid = false;
        } else if (password.length() < 4) {
            passwordLayout.setError("Password must be at least 4 characters");
            isValid = false;
        } else {
            passwordLayout.setError(null);
        }

        if (!isValid) return;

        // Show progress
        progressBar.setVisibility(View.VISIBLE);
        createAccountButton.setEnabled(false);

        // Create account in background
        new Thread(() -> {
            // Hash password
            String passwordHash = BCrypt.hashpw(password, BCrypt.gensalt());
            
            // Create user
            User user = new User(username, passwordHash, codeforcesHandle);
            user.setStartupPasswordEnabled(startupPasswordEnabled);
            
            long userId = userDAO.createUser(user);

            runOnUiThread(() -> {
                progressBar.setVisibility(View.GONE);
                createAccountButton.setEnabled(true);

                if (userId > 0) {
                    Toast.makeText(this, R.string.account_created, Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(SetupActivity.this, MainActivity.class);
                    startActivity(intent);
                    finish();
                    overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
                } else {
                    Toast.makeText(this, "Failed to create account", Toast.LENGTH_LONG).show();
                }
            });
        }).start();
    }
}
