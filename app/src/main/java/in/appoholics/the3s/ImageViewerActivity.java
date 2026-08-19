package in.appoholics.the3s;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;

import java.util.ArrayList;

/** Full-screen product photo viewer: swipe between photos, pinch / double-tap to zoom. */
public class ImageViewerActivity extends AppCompatActivity {
    public static void open(Context ctx, ArrayList<String> urls, int index) {
        Intent i = new Intent(ctx, ImageViewerActivity.class);
        i.putStringArrayListExtra("urls", urls);
        i.putExtra("index", index);
        ctx.startActivity(i);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_viewer);
        ArrayList<String> urls = getIntent().getStringArrayListExtra("urls");
        if (urls == null) urls = new ArrayList<>();
        int index = getIntent().getIntExtra("index", 0);
        ViewPager2 pager = findViewById(R.id.viewer_pager);
        TextView counter = findViewById(R.id.viewer_counter);
        final ArrayList<String> list = urls;
        pager.setAdapter(new RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            @NonNull @Override public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_image_viewer, parent, false);
                return new RecyclerView.ViewHolder(v) {};
            }
            @Override public void onBindViewHolder(@NonNull RecyclerView.ViewHolder h, int pos) {
                ZoomImageView iv = h.itemView.findViewById(R.id.zoom_image);
                Glide.with(iv).load(list.get(pos)).into(iv);
            }
            @Override public int getItemCount() { return list.size(); }
        });
        pager.setCurrentItem(Math.max(0, Math.min(index, urls.size() - 1)), false);
        pager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override public void onPageSelected(int position) { counter.setText((position + 1) + " / " + list.size()); }
        });
        counter.setText((pager.getCurrentItem() + 1) + " / " + urls.size());
        counter.setVisibility(urls.size() > 1 ? View.VISIBLE : View.GONE);
        findViewById(R.id.viewer_close).setOnClickListener(v -> finish());
    }
}
