package org.roko.smplweather;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import org.roko.smplweather.model.ListViewItemModel;

import java.util.Collections;
import java.util.List;

public class MyAdapter<T extends ListViewItemModel> extends BaseAdapter {
    private LayoutInflater mLayoutInflater;
    private List<T> items = Collections.emptyList();

    public MyAdapter(Context ctx) {
        this.mLayoutInflater = LayoutInflater.from(ctx);
    }

    public void setItems(List<T> items) {
        this.items = items;
    }

    @Override
    public int getCount() {
        return items.size();
    }

    @Override
    public T getItem(int position) {
        if (items.size() <= position) {
            throw new IllegalArgumentException("Index \"" + position + "\" is out of range");
        }
        return items.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = mLayoutInflater.inflate(R.layout.card, parent, false);
        }

        ListViewItemModel item = items.get(position);

        TextView tvTitle = convertView.findViewById(R.id.itmTitle);
        tvTitle.setText(item.getTitle());
        TextView tvDescription = convertView.findViewById(R.id.itmDescription);
        tvDescription.setText(item.getDescription());
        TextView tvTempDaily = convertView.findViewById(R.id.tempDaily);
        tvTempDaily.setText(item.getTempDaily());
        TextView tvTempNightly = convertView.findViewById(R.id.tempNightly);
        tvTempNightly.setText(item.getTempNightly());

        if (TextUtils.isEmpty(item.getTempDaily()) && TextUtils.isEmpty(item.getTempNightly())) {
            TextView tvDivider = convertView.findViewById(R.id.tempDivider);
            tvDivider.setVisibility(View.GONE);
        }

        return convertView;
    }


}
