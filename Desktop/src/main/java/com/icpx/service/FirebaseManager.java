package com.icpx.service;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.cloud.FirestoreClient; // If using Firestore
import com.google.cloud.firestore.Firestore;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.UserRecord;

import java.io.IOException;
import java.io.InputStream;

public class FirebaseManager {
    private static boolean initialized = false;
    private static Firestore firestore;

    public static void initialize() {
        if (initialized) return;
        
        try {
            InputStream serviceAccount = FirebaseManager.class.getResourceAsStream("/service-account.json");
            
            if (serviceAccount == null) {
                System.err.println("Service account file not found!");
                return;
            }

            FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                .build();

            FirebaseApp.initializeApp(options);
            initialized = true;
            
            // Initialize Firestore
            firestore = FirestoreClient.getFirestore();
            
            System.out.println("Firebase initialized successfully.");
            
        } catch (IOException e) {
            System.err.println("Error initializing Firebase: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static Firestore getFirestore() {
        if (!initialized) {
            initialize();
        }
        return firestore;
    }
    
    /**
     * Look up a user by email using Firebase Auth Admin SDK
     * @param email The email to look up
     * @return UserRecord if found, null otherwise
     */
    public static UserRecord getUserByEmail(String email) {
        if (!initialized) {
            initialize();
        }
        try {
            return FirebaseAuth.getInstance().getUserByEmail(email);
        } catch (Exception e) {
            System.err.println("Could not find user by email: " + e.getMessage());
            return null;
        }
    }
    
    public static boolean isInitialized() {
        return initialized;
    }
}
