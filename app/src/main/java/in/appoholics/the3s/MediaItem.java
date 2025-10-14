package in.appoholics.the3s;

public class MediaItem {
    private final String url;
    private final String type; // "image" or "video"

    public MediaItem(String url, String type) {
        this.url = url;
        this.type = type;
    }

    public String getUrl() { return url; }
    public String getType() { return type; }
}