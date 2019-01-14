package org.roko.smplweather.model;

import java.io.Serializable;
import java.util.List;

public class MainActivityViewModel implements Serializable {
    private String actionBarTitle;
    private List<ForecastItem> items;
    private long forecastFromUTC = -1;

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

    public long getForecastFromUTC() {
        return forecastFromUTC;
    }

    public void setForecastFromUTC(long forecastFromUTC) {
        this.forecastFromUTC = forecastFromUTC;
    }
}
