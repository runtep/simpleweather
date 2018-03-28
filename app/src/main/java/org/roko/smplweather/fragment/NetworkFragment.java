package org.roko.smplweather.fragment;


import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;

import org.roko.smplweather.RssReadResult;
import org.roko.smplweather.RssReadTask;
import org.roko.smplweather.RequestCallback;

public class NetworkFragment extends Fragment {

    private static final String URL_KEY = "UrlKey";

    public static final String TAG = "NetworkFragment";

    private String urlString;
    private RequestCallback<RssReadResult> callback;
    private RssReadTask task;

    public static NetworkFragment getInstance(FragmentManager fragmentManager, String url) {
        NetworkFragment networkFragment = new NetworkFragment();
        Bundle args = new Bundle();
        args.putString(URL_KEY, url);
        networkFragment.setArguments(args);
        fragmentManager.beginTransaction().add(networkFragment, TAG).commit();
        return networkFragment;
    }

    public NetworkFragment() {}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        urlString = getArguments().getString(URL_KEY);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void onAttach(Context context) {
        super.onAttach(context);
        callback = (RequestCallback<RssReadResult>) context;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        callback = null;
    }

    @Override
    public void onDestroy() {
        interruptTask();
        super.onDestroy();
    }

    public void startTask() {
        interruptTask();
        task = new RssReadTask(callback);
        task.execute(urlString);
    }

    public void interruptTask() {
        if (task != null) {
            task.cancel(true);
        }
    }
}
