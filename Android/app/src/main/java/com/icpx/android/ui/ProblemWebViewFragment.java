package com.icpx.android.ui;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintManager;
import android.util.Base64;
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
import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.webkit.WebSettingsCompat;
import androidx.webkit.WebViewFeature;
import androidx.core.content.FileProvider;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import com.icpx.android.R;

import java.io.File;
import java.io.FileOutputStream;

public class ProblemWebViewFragment extends Fragment {
    private static final String ARG_URL = "url";
    private static final String ARG_NAME = "name";

    private String problemUrl;
    private String problemName;
    private WebView webView;
    private androidx.swiperefreshlayout.widget.SwipeRefreshLayout swipeRefresh;
    private ProgressBar linearProgressBar;
    private ProgressBar progressBar;
    private LinearLayout errorContainer;
    private TextView errorText;
    private Button retryButton;
    private Button openBrowserButton;
    private LinearLayout pdfProgressContainer;
    private TextView pdfStatusText;
    private ProgressBar pdfProgressBar;
    private TextView pdfPercentageText;
    private FloatingActionButton fabOpenPdf;

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
            problemName = getArguments().getString(ARG_NAME);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_problem_browser, container, false);
        
        webView = view.findViewById(R.id.webView);
        swipeRefresh = view.findViewById(R.id.swipeRefresh);
        linearProgressBar = view.findViewById(R.id.linearProgressBar);
        progressBar = view.findViewById(R.id.progressBar);
        errorContainer = view.findViewById(R.id.errorContainer);
        errorText = view.findViewById(R.id.errorText);
        retryButton = view.findViewById(R.id.retryButton);
        openBrowserButton = view.findViewById(R.id.openBrowserButton);
        pdfProgressContainer = view.findViewById(R.id.pdfProgressContainer);
        pdfStatusText = view.findViewById(R.id.pdfStatusText);
        pdfProgressBar = view.findViewById(R.id.pdfProgressBar);
        pdfPercentageText = view.findViewById(R.id.pdfPercentageText);
        fabOpenPdf = view.findViewById(R.id.fabOpenPdf);
        
        setupSwipeRefresh();
        setupWebView();
        setupButtons();
        setupFab();
        loadProblem();
        
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
        
        // Match Chrome mobile behavior for Codeforces
        settings.setLayoutAlgorithm(android.webkit.WebSettings.LayoutAlgorithm.TEXT_AUTOSIZING);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        
        // Set Chrome mobile user agent for proper mobile site rendering
        String userAgent = "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.6099.230 Mobile Safari/537.36";
        settings.setUserAgentString(userAgent);
        
        // Default text size (Chrome default)
        settings.setTextZoom(100);
        
        // No initial scale - let site determine viewport
        webView.setInitialScale(0);
        
        // Performance optimizations
        settings.setCacheMode(android.webkit.WebSettings.LOAD_DEFAULT);
        settings.setDatabaseEnabled(true);
        settings.setRenderPriority(android.webkit.WebSettings.RenderPriority.HIGH);
        
        // Enable hardware acceleration for WebView layer
        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        
        // Add JavaScript interface to receive PDF data
        webView.addJavascriptInterface(new Object() {
            @android.webkit.JavascriptInterface
            public void updateProgress(int percentage, String status) {
                requireActivity().runOnUiThread(() -> {
                    pdfProgressBar.setProgress(percentage);
                    pdfPercentageText.setText(percentage + "%");
                    pdfStatusText.setText(status);
                });
            }
            
            @android.webkit.JavascriptInterface
            public void receivePdfData(String base64Data, String filename) {
                new Thread(() -> {
                    try {
                        requireActivity().runOnUiThread(() -> {
                            pdfStatusText.setText("Saving PDF...");
                            pdfProgressBar.setProgress(95);
                            pdfPercentageText.setText("95%");
                        });
                        // Decode base64 PDF data
                        byte[] pdfData = Base64.decode(base64Data.split(",")[1], Base64.DEFAULT);
                        
                        // Save to app's internal storage
                        File dir = new File(requireContext().getFilesDir(), "problems");
                        if (!dir.exists()) {
                            dir.mkdirs();
                        }
                        
                        String sanitizedName = (problemName != null ? problemName : "problem").replaceAll("[^a-zA-Z0-9.-]", "_");
                        File pdfFile = new File(dir, sanitizedName + ".pdf");
                        
                        FileOutputStream fos = new FileOutputStream(pdfFile);
                        fos.write(pdfData);
                        fos.close();
                        
                        // Notify parent activity
                        requireActivity().runOnUiThread(() -> {
                            pdfProgressBar.setProgress(100);
                            pdfPercentageText.setText("100%");
                            pdfStatusText.setText("PDF saved successfully!");
                            
                            // Hide progress after 2 seconds
                            new android.os.Handler().postDelayed(() -> {
                                pdfProgressContainer.setVisibility(View.GONE);
                            }, 2000);
                            
                            if (getActivity() instanceof ProblemTabbedActivity) {
                                ((ProblemTabbedActivity) getActivity()).onPdfDownloaded();
                            }
                        });
                        
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }).start();
            }
        }, "AndroidPDF");
        
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
                
                // Check if PDF already exists
                File pdfFile = getPdfFile();
                if (pdfFile != null && pdfFile.exists()) {
                    // PDF already exists, show FAB and update status
                    requireActivity().runOnUiThread(() -> {
                        pdfProgressContainer.setVisibility(View.VISIBLE);
                        pdfStatusText.setText("PDF already generated");
                        pdfProgressBar.setProgress(100);
                        pdfPercentageText.setText("100%");
                        fabOpenPdf.setVisibility(View.VISIBLE);
                        
                        // Hide progress after 2 seconds
                        new android.os.Handler().postDelayed(() -> {
                            pdfProgressContainer.setVisibility(View.GONE);
                        }, 2000);
                    });
                } else {
                    // Auto-download PDF after page loads
                    new android.os.Handler().postDelayed(() -> {
                        autoDownloadPdf();
                    }, 2000); // Wait 2 seconds for page to fully render
                }
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
    
    private void setupFab() {
        fabOpenPdf.setOnClickListener(v -> {
            File pdfFile = getPdfFile();
            if (pdfFile != null && pdfFile.exists()) {
                openPdfWithExternalViewer(pdfFile);
            } else {
                android.widget.Toast.makeText(requireContext(), "PDF not yet available", android.widget.Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private File getPdfFile() {
        if (problemName == null || problemName.isEmpty()) {
            return null;
        }
        String sanitizedName = problemName.replaceAll("[^a-zA-Z0-9.-]", "_");
        File dir = new File(requireContext().getFilesDir(), "problems");
        return new File(dir, sanitizedName + ".pdf");
    }
    
    public void onPdfDownloaded() {
        File pdfFile = getPdfFile();
        if (pdfFile != null && pdfFile.exists()) {
            fabOpenPdf.setVisibility(View.VISIBLE);
        }
    }
    
    private void openPdfWithExternalViewer(File pdfFile) {
        try {
            Uri pdfUri = FileProvider.getUriForFile(
                requireContext(),
                requireContext().getPackageName() + ".provider",
                pdfFile
            );
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(pdfUri, "application/pdf");
            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
            if (intent.resolveActivity(requireContext().getPackageManager()) != null) {
                startActivity(intent);
            } else {
                android.widget.Toast.makeText(requireContext(), "No PDF viewer found", android.widget.Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            android.widget.Toast.makeText(requireContext(), "Error opening PDF", android.widget.Toast.LENGTH_SHORT).show();
        }
    }
    
    private void autoDownloadPdf() {
        if (webView == null) {
            return;
        }
        
        // Show PDF progress container
        requireActivity().runOnUiThread(() -> {
            pdfProgressContainer.setVisibility(View.VISIBLE);
            pdfStatusText.setText("Preparing PDF...");
            pdfProgressBar.setProgress(0);
            pdfPercentageText.setText("0%");
        });
        
        // Execute customized JavaScript to generate and send PDF data
        String javascript = "javascript:!async function(){" +
            "AndroidPDF.updateProgress(5,'Loading PDF library...');" +
            "if(typeof html2pdf=='undefined'){" +
            "let e=document.createElement('script');" +
            "e.src='https://cdnjs.cloudflare.com/ajax/libs/html2pdf.js/0.10.1/html2pdf.bundle.min.js'," +
            "document.body.appendChild(e)," +
            "await new Promise(t=>e.onload=t)" +
            "}" +
            "AndroidPDF.updateProgress(15,'Preparing content...');" +
            "AndroidPDF.updateProgress(25,'Cleaning content...');" +
            "['#header','#footer','#sidebar','.sidebox','.menu-box','.second-level-menu-list'," +
            "'.roundbox.datatable','.lang-chooser','.problem-action-links','#problem_stats'," +
            "'.contest-name','.custom-links-CodeforcesHelp','.tags-box'," +
            "'.ttypography > .header'].forEach(e=>{" +
            "document.querySelectorAll(e).forEach(e=>e.remove())" +
            "});" +
            "AndroidPDF.updateProgress(40,'Applying styles...');" +
            "let t=document.querySelector('.problemindexholder');" +
            "if(!t){" +
            "console.error('Could not find problem content');" +
            "return" +
            "}" +
            "let o=`body {background: white !important; margin: 0; padding: 0;} " +
            ".problemindexholder {margin: 0 !important; padding: 0 !important;" +
            "width: 100% !important; max-width: 100% !important;background: white; color: #000;" +
            "font-family: 'Times New Roman', serif; font-size: 10.5pt;line-height: 1.3;}" +
            "img {max-width: 100%;height: auto;page-break-inside: avoid;} " +
            ".sample-test {page-break-inside: avoid;display: flex;flex-direction: row;gap: 10px;} " +
            ".input, .output { flex: 1; }" +
            "pre {page-break-inside: avoid;background: #f4f4f4;padding: 6px;margin: 4px 0;" +
            "border: 1px solid #ddd;font-family: 'Courier New', monospace;font-size: 9.5pt;" +
            "white-space: pre-wrap;word-wrap: break-word;} " +
            "h3, .section-title {page-break-after: avoid;}`;" +
            "let r=document.createElement('style');" +
            "r.type='text/css',r.innerText=o,document.head.appendChild(r)," +
            "document.body.innerHTML='',document.body.appendChild(t);" +
            "let a=document.querySelector('.title')?.innerText||'Codeforces Problem'," +
            "i=a.replace(/^[A-Z0-9]+\\.\\s*/,'').trim()," +
            "n=i.replace(/[<>:\"/\\\\|?*\\x00-\\x1F]/g,'_').trim()+'.pdf';" +
            "AndroidPDF.updateProgress(60,'Generating PDF...');" +
            "const pdf=await html2pdf().set({" +
            "margin:7,filename:n," +
            "image:{type:'jpeg',quality:.98}," +
            "html2canvas:{scale:2,useCORS:!0,scrollY:0,backgroundColor:'#ffffff'}," +
            "jsPDF:{unit:'mm',format:'a4',orientation:'portrait'}," +
            "pagebreak:{mode:['css','legacy']}" +
            "}).from(document.body).outputPdf('datauristring');" +
            "AndroidPDF.updateProgress(90,'Finalizing...');" +
            "AndroidPDF.receivePdfData(pdf,n);" +
            "}();";        
        webView.evaluateJavascript(javascript, null);
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
