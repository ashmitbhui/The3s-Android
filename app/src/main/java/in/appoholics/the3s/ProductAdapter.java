package in.appoholics.the3s;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.parse.ParseUser;

import java.text.NumberFormat;
import java.util.Currency;
import java.util.List;
import java.util.Locale;

public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ProductViewHolder> {

    /**
     * Interface to communicate selection count changes back to the hosting Activity/Fragment.
     */
    public interface OnProductSelectionChangedListener {
        void onSelectionChanged(int selectedCount);
    }

    private final Context context;
    private final List<Product> productList;
    private OnProductSelectionChangedListener listener; // Listener is a field

    public ProductAdapter(Context context, List<Product> productList, OnProductSelectionChangedListener listener) {
        this.context = context;
        this.productList = productList;
    }

    public void setOnProductSelectionChangedListener(OnProductSelectionChangedListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ProductViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_product_card, parent, false);
        return new ProductViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProductViewHolder holder, int position) {
        Product product = productList.get(position);
        if (product == null) return;

        // --- Bind Data to Views ---
        holder.name.setText(product.getName());
        holder.detail.setText(product.getDetail());

        // Price Visibility Logic
        if (ParseUser.getCurrentUser() != null) {
            holder.price.setText(DealerCurrency.format(product.getPrice(), DealerCurrency.isINR() ? 0 : 2));
            holder.price.setVisibility(View.VISIBLE);
        } else {
            holder.price.setVisibility(View.GONE);
        }

        Glide.with(context)
                .load(product.getImageUrl())
                .placeholder(R.drawable.placeholder_image)
                .into(holder.image);

        // --- Set Initial States from the Product object ---
        holder.favouriteButton.setImageResource(
                product.isFavorite() ? R.drawable.ic_favourite_filled : R.drawable.ic_favourite_outline);

        holder.selectionCheckbox.setOnCheckedChangeListener(null); // Prevent listener loop
        holder.selectionCheckbox.setChecked(product.isSelected());

        // --- Set Listeners ---
        holder.selectionCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            product.setSelected(isChecked);
            if (listener != null) {
                int count = 0;
                for (Product p : productList) {
                    if (p.isSelected()) count++;
                }
                listener.onSelectionChanged(count);
            }
        });

        holder.favouriteButton.setOnClickListener(v -> {
            // Instantly update UI
            product.setFavorite(!product.isFavorite());
            notifyItemChanged(holder.getAdapterPosition());

            // Perform DB operation in the background
            AppDatabase.databaseWriteExecutor.execute(() -> {
                FavoriteProductDao dao = AppDatabase.getDatabase(context).favoriteProductDao();
                String uniqueProductId = product.getParseObjectId();

                if (product.isFavorite()) {
                    dao.insert(new FavoriteProduct(uniqueProductId, product.getName(), product.getDetail(), String.valueOf(product.getPrice()), product.getImageUrl(), product.getCategory(), product.getBrand()));
                } else {
                    dao.delete(new FavoriteProduct(uniqueProductId, "", "", "", "", "", ""));
                }
            });
        });

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, ProductDetail.class);
            intent.putExtra("PRODUCT_ID", product.getParseObjectId());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return productList.size();
    }

    public static class ProductViewHolder extends RecyclerView.ViewHolder {
        ImageView image;
        TextView name, price, detail;
        CheckBox selectionCheckbox;
        ImageButton favouriteButton;

        public ProductViewHolder(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.product_image);
            name = itemView.findViewById(R.id.product_name);
            price = itemView.findViewById(R.id.product_price);
            detail = itemView.findViewById(R.id.product_detail);
            selectionCheckbox = itemView.findViewById(R.id.selection_checkbox);
            favouriteButton = itemView.findViewById(R.id.favourite_button);
        }
    }
}