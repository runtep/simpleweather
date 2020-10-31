package org.roko.smplweather.tasks;

import android.content.ContentValues;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;

import org.roko.smplweather.TaskResult;
import org.roko.smplweather.RequestCallback;
import org.roko.smplweather.model.xml.RssResponse;
import org.roko.smplweather.model.json.SearchCityResponse;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.List;

import androidx.annotation.Nullable;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;
import retrofit2.converter.scalars.ScalarsConverterFactory;
import retrofit2.converter.simplexml.SimpleXmlConverterFactory;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Query;

public class GenericTask extends AsyncTask<String, Void, ResponseWrapper> {

    private RequestCallback<TaskResult> callback;
    private final TaskCallContext taskCallContext;

    public GenericTask(RequestCallback<TaskResult> callback, TaskCallContext taskCallContext) {
        setCallback(callback);
        this.taskCallContext = taskCallContext;
    }

    public void setCallback(RequestCallback<TaskResult> callback) {
        this.callback = callback;
    }

    @Override
    protected void onPreExecute() {
        if (callback != null) {
            NetworkInfo networkInfo = callback.getActiveNetworkInfo();
            if (networkInfo == null || !networkInfo.isConnected() ||
                    (networkInfo.getType() != ConnectivityManager.TYPE_WIFI
                            && networkInfo.getType() != ConnectivityManager.TYPE_MOBILE)) {
                callback.handleResult(taskCallContext.getAction(),
                        new TaskResult(TaskResult.Code.NETWORK_ISSUE),
                        Bundle.EMPTY);
                cancel(true);
            }
        }
    }

    protected ResponseWrapper doInBackground(String... args) {
        ResponseWrapper res = null;
        if (!isCancelled() && args != null && args.length > 0) {
            String query = args[0];
            res = processBackgroundRequest(query, taskCallContext);
        }
        return res;
    }

    private ResponseWrapper processBackgroundRequest(final String query, TaskCallContext ctx) {
        final String url = ctx.getUrl();
        final @TaskAction String action = ctx.getAction();
        final ContentValues sessionStorage = ctx.getSessionStorage();
        ResponseWrapper responseWrapper = null;
        Retrofit.Builder retrofitBuilder = new Retrofit.Builder().baseUrl(url);
        if (TaskAction.GET_HOURLY_FORECAST_BY_CITY_ID.equals(action)) {
            retrofitBuilder.addConverterFactory(ScalarsConverterFactory.create());
        } else if (TaskAction.GET_RSS_BODY_BY_RSS_ID.equals(action)) {
            retrofitBuilder.addConverterFactory(SimpleXmlConverterFactory.create());
        } else {
            retrofitBuilder.addConverterFactory(JacksonConverterFactory.create());
        }
        Retrofit retrofit = retrofitBuilder.build();
        ApiService service = retrofit.create(ApiService.class);
        RequestProcessor processor = new RequestProcessor();
        switch (action) {
            case TaskAction.GET_RSS_BODY_BY_RSS_ID: {
                responseWrapper = processor.processReadRssRequest(service, query);
            }
            break;
            case TaskAction.SEARCH_CITY_BY_NAME: {
                responseWrapper = processor.processSearchCityRequest(service, query);
            }
            break;
            case TaskAction.GET_RSS_ID_BY_CITY_ID: {
                try {
                    // cookies are necessary to fetch rss feed id by cityId
                    processor.checkCookies(url, ctx.getPages(), sessionStorage);
                    responseWrapper = processor.processGetRssIdByCityId(service, query, sessionStorage);
                } catch (IOException e) {
                    responseWrapper = new ResponseWrapper(e);
                }
            }
            break;
            case TaskAction.GET_HOURLY_FORECAST_BY_CITY_ID: {
                try {
                    processor.checkCookies(url, ctx.getPages(), sessionStorage);
                    responseWrapper = processor.processGetHourlyForecastByCityId(service, query, sessionStorage);
                } catch (IOException e) {
                    responseWrapper = new ResponseWrapper(e);
                }
            }
            break;
        }

        return responseWrapper;
    }

    @Override
    protected void onPostExecute(@Nullable ResponseWrapper result) {
        if (result != null && callback != null) {
            TaskResult taskResult;
            if (result.exception != null) {
                if (result.exception instanceof SocketTimeoutException) {
                    taskResult = new TaskResult(TaskResult.Code.TIMEOUT_EXPIRED);
                } else {
                    taskResult = new TaskResult(TaskResult.Code.ERROR,
                            result.exception.toString());
                }
            } else {
                if (result.content != null) {
                    taskResult = new TaskResult(TaskResult.Code.SUCCESS, result.content);
                } else {
                    taskResult = TaskResult.nullContent();
                }
            }
            callback.handleResult(taskCallContext.getAction(), taskResult, taskCallContext.getNextTask());
        }
    }

    interface ApiService {

        @GET("rss/forecasts/index.php")
        Call<RssResponse> getRssByCityId(@Query("s") String cityId);

        @POST("hmc-output/select/select_s2.php?q[_type]=query&id_lang=1")
        Call<SearchCityResponse> searchCityByName(@Query("q[term]") String name);

        @SuppressWarnings("rawtypes")
        @FormUrlEncoded()
        @POST("hmc-output/forecast/tab_0.php")
        Call<List> getCityDetailsById(@Field("id_city") String cityId, @Header("Cookie") String cookie);

        @FormUrlEncoded()
        @POST("hmc-output/forecast/gr.php")
        Call<String> getHourlyForecastJsString(@Field("id_city") String cityId, @Header("Cookie") String cookie);
    }
}
