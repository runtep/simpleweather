package org.roko.smplweather.utils;

import android.text.TextUtils;
import android.util.Log;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;

import org.roko.smplweather.model.HourlyDataWrapper;
import org.roko.smplweather.model.HourlyForecast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.roko.smplweather.Constants.ForecastFields.*;
import static org.roko.smplweather.Constants.PayloadArrayNames.*;

public class HourlyForecastParser {

    private HourlyForecastParser() {
    }

    public static HourlyForecast parse(String payload) {

        // list of structures
        // [
        //   [date: "today_16:00", value: 1],
        //   [date: "today_18:00", value: 2],
        //   ...
        // ]
        List<Map<String, String>> temperatureValues = parseArray(payload, ARR_TEMPERATURE, TEMPERATURE_CELSIUS);
        List<Map<String, String>> windDirNames = parseArray(payload, ARR_WIND_DIR_NAME, WIND_DIR_NAME);
        List<Map<String, String>> windSpeedValues = parseArray(payload, ARR_WIND_SPEED, WIND_SPEED_METERS);
        List<Map<String, String>> humidityValues = parseArray(payload, ARR_HUMIDITY, HUMIDITY_PERCENT);
        List<Map<String, String>> precipValues = parseArray(payload, ARR_PRECIP_VAL, PRECIP_MILLIMETERS);
        List<Map<String, String>> precipProbabilities = parseArray(payload, ARR_PRECIP_VER, PRECIP_PROBABILITY_PERCENT);
        List<Map<String, String>> descriptions = parseArray(payload, ARR_PHENOMENON_NAME, FORECAST_DESCR);

        // map transformation - date value is extracted and used as a key
        // [
        //   "today_16:00": [date: "today_16:00", value: 1],
        //   "today_18:00": [date: "today_18:00", value: 2],
        //   ...
        // ]
        Map<String, Map<String, String>> target = Stream.of(temperatureValues).collect(
            Collectors.toMap(
                (Map<String, String> e) -> e.get(DATE_STRING),
                (Map<String, String> e) -> {
                    Map<String, String> forecastStub = new HashMap<>();
                    forecastStub.put(DATE_STRING, e.get(DATE_STRING));
                    forecastStub.put(TEMPERATURE_CELSIUS, e.get(TEMPERATURE_CELSIUS));
                    return forecastStub;
                },
                () -> new LinkedHashMap<>()
            )
        );

        // Replace digit with letter for wind direction
        Stream.of(windDirNames).forEach(e -> {
            String dir = e.get(WIND_DIR_NAME);
            if (!TextUtils.isEmpty(dir) && dir.endsWith("3" /* digit */)) {
                e.put(WIND_DIR_NAME, dir.replace('3', 'З' /* letter */));
            }
        });

        // supply each value entry with extra data
        // [
        //   "today_16:00": [date: "today_16:00", value: 1, anotherValue: 9, ...],
        //   "today_18:00": [date: "today_18:00", value: 2, anotherValue: 10, ...],
        //   ...
        // ]
        supplyMap(target, windDirNames, WIND_DIR_NAME);
        supplyMap(target, windSpeedValues, WIND_SPEED_METERS);
        supplyMap(target, humidityValues, HUMIDITY_PERCENT);
        supplyMap(target, precipValues, PRECIP_MILLIMETERS);
        supplyMap(target, precipProbabilities, PRECIP_PROBABILITY_PERCENT);
        supplyMap(target, descriptions, FORECAST_DESCR);

        Map<String, HourlyDataWrapper> hourlyData = new LinkedHashMap<>();
        Stream.of(target).forEach(entry -> {
            HourlyDataWrapper hdw = new HourlyDataWrapper();
            hdw.putAll(entry.getValue());
            hourlyData.put(entry.getKey(), hdw);
        });
        // regroup by days
        // [
        //   "today": [
        //     [date: "today_16:00", value: 1, anotherValue: 9],
        //     [date: "today_18:00", value: 2, anotherValue: 10],
        //     ...
        //   ],
        //   "tomorrow": [ ... ]
        // ]
        HourlyForecast hf = new HourlyForecast(groupByDays(hourlyData));

        return hf;
    }

