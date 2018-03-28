package org.roko.smplweather;

import org.roko.smplweather.model.RssChannel;

public class RequestResult {
    public RssChannel content;
    public Exception exception;
    public RequestResult(RssChannel content) {
        this.content = content;
    }
    public RequestResult(Exception exception) {
        this.exception = exception;
    }
}
