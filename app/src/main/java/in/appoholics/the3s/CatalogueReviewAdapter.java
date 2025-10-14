package in.appoholics.the3s;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.text.NumberFormat;
import java.util.Currency;
import java.util.List;
import java.util.Locale;

public class CatalogueReviewAdapter extends RecyclerView.Adapter<CatalogueReviewAdapter.ReviewViewHolder> {

    public interface OnItemRemovedListener {
        void onItemRemoved(int position);
    }

    private final Context context;
    private final List<Product> productList;
    private final OnItemRemovedListener listener;

    public CatalogueReviewAdapter(Context context, List<Product> productList, OnItemRemovedListener listener) {
        this.context = context;
        this.productList = productList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ReviewViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_catalogue_review, parent, false);
        return new ReviewViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReviewViewHolder holder, int position) {
        Product product = productList.get(position);
        holder.name.setText(product.getName());

        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));
        currencyFormat.setMaximumFractionDigits(0);
        holder.price.setText(currencyFormat.format(product.getPrice()));

        Glide.with(context).load(product.getImageUrl()).into(holder.image);

        holder.removeButton.setOnClickListener(v -> listener.onItemRemoved(holder.getAdapterPosition()));
    }

    @Override
    public int getItemCount() {
        return productList.size();
    }

    public static class ReviewViewHolder extends RecyclerView.ViewHolder {
        ImageView image;
        TextView name, price;
        ImageButton removeButton;
        public ReviewViewHolder(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.product_image);
            name = itemView.findViewById(R.id.product_name);
            price = itemView.findViewById(R.id.product_price);
            removeButton = itemView.findViewById(R.id.remove_button);
        }
    }

    public void updateList(List<Product> newProducts) {
        this.productList.clear();
        this.productList.addAll(newProducts);
        notifyDataSetChanged();
    }

}