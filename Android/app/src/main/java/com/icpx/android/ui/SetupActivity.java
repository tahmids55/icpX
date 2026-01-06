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
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.icpx.android.R;
import com.icpx.android.database.UserDAO;
import com.icpx.android.firebase.FirebaseManager;
import com.icpx.android.model.User;

import org.mindrot.jbcrypt.BCrypt;

import java.util.HashMap;
import java.util.Map;

/**
 * Setup activity for first-time users
 */
public class SetupActivity extends AppCompatActivity {

    private static final String TAG = "SetupActivity";
    
    private TextInputLayout usernameLayout;
    private TextInputLayout passwordLayout;
    private TextInputLayout codeforcesHandleLayout;
    private TextInputEditText usernameEditText;
    private TextInputEditText passwordEditText;
    private TextInputEditText codeforcesHandleEditText;
    private MaterialCheckBox startupPasswordCheckbox;
    private MaterialButton createAccountButton;
    private MaterialButton googleSignInButton;
    private android.widget.TextView loginLinkText;
    private ProgressBar progressBar;
    
    private UserDAO userDAO;
    private FirebaseAuth firebaseAuth;
    private GoogleSignInClient googleSignInClient;
    private ActivityResultLauncher<Intent> googleSignInLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup);

        initViews();
        userDAO = new UserDAO(this);
        firebaseAuth = FirebaseAuth.getInstance();
        
        // Setup Google Sign-In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        googleSignInClient = GoogleSignIn.getClient(this, gso);
        
        // Setup activity result launcher for Google Sign-In
        googleSignInLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    Log.d(TAG, "Google Sign-In result received - code: " + result.getResultCode());
                    Toast.makeText(this, "Result code: " + result.getResultCode(), Toast.LENGTH_SHORT).show();
                    
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                        handleGoogleSignInResult(task);
                    } else {
                        Log.d(TAG, "Google Sign-In cancelled or failed - code: " + result.getResultCode());
                        progressBar.setVisibility(View.GONE);
                        googleSignInButton.setEnabled(true);
                        if (result.getResultCode() == Activity.RESULT_CANCELED) {
                            Toast.makeText(this, "Sign-in cancelled", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(this, "Sign-in failed with code: " + result.getResultCode(), Toast.LENGTH_LONG).show();
                        }
                    }
                });
        
        createAccountButton.setOnClickListener(v -> handleCreateAccount());
        googleSignInButton.setOnClickListener(v -> signInWithGoogle());
        loginLinkText.setOnClickListener(v -> {
            Intent intent = new Intent(SetupActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        });
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
        googleSignInButton = findViewById(R.id.googleSignInButton);
        loginLinkText = findViewById(R.id.loginLinkText);
        progressBar = findViewById(R.id.progressBar);
    }

    private void handleCreateAccount() {
        String email = usernameEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString();
        String codeforcesHandle = codeforcesHandleEditText.getText().toString().trim();
        boolean startupPasswordEnabled = startupPasswordCheckbox.isChecked();

        // Validate inputs
        boolean isValid = true;
        
        if (email.isEmpty()) {
            usernameLayout.setError(getString(R.string.field_required));
            isValid = false;
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            usernameLayout.setError("Please enter a valid email");
            isValid = false;
        } else {
            usernameLayout.setError(null);
        }

        if (password.isEmpty()) {
            passwordLayout.setError(getString(R.string.field_required));
            isValid = false;
        } else if (password.length() < 6) {
            passwordLayout.setError("Password must be at least 6 characters");
            isValid = false;
        } else {
            passwordLayout.setError(null);
        }

        if (!isValid) return;

        // Show progress
        progressBar.setVisibility(View.VISIBLE);
        createAccountButton.setEnabled(false);

        // Create Firebase account
        firebaseAuth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener(authResult -> {
                FirebaseUser firebaseUser = authResult.getUser();
                if (firebaseUser != null) {
                    // Save user data to Firestore
                    Map<String, Object> userData = new HashMap<>();
                    userData.put("email", email);
                    userData.put("codeforcesHandle", codeforcesHandle);
                    userData.put("startupPasswordEnabled", startupPasswordEnabled);
                    userData.put("createdAt", System.currentTimeMillis());
                    
                    FirebaseManager.getInstance().saveUserData(firebaseUser.getUid(), userData,
                        new FirebaseManager.FirestoreCallback() {
                            @Override
                            public void onSuccess() {
                                // Also save locally for offline access
                                new Thread(() -> {
                                    String passwordHash = BCrypt.hashpw(password, BCrypt.gensalt());
                                    User user = new User(email, passwordHash, codeforcesHandle);
                                    user.setStartupPasswordEnabled(startupPasswordEnabled);
                                    userDAO.createUser(user);
                                }).start();
                                
                                runOnUiThread(() -> {
                                    progressBar.setVisibility(View.GONE);
                                    Toast.makeText(SetupActivity.this, "Account created! Signed in as " + email, Toast.LENGTH_SHORT).show();
                                    Intent intent = new Intent(SetupActivity.this, MainActivity.class);
                                    startActivity(intent);
                                    finish();
                                    overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
                                });
                            }
                            
                            @Override
                            public void onFailure(Exception e) {
                                runOnUiThread(() -> {
                                    progressBar.setVisibility(View.GONE);
                                    createAccountButton.setEnabled(true);
                                    Toast.makeText(SetupActivity.this, "Failed to save user data: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                });
                            }
                        });
                }
            })
            .addOnFailureListener(e -> {
                progressBar.setVisibility(View.GONE);
                createAccountButton.setEnabled(true);
                Toast.makeText(this, "Account creation failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
            });
    }
    
    private void signInWithGoogle() {
        Log.d(TAG, "Starting Google Sign-In flow");
        Toast.makeText(this, "Opening Google Sign-In...", Toast.LENGTH_SHORT).show();
        progressBar.setVisibility(View.VISIBLE);
        googleSignInButton.setEnabled(false);
        
        // Sign out first to ensure fresh sign-in
        googleSignInClient.signOut().addOnCompleteListener(this, task -> {
            try {
                Intent signInIntent = googleSignInClient.getSignInIntent();
                Log.d(TAG, "Launching Google Sign-In intent");
                googleSignInLauncher.launch(signInIntent);
            } catch (Exception e) {
                Log.e(TAG, "Error launching Google Sign-In", e);
                progressBar.setVisibility(View.GONE);
                googleSignInButton.setEnabled(true);
                Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
    
    private void handleGoogleSignInResult(Task<GoogleSignInAccount> completedTask) {
        Log.d(TAG, "handleGoogleSignInResult called, isSuccessful: " + completedTask.isSuccessful());
        
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);
            if (account == null) {
                throw new Exception("Account is null");
            }
            Log.d(TAG, "Google Sign-In successful, account: " + account.getEmail());
            Toast.makeText(this, "Signed in as " + account.getEmail(), Toast.LENGTH_SHORT).show();
            
            // Show progress
            progressBar.setVisibility(View.VISIBLE);
            googleSignInButton.setEnabled(false);
            createAccountButton.setEnabled(false);
            
            // Sign in to Firebase with Google account
            Log.d(TAG, "Signing in to Firebase with Google account");
            Toast.makeText(this, "Connecting to Firebase...", Toast.LENGTH_SHORT).show();
            
            FirebaseManager.getInstance().signInWithGoogle(account, new FirebaseManager.AuthCallback() {
                @Override
                public void onSuccess(FirebaseUser user) {
                    Log.d(TAG, "Firebase sign-in successful: " + user.getEmail());
                    runOnUiThread(() -> Toast.makeText(SetupActivity.this, "Saving data...", Toast.LENGTH_SHORT).show());
                    // Save user data to Firestore
                    String codeforcesHandle = codeforcesHandleEditText.getText().toString().trim();
                    
                    Map<String, Object> userData = new HashMap<>();
                    userData.put("email", user.getEmail());
                    userData.put("displayName", user.getDisplayName());
                    userData.put("photoUrl", user.getPhotoUrl() != null ? user.getPhotoUrl().toString() : "");
                    userData.put("codeforcesHandle", codeforcesHandle);
                    userData.put("startupPasswordEnabled", false); // No password for Google sign-in
                    userData.put("createdAt", System.currentTimeMillis());
                    
                    Log.d(TAG, "Saving user data to Firestore for UID: " + user.getUid());
                    Log.d(TAG, "User data: " + userData.toString());
                    
                    // Save locally first
                    new Thread(() -> {
                        try {
                            User localUser = new User(user.getEmail(), "", codeforcesHandle);
                            localUser.setStartupPasswordEnabled(false);
                            userDAO.createUser(localUser);
                            Log.d(TAG, "User saved to local database");
                        } catch (Exception e) {
                            Log.e(TAG, "Error saving to local database", e);
                        }
                    }).start();
                    
                    // Save to Firestore in background (non-blocking)
                    FirebaseManager.getInstance().saveUserData(user.getUid(), userData,
                        new FirebaseManager.FirestoreCallback() {
                            @Override
                            public void onSuccess() {
                                Log.d(TAG, "User data saved successfully to Firestore");
                            }
                            
                            @Override
                            public void onFailure(Exception e) {
                                Log.e(TAG, "Failed to save user data to Firestore", e);
                            }
                        });
                    
                    // Navigate to dashboard immediately
                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(SetupActivity.this, 
                                "Welcome " + user.getDisplayName() + "!", 
                                Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(SetupActivity.this, MainActivity.class);
                        startActivity(intent);
                        finish();
                    });
                }
                
                @Override
                public void onFailure(Exception e) {
                    Log.e(TAG, "Firebase sign-in with Google failed", e);
                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        googleSignInButton.setEnabled(true);
                        createAccountButton.setEnabled(true);
                        Toast.makeText(SetupActivity.this, 
                                "Google sign-in failed: " + e.getMessage(), 
                                Toast.LENGTH_LONG).show();
                    });
                }
            });
            
        } catch (ApiException e) {
            Log.e(TAG, "Google Sign-In ApiException - Status code: " + e.getStatusCode() + ", Message: " + e.getMessage());
            progressBar.setVisibility(View.GONE);
            googleSignInButton.setEnabled(true);
            
            String errorMsg;
            switch (e.getStatusCode()) {
                case 10:
                    errorMsg = "Developer error: Check SHA-1 certificate and package name in Firebase Console";
                    break;
                case 12500:
                    errorMsg = "Sign-in currently unavailable. Please try again later.";
                    break;
                case 12501:
                    errorMsg = "Sign-in cancelled";
                    break;
                default:
                    errorMsg = "Sign-in failed (Code " + e.getStatusCode() + "): " + e.getMessage();
            }
            Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error in handleGoogleSignInResult", e);
            progressBar.setVisibility(View.GONE);
            googleSignInButton.setEnabled(true);
            createAccountButton.setEnabled(true);
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}
