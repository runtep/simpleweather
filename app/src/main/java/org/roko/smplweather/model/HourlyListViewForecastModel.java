package org.roko.smplweather.model;

public class HourlyListViewForecastModel implements HourlyListViewItemModel {
    private String time,
            temperature,
            description,
            wind,
            humidity,
            precipLevel,
            precipProbability;

    public HourlyListViewForecastModel() {}

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getTemperature() {
        return temperature;
    }

    public void setTemperature(String temperature) {
        this.temperature = temperature;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getWind() {
        return wind;
    }

    public void setWind(String wind) {
        this.wind = wind;
    }

    public String getHumidity() {
        return humidity;
    }

    public void setHumidity(String humidity) {
        this.humidity = humidity;
    }

    public String getPrecipLevel() {
        return precipLevel;
    }

    public void setPrecipLevel(String precipLevel) {
        this.precipLevel = precipLevel;
    }

    public String getPrecipProbability() {
        return precipProbability;
    }

    public void setPrecipProbability(String precipProbability) {
        this.precipProbability = precipProbability;
    }
}
