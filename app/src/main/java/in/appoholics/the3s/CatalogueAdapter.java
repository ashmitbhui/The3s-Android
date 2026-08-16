package in.appoholics.the3s;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.util.List;
import java.util.Set;

public class CatalogueAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    /**
     * The interface (contract) that the Activity must implement to listen for selection events.
     */
    public interface OnProductSelectionListener {
        void onProductSelected(Product product, boolean isSelected);
    }

    // Public constants so the Activity's SpanSizeLookup can access them
    public static final int TYPE_BRAND = 0;
    public static final int TYPE_CATEGORY = 1;
    public static final int TYPE_PRODUCT = 2;

    private final Context context;
    private final List<Object> displayList;
    private final Set<Product> selectedProducts;
    private final OnProductSelectionListener listener;

    public CatalogueAdapter(Context context, List<Object> displayList, Set<Product> selectedProducts, OnProductSelectionListener listener) {
        this.context = context;
        this.displayList = displayList;
        this.selectedProducts = selectedProducts;
        this.listener = listener;
    }

    @Override
    public int getItemViewType(int position) {
        if (displayList.get(position) instanceof BrandHeader) {
            return TYPE_BRAND;
        } else if (displayList.get(position) instanceof CategoryHeader) {
            return TYPE_CATEGORY;
        } else {
            return TYPE_PRODUCT;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        switch (viewType) {
            case TYPE_BRAND:
                return new BrandViewHolder(inflater.inflate(R.layout.item_catalogue_brand, parent, false));
            case TYPE_CATEGORY:
                return new CategoryViewHolder(inflater.inflate(R.layout.item_catalogue_category, parent, false));
            case TYPE_PRODUCT:
            default:
                return new ProductViewHolder(inflater.inflate(R.layout.item_catalogue_product_grid, parent, false));
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        switch (holder.getItemViewType()) {
            case TYPE_BRAND:
                ((BrandViewHolder) holder).bind((BrandHeader) displayList.get(position));
                break;
            case TYPE_CATEGORY:
                ((CategoryViewHolder) holder).bind((CategoryHeader) displayList.get(position));
                break;
            case TYPE_PRODUCT:
                ((ProductViewHolder) holder).bind((Product) displayList.get(position));
                break;
        }
    }

    @Override
    public int getItemCount() {
        return displayList.size();
    }

    // --- ViewHolder for Brand Headers ---
    static class BrandViewHolder extends RecyclerView.ViewHolder {
        TextView brandName;
        BrandViewHolder(@NonNull View itemView) {
            super(itemView);
            brandName = itemView.findViewById(R.id.brand_header_name);
        }
        void bind(BrandHeader item) {
            brandName.setText(item.brandName);
        }
    }

    // --- ViewHolder for Category Headers ---
    static class CategoryViewHolder extends RecyclerView.ViewHolder {
        TextView categoryName;
        CategoryViewHolder(@NonNull View itemView) {
            super(itemView);
            categoryName = itemView.findViewById(R.id.category_header_name);
        }
        void bind(CategoryHeader item) {
            categoryName.setText(item.categoryName);
        }
    }

    // --- ViewHolder for Products ---
    class ProductViewHolder extends RecyclerView.ViewHolder {
        ImageView image;
        TextView name, price, detail;
        CheckBox checkBox;
        ProductViewHolder(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.product_image);
            name = itemView.findViewById(R.id.product_name);
            price = itemView.findViewById(R.id.product_price);
            detail = itemView.findViewById(R.id.product_detail);
            checkBox = itemView.findViewById(R.id.product_checkbox);
        }
        void bind(Product product) {
            name.setText(product.getName());
            if (UserStatus.isLoggedIn()) {   // dealer prices are login-only
                price.setText(String.format(java.util.Locale.getDefault(), "₹%,.0f", product.getPrice()));
                price.setVisibility(View.VISIBLE);
            } else {
                price.setVisibility(View.GONE);
            }
            String detailText = product.getDetail();
            if (detailText == null || detailText.trim().isEmpty()) {
                detail.setVisibility(View.GONE);
            } else {
                detail.setVisibility(View.VISIBLE);
                detail.setText(detailText);
            }
            Glide.with(context).load(product.getImageUrl()).into(image);

            checkBox.setOnCheckedChangeListener(null);
            checkBox.setChecked(selectedProducts.contains(product));

            checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (listener != null) {
                    listener.onProductSelected(product, isChecked);
                }
            });
        }
    }
}