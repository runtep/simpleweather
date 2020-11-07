package org.roko.smplweather.model;

import java.util.List;

public class HourlyDataForDay {

    public final String dayStr;
    public final List<HourlyDataWrapper> hourlyData;

    public HourlyDataForDay(String dayStr, List<HourlyDataWrapper> hourlyData) {
        this.dayStr = dayStr;
        this.hourlyData = hourlyData;
    }
}
