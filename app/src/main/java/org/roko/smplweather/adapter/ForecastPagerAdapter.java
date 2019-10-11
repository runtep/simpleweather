package org.roko.smplweather.adapter;

import android.content.res.Resources;
import android.util.SparseArray;
import android.view.ViewGroup;

import org.roko.smplweather.R;
import org.roko.smplweather.fragment.DailyTabFragment;
import org.roko.smplweather.fragment.HourlyTabFragment;
import org.roko.smplweather.fragment.TabFragment;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

public class ForecastPagerAdapter extends FragmentPagerAdapter {

    private Resources resources;
    private final int numOfTabs;
    private final SparseArray<Fragment> sparseArray;

    public ForecastPagerAdapter(FragmentManager fm, Resources resources) {
        super(fm);
        this.resources = resources;
        this.numOfTabs = 2;
        sparseArray = new SparseArray<>(2);
    }

    @Override
    public Fragment getItem(int i) {
        switch (i) {
            case 0:
                return new DailyTabFragment();
            case 1:
                return new HourlyTabFragment();
        }
        return null;
    }

    @NonNull
    @Override
    public Object instantiateItem(@NonNull ViewGroup container, int position) {
        Fragment f = (Fragment) super.instantiateItem(container, position);
        sparseArray.put(position, f);
        return f;
    }

    @Override
    public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
        super.destroyItem(container, position, object);
        sparseArray.remove(position);
    }

    public boolean storageEmpty() {
        return sparseArray.size() == 0;
    }

    public <T extends TabFragment> T getStoredFragment(int idx) {
        return (T) sparseArray.get(idx);
    }

    @Override
    public int getCount() {
        return numOfTabs;
    }

    @Nullable
    @Override
    public CharSequence getPageTitle(int position) {
        switch (position) {
            case 0: {
                return resources.getString(R.string.tab_daily);
            }
            case 1: {
                return resources.getString(R.string.tab_hourly);
            }
        }
        return "";
    }
}
