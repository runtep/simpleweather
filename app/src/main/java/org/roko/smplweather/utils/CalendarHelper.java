package org.roko.smplweather.utils;

import java.util.Calendar;
import java.util.TimeZone;

public final class CalendarHelper {

    private CalendarHelper() {}

    public static Calendar provideFor(TimeZone tz) {
        return Calendar.getInstance(tz);
    }

    public static Calendar provideForUTC() {
        return provideFor(TimeZone.getTimeZone("UTC"));
    }

    public static Calendar provideFor(TimeZone tz, long millis) {
        Calendar cal = provideFor(tz);
        cal.setTimeInMillis(millis);
        return cal;
    }

    public static Calendar provideForUTC(long millis) {
        Calendar cal = provideForUTC();
        cal.setTimeInMillis(millis);
        return cal;
    }

    public static boolean ifPrecedingDay(Calendar lookFrom, Calendar target) {
        return target.get(Calendar.YEAR) < lookFrom.get(Calendar.YEAR) ||
                target.get(Calendar.YEAR) == lookFrom.get(Calendar.YEAR) &&
                target.get(Calendar.DAY_OF_YEAR) < lookFrom.get(Calendar.DAY_OF_YEAR);
    }

    public static boolean ifSameDay(Calendar lookFrom, Calendar target) {
        return target.get(Calendar.YEAR) == lookFrom.get(Calendar.YEAR) &&
                target.get(Calendar.DAY_OF_YEAR) == lookFrom.get(Calendar.DAY_OF_YEAR);
    }

    public static boolean ifTomorrow(Calendar lookFrom, Calendar target) {
        return target.get(Calendar.YEAR) == lookFrom.get(Calendar.YEAR) &&
                target.get(Calendar.DAY_OF_YEAR) == lookFrom.get(Calendar.DAY_OF_YEAR) + 1 ||
                target.get(Calendar.YEAR) == lookFrom.get(Calendar.YEAR) + 1 &&
                        target.get(Calendar.MONTH) == Calendar.JANUARY && lookFrom.get(Calendar.MONTH) == Calendar.DECEMBER &&
                        target.get(Calendar.DAY_OF_MONTH) == 1 && lookFrom.get(Calendar.DAY_OF_MONTH) == 31;
    }

    public static void cropTime(Calendar cal) {
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
    }
}
