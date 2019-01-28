package org.roko.smplweather.model;

public class HourlyListViewDividerModel implements HourlyListViewItemModel {
    private String title;

    public HourlyListViewDividerModel(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }
}
