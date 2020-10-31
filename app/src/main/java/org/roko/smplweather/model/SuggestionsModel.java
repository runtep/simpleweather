package org.roko.smplweather.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SuggestionsModel implements Serializable {

    private List<SuggestionListViewItemModel> suggestions;
    private String query;

    public SuggestionsModel(List<SuggestionListViewItemModel> suggestions) {
        setSuggestions(suggestions);
    }

    public List<SuggestionListViewItemModel> getSuggestions() {
        return new ArrayList<>(suggestions);
    }

    public void setSuggestions(List<SuggestionListViewItemModel> suggestions) {
        this.suggestions = new ArrayList<>(suggestions);
    }

    public void clear() {
        this.suggestions = Collections.emptyList();
        this.query = "";
    }

    public boolean isEmpty() {
        return _isQueryEmpty() || _isSuggestionsEmpty();
    }

    private boolean _isSuggestionsEmpty() {
        return this.suggestions.isEmpty();
    }

    private boolean _isQueryEmpty() {
        return this.query == null || this.query.trim().isEmpty();
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }
}
