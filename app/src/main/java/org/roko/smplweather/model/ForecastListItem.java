package org.roko.smplweather.model;

public class ForecastListItem extends ListItemViewModel {
    private long dateTimeUTC = -1;

    public ForecastListItem(String id, String title, String description, long dateTimeUTC) {
        super(id, title, description);
        this.dateTimeUTC = dateTimeUTC;
    }

    public ForecastListItem(String title, String description, long dateTimeUTC) {
        super(title, description);
        this.dateTimeUTC = dateTimeUTC;
    }

    public long getDateTimeUTC() {
        return dateTimeUTC;
    }
}
