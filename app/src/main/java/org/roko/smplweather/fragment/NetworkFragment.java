package org.roko.smplweather.fragment;

import android.content.ContentValues;
import android.content.Context;
import android.os.Bundle;

import org.roko.smplweather.tasks.GenericTask;
import org.roko.smplweather.TaskResult;
import org.roko.smplweather.RequestCallback;
import org.roko.smplweather.tasks.TaskAction;
import org.roko.smplweather.tasks.TaskCallContext;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

public class NetworkFragment extends Fragment {

    private static final String STATE_KEY_SESSION_STORAGE =
            "org.roko.smplweather.fragment.NetworkFragment.SESSION_STORAGE";
    private static final String ARG_KEY_URL = "UrlKey";
    private static final String ARG_KEY_PAGES = "PagesKey";

    public static final String TAG = "NetworkFragment";

    private String urlString;
    private String[] lightweightPages;
    private RequestCallback<TaskResult> callback;
    private GenericTask task;
    private ContentValues sessionStorage = new ContentValues();

    public static NetworkFragment getInstance(FragmentManager fragmentManager, String url,
                                              String[] lightWeightPages) {
        Fragment existing = fragmentManager.findFragmentByTag(TAG);
        if (existing != null) {
            return (NetworkFragment) existing;
        }
        NetworkFragment networkFragment = new NetworkFragment();
        Bundle args = new Bundle();
        args.putString(ARG_KEY_URL, url);
        args.putStringArray(ARG_KEY_PAGES, lightWeightPages);
        networkFragment.setArguments(args);
        fragmentManager.beginTransaction().add(networkFragment, TAG).commit();
        return networkFragment;
    }

    public NetworkFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        urlString = getArguments().getString(ARG_KEY_URL);
        lightweightPages = getArguments().getStringArray(ARG_KEY_PAGES);
        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey(STATE_KEY_SESSION_STORAGE)) {
                sessionStorage = savedInstanceState.getParcelable(STATE_KEY_SESSION_STORAGE);
            }
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(STATE_KEY_SESSION_STORAGE, sessionStorage);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void onAttach(@NonNull Context context) {
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

    public void startTask(@TaskAction String taskAction, String queryString) {
        startTask(taskAction, queryString, Bundle.EMPTY);
    }

    public void startTask(@TaskAction String taskAction, String queryString, Bundle nextTask) {
        interruptTask();
        task = new GenericTask(callback, TaskCallContext.of(urlString, lightweightPages,
                sessionStorage, taskAction, nextTask));
        task.execute(queryString);
    }

    public void interruptTask() {
        if (task != null) {
            task.cancel(true);
        }
    }
}
