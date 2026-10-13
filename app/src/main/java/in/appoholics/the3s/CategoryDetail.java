package in.appoholics.the3s;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.slider.RangeSlider;
import com.google.android.material.textfield.TextInputEditText;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Currency;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class CategoryDetail extends AppCompatActivity implements ProductAdapter.OnProductSelectionChangedListener {

    private RecyclerView productRecyclerView;
    private ProductAdapter productAdapter;
    private TextView categoryHeader;
    private Button shareSelectedButton, loginButton;
    ;
    private ImageButton backButton;

    // Maintain two lists: one for the original data, one for the filtered/sorted data
    private final List<Product> masterProductList = new ArrayList<>();
    private final List<Product> displayedProductList = new ArrayList<>();

    // Keep track of current filter/sort state
    private int currentSortOptionId = R.id.radio_default;
    private float minPrice = 0;
    private float maxPrice = 1500;
    private double overallMinPrice = 0.0;
    private double overallMaxPrice = 1500.0; // Default max

    @Override
    protected void onResume() {
        super.onResume();
        updateLoginButtonState();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_category_detail);
        EdgeToEdgeHelper.apply(this, android.R.id.content, R.id.category_detail_content, R.id.share_selected_button);

        initializeViews();
        setupRecyclerView();

        String categoryName = getIntent().getStringExtra("CATEGORY_NAME");
        String brandName = getIntent().getStringExtra("BRAND_NAME");
        applyBrandColour(brandName);

        if (categoryName != null && !categoryName.isEmpty()) {
            categoryHeader.setText(categoryName);
            loadProductsFromParse(categoryName, brandName);
            loadCategoryCatalogue(categoryName, brandName);
        } else {
            Toast.makeText(this, "Category not found", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    /**
     * Colours the header (and the status bar area above it) in the brand colour
     * of the world this category belongs to.
     */
    /** 'Coaster' should match the 'Coasters' category — compare trailing-s-insensitively. */
    private static String normName(String s) {
        String t = (s == null ? "" : s).toLowerCase().replaceAll("[^a-z0-9]", "");
        return t.endsWith("s") ? t.substring(0, t.length() - 1) : t;
    }

    /** Shows the 'Category catalogue' bar when the portal has a PDF for this category. */
    private void loadCategoryCatalogue(String categoryName, @Nullable String brandName) {
        String brand = brandName != null ? brandName : "3S";
        ParseQuery<ParseObject> q = new ParseQuery<>("Catalogue");
        q.whereEqualTo("isEnabled", true);
        q.whereEqualTo("Brand", brand);
        q.findInBackground((rows, e) -> {
            if (e != null || rows == null || isFinishing()) return;
            ParseObject match = null;
            for (ParseObject r : rows) {
                if (normName(r.getString("Name")).equals(normName(categoryName))) { match = r; break; }
            }
            if (match == null || match.getParseFile("File") == null) return;
            final String url = match.getParseFile("File").getUrl();
            final String fileName = categoryName.replaceAll("[^\\w\\- ]+", "").trim() + " Catalogue.pdf";
            View bar = findViewById(R.id.catalogue_bar);
            android.widget.TextView sub = findViewById(R.id.catalogue_sub);
            sub.setText("Full " + categoryName + " range as a PDF");
            bar.setVisibility(View.VISIBLE);
            findViewById(R.id.catalogue_view).setOnClickListener(v -> openCataloguePdf(url, fileName, false));
            findViewById(R.id.catalogue_share).setOnClickListener(v -> openCataloguePdf(url, fileName, true));
        });
    }

    /** Downloads the catalogue PDF to cache, then opens it (or shares it). */
    private void openCataloguePdf(String url, String fileName, boolean share) {
        ProgressDialog progress = new ProgressDialog(this);
        progress.setMessage(share ? "Preparing catalogue…" : "Opening catalogue…");
        progress.setCancelable(false);
        progress.show();
        ExecutorService ex = Executors.newSingleThreadExecutor();
        ex.execute(() -> {
            Uri uri = null;
            try {
                File dir = new File(getCacheDir(), "catalogues");
                if (!dir.exists()) dir.mkdirs();
                File out = new File(dir, fileName);
                try (InputStream in = new java.net.URL(url).openStream();
                     OutputStream fos = new FileOutputStream(out)) {
                    byte[] buf = new byte[8192];
                    int len;
                    while ((len = in.read(buf)) > 0) fos.write(buf, 0, len);
                }
                uri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", out);
            } catch (Exception err) {
                Log.e("Catalogue", "download failed", err);
            }
            final Uri result = uri;
            runOnUiThread(() -> {
                progress.dismiss();
                if (result == null) { Toast.makeText(this, "Could not load the catalogue.", Toast.LENGTH_SHORT).show(); return; }
                Intent intent;
                if (share) {
                    intent = new Intent(Intent.ACTION_SEND).setType("application/pdf")
                            .putExtra(Intent.EXTRA_STREAM, result);
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    startActivity(Intent.createChooser(intent, "Share catalogue"));
                } else {
                    intent = new Intent(Intent.ACTION_VIEW).setDataAndType(result, "application/pdf");
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    try { startActivity(intent); }
                    catch (Exception noApp) { Toast.makeText(this, "No app found to open PDF.", Toast.LENGTH_SHORT).show(); }
                }
            });
        });
    }

    private void applyBrandColour(String brandName) {
        int colour = 0xFF16604A; // 3S default
        if ("Trophyverse".equals(brandName)) {
            colour = 0xFF5E0021;
        } else if ("Verbose".equals(brandName)) {
            colour = 0xFF0032D2;
        }
        findViewById(R.id.header_bar).setBackgroundColor(colour);
        // API 35+ ignores setStatusBarColor; the content container paints the inset strip instead
        findViewById(R.id.category_detail_content).setBackgroundColor(colour);
        getWindow().setStatusBarColor(colour);
        androidx.core.view.WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView())
                .setAppearanceLightStatusBars(false);
    }

    @Override
    public void onSelectionChanged(int selectedCount) {
        if (selectedCount > 0) {
            shareSelectedButton.setText("Share Selected (" + selectedCount + ")");
            if (shareSelectedButton.getVisibility() == View.GONE) {
                shareSelectedButton.setVisibility(View.VISIBLE);
                // Optional: Add a simple animation
                shareSelectedButton.animate().translationY(0).setDuration(200);
            }
        } else {
            // Optional: Animate the button out
            shareSelectedButton.animate().translationY(shareSelectedButton.getHeight() + 50).setDuration(200)
                    .withEndAction(() -> shareSelectedButton.setVisibility(View.GONE));
        }
    }

    private void initializeViews() {
        AppCompatButton sortButton = findViewById(R.id.sort_button);
        AppCompatButton filterButton = findViewById(R.id.filter_button);
        productRecyclerView = findViewById(R.id.product_recycler_view);
        categoryHeader = findViewById(R.id.category_detail_header);
        shareSelectedButton = findViewById(R.id.share_selected_button);
        backButton = findViewById(R.id.back_button);
        loginButton = findViewById(R.id.login);

        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        loginButton.setOnClickListener(v -> {
            // Check if a user is currently logged in
            if (ParseUser.getCurrentUser() != null) {
                // If yes, perform logout
                ParseUser.logOutInBackground(e -> {
                    if (e == null) {
                        Toast.makeText(CategoryDetail.this, "Logged out successfully.", Toast.LENGTH_SHORT).show();
                        updateLoginButtonState();
                    } else {
                        Toast.makeText(CategoryDetail.this, "Logout failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                // If no, show the login dialog
                showLoginDialog();
            }
            productAdapter.notifyDataSetChanged();
        });

        sortButton.setOnClickListener(v -> showSortDialog());
        filterButton.setOnClickListener(v -> showFilterDialog());
        shareSelectedButton.setOnClickListener(v -> shareSelectedProducts());
    }

    private void updateLoginButtonState() {
        boolean loggedIn = ParseUser.getCurrentUser() != null;
        loginButton.setText(loggedIn ? "Logout" : "Login");
        // price sort / price-range filter only make sense once prices are visible
        View sortFilterRow = findViewById(R.id.sort_filter_row);
        if (sortFilterRow != null) sortFilterRow.setVisibility(loggedIn ? View.VISIBLE : View.GONE);
        if (productAdapter != null) productAdapter.notifyDataSetChanged();
    }

    private void setupRecyclerView() {
        productAdapter = new ProductAdapter(this, displayedProductList, this);
        productAdapter.setOnProductSelectionChangedListener(this);
        productRecyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        productRecyclerView.setAdapter(productAdapter);
    }

    private void loadProductsFromParse(String categoryName, @Nullable String brandName) {
        SharedPreferences prefs = getSharedPreferences("AppSettings", MODE_PRIVATE);
        String currentSeries = prefs.getString("CURRENT_SERIES", null);

        ParseQuery<ParseObject> query = new ParseQuery<>("Products");
        query.whereEqualTo("Category", categoryName);

        if (brandName != null && !brandName.isEmpty()) {
            query.whereEqualTo("Brand", brandName);
        }

        if (currentSeries != null && !currentSeries.isEmpty()) {
            query.whereEqualTo("Series", currentSeries);
        }

        query.whereEqualTo("isEnabled", true);
        query.orderByAscending("Order");

        query.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> objects, ParseException e) {
                if (e == null) {
                    masterProductList.clear();
                    for (ParseObject productObject : objects) {
                        String objectId = productObject.getObjectId();
                        String name = productObject.getString("Name");
                        String detail = productObject.getString("Detail");
                        double price = DealerPricing.apply(productObject.getNumber("Price").doubleValue());
                        String imageUrl = "";
                        ParseFile imageFile = productObject.getParseFile("Image");
                        if (imageFile != null) {
                            imageUrl = imageFile.getUrl();
                        }
                        String category = productObject.getString("Category");
                        String brand = productObject.getString("Brand");
                        masterProductList.add(new Product(objectId, name, detail, price, imageUrl, category, brand));
                    }
                    if (!masterProductList.isEmpty()) {
                        double min = Double.MAX_VALUE;
                        double max = Double.MIN_VALUE;
                        for (Product p : masterProductList) {
                            if (p.getPrice() < min) min = p.getPrice();
                            if (p.getPrice() > max) max = p.getPrice();
                        }
                        overallMinPrice = min;
                        overallMaxPrice = max;

                        // Set the initial filter range to include all products
                        minPrice = (float) overallMinPrice;
                        maxPrice = (float) overallMaxPrice;
                    }

                    applyFiltersAndSort(); // Apply default filters and sort after loading
                } else {
                    Log.e("ParseError", "Error loading products: " + e.getMessage());
                    Toast.makeText(CategoryDetail.this, "Failed to load products.", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void showLoginDialog() {
        final Dialog dialog = new Dialog(CategoryDetail.this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_login);

        TextInputEditText usernameEditText = dialog.findViewById(R.id.edit_text_username);
        TextInputEditText passwordEditText = dialog.findViewById(R.id.edit_text_password);
        Button submitButton = dialog.findViewById(R.id.button_submit_login);

        submitButton.setOnClickListener(v -> {
            String username = usernameEditText.getText().toString().trim();
            String password = passwordEditText.getText().toString().trim();

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(CategoryDetail.this, "Username and password cannot be empty.", Toast.LENGTH_SHORT).show();
                return;
            }

            ParseUser.logInInBackground(username, password, (user, e) -> {
                if (user != null) {
                    // Login successful
                    Toast.makeText(CategoryDetail.this, "Login Successful!", Toast.LENGTH_SHORT).show();
                    updateLoginButtonState(); // Update the main button to "Logout"
                    dialog.dismiss();
                } else {
                    // Login failed
                    Toast.makeText(CategoryDetail.this, "Login failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        });

        dialog.show();
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
    }

    private void applyFiltersAndSort() {
        // 1. Start with a fresh list from the master data
        List<Product> filteredList = new ArrayList<>(masterProductList);

        // 2. Apply Price Filter using Java 8 streams
        filteredList = filteredList.stream()
                .filter(p -> p.getPrice() >= minPrice && p.getPrice() <= maxPrice)
                .collect(Collectors.toList());

        // 3. Apply Sorting
        if (currentSortOptionId == R.id.radio_low_to_high) {
            Collections.sort(filteredList, (p1, p2) -> Double.compare(p1.getPrice(), p2.getPrice()));
        } else if (currentSortOptionId == R.id.radio_high_to_low) {
            Collections.sort(filteredList, (p1, p2) -> Double.compare(p2.getPrice(), p1.getPrice()));
        } else {

        }

        // 4. Update the adapter's list with the final, processed data
        displayedProductList.clear();
        displayedProductList.addAll(filteredList);
        productAdapter.notifyDataSetChanged();
    }

    private void shareSelectedProducts() {
        ArrayList<ProductShare.Item> items = new ArrayList<>();
        for (Product product : masterProductList) {
            if (!product.isSelected()) continue;
            ArrayList<String> images = new ArrayList<>();
            if (product.getImageUrl() != null && !product.getImageUrl().isEmpty()) images.add(product.getImageUrl());
            items.add(new ProductShare.Item(product.getName(), product.getPrice(), product.getDetail(),
                    null, null, images));
        }
        if (items.isEmpty()) {
            Toast.makeText(this, "Select at least one product", Toast.LENGTH_SHORT).show();
            return;
        }
        ProductShare.share(this, items);
    }

    // Helper method to copy files
    private void copy(File src, File dst) throws Exception {
        try (InputStream in = new java.io.FileInputStream(src);
             OutputStream out = new FileOutputStream(dst)) {
            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
        }
    }

    private void showSortDialog() {
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_sort);

        RadioGroup sortRadioGroup = dialog.findViewById(R.id.sort_radio_group);
        Button applyButton = dialog.findViewById(R.id.apply_sort_button);
        Button cancelButton = dialog.findViewById(R.id.cancel_sort_button);

        sortRadioGroup.check(currentSortOptionId);

        applyButton.setOnClickListener(v -> {
            currentSortOptionId = sortRadioGroup.getCheckedRadioButtonId();
            applyFiltersAndSort();
            dialog.dismiss();
        });

        cancelButton.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
    }

    private void showFilterDialog() {
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_filter);

        RangeSlider rangeSlider = dialog.findViewById(R.id.dialog_price_range_slider);
        TextView priceRangeText = dialog.findViewById(R.id.price_range_text);
        Button applyButton = dialog.findViewById(R.id.apply_filter_button);
        Button cancelButton = dialog.findViewById(R.id.cancel_filter_button);

        rangeSlider.setValueFrom((float) overallMinPrice);
        rangeSlider.setValueTo((float) overallMaxPrice);
        // Set the thumbs to the currently selected range
        rangeSlider.setValues(minPrice, maxPrice);

        updatePriceRangeText(priceRangeText, rangeSlider.getValues());

        rangeSlider.addOnChangeListener((slider, value, fromUser) -> updatePriceRangeText(priceRangeText, slider.getValues()));

        applyButton.setOnClickListener(v -> {
            List<Float> values = rangeSlider.getValues();
            minPrice = values.get(0);
            maxPrice = values.get(1);
            applyFiltersAndSort();
            dialog.dismiss();
        });

        cancelButton.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
    }

    private void updatePriceRangeText(TextView textView, List<Float> values) {
        int digits = DealerCurrency.isINR() ? 0 : 2;
        String min = DealerCurrency.format(values.get(0), digits);
        String max = DealerCurrency.format(values.get(1), digits);
        textView.setText(String.format("%s - %s", min, max));
    }
}
