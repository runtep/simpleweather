package org.roko.smplweather.adapter;

import android.content.Context;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import org.roko.smplweather.R;
import org.roko.smplweather.model.DailyForecastListViewItemModel;

public class DailyForecastListViewAdapter extends BasicListViewAdapter<DailyForecastListViewItemModel> {
    public DailyForecastListViewAdapter(Context ctx) {
        super(ctx, R.layout.card);
    }

    @Override
    protected void propagateItemDataToView(DailyForecastListViewItemModel item, View convertView) {
        super.propagateItemDataToView(item, convertView);

        TextView tvTempDaily = convertView.findViewById(R.id.tempDaily);
        tvTempDaily.setText(item.getTempDaily());
        TextView tvTempNightly = convertView.findViewById(R.id.tempNightly);
        tvTempNightly.setText(item.getTempNightly());
        TextView tvWind = convertView.findViewById(R.id.wind);
        tvWind.setText(item.getWind());
        TextView tvPressure = convertView.findViewById(R.id.pressure);
        tvPressure.setText(item.getPressure());

        if (TextUtils.isEmpty(item.getTempDaily()) && TextUtils.isEmpty(item.getTempNightly())) {
            TextView tvDivider = convertView.findViewById(R.id.tempDivider);
            tvDivider.setVisibility(View.GONE);
        }
    }
}
