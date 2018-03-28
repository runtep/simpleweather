package org.roko.smplweather;

import android.support.annotation.NonNull;

import org.roko.smplweather.model.RssChannel;

public class RssReadResult {
    public interface Code {
        int NULL_CONTENT = -3;
        int NETWORK_ISSUE = -2;
        int ERROR = -1;
        int SUCCESS = 0;
    }
    private final int code;
    private String details;
    private RssChannel content;

    public RssReadResult(int code) {
        this.code = code;
    }

    public RssReadResult(int code, String message) {
        this.code = code;
        this.details = message;
    }

    public RssReadResult(int code, @NonNull RssChannel content) {
        this.code = code;
        this.content = content;
    }

    public int getCode() {
        return code;
    }

    public String getDetails() {
        return details;
    }

    public RssChannel getContent() {
        return content;
    }
}
