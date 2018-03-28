package org.roko.smplweather;

import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;

import org.roko.smplweather.model.RssChannel;
import org.roko.smplweather.model.RssItem;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;

public class RssReadTask extends AsyncTask<String, Integer, RequestResult> {

    private RequestCallback<RssReadResult> callback;

    public RssReadTask(RequestCallback<RssReadResult> mCallback) {
        setCallback(mCallback);
    }

    public void setCallback(RequestCallback<RssReadResult> callback) {
        this.callback = callback;
    }

    @Override
    protected void onPreExecute() {
        if (callback != null) {
            NetworkInfo networkInfo = callback.getActiveNetworkInfo();
            if (networkInfo == null || !networkInfo.isConnected() ||
                    (networkInfo.getType() != ConnectivityManager.TYPE_WIFI
                            && networkInfo.getType() != ConnectivityManager.TYPE_MOBILE)) {
                callback.handleResult(new RssReadResult(RssReadResult.Code.NETWORK_ISSUE));
                cancel(true);
            }
        }
    }

    @Override
    protected RequestResult doInBackground(String... args) {
        RequestResult res = null;
        if (!isCancelled() && args != null && args.length > 0) {
            String urlString = args[0];
            try {
                URL url = new URL(urlString);
                RssChannel content = getContent(url);
                res = new RequestResult(content);
            } catch (Exception e) {
                res = new RequestResult(e);
            }
        }
        return res;
    }

    @Override
    protected void onPostExecute(RequestResult result) {
        if (result != null && callback != null) {
            RssReadResult rssResult;
            if (result.exception != null) {
                rssResult = new RssReadResult(RssReadResult.Code.ERROR,
                        result.exception.toString());
            } else {
                if (result.content != null) {
                    rssResult = new RssReadResult(RssReadResult.Code.SUCCESS, result.content);
                } else {
                    rssResult = new RssReadResult(RssReadResult.Code.NULL_CONTENT);
                }
            }
            callback.handleResult(rssResult);
        }
    }

    // -----------------------------------------------------------------------------------------
    RssChannel getContent(URL url) throws IOException {
        InputStream is = null;
        HttpsURLConnection conn = null;
        RssChannel res = null;
        int timeoutMillis = 3000;
        try {
            conn = (HttpsURLConnection) url.openConnection();
            conn.setReadTimeout(timeoutMillis);
            conn.setConnectTimeout(timeoutMillis);
            conn.setRequestMethod("GET");
            conn.setDoInput(true);
            conn.connect();

            int responseCode = conn.getResponseCode();
            if (responseCode != HttpsURLConnection.HTTP_OK) {
                throw new IOException("Failed with error HTTP " + responseCode);
            }
            is = conn.getInputStream();

            if (is != null) {
                res = readFromInputStream(is);
            }
        } finally {
            if (is != null) is.close();
            if (conn != null) conn.disconnect();
        }
        return res;
    }

    public RssChannel readFromInputStream(InputStream is)
            throws IOException {
        Reader reader = new InputStreamReader(is, Charset.forName("utf-8"));
        try {
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            XmlPullParser xmlParser = factory.newPullParser();
            xmlParser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            xmlParser.setInput(reader);
            xmlParser.nextTag();
            return readTheFeed(xmlParser);
        } catch (XmlPullParserException e) {
            return null;
        }
    }

    private RssChannel readTheFeed(XmlPullParser parser) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, null, "rss");
        RssChannel rssChannel = null;
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            if ("channel".equals(parser.getName())) {
                rssChannel = parseChannel(parser);
            } else {
                skip(parser);
            }
        }

        return rssChannel;
    }

    private RssChannel parseChannel(XmlPullParser parser) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, null, "channel");
        RssChannel rssChannel = new RssChannel();
        List<RssItem> rssItems = null;
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            if ("ttl".equals(name)) {
                parser.require(XmlPullParser.START_TAG, null, "ttl");
                rssChannel.setTtl(fetchText(parser));
                parser.require(XmlPullParser.END_TAG, null, "ttl");
            } else if ("link".equals(name)) {
                parser.require(XmlPullParser.START_TAG, null, "link");
                rssChannel.setLink(fetchText(parser));
                parser.require(XmlPullParser.END_TAG, null, "link");
            } else if ("item".equals(name)) {
                if (rssItems == null) {
                    rssItems = new ArrayList<>();
                    rssChannel.setItems(rssItems);
                }
                rssItems = rssChannel.getItems();

                RssItem rssItem = parseItem(parser);
                rssItems.add(rssItem);
            } else {
                skip(parser);
            }
        }

        return rssChannel;
    }

    private RssItem parseItem(XmlPullParser parser) throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, null, "item");
        RssItem rssItem = new RssItem();
        String title = null, description = null, category = null, source = null;
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            if ("title".equals(name)) {
                title = fetchText(parser);
            } else if ("description".equals(name)) {
                description = fetchText(parser);
            } else if ("category".equals(name)) {
                category = fetchText(parser);
            } else if ("source".equals(name)) {
                source = fetchText(parser);
            } else {
                skip(parser);
            }
        }
        rssItem.setTitle(title);
        rssItem.setDescription(description);
        rssItem.setCategory(category);
        rssItem.setSource(source);
        return rssItem;
    }

    private String fetchText(XmlPullParser parser) throws IOException, XmlPullParserException {
        String result = "";
        if (parser.next() == XmlPullParser.TEXT) {
            result = parser.getText();
            parser.nextTag();
        }
        return result;
    }


    private void skip(XmlPullParser parser) throws XmlPullParserException, IOException {
        if (parser.getEventType() != XmlPullParser.START_TAG) {
            throw new IllegalStateException();
        }
        int depth = 1;
        while (depth != 0) {
            switch (parser.next()) {
                case XmlPullParser.END_TAG:
                    depth--;
                    break;
                case XmlPullParser.START_TAG:
                    depth++;
                    break;
            }
        }
    }
    // -----------------------------------------------------------------------------------------
}