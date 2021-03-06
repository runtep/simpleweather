package org.roko.smplweather;

import java.util.Objects;

import androidx.annotation.IntDef;

public class TaskResult {

    @IntDef({Code.SUCCESS, Code.ERROR, Code.NETWORK_ISSUE, Code.NULL_CONTENT, Code.TIMEOUT_EXPIRED})
    public @interface Code {
        int TIMEOUT_EXPIRED = -4;
        int NULL_CONTENT = -3;
        int NETWORK_ISSUE = -2;
        int ERROR = -1;
        int SUCCESS = 0;
    }
    private final @Code int code;
    private String details;
    private Object content;

    public static TaskResult nullContent() {
        return new TaskResult(Code.NULL_CONTENT);
    }

    public TaskResult(@Code int code) {
        this.code = code;
    }

    public TaskResult(@Code int code, String message) {
        this.code = code;
        this.details = message;
    }

    public TaskResult(@Code int code, Object content) {
        this.code = code;
        this.content = Objects.requireNonNull(content);
    }

    public @Code int getCode() {
        return code;
    }

    public String getDetails() {
        return details;
    }

    public Object getContent() {
        return content;
    }
}
