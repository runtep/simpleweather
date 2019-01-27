package org.roko.smplweather.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import org.roko.smplweather.R;
import org.roko.smplweather.model.ListViewItemModel;

import java.util.Collections;
import java.util.List;

public class BasicListViewAdapter<T extends ListViewItemModel> extends BaseAdapter {
    protected final LayoutInflater mLayoutInflater;
    protected int layoutId;
    private List<T> items = Collections.emptyList();

    public BasicListViewAdapter(Context ctx, int layoutId) {
        this.mLayoutInflater = LayoutInflater.from(ctx);
        defineCustomLayout(layoutId);
    }

    public BasicListViewAdapter(Context ctx) {
        this.mLayoutInflater = LayoutInflater.from(ctx);
        defineCustomLayout(R.layout.item);
    }

    protected void defineCustomLayout(int layoutId) {
        this.layoutId = layoutId;
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