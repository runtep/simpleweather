package org.roko.smplweather.adapter.recycler;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.roko.smplweather.R;
import org.roko.smplweather.model.BasicListViewItemModel;

import androidx.annotation.NonNull;

public abstract class BaseRecyclerViewAdapter
            <VH extends BaseRecyclerViewAdapter.BaseViewHolder<IM>,
                    IM extends BasicListViewItemModel>
        extends AbstractRecyclerViewAdapter<VH, IM> {

    protected final int layoutId;

    public BaseRecyclerViewAdapter(int layoutId) {
        this.layoutId = layoutId;
    }

    public BaseRecyclerViewAdapter() {
        this.layoutId = R.layout.item;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(layoutId, parent, false);
        return createViewHolderInstance(view);
    }

    protected abstract VH createViewHolderInstance(View view);

    static class BaseViewHolder<T extends BasicListViewItemModel> extends AbstractViewHolder<T> {

        protected final TextView tvTitle, tvDescription;

        BaseViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.itmTitle);
            tvDescription = itemView.findViewById(R.id.itmDescription);
        }

        @Override
        public void bind(T item) {
            tvTitle.setText(item.getTitle());
            tvDescription.setText(item.getDescription());
        }
    }
}
