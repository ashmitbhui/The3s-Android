package in.appoholics.the3s;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;

import com.bumptech.glide.Glide;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;

/**
 * Shares one or more products with every image and the full details.
 * Logged-in dealers first choose how prices appear (list / with margin / none);
 * browsing users always share without prices. Mirrors the iOS SharePricingSheet.
 */
public final class ProductShare {

    /** One product to share: text fields plus every image URL (cover first). */
    public static final class Item {
        final String name;
        @Nullable final Double price;
        @Nullable final String detail;
        @Nullable final Number moq;
        @Nullable final Number masterPacking;
        final List<String> imageUrls;

        public Item(String name, @Nullable Double price, @Nullable String detail,
                    @Nullable Number moq, @Nullable Number masterPacking, List<String> imageUrls) {
            this.name = name == null ? "" : name;
            this.price = price;
            this.detail = detail;
            this.moq = moq;
            this.masterPacking = masterPacking;
            this.imageUrls = imageUrls == null ? new ArrayList<>() : imageUrls;
        }
    }

    /** How prices appear in the shared text. */
    public static final class Pricing {
        static final int NONE = 0, LIST = 1, MARGIN = 2;
        final int mode;
        final double value;
        final boolean isPercent;

        private Pricing(int mode, double value, boolean isPercent) {
            this.mode = mode; this.value = value; this.isPercent = isPercent;
        }
        static Pricing none() { return new Pricing(NONE, 0, false); }
        static Pricing list() { return new Pricing(LIST, 0, false); }
        static Pricing margin(double v, boolean pct) { return new Pricing(MARGIN, v, pct); }

        @Nullable Double apply(@Nullable Double p) {
            if (p == null || mode == NONE) return null;
            if (mode == LIST) return p;
            return isPercent ? p * (1 + value / 100.0) : p + value;
        }
    }

    private ProductShare() {}

    /** Entry point: asks a logged-in dealer about prices, then shares. */
    public static void share(Activity activity, List<Item> items) {
        if (items == null || items.isEmpty()) return;
        if (UserStatus.isLoggedIn()) {
            askPricing(activity, items.size(), pricing -> doShare(activity, items, pricing));
        } else {
            doShare(activity, items, Pricing.none());
        }
    }

    interface PricingCallback { void onChosen(Pricing pricing); }

    private static void askPricing(Activity activity, int count, PricingCallback cb) {
        final Dialog dialog = new Dialog(activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_share_pricing);

        TextView title = dialog.findViewById(R.id.share_pricing_title);
        RadioGroup group = dialog.findViewById(R.id.share_pricing_group);
        View marginRow = dialog.findViewById(R.id.margin_row);
        RadioGroup marginType = dialog.findViewById(R.id.margin_type_group);
        EditText marginValue = dialog.findViewById(R.id.margin_value);
        Button noPrice = dialog.findViewById(R.id.share_no_price_button);
        Button cont = dialog.findViewById(R.id.share_continue_button);

        title.setText(count > 1 ? "Share " + count + " products" : "Share product");
        group.setOnCheckedChangeListener((g, id) -> {
            boolean margin = id == R.id.radio_add_margin;
            marginRow.setVisibility(margin ? View.VISIBLE : View.GONE);
            if (margin) marginValue.requestFocus();
        });
        marginType.setOnCheckedChangeListener((g, id) ->
                marginValue.setHint(id == R.id.radio_margin_percent ? "e.g. 15" : "e.g. 50"));

        noPrice.setOnClickListener(v -> { dialog.dismiss(); cb.onChosen(Pricing.none()); });
        cont.setOnClickListener(v -> {
            if (group.getCheckedRadioButtonId() == R.id.radio_add_margin) {
                double margin;
                try {
                    margin = Double.parseDouble(marginValue.getText().toString().trim().replace(",", ""));
                } catch (NumberFormatException e) { margin = -1; }
                if (margin < 0) {
                    marginValue.setError("Enter a margin");
                    return;
                }
                boolean pct = marginType.getCheckedRadioButtonId() == R.id.radio_margin_percent;
                dialog.dismiss();
                cb.onChosen(Pricing.margin(margin, pct));
            } else {
                dialog.dismiss();
                cb.onChosen(Pricing.list());
            }
        });

        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
    }

    /** Name, price (per pricing) and the detail without trade-only lines. */
    static String shareText(Item item, Pricing pricing) {
        StringBuilder sb = new StringBuilder(item.name);
        Double p = pricing.apply(item.price);
        if (p != null) sb.append("\nPrice: ").append(formatPrice(p));
        String d = detailForSharing(item.detail);
        if (!d.isEmpty()) sb.append("\n").append(d);
        return sb.toString();
    }

