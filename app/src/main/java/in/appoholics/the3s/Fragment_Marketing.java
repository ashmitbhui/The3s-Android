package in.appoholics.the3s;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textfield.TextInputEditText;
import com.parse.ParseFile;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Marketing tab: shareable videos, PDFs and images.
 *
 * Reads the Parse "Marketing" class and falls back to the legacy "Videos"
 * class, so existing video rows keep working without a migration.
 */
public class Fragment_Marketing extends Fragment implements MarketingAdapter.OnAssetActionListener {

    private Button loginButton;
    private RecyclerView recyclerView;
    private TextInputEditText searchEditText;
    private View emptyStateView;
    private AppCompatButton filterAll, filterVideos, filterPdfs, filterImages;

    private MarketingAdapter adapter;
    private final List<MarketingAsset> allAssets = new ArrayList<>();
    private final List<MarketingAsset> displayed = new ArrayList<>();
    private String typeFilter = null;          // null = all

    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_marketing, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        loginButton = view.findViewById(R.id.login);
        recyclerView = view.findViewById(R.id.marketing_recycler_view);
        searchEditText = view.findViewById(R.id.marketing_search_edit_text);
        emptyStateView = view.findViewById(R.id.marketing_empty_state);
        filterAll = view.findViewById(R.id.filter_all);
        filterVideos = view.findViewById(R.id.filter_videos);
        filterPdfs = view.findViewById(R.id.filter_pdfs);
        filterImages = view.findViewById(R.id.filter_images);

        adapter = new MarketingAdapter(requireContext(), displayed, this);
        recyclerView.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        recyclerView.setAdapter(adapter);

