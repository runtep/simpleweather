package org.roko.smplweather.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class HourlyForecast {

    private final Map<Long, List<HourlyDataWrapper>> forecast;

    public HourlyForecast(Map<Long, List<HourlyDataWrapper>> hourlyByDays) {
        this.forecast = hourlyByDays;
    }

    public boolean isEmpty() {
        return forecast.isEmpty();
    }

    public int getCountOfDays() {
        return forecast.size();
    }

    public Set<Long> getDaysAsMillis() {
        return forecast.keySet();
    }

    public List<HourlyDataWrapper> getHourlyDataForDay(long dateTime) {
        if (forecast.containsKey(dateTime)) {
            return new ArrayList<>(forecast.get(dateTime));
        }
        return Collections.emptyList();
    }
}
