package org.roko.smplweather.activity;

import android.app.SearchManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;

import org.roko.smplweather.Constants;
import org.roko.smplweather.adapter.BasicListViewAdapter;
import org.roko.smplweather.R;
import org.roko.smplweather.RequestCallback;
import org.roko.smplweather.TaskResult;
import org.roko.smplweather.adapter.ForecastPagerAdapter;
import org.roko.smplweather.fragment.DailyTabFragment;
import org.roko.smplweather.fragment.HourlyTabFragment;
import org.roko.smplweather.fragment.NetworkFragment;
import org.roko.smplweather.model.City;
import org.roko.smplweather.model.DailyForecastItem;
import org.roko.smplweather.model.DailyListViewItemModel;
import org.roko.smplweather.model.DailyListViewItemModelImpl;
import org.roko.smplweather.model.HourlyListViewItemContent;
import org.roko.smplweather.model.HourlyListViewItemModel;
import org.roko.smplweather.model.HourlyDataWrapper;
import org.roko.smplweather.model.HourlyForecast;
import org.roko.smplweather.model.HourlyListViewItemDivider;
import org.roko.smplweather.model.MainActivityViewModel;
import org.roko.smplweather.model.SuggestionListViewItemModel;
import org.roko.smplweather.model.SuggestionListViewItemModelImpl;
import org.roko.smplweather.model.SuggestionsModel;
import org.roko.smplweather.model.xml.RssChannel;
import org.roko.smplweather.model.xml.RssItem;
import org.roko.smplweather.tasks.TaskAction;
import org.roko.smplweather.utils.CalendarHelper;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.SearchView;
import androidx.core.util.Pair;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.viewpager.widget.ViewPager;

public class MainActivity extends AppCompatActivity implements RequestCallback<TaskResult> {

    private static final String DATE_SOURCE_PATTERN = "dd.MM.yyyy HH:mm'('z')'";
    private static final String DATE_PATTERN_ITEM_TITLE = "dd MMMM";

    private static final Locale LOCALE_RU = new Locale("ru");

    private static final Pattern PATTERN_FORECAST_DATE =
            Pattern.compile("(^\\D+)\\s(\\d{2}\\.\\d{2}\\.\\d{4})\\D+(\\d{2}\\:\\d{2}\\(*.+\\))");

    private static final Pattern PATTERN_FORECAST_TEMPS =
            Pattern.compile("Температура\\s+ночью\\s+(-?\\d+.+),\\s+дн[её]м\\s+(-?\\d+.+)[.\\n]?");
    private static final Pattern PATTERN_FORECAST_WIND = Pattern.compile("Ветер\\s+(.+),\\s+(.+)");
    private static final Pattern PATTERN_FORECAST_PRESS =
            Pattern.compile("давление\\s+ночью\\s+(-?\\d+)\\s+(.+),\\s+дн[её]м\\s+(-?\\d+)\\s+(.+)");

    private static final ThreadLocal<SimpleDateFormat> DF_SOURCE = new ThreadLocal<SimpleDateFormat>() {
        @Override
        protected SimpleDateFormat initialValue() {
            SimpleDateFormat sdf = new SimpleDateFormat(DATE_SOURCE_PATTERN, LOCALE_RU);
            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
            return sdf;
        }
    };

    private static final ThreadLocal<SimpleDateFormat> DF_LIST_ITEM_TITLE = new ThreadLocal<SimpleDateFormat>() {
        @Override
        protected SimpleDateFormat initialValue() {
            SimpleDateFormat sdf = new SimpleDateFormat(DATE_PATTERN_ITEM_TITLE, LOCALE_RU);
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

    private static final List<String> WIND_DIRECTIONS =
            Collections.unmodifiableList(Arrays.asList("С", "СВ", "В", "ЮВ", "Ю", "ЮЗ", "З", "СЗ"));

    private NetworkFragment mNetworkFragment;

    private boolean isRequestRunning = false;

    private ViewPager mViewPager;
    private ForecastPagerAdapter mPagerAdapter;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private BasicListViewAdapter<SuggestionListViewItemModel> mSuggestionsAdapter;

    private MainActivityViewModel model;
    private SuggestionsModel suggestionsModel;
    private boolean skipQueryTextChangeListener = false;

    private SearchView mSearchView;
    private MenuItem mSearchMenuItem;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle("");
        }

        mViewPager = findViewById(R.id.vPager);
        mPagerAdapter = new ForecastPagerAdapter(getSupportFragmentManager(), getResources());
        mViewPager.setAdapter(mPagerAdapter);
        mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int i, float v, int i1) {}

