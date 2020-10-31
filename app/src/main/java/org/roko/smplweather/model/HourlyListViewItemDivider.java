package org.roko.smplweather.model;

public class HourlyListViewItemDivider implements HourlyListViewItemModel {

    private final String title;

    public HourlyListViewItemDivider(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }
}