        filterAll.setOnClickListener(v -> setTypeFilter(null));
        filterVideos.setOnClickListener(v -> setTypeFilter(MarketingAsset.TYPE_VIDEO));
        filterPdfs.setOnClickListener(v -> setTypeFilter(MarketingAsset.TYPE_PDF));
        filterImages.setOnClickListener(v -> setTypeFilter(MarketingAsset.TYPE_IMAGE));

        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) { applyFilters(); }
            @Override public void afterTextChanged(Editable s) { }
        });

        loginButton.setOnClickListener(v -> {
            if (ParseUser.getCurrentUser() != null) {
                ParseUser.logOutInBackground(e -> {
                    Toast.makeText(getContext(),
                            e == null ? "Logged out successfully." : "Logout failed: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                    updateLoginButtonState();
                });
            } else {
                showLoginDialog();
            }
        });

        updateLoginButtonState();
        setTypeFilter(null);
        loadAssets();
    }

    @Override
    public void onResume() {
        super.onResume();
        updateLoginButtonState();
    }

    // ---------- data ----------

    private void loadAssets() {
        allAssets.clear();
        ParseQuery<ParseObject> query = new ParseQuery<>("Marketing");
        query.setLimit(1000);
        query.addAscendingOrder("Order");
        query.findInBackground((objects, e) -> {
            if (!isAdded()) return;
            if (e == null && objects != null) {
                for (ParseObject o : objects) {
                    if (Boolean.FALSE.equals(o.getBoolean("isEnabled")) && o.has("isEnabled")) {
                        continue;
                    }
                    ParseFile file = o.getParseFile("File");
                    ParseFile thumb = o.getParseFile("Thumb");
                    if (file == null) continue;
                    allAssets.add(new MarketingAsset(
                            o.getObjectId(), o.getString("Name"), o.getString("Type"),
                            o.getString("Category"), o.getString("Brand"),
                            file.getUrl(), thumb != null ? thumb.getUrl() : null));
                }
            } else if (e != null) {
                Log.w("Marketing", "Marketing class unavailable: " + e.getMessage());
            }
            loadLegacyVideos();
        });
    }

    /** Existing rows in the old "Videos" class still appear, as videos. */
    private void loadLegacyVideos() {
        ParseQuery<ParseObject> query = new ParseQuery<>("Videos");
        query.setLimit(1000);
        query.addAscendingOrder("Order");
        query.findInBackground((objects, e) -> {
            if (!isAdded()) return;
            if (e == null && objects != null) {
                for (ParseObject o : objects) {
                    ParseFile video = o.getParseFile("Video");
                    ParseFile thumb = o.getParseFile("Thumb");
                    if (video == null) continue;
                    allAssets.add(new MarketingAsset(
                            o.getObjectId(), o.getString("Name"), MarketingAsset.TYPE_VIDEO,
                            o.getString("Category"), o.getString("Brand"),
                            video.getUrl(), thumb != null ? thumb.getUrl() : null));
                }
            }
            applyFilters();
        });
    }

    // ---------- filtering ----------

    private void setTypeFilter(String type) {
        typeFilter = type;
        AppCompatButton[] buttons = {filterAll, filterVideos, filterPdfs, filterImages};
        String[] types = {null, MarketingAsset.TYPE_VIDEO, MarketingAsset.TYPE_PDF, MarketingAsset.TYPE_IMAGE};
        for (int i = 0; i < buttons.length; i++) {
            boolean on = (types[i] == null && type == null) || (types[i] != null && types[i].equals(type));
            buttons[i].setBackgroundResource(on ? R.drawable.bg_chip_selected : R.drawable.bg_chip_light);
            buttons[i].setTextColor(ContextCompat.getColor(requireContext(),
                    on ? R.color.white : R.color.chipText));
        }
        applyFilters();
    }

    private void applyFilters() {
        String q = searchEditText.getText() == null ? ""
                : searchEditText.getText().toString().trim().toLowerCase();
        displayed.clear();
        for (MarketingAsset a : allAssets) {
            if (typeFilter != null && !typeFilter.equals(a.getType())) continue;
            if (!q.isEmpty()) {
                String name = a.getName() == null ? "" : a.getName().toLowerCase();
                String cat = a.getCategory() == null ? "" : a.getCategory().toLowerCase();
                if (!name.contains(q) && !cat.contains(q)) continue;
            }
            displayed.add(a);
        }
        adapter.notifyDataSetChanged();
        boolean empty = displayed.isEmpty();
        emptyStateView.setVisibility(empty ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(empty ? View.GONE : View.VISIBLE);
    }

    // ---------- open & share ----------

    @Override
    public void onOpen(MarketingAsset asset) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(asset.getFileUrl()));
        try {
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(getContext(), "Nothing on this device can open that file.",
                    Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onShare(MarketingAsset asset) {
        ProgressDialog progress = new ProgressDialog(requireContext());
        progress.setMessage("Preparing " + asset.getType() + "…");
        progress.setCancelable(false);
        progress.show();

        io.execute(() -> {
            try {
                File dir = new File(requireContext().getCacheDir(), "shared");
                if (!dir.exists() && !dir.mkdirs()) {
                    throw new IllegalStateException("could not create cache folder");
                }
                File out = new File(dir, asset.getDownloadName());
                try (InputStream in = new URL(asset.getFileUrl()).openStream();
                     FileOutputStream fos = new FileOutputStream(out)) {
                    byte[] buf = new byte[8192];
                    int n;
                    while ((n = in.read(buf)) != -1) {
                        fos.write(buf, 0, n);
                    }
                }
                Uri uri = FileProvider.getUriForFile(requireContext(),
                        requireContext().getPackageName() + ".fileprovider", out);
                main.post(() -> {
                    progress.dismiss();
                    if (!isAdded()) return;
                    Intent share = new Intent(Intent.ACTION_SEND);
                    share.setType(asset.getMimeType());
                    share.putExtra(Intent.EXTRA_STREAM, uri);
                    share.putExtra(Intent.EXTRA_TEXT, asset.getName());
                    share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    startActivity(Intent.createChooser(share, "Share " + asset.getName()));
                });
            } catch (Exception ex) {
                main.post(() -> {
                    progress.dismiss();
                    if (!isAdded()) return;
                    Log.e("Marketing", "share failed", ex);
                    Toast.makeText(getContext(), "Could not prepare that file to share.",
                            Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    // ---------- login ----------

    private void updateLoginButtonState() {
        loginButton.setText(ParseUser.getCurrentUser() != null ? "Logout" : "Login");
    }

    private void showLoginDialog() {
        final Dialog dialog = new Dialog(requireContext());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_login);

        TextInputEditText username = dialog.findViewById(R.id.edit_text_username);
        TextInputEditText password = dialog.findViewById(R.id.edit_text_password);
        Button submit = dialog.findViewById(R.id.button_submit_login);

        submit.setOnClickListener(v -> {
            String u = username.getText() == null ? "" : username.getText().toString().trim();
            String p = password.getText() == null ? "" : password.getText().toString().trim();
            if (u.isEmpty() || p.isEmpty()) {
                Toast.makeText(getContext(), "Username and password cannot be empty.",
                        Toast.LENGTH_SHORT).show();
                return;
            }
            ParseUser.logInInBackground(u, p, (user, e) -> {
                if (user != null) {
                    Toast.makeText(getContext(), "Login Successful!", Toast.LENGTH_SHORT).show();
                    updateLoginButtonState();
                    dialog.dismiss();
                } else {
                    Toast.makeText(getContext(), "Login failed: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                }
            });
        });

        dialog.show();
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        io.shutdownNow();
    }
}
