package org.roko.smplweather.model;

import java.io.Serializable;
import java.util.List;

public class MainActivityViewModel implements Serializable {
    private String actionBarTitle;
    private List<ListItemViewModel> items;
    private long lastUpdateUTC = -1;

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

    public long getLastUpdateUTC() {
        return lastUpdateUTC;
    }

    public void setLastUpdateUTC(long lastUpdateMs) {
        this.lastUpdateUTC = lastUpdateMs;
    }
}
