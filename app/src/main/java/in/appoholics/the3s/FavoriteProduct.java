// FavoriteProduct.java
package in.appoholics.the3s;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "favorites_table")
public class FavoriteProduct {

    @PrimaryKey
    @NonNull
    private String productId; // Use a unique ID from Parse as the primary key

    private String name;
    private String detail;
    private String price;
    private String imageUrl;
    private String category;
    private String brand;

    // --- Constructor, Getters, and Setters ---
    public FavoriteProduct(@NonNull String productId, String name, String detail, String price, String imageUrl, String category, String brand) {
        this.productId = productId;
        this.name = name;
        this.detail = detail;
        this.price = price;
        this.imageUrl = imageUrl;
        this.category = category;
        this.brand = brand;
    }

    @NonNull
    public String getProductId() {
        return productId;
    }

    public String getName() {
        return name;
    }

    public String getDetail() {
        return detail;
    }

    public String getPrice() {
        return price;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public String getCategory() {
        return category;
    }

    public String getBrand() {
        return brand;
    }

}