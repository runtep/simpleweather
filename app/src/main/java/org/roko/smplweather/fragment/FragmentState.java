package org.roko.smplweather.fragment;

import org.roko.smplweather.model.ListViewItemModel;

import java.io.Serializable;
import java.util.List;

public class FragmentState<T extends ListViewItemModel> implements Serializable {

    final List<T> items;

    public FragmentState(List<T> items) {
        this.items = items;
    }
}
