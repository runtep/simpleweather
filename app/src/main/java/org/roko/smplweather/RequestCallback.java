package org.roko.smplweather;

import android.net.NetworkInfo;
import android.os.Bundle;

import androidx.annotation.NonNull;

public interface RequestCallback<T> {

    void handleResult(String taskAction, @NonNull T result, Bundle bundle);

    NetworkInfo getActiveNetworkInfo();
}
