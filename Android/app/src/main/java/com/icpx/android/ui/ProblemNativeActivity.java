package com.icpx.android.ui;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;

import com.icpx.android.R;
import com.icpx.android.model.ProblemContent;
import com.icpx.android.utils.ProblemParser;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Activity to display problem statement in native Android views with offline support
 */
public class ProblemNativeActivity extends AppCompatActivity {

    private static final String TAG = "ProblemNativeActivity";
    public static final String EXTRA_PROBLEM_URL = "problem_url";
    public static final String EXTRA_PROBLEM_NAME = "problem_name";

    private ProgressBar progressBar;
    private TextView tvProblemTitle, tvTimeLimit, tvMemoryLimit;
    private TextView tvProblemStatement, tvInputFormat, tvOutputFormat, tvNotes, tvNotesLabel;
    private LinearLayout examplesContainer;

    private File cacheDir;
    private File imagesCacheDir;
    private ExecutorService executorService;
    private Handler mainHandler;
    private OkHttpClient httpClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_problem_native);

        // Setup toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        // Initialize views
        progressBar = findViewById(R.id.progressBar);
        tvProblemTitle = findViewById(R.id.tvProblemTitle);
        tvTimeLimit = findViewById(R.id.tvTimeLimit);
        tvMemoryLimit = findViewById(R.id.tvMemoryLimit);
        tvProblemStatement = findViewById(R.id.tvProblemStatement);
        tvInputFormat = findViewById(R.id.tvInputFormat);
        tvOutputFormat = findViewById(R.id.tvOutputFormat);
        tvNotes = findViewById(R.id.tvNotes);
        tvNotesLabel = findViewById(R.id.tvNotesLabel);
        examplesContainer = findViewById(R.id.examplesContainer);

        // Initialize cache directories
        cacheDir = new File(getCacheDir(), "problem_html_cache");
        imagesCacheDir = new File(getCacheDir(), "problem_images");
        if (!cacheDir.exists()) cacheDir.mkdirs();
        if (!imagesCacheDir.exists()) imagesCacheDir.mkdirs();

        executorService = Executors.newCachedThreadPool();
        mainHandler = new Handler(Looper.getMainLooper());
        httpClient = new OkHttpClient();

        // Get problem URL
        String problemUrl = getIntent().getStringExtra(EXTRA_PROBLEM_URL);
        String problemName = getIntent().getStringExtra(EXTRA_PROBLEM_NAME);

        if (getSupportActionBar() != null && problemName != null) {
            getSupportActionBar().setTitle(problemName);
        }

        if (problemUrl != null) {
            loadProblem(problemUrl);
        }
    }

    private void loadProblem(String url) {
        progressBar.setVisibility(View.VISIBLE);

        executorService.execute(() -> {
            try {
                // Check cache first
                String cachedHtml = loadHtmlFromCache(url);
                
                if (cachedHtml != null) {
                    Log.d(TAG, "Loading from cache");
                    displayProblem(cachedHtml, url);
                } else {
                    Log.d(TAG, "Fetching from network");
                    // Fetch from network
                    Request request = new Request.Builder().url(url).build();
                    Response response = httpClient.newCall(request).execute();
                    
                    if (response.isSuccessful() && response.body() != null) {
                        String html = response.body().string();
                        saveHtmlToCache(url, html);
                        displayProblem(html, url);
                    } else {
                        showError("Failed to load problem");
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error loading problem: " + e.getMessage(), e);
                showError("Error: " + e.getMessage());
            }
        });
    }

    private void displayProblem(String html, String baseUrl) {
        ProblemContent content = ProblemParser.parseProblemHtml(html, baseUrl);

        mainHandler.post(() -> {
            progressBar.setVisibility(View.GONE);

            // Set basic info
            tvProblemTitle.setText(content.getProblemName());
            tvTimeLimit.setText("⏱ " + (content.getTimeLimit() != null ? content.getTimeLimit() : "N/A"));
            tvMemoryLimit.setText("💾 " + (content.getMemoryLimit() != null ? content.getMemoryLimit() : "N/A"));

            // Set problem statement
            tvProblemStatement.setText(content.getProblemStatement() != null ? content.getProblemStatement() : "No description available");

            // Set input/output formats
            tvInputFormat.setText(content.getInputFormat() != null ? content.getInputFormat() : "Not specified");
            tvOutputFormat.setText(content.getOutputFormat() != null ? content.getOutputFormat() : "Not specified");

            // Add examples
            examplesContainer.removeAllViews();
            int exampleNum = 1;
            for (ProblemContent.TestCase example : content.getExamples()) {
                addExampleView(exampleNum++, example.getInput(), example.getOutput());
            }

            // Set notes
            if (content.getNotes() != null && !content.getNotes().isEmpty()) {
                tvNotesLabel.setVisibility(View.VISIBLE);
                tvNotes.setVisibility(View.VISIBLE);
                tvNotes.setText(content.getNotes());
            }

            // Download images
            downloadImages(content.getImageUrls());
            
            Toast.makeText(this, "Problem loaded successfully", Toast.LENGTH_SHORT).show();
        });
    }

    private void addExampleView(int num, String input, String output) {
        View exampleView = LayoutInflater.from(this).inflate(R.layout.item_example, examplesContainer, false);
        
        TextView tvExampleNum = exampleView.findViewById(R.id.tvExampleNum);
        TextView tvInput = exampleView.findViewById(R.id.tvInput);
        TextView tvOutput = exampleView.findViewById(R.id.tvOutput);

        tvExampleNum.setText("Example " + num);
        tvInput.setText(input);
        tvOutput.setText(output);

        examplesContainer.addView(exampleView);
    }

    private void downloadImages(java.util.List<String> imageUrls) {
        for (String imageUrl : imageUrls) {
            executorService.execute(() -> {
                try {
                    File imageFile = getImageCacheFile(imageUrl);
                    
                    if (!imageFile.exists()) {
                        Log.d(TAG, "Downloading image: " + imageUrl);
                        Request request = new Request.Builder().url(imageUrl).build();
                        Response response = httpClient.newCall(request).execute();
                        
                        if (response.isSuccessful() && response.body() != null) {
                            FileOutputStream fos = new FileOutputStream(imageFile);
                            fos.write(response.body().bytes());
                            fos.close();
                            Log.d(TAG, "Image cached: " + imageFile.getName());
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error downloading image: " + e.getMessage(), e);
                }
            });
        }
    }

    private void saveHtmlToCache(String url, String html) {
        try {
            File cacheFile = getCacheFile(url);
            FileOutputStream fos = new FileOutputStream(cacheFile);
            fos.write(html.getBytes("UTF-8"));
            fos.close();
            Log.d(TAG, "HTML cached: " + cacheFile.getName());
        } catch (Exception e) {
            Log.e(TAG, "Error caching HTML: " + e.getMessage(), e);
        }
    }

    private String loadHtmlFromCache(String url) {
        try {
            File cacheFile = getCacheFile(url);
            if (!cacheFile.exists()) return null;

            FileInputStream fis = new FileInputStream(cacheFile);
            byte[] data = new byte[(int) cacheFile.length()];
            fis.read(data);
            fis.close();
            return new String(data, "UTF-8");
        } catch (Exception e) {
            Log.e(TAG, "Error loading HTML from cache: " + e.getMessage(), e);
            return null;
        }
    }

    private File getCacheFile(String url) {
        try {
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
            return new File(cacheDir, "default.html");
        }
    }

    private File getImageCacheFile(String imageUrl) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            byte[] hash = digest.digest(imageUrl.getBytes("UTF-8"));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xFF & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            String extension = imageUrl.substring(imageUrl.lastIndexOf("."));
            return new File(imagesCacheDir, hexString.toString() + extension);
        } catch (Exception e) {
            return new File(imagesCacheDir, "default.png");
        }
    }

    private void showError(String message) {
        mainHandler.post(() -> {
            progressBar.setVisibility(View.GONE);
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        if (executorService != null) {
            executorService.shutdown();
        }
        super.onDestroy();
    }
}
