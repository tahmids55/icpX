package com.icpx.android.adapters;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.icpx.android.ui.fragments.StatsAllTimeFragment;
import com.icpx.android.ui.fragments.StatsDailyFragment;

/**
 * Adapter for statistics ViewPager
 */
public class StatsPagerAdapter extends FragmentStateAdapter {

    public StatsPagerAdapter(@NonNull Fragment fragment) {
        super(fragment);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0:
                return new StatsAllTimeFragment();
            case 1:
                return new StatsDailyFragment();
            default:
                return new StatsAllTimeFragment();
        }
    }

    @Override
    public int getItemCount() {
        return 2;
    }
}
