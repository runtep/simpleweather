package org.roko.smplweather;

import android.net.NetworkInfo;
import android.os.Bundle;

import org.roko.smplweather.tasks.TaskAction;

public interface RequestCallback<T> {

    void handleResult(@TaskAction String taskAction, T result, Bundle nextTask);

    NetworkInfo getActiveNetworkInfo();
}
