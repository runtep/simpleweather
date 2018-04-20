package org.roko.smplweather.activity;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.EditText;
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
import org.roko.smplweather.model.ListItemViewModel;
import org.roko.smplweather.model.SearchActivityViewModel;
import org.roko.smplweather.tasks.TaskAction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class SearchResultsActivity extends AppCompatActivity implements RequestCallback<TaskResult> {

    private NetworkFragment mNetworkFragment;
    private MyAdapter myAdapter;
    private EditText mEditText;

    private boolean isRequestRunning;
    private SearchActivityViewModel viewModel;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.search_activity);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle(getString(R.string.search_title));
        }

        myAdapter = new MyAdapter(this);
        ListView mListView = (ListView) findViewById(R.id.search_results_lv);
        mListView.setEmptyView(findViewById(R.id.search_results_empty));
        mListView.setAdapter(myAdapter);
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                onListItemClick(adapterView, view, position, id);
            }
        });
        mEditText = (EditText) findViewById(R.id.search_query);
        mEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                return onEditorActionHandler(v, actionId);
            }
        });

        String urlString = getString(R.string.url_main);
        mNetworkFragment = NetworkFragment.getInstance(getSupportFragmentManager(), urlString);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        HashMap m = (HashMap) mNetworkFragment.getSessionStorage();
        if (!m.isEmpty()) {
            outState.putSerializable(Constants.BUNDLE_KEY_SESSION_STORAGE, m);
        }
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        HashMap m = (HashMap) savedInstanceState.getSerializable(Constants.BUNDLE_KEY_SESSION_STORAGE);
        if (m != null && !m.isEmpty()) {
            mNetworkFragment.setSessionStorage(m);
        }
    }

    private boolean onEditorActionHandler(TextView view, int actionId) {
        switch (actionId) {
            case EditorInfo.IME_ACTION_SEARCH: {
                invokeSearch(view);
                return true;
            }
        }
        return false;
    }

    public void onSearchButtonClick(View view) {
        invokeSearch(mEditText);
    }

    private void invokeSearch(TextView textView) {
        if (!isRequestRunning) {
            String query = textView.getText().toString();
            if (!query.isEmpty()) {
                isRequestRunning = true;
                mNetworkFragment.startTask(TaskAction.SEARCH_CITY_BY_NAME, query);
            }
        }
    }

    private void onListItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        ListItemViewModel item = (ListItemViewModel) adapterView.getItemAtPosition(position);
        String cityId = item.getId();

        if (!isRequestRunning) {
            isRequestRunning = true;
            mNetworkFragment.startTask(TaskAction.GET_RSS_ID_BY_CITY_ID, cityId);
        }
    }

    // ---------------------------------------------------------------------------------------------
    @Override
    public void handleResult(String taskAction, @NonNull TaskResult result) {
        if (TaskResult.Code.SUCCESS == result.getCode()) {
            if (TaskAction.SEARCH_CITY_BY_NAME.equals(taskAction)) {
                @SuppressWarnings("unchecked")
                List<City> cityList = (List<City>) result.getContent();
                viewModel = convert(cityList);
                myAdapter.setItems(viewModel.getItems());
                myAdapter.notifyDataSetChanged();
            } else if (TaskAction.GET_RSS_ID_BY_CITY_ID.equals(taskAction)) {
                String rssId = (String) result.getContent();

                Intent intent = new Intent(this, MainActivity.class);
                intent.setAction(Constants.INTENT_ACTION_SELECT_CITY);
                intent.putExtra(Constants.PARAM_KEY_RSS_ID, rssId);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

                startActivity(intent);
            }
        } else {
            final int messageId;
            switch (result.getCode()) {
                case TaskResult.Code.NULL_CONTENT:
                    messageId = R.string.toast_no_content;
                    break;
                case TaskResult.Code.NETWORK_ISSUE:
                    messageId = R.string.toast_network_issue;
                    break;
                default:
                    messageId = R.string.toast_update_error;
            }
            Toast.makeText(this, messageId, Toast.LENGTH_SHORT).show();
        }
        isRequestRunning = false;
    }

    @Override
    public NetworkInfo getActiveNetworkInfo() {
        ConnectivityManager manager =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        return manager.getActiveNetworkInfo();
    }
    // ---------------------------------------------------------------------------------------------

    private SearchActivityViewModel convert(List<City> cityList) {
        SearchActivityViewModel viewModel = new SearchActivityViewModel();
        if (!cityList.isEmpty()) {
            List<ListItemViewModel> items = new ArrayList<>(cityList.size());
            for (City city : cityList) {
                String title = city.getTitle();
                String subtitle = city.getPath();
                ListItemViewModel item = new ListItemViewModel(city.getId(), title, subtitle);
                items.add(item);
            }
            viewModel.setItems(items);
        }
        return viewModel;
    }
}
