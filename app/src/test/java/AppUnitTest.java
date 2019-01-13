import android.content.Context;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.roko.smplweather.Constants;
import org.roko.smplweather.R;
import org.roko.smplweather.activity.MainActivity;
import org.roko.smplweather.model.ForecastItem;
import org.roko.smplweather.model.ListViewItemModel;
import org.roko.smplweather.model.MainActivityViewModel;
import org.roko.smplweather.model.xml.RssChannel;
import org.roko.smplweather.model.xml.RssItem;
import org.roko.smplweather.utils.CalendarHelper;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class AppUnitTest {

    private static final String TITLE_PREFIX = "stub, ";
    private static final String DESC = "stub";
    private static final String SOURCE = "stub, stub 03.10.2018 в 10:03(UTC)";

    private static TimeZone TZ_UTC = TimeZone.getTimeZone("UTC");
    private static Locale LOCALE_RU = new Locale("ru");

    private static final ThreadLocal<SimpleDateFormat> DATE_FORMAT = new ThreadLocal<SimpleDateFormat>() {
        @Override
        protected SimpleDateFormat initialValue() {
            SimpleDateFormat dfFull = new SimpleDateFormat("yyyy-MM-dd");
            dfFull.setTimeZone(TZ_UTC);
            return dfFull;
        }
    };

    private static Context CONTEXT;

    @Before
    public void setUp() {
        CONTEXT = Mockito.mock(Context.class);
        Mockito.when(CONTEXT.getString(R.string.const_hg_cl)).thenReturn(Mockito.anyString());
    }

    @Test
    public void simpleTest() {
        Calendar supplier = CalendarHelper.supplyUTC();
        setDate(supplier, 2018, Calendar.OCTOBER, 1);

        RssChannel channel = provide(supplier, 3);

        Calendar startPoint = CalendarHelper.supplyUTC();
        setDate(startPoint, 2018, Calendar.OCTOBER, 2);
        System.out.println("\"today\"=" + DATE_FORMAT.get().format(startPoint.getTime()));

        MainActivityViewModel model = MainActivity.convertToViewModel(CONTEXT, channel, startPoint);
        Assert.assertTrue("Failed to parse 'last update'", model.getLastUpdateUTC() != -1);

        checkDateOrder(model.getItems());
    }

    @Test
    public void complexTest() {
        System.out.println("Test joint between two years");

        Calendar supplier = CalendarHelper.supplyUTC();
        setDate(supplier, 2018, Calendar.DECEMBER, 29);

        RssChannel channel = provide(supplier, 5);

        Calendar startPoint = CalendarHelper.supplyUTC();

        System.out.println("Current date is before new year");
        setDate(startPoint, 2018, Calendar.DECEMBER, 29);
        System.out.println("\"today\"=" + DATE_FORMAT.get().format(startPoint.getTime()));

        MainActivityViewModel model = MainActivity.convertToViewModel(CONTEXT, channel, startPoint);
        checkDateOrder(model.getItems());

        System.out.println("Current date is after new year");
        setDate(startPoint, 2019, Calendar.JANUARY, 2);
        System.out.println("\"today\"=" + DATE_FORMAT.get().format(startPoint.getTime()));

        model = MainActivity.convertToViewModel(CONTEXT, channel, startPoint);
        checkDateOrder(model.getItems());
    }

    @Test
    public void converterTest() {
        Calendar supplier = CalendarHelper.supplyUTC();
        setDate(supplier, 2018, Calendar.DECEMBER, 30);

        RssChannel channel = provide(supplier, 3);

        Calendar startPoint = CalendarHelper.supplyUTC();
        setDate(startPoint, 2018, Calendar.DECEMBER, 31);
        System.out.println("\"today\"=" + DATE_FORMAT.get().format(startPoint.getTime()));

        MainActivityViewModel model = MainActivity.convertToViewModel(CONTEXT, channel, startPoint);

        setDate(startPoint, 2018, Calendar.DECEMBER, 31); // reset
        List<ListViewItemModel> lvItems = MainActivity.convert(model.getItems(), startPoint);

        for (ListViewItemModel lvItem : lvItems) {
            System.out.println(lvItem.getTitle());
        }

        Assert.assertTrue(lvItems.get(1).getTitle().startsWith(Constants.RU_TODAY));
        Assert.assertTrue(lvItems.get(2).getTitle().startsWith(Constants.RU_TOMORROW));
    }

    private static RssChannel provide(Calendar supplier, int itemCount) {
        RssChannel channel = new RssChannel();
        List<RssItem> rssItems = new ArrayList<>(itemCount);
        channel.setItems(rssItems);

        for (int i = 0; i < itemCount; i++) {
            rssItems.add(provide(dayAndMonth(supplier)));
            supplier.add(Calendar.DAY_OF_YEAR, 1);
        }
        return channel;
    }

    private static String dayAndMonth(Calendar supplier) {
        return supplier.get(Calendar.DAY_OF_MONTH) + " " +
                supplier.getDisplayName(Calendar.MONTH, Calendar.LONG, LOCALE_RU);
    }

    private static void checkDateOrder(List<ForecastItem> items) {
        long prevMillis = -1;
        for (ForecastItem fItem : items) {
            long itemMillis = fItem.getDateTimeUTC();
            Assert.assertTrue(itemMillis != -1);

            Assert.assertTrue("Date order violated", itemMillis > prevMillis);
            prevMillis = itemMillis;

            Date d = new Date(itemMillis);
            System.out.println("title=\"" + fItem.getTitle() + "\"; date=" + DATE_FORMAT.get().format(d));
        }
    }

    private static RssItem provide(String dayAndMonth) {
        RssItem item = new RssItem();
        item.setTitle(TITLE_PREFIX + dayAndMonth);
        item.setDescription(DESC);
        item.setSource(SOURCE);
        return item;
    }

    private static void setDate(Calendar cal, int year, int monthFromZero, int dayOfMonth) {
        cal.set(Calendar.YEAR, year);
        cal.set(Calendar.MONTH, monthFromZero);
        cal.set(Calendar.DAY_OF_MONTH, dayOfMonth);
        setTime(cal, 0, 0, 0);
    }

    private static void setTime(Calendar cal, int hour24, int min, int sec) {
        cal.set(Calendar.HOUR_OF_DAY, hour24);
        cal.set(Calendar.MINUTE, min);
        cal.set(Calendar.SECOND, sec);
        cal.set(Calendar.MILLISECOND, 0);
    }
}
