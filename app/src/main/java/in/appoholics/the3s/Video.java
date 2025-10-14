package in.appoholics.the3s;

public class Video {
    private final String name;
    private final String category;
    private final String thumbnailUrl;
    private final String videoUrl;

    public Video(String name, String category, String thumbnailUrl, String videoUrl) {
        this.name = name;
        this.category = category;
        this.thumbnailUrl = thumbnailUrl;
        this.videoUrl = videoUrl;
    }

    public String getName() {
        return name;
    }

    public String getCategory() {
        return category;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public String getVideoUrl() {
        return videoUrl;
    }
}