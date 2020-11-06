package org.roko.smplweather.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class HourlyForecast implements Iterable<HourlyDataForDay> {

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

    private Set<Long> getDaysAsMillis() {
        return forecast.keySet();
    }

    private List<HourlyDataWrapper> getHourlyDataForDay(long dateTime) {
        if (forecast.containsKey(dateTime)) {
            return new ArrayList<>(forecast.get(dateTime));
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
        private final List<Long> keys;

        public HFIterator(HourlyForecast target) {
            this.target = target;
            List<Long> keys = new ArrayList<>(target.getDaysAsMillis());
            Collections.sort(keys);
            this.keys = keys;
        }

        @Override
        public boolean hasNext() {
            return cursor < keys.size();
        }

        @Override
        public HourlyDataForDay next() {
            Long dayAsMillis = keys.get(cursor);
            List<HourlyDataWrapper> data = target.getHourlyDataForDay(dayAsMillis);
            HourlyDataForDay result = new HourlyDataForDay(dayAsMillis, data);
            cursor = cursor + 1;
            return result;
        }
    }
}
