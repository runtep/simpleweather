package org.roko.smplweather.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class HourlyForecast implements Iterable<HourlyDataForDay> {

    private final Map<String, List<HourlyDataWrapper>> forecast;

    public HourlyForecast(Map<String, List<HourlyDataWrapper>> hourlyByDays) {
        this.forecast = hourlyByDays;
    }

    public boolean isEmpty() {
        return forecast.isEmpty();
    }

    public int getCountOfDays() {
        return forecast.size();
    }

    private Set<String> getDays() {
        return forecast.keySet();
    }

    private List<HourlyDataWrapper> getHourlyDataForDay(String dateTimeStr) {
        if (forecast.containsKey(dateTimeStr)) {
            return new ArrayList<>(forecast.get(dateTimeStr));
        }
        return Collections.emptyList();
    }

    @Override
    public Iterator<HourlyDataForDay> iterator() {
        return new HFIterator(this);
    }

    static class HFIterator implements Iterator<HourlyDataForDay> {

        private final HourlyForecast target;
        private int cursor = 0;
        private final List<String> keys;

        public HFIterator(HourlyForecast target) {
            this.target = target;
            List<String> keys = new ArrayList<>(target.getDays());
            Collections.sort(keys);
            this.keys = keys;
        }

        @Override
        public boolean hasNext() {
            return cursor < keys.size();
        }

        @Override
        public HourlyDataForDay next() {
            String dateTimeStr = keys.get(cursor);
            List<HourlyDataWrapper> data = target.getHourlyDataForDay(dateTimeStr);
            HourlyDataForDay result = new HourlyDataForDay(dateTimeStr, data);
            cursor = cursor + 1;
            return result;
        }
    }
}
