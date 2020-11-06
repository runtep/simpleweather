package org.roko.smplweather.model;

import java.io.Serializable;
import java.util.List;

public class MainActivityViewModel implements Serializable {

    private String actionBarTitle;
    private List<HourlyListViewItemModel> hourlyViewModel;
    private List<DailyForecastItem> dailyItems;
    private long rssProvidedUTC = -1;

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

    public List<HourlyListViewItemModel> getHourlyViewModel() {
        return hourlyViewModel;
    }

    public void setHourlyViewModel(List<HourlyListViewItemModel> hourlyItems) {
        this.hourlyViewModel = hourlyItems;
    }

    public long getRssProvidedUTC() {
        return rssProvidedUTC;
    }

    public void setRssProvidedUTC(long rssProvidedUTC) {
        this.rssProvidedUTC = rssProvidedUTC;
    }
}
