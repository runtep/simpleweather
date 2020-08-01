package org.roko.smplweather;

import android.app.Application;
import android.content.Context;

import androidx.appcompat.app.AppCompatDelegate;

public class ExtApplication extends Application {

    @Override
    public void onCreate() {
        int uiMode = getSharedPreferences(Constants.PRIVATE_STORAGE_NAME, Context.MODE_PRIVATE)
                .getInt(Constants.PARAMS_UI_MODE, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        AppCompatDelegate.setDefaultNightMode(uiMode);
        super.onCreate();
    }
}
