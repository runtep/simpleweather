package org.roko.smplweather.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import org.roko.smplweather.model.ListViewItemModel;

import java.util.ArrayList;
import java.util.List;

abstract class AbstractListViewAdapter<T extends ListViewItemModel> extends BaseAdapter {
    protected final LayoutInflater mLayoutInflater;
    protected List<T> items = new ArrayList<>();

    public AbstractListViewAdapter(Context context) {
        this.mLayoutInflater = LayoutInflater.from(context);
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
    public abstract View getView(int position, View convertView, ViewGroup parent);
}
