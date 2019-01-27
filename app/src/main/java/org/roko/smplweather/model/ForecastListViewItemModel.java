package org.roko.smplweather.model;

public class ForecastListViewItemModel extends ListViewItemModel {
    private String tempDaily, tempNightly, wind, pressure;

    public ForecastListViewItemModel(String title, String description) {
        super(title, description);
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
