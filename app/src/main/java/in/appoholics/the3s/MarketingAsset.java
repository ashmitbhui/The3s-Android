package in.appoholics.the3s;

/**
 * A shareable marketing asset: a video, a PDF or an image.
 *
 * Backed by the Parse "Marketing" class:
 *   Name (String), Type (String: video|pdf|image), File (File),
 *   Thumb (File, optional preview), Brand (String, optional),
 *   Category (String, optional), Order (Number), isEnabled (Boolean)
 */
public class MarketingAsset {

    public static final String TYPE_VIDEO = "video";
    public static final String TYPE_PDF = "pdf";
    public static final String TYPE_IMAGE = "image";

    private final String id;
    private final String name;
    private final String type;
    private final String category;
    private final String brand;
    private final String fileUrl;
    private final String thumbnailUrl;

    public MarketingAsset(String id, String name, String type, String category,
                          String brand, String fileUrl, String thumbnailUrl) {
        this.id = id;
        this.name = name;
        this.type = normaliseType(type, fileUrl);
        this.category = category;
        this.brand = brand;
        this.fileUrl = fileUrl;
        this.thumbnailUrl = thumbnailUrl;
    }

    /** Falls back to the file extension when Type is missing or unknown. */
    private static String normaliseType(String type, String url) {
        if (type != null) {
            String t = type.trim().toLowerCase();
            if (t.equals(TYPE_VIDEO) || t.equals(TYPE_PDF) || t.equals(TYPE_IMAGE)) {
                return t;
            }
        }
        String lower = url == null ? "" : url.toLowerCase();
        if (lower.endsWith(".pdf")) return TYPE_PDF;
        if (lower.endsWith(".mp4") || lower.endsWith(".mov") || lower.endsWith(".m4v")) return TYPE_VIDEO;
        return TYPE_IMAGE;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public String getCategory() {
        return category;
    }

    public String getBrand() {
        return brand;
    }

    public String getFileUrl() {
        return fileUrl;
    }

    /** Videos and PDFs may have no preview; images preview themselves. */
    public String getThumbnailUrl() {
        if (thumbnailUrl != null && !thumbnailUrl.isEmpty()) {
            return thumbnailUrl;
        }
        return TYPE_IMAGE.equals(type) ? fileUrl : null;
    }

    public String getMimeType() {
        switch (type) {
            case TYPE_PDF:
                return "application/pdf";
            case TYPE_VIDEO:
                return "video/*";
            default:
                return "image/*";
        }
    }

    /** File name used when the asset is downloaded for sharing. */
    public String getDownloadName() {
        String safe = (name == null || name.trim().isEmpty() ? "asset" : name.trim())
                .replaceAll("[^A-Za-z0-9 ._-]", "").replaceAll("\\s+", "_");
        String ext = TYPE_PDF.equals(type) ? ".pdf" : TYPE_VIDEO.equals(type) ? ".mp4" : ".jpg";
        if (fileUrl != null) {
            int dot = fileUrl.lastIndexOf('.');
            if (dot > 0 && fileUrl.length() - dot <= 5) {
                ext = fileUrl.substring(dot);
            }
        }
        return safe + ext;
    }
}