    private static Map<String, List<HourlyDataWrapper>> groupByDays(Map<String, HourlyDataWrapper> forecast) {
        Map<String, List<HourlyDataWrapper>> map = new LinkedHashMap<>();
        List<HourlyDataWrapper> membersOfOneDay = new ArrayList<>();
        DateTime dtCurrentItem;
        DateTime dtPrevItem = null;
        List<String> keys = new ArrayList<>(forecast.keySet());
        Collections.sort(keys); // ascending order is important
        for (String key : keys) {
            dtCurrentItem = new DateTime(key);
            HourlyDataWrapper currentItem = forecast.get(key);
            if (dtPrevItem == null) {
                dtPrevItem = new DateTime(key);
            } else {
                if (dtPrevItem.before(dtCurrentItem)) {
                    // dtPrevItem holds dateTime of a last item within a single day,
                    // so we take it's date-only value (crop time) and use as a key in result map
                    map.put(dtPrevItem.dateOnlyString(), membersOfOneDay);
                    membersOfOneDay = new ArrayList<>();
                }
                dtPrevItem = new DateTime(key);
            }
            membersOfOneDay.add(currentItem);
        }
        if (dtPrevItem != null && !membersOfOneDay.isEmpty()) {
            map.put(dtPrevItem.dateOnlyString(), membersOfOneDay);
        }

        return map;
    }

    private static void supplyMap(final Map<String, Map<String, String>> map,
                                  List<Map<String, String>> list,
                                  String targetFieldName) {
        Stream.of(list).forEach((Map<String, String> e) -> {
            String dateStr = e.get(DATE_STRING);
            Map<String, String> forecastStub = map.get(dateStr);
            if (forecastStub != null) {
                forecastStub.put(targetFieldName, e.get(targetFieldName));
            }
        });
    }

    private static List<Map<String, String>> parseArray(String payload, final String arrayName, final String valueAlias) {

        List<Map<String, String>> resultItems = new ArrayList<>();

        Pattern pArray = Pattern.compile(";\\s+" + arrayName + "\\s*=\\s*\\[(.+?)\\];");
        Pattern pXY = Pattern.compile("x:\\s+(.+?),\\s+y:\\s+\"?([-\\d.]+|null|[А-Я\\d-]{1,3}|[А-Яа-я,. ]+)\"?,");
        Pattern pJSDate = Pattern.compile("Date.UTC\\((\\d+?),(\\d+?),(\\d+?),(\\d+?)\\)");

        Matcher arrayMatcher = pArray.matcher(payload.replaceAll("[\r\n]", ""));

        if (arrayMatcher.find()) {
            Log.i("parser", "match found for " + arrayName);
            String array = arrayMatcher.group(1);

            Matcher xyMatcher = pXY.matcher(array);

            int k = 0;

            while (xyMatcher.find(k)) {
                String xValue = xyMatcher.group(1);
                String yValue = xyMatcher.group(2);
                String targetValue = (!"null".equals(yValue) ? yValue : "0");
                k = xyMatcher.end();

                Matcher dateMatcher = pJSDate.matcher(xValue);
                String dateStr = "";
                if (dateMatcher.find()) {
                    int year = Integer.parseInt(dateMatcher.group(1));
                    int mon = Integer.parseInt(dateMatcher.group(2)); // zero-based
                    int day = Integer.parseInt(dateMatcher.group(3));
                    int hour24 = Integer.parseInt(dateMatcher.group(4));

                    dateStr = String.format("%d%s%s%s00", year,
                            withLeadZero(mon + 1),
                            withLeadZero(day),
                            withLeadZero(hour24));
                }

                Map<String, String> map = new HashMap<>(2);
                map.put(DATE_STRING, dateStr);
                map.put(valueAlias, targetValue);

                resultItems.add(map);
            }
        } else {
            Log.w("parser", "no match found for array " + arrayName);
        }
        return resultItems;
    }

    private static String withLeadZero(int value) {
        if (value < 0 || value > 99) throw new IllegalArgumentException("Value is out of bounds");
        return value < 10 ? "0" + value : Integer.toString(value);
    }
}