    private static final String[] TRADE_ONLY = {
            "minimum qty", "min qty", "min. qty", "minimum quantity", "moq", "master pack"};

    /**
     * Detail text without minimum-qty / MOQ / master-packing lines. Details are
     * newline-separated (3S) or "; "-separated (Verbose); both are handled.
     */
    static String detailForSharing(@Nullable String detail) {
        if (detail == null) return "";
        StringBuilder out = new StringBuilder();
        for (String line : detail.split("\\r?\\n")) {
            StringBuilder kept = new StringBuilder();
            for (String part : line.split(";")) {
                String t = part.trim();
                if (t.isEmpty()) continue;
                String low = t.toLowerCase(Locale.ROOT);
                boolean trade = false;
                for (String k : TRADE_ONLY) if (low.contains(k)) { trade = true; break; }
                if (trade) continue;
                if (kept.length() > 0) kept.append("; ");
                kept.append(t);
            }
            if (kept.length() > 0) {
                if (out.length() > 0) out.append("\n");
                out.append(kept);
            }
        }
        return out.toString().trim();
    }

    static String formatPrice(double p) {
        if (p == Math.rint(p)) return String.format(Locale.US, "₹%,.0f", p);
        return String.format(Locale.US, "₹%,.2f", p);
    }

    private static void doShare(Activity activity, List<Item> items, Pricing pricing) {
        ProgressDialog progress = new ProgressDialog(activity);
        progress.setMessage("Preparing images…");
        progress.setCancelable(false);
        progress.show();

        Executors.newSingleThreadExecutor().execute(() -> {
            StringBuilder text = new StringBuilder();
            ArrayList<Uri> uris = new ArrayList<>();
            int n = 0;
            File dir = new File(activity.getCacheDir(), "images");
            if (!dir.exists()) dir.mkdirs();

            boolean cards = items.size() > 1;   // several products: one card image per product
            for (Item item : items) {
                if (cards) {
                    // Photo + this product's own details in one image, so every picture
                    // in WhatsApp carries the right text (only one caption per batch otherwise).
                    android.graphics.Bitmap photo = null;
                    String url = item.imageUrls.isEmpty() ? null : item.imageUrls.get(0);
                    if (url != null && !url.isEmpty()) {
                        try {
                            photo = Glide.with(activity).asBitmap().load(url).submit(1000, 1000).get();
                        } catch (Exception e) {
                            Log.e("ProductShare", "Could not load " + url, e);
                        }
                    }
                    Double p = pricing.apply(item.price);
                    android.graphics.Bitmap card = ShareCard.render(photo, item.name,
                            p == null ? null : formatPrice(p), detailForSharing(item.detail));
                    try {
                        File dest = new File(dir, "share_card_" + (++n) + ".jpg");
                        try (OutputStream out = new FileOutputStream(dest)) {
                            card.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, out);
                        }
                        uris.add(FileProvider.getUriForFile(activity,
                                activity.getPackageName() + ".fileprovider", dest));
                    } catch (Exception e) {
                        Log.e("ProductShare", "Could not write card", e);
                    }
                    continue;
                }
                if (text.length() > 0) text.append("\n\n");
                text.append(shareText(item, pricing));
                for (String url : item.imageUrls) {
                    if (url == null || url.isEmpty()) continue;
                    try {
                        File downloaded = Glide.with(activity).asFile().load(url).submit().get();
                        File dest = new File(dir, "share_image_" + (++n) + ".jpg");
                        copy(downloaded, dest);
                        uris.add(FileProvider.getUriForFile(activity,
                                activity.getPackageName() + ".fileprovider", dest));
                    } catch (Exception e) {
                        Log.e("ProductShare", "Could not prepare " + url, e);
                    }
                }
            }

            activity.runOnUiThread(() -> {
                progress.dismiss();
                Intent intent;
                if (uris.isEmpty()) {
                    intent = new Intent(Intent.ACTION_SEND).setType("text/plain");
                } else if (uris.size() == 1) {
                    intent = new Intent(Intent.ACTION_SEND).setType("image/*")
                            .putExtra(Intent.EXTRA_STREAM, uris.get(0));
                } else {
                    intent = new Intent(Intent.ACTION_SEND_MULTIPLE).setType("image/*")
                            .putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
                }
                if (text.length() > 0) intent.putExtra(Intent.EXTRA_TEXT, text.toString());
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                try {
                    activity.startActivity(Intent.createChooser(intent, "Share via"));
                } catch (Exception e) {
                    Toast.makeText(activity, "No app available to share", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private static void copy(File src, File dst) throws Exception {
        try (InputStream in = new FileInputStream(src); OutputStream out = new FileOutputStream(dst)) {
            byte[] buf = new byte[8192];
            int len;
            while ((len = in.read(buf)) > 0) out.write(buf, 0, len);
        }
    }
}
