package org.roko.smplweather.utils;

import android.content.Context;
import android.text.TextUtils;
import android.util.TypedValue;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;

import org.roko.smplweather.Constants;
import org.roko.smplweather.R;
import org.roko.smplweather.model.City;
import org.roko.smplweather.model.DailyForecastItem;
import org.roko.smplweather.model.DailyListViewItemModel;
import org.roko.smplweather.model.DailyListViewItemModelImpl;
import org.roko.smplweather.model.HourlyDataForDay;
import org.roko.smplweather.model.HourlyDataWrapper;
import org.roko.smplweather.model.HourlyForecast;
import org.roko.smplweather.model.HourlyListViewItemContent;
import org.roko.smplweather.model.HourlyListViewItemDivider;
import org.roko.smplweather.model.HourlyListViewItemModel;
import org.roko.smplweather.model.MainActivityViewModel;
import org.roko.smplweather.model.SuggestionListViewItemModel;
import org.roko.smplweather.model.SuggestionListViewItemModelImpl;
import org.roko.smplweather.model.xml.RssChannel;
import org.roko.smplweather.model.xml.RssItem;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ConvertingHelper {

    private static final String DATE_PATTERN_FORECAST_DATE = "dd.MM.yyyy HH:mm'('z')'";
    private static final String DATE_PATTERN_DAY_WITH_MONTH_NAME = "dd MMMM";

    private static final Locale LOCALE_RU = new Locale("ru");

    private static final Pattern PATTERN_FORECAST_DATE =
            Pattern.compile("(^\\D+)\\s(\\d{2}\\.\\d{2}\\.\\d{4})\\D+(\\d{2}\\:\\d{2}\\(*.+\\))");

    private static final Pattern PATTERN_FORECAST_TEMPS =
            Pattern.compile("Температура\\s+ночью\\s+(-?\\d+.+),\\s+дн[её]м\\s+(-?\\d+.+)[.\\n]?");
    private static final Pattern PATTERN_FORECAST_WIND = Pattern.compile("Ветер\\s+(.+),\\s+(.+)");
    private static final Pattern PATTERN_FORECAST_PRESS =
            Pattern.compile("давление\\s+ночью\\s+(-?\\d+)\\s+(.+),\\s+дн[её]м\\s+(-?\\d+)\\s+(.+)");

    private static final ThreadLocal<SimpleDateFormat> DF_FORECAST_DATE = new ThreadLocal<SimpleDateFormat>() {
        @Override
        protected SimpleDateFormat initialValue() {
            SimpleDateFormat sdf = new SimpleDateFormat(DATE_PATTERN_FORECAST_DATE, LOCALE_RU);
            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
            return sdf;
        }
    };

    private static final ThreadLocal<SimpleDateFormat> DF_DAY_WITH_MONTH_NAME = new ThreadLocal<SimpleDateFormat>() {
        @Override
        protected SimpleDateFormat initialValue() {
            SimpleDateFormat sdf = new SimpleDateFormat(DATE_PATTERN_DAY_WITH_MONTH_NAME, LOCALE_RU);
            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
            return sdf;
        }
    };

    private static final ThreadLocal<SimpleDateFormat> HH_MM = new ThreadLocal<SimpleDateFormat>() {
        @Override
        protected SimpleDateFormat initialValue() {
            return new SimpleDateFormat("HH:mm", LOCALE_RU);
        }
    };

    private ConvertingHelper() {}

    public static MainActivityViewModel convertToViewModel(Context context, RssChannel channel) {
        return convertToViewModel(context, channel, CalendarHelper.provideForUTC());
    }

    public static MainActivityViewModel convertToViewModel(Context context, RssChannel channel,
                                                           final Calendar calItemUTC) {
        final int currentYear = calItemUTC.get(Calendar.YEAR);
        final int currentMonth = calItemUTC.get(Calendar.MONTH);
        String hgCol = context.getString(R.string.const_hg_cl);
        String city = "";
        long rssProvidedUTC = -1;
        List<DailyForecastItem> items = new ArrayList<>(channel.getItems().size());
        int mod = 0;
        for (RssItem rssItem : channel.getItems()) {
            String rssTitle = rssItem.getTitle();
            int pos = rssTitle.lastIndexOf(',');
            String title;
            long itemDateUTC = -1;
            if (pos != -1) {
                if (city.isEmpty()) {
                    city = rssTitle.substring(0, pos);
                }
                title = rssTitle.substring(pos + 1).trim();
                if (tryParseDayMonthUTC(title, calItemUTC)) {
                    // Handle situation when item's day was in previous year
                    if (currentMonth == Calendar.JANUARY) {
                        if (calItemUTC.get(Calendar.MONTH) == Calendar.DECEMBER) {
                            mod = -1; // decrease year for currently parsed item
                        } else {
                            mod = 0; // reset modifier
                        }
                    }
                    calItemUTC.set(Calendar.YEAR, currentYear + mod);
                    itemDateUTC = calItemUTC.getTimeInMillis();
                    // Handle situation when the following items' days will be in next year
                    if (currentMonth == Calendar.DECEMBER &&
                            calItemUTC.get(Calendar.MONTH) == Calendar.DECEMBER &&
                            calItemUTC.get(Calendar.DAY_OF_MONTH) == 31) {
                        mod = 1; // increase year for each next parsed items
                    }
                }
            } else {
                title = rssTitle;
            }

            String rssDesc = rssItem.getDescription();
            StringBuilder details = new StringBuilder(rssDesc.replaceAll("\\. +", "\n"));

            pos = details.lastIndexOf(hgCol);
            if (pos != -1) {
                details.insert(pos + hgCol.length(), '\n');
            }

            String rssSource = rssItem.getSource();
            pos = rssSource.lastIndexOf(',');
            if (pos != -1 && rssProvidedUTC == -1) {
                String dateInfo = rssSource.substring(pos + 1);

                Matcher m = PATTERN_FORECAST_DATE.matcher(dateInfo);
                if (m.matches()) {
                    String dateString = m.group(2) + " " + m.group(3);
                    try {
                        Date d = DF_FORECAST_DATE.get().parse(dateString);
                        rssProvidedUTC = d.getTime();
                    } catch (ParseException ignored) {
                    }
                }
            }

            DailyForecastItem forecastItem;

            String[] forecastParts = details.toString().split("\\n");
            if (forecastParts.length == 5) {
                forecastItem = new DailyForecastItem(title, itemDateUTC);

                String conditionsDesc = forecastParts[0];

                StringBuilder forecastDescription = new StringBuilder(conditionsDesc);

                String tempDesc = forecastParts[1];
                Matcher m = PATTERN_FORECAST_TEMPS.matcher(tempDesc);
                if (m.find()) {
                    String tempNightly = m.group(1);
                    String tempDaily = m.group(2);

                    forecastItem.setTempDaily(tempDaily);
                    forecastItem.setTempNightly(tempNightly);
                } else {
                    forecastDescription.append("\n").append(tempDesc);
                }

                String windDesc = forecastParts[2];
                m = PATTERN_FORECAST_WIND.matcher(windDesc);
                if (m.find()) {
                    String dirFull = m.group(1);
                    String velocity = m.group(2);

                    String dirAbbreviation;
                    if (dirFull.indexOf('-') != -1) {
                        dirAbbreviation = Stream.of(dirFull.split("-"))
                                .map(part -> part.substring(0, 1).toUpperCase())
                                .collect(Collectors.joining());
                    } else {
                        dirAbbreviation = dirFull.substring(0, 1).toUpperCase();
                    }
                    String arrow = windDirectionArrow(
                            context.getResources()
                                    .getStringArray(R.array.windDirectionAbbreviations),
                            context.getResources()
                                    .getStringArray(R.array.windDirectionArrows),
                            dirAbbreviation);
                    forecastItem.setWind(dirAbbreviation + " " + arrow + " " + velocity);
                } else {
                    forecastDescription.append("\n").append(windDesc);
                }

                String pressureDesc = forecastParts[3];
                m = PATTERN_FORECAST_PRESS.matcher(pressureDesc);
                if (m.find()) {
                    String pressNightly = m.group(1);
                    String pressUnits = m.group(2);
                    String pressDaily = m.group(3);

                    forecastItem.setPressure((pressDaily.equals(pressNightly) ? pressDaily :
                            pressDaily + "-" + pressNightly) + " " + pressUnits);
                } else {
                    forecastDescription.append("\n").append(pressureDesc);
                }

                String precipitationDesc = forecastParts[4];
                forecastDescription.append("\n").append(precipitationDesc);

                forecastItem.setDescription(forecastDescription.toString());
            } else {
                forecastItem = new DailyForecastItem(title, itemDateUTC);

                String detailsString = details.toString();

                Matcher m = PATTERN_FORECAST_TEMPS.matcher(detailsString);
                if (m.find()) {
                    String nightly = m.group(1);
                    String daily = m.group(2);
                    detailsString = m.replaceFirst("");

                    forecastItem.setTempDaily(daily);
                    forecastItem.setTempNightly(nightly);
                }
                forecastItem.setDescription(detailsString);
            }

            items.add(forecastItem);
        }

        MainActivityViewModel model = new MainActivityViewModel();
        model.setActionBarTitle(city);
        model.setDailyItems(items);
        model.setRssProvidedUTC(rssProvidedUTC);

        return model;
    }

    private static boolean tryParseDayMonthUTC(String title, Calendar itemDayUTC) {
        try {
            itemDayUTC.setTime(DF_DAY_WITH_MONTH_NAME.get().parse(title));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // ---------------------------------------------------------------------------------------------

    public static List<DailyListViewItemModel> toDailyViewModel(List<DailyForecastItem> items) {
        return convert(items, CalendarHelper.provideFor(TimeZone.getDefault()));
    }

    public static List<DailyListViewItemModel> convert(List<DailyForecastItem> items,
                                                       Calendar calToday) {
        List<DailyListViewItemModel> res = new ArrayList<>(items.size());

        // use utc calendar for items since forecast is already tied to location
        Calendar calItem = CalendarHelper.provideFor(TimeZone.getTimeZone("UTC"));

        int todayIdx = -1, idx = 0;
        for (DailyForecastItem item : items) {
            String title;
            long itemDateUTC = item.getDateTimeUTC();
            if (itemDateUTC != -1) {
                calItem.setTimeInMillis(itemDateUTC);
                String prefix;
                if (CalendarHelper.ifSameDay(calToday, calItem)) {
                    prefix = Constants.RU_TODAY;
                    todayIdx = idx;
                } else if (CalendarHelper.ifTomorrow(calToday, calItem)) {
                    prefix = Constants.RU_TOMORROW;
                } else {
                    prefix = calItem.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, LOCALE_RU);
                    prefix = Character.toUpperCase(prefix.charAt(0)) + prefix.substring(1);
                }
                title = prefix + ", " + item.getTitle();
            } else {
                title = item.getTitle();
            }
            String desc = item.getDescription();

            DailyListViewItemModelImpl vm = new DailyListViewItemModelImpl(title, desc);
            vm.setTempDaily(item.getTempDaily());
            vm.setTempNightly(item.getTempNightly());
            vm.setWind(item.getWind());
            vm.setPressure(item.getPressure());

            res.add(vm);

            idx++;
        }

        if (todayIdx > 0) {
            res = new ArrayList<>(res.subList(todayIdx, res.size()));
        }

        return res;
    }

    private static String windDirectionArrow(String[] directionAbbreviations, String[] directionArrows,
                                             String abbreviation) {
        if (directionAbbreviations.length != directionArrows.length) {
            throw new IllegalArgumentException("Wind directions/directionAbbreviations arrays length mismatch");
        }
        int index = Arrays.asList(directionAbbreviations).indexOf(abbreviation);
        if (index != -1) {
            return directionArrows[index];
        }
        return "";
    }

    // ---------------------------------------------------------------------------------------------

    public static List<HourlyListViewItemModel> toHourlyViewModel(Context context,
                                                                  HourlyForecast hf) {
        List<HourlyListViewItemModel> hourlyListViewItemModels = new ArrayList<>();

        // Obtain wall-time of user`s device ported into UTC. This trick is needed
        // since forecast is provided in local time of considered city but stored as UTC
        Calendar local = CalendarHelper.provideFor(TimeZone.getDefault());
        Calendar dtNow = CalendarHelper.provideForUTC();
        dtNow.set(Calendar.YEAR, local.get(Calendar.YEAR));
        dtNow.set(Calendar.MONTH, local.get(Calendar.MONTH));
        dtNow.set(Calendar.DAY_OF_YEAR, local.get(Calendar.DAY_OF_YEAR));
        dtNow.set(Calendar.HOUR_OF_DAY, local.get(Calendar.HOUR_OF_DAY));
        dtNow.set(Calendar.MINUTE, 0);
        dtNow.set(Calendar.SECOND, 0);
        dtNow.set(Calendar.MILLISECOND, 0);

        // dateTime of each entry is assumed to be in target city`s timezone, but represented as UTC
        Calendar dtOfEntry = CalendarHelper.provideForUTC();
        for (HourlyDataForDay day : hf) {
            dtOfEntry.setTimeInMillis(/*day.dayStr*/0L); // TODO: TDB
            if (CalendarHelper.ifPrecedingDay(dtNow, dtOfEntry)) {
                continue;
            }
            String prefix;
            if (CalendarHelper.ifSameDay(dtNow, dtOfEntry)) {
                prefix = Constants.RU_TODAY;
            } else if (CalendarHelper.ifTomorrow(dtNow, dtOfEntry)) {
                prefix = Constants.RU_TOMORROW;
            } else {
                prefix = dtOfEntry.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, LOCALE_RU);
                prefix = Character.toUpperCase(prefix.charAt(0)) + prefix.substring(1);
            }
            String title = prefix + ", " + DF_DAY_WITH_MONTH_NAME.get().format(dtOfEntry.getTime());

            hourlyListViewItemModels.add(new HourlyListViewItemDivider(title));
            hourlyListViewItemModels.addAll(toHourlyContent(context, day.hourlyData));
        }

        return hourlyListViewItemModels;
    }

    private static List<HourlyListViewItemContent> toHourlyContent(Context context,
                                                                   List<HourlyDataWrapper> items) {
        List<HourlyListViewItemContent> res = new ArrayList<>(items.size());
        HH_MM.get().setTimeZone(TimeZone.getTimeZone("UTC"));

        for (HourlyDataWrapper hdw : items) {
            HourlyListViewItemContent vm = new HourlyListViewItemContent();
            // Time
            Long dateMillis = hdw.getDateMillis();
            String time = "";
            if (dateMillis != -1) {
                time = HH_MM.get().format(new Date(dateMillis));
            }
            vm.setTime(time);
            // Temperature
            String tempCelsius = hdw.getTempCelsius();
            vm.setTemperature(tempCelsius + "°");
            // Description
            String desc = hdw.getDescription();
            vm.setDescription(desc);
            // Wind info
            String windSpeed = hdw.getWindSpeedMeters();
            StringBuilder windInfo = new StringBuilder();
            if (!TextUtils.isEmpty(windSpeed)) {
                if (!"0".equals(windSpeed.trim())) {
                    String direction = hdw.getWindDirName().replace("-", "");
                    String arrow = windDirectionArrow(
                            context.getResources()
                                    .getStringArray(R.array.windDirectionAbbreviations),
                            context.getResources()
                                    .getStringArray(R.array.windDirectionArrows), direction);
                    windInfo.append("Ветер ").append(direction).append(" ").append(arrow).append(" ").
                            append(hdw.getWindSpeedMeters()).append(" м/с");
                } else {
                    windInfo.append("Штиль");
                }
            }
            vm.setWind(windInfo.toString());
            // Humidity
            vm.setHumidity("Влажность " + hdw.getHumidityPercent() + "%");
            // Precipitation
            String precipitationLevel = hdw.getPrecipitationMillimeters();
            if (!TextUtils.isEmpty(precipitationLevel)) {
                float ovalDiameterVariationPercentage = .0f;
                float valueMm = .0f;
                try {
                    valueMm = Float.parseFloat(precipitationLevel);
                    TypedValue resVal = new TypedValue();
                    context.getResources().getValue(R.dimen.maxPrecipitationLevelThresholdMm, resVal, true);
                    float maxValueThresholdMm = resVal.getFloat();
                    ovalDiameterVariationPercentage =
                            Math.min(valueMm, maxValueThresholdMm) * 100f / maxValueThresholdMm;
                } catch (NumberFormatException ignored) {}
                vm.setOvalDiameterVariationPercentage(ovalDiameterVariationPercentage);
                if (valueMm > .0f) {
                    String prob = hdw.getPrecipitationProbability();
                    vm.setPrecipLevel(valueMm + " мм");
                    vm.setPrecipProbability(prob + "%");
                }
            }
            res.add(vm);
        }
        return res;
    }

    // ---------------------------------------------------------------------------------------------

    public static List<SuggestionListViewItemModel> toSuggestionsViewModel(List<City> cityList) {
        return Stream.of(cityList)
                .map(city -> new SuggestionListViewItemModelImpl(city.getId(), city.getTitle(), city.getPath()))
                .collect(Collectors.toList());
    }
}
