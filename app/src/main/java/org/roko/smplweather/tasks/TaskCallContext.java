package org.roko.smplweather.tasks;

import java.util.Arrays;
import java.util.List;

public class TaskCallContext {

    private String url, action, query;
    private List<String> pages;

    public static TaskCallContext of(String url, String action, String query,
                                     String[] pages) {
        TaskCallContext ctx = new TaskCallContext(url, action, query);
        ctx.pages = Arrays.asList(pages);
        return ctx;
    }

    protected TaskCallContext(String url, String action, String query) {
        this.url = url;
        this.action = action;
        this.query = query;
    }

    String getUrl() {
        return url;
    }

    String getAction() {
        return action;
    }

    String getQuery() {
        return query;
    }

    List<String> getPages() {
        return pages;
    }
}
