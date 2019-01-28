package org.roko.smplweather.fragment;


import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;

import org.roko.smplweather.tasks.GenericTask;
import org.roko.smplweather.TaskResult;
import org.roko.smplweather.RequestCallback;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class NetworkFragment extends Fragment {

    private static final String URL_KEY = "UrlKey";

    public static final String TAG = "NetworkFragment";

    private String urlString;
    private RequestCallback<TaskResult> callback;
    private GenericTask task;
    private Map<String, Object> sessionStorage = new HashMap<>();

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
        callback = (RequestCallback<TaskResult>) context;
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

    public void startTask(String actionString, String queryString) {
        startTask(actionString, queryString, Bundle.EMPTY);
    }

    public void startTask(String actionString, String queryString, Bundle bundle) {
        interruptTask();
        task = new GenericTask(callback);
        task.setSessionStorage(sessionStorage);
        task.setBundle(bundle);
        task.execute(urlString, actionString, queryString);
    }

    public void interruptTask() {
        if (task != null) {
            task.cancel(true);
        }
    }

    // ---------------------------------------------------------------------------------------------

    public Serializable getSessionStorage() {
        return new HashMap<>(sessionStorage);
    }

    public void setSessionStorage(@NonNull Map<String, Object> sessionStorage) {
        this.sessionStorage = new HashMap<>(sessionStorage);
    }
}