            @Override
            public void onPageSelected(int i) {}

            @Override
            public void onPageScrollStateChanged(int state) {
                enableDisableSwipeRefresh(state == ViewPager.SCROLL_STATE_IDLE);
            }
        });

        mSwipeRefreshLayout = findViewById(R.id.swipe_container);
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                fetchRssBodyThenHourlyData(getStoredRssId(), getStoredCityId());
            }
        });

        mSuggestionsAdapter = new BasicListViewAdapter<>(this);
        ListView mListViewSuggestions = findViewById(R.id.suggestionsView);
        mListViewSuggestions.setAdapter(mSuggestionsAdapter);
        mListViewSuggestions.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                SuggestionListViewItemModel item = (SuggestionListViewItemModel) adapterView.getItemAtPosition(i);
                String cityId = item.get_id();
                if (!Constants.EMPTY_CITY_ID.equals(cityId)) {
                    //
                    updateModelAndDisplaySuggestions(Collections.emptyList());
                    collapseSearchView();
                    //
                    if (!TextUtils.isEmpty(cityId)) {
                        forceFetchRssIdThenBody(cityId);
                    }
                }
            }
        });

        String urlString = getString(R.string.url_main);
        String[] lightweightPages = getResources().getStringArray(R.array.lightweightPages);
        mNetworkFragment =
                NetworkFragment.getInstance(getSupportFragmentManager(), urlString, lightweightPages);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (this.model != null) {
            outState.putSerializable(Constants.BUNDLE_KEY_MAIN_ACTIVITY_VIEW_MODEL, model);
        }
        if (this.suggestionsModel != null) {
            suggestionsModel.setQuery(mSearchView.getQuery().toString());
            outState.putSerializable(Constants.BUNDLE_KEY_SUGGESTIONS_MODEL, suggestionsModel);
        }
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        this.suggestionsModel = (SuggestionsModel) savedInstanceState.getSerializable(
                Constants.BUNDLE_KEY_SUGGESTIONS_MODEL);
        MainActivityViewModel vModel = (MainActivityViewModel) savedInstanceState.getSerializable(
                Constants.BUNDLE_KEY_MAIN_ACTIVITY_VIEW_MODEL);
        if (vModel != null) {
            this.model = vModel;
        }
    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        if (this.model == null || isTTLExpired(System.currentTimeMillis())) {
            fetchRssBodyThenHourlyData(getStoredRssId(), getStoredCityId());
        } else if (this.model != null) {
            updateUIFromViewModel(this.model);
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

    private void storeCityId(String cityId) {
        String stored = getStoredCityId();
        if (!stored.equals(cityId)) {
            SharedPreferences.Editor editor = getSharedPreferences().edit();
            editor.putString(Constants.PARAMS_KEY_CITY_ID, cityId);
            editor.apply();
        }
    }

    private void storeLastUpdateTime(long lastUpdateMillis) {
        SharedPreferences.Editor editor = getSharedPreferences().edit();
        editor.putLong(Constants.PARAMS_KEY_LAST_UPDATE_MILLIS, lastUpdateMillis);
        editor.apply();
    }

    private void storeTimeToLive(String ttlMinutes) {
        if (!TextUtils.isEmpty(ttlMinutes) && TextUtils.isDigitsOnly(ttlMinutes)) {
            SharedPreferences.Editor editor = getSharedPreferences().edit();
            long ttlMs = Long.parseLong(ttlMinutes) * 60_000L;
            editor.putLong(Constants.PARAMS_KEY_TIME_TO_LIVE_MILLIS, ttlMs);
            editor.apply();
        }
    }

    /** Perform action with demand of subsequent action:
     *  fetch rss body by rssId -> fetch hourly forecast by cityId
     */
    private void fetchRssBodyThenHourlyData(String rssId, String cityId) {
        if (!isRequestRunning) {
            isRequestRunning = true;
            mSwipeRefreshLayout.setRefreshing(true);
            Bundle nextTask = new Bundle();
            nextTask.putString(Constants.BUNDLE_KEY_TRIGGER, "swipeLayoutRefresh");
            nextTask.putString(Constants.BUNDLE_KEY_NEXT_TASK_ACTION, TaskAction.GET_HOURLY_FORECAST_BY_CITY_ID);
            nextTask.putString(Constants.BUNDLE_KEY_CITY_ID, cityId);
            mNetworkFragment.startTask(TaskAction.GET_RSS_BODY_BY_RSS_ID, rssId, nextTask);
        }
    }

    /** Perform action with demand of subsequent action:
     *  fetch rssId by cityId -> fetch rss body by rssId
     */
    private void forceFetchRssIdThenBody(String cityId) {
        isRequestRunning = true;
        mSwipeRefreshLayout.setRefreshing(true);
        Bundle nextTask = new Bundle();
        nextTask.putString(Constants.BUNDLE_KEY_TRIGGER, "citySelectedFromListOfSuggestions");
        nextTask.putString(Constants.BUNDLE_KEY_NEXT_TASK_ACTION, TaskAction.GET_RSS_BODY_BY_RSS_ID);
        nextTask.putString(Constants.BUNDLE_KEY_CITY_ID, cityId);
        mNetworkFragment.startTask(TaskAction.GET_RSS_ID_BY_CITY_ID, cityId, nextTask);
    }

    private Pair<DailyTabFragment, HourlyTabFragment> getPagerFragments() {
        DailyTabFragment f1;
        HourlyTabFragment f2;
        if (!mPagerAdapter.storageEmpty()) {
            f1 = mPagerAdapter.getStoredFragment(0);
            f2 = mPagerAdapter.getStoredFragment(1);
        } else {
            f1 = (DailyTabFragment) getSupportFragmentManager()
                    .findFragmentByTag("android:switcher:" + R.id.vPager + ":" + 0);
            f2 = (HourlyTabFragment) getSupportFragmentManager()
                    .findFragmentByTag("android:switcher:" + R.id.vPager + ":" + 1);
        }
        return Pair.create(f1, f2);
    }

    private void updateUIFromViewModel(MainActivityViewModel model) {
        if (model != null) {
            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null) {
                actionBar.setTitle(model.getActionBarTitle());
            }
            Pair<DailyTabFragment, HourlyTabFragment> pf = getPagerFragments();
            DailyTabFragment f1 = pf.first;
            HourlyTabFragment f2 = pf.second;
            f1.updateContent(toDailyViewModel(model.getDailyItems()));
            f2.updateContent(model.getHourlyViewModel());
        }
    }

    private void updateModelAndDisplaySuggestions(List<SuggestionListViewItemModel> items) {
        if (items.isEmpty()) {
            if (suggestionsModel != null) {
                suggestionsModel.clear();
            }
        } else {
            if (suggestionsModel == null) {
                suggestionsModel = new SuggestionsModel(items);
            } else {
                suggestionsModel.setSuggestions(items);
            }
        }
        mSuggestionsAdapter.setItems(items);
        mSuggestionsAdapter.notifyDataSetChanged();
    }

    private void enableDisableSwipeRefresh(boolean enabled) {
        if (mSwipeRefreshLayout != null) {
            mSwipeRefreshLayout.setEnabled(enabled);
        }
    }

    private void setSearchActive(boolean searchActive) {
        LinearLayout linearLayout = findViewById(R.id.suggestionsContainer);
        linearLayout.setVisibility(searchActive ? View.VISIBLE : View.GONE);

        Pair<DailyTabFragment, HourlyTabFragment> pf = getPagerFragments();
        pf.first.setEnabled(!searchActive);
        pf.second.setEnabled(!searchActive);
        mViewPager.setEnabled(!searchActive);
        enableDisableSwipeRefresh(!searchActive);
    }

    // RequestCallback -----------------------------------------------------------------------------

    @Override
    public void handleResult(@TaskAction String taskAction, TaskResult result, Bundle nextTask) {
        int messageId = -1;
        switch (result.getCode()) {
            case TaskResult.Code.SUCCESS: {
                if (TaskAction.GET_RSS_BODY_BY_RSS_ID.equals(taskAction)) {
                    RssChannel channel = (RssChannel) result.getContent();
                    model = convertToViewModel(channel);
                    storeTimeToLive(channel.getTtl());
                    if (!nextTask.containsKey(Constants.BUNDLE_KEY_NEXT_TASK_ACTION)) {
                        // If subsequent action is not demanded - show fetched result at once,
                        // otherwise view model will be updated after subsequent action
                        storeLastUpdateTime(System.currentTimeMillis());
                        updateUIFromViewModel(model);
                    }
                } else if (TaskAction.SEARCH_CITY_BY_NAME.equals(taskAction)) {
                    List<City> cityList = (List<City>) result.getContent();
                    updateModelAndDisplaySuggestions(convertToItemModel(cityList));
                } else if (TaskAction.GET_RSS_ID_BY_CITY_ID.equals(taskAction)) {
                    String rssId = (String) result.getContent();
                    storeSelected(rssId);
                } else if (TaskAction.GET_HOURLY_FORECAST_BY_CITY_ID.equals(taskAction)) {
                    HourlyForecast hf = (HourlyForecast) result.getContent();
                    if (model == null) {
                        model = new MainActivityViewModel();
                    }
                    // Hourly forecast is provided for cities of Russia only
                    if (hf.isEmpty()) {
                        model.setHourlyViewModel(Collections.emptyList());
                    } else {
                        model.setHourlyViewModel(toHourlyViewModel(this, hf));
                    }
                    storeLastUpdateTime(System.currentTimeMillis());
                    updateUIFromViewModel(model);
                }
            }
            break;
            case TaskResult.Code.NULL_CONTENT: {
                if (TaskAction.SEARCH_CITY_BY_NAME.equals(taskAction)) {
                    SuggestionListViewItemModel emptyValue = new SuggestionListViewItemModelImpl(
                            Constants.EMPTY_CITY_ID, getString(R.string.toast_no_content), "");
                    updateModelAndDisplaySuggestions(Collections.singletonList(emptyValue));
                } else {
                    messageId = R.string.toast_no_content;
                }
            }
            break;
            case TaskResult.Code.NETWORK_ISSUE: {
                messageId = R.string.toast_network_issue;
            }
            break;
            case TaskResult.Code.TIMEOUT_EXPIRED: {
                messageId = R.string.toast_timeout_expired;
            }
            break;
            case TaskResult.Code.ERROR: // consecutive
            default:
                messageId = R.string.toast_update_error;
        }

        boolean keepRefreshingState = false;

        // Manage subsequent actions
        if (TaskResult.Code.SUCCESS == result.getCode() && nextTask.containsKey(Constants.BUNDLE_KEY_NEXT_TASK_ACTION)) {
            String nextTaskAction = nextTask.getString(Constants.BUNDLE_KEY_NEXT_TASK_ACTION);
            if (TaskAction.GET_HOURLY_FORECAST_BY_CITY_ID.equals(nextTaskAction)) {
                keepRefreshingState = true;
                String cityId = nextTask.getString(Constants.BUNDLE_KEY_CITY_ID);
                mNetworkFragment.startTask(TaskAction.GET_HOURLY_FORECAST_BY_CITY_ID, cityId);
            } else if (TaskAction.GET_RSS_BODY_BY_RSS_ID.equals(nextTaskAction)) {
                if (TaskAction.GET_RSS_ID_BY_CITY_ID.equals(taskAction)) {
                    keepRefreshingState = true;

                    String cityId = nextTask.getString(Constants.BUNDLE_KEY_CITY_ID);
                    storeCityId(cityId);

                    Bundle next = new Bundle();
                    next.putString(Constants.BUNDLE_KEY_TRIGGER, "callChainAfterGetRssId");
                    next.putString(Constants.BUNDLE_KEY_NEXT_TASK_ACTION, TaskAction.GET_HOURLY_FORECAST_BY_CITY_ID);
                    next.putString(Constants.BUNDLE_KEY_CITY_ID, cityId);

                    mNetworkFragment.startTask(TaskAction.GET_RSS_BODY_BY_RSS_ID, (String) result.getContent(), next);
                }
            }
        }

        if (!keepRefreshingState) {
            isRequestRunning = false;
            mSwipeRefreshLayout.setRefreshing(false);

            if (messageId != -1) {
                Toast.makeText(this, messageId, Toast.LENGTH_SHORT).show();
            }
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
                if (skipQueryTextChangeListener) {
                    return true;
                }
                String query = newText.trim();
                if (query.length() >= getResources().getInteger(R.integer.minSuggestLenThreshold)) {
                    isRequestRunning = true;
                    mNetworkFragment.startTask(TaskAction.SEARCH_CITY_BY_NAME, query);
                } else {
                    mNetworkFragment.interruptTask();
                    isRequestRunning = false;
                    updateModelAndDisplaySuggestions(Collections.emptyList());
                }
                return true;
            }
        });
        mSearchMenuItem = menu.findItem(R.id.action_search);
        mSearchMenuItem.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionExpand(MenuItem menuItem) {
                if (isRequestRunning) {
                    return false;
                }
                setSearchActive(true);
                return true;
            }

            @Override
            public boolean onMenuItemActionCollapse(MenuItem menuItem) {
                setSearchActive(false);
                updateModelAndDisplaySuggestions(Collections.emptyList());
                return true;
            }
        });

        if (suggestionsModel != null && !suggestionsModel.isEmpty()) {
            skipQueryTextChangeListener = true;

            mSearchView.setIconified(false);
            mSearchMenuItem.expandActionView();

            mSuggestionsAdapter.setItems(suggestionsModel.getSuggestions());
            mSuggestionsAdapter.notifyDataSetChanged();

            mSearchView.setQuery(suggestionsModel.getQuery(), false);

            skipQueryTextChangeListener = false;
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_refresh:
                fetchRssBodyThenHourlyData(getStoredRssId(), getStoredCityId());
                return true;
            case R.id.action_switch_ui_mode: {
                int mode = getSharedPreferences().getInt(Constants.PARAMS_UI_MODE,
                        AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                int[] array = getResources().getIntArray(R.array.uiModeValues);
                int selectedModeIdx = -1;
                for (int i = 0; i < array.length; i++) {
                    if (array[i] == mode) {
                        selectedModeIdx = i;
                        break;
                    }
                }
                new AlertDialog.Builder(this)
                        .setTitle(R.string.dots_menu_switch_ui_mode)
                        .setSingleChoiceItems(
                                getResources().getStringArray(R.array.uiModes),
                                selectedModeIdx,
                                (dialog, which) -> {
                                    int selectedMode = array[which];
                                    getSharedPreferences().edit()
                                            .putInt(Constants.PARAMS_UI_MODE, selectedMode)
                                            .apply();
                                    AppCompatDelegate.setDefaultNightMode(selectedMode);
                                })
                        .setPositiveButton("OK", null)
                        .show();
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void collapseSearchView() {
        mSearchView.setQuery("", false);
        mSearchView.setIconified(true);
        mSearchMenuItem.collapseActionView();
    }

    //----------------------------------------------------------------------------------------------

    private SharedPreferences getSharedPreferences() {
        return getSharedPreferences(Constants.PRIVATE_STORAGE_NAME, Context.MODE_PRIVATE);
    }

    private String getStoredRssId() {
        return getSharedPreferences().getString(Constants.PARAM_KEY_RSS_ID,
                getString(R.string.default_rss_id));
    }

    private String getStoredCityId() {
        return getSharedPreferences().getString(Constants.PARAMS_KEY_CITY_ID,
                getString(R.string.default_city_id));
    }

    private long getLastUpdateMillis() {
        return getSharedPreferences().getLong(Constants.PARAMS_KEY_LAST_UPDATE_MILLIS, -1);
    }

    private long getTimeToLiveMillis() {
        return getSharedPreferences().getLong(Constants.PARAMS_KEY_TIME_TO_LIVE_MILLIS, -1);
    }

    private boolean isTTLExpired(long currentMillis) {
        long lastUpdate = getLastUpdateMillis();
        long ttl = getTimeToLiveMillis();
        return lastUpdate != -1 && ttl != -1 && (currentMillis - lastUpdate > ttl);
    }

    private MainActivityViewModel convertToViewModel(RssChannel channel) {
        return convertToViewModel(this, channel, CalendarHelper.provideForUTC());
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
                        Date d = DF_SOURCE.get().parse(dateString);
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

                    String dirShort = "";
                    if (dirFull.indexOf('-') != -1) {
                        String[] parts = dirFull.split("-");
                        for (String part : parts) {
                            dirShort += Character.toUpperCase(part.charAt(0));
                        }
                    } else {
                        dirShort += Character.toUpperCase(dirFull.charAt(0));
                    }
                    String arrow = windDirectionArrow(context.getResources()
                            .getStringArray(R.array.windDirectionArrows), dirShort);
                    forecastItem.setWind(dirShort + " " + arrow + " " + velocity);
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

    private static List<SuggestionListViewItemModel> convertToItemModel(List<City> cityList) {
        return Stream.of(cityList)
                .map(city -> new SuggestionListViewItemModelImpl(city.getId(), city.getTitle(), city.getPath()))
                .collect(Collectors.toList());
    }

    private static boolean tryParseDayMonthUTC(String title, Calendar itemDayUTC) {
        try {
            itemDayUTC.setTime(DF_LIST_ITEM_TITLE.get().parse(title));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static List<HourlyListViewItemModel> toHourlyViewModel(Context context,
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
        Set<Long> setOfDaysAsMillis = hf.getDaysAsMillis();
        List<Long> listOfDaysAsMillis = Stream.of(setOfDaysAsMillis).toList();
        Collections.sort(listOfDaysAsMillis);
        for (Long day : listOfDaysAsMillis) {
            dtOfEntry.setTimeInMillis(day);
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
            String title = prefix + ", " + DF_LIST_ITEM_TITLE.get().format(dtOfEntry.getTime());

            hourlyListViewItemModels.add(new HourlyListViewItemDivider(title));
            List<HourlyDataWrapper> hourlyData = hf.getHourlyDataForDay(day);
            hourlyListViewItemModels.addAll(toHourlyContent(context, hourlyData));
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
                    String arrow = windDirectionArrow(context.getResources()
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

    private static List<DailyListViewItemModel> toDailyViewModel(List<DailyForecastItem> items) {
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

    private static String windDirectionArrow(String[] arrayOfArrows, String windDirectionString) {
        if (WIND_DIRECTIONS.size() != arrayOfArrows.length) {
            throw new IllegalArgumentException("Inconsistent wind directions data");
        }
        int index = WIND_DIRECTIONS.indexOf(windDirectionString);
        if (index != -1) {
            return arrayOfArrows[index];
        }
        return windDirectionString;
    }
}
