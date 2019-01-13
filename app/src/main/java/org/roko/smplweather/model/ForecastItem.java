package org.roko.smplweather.model;

import java.io.Serializable;

public class ForecastItem implements Serializable {
    private String title;
    private String description;
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

    public long getDateTimeUTC() {
        return dateTimeUTC;
    }
}
