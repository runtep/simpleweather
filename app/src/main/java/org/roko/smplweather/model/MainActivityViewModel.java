package org.roko.smplweather.model;

import java.io.Serializable;
import java.util.List;

public class MainActivityViewModel implements Serializable {
    private String actionBarTitle;
    private List<ForecastListItem> items;
    private long lastUpdateUTC = -1;

    public String getActionBarTitle() {
        return actionBarTitle;
    }

    public void setActionBarTitle(String actionBarTitle) {
        this.actionBarTitle = actionBarTitle;
    }

    public List<ForecastListItem> getItems() {
        return items;
    }

    public void setItems(List<ForecastListItem> items) {
        this.items = items;
    }

    public long getLastUpdateUTC() {
        return lastUpdateUTC;
    }

    public void setLastUpdateUTC(long lastUpdateMs) {
        this.lastUpdateUTC = lastUpdateMs;
    }
}
