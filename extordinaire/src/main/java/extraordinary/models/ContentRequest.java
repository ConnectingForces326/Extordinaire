package extraordinary.models;

public class ContentRequest {
    private final String topic;
    private final String platform;
    private final Niche niche;
    private final Style style;

    public ContentRequest(String topic, String platform) {
        this(topic, platform, Niche.GENERAL, Style.HYPE);
    }

    public ContentRequest(String topic, String platform, Niche niche) {
        this(topic, platform, niche, Style.HYPE);
    }

    public ContentRequest(String topic, String platform, Niche niche, Style style) {
        this.topic = topic;
        this.platform = platform;
        this.niche = niche;
        this.style = style;
    }

    public String topic() { return topic; }
    public String platform() { return platform; }
    public Niche niche() { return niche; }
    public Style style() { return style; }
}
