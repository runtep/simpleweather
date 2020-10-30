package org.roko.smplweather.tasks;

import androidx.annotation.StringDef;

@StringDef({TaskAction.GET_RSS_BODY_BY_RSS_ID, TaskAction.GET_RSS_ID_BY_CITY_ID,
        TaskAction.SEARCH_CITY_BY_NAME, TaskAction.GET_HOURLY_FORECAST_BY_CITY_ID})
public @interface TaskAction {

    String GET_RSS_BODY_BY_RSS_ID = "read_rss_by_id";
    String SEARCH_CITY_BY_NAME = "search_city_by_name";
    String GET_RSS_ID_BY_CITY_ID = "get_rss_id_by_city_id";
    String GET_HOURLY_FORECAST_BY_CITY_ID = "get_hourly_forecast";
}
