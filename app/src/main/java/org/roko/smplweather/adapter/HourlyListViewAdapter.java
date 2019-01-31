package org.roko.smplweather.adapter;

import android.content.Context;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.roko.smplweather.R;
import org.roko.smplweather.model.HourlyListViewItemContent;
import org.roko.smplweather.model.HourlyListViewItemModel;
import org.roko.smplweather.model.HourlyListViewItemDivider;
import org.roko.smplweather.model.BasicListViewItemModelImpl;

public class HourlyListViewAdapter extends AbstractListViewAdapter<HourlyListViewItemModel> {

    public HourlyListViewAdapter(Context ctx) {
        super(ctx);
    }

    @Override
    public int getViewTypeCount() {
        return 2;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        HourlyListViewItemModel generic = getItem(position);

        if (generic instanceof HourlyListViewItemDivider) {
            convertView = mLayoutInflater.inflate(R.layout.list_divider, parent, false);

            TextView tv = convertView.findViewById(R.id.listDividerText);
            HourlyListViewItemDivider item = (HourlyListViewItemDivider) generic;
            tv.setText(item.getTitle());
        } else if (generic instanceof HourlyListViewItemContent) {
            convertView = mLayoutInflater.inflate(R.layout.hourly_item, parent, false);

            HourlyListViewItemContent m = (HourlyListViewItemContent) generic;

            TextView tv = convertView.findViewById(R.id.hiTime);
            tv.setText(m.getTime());
            tv = convertView.findViewById(R.id.hiTemperature);
            tv.setText(m.getTemperature());
            tv = convertView.findViewById(R.id.hiDescription);
            setValueHideIfEmpty(tv, m.getDescription());
            tv = convertView.findViewById(R.id.hiWind);
            setValueHideIfEmpty(tv, m.getWind());
            tv = convertView.findViewById(R.id.hiHumidityLevel);
            tv.setText(m.getHumidity());
            tv = convertView.findViewById(R.id.hiPrecipLevel);
            tv.setText(m.getPrecipLevel());
            tv = convertView.findViewById(R.id.hiPrecipProbability);
            tv.setText(m.getPrecipProbability());
        } else if (generic instanceof BasicListViewItemModelImpl) {
            convertView = mLayoutInflater.inflate(R.layout.hourly_item, parent, false);

            BasicListViewItemModelImpl item = (BasicListViewItemModelImpl) generic;

            TextView tvTitle = convertView.findViewById(R.id.itmTitle);
            tvTitle.setText(item.getTitle());
            TextView tvDescription = convertView.findViewById(R.id.itmDescription);
            tvDescription.setText(item.getDescription());
        }

        return convertView;
    }

    private static void setValueHideIfEmpty(TextView tv, String value) {
        if (TextUtils.isEmpty(value)) {
            tv.setVisibility(View.GONE);
        } else {
            tv.setText(value);
        }
    }
}
