package org.roko.smplweather.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.roko.smplweather.R;
import org.roko.smplweather.adapter.recycler.HeaderItemDecoration;
import org.roko.smplweather.adapter.recycler.HourlyForecastRecyclerViewAdapter;
import org.roko.smplweather.model.HourlyListViewItemModel;

import java.io.Serializable;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import static org.roko.smplweather.Constants.BUNDLE_KEY_FRAGMENT_STATE;

public class HourlyTabFragment extends Fragment implements TabFragment<HourlyListViewItemModel> {
    private HourlyForecastRecyclerViewAdapter mForecastAdapter;
    private RecyclerView mContentHolder;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.tab_content, container, false);

        mContentHolder = view.findViewById(R.id.contentHolder);
        mForecastAdapter = new HourlyForecastRecyclerViewAdapter();
        mContentHolder.setAdapter(mForecastAdapter);
        mContentHolder.addItemDecoration(new HeaderItemDecoration(mForecastAdapter));

        return view;
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable(BUNDLE_KEY_FRAGMENT_STATE,
                new FragmentState<>(mForecastAdapter.getItems()));
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (savedInstanceState != null && savedInstanceState.containsKey(BUNDLE_KEY_FRAGMENT_STATE)) {
            Serializable state = savedInstanceState.getSerializable(BUNDLE_KEY_FRAGMENT_STATE);
            if (state != null && FragmentState.class == state.getClass()) {
                updateContent(((FragmentState) state).items);
            }
        }
    }

    @Override
    public void updateContent(List<HourlyListViewItemModel> items) {
        if (mForecastAdapter != null && mContentHolder != null) {
            mForecastAdapter.setItems(items);
            mForecastAdapter.notifyDataSetChanged();
        }
    }
}
