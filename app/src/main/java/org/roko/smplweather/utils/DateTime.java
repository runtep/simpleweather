package org.roko.smplweather.utils;

public class DateTime {

    public final String sYear, sMonth, sDayOfMonth, sHour24, sMinute;
    public final int year, month, dayOfMonth;

    public DateTime(String sYear, String sMonth, String sDayOfMonth, String sHour24, String sMinute) {
        this.sYear = sYear;
        this.sMonth = sMonth;
        this.sDayOfMonth = sDayOfMonth;
        this.sHour24 = sHour24;
        this.sMinute = sMinute;
        //
        this.year = Integer.parseInt(sYear);
        this.month = Integer.parseInt(sMonth);
        this.dayOfMonth = Integer.parseInt(sDayOfMonth);
    }

    public DateTime(String dateTimeString) {
        this(dateTimeString.substring(0, 4),
                dateTimeString.substring(4, 6),
                dateTimeString.substring(6, 8),
                dateTimeString.substring(8, 10),
                dateTimeString.substring(10, 12));
    }

    public boolean before(DateTime that) {
        return this.year < that.year ||
                this.year == that.year &&
                        (this.month < that.month ||
                                this.month == that.month && this.dayOfMonth < that.dayOfMonth);
    }

    public String dateOnlyString() {
        return sYear + sMonth + sDayOfMonth;
    }
}