package org.roko.smplweather.model;

public class SearchResultItemModel extends ListViewItemModel {
    private String id;

    public SearchResultItemModel(String id, String title, String description) {
        super(title, description);
        this.id = id;
    }

    public String getId() {
        return id;
    }
}
