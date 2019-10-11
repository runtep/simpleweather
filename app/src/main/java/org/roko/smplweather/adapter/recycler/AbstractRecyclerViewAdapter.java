package org.roko.smplweather.adapter.recycler;

import android.view.View;
import android.view.ViewGroup;

import org.roko.smplweather.model.ListViewItemModel;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public abstract class AbstractRecyclerViewAdapter
            <VH extends AbstractRecyclerViewAdapter.AbstractViewHolder<IM>,
                    IM extends ListViewItemModel>
        extends RecyclerView.Adapter<VH>{

    protected List<IM> items = Collections.emptyList();

    public AbstractRecyclerViewAdapter() {
    }

    public void setItems(List<IM> items) {
        Objects.requireNonNull(items);
        this.items = items;
    }

    public List<IM> getItems() {
        return this.items;
    }

    @NonNull
    public abstract VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType);

    public void onBindViewHolder(@NonNull VH holder, int position) {
        holder.bind(items.get(position));
    }

    public int getItemCount() {
        return items.size();
    }

    static abstract class AbstractViewHolder<T extends ListViewItemModel>
            extends RecyclerView.ViewHolder {

        public AbstractViewHolder(@NonNull View itemView) {
            super(itemView);
        }

        public abstract void bind(T item);
    }
}
