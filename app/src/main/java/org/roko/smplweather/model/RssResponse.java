package org.roko.smplweather.model;

import org.simpleframework.xml.Element;
import org.simpleframework.xml.Root;

@Root(name = "rss", strict = false)
public class RssResponse {
    @Element(name = "channel")
    private RssChannel channel;

    public RssChannel getChannel() {
        return channel;
    }

    public void setChannel(RssChannel channel) {
        this.channel = channel;
    }
}
