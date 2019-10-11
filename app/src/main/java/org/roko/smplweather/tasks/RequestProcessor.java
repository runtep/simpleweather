package org.roko.smplweather.tasks;

import org.roko.smplweather.model.City;
import org.roko.smplweather.model.HourlyForecast;
import org.roko.smplweather.model.json.Result;
import org.roko.smplweather.model.json.SearchCityResponse;
import org.roko.smplweather.model.xml.RssChannel;
import org.roko.smplweather.model.xml.RssResponse;
import org.roko.smplweather.utils.DailyForecastParser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import retrofit2.Call;
import retrofit2.Response;

final class RequestProcessor {
    private static final String STORAGE_KEY_COOKIES = "some_cookies";
    private static final String HEADER_KEY_SET_COOKIE = "Set-Cookie";
    private static final String PATTERN_EXTRACT_RSS_ID =
            ".?(<a +href=[^<]+</a>[^<]+){0,1}(<a +href=.?\"[^=]+=(\\d+).?\">RSS</a>).?";

    private Random random = new Random();

    ResponseWrapper processReadRssRequest(GenericTask.ApiService service, String queryString) {
        ResponseWrapper requestResult;
        Call<RssResponse> serviceCall = service.getRssByCityId(queryString);
        try {
            Response<RssResponse> response = serviceCall.execute();
            RssChannel channel;
            if (response.isSuccessful()) {
                RssResponse rssResponse = response.body();
                channel = rssResponse.getChannel();
            } else {
                channel = null;
            }
            requestResult = new ResponseWrapper(channel);
        } catch (Exception e) {
            requestResult = new ResponseWrapper(e);
        }
        return requestResult;
    }

    void checkCookies(String url, List<String> pages,
                      Map<String, Object> sessionStorage) throws IOException {
        if (!sessionStorage.containsKey(STORAGE_KEY_COOKIES)) {
            OkHttpClient okHttpClient = new OkHttpClient();
            String targetUrl = randomPage(url, pages);
            Request okRequest = new Request.Builder()
                    .url(targetUrl)
                    .build();
            okhttp3.Call okCall = okHttpClient.newCall(okRequest);
            okhttp3.ResponseBody responseBody = null;
            try {
                okhttp3.Response okResponse = okCall.execute();
                if (okResponse.isSuccessful()) {
                    responseBody = okResponse.body();
                    List<String> cookies = okResponse.headers(HEADER_KEY_SET_COOKIE);
                    if (!cookies.isEmpty()) {
                        StringBuilder cookieValue = new StringBuilder();
                        String div = "";
                        for (String val : cookies) {
                            int semicolon = val.indexOf(';');
                            if (semicolon != -1) {
                                val = val.substring(0, semicolon);
                            }
                            cookieValue.append(div).append(val);
                            div = ";";
                        }
                        sessionStorage.put(STORAGE_KEY_COOKIES, cookieValue.toString());
                    }
                }
            } finally {
                if (responseBody != null) {
                    responseBody.close();
                }
            }
        }
    }

    ResponseWrapper processSearchCityRequest(GenericTask.ApiService service, String queryString) {
        ResponseWrapper requestResult;
        Call<SearchCityResponse> serviceCall = service.searchCityByName(queryString);
        try {
            Response<SearchCityResponse> response = serviceCall.execute();
            List<City> content;
            if (response.isSuccessful()) {
                content = parseCityList(response);
            } else {
                content = null;
            }
            requestResult = new ResponseWrapper(content);
        } catch (Exception e) {
            requestResult = new ResponseWrapper(e);
        }
        return requestResult;
    }

    ResponseWrapper processGetRssIdByCityId(GenericTask.ApiService service, String query,
                                                   Map<String, Object> sessionStorage) {
        String cookieHeader = "";
        if (sessionStorage.containsKey(STORAGE_KEY_COOKIES)) {
            cookieHeader = (String) sessionStorage.get(STORAGE_KEY_COOKIES);
        }

        ResponseWrapper responseWrapper;
        Call<List> serviceCall = service.getCityDetailsById(query, cookieHeader);
        try {
            Response<List> response = serviceCall.execute();
            Object content = null;
            if (response.isSuccessful()) {
                List list = response.body();
                if (list.size() == 9) {
                    String html = list.get(8).toString();
                    Pattern p = Pattern.compile(PATTERN_EXTRACT_RSS_ID);
                    Matcher m = p.matcher(html);
                    if (m.find()) {
                        content = m.group(3);
                    }
                }
            }
            responseWrapper = new ResponseWrapper(content);
        } catch (Exception e) {
            responseWrapper = new ResponseWrapper(e);
        }
        return responseWrapper;
    }

    ResponseWrapper processGetHourlyForecastByCityId(GenericTask.ApiService service,
                                                            String query,
                                                            Map<String, Object> sessionStorage) {
        String cookieHeader = "";
        if (sessionStorage.containsKey(STORAGE_KEY_COOKIES)) {
            cookieHeader = (String) sessionStorage.get(STORAGE_KEY_COOKIES);
        }
        ResponseWrapper responseWrapper;
        Call<String> serviceCall = service.getHourlyForecastJsString(query, cookieHeader);
        try {
            Response<String> response = serviceCall.execute();
            HourlyForecast result = null;
            if (response.isSuccessful()) {
                String payload = response.body();
                result = DailyForecastParser.parse(payload);
            }
            responseWrapper = new ResponseWrapper(result);
        } catch (Exception e) {
            responseWrapper = new ResponseWrapper(e);
        }
        return responseWrapper;
    }

    private List<City> parseCityList(Response<SearchCityResponse> response) {
        SearchCityResponse searchCityResponse = response.body();
        List<Result> listOfResults = searchCityResponse.getResults();
        if (listOfResults.size() == 1 && "0".equals(listOfResults.get(0).getId())) {
            // zero id means no result
            return null;
        } else {
            List<City> cityList = new ArrayList<>(listOfResults.size());
            for (Result r : listOfResults) {
                String resId = r.getId();
                String resText = r.getText();

                City city = new City();

                int slash = resId.indexOf('/');
                if (slash > -1) {
                    String id = resId.substring(0, slash);
                    int star = resId.indexOf('*');
                    if (star > -1) {
                        id = id.substring(0, star);
                    }
                    city.setId(id);
                }

                int com = resText.indexOf(',');
                if (com != -1) {
                    String title = resText.substring(0, com);
                    String path = resText.substring(com + 1, resText.length()).trim();
                    city.setTitle(title);
                    city.setPath(path);
                } else {
                    city.setTitle(resText);
                    city.setPath("");
                }

                cityList.add(city);
            }
            return cityList;
        }
    }

    private String randomPage(String url, List<String> pages) {
        int k = random.nextInt(pages.size());
        return url + (url.charAt(url.length() - 1) == '/' ? "" : "/") + pages.get(k);
    }
}
