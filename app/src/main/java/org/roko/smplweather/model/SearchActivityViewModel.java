package org.roko.smplweather.model;

import java.util.List;

public class SearchActivityViewModel {
    private List<ListItemViewModel> items;

    public List<ListItemViewModel> getItems() {
        return items;
    }

    public void setItems(List<ListItemViewModel> items) {
        this.items = items;
    }
}
