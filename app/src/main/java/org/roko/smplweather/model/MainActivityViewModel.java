package org.roko.smplweather.model;

import java.io.Serializable;
import java.util.List;

public class MainActivityViewModel implements Serializable {
    private String actionBarTitle;
    private List<ForecastItem> items;
    private long lastUpdateUTC = -1;

    public String getActionBarTitle() {
        return actionBarTitle;
    }

    public void setActionBarTitle(String actionBarTitle) {
        this.actionBarTitle = actionBarTitle;
    }

    public List<ForecastItem> getItems() {
        return items;
    }

    public void setItems(List<ForecastItem> items) {
        this.items = items;
    }

    public long getLastUpdateUTC() {
        return lastUpdateUTC;
    }

    public void setLastUpdateUTC(long lastUpdateMs) {
        this.lastUpdateUTC = lastUpdateMs;
    }
}
