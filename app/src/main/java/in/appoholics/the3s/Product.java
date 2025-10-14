// Product.java
package in.appoholics.the3s;

import android.os.Parcel;
import android.os.Parcelable;

public class Product implements Parcelable {
    private final String parseObjectId;
    private final String name;
    private final String detail;
    private double price;
    private final String imageUrl;
    private final String category; // ✨ ADDED
    private final String brand;    // ✨ ADDED
    private boolean isFavorite;
    private boolean isSelected;

    // ✨ UPDATED CONSTRUCTOR
    public Product(String parseObjectId, String name, String detail, double price, String imageUrl, String category, String brand) {
        this.parseObjectId = parseObjectId;
        this.name = name;
        this.detail = detail;
        this.price = price;
        this.imageUrl = imageUrl;
        this.category = category;
        this.brand = brand;
    }

    // --- Getters and Setters ---
    public String getParseObjectId() {
        return parseObjectId;
    }

    public String getName() {
        return name;
    }

    public String getDetail() {
        return detail;
    } // ✨ ADDED

    public double getPrice() {
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

    public boolean isFavorite() {
        return isFavorite;
    }

    public void setFavorite(boolean favorite) {
        isFavorite = favorite;
    }

    public boolean isSelected() {
        return isSelected;
    }

    public void setSelected(boolean selected) {
        isSelected = selected;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    // --- PARCELABLE IMPLEMENTATION ---
    protected Product(Parcel in) {
        parseObjectId = in.readString();
        name = in.readString();
        detail = in.readString();
        price = in.readDouble();
        imageUrl = in.readString();
        category = in.readString();
        brand = in.readString();
        isFavorite = in.readByte() != 0;
        isSelected = in.readByte() != 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(parseObjectId);
        dest.writeString(name);
        dest.writeString(detail);
        dest.writeDouble(price);
        dest.writeString(imageUrl);
        dest.writeString(category);
        dest.writeString(brand);
        dest.writeByte((byte) (isFavorite ? 1 : 0));
        dest.writeByte((byte) (isSelected ? 1 : 0));
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<Product> CREATOR = new Creator<Product>() {
        @Override
        public Product createFromParcel(Parcel in) {
            return new Product(in);
        }

        @Override
        public Product[] newArray(int size) {
            return new Product[size];
        }
    };
}