package com.icpx.android.ui;

import android.content.Context;
import android.content.DialogInterface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.icpx.android.R;
import com.icpx.android.utils.WebViewCacheManager;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.security.MessageDigest;

/**
 * Activity to display Codeforces problem statements in a WebView with offline caching support.
 * 
 * OFFLINE CACHING FEATURES:
 * - Automatically caches pages when loaded with internet connection
 * - Pages can be viewed offline after first successful load
 * - Smart cache mode selection based on network availability
 * - Uses WebView's built-in cache (managed by system)
 * - Menu options to view cache info and clear cache
 * 
 * CACHE MODES:
 * - Online: LOAD_DEFAULT (loads from network, updates cache)
 * - Offline: LOAD_CACHE_ELSE_NETWORK (loads from cache if available)
 * 
 * USAGE:
 * 1. First time: Requires internet to load and cache the page
 * 2. Subsequent visits: Can view without internet if previously cached
 * 3. Use "Clear Cache" menu option to free up space when needed
 */
public class ProblemWebViewActivity extends AppCompatActivity {

    private static final String TAG = "ProblemWebViewActivity";
    public static final String EXTRA_PROBLEM_URL = "problem_url";
    public static final String EXTRA_PROBLEM_NAME = "problem_name";
    
    private WebView webView;
    private ProgressBar progressBar;
    private boolean isNetworkAvailable = false;
    private String currentUrl = null;
    private File cacheDir;
    private boolean isLoadingFromCache = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_problem_webview);
        
        // Setup toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            
            // Set title from intent
            String problemName = getIntent().getStringExtra(EXTRA_PROBLEM_NAME);
            if (problemName != null && !problemName.isEmpty()) {
                getSupportActionBar().setTitle(problemName);
            } else {
                getSupportActionBar().setTitle("Problem Statement");
            }
        }
        
        // Initialize views
        webView = findViewById(R.id.webView);
        progressBar = findViewById(R.id.progressBar);
        
        // Initialize cache directory
        cacheDir = new File(getCacheDir(), "webview_html_cache");
        if (!cacheDir.exists()) {
            cacheDir.mkdirs();
        }
        
        // Check if WebView is properly initialized
        if (webView == null) {
            Log.e(TAG, "WebView not found in layout");
            Toast.makeText(this, "Error loading WebView", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        // Get problem URL from intent
        String problemUrl = getIntent().getStringExtra(EXTRA_PROBLEM_URL);
        currentUrl = problemUrl;
        
        if (problemUrl != null && !problemUrl.isEmpty()) {
            try {
                setupWebView();
                loadProblem(problemUrl);
            } catch (Exception e) {
                Log.e(TAG, "Error setting up WebView: " + e.getMessage(), e);
                Toast.makeText(this, "Error loading problem", Toast.LENGTH_SHORT).show();
                finish();
            }
        } else {
            // Handle error - no URL provided
            Log.e(TAG, "No URL provided to WebView");
            Toast.makeText(this, "No problem URL provided", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void setupWebView() {
        try {
            // Check network availability
            isNetworkAvailable = isNetworkAvailable();
            
            WebSettings webSettings = webView.getSettings();
            webSettings.setJavaScriptEnabled(true);
            webSettings.setDomStorageEnabled(true);
            webSettings.setLoadWithOverviewMode(true);
            webSettings.setUseWideViewPort(true);
            webSettings.setBuiltInZoomControls(true);
            webSettings.setDisplayZoomControls(false);
            webSettings.setSupportZoom(true);
            webSettings.setDefaultTextEncodingName("utf-8");
            
            // Additional settings for better compatibility
            webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
            webSettings.setDatabaseEnabled(true);
            webSettings.setAllowFileAccess(false);
            webSettings.setAllowContentAccess(false);
            
            // ============ OFFLINE CACHING CONFIGURATION ============
            // Enable all cache-related settings for persistent offline access
            webSettings.setDomStorageEnabled(true); // Already set above, but crucial for cache
            
            // Always try cache first for better performance
            webSettings.setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
            
            // Set WebViewClient to handle page navigation
            webView.setWebViewClient(new WebViewClient() {
                
                @Override
                public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                    // When loading from cache offline, block all external resource requests
                    if (isLoadingFromCache && !isNetworkAvailable) {
                        String url = request.getUrl().toString();
                        // Allow data URIs and about:blank
                        if (url.startsWith("data:") || url.startsWith("about:")) {
                            return super.shouldInterceptRequest(view, request);
                        }
                        // Block all other network requests
                        Log.d(TAG, "Blocking resource request (offline): " + url);
                        return new WebResourceResponse("text/plain", "UTF-8", null);
                    }
                    return super.shouldInterceptRequest(view, request);
                }
                
                @Override
                public void onPageFinished(WebView view, String url) {
                    super.onPageFinished(view, url);
                    if (progressBar != null) {
                        progressBar.setVisibility(View.GONE);
                    }
                    
                    Log.d(TAG, "Page finished loading: " + url);
                    Log.d(TAG, "Network available: " + isNetworkAvailable);
                    
                    // Reset loading from cache flag
                    if (isLoadingFromCache) {
                        isLoadingFromCache = false;
                        Log.d(TAG, "Successfully loaded from cache offline");
                    }
                    
                    // Save HTML content for offline access
                    if (isNetworkAvailable && url.contains("codeforces.com")) {
                        // Delay to ensure page is fully rendered
                        new Handler(Looper.getMainLooper()).postDelayed(() -> {
                            Log.d(TAG, "Attempting to save HTML with inline styles to cache...");
                            
                            // JavaScript to get HTML with inlined styles
                            String jsCode = 
                                "(function() {" +
                                "  var html = document.documentElement.cloneNode(true);" +
                                "  var styles = Array.from(document.styleSheets).map(sheet => {" +
                                "    try {" +
                                "      return Array.from(sheet.cssRules).map(rule => rule.cssText).join('\\n');" +
                                "    } catch(e) { return ''; }" +
                                "  }).join('\\n');" +
                                "  var styleTag = '<style>' + styles + '</style>';" +
                                "  var headContent = html.querySelector('head').innerHTML;" +
                                "  html.querySelector('head').innerHTML = styleTag + headContent;" +
                                "  return html.outerHTML;" +
                                "})();";
                            
                            view.evaluateJavascript(jsCode, htmlWithStyles -> {
                                if (htmlWithStyles != null && !htmlWithStyles.equals("null")) {
                                    Log.d(TAG, "HTML with inline styles received, length: " + htmlWithStyles.length());
                                    saveHtmlToCache(url, htmlWithStyles);
                                } else {
                                    Log.e(TAG, "Failed to get HTML with inline styles, falling back to normal HTML");
                                    // Fallback to normal HTML
                                    view.evaluateJavascript(
                                        "(function() { return document.documentElement.outerHTML; })();",
                                        html -> {
                                            if (html != null && !html.equals("null")) {
                                                saveHtmlToCache(url, html);
                                            }
                                        }
                                    );
                                }
                            });
                        }, 2000); // Wait 2 seconds for CSS to fully load
                    } else if (!isNetworkAvailable) {
                        Log.d(TAG, "Offline mode - not attempting to cache");
                    }
                }
                
                @Override
                public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                    super.onReceivedError(view, request, error);
                    Log.e(TAG, "WebView error: " + error.getDescription());
                    if (progressBar != null) {
                        progressBar.setVisibility(View.GONE);
                    }
                    runOnUiThread(() -> {
                        if (!isNetworkAvailable) {
                            Toast.makeText(ProblemWebViewActivity.this, 
                                "No cached version available. Please connect to internet.", 
                                Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(ProblemWebViewActivity.this, 
                                "Error loading page", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
                
                @Override
                public boolean shouldOverrideUrlLoading(WebView view, String url) {
                    // Allow navigation within Codeforces
                    if (url.contains("codeforces.com")) {
                        return false;
                    }
                    // Block external links
                    return true;
                }
            });
        
            // Set WebChromeClient to show loading progress
            webView.setWebChromeClient(new WebChromeClient() {
                @Override
                public void onProgressChanged(WebView view, int newProgress) {
                    if (progressBar != null) {
                        if (newProgress < 100) {
                            progressBar.setVisibility(View.VISIBLE);
                            progressBar.setProgress(newProgress);
                        } else {
                            progressBar.setVisibility(View.GONE);
                        }
                    }
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error in setupWebView: " + e.getMessage(), e);
            throw e;
        }
    }

    private void loadProblem(String url) {
        try {
            Log.d(TAG, "Loading URL: " + url);
            Log.d(TAG, "Network available: " + isNetworkAvailable);
            if (progressBar != null) {
                progressBar.setVisibility(View.VISIBLE);
            }
            
            // If offline, try to load from HTML cache first
            if (!isNetworkAvailable) {
                Log.d(TAG, "Attempting to load from HTML cache...");
                String cachedHtml = loadHtmlFromCache(url);
                if (cachedHtml != null && !cachedHtml.isEmpty()) {
                    Log.d(TAG, "Cache found! Loading from HTML cache. Size: " + cachedHtml.length());
                    Toast.makeText(this, "Loading from cache (Offline mode)", Toast.LENGTH_SHORT).show();
                    isLoadingFromCache = true;
                    webView.loadDataWithBaseURL(url, cachedHtml, "text/html", "UTF-8", null);
                    if (progressBar != null) {
                        progressBar.setVisibility(View.GONE);
                    }
                    return;
                } else {
                    Log.e(TAG, "No cached HTML found for this URL");
                    Toast.makeText(this, "No cached version available. Please connect to internet.", Toast.LENGTH_LONG).show();
                    if (progressBar != null) {
                        progressBar.setVisibility(View.GONE);
                    }
                    return;
                }
            }
            
            // Load from network
            Log.d(TAG, "Loading from network...");
            webView.loadUrl(url);
        } catch (Exception e) {
            Log.e(TAG, "Error loading URL: " + e.getMessage(), e);
            Toast.makeText(this, "Failed to load problem", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Save HTML content to cache file
     */
    private void saveHtmlToCache(String url, String html) {
        try {
            Log.d(TAG, "saveHtmlToCache called for URL: " + url);
            
            // Remove quotes added by JavaScript
            if (html.startsWith("\"") && html.endsWith("\"")) {
                html = html.substring(1, html.length() - 1);
            }
            html = html.replace("\\u003C", "<")
                      .replace("\\n", "\n")
                      .replace("\\t", "\t")
                      .replace("\\\"", "\"");
            
            File cacheFile = getCacheFile(url);
            Log.d(TAG, "Cache file path: " + cacheFile.getAbsolutePath());
            Log.d(TAG, "HTML content length after unescape: " + html.length());
            
            FileOutputStream fos = new FileOutputStream(cacheFile);
            fos.write(html.getBytes("UTF-8"));
            fos.close();
            
            // Verify file was written
            if (cacheFile.exists()) {
                Log.d(TAG, "✅ HTML cached successfully! File size: " + cacheFile.length() + " bytes");
                runOnUiThread(() -> {
                    Toast.makeText(ProblemWebViewActivity.this, 
                        "Page cached for offline access", Toast.LENGTH_SHORT).show();
                });
            } else {
                Log.e(TAG, "❌ Cache file was not created!");
            }
        } catch (Exception e) {
            Log.e(TAG, "❌ Error saving HTML to cache: " + e.getMessage(), e);
        }
    }

    /**
     * Load HTML content from cache file
     */
    private String loadHtmlFromCache(String url) {
        try {
            File cacheFile = getCacheFile(url);
            if (!cacheFile.exists()) {
                Log.d(TAG, "No cached HTML found for URL");
                return null;
            }
            
            FileInputStream fis = new FileInputStream(cacheFile);
            BufferedReader reader = new BufferedReader(new InputStreamReader(fis, "UTF-8"));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            reader.close();
            fis.close();
            
            Log.d(TAG, "Loaded HTML from cache: " + cacheFile.getName());
            return sb.toString();
        } catch (Exception e) {
            Log.e(TAG, "Error loading HTML from cache: " + e.getMessage(), e);
            return null;
        }
    }

    /**
     * Get cache file for a URL
     */
    private File getCacheFile(String url) {
        try {
            // Create a hash of the URL for the filename
            MessageDigest digest = MessageDigest.getInstance("MD5");
            byte[] hash = digest.digest(url.getBytes("UTF-8"));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xFF & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return new File(cacheDir, hexString.toString() + ".html");
        } catch (Exception e) {
            Log.e(TAG, "Error creating cache file: " + e.getMessage(), e);
            return new File(cacheDir, "default.html");
        }
    }

    /**
     * Check if network connection is available
     * @return true if network is available, false otherwise
     */
    private boolean isNetworkAvailable() {
        try {
            ConnectivityManager connectivityManager = 
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            if (connectivityManager != null) {
                NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
                boolean available = activeNetworkInfo != null && activeNetworkInfo.isConnected();
                Log.d(TAG, "Network available: " + available);
                return available;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error checking network: " + e.getMessage(), e);
        }
        return false;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.webview_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        
        if (itemId == android.R.id.home) {
            onBackPressed();
            return true;
        } else if (itemId == R.id.action_refresh) {
            // Refresh the page
            if (webView != null) {
                webView.reload();
                Toast.makeText(this, "Refreshing...", Toast.LENGTH_SHORT).show();
            }
            return true;
        } else if (itemId == R.id.action_cache_info) {
            // Show cache information
            showCacheInfo();
            return true;
        } else if (itemId == R.id.action_clear_cache) {
            // Clear cache with confirmation
            showClearCacheDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Show dialog with cache information
     */
    private void showCacheInfo() {
        String cacheSize = WebViewCacheManager.getFormattedCacheSize(this);
        String networkStatus = isNetworkAvailable ? "Online" : "Offline";
        
        new AlertDialog.Builder(this)
            .setTitle("Cache Information")
            .setMessage("Cache Size: " + cacheSize + "\n" +
                       "Network Status: " + networkStatus + "\n\n" +
                       "Cached pages can be viewed offline after first load.")
            .setPositiveButton("OK", null)
            .show();
    }

    /**
     * Show confirmation dialog before clearing cache
     */
    private void showClearCacheDialog() {
        new AlertDialog.Builder(this)
            .setTitle("Clear Cache")
            .setMessage("This will remove all cached pages. You'll need internet to view them again.\n\nAre you sure?")
            .setPositiveButton("Clear", (dialog, which) -> {
                WebViewCacheManager.clearCache(ProblemWebViewActivity.this, webView);
                // Also clear HTML cache
                clearHtmlCache();
                Toast.makeText(ProblemWebViewActivity.this, 
                    "Cache cleared successfully", Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton("Cancel", null)
            .show();
    }
    
    /**
     * Clear all HTML cache files
     */
    private void clearHtmlCache() {
        try {
            if (cacheDir != null && cacheDir.exists()) {
                File[] files = cacheDir.listFiles();
                if (files != null) {
                    for (File file : files) {
                        file.delete();
                    }
                }
                Log.d(TAG, "HTML cache cleared");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error clearing HTML cache: " + e.getMessage(), e);
        }
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (webView != null) {
            webView.onPause();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (webView != null) {
            webView.onResume();
        }
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            try {
                // DO NOT clear cache - we need it for offline access
                webView.clearHistory();
                webView.loadUrl("about:blank");
                webView.onPause();
                webView.removeAllViews();
                webView.destroyDrawingCache();
                webView.destroy();
                webView = null;
            } catch (Exception e) {
                Log.e(TAG, "Error destroying WebView: " + e.getMessage(), e);
            }
        }
        super.onDestroy();
    }
}
