package org.roko.smplweather.model;

import org.roko.smplweather.utils.DateOnly;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class HourlyForecast implements Iterable<HourlyDataForDay> {

    private final List<HourlyDataForDay> data;

    public HourlyForecast(Map<String, List<HourlyDataWrapper>> hourlyByDays) {
        List<String> keys = new ArrayList<>(hourlyByDays.keySet());
        Collections.sort(keys);
        data = new ArrayList<>(keys.size());
        for (String key : keys) {
            List<HourlyDataWrapper> value = hourlyByDays.get(key);
            HourlyDataForDay dataItem = new HourlyDataForDay(new DateOnly(key), value);
            data.add(dataItem);
        }
    }

    public boolean isEmpty() {
        return data.isEmpty();
    }

    @Override
    public Iterator<HourlyDataForDay> iterator() {
        return new HFIterator(this);
    }

    static class HFIterator implements Iterator<HourlyDataForDay> {

        private final HourlyForecast target;
        private int cursor = 0;

        public HFIterator(HourlyForecast target) {
            this.target = target;
        }

        @Override
        public boolean hasNext() {
            return cursor < target.data.size();
        }

        @Override
        public HourlyDataForDay next() {
            HourlyDataForDay result = target.data.get(cursor);
            cursor = cursor + 1;
            return result;
        }
    }
}
