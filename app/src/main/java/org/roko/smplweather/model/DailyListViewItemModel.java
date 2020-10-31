package org.roko.smplweather.model;

public interface DailyListViewItemModel extends BasicListViewItemModel {

    String getTempDaily();

    String getTempNightly();

    String getWind();

    String getPressure();
}
