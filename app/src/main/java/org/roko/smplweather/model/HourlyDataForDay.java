package org.roko.smplweather.model;

import java.util.List;

public class HourlyDataForDay {

    public final long dayAsMillis;
    public final List<HourlyDataWrapper> hourlyData;

    public HourlyDataForDay(long dayAsMillis, List<HourlyDataWrapper> hourlyData) {
        this.dayAsMillis = dayAsMillis;
        this.hourlyData = hourlyData;
    }
}
