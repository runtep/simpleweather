package org.roko.smplweather.utils;

public class DateOnly {

    public final String sYear, sMonth, sDayOfMonth;
    public final int year, monthFromOne, dayOfMonth;

    public DateOnly(String sYear, String sMonth, String sDayOfMonth) {
        this.sYear = sYear;
        this.sMonth = sMonth;
        this.sDayOfMonth = sDayOfMonth;
        this.year = Integer.parseInt(sYear);
        this.monthFromOne = Integer.parseInt(sMonth);
        this.dayOfMonth = Integer.parseInt(sDayOfMonth);
    }

    public DateOnly(String dateString) {
        this(dateString.substring(0, 4),
                dateString.substring(4, 6),
                dateString.substring(6, 8));
    }
}
