package org.roko.smplweather.tasks;

import org.roko.smplweather.model.City;
import org.roko.smplweather.model.RssChannel;
import org.roko.smplweather.model.RssResponse;
import org.roko.smplweather.model.json.Result;
import org.roko.smplweather.model.json.SearchCityResponse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
            ".?<a +href=[^<]+</a>[^<]+(<a +href=.?\"[^=]+=(\\d+).?\">RSS</a>).?";

    public ResponseWrapper processReadRssRequest(GenericTask.ApiService service, String queryString) {
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

    public boolean checkCookies(String url, Map<String, Object> sessionStorage) {
        if (!sessionStorage.containsKey(STORAGE_KEY_COOKIES)) {
            OkHttpClient okHttpClient = new OkHttpClient();
            Request okRequest = new Request.Builder()
                    .url(url)
                    .build();
            okhttp3.Call okCall = okHttpClient.newCall(okRequest);
            try {
                okhttp3.Response okResponse = okCall.execute();
                if (okResponse.isSuccessful()) {
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
            } catch (IOException e) {
                return false;
            }
        }
        return true;
    }

    public ResponseWrapper processSearchCityRequest(GenericTask.ApiService service, String queryString) {
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

    public ResponseWrapper processGetRssIdByCityId(GenericTask.ApiService service, String query,
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
                        content = m.group(2);
                    }
                }
            }
            responseWrapper = new ResponseWrapper(content);
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
}
