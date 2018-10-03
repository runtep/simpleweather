package org.roko.smplweather.model;

import java.util.List;

public class SearchActivityViewModel {
    private List<SearchResultItemModel> items;

    public List<SearchResultItemModel> getItems() {
        return items;
    }

    public void setItems(List<SearchResultItemModel> items) {
        this.items = items;
    }
}
