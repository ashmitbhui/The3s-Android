package in.appoholics.the3s;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.util.List;

public class VideoAdapter extends RecyclerView.Adapter<VideoAdapter.VideoViewHolder> {

    private final Context context;
    private final List<Video> videoList;

    public VideoAdapter(Context context, List<Video> videoList) {
        this.context = context;
        this.videoList = videoList;
    }

    @NonNull
    @Override
    public VideoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_video_card, parent, false);
        return new VideoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VideoViewHolder holder, int position) {
        Video videoItem = videoList.get(position);

        holder.productName.setText(videoItem.getName());

        Glide.with(context)
                .load(videoItem.getThumbnailUrl())
                .placeholder(R.drawable.placeholder_image)
                .into(holder.thumbnail);

        // ✨ UPDATED CLICK LISTENER LOGIC
        holder.itemView.setOnClickListener(v -> {
            String url = videoItem.getVideoUrl();
            if (url != null && !url.isEmpty()) {
                // Create an intent to view the URL
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                context.startActivity(browserIntent);
            } else {
                Toast.makeText(context, "Video URL not available.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public int getItemCount() {
        return videoList.size();
    }

    public static class VideoViewHolder extends RecyclerView.ViewHolder {
        ImageView thumbnail;
        TextView productName;
        public VideoViewHolder(@NonNull View itemView) {
            super(itemView);
            thumbnail = itemView.findViewById(R.id.video_thumbnail_image);
            productName = itemView.findViewById(R.id.video_name_textview);
        }
    }
}