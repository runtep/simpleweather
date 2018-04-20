package org.roko.smplweather.model;

import java.io.Serializable;

public class ListItemViewModel implements Serializable {
    private String id;
    private String title;
    private String description;

    public ListItemViewModel(String id, String title, String description) {
        this.id = id;
        this.title = title;
        this.description = description;
    }

    public ListItemViewModel(String title, String description) {
        this.title = title;
        this.description = description;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }
}
