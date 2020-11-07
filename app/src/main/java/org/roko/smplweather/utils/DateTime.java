package org.roko.smplweather.utils;

public class DateTime extends DateOnly {

    public final String sHour24, sMinute;
    public final int hour24, minute;

    public DateTime(String sYear, String sMonth, String sDayOfMonth, String sHour24, String sMinute) {
        super(sYear, sMonth, sDayOfMonth);
        this.sHour24 = sHour24;
        this.sMinute = sMinute;
        this.hour24 = Integer.parseInt(sHour24);
        this.minute = Integer.parseInt(sMinute);
    }

    public DateTime(String dateTimeString) {
        this(dateTimeString.substring(0, 4),
                dateTimeString.substring(4, 6),
                dateTimeString.substring(6, 8),
                dateTimeString.substring(8, 10),
                dateTimeString.substring(10, 12));
    }

    public boolean isDayBefore(DateTime that) {
        return this.year < that.year ||
                this.year == that.year &&
                        (this.monthFromOne < that.monthFromOne ||
                                this.monthFromOne == that.monthFromOne && this.dayOfMonth < that.dayOfMonth);
    }

    public String dateOnlyString() {
        return sYear + sMonth + sDayOfMonth;
    }
}