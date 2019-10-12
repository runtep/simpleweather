package org.roko.smplweather.adapter.recycler;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;

import org.roko.smplweather.R;
import org.roko.smplweather.model.HourlyListViewItemContent;
import org.roko.smplweather.model.HourlyListViewItemDivider;
import org.roko.smplweather.model.HourlyListViewItemModel;

import java.util.Collections;
import java.util.List;

import androidx.annotation.NonNull;

public class HourlyForecastRecyclerViewAdapter extends AbstractRecyclerViewAdapter
        <AbstractRecyclerViewAdapter.AbstractViewHolder<HourlyListViewItemModel>, HourlyListViewItemModel>
        implements HeaderItemDecoration.StickyHeaderInterface {

    private List<Integer> headerIndices = Collections.emptyList();

    public HourlyForecastRecyclerViewAdapter() {
        super();
    }

    @Override
    public void setItems(List<HourlyListViewItemModel> items) {
        super.setItems(items);
        headerIndices = Stream.range(0, items.size() - 1)
                .filter(i -> HourlyListViewItemDivider.class == items.get(i).getClass())
                .collect(Collectors.toList());
    }

    @Override
    public int getItemViewType(int position) {
        HourlyListViewItemModel m = this.items.get(position);
        if (HourlyListViewItemContent.class == m.getClass()) {
            return R.layout.hourly_item;
        } else if (HourlyListViewItemDivider.class == m.getClass()) {
            return R.layout.list_divider;
        }
        return 0;
    }

    @NonNull
    @Override
    public HourlyForecastViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (R.layout.hourly_item == viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.hourly_item, parent, false);
            return new DetailsViewHolder(view);
        } else if (R.layout.list_divider == viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.list_divider, parent, false);
            return new DividerViewHolder(view);
        }
        throw new IllegalArgumentException("Unrecognized type of view");
    }


    @Override
    public int getHeaderPositionForItem(int itemPosition) {
        return Stream.of(headerIndices)
                .filter(itm -> itm <= itemPosition)
                .findLast()
                .orElse(0);
    }

    @Override
    public int getHeaderLayout(int headerPosition) {
        return R.layout.sticky_header;
    }

    @Override
    public void bindHeaderData(View header, int headerPosition) {
        TextView tv = header.findViewById(R.id.listDividerText);
        HourlyListViewItemDivider d = (HourlyListViewItemDivider) this.items.get(headerPosition);
        tv.setText(d.getTitle());
    }

    @Override
    public boolean isHeader(int itemPosition) {
        return headerIndices.contains(itemPosition);
    }

    private static abstract class HourlyForecastViewHolder<T extends HourlyListViewItemModel>
            extends AbstractRecyclerViewAdapter.AbstractViewHolder<T> {

        protected HourlyForecastViewHolder(@NonNull View itemView) {
            super(itemView);
        }

        protected static void setValueOrHideIfEmpty(TextView tv, String value) {
            if (TextUtils.isEmpty(value)) {
                tv.setVisibility(View.GONE);
            } else {
                tv.setVisibility(View.VISIBLE);
                tv.setText(value);
            }
        }
    }

    private static class DetailsViewHolder
            extends HourlyForecastViewHolder<HourlyListViewItemContent> {

        TextView tvTime, tvTemp, tvDesc, tvWind, tvHum, tvPrecip, tvPrecipProb;

        DetailsViewHolder(@NonNull View itemView) {
            super(itemView);

            tvTime = itemView.findViewById(R.id.hiTime);
            tvTemp = itemView.findViewById(R.id.hiTemperature);
            tvDesc = itemView.findViewById(R.id.hiDescription);
            tvWind = itemView.findViewById(R.id.hiWind);
            tvHum = itemView.findViewById(R.id.hiHumidityLevel);
            tvPrecip = itemView.findViewById(R.id.hiPrecipLevel);
            tvPrecipProb = itemView.findViewById(R.id.hiPrecipProbability);
        }

        @Override
        public void bind(HourlyListViewItemContent m) {
            tvTime.setText(m.getTime());
            tvTemp.setText(m.getTemperature());
            setValueOrHideIfEmpty(tvDesc, m.getDescription());
            setValueOrHideIfEmpty(tvWind, m.getWind());
            tvHum.setText(m.getHumidity());
            tvPrecip.setText(m.getPrecipLevel());
            tvPrecipProb .setText(m.getPrecipProbability());
        }
    }

    private static class DividerViewHolder
            extends HourlyForecastViewHolder<HourlyListViewItemDivider> {

        TextView tvDiv;

        DividerViewHolder(@NonNull View itemView) {
            super(itemView);

            tvDiv = itemView.findViewById(R.id.listDividerText);
        }

        @Override
        public void bind(HourlyListViewItemDivider item) {
            tvDiv.setText(item.getTitle());
        }
    }
}
