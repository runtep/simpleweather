package org.roko.smplweather.model;

import java.io.Serializable;

public class ListViewItemModel implements Serializable {
    private String title;
    private String description;
    private String tempDaily, tempNightly, wind, pressure;

    public ListViewItemModel(String title, String description) {
        this.title = title;
        this.description = description;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
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

    public String getWind() {
        return wind;
    }

    public void setWind(String wind) {
        this.wind = wind;
    }

    public String getPressure() {
        return pressure;
    }

    public void setPressure(String pressure) {
        this.pressure = pressure;
    }
}
