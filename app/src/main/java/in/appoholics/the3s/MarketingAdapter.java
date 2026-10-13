package in.appoholics.the3s;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

/** Grid of marketing assets; each tile opens the asset or shares it. */
public class MarketingAdapter extends RecyclerView.Adapter<MarketingAdapter.AssetViewHolder> {

    public interface OnAssetActionListener {
        void onOpen(MarketingAsset asset);

        void onShare(MarketingAsset asset);
    }

    private final Context context;
    private final List<MarketingAsset> assets;
    private final OnAssetActionListener listener;

    public MarketingAdapter(Context context, List<MarketingAsset> assets,
                            OnAssetActionListener listener) {
        this.context = context;
        this.assets = assets;
        this.listener = listener;
    }

    @NonNull
    @Override
    public AssetViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.item_marketing_asset, parent, false);
        return new AssetViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull AssetViewHolder holder, int position) {
        MarketingAsset asset = assets.get(position);
        holder.name.setText(asset.getName());

        String thumb = asset.getThumbnailUrl();
        if (thumb != null && !thumb.isEmpty()) {
            Glide.with(context).load(thumb).placeholder(R.drawable.placeholder_image).into(holder.thumb);
        } else {
            holder.thumb.setImageResource(R.drawable.placeholder_image);
        }

        switch (asset.getType()) {
            case MarketingAsset.TYPE_VIDEO:
                holder.badge.setText("VIDEO");
                holder.playIcon.setVisibility(View.VISIBLE);
                break;
            case MarketingAsset.TYPE_PDF:
                holder.badge.setText("PDF");
                holder.playIcon.setVisibility(View.GONE);
                break;
            default:
                holder.badge.setText("IMAGE");
                holder.playIcon.setVisibility(View.GONE);
                break;
        }

        holder.itemView.setOnClickListener(v -> listener.onOpen(asset));
        holder.shareButton.setOnClickListener(v -> listener.onShare(asset));
    }

    @Override
    public int getItemCount() {
        return assets.size();
    }

    static class AssetViewHolder extends RecyclerView.ViewHolder {
        final ImageView thumb;
        final ImageView playIcon;
        final ImageView shareButton;
        final TextView name;
        final TextView badge;

        AssetViewHolder(@NonNull View itemView) {
            super(itemView);
            thumb = itemView.findViewById(R.id.asset_thumb);
            playIcon = itemView.findViewById(R.id.asset_play_icon);
            shareButton = itemView.findViewById(R.id.asset_share);
            name = itemView.findViewById(R.id.asset_name);
            badge = itemView.findViewById(R.id.asset_type_badge);
        }
    }
}
