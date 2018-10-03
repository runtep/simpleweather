package org.roko.smplweather.utils;

import java.util.Calendar;
import java.util.TimeZone;

public final class CalendarHelper {
    private CalendarHelper() {}

    public static Calendar supply(TimeZone tz) {
        return Calendar.getInstance(tz);
    }

    public static Calendar supplyUTC() {
        return supply(TimeZone.getTimeZone("UTC"));
    }

    public static Calendar supply(TimeZone tz, long millis) {
        Calendar cal = supply(tz);
        cal.setTimeInMillis(millis);
        return cal;
    }

    public static Calendar supplyUTC(long millis) {
        Calendar cal = supplyUTC();
        cal.setTimeInMillis(millis);
        return cal;
    }

    public static boolean ifToday(Calendar calToday, Calendar calItem) {
        return calItem.get(Calendar.YEAR) == calToday.get(Calendar.YEAR) &&
                calItem.get(Calendar.DAY_OF_YEAR) == calToday.get(Calendar.DAY_OF_YEAR);
    }

    public static boolean ifTomorrow(Calendar calToday, Calendar calItem) {
        return calItem.get(Calendar.YEAR) == calToday.get(Calendar.YEAR) &&
                calItem.get(Calendar.DAY_OF_YEAR) == calToday.get(Calendar.DAY_OF_YEAR) + 1 ||
                calItem.get(Calendar.YEAR) == calToday.get(Calendar.YEAR) + 1 &&
                        calItem.get(Calendar.MONTH) == Calendar.JANUARY && calToday.get(Calendar.MONTH) == Calendar.DECEMBER &&
                        calItem.get(Calendar.DAY_OF_MONTH) == 1 && calToday.get(Calendar.DAY_OF_MONTH) == 31;
    }
}
