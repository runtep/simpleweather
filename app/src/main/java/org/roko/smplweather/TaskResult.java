package org.roko.smplweather;

import android.support.annotation.NonNull;

public class TaskResult {
    public interface Code {
        int TIMEOUT_EXPIRED = -4;
        int NULL_CONTENT = -3;
        int NETWORK_ISSUE = -2;
        int ERROR = -1;
        int SUCCESS = 0;
    }
    private final int code;
    private String details;
    private Object content;

    public TaskResult(int code) {
        this.code = code;
    }

    public TaskResult(int code, String message) {
        this.code = code;
        this.details = message;
    }

    public TaskResult(int code, @NonNull Object content) {
        this.code = code;
        this.content = content;
    }

    public int getCode() {
        return code;
    }

    public String getDetails() {
        return details;
    }

    public Object getContent() {
        return content;
    }
}
