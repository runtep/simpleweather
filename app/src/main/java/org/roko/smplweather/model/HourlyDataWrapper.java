package org.roko.smplweather.model;

import android.text.TextUtils;

import com.annimon.stream.Stream;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.roko.smplweather.Constants.ForecastFields.*;

public class HourlyDataWrapper implements Serializable {
    private Map<String, String> map = new HashMap<>(5);

    private static final List<String> ALLOWED_KEYS =
            Arrays.asList(TEMPERATURE_CELSIUS, WIND_DIR_NAME, WIND_SPEED_METERS, HUMIDITY_PERCENT,
                    PRECIP_MILLIMETERS, PRECIP_PROBABILITY_PERCENT, DATE_MILLIS, FORECAST_DESCR);

    public HourlyDataWrapper() {
    }

    public boolean put(String key, String value) {
        if (!ALLOWED_KEYS.contains(key)) {
            return false;
        }
        map.put(key, value);
        return true;
    }

    public void putAll(Map<String, String> map) {
        Stream.of(map).forEach(e -> {
            put(e.getKey(), e.getValue());
        });
    }

    public Long getDateMillis() {
        String d = map.get(DATE_MILLIS);
        if (!TextUtils.isEmpty(d)) {
            return Long.parseLong(d);
        }
        return -1L;
    }

    public String getTempCelsius() {
        return map.get(TEMPERATURE_CELSIUS);
    }

    public String getWindDirName() {
        return map.get(WIND_DIR_NAME);
    }

    public String getWindSpeedMeters() {
        return map.get(WIND_SPEED_METERS);
    }

    public String getHumidityPercent() {
        return map.get(HUMIDITY_PERCENT);
    }

    public String getPrecipitationMillimeters() {
        return map.get(PRECIP_MILLIMETERS);
    }

    public String getPrecipitationProbability() {
        return map.get(PRECIP_PROBABILITY_PERCENT);
    }

    public String getDescription() {
        return map.get(FORECAST_DESCR);
    }
}
