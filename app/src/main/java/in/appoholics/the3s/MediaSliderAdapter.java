package in.appoholics.the3s;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.PlaybackException;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.ui.PlayerView;

import java.io.File;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MediaSliderAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final String TAG = "MediaSliderAdapter"; // Tag for logging
    private static final int TYPE_IMAGE = 1;
    private static final int TYPE_VIDEO = 2;

    private final Context context;
    private final List<MediaItem> mediaItems;
    private ExoPlayer exoPlayer;

    public MediaSliderAdapter(Context context, List<MediaItem> mediaItems) {
        this.context = context;
        this.mediaItems = mediaItems;
        initializePlayer();
    }

    private void initializePlayer() {
        if (exoPlayer == null) {
            exoPlayer = new ExoPlayer.Builder(context).build();
            // ✨ ADD A LISTENER to log errors and state changes
            exoPlayer.addListener(new Player.Listener() {
                @Override
                public void onPlayerError(@NonNull PlaybackException error) {
                    Log.e(TAG, "ExoPlayer Error: ", error);
                }

                @Override
                public void onPlaybackStateChanged(int playbackState) {
                    String stateString;
                    switch (playbackState) {
                        case ExoPlayer.STATE_IDLE:
                            stateString = "Idle";
                            break;
                        case ExoPlayer.STATE_BUFFERING:
                            stateString = "Buffering";
                            break;
                        case ExoPlayer.STATE_READY:
                            stateString = "Ready";
                            break;
                        case ExoPlayer.STATE_ENDED:
                            stateString = "Ended";
                            break;
                        default:
                            stateString = "UNKNOWN";
                            break;
                    }
                    Log.d(TAG, "Playback state changed to: " + stateString);
                }
            });
        }
    }

    @Override
    public int getItemViewType(int position) {
        if ("video".equalsIgnoreCase(mediaItems.get(position).getType())) {
            return TYPE_VIDEO;
        } else {
            return TYPE_IMAGE;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_VIDEO) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_video_slider, parent, false);
            return new VideoViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_image_slider, parent, false);
            return new ImageViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        MediaItem item = mediaItems.get(position);

        if (holder.getItemViewType() == TYPE_VIDEO) {
            VideoViewHolder videoHolder = (VideoViewHolder) holder;
            videoHolder.shareButton.setOnClickListener(v -> shareMediaItem(item));
        } else {
            ImageViewHolder imageHolder = (ImageViewHolder) holder;
            Glide.with(context).load(item.getUrl()).into(imageHolder.imageView);
            imageHolder.shareButton.setOnClickListener(v -> shareMediaItem(item));
            // tap to enlarge: full-screen viewer with all the product's photos
            imageHolder.imageView.setOnClickListener(v -> {
                java.util.ArrayList<String> urls = new java.util.ArrayList<>();
                int start = 0;
                for (MediaItem m : mediaItems) {
                    if ("image".equalsIgnoreCase(m.getType())) {
                        if (m == item) start = urls.size();
                        urls.add(m.getUrl());
                    }
                }
                ImageViewerActivity.open(context, urls, start);
            });
        }
    }

    @Override
    public void onViewAttachedToWindow(@NonNull RecyclerView.ViewHolder holder) {
        super.onViewAttachedToWindow(holder);
        if (holder instanceof VideoViewHolder) {
            Log.d(TAG, "Attaching player to position: " + holder.getAdapterPosition());
            VideoViewHolder videoHolder = (VideoViewHolder) holder;
            MediaItem item = mediaItems.get(holder.getAdapterPosition());

            videoHolder.playerView.setPlayer(exoPlayer);
            com.google.android.exoplayer2.MediaItem mediaItem = com.google.android.exoplayer2.MediaItem.fromUri(item.getUrl());
            exoPlayer.setMediaItem(mediaItem);
            exoPlayer.seekToDefaultPosition();
            // ✨ --- ADD/UPDATE THESE LINES --- ✨
            exoPlayer.setRepeatMode(Player.REPEAT_MODE_ONE); // Ensures video loops
            exoPlayer.setPlayWhenReady(true);
            exoPlayer.prepare();
            exoPlayer.setVolume(0f);
//            videoHolder.muteButton.setImageResource(R.drawable.ic_mute);
            exoPlayer.setPlayWhenReady(true);
        }
    }

    @Override
    public void onViewDetachedFromWindow(@NonNull RecyclerView.ViewHolder holder) {
        super.onViewDetachedFromWindow(holder);
        if (holder instanceof VideoViewHolder) {
            Log.d(TAG, "Detaching player from position: " + holder.getAdapterPosition());
            VideoViewHolder videoHolder = (VideoViewHolder) holder;
            videoHolder.playerView.setPlayer(null);
            exoPlayer.pause(); // Use pause() for simplicity
        }
    }


    private void shareMediaItem(MediaItem item) {
        ProgressDialog progressDialog = new ProgressDialog(context);
        progressDialog.setMessage("Preparing media...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            try {
                // Background Task: Download file using Glide
                File file = Glide.with(context).asFile().load(item.getUrl()).submit().get();
                File destDir = new File(context.getCacheDir(), "images");
                if (!destDir.exists()) destDir.mkdirs();

                String fileExtension = item.getType().equals("video") ? ".mp4" : ".jpg";
                File destFile = new File(destDir, "share_media" + fileExtension);

                copy(file, destFile);

                Uri mediaUri = FileProvider.getUriForFile(
                        context,
                        context.getApplicationContext().getPackageName() + ".fileprovider",
                        destFile
                );

                // UI Task: Launch share intent
                ((ProductDetail) context).runOnUiThread(() -> {
                    progressDialog.dismiss();
                    Intent shareIntent = new Intent(Intent.ACTION_SEND);
                    String mimeType = item.getType().equals("video") ? "video/*" : "image/*";
                    shareIntent.setType(mimeType);
                    shareIntent.putExtra(Intent.EXTRA_STREAM, mediaUri);
                    shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    context.startActivity(Intent.createChooser(shareIntent, "Share Media"));
                });

            } catch (Exception e) {
                ((ProductDetail) context).runOnUiThread(() -> {
                    progressDialog.dismiss();
                    Toast.makeText(context, "Failed to share media.", Toast.LENGTH_SHORT).show();
                    Log.e("ShareError", "Failed to share media file", e);
                });
            }
        });
    }

    // Helper method to copy files
    private void copy(File src, File dst) throws Exception {
        // ... (this method is the same as before)
    }

    public void releasePlayer() {
        if (exoPlayer != null) {
            exoPlayer.release();
            exoPlayer = null;
        }
    }

    @Override
    public int getItemCount() {
        return mediaItems.size();
    }

    /**
     * ViewHolder for Image items.
     * It finds and holds a reference to the ImageView.
     */
    public static class ImageViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        ImageButton shareButton;
        public ImageViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.slider_image_view);
            shareButton = itemView.findViewById(R.id.share_media_button);
        }
    }

    /**
     * ViewHolder for Video items.
     * It finds and holds a reference to the PlayerView.
     */
    public static class VideoViewHolder extends RecyclerView.ViewHolder {
        PlayerView playerView;
        ImageButton shareButton;
        public VideoViewHolder(@NonNull View itemView) {
            super(itemView);
            playerView = itemView.findViewById(R.id.player_view);
            shareButton = itemView.findViewById(R.id.share_media_button);
        }
    }
}