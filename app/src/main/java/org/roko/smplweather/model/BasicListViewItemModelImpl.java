package org.roko.smplweather.model;

public abstract class BasicListViewItemModelImpl {

    protected final String title;
    protected final String description;

    public BasicListViewItemModelImpl(String title, String description) {
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
