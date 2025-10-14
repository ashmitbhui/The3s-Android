package in.appoholics.the3s;

public class VideoItem {
    private final String productObjectId; // The ID of the product this video belongs to
    private final String productName;
    private final String category;
    private final String thumbnailUrl;

    public VideoItem(String productObjectId, String productName, String category, String thumbnailUrl) {
        this.productObjectId = productObjectId;
        this.productName = productName;
        this.category = category;
        this.thumbnailUrl = thumbnailUrl;
    }

    public String getProductObjectId() { return productObjectId; }
    public String getProductName() { return productName; }
    public String getCategory() { return category; }
    public String getThumbnailUrl() { return thumbnailUrl; }
}