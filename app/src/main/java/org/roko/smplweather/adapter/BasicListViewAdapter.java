package org.roko.smplweather.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import org.roko.smplweather.Constants;
import org.roko.smplweather.R;
import org.roko.smplweather.model.ListViewItemModel;

import java.util.Collections;
import java.util.List;

public class BasicListViewAdapter<T extends ListViewItemModel> extends AbstractListViewAdapter<T> {
    protected final int layoutId;

    public BasicListViewAdapter(Context ctx) {
        super(ctx);
        this.layoutId = R.layout.item;
    }

    public BasicListViewAdapter(Context ctx, int layoutId) {
        super(ctx);
        this.layoutId = layoutId;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = mLayoutInflater.inflate(layoutId, parent, false);
        }

        T item = getItem(position);

        propagateItemDataToView(item, convertView);

        return convertView;
    }

    protected void propagateItemDataToView(T item, View convertView) {
        TextView tvTitle = convertView.findViewById(R.id.itmTitle);
        tvTitle.setText(item.getTitle());
        TextView tvDescription = convertView.findViewById(R.id.itmDescription);
        tvDescription.setText(item.getDescription());
    }
}