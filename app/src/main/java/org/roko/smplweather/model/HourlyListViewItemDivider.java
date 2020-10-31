package org.roko.smplweather.model;

public class HourlyListViewItemDivider implements HourlyListViewItemModel {

    private String title;

    public HourlyListViewItemDivider(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }
}
