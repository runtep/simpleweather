package org.roko.smplweather.model;

import java.io.Serializable;
import java.util.List;

public class DailyForecastItem implements Serializable {
    private String title, description; // mandatory fields
    private long dateTimeUTC = -1;
    private String tempDaily, tempNightly, wind, pressure; // extra fields
    private List<HourlyDataWrapper> hourlyData;

    public DailyForecastItem(String title, long dateTimeUTC) {
        this.title = title;
        this.dateTimeUTC = dateTimeUTC;
    }

    public DailyForecastItem(String title) {
        this.title = title;
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

    public List<HourlyDataWrapper> getHourlyData() {
        return hourlyData;
    }

    public void setHourlyData(List<HourlyDataWrapper> hourlyData) {
        this.hourlyData = hourlyData;
    }
}
