package com.icpx.android.ui;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.webkit.WebSettingsCompat;
import androidx.webkit.WebViewFeature;

import com.icpx.android.R;

public class ProblemWebViewFragment extends Fragment {
    private static final String ARG_URL = "url";
    private static final String ARG_NAME = "name";

    private String problemUrl;
    private WebView webView;
    private ProgressBar linearProgressBar;
    private ProgressBar progressBar;
    private LinearLayout errorContainer;
    private TextView errorText;
    private Button retryButton;
    private Button openBrowserButton;

    public static ProblemWebViewFragment newInstance(String url, String name) {
        ProblemWebViewFragment fragment = new ProblemWebViewFragment();
        Bundle args = new Bundle();
        args.putString(ARG_URL, url);
        args.putString(ARG_NAME, name);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            problemUrl = getArguments().getString(ARG_URL);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_problem_browser, container, false);
        
        webView = view.findViewById(R.id.webView);
        linearProgressBar = view.findViewById(R.id.linearProgressBar);
        progressBar = view.findViewById(R.id.progressBar);
        errorContainer = view.findViewById(R.id.errorContainer);
        errorText = view.findViewById(R.id.errorText);
        retryButton = view.findViewById(R.id.retryButton);
        openBrowserButton = view.findViewById(R.id.openBrowserButton);
        
        setupWebView();
        setupButtons();
        loadProblem();
        
        return view;
    }

    private void setupWebView() {
        // Configure WebView settings
        android.webkit.WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);
        settings.setSupportZoom(true);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        
        // Fix viewport and scaling issues - zoom in for better readability
        settings.setLayoutAlgorithm(android.webkit.WebSettings.LayoutAlgorithm.TEXT_AUTOSIZING);
        settings.setLoadWithOverviewMode(false);  // Don't shrink to fit
        settings.setUseWideViewPort(false);  // Use actual viewport width
        
        // Increase text zoom for better readability
        settings.setTextZoom(130);  // 130% zoom
        settings.setMinimumFontSize(14);  // Minimum font size
        
        // Set initial scale for better zoom
        webView.setInitialScale(130);  // Start at 130% zoom
        
        // Performance optimizations
        settings.setCacheMode(android.webkit.WebSettings.LOAD_DEFAULT);
        settings.setDatabaseEnabled(true);
        settings.setRenderPriority(android.webkit.WebSettings.RenderPriority.HIGH);
        
        // Enable hardware acceleration for WebView layer
        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        
        // Enable safe browsing if available
        if (WebViewFeature.isFeatureSupported(WebViewFeature.SAFE_BROWSING_ENABLE)) {
            WebSettingsCompat.setSafeBrowsingEnabled(settings, true);
        }
        
        // Force dark mode support if available
        if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK)) {
            int nightModeFlags = getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK;
            if (nightModeFlags == android.content.res.Configuration.UI_MODE_NIGHT_YES) {
                WebSettingsCompat.setForceDark(settings, WebSettingsCompat.FORCE_DARK_ON);
            }
        }
        
        // Set WebViewClient
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                showLoading();
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                hideLoading();
                showWebView();
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                super.onReceivedError(view, request, error);
                if (request.isForMainFrame()) {
                    String errorMessage = "Failed to load problem";
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                        errorMessage = error.getDescription().toString();
                    }
                    showError(errorMessage);
                }
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                // Load URLs within the WebView
                return false;
            }
        });
        
        // Set WebChromeClient for progress updates
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                super.onProgressChanged(view, newProgress);
                if (newProgress < 100) {
                    linearProgressBar.setVisibility(View.VISIBLE);
                    linearProgressBar.setProgress(newProgress);
                } else {
                    linearProgressBar.setVisibility(View.GONE);
                }
            }
        });
    }

    private void setupButtons() {
        retryButton.setOnClickListener(v -> loadProblem());
        
        openBrowserButton.setOnClickListener(v -> {
            if (problemUrl != null && !problemUrl.isEmpty()) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(problemUrl));
                startActivity(browserIntent);
            } else {
                android.widget.Toast.makeText(requireContext(), "No problem URL available", android.widget.Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadProblem() {
        if (problemUrl != null && !problemUrl.isEmpty()) {
            showLoading();
            webView.loadUrl(problemUrl);
        } else {
            showError("No problem URL available");
        }
    }

    private void showLoading() {
        webView.setVisibility(View.GONE);
        errorContainer.setVisibility(View.GONE);
        progressBar.setVisibility(View.VISIBLE);
        linearProgressBar.setVisibility(View.VISIBLE);
        linearProgressBar.setProgress(0);
    }

    private void hideLoading() {
        progressBar.setVisibility(View.GONE);
    }

    private void showWebView() {
        errorContainer.setVisibility(View.GONE);
        webView.setVisibility(View.VISIBLE);
    }

    private void showError(String message) {
        hideLoading();
        webView.setVisibility(View.GONE);
        errorText.setText(message);
        errorContainer.setVisibility(View.VISIBLE);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (webView != null) {
            webView.onPause();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (webView != null) {
            webView.onResume();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (webView != null) {
            webView.destroy();
        }
    }
}
