package com.icpx.android.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.icpx.android.R;
import com.icpx.android.database.UserDAO;
import com.icpx.android.firebase.FirebaseManager;
import com.icpx.android.model.User;

import org.mindrot.jbcrypt.BCrypt;

/**
 * Login activity
 */
public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";
    
    private TextInputLayout usernameLayout;
    private TextInputLayout passwordLayout;
    private TextInputEditText usernameEditText;
    private TextInputEditText passwordEditText;
    private MaterialButton loginButton;
    private MaterialButton googleSignInButton;
    private ProgressBar progressBar;
    
    private UserDAO userDAO;
    private FirebaseAuth firebaseAuth;
    private GoogleSignInClient googleSignInClient;
    private ActivityResultLauncher<Intent> googleSignInLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        initViews();
        userDAO = new UserDAO(this);
        firebaseAuth = FirebaseAuth.getInstance();
        
        // Setup Google Sign-In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        googleSignInClient = GoogleSignIn.getClient(this, gso);
        
        // Setup activity result launcher
        googleSignInLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                        handleGoogleSignInResult(task);
                    } else {
                        progressBar.setVisibility(View.GONE);
                        googleSignInButton.setEnabled(true);
                    }
                });
        
        loginButton.setOnClickListener(v -> handleLogin());
        googleSignInButton.setOnClickListener(v -> signInWithGoogle());
    }

    private void initViews() {
        usernameLayout = findViewById(R.id.usernameLayout);
        passwordLayout = findViewById(R.id.passwordLayout);
        usernameEditText = findViewById(R.id.usernameEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        loginButton = findViewById(R.id.loginButton);
        googleSignInButton = findViewById(R.id.googleSignInButton);
        progressBar = findViewById(R.id.progressBar);
    }

    private void handleLogin() {
        String email = usernameEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString();

        // Validate inputs
        if (email.isEmpty()) {
            usernameLayout.setError(getString(R.string.field_required));
            return;
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            usernameLayout.setError("Please enter a valid email");
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

        // Sign in with Firebase
        firebaseAuth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener(authResult -> {
                FirebaseUser firebaseUser = authResult.getUser();
                if (firebaseUser != null) {
                    Toast.makeText(this, "Welcome back!", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                    startActivity(intent);
                    finish();
                    overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
                }
            })
            .addOnFailureListener(e -> {
                progressBar.setVisibility(View.GONE);
                loginButton.setEnabled(true);
                Toast.makeText(this, "Login failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                passwordEditText.setText("");
            });
    }
    
    private void signInWithGoogle() {
        progressBar.setVisibility(View.VISIBLE);
        googleSignInButton.setEnabled(false);
        
        googleSignInClient.signOut().addOnCompleteListener(this, task -> {
            Intent signInIntent = googleSignInClient.getSignInIntent();
            googleSignInLauncher.launch(signInIntent);
        });
    }
    
    private void handleGoogleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);
            
            // Sign in to Firebase
            FirebaseManager.getInstance().signInWithGoogle(account, new FirebaseManager.AuthCallback() {
                @Override
                public void onSuccess(FirebaseUser user) {
                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(LoginActivity.this, "Welcome back " + user.getDisplayName() + "!", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                        startActivity(intent);
                        finish();
                    });
                }
                
                @Override
                public void onFailure(Exception e) {
                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        googleSignInButton.setEnabled(true);
                        Toast.makeText(LoginActivity.this, "Login failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
                }
            });
        } catch (ApiException e) {
            progressBar.setVisibility(View.GONE);
            googleSignInButton.setEnabled(true);
            Toast.makeText(this, "Google sign-in failed", Toast.LENGTH_SHORT).show();
        }
    }
}
