package org.roko.smplweather.tasks;

final class ResponseWrapper {

    Object content;
    Exception exception;
    ResponseWrapper(Object content) {
        this.content = content;
    }
    ResponseWrapper(Exception exception) {
        this.exception = exception;
    }
}
