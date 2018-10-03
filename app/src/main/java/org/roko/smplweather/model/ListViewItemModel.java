package org.roko.smplweather.model;

import java.io.Serializable;

public class ListViewItemModel implements Serializable {
    private String title;
    private String description;

    public ListViewItemModel(String title, String description) {
        this.title = title;
        this.description = description;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }
}
