package org.roko.smplweather.activity;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.support.annotation.NonNull;
import android.support.v4.widget.CursorAdapter;
import android.support.v4.widget.SimpleCursorAdapter;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.roko.smplweather.Constants;
import org.roko.smplweather.MyAdapter;
import org.roko.smplweather.R;
import org.roko.smplweather.RequestCallback;
import org.roko.smplweather.TaskResult;
import org.roko.smplweather.fragment.NetworkFragment;
import org.roko.smplweather.model.City;
import org.roko.smplweather.model.ForecastItem;
import org.roko.smplweather.model.ListViewItemModel;
import org.roko.smplweather.model.MainActivityViewModel;
import org.roko.smplweather.model.SuggestionsModel;
import org.roko.smplweather.model.xml.RssChannel;
import org.roko.smplweather.model.xml.RssItem;
import org.roko.smplweather.tasks.TaskAction;
import org.roko.smplweather.utils.CalendarHelper;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity implements RequestCallback<TaskResult> {

    private static final String DATE_SOURCE_PATTERN = "dd.MM.yyyy HH:mm'('z')'";
    private static final String DATE_TARGET_PATTERN = "dd MMM HH:mm";
    private static final String DATE_PATTERN_ITEM_TITLE = "dd MMMMM";

    private static final Locale LOCALE_RU = new Locale("ru");

    private static Pattern PATTERN_FORECAST_DATE =
            Pattern.compile("(^\\D+)\\s(\\d{2}\\.\\d{2}\\.\\d{4})\\D+(\\d{2}\\:\\d{2}\\(*.+\\))");

    private static Pattern PATTERN_FORECAST_TEMPS =
            Pattern.compile("Температура\\s+ночью\\s+(-?\\d+.+),\\s+дн[её]м\\s+(-?\\d+.+)[.\\n]");

    private static ThreadLocal<SimpleDateFormat> DF_SOURCE = new ThreadLocal<SimpleDateFormat>() {
        @Override
        protected SimpleDateFormat initialValue() {
            SimpleDateFormat sdf = new SimpleDateFormat(DATE_SOURCE_PATTERN, Locale.getDefault());
            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
            return sdf;
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
            SimpleDateFormat sdf = new SimpleDateFormat(DATE_PATTERN_ITEM_TITLE, LOCALE_RU);
            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
            return sdf;
        }
    };

    private static final String[] COLUMNS = new String[]{
            BaseColumns._ID,
            SearchManager.SUGGEST_COLUMN_TEXT_1,
            SearchManager.SUGGEST_COLUMN_TEXT_2,
            SearchManager.SUGGEST_COLUMN_INTENT_DATA
    };

    private NetworkFragment mNetworkFragment;

    private boolean isRequestRunning = false;

    private SwipeRefreshLayout mSwipeRefreshLayout;
    private MyAdapter<ListViewItemModel> myAdapter;
    private TextView mFooter;

    private MainActivityViewModel model;
    private SuggestionsModel suggestionsModel;

    private SearchView mSearchView;
    private CursorAdapter mCursorAdapter;

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

        myAdapter = new MyAdapter<>(this);
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
        HashMap m = (HashMap) mNetworkFragment.getSessionStorage();
        if (!m.isEmpty()) {
            outState.putSerializable(Constants.BUNDLE_KEY_SESSION_STORAGE, m);
        }
        if (this.suggestionsModel != null) {
            suggestionsModel.setQuery(mSearchView.getQuery().toString());
            outState.putSerializable(Constants.BUNDLE_KEY_SUGGESTIONS_MODEL, suggestionsModel);
        }
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        HashMap m = (HashMap) savedInstanceState.getSerializable(Constants.BUNDLE_KEY_SESSION_STORAGE);
        if (m != null && !m.isEmpty()) {
            mNetworkFragment.setSessionStorage(m);
        }
        suggestionsModel = (SuggestionsModel) savedInstanceState.getSerializable(
                Constants.BUNDLE_KEY_SUGGESTIONS_MODEL);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            String cityId = intent.getStringExtra(SearchManager.QUERY);
            if (cityId == null) {
                cityId = intent.getDataString();
            }
            suggestionsModel.clear();
            fillSuggestions(Collections.<City>emptyList());
            collapseSearchView(mSearchView);

            if (cityId != null && !cityId.isEmpty()) {
                isRequestRunning = true;
                mSwipeRefreshLayout.setRefreshing(true);
                mNetworkFragment.startTask(TaskAction.GET_RSS_ID_BY_CITY_ID, cityId);
            }
        }
    }

    private void storeSelected(String selectedRssId) {
        String storedRssId = getStoredRssId();
        if (!storedRssId.equals(selectedRssId)) {
            SharedPreferences.Editor editor = getSharedPreferences().edit();
            editor.putString(Constants.PARAM_KEY_RSS_ID, selectedRssId);
            editor.apply();
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

    private void fillSuggestions(List<City> cityList) {
        mCursorAdapter.changeCursor(prepareCursor(cityList));
    }

    private Cursor prepareCursor(List<City> cityList) {
        MatrixCursor mCursor = new MatrixCursor(COLUMNS);
        int number = 1;
        for (City city : cityList) {
            MatrixCursor.RowBuilder rowBuilder = mCursor.newRow();
            rowBuilder.add(number++).add(city.getTitle()).add(city.getPath()).add(city.getId());
        }
        return mCursor;
    }

    // RequestCallback -----------------------------------------------------------------------------

    @Override
    public void handleResult(String taskAction, @NonNull TaskResult result) {
        int messageId = -1;
        switch (result.getCode()) {
            case TaskResult.Code.SUCCESS: {
                if (TaskAction.READ_RSS_BY_ID.equals(taskAction)) {
                    RssChannel channel = (RssChannel) result.getContent();
                    model = convertToViewModel(channel);
                    updateUIFromViewModel(model);
                    messageId = R.string.toast_update_completed;
                } else if (TaskAction.SEARCH_CITY_BY_NAME.equals(taskAction)) {
                    List<City> cityList = (List<City>) result.getContent();
                    suggestionsModel = new SuggestionsModel(cityList);
                    fillSuggestions(cityList);
                } else if (TaskAction.GET_RSS_ID_BY_CITY_ID.equals(taskAction)) {
                    String rssId = (String) result.getContent();
                    storeSelected(rssId);
                }
            }
            break;
            case TaskResult.Code.NULL_CONTENT: {
                if (TaskAction.SEARCH_CITY_BY_NAME.equals(taskAction)) {
                    suggestionsModel.clear();
                    fillSuggestions(Collections.<City>emptyList());
                }
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
        if (messageId != -1) {
            Toast.makeText(this, messageId, Toast.LENGTH_SHORT).show();
        }

        if (TaskAction.GET_RSS_ID_BY_CITY_ID.equals(taskAction) &&
                TaskResult.Code.SUCCESS == result.getCode()) {
            // preserve refreshing state and immediately start new request
            mNetworkFragment.startTask(TaskAction.READ_RSS_BY_ID, (String) result.getContent());
        } else {
            isRequestRunning = false;
            mSwipeRefreshLayout.setRefreshing(false);
        }
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

        // Associate searchable configuration with the SearchView
        final SearchManager searchManager =
                (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        mSearchView = (SearchView) menu.findItem(R.id.action_search).getActionView();
        mSearchView.setSearchableInfo(
                searchManager.getSearchableInfo(getComponentName()));
        mSearchView.setSubmitButtonEnabled(false);
        mSearchView.setIconifiedByDefault(true);
        mSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                // TODO: should we limit request frequency by checking #isRequestRunning?
                String query = newText.trim();
                if (query.length() >= getResources().getInteger(R.integer.minSuggestLenThreshold)) {
                    isRequestRunning = true;
                    mNetworkFragment.startTask(TaskAction.SEARCH_CITY_BY_NAME, query);
                }
                return true;
            }
        });

        Cursor mCursor;
        boolean restore = suggestionsModel != null && !suggestionsModel.isEmpty();
        if (restore) {
            mCursor = prepareCursor(suggestionsModel.getSuggestions());
            mSearchView.setQuery(suggestionsModel.getQuery(), false);
            suggestionsModel.clear();
        } else {
            mCursor = new MatrixCursor(COLUMNS); // empty cursor
        }

        mCursorAdapter = new SimpleCursorAdapter(
                this,
                R.layout.suggestion,
                mCursor,
                new String[]{
                        SearchManager.SUGGEST_COLUMN_TEXT_1,
                        SearchManager.SUGGEST_COLUMN_TEXT_2},
                new int[]{
                        R.id.suggestionTitle,
                        R.id.suggestionSubtitle},
                0);
        mSearchView.setSuggestionsAdapter(mCursorAdapter);

        if (restore) {
            mSearchView.setIconified(false);
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_refresh:
                loadRss(getStoredRssId());
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void collapseSearchView(SearchView v) {
        if (v != null) {
            v.setQuery("", false);
            v.setIconified(true);
        }
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
        return convertToViewModel(this, channel, CalendarHelper.supplyUTC());
    }

    public static MainActivityViewModel convertToViewModel(Context context, RssChannel channel,
                                                           final Calendar calItemUTC) {
        final int currentYear = calItemUTC.get(Calendar.YEAR);
        final int currentMonth = calItemUTC.get(Calendar.MONTH);
        String hgCol = context.getString(R.string.const_hg_cl);
        String city = "";
        long lastUpdateUTC = -1;
        List<ForecastItem> items = new ArrayList<>(channel.getItems().size());
        int mod = 0;
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
            if (pos != -1 && lastUpdateUTC == -1) {
                String dateInfo = rssSource.substring(pos + 1);

                Matcher m = PATTERN_FORECAST_DATE.matcher(dateInfo);
                if (m.matches()) {
                    String dateString = m.group(2) + " " + m.group(3);
                    try {
                        Date d = DF_SOURCE.get().parse(dateString);
                        lastUpdateUTC = d.getTime();
                    } catch (ParseException ignored) {
                    }
                }
            }
            String detailsString = details.toString();
            ForecastItem fi = new ForecastItem(title, detailsString, itemDateUTC);

            Matcher m = PATTERN_FORECAST_TEMPS.matcher(detailsString);
            if (m.find()) {
                String nightly = m.group(1);
                String daily = m.group(2);
                detailsString = m.replaceFirst("");

                fi.setTempDaily(daily);
                fi.setTempNightly(nightly);
                fi.setDescription(detailsString);
            }

            items.add(fi);
        }

        MainActivityViewModel model = new MainActivityViewModel();
        model.setActionBarTitle(city);
        model.setItems(items);
        model.setLastUpdateUTC(lastUpdateUTC);

        return model;
    }

    private static boolean tryParseDayMonthUTC(String title, Calendar itemDayUTC) {
        try {
            itemDayUTC.setTime(DF_LIST_ITEM_TITLE.get().parse(title));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static List<ListViewItemModel> convert(List<ForecastItem> items) {
        return convert(items, CalendarHelper.supply(TimeZone.getDefault()));
    }

    public static List<ListViewItemModel> convert(List<ForecastItem> items, Calendar calToday) {
        List<ListViewItemModel> res = new ArrayList<>(items.size());

        // use utc calendar for items since forecast is already tied to location
        Calendar calItem = CalendarHelper.supply(TimeZone.getTimeZone("UTC"));

        for (ForecastItem item : items) {
            String title;
            long itemDateUTC = item.getDateTimeUTC();
            if (itemDateUTC != -1) {
                calItem.setTimeInMillis(itemDateUTC);
                String prefix;
                if (CalendarHelper.ifToday(calToday, calItem)) {
                    prefix = Constants.RU_TODAY;
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

            ListViewItemModel vm = new ListViewItemModel(title, desc);
            vm.setTempDaily(item.getTempDaily());
            vm.setTempNightly(item.getTempNightly());

            res.add(vm);
        }

        return res;
    }
}
