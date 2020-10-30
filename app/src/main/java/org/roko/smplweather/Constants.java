package org.roko.smplweather;

public final class Constants {
    private Constants() {}

    public static final String PRIVATE_STORAGE_NAME = "org.roko.smplweather.STORAGE";

    public static final String PARAM_KEY_RSS_ID = "rssId";
    public static final String PARAMS_KEY_LAST_UPDATE_MILLIS = "lastUpdateMillis";
    public static final String PARAMS_KEY_TIME_TO_LIVE_MILLIS = "timeToLiveMillis";
    public static final String PARAMS_KEY_CITY_ID = "cityId";
    public static final String PARAMS_UI_MODE = "uiMode";

    public static final String BUNDLE_KEY_MAIN_ACTIVITY_VIEW_MODEL = "mainActivityViewModel";
    public static final String BUNDLE_KEY_SUGGESTIONS_MODEL = "suggestionsModel";
    public static final String BUNDLE_KEY_FRAGMENT_STATE = "fragmentState";

    public static final String BUNDLE_KEY_NEXT_TASK_ACTION = "nextTaskAction";
    public static final String BUNDLE_KEY_CITY_ID = "cityId";
    public static final String BUNDLE_KEY_TRIGGER = "trigger";

    public static final String RU_TODAY = "Сегодня";
    public static final String RU_TOMORROW = "Завтра";

    public static class ForecastFields {
        public static final String DATE_MILLIS = "dateMs";
        public static final String TEMPERATURE_CELSIUS = "tempValCelsius";
        public static final String WIND_DIR_NAME = "windDirName";
        public static final String WIND_SPEED_METERS = "windSpeedMeters";
        public static final String HUMIDITY_PERCENT = "humidityPercent";
        public static final String PRECIP_MILLIMETERS = "precipValMill";
        public static final String PRECIP_PROBABILITY_PERCENT = "precipProbPercent";
        public static final String FORECAST_DESCR = "description";
    }

    public static class PayloadArrayNames {
        public static final String ARR_TEMPERATURE = "arr_temperature";
        public static final String ARR_WIND_DIR_NAME = "arr_wind_dir_name";
        public static final String ARR_WIND_SPEED = "arr_wind_speed";
        public static final String ARR_HUMIDITY = "arr_humidity";
        public static final String ARR_PRECIP_VAL = "arr_precip_val";
        public static final String ARR_PRECIP_VER = "arr_precip_ver";
        public static final String ARR_PHENOMENON_NAME = "arr_phenomenon_name";
    }
}
