package com.icpx.service;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.api.client.auth.oauth2.TokenResponse;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.icpx.MainApp;
import javafx.application.Platform;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.Arrays;
import java.util.List;

public class AuthService {

    private static final String API_KEY = "AIzaSyDXTK4yNZGclT7Xe-KL8La58G5ZzDQ04hQ";
    private static final String AUTH_URL = "https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=" + API_KEY;
    private static final String IDP_URL = "https://identitytoolkit.googleapis.com/v1/accounts:signInWithIdp?key=" + API_KEY;
    private static final String CREDENTIALS_FILE = "/desktop_oauth_credentials.json";
    private static final String TOKENS_DIRECTORY_PATH = System.getProperty("user.home") + "/.icpx/tokens";
    private static final String SESSION_FILE_PATH = System.getProperty("user.home") + "/.icpx/session.json";
    
    private static String currentUserId;
    private static String currentUserEmail;
    private static String idToken;
    
    // Static initializer to restore session on class load
    static {
        restoreSession();
    }
    
    /**
     * Restore session from saved file (persistent login)
     */
    private static void restoreSession() {
        try {
            File sessionFile = new File(SESSION_FILE_PATH);
            if (sessionFile.exists()) {
                try (FileReader reader = new FileReader(sessionFile)) {
                    JsonObject session = new Gson().fromJson(reader, JsonObject.class);
                    if (session != null) {
                        if (session.has("userId")) {
                            currentUserId = session.get("userId").getAsString();
                        }
                        if (session.has("email")) {
                            currentUserEmail = session.get("email").getAsString();
                        }
                        if (session.has("idToken")) {
                            idToken = session.get("idToken").getAsString();
                        }
                        System.out.println("Session restored for: " + currentUserEmail);
                        
                        // Sync user profile to Firebase on session restore
                        syncUserProfileOnStartup();
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to restore session: " + e.getMessage());
        }
    }
    
    /**
     * Sync user profile to Firebase on startup
     */
    private static void syncUserProfileOnStartup() {
        if (!isAuthenticated()) return;
        
        new Thread(() -> {
            try {
                // Get current rating
                double rating = com.icpx.database.SettingsDAO.getUserRating();
                FriendsService.syncUserRatingToFirebase(rating);
            } catch (Exception e) {
                System.err.println("Error syncing profile on startup: " + e.getMessage());
            }
        }).start();
    }
    
    /**
     * Save session to file for persistent login
     */
    private static void saveSession() {
        try {
            File sessionDir = new File(System.getProperty("user.home") + "/.icpx");
            if (!sessionDir.exists()) {
                sessionDir.mkdirs();
            }
            
            JsonObject session = new JsonObject();
            session.addProperty("userId", currentUserId);
            session.addProperty("email", currentUserEmail);
            session.addProperty("idToken", idToken);
            
            try (FileWriter writer = new FileWriter(SESSION_FILE_PATH)) {
                new Gson().toJson(session, writer);
            }
            System.out.println("Session saved for: " + currentUserEmail);
        } catch (Exception e) {
            System.err.println("Failed to save session: " + e.getMessage());
        }
    }
    
    /**
     * Clear saved session file
     */
    private static void clearSession() {
        try {
            File sessionFile = new File(SESSION_FILE_PATH);
            if (sessionFile.exists()) {
                sessionFile.delete();
            }
        } catch (Exception e) {
            System.err.println("Failed to clear session: " + e.getMessage());
        }
    }

    public static boolean signIn(String email, String password) {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPost request = new HttpPost(AUTH_URL);
            request.setHeader("Content-Type", "application/json");

            JsonObject json = new JsonObject();
            json.addProperty("email", email);
            json.addProperty("password", password);
            json.addProperty("returnSecureToken", true);

            request.setEntity(new StringEntity(json.toString()));

            try (CloseableHttpResponse response = httpClient.execute(request)) {
                String responseString = EntityUtils.toString(response.getEntity());
                JsonObject responseJson = new Gson().fromJson(responseString, JsonObject.class);

                if (responseJson.has("localId") && responseJson.has("idToken")) {
                    currentUserId = responseJson.get("localId").getAsString();
                    currentUserEmail = responseJson.get("email").getAsString();
                    idToken = responseJson.get("idToken").getAsString();
                    saveSession(); // Persist login state
                    return true;
                } else {
                    System.err.println("Auth failed: " + responseString);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean signInWithGoogle() {
        try {
            // Load client secrets from credentials file
            InputStream in = AuthService.class.getResourceAsStream(CREDENTIALS_FILE);
            if (in == null) {
                System.err.println("Resource not found: " + CREDENTIALS_FILE);
                System.err.println("Please create desktop_oauth_credentials.json in src/main/resources/");
                return false;
            }
            
            NetHttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
            JsonFactory jsonFactory = GsonFactory.getDefaultInstance();
            
            GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(jsonFactory, new InputStreamReader(in));
            
            // Check if credentials are configured
            String clientId = clientSecrets.getDetails().getClientId();
            if (clientId == null || clientId.contains("YOUR_DESKTOP_CLIENT_ID")) {
                System.err.println("ERROR: Please configure your Desktop OAuth credentials!");
                System.err.println("Edit: src/main/resources/desktop_oauth_credentials.json");
                System.err.println("Get credentials from: https://console.cloud.google.com/apis/credentials");
                return false;
            }
            
            // Scopes for user info
            List<String> scopes = Arrays.asList("openid", "email", "profile");
            
            // Create tokens directory if it doesn't exist
            File tokensDir = new File(TOKENS_DIRECTORY_PATH);
            if (!tokensDir.exists()) {
                tokensDir.mkdirs();
            }

            // Build the authorization flow with client secret
            GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                    httpTransport, jsonFactory, clientSecrets, scopes)
                    .setDataStoreFactory(new FileDataStoreFactory(tokensDir))
                    .setAccessType("offline")
                    .build();

            // Always start OAuth flow with browser to let user choose account
            // (Don't use cached credentials - user wants to see account picker)
            LocalServerReceiver receiver = new LocalServerReceiver.Builder()
                    .setPort(8888)
                    .setCallbackPath("/Callback")
                    .build();

            try {
                String redirectUri = receiver.getRedirectUri();
                String url = flow.newAuthorizationUrl()
                        .setRedirectUri(redirectUri)
                        .build();

                System.out.println("Opening browser for Google Sign-In...");
                System.out.println("If browser doesn't open, visit: " + url);

                // Open browser using JavaFX HostServices
                Platform.runLater(() -> {
                    if (MainApp.getInstance() != null) {
                        MainApp.getInstance().openUrl(url);
                    }
                });

                // Wait for authorization code
                String code = receiver.waitForCode();
                
                // Exchange code for tokens
                TokenResponse tokenResponse = flow.newTokenRequest(code)
                        .setRedirectUri(redirectUri)
                        .execute();

                // Store credentials for future use
                flow.createAndStoreCredential(tokenResponse, "user");

                return exchangeGoogleTokenForFirebase(tokenResponse.getAccessToken());
            } finally {
                receiver.stop();
            }
        } catch (Exception e) {
            System.err.println("Google Auth error: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    private static boolean exchangeGoogleTokenForFirebase(String accessToken) {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPost request = new HttpPost(IDP_URL);
            request.setHeader("Content-Type", "application/json");

            JsonObject json = new JsonObject();
            json.addProperty("postBody", "access_token=" + accessToken + "&providerId=google.com");
            json.addProperty("requestUri", "http://localhost");
            json.addProperty("returnIdpCredential", true);
            json.addProperty("returnSecureToken", true);

            request.setEntity(new StringEntity(json.toString()));

            try (CloseableHttpResponse response = httpClient.execute(request)) {
                String responseString = EntityUtils.toString(response.getEntity());
                JsonObject responseJson = new Gson().fromJson(responseString, JsonObject.class);

                if (responseJson.has("localId") && responseJson.has("idToken")) {
                    currentUserId = responseJson.get("localId").getAsString();
                    currentUserEmail = responseJson.get("email").getAsString();
                    idToken = responseJson.get("idToken").getAsString();
                    saveSession(); // Persist login state
                    return true;
                } else {
                    System.err.println("Firebase exchange failed: " + responseString);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static void signOut() {
        currentUserId = null;
        currentUserEmail = null;
        idToken = null;
        
        // Clear persisted session
        clearSession();
        
        // Clear stored Google OAuth tokens
        try {
            File tokensDir = new File(TOKENS_DIRECTORY_PATH);
            if (tokensDir.exists()) {
                File[] files = tokensDir.listFiles();
                if (files != null) {
                    for (File file : files) {
                        file.delete();
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error clearing stored tokens: " + e.getMessage());
        }
    }

    public static String getCurrentUserId() {
        return currentUserId;
    }

    public static String getCurrentUserEmail() {
        return currentUserEmail;
    }
    
    public static String getIdToken() {
        return idToken;
    }
    
    public static boolean isAuthenticated() {
        return currentUserId != null;
    }
}
