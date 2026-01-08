package com.icpx.android.ui;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Button;
import android.widget.FrameLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.core.content.FileProvider;
import com.icpx.android.R;
import java.io.File;

public class ProblemPdfFragment extends Fragment {
    private static final String ARG_NAME = "name";
    private String problemName;
    private ProgressBar progressBar;
    private TextView messageText;
    private Button downloadButton;
    private Button viewPdfButton;
    private FrameLayout pdfContainer;
    private WebView pdfWebView;

    public static ProblemPdfFragment newInstance(String name) {
        ProblemPdfFragment fragment = new ProblemPdfFragment();
        Bundle args = new Bundle();
        args.putString(ARG_NAME, name);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            problemName = getArguments().getString(ARG_NAME);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_problem_pdf, container, false);
        
        progressBar = view.findViewById(R.id.progressBar);
        messageText = view.findViewById(R.id.messageText);
        downloadButton = view.findViewById(R.id.downloadButton);
        viewPdfButton = view.findViewById(R.id.viewPdfButton);
        pdfContainer = view.findViewById(R.id.pdfContainer);
        
        loadPdf();
        
        downloadButton.setOnClickListener(v -> {
            // Switch to online tab
            if (getActivity() instanceof ProblemTabbedActivity) {
                ((ProblemTabbedActivity) getActivity()).switchToOnlineTab();
            }
        });
        
        viewPdfButton.setOnClickListener(v -> {
            File pdfFile = getPdfFile();
            if (pdfFile != null && pdfFile.exists()) {
                openPdfWithExternalViewer(pdfFile);
            }
        });
        
        return view;
    }

    private void loadPdf() {
        File pdfFile = getPdfFile();
        
        if (pdfFile != null && pdfFile.exists()) {
            showPdf(pdfFile);
        } else {
            showDownloadPrompt();
        }
    }

    private File getPdfFile() {
        if (problemName == null || problemName.isEmpty()) {
            return null;
        }
        
        String sanitizedName = problemName.replaceAll("[^a-zA-Z0-9.-]", "_");
        File dir = new File(requireContext().getFilesDir(), "problems");
        return new File(dir, sanitizedName + ".pdf");
    }

    private void showPdf(File pdfFile) {
        progressBar.setVisibility(View.GONE);
        downloadButton.setVisibility(View.GONE);
        messageText.setVisibility(View.VISIBLE);
        viewPdfButton.setVisibility(View.VISIBLE);
        pdfContainer.setVisibility(View.GONE);
        
        // Show message with view button
        messageText.setText("PDF saved for offline access.\nTap below to view in PDF reader.");
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
                messageText.setText("No PDF viewer app found.\nPlease install a PDF reader from Play Store.");
            }
        } catch (Exception e) {
            showError("Error opening PDF: " + e.getMessage());
        }
    }

    private void showDownloadPrompt() {
        progressBar.setVisibility(View.GONE);
        downloadButton.setVisibility(View.VISIBLE);
        viewPdfButton.setVisibility(View.GONE);
        messageText.setVisibility(View.VISIBLE);
        pdfContainer.setVisibility(View.GONE);
        messageText.setText("No offline version available.\nSwitch to Online tab to download the PDF.");
    }

    private void showError(String error) {
        progressBar.setVisibility(View.GONE);
        downloadButton.setVisibility(View.VISIBLE);
        viewPdfButton.setVisibility(View.GONE);
        messageText.setVisibility(View.VISIBLE);
        pdfContainer.setVisibility(View.GONE);
        messageText.setText(error);
    }

    public void onPdfDownloaded() {
        // Called when PDF is downloaded from online tab
        loadPdf();
    }
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (pdfWebView != null) {
            pdfWebView.destroy();
        }
    }
}
