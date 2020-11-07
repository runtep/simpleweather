package org.roko.smplweather.model;

import org.roko.smplweather.utils.DateOnly;

import java.util.List;

public class HourlyDataForDay {

    public final DateOnly day;
    public final List<HourlyDataWrapper> hourlyData;

    public HourlyDataForDay(DateOnly day, List<HourlyDataWrapper> hourlyData) {
        this.day = day;
        this.hourlyData = hourlyData;
    }
}
