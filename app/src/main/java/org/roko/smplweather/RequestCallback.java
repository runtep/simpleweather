package org.roko.smplweather;

import android.net.NetworkInfo;
import android.support.annotation.NonNull;

public interface RequestCallback<T> {

    void handleResult(@NonNull T result);

    NetworkInfo getActiveNetworkInfo();
}
