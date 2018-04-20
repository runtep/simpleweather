package org.roko.smplweather.model;

import java.io.Serializable;
import java.util.List;

public class MainActivityVewModel implements Serializable {
    private String actionBarTitle;
    private List<ListItemViewModel> items;
    private String footer;

    public String getActionBarTitle() {
        return actionBarTitle;
    }

    public void setActionBarTitle(String actionBarTitle) {
        this.actionBarTitle = actionBarTitle;
    }

    public List<ListItemViewModel> getItems() {
        return items;
    }

    public void setItems(List<ListItemViewModel> items) {
        this.items = items;
    }

    public String getFooter() {
        return footer;
    }

    public void setFooter(String footer) {
        this.footer = footer;
    }
}
