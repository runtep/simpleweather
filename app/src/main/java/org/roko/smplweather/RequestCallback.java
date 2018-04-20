package org.roko.smplweather;

import android.net.NetworkInfo;
import android.support.annotation.NonNull;

public interface RequestCallback<T> {

    void handleResult(String taskAction, @NonNull T result);

    NetworkInfo getActiveNetworkInfo();
}
