package org.roko.smplweather.model;

import java.io.Serializable;
import java.util.List;

public class MainActivityViewModel implements Serializable {
    private String actionBarTitle;
    private List<HourlyListViewItemModel> hourlyDataItems;
    private List<DailyForecastItem> dailyItems;
    private long forecastFromUTC = -1;

    public String getActionBarTitle() {
        return actionBarTitle;
    }

    public void setActionBarTitle(String actionBarTitle) {
        this.actionBarTitle = actionBarTitle;
    }

    public List<DailyForecastItem> getDailyItems() {
        return dailyItems;
    }

    public void setDailyItems(List<DailyForecastItem> dailyItems) {
        this.dailyItems = dailyItems;
    }

    public List<HourlyListViewItemModel> getHourlyDataItems() {
        return hourlyDataItems;
    }

    public void setHourlyDataItems(List<HourlyListViewItemModel> hourlyDataItems) {
        this.hourlyDataItems = hourlyDataItems;
    }

    public long getForecastFromUTC() {
        return forecastFromUTC;
    }

    public void setForecastFromUTC(long forecastFromUTC) {
        this.forecastFromUTC = forecastFromUTC;
    }
}
