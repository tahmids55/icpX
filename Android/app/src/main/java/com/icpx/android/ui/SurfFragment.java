package com.icpx.android.ui;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.icpx.android.R;

/**
 * Fragment for Surf tab - provides basic web browsing functionality
 */
public class SurfFragment extends Fragment {
    private static final String ARG_URL = "url";
    
    private String initialUrl;
    private WebView webView;
    private SwipeRefreshLayout swipeRefresh;
    private ProgressBar linearProgressBar;
    private ProgressBar progressBar;

    public static SurfFragment newInstance(String url) {
        SurfFragment fragment = new SurfFragment();
        Bundle args = new Bundle();
        args.putString(ARG_URL, url);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            initialUrl = getArguments().getString(ARG_URL);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_surf, container, false);
        
        webView = view.findViewById(R.id.webView);
        swipeRefresh = view.findViewById(R.id.swipeRefresh);
        linearProgressBar = view.findViewById(R.id.linearProgressBar);
        progressBar = view.findViewById(R.id.progressBar);
        
        setupSwipeRefresh();
        setupWebView();
        loadUrl();
        
        return view;
    }

    private void setupSwipeRefresh() {
        swipeRefresh.setColorSchemeResources(
            android.R.color.holo_blue_bright,
            android.R.color.holo_green_light,
            android.R.color.holo_orange_light,
            android.R.color.holo_red_light
        );
        
        swipeRefresh.setOnRefreshListener(() -> {
            webView.reload();
            swipeRefresh.setRefreshing(false);
        });
    }

    private void setupWebView() {
        // Enable JavaScript
        webView.getSettings().setJavaScriptEnabled(true);
        
        // Enable DOM storage
        webView.getSettings().setDomStorageEnabled(true);
        
        // Enable zooming
        webView.getSettings().setSupportZoom(true);
        webView.getSettings().setBuiltInZoomControls(true);
        webView.getSettings().setDisplayZoomControls(false);
        
        // Set viewport and layout for normal desktop view
        webView.getSettings().setUseWideViewPort(true);
        webView.getSettings().setLoadWithOverviewMode(true);
        webView.setInitialScale(1);
        
        // Set user agent
        webView.getSettings().setUserAgentString(
            "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.120 Mobile Safari/537.36"
        );

        // WebViewClient for handling page loading
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                linearProgressBar.setVisibility(View.VISIBLE);
                progressBar.setVisibility(View.VISIBLE);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                linearProgressBar.setVisibility(View.GONE);
                progressBar.setVisibility(View.GONE);
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                // Load all URLs in the WebView
                return false;
            }
        });

        // WebChromeClient for progress bar
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                linearProgressBar.setProgress(newProgress);
                if (newProgress == 100) {
                    linearProgressBar.setVisibility(View.GONE);
                } else {
                    linearProgressBar.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    private void loadUrl() {
        if (initialUrl != null && !initialUrl.isEmpty()) {
            webView.loadUrl(initialUrl);
        } else {
            // Load Codeforces by default
            webView.loadUrl("https://codeforces.com");
        }
    }

    @Override
    public void onDestroyView() {
        if (webView != null) {
            webView.destroy();
        }
        super.onDestroyView();
    }
}
