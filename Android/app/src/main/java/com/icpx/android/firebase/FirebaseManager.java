package com.icpx.android.firebase;

import android.util.Log;

import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

/**
 * Firebase manager for authentication and Firestore operations
 */
public class FirebaseManager {
    
    private static final String TAG = "FirebaseManager";
    private static FirebaseManager instance;
    private FirebaseAuth auth;
    private FirebaseFirestore firestore;
    
    private FirebaseManager() {
        auth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
    }
    
    public static synchronized FirebaseManager getInstance() {
        if (instance == null) {
            instance = new FirebaseManager();
        }
        return instance;
    }
    
    /**
     * Get current Firebase user
     */
    public FirebaseUser getCurrentUser() {
        return auth.getCurrentUser();
    }
    
    /**
     * Sign in with email and password
     */
    public void signIn(String email, String password, AuthCallback callback) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener(authResult -> callback.onSuccess(authResult.getUser()))
            .addOnFailureListener(callback::onFailure);
    }
    
    /**
     * Create new user with email and password
     */
    public void signUp(String email, String password, AuthCallback callback) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener(authResult -> callback.onSuccess(authResult.getUser()))
            .addOnFailureListener(callback::onFailure);
    }
    
    /**
     * Sign out current user
     */
    public void signOut() {
        auth.signOut();
    }
    
    /**
     * Sign in with Google account
     */
    public void signInWithGoogle(GoogleSignInAccount account, AuthCallback callback) {
        AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
        auth.signInWithCredential(credential)
            .addOnSuccessListener(authResult -> callback.onSuccess(authResult.getUser()))
            .addOnFailureListener(callback::onFailure);
    }
    
    /**
     * Save target to Firestore
     */
    public void saveTarget(String userId, Map<String, Object> targetData, FirestoreCallback callback) {
        String targetId = (String) targetData.get("id");
        firestore.collection("users")
                .document(userId)
                .collection("targets")
                .document(targetId)
                .set(targetData, SetOptions.merge())
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(callback::onFailure);
    }
    
    /**
     * Get all targets for a user
     */
    public void getTargets(String userId, DataCallback callback) {
        firestore.collection("users")
                .document(userId)
                .collection("targets")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    callback.onSuccess(queryDocumentSnapshots.getDocuments());
                })
                .addOnFailureListener(callback::onFailure);
    }
    
    /**
     * Save user data to Firestore
     */
    public void saveUserData(String userId, Map<String, Object> userData, FirestoreCallback callback) {
        Log.d(TAG, "saveUserData called for userId: " + userId);
        firestore.collection("users")
                .document(userId)
                .set(userData, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "saveUserData successful");
                    callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "saveUserData failed", e);
                    callback.onFailure(e);
                });
    }
    
    /**
     * Get user data from Firestore
     */
    public void getUserData(String userId, DataCallback callback) {
        firestore.collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        java.util.List<com.google.firebase.firestore.DocumentSnapshot> list = new java.util.ArrayList<>();
                        list.add(documentSnapshot);
                        callback.onSuccess(list);
                    } else {
                        callback.onFailure(new Exception("User document not found"));
                    }
                })
                .addOnFailureListener(callback::onFailure);
    }
    
    /**
     * Delete target from Firestore
     */
    public void deleteTarget(String userId, String targetId, FirestoreCallback callback) {
        firestore.collection("users")
                .document(userId)
                .collection("targets")
                .document(targetId)
                .delete()
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(callback::onFailure);
    }
    
    // Callback interfaces
    public interface AuthCallback {
        void onSuccess(FirebaseUser user);
        void onFailure(Exception e);
    }
    
    public interface FirestoreCallback {
        void onSuccess();
        void onFailure(Exception e);
    }
    
    public interface DataCallback {
        void onSuccess(java.util.List<com.google.firebase.firestore.DocumentSnapshot> documents);
        void onFailure(Exception e);
    }
}
