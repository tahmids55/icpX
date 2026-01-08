package com.icpx.android.ui;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.icpx.android.R;

public class ProblemTabbedActivity extends AppCompatActivity {
    public static final String EXTRA_PROBLEM_URL = "problem_url";
    public static final String EXTRA_PROBLEM_NAME = "problem_name";

    private String problemUrl;
    private String problemName;
    private ViewPager2 viewPager;
    private SurfFragment surfFragment;
    private ProblemWebViewFragment webViewFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        android.util.Log.d("ProblemTabbedActivity", "onCreate started");
        
        setContentView(R.layout.activity_problem_tabbed);

        problemUrl = getIntent().getStringExtra(EXTRA_PROBLEM_URL);
        problemName = getIntent().getStringExtra(EXTRA_PROBLEM_NAME);
        
        android.util.Log.d("ProblemTabbedActivity", "URL: " + problemUrl + ", Name: " + problemName);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(problemName != null ? problemName : "Problem");
        }

        viewPager = findViewById(R.id.viewPager);
        TabLayout tabLayout = findViewById(R.id.tabLayout);

        ProblemPagerAdapter adapter = new ProblemPagerAdapter(this);
        viewPager.setAdapter(adapter);
        
        // Disable swipe gesture between tabs
        viewPager.setUserInputEnabled(false);

        // Setup TabLayout with ViewPager2
        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            switch (position) {
                case 0:
                    tab.setText("WEB");
                    tab.setIcon(R.drawable.ic_web);
                    break;
                case 1:
                    tab.setText("PDF");
                    tab.setIcon(R.drawable.ic_pdf);
                    break;
            }
        }).attach();
        
        android.util.Log.d("ProblemTabbedActivity", "onCreate completed successfully");
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
    
    public void switchToOnlineTab() {
        if (viewPager != null) {
            viewPager.setCurrentItem(1, true);
        }
    }

    public void onPdfDownloaded() {
        if (webViewFragment != null) {
            webViewFragment.onPdfDownloaded();
        }
    }

    private class ProblemPagerAdapter extends FragmentStateAdapter {
        public ProblemPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
            super(fragmentActivity);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            switch (position) {
                case 0:
                    surfFragment = SurfFragment.newInstance(problemUrl);
                    return surfFragment;
                case 1:
                    webViewFragment = ProblemWebViewFragment.newInstance(problemUrl, problemName);
                    return webViewFragment;
                default:
                    return SurfFragment.newInstance(problemUrl);
            }
        }

        @Override
        public int getItemCount() {
            return 2;
        }
    }
}
