package org.roko.smplweather.model;

public class ListItemViewModel {
    String title;
    String description;

    public ListItemViewModel(String title, String description) {
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
