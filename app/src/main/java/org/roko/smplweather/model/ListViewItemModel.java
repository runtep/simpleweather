package org.roko.smplweather.model;

import java.io.Serializable;

public class ListViewItemModel implements Serializable {
    protected String _id;
    protected String title;
    protected String description;

    public ListViewItemModel(String _id, String title, String description) {
        this._id = _id;
        this.title = title;
        this.description = description;
    }

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

    public String get_id() {
        return _id;
    }

    public void set_id(String _id) {
        this._id = _id;
    }
}
