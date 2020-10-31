package org.roko.smplweather.fragment;

import org.roko.smplweather.model.ListViewItemModel;

import java.util.List;

public interface TabFragment<T extends ListViewItemModel> {

    void updateContent(List<T> items);
}
