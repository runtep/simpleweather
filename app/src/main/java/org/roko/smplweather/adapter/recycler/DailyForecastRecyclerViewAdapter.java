package org.roko.smplweather.adapter.recycler;

import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import org.roko.smplweather.R;
import org.roko.smplweather.model.DailyListViewItemModel;

import androidx.annotation.NonNull;

public class DailyForecastRecyclerViewAdapter extends BaseRecyclerViewAdapter
        <BaseRecyclerViewAdapter.BaseViewHolder<DailyListViewItemModel>, DailyListViewItemModel> {

    public DailyForecastRecyclerViewAdapter() {
        super(R.layout.daily_item);
    }

    @Override
    protected DailyForecastViewHolder createViewHolderInstance(View view) {
        return new DailyForecastViewHolder(view);
    }

    private static class DailyForecastViewHolder extends BaseViewHolder<DailyListViewItemModel> {

        private final TextView tvTempDaily, tvTempNightly, tvWind, tvPressure, tvDivider;

        private DailyForecastViewHolder(@NonNull View itemView) {
            super(itemView);

            tvTempDaily = itemView.findViewById(R.id.tempDaily);
            tvDivider = itemView.findViewById(R.id.tempDivider);
            tvTempNightly = itemView.findViewById(R.id.tempNightly);
            tvWind = itemView.findViewById(R.id.wind);
            tvPressure = itemView.findViewById(R.id.pressure);
        }

        @Override
        public void bind(DailyListViewItemModel item) {
            super.bind(item);

            tvTempDaily.setText(item.getTempDaily());
            tvTempNightly.setText(item.getTempNightly());
            tvWind.setText(item.getWind());
            tvPressure.setText(item.getPressure());

            if (TextUtils.isEmpty(item.getTempDaily()) && TextUtils.isEmpty(item.getTempNightly())) {
                tvDivider.setVisibility(View.GONE);
            }
        }
    }
}
