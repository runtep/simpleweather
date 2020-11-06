package org.roko.smplweather.activity;

import android.app.SearchManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

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
import org.roko.smplweather.model.HourlyForecast;
import org.roko.smplweather.model.MainActivityViewModel;
import org.roko.smplweather.model.SuggestionListViewItemModel;
import org.roko.smplweather.model.SuggestionListViewItemModelImpl;
import org.roko.smplweather.model.SuggestionsModel;
import org.roko.smplweather.model.xml.RssChannel;
import org.roko.smplweather.tasks.TaskAction;
import org.roko.smplweather.utils.ConvertingHelper;

import java.util.Collections;
import java.util.List;

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
            f1.updateContent(ConvertingHelper.toDailyViewModel(model.getDailyItems()));
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
                    model = ConvertingHelper.convertToViewModel(this, channel);
                    storeTimeToLive(channel.getTtl());
                    if (!nextTask.containsKey(Constants.BUNDLE_KEY_NEXT_TASK_ACTION)) {
                        // If subsequent action is not demanded - show fetched result at once,
                        // otherwise view model will be updated after subsequent action
                        storeLastUpdateTime(System.currentTimeMillis());
                        updateUIFromViewModel(model);
                    }
                } else if (TaskAction.SEARCH_CITY_BY_NAME.equals(taskAction)) {
                    List<City> cityList = (List<City>) result.getContent();
                    updateModelAndDisplaySuggestions(ConvertingHelper.toSuggestionsViewModel(cityList));
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
                        model.setHourlyViewModel(ConvertingHelper.toHourlyViewModel(this, hf));
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
}
