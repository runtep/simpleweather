package org.roko.smplweather.model;

import java.io.Serializable;

public class ForecastItem implements Serializable {
    private String title;
    private String description;
    private String tempDaily, tempNightly;
    private long dateTimeUTC = -1;

    public ForecastItem(String title, String description, long dateTimeUTC) {
        this.title = title;
        this.description = description;
        this.dateTimeUTC = dateTimeUTC;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public long getDateTimeUTC() {
        return dateTimeUTC;
    }

    public String getTempDaily() {
        return tempDaily;
    }

    public void setTempDaily(String tempDaily) {
        this.tempDaily = tempDaily;
    }

    public String getTempNightly() {
        return tempNightly;
    }

    public void setTempNightly(String tempNightly) {
        this.tempNightly = tempNightly;
    }
}
