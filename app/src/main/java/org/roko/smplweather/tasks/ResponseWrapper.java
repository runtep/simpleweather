package org.roko.smplweather.tasks;

import androidx.annotation.Nullable;

final class ResponseWrapper {

    final Object content;
    final Exception exception;

    ResponseWrapper(@Nullable Object content) {
        this.content = content;
        this.exception = null;
    }
    ResponseWrapper(Exception exception) {
        this.exception = exception;
        this.content = null;
    }
}
