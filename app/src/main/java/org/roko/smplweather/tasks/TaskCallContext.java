package org.roko.smplweather.tasks;

import android.content.ContentValues;
import android.os.Bundle;

import java.util.Arrays;
import java.util.List;

import androidx.annotation.Nullable;

public class TaskCallContext {

    private final String url;
    private final List<String> pages;
    private final ContentValues sessionStorage;
    private final @TaskAction String action;
    private final Bundle nextTask;

    public static TaskCallContext of(String url, String[] pages, ContentValues sessionStorage,
                                     @TaskAction String action, @Nullable Bundle nextTask) {
        return new TaskCallContext(url, pages, sessionStorage, action, nextTask);
    }

    protected TaskCallContext(String url, String[] pages, ContentValues sessionStorage,
                              @TaskAction String action, Bundle nextTask) {
        this.url = url;
        this.pages = Arrays.asList(pages);
        this.sessionStorage = sessionStorage;
        this.action = action;
        this.nextTask = nextTask == null ? Bundle.EMPTY : nextTask;
    }

    String getUrl() {
        return url;
    }

    ContentValues getSessionStorage() {
        return sessionStorage;
    }

    @TaskAction String getAction() {
        return action;
    }

    Bundle getNextTask() {
        return nextTask;
    }

    List<String> getPages() {
        return pages;
    }
}
