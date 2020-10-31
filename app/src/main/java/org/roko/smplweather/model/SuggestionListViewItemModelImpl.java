package org.roko.smplweather.model;

public class SuggestionListViewItemModelImpl extends BasicListViewItemModelImpl implements SuggestionListViewItemModel {

    protected final String _id;

    public SuggestionListViewItemModelImpl(String _id, String title, String description) {
        super(title, description);
        this._id = _id;
    }

    @Override
    public String get_id() {
        return this._id;
    }
}
