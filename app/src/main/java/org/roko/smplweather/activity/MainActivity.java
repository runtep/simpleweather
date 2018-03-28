package org.roko.smplweather.activity;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.annotation.NonNull;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.roko.smplweather.MyAdapter;
import org.roko.smplweather.R;
import org.roko.smplweather.RequestCallback;
import org.roko.smplweather.RssReadResult;
import org.roko.smplweather.fragment.NetworkFragment;
import org.roko.smplweather.model.ListItemViewModel;
import org.roko.smplweather.model.MainActivityVewModel;
import org.roko.smplweather.model.RssChannel;
import org.roko.smplweather.model.RssItem;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity implements RequestCallback<RssReadResult> {

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

    private NetworkFragment mNetworkFragment;

    private boolean isRequestRunning = false;

    private ListView mListView;
    private MyAdapter myAdapter;
    private TextView mFooter;

    private MainActivityVewModel model;

    private Pattern patternActualDate =
            Pattern.compile("(^\\D+)\\s(\\d{2}\\.\\d{2}\\.\\d{4})\\D+(\\d{2}\\:\\d{2}\\(*.+\\))");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle("");
        }

        myAdapter = new MyAdapter(this);
        mListView = (ListView) findViewById(R.id.listView);
        mListView.setEmptyView(findViewById(R.id.emptyElement));
        mListView.setAdapter(myAdapter);

        mFooter = (TextView) findViewById(R.id.footer);

        String urlString = getResources().getString(R.string.main_url);
        mNetworkFragment = NetworkFragment.getInstance(getSupportFragmentManager(), urlString);

        MainActivityVewModel persistedModel =
                (MainActivityVewModel) getLastCustomNonConfigurationInstance();
        if (persistedModel != null) {
            this.model = persistedModel;
            updateUIFromViewModel(this.model);
        }
    }

    @Override
    public Object onRetainCustomNonConfigurationInstance() {
        return model;
    }

    private void loadRss() {
        if (!isRequestRunning) {
            isRequestRunning = true;
            mNetworkFragment.startTask();
        }
    }

    private void updateUIFromViewModel(MainActivityVewModel model) {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(model.getActionBarTitle());
        }
        myAdapter.setItems(model.getItems());
        myAdapter.notifyDataSetChanged();
        mFooter.setText(model.getFooter());
    }

    // RequestCallback -----------------------------------------------------------------------------

    @Override
    public void handleResult(@NonNull RssReadResult result) {
        final int messageId;
        switch (result.getCode()) {
            case RssReadResult.Code.SUCCESS: {
                model = convertToViewModel(result.getContent());
                updateUIFromViewModel(model);
                messageId = R.string.toast_update_completed;
            }
            break;
            case RssReadResult.Code.NULL_CONTENT: {
                messageId = R.string.toast_no_content;
            }
            break;
            case RssReadResult.Code.NETWORK_ISSUE: {
                messageId = R.string.toast_network_issue;
            }
            break;
            default:
                messageId = R.string.toast_update_error;
        }

        Toast.makeText(this, messageId, Toast.LENGTH_SHORT).show();

        isRequestRunning = false;
    }

    @Override
    public NetworkInfo getActiveNetworkInfo() {
        ConnectivityManager manager =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        return manager.getActiveNetworkInfo();
    }

    // Panel menu initialization -------------------------------------------------------------------

    @Override
    public boolean onCreatePanelMenu(int featureId, Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return super.onCreatePanelMenu(featureId, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_refresh:
                loadRss();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    //----------------------------------------------------------------------------------------------

    private MainActivityVewModel convertToViewModel(RssChannel channel) {
        String hgCol = getResources().getString(R.string.const_hg_cl);
        String city = "", lastUpdate = "";
        List<ListItemViewModel> items = new ArrayList<>(channel.getItems().size());
        for (RssItem rssItem : channel.getItems()) {
            String rssTitle = rssItem.getTitle();
            int pos = rssTitle.lastIndexOf(',');
            String day = "";
            if (pos != -1) {
                if (city.isEmpty()) {
                    city = rssTitle.substring(0, pos);
                }
                day = rssTitle.substring(pos + 1).trim();
            }

            String rssDesc = rssItem.getDescription();
            StringBuilder details = new StringBuilder(rssDesc.replaceAll("\\. +", "\\.\n"));

            pos = details.lastIndexOf(hgCol);
            if (pos != -1) {
                details.insert(pos + hgCol.length(), '\n');
            }

            String rssSource = rssItem.getSource();
            pos = rssSource.lastIndexOf(',');
            if (pos != -1 && lastUpdate.isEmpty()) {
                String dateInfo = rssSource.substring(pos + 1);

                Matcher m = patternActualDate.matcher(dateInfo);
                if (m.matches()) {
                    String dateString = m.group(2) + " " + m.group(3);
                    try {
                        DF_SOURCE.get().setTimeZone(TimeZone.getTimeZone("UTC"));
                        Date d = DF_SOURCE.get().parse(dateString);

                        DF_TARGET.get().setTimeZone(TimeZone.getDefault());
                        lastUpdate = DF_TARGET.get().format(d);
                    } catch (ParseException e) {
                        lastUpdate = dateString;
                    }
                }
            }

            items.add(new ListItemViewModel(day, details.toString()));
        }

        String footer = getResources().getString(R.string.footer_actuality) + " " + lastUpdate;

        MainActivityVewModel model = new MainActivityVewModel();
        model.setActionBarTitle(city);
        model.setItems(items);
        model.setFooter(footer);

        return model;
    }
}
