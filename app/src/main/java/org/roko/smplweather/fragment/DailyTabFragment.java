package org.roko.smplweather.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import org.roko.smplweather.R;
import org.roko.smplweather.adapter.DailyForecastListViewAdapter;
import org.roko.smplweather.model.DailyListViewItemModel;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import static org.roko.smplweather.Constants.BUNDLE_KEY_FRAGMENT_STATE;

public class DailyTabFragment extends Fragment implements TabFragment<DailyListViewItemModel> {
    private DailyForecastListViewAdapter mDailyForecastAdapter;
    private LinearLayout mContentHolder;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.tab_content, container, false);

        mContentHolder = view.findViewById(R.id.contentHolder);
        mDailyForecastAdapter = new DailyForecastListViewAdapter(getContext());

        return view;
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable(BUNDLE_KEY_FRAGMENT_STATE,
                new FragmentState<>(mDailyForecastAdapter.getItems()));
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (savedInstanceState != null && savedInstanceState.containsKey(BUNDLE_KEY_FRAGMENT_STATE)){
            FragmentState<DailyListViewItemModel> state =
                    (FragmentState<DailyListViewItemModel>) savedInstanceState
                            .getSerializable(BUNDLE_KEY_FRAGMENT_STATE);
            updateContent(state.items);
        }
    }

    @Override
    public void updateContent(List<DailyListViewItemModel> items) {
        if (mDailyForecastAdapter != null && mContentHolder != null) {
            mDailyForecastAdapter.setItems(items);
            mContentHolder.removeAllViews();
            for (int i = 0; i < items.size(); i++) {
                mContentHolder.addView(mDailyForecastAdapter.getView(i, null, mContentHolder), i);
            }
            mDailyForecastAdapter.notifyDataSetChanged();
        }
    }
}
