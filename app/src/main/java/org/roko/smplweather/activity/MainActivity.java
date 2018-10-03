package org.roko.smplweather.activity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.annotation.NonNull;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.roko.smplweather.Constants;
import org.roko.smplweather.MyAdapter;
import org.roko.smplweather.R;
import org.roko.smplweather.RequestCallback;
import org.roko.smplweather.TaskResult;
import org.roko.smplweather.fragment.NetworkFragment;
import org.roko.smplweather.model.ForecastListItem;
import org.roko.smplweather.model.ListItemViewModel;
import org.roko.smplweather.model.MainActivityViewModel;
import org.roko.smplweather.model.RssChannel;
import org.roko.smplweather.model.RssItem;
import org.roko.smplweather.tasks.TaskAction;
import org.roko.smplweather.utils.CalendarHelper;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity implements RequestCallback<TaskResult> {

    private static final String DATE_SOURCE_PATTERN = "dd.MM.yyyy HH:mm'('z')'";
    private static final String DATE_TARGET_PATTERN = "dd MMM HH:mm";

    private static ThreadLocal<SimpleDateFormat> DF_SOURCE = new ThreadLocal<SimpleDateFormat>() {
        @Override
        protected SimpleDateFormat initialValue() {
            return new SimpleDateFormat(DATE_SOURCE_PATTERN, Locale.getDefault());
        }
    };

    private static ThreadLocal<SimpleDateFormat> DF_TARGET = new ThreadLocal<SimpleDateFormat>() {
        @Override
        protected SimpleDateFormat initialValue() {
            return new SimpleDateFormat(DATE_TARGET_PATTERN, Locale.getDefault());
        }
    };

    private static ThreadLocal<SimpleDateFormat> DF_LIST_ITEM_TITLE = new ThreadLocal<SimpleDateFormat>() {
        @Override
        protected SimpleDateFormat initialValue() {
            SimpleDateFormat sdf = new SimpleDateFormat("dd MMMMM", new Locale("ru"));
            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
            return sdf;
        }
    };

    private NetworkFragment mNetworkFragment;

    private boolean isRequestRunning = false;

    private MyAdapter myAdapter;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private TextView mFooter;

    private MainActivityViewModel model;

    private Pattern patternActualDate =
            Pattern.compile("(^\\D+)\\s(\\d{2}\\.\\d{2}\\.\\d{4})\\D+(\\d{2}\\:\\d{2}\\(*.+\\))");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);

        mSwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_container);
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                loadRss(getStoredRssId());
            }
        });

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle("");
        }

        myAdapter = new MyAdapter(this);
        ListView mListView = (ListView) findViewById(R.id.listView);
        mListView.setAdapter(myAdapter);

        mFooter = (TextView) findViewById(R.id.footer);

        String urlString = getString(R.string.url_main);
        mNetworkFragment = NetworkFragment.getInstance(getSupportFragmentManager(), urlString);

        if (savedInstanceState != null &&
                savedInstanceState.containsKey(Constants.BUNDLE_KEY_MAIN_ACTIVITY_VIEW_MODEL)) {
            this.model = (MainActivityViewModel) savedInstanceState.getSerializable(
                    Constants.BUNDLE_KEY_MAIN_ACTIVITY_VIEW_MODEL);
            updateUIFromViewModel(this.model);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (this.model != null) {
            outState.putSerializable(Constants.BUNDLE_KEY_MAIN_ACTIVITY_VIEW_MODEL, model);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        if (Constants.INTENT_ACTION_SELECT_CITY.equals(intent.getAction())) {
            String storedRssId = getStoredRssId();
            String selectedRssId = intent.getStringExtra(Constants.PARAM_KEY_RSS_ID);
            if (!storedRssId.equals(selectedRssId)) {
                SharedPreferences.Editor editor = getSharedPreferences().edit();
                editor.putString(Constants.PARAM_KEY_RSS_ID, selectedRssId);
                editor.apply();

                loadRss(selectedRssId);
            }
        }
    }

    private void loadRss(@NonNull String rssId) {
        if (!isRequestRunning) {
            isRequestRunning = true;
            mSwipeRefreshLayout.setRefreshing(true);
            mNetworkFragment.startTask(TaskAction.READ_RSS_BY_ID, rssId);
        }
    }

    private void updateUIFromViewModel(MainActivityViewModel model) {
        if (model != null) {
            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null) {
                actionBar.setTitle(model.getActionBarTitle());
            }
            myAdapter.setItems(convert(model.getItems()));
            myAdapter.notifyDataSetChanged();
            long lastUpdateUTC = model.getLastUpdateUTC();
            if (lastUpdateUTC != -1) {
                DF_TARGET.get().setTimeZone(TimeZone.getDefault()); // to user's timezone
                String lastUpdateDateTime = DF_TARGET.get().format(new Date(lastUpdateUTC));
                String footer = getString(R.string.footer_actuality) + " " + lastUpdateDateTime;
                mFooter.setText(footer);
            }
        }
    }

    private void goToSearch() {
        Intent intent = new Intent(this, SearchResultsActivity.class);
        startActivity(intent);
    }

    // RequestCallback -----------------------------------------------------------------------------

    @Override
    public void handleResult(String taskAction, @NonNull TaskResult result) {
        final int messageId;
        switch (result.getCode()) {
            case TaskResult.Code.SUCCESS: {
                RssChannel channel = (RssChannel) result.getContent();
                model = convertToViewModel(channel);
                updateUIFromViewModel(model);
                messageId = R.string.toast_update_completed;
            }
            break;
            case TaskResult.Code.NULL_CONTENT: {
                messageId = R.string.toast_no_content;
            }
            break;
            case TaskResult.Code.NETWORK_ISSUE: {
                messageId = R.string.toast_network_issue;
            }
            break;
            default:
                messageId = R.string.toast_update_error;
        }

        Toast.makeText(this, messageId, Toast.LENGTH_SHORT).show();

        isRequestRunning = false;
        mSwipeRefreshLayout.setRefreshing(false);
    }

    @Override
    public NetworkInfo getActiveNetworkInfo() {
        ConnectivityManager manager =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        return manager.getActiveNetworkInfo();
    }

    // Panel menu initialization -------------------------------------------------------------------

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_refresh:
                loadRss(getStoredRssId());
                return true;
            case R.id.action_search:
                goToSearch();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    //----------------------------------------------------------------------------------------------

    private SharedPreferences getSharedPreferences() {
        return getSharedPreferences(Constants.PRIVATE_STORAGE_NAME, Context.MODE_PRIVATE);
    }

    private String getStoredRssId() {
        return getSharedPreferences().getString(Constants.PARAM_KEY_RSS_ID,
                getString(R.string.default_city_id));
    }

    private MainActivityViewModel convertToViewModel(RssChannel channel) {
        Calendar todayUTC = CalendarHelper.supply(TimeZone.getTimeZone("UTC"));
        Calendar itemDayUTC = CalendarHelper.supply(TimeZone.getTimeZone("UTC"));

        String hgCol = getString(R.string.const_hg_cl);
        String city = "";
        long lastUpdateUTC = -1;
        List<ForecastListItem> items = new ArrayList<>(channel.getItems().size());
        for (RssItem rssItem : channel.getItems()) {
            String rssTitle = rssItem.getTitle();
            int pos = rssTitle.lastIndexOf(',');
            String title = "";
            long itemDateUTC = -1;
            if (pos != -1) {
                if (city.isEmpty()) {
                    city = rssTitle.substring(0, pos);
                }
                title = rssTitle.substring(pos + 1).trim();
                itemDateUTC = toDateUTC(title, todayUTC, itemDayUTC);
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
            if (pos != -1 && lastUpdateUTC == -1) {
                String dateInfo = rssSource.substring(pos + 1);

                Matcher m = patternActualDate.matcher(dateInfo);
                if (m.matches()) {
                    String dateString = m.group(2) + " " + m.group(3);
                    try {
                        DF_SOURCE.get().setTimeZone(TimeZone.getTimeZone("UTC"));
                        Date d = DF_SOURCE.get().parse(dateString);
                        lastUpdateUTC = d.getTime();
                    } catch (ParseException ignored) {}
                }
            }

            items.add(new ForecastListItem(title, details.toString(), itemDateUTC));
        }

        MainActivityViewModel model = new MainActivityViewModel();
        model.setActionBarTitle(city);
        model.setItems(items);
        model.setLastUpdateUTC(lastUpdateUTC);

        return model;
    }

    private static long toDateUTC(String title, Calendar todayUTC, Calendar itemDayUTC) {
        try {
            itemDayUTC.setTime(DF_LIST_ITEM_TITLE.get().parse(title));
            itemDayUTC.set(Calendar.YEAR, todayUTC.get(Calendar.YEAR));
            return itemDayUTC.getTimeInMillis();
        } catch (Exception e) {
            return -1;
        }
    }

    private static List<ListItemViewModel> convert(List<ForecastListItem> items) {
        List<ListItemViewModel> res = new ArrayList<>(items.size());

        Calendar calToday = CalendarHelper.supply(TimeZone.getDefault());
        // use utc calendar for items since forecast is already tied to location
        Calendar calItem = CalendarHelper.supply(TimeZone.getTimeZone("UTC"));
        Locale ru = new Locale("ru");
        for (ForecastListItem item : items) {
            String title;
            long itemDateUTC = item.getDateTimeUTC();
            if (itemDateUTC != -1) {
                calItem.setTimeInMillis(itemDateUTC);
                String prefix;
                if (CalendarHelper.ifToday(calToday, calItem)) {
                    prefix = "Сегодня";
                } else if (CalendarHelper.ifTomorrow(calToday, calItem)) {
                    prefix = "Завтра";
                } else {
                    prefix = calItem.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, ru);
                    prefix = Character.toUpperCase(prefix.charAt(0)) + prefix.substring(1);
                }
                title = prefix + ", " + item.getTitle();
            } else {
                title = item.getTitle();
            }
            String desc = item.getDescription();

            res.add(new ListItemViewModel(title, desc));
        }

        return res;
    }
}
