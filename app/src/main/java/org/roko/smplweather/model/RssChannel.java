package org.roko.smplweather.model;

import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;

import java.util.List;

@Root(name = "channel", strict = false)
public class RssChannel {
    @Element(name = "ttl", required = false)
    private String ttl;
    @Element(name = "link", required = false)
    private String link;
    @ElementList(inline = true, name = "items", required = false)
    private List<RssItem> items;

    public String getTtl() {
        return ttl;
    }

    public void setTtl(String ttl) {
        this.ttl = ttl;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public List<RssItem> getItems() {
        return items;
    }

    public void setItems(List<RssItem> items) {
        this.items = items;
    }
}
