package in.appoholics.the3s;

import android.app.Dialog;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.parse.FindCallback;
import com.parse.GetCallback;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.tbuonomo.viewpagerdotsindicator.DotsIndicator;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.Locale;

public class ProductDetail extends AppCompatActivity {

    // UI Elements
    private ViewPager2 viewPager;
    private TabLayout tabLayout;
    private TextView nameTextView, priceTextView, descriptionTextView;
    private ImageButton favoriteButton;
    private MaterialButton shareButton;
    private DotsIndicator dotsIndicator;


    private MediaSliderAdapter sliderAdapter;

    private final ArrayList<MediaItem> mediaItems = new ArrayList<>();


    // State variables
    private ParseObject currentProduct;
    private boolean isCurrentlyFavorite = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_detail);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Enable the back arrow (up button)
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        initializeViews();

        String productId = getIntent().getStringExtra("PRODUCT_ID");
        if (productId != null && !productId.isEmpty()) {
            loadProductAndMedia(productId);
        } else {
            Toast.makeText(this, "Product ID not found.", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (sliderAdapter != null) {
            sliderAdapter.releasePlayer();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle arrow click here
        if (item.getItemId() == android.R.id.home) {
            finish(); // Close this activity and return to previous one
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void initializeViews() {
        viewPager = findViewById(R.id.product_image_slider);
        dotsIndicator = findViewById(R.id.dots_indicator);
        nameTextView = findViewById(R.id.product_detail_name);
        priceTextView = findViewById(R.id.product_detail_price);
        descriptionTextView = findViewById(R.id.product_detail_description);
        favoriteButton = findViewById(R.id.product_detail_favorite);
        shareButton = findViewById(R.id.share_button);

    }

    private void loadProductAndMedia(String productId) {
        // Step 1: Fetch the main product object from the "Products" class
        ParseQuery<ParseObject> productQuery = ParseQuery.getQuery("Products");
        productQuery.include("Media");
        productQuery.getInBackground(productId, (product, e) -> {
            if (e == null && product != null) {
                currentProduct = product; // Store the loaded product
                String productName = product.getString("Name");
                String productCategory = product.getString("Category");
                // Populate UI text fields
                populateTextViews(product);

                // Check its favorite status against the local Room DB
                checkFavoriteStatus(productId);

                // Setup the favorite button's click listener
                setupFavoriteButtonListener();

                mediaItems.clear();
                ParseFile mainImageFile = product.getParseFile("Image");

                if (mainImageFile != null) {
                    mediaItems.add(new MediaItem(mainImageFile.getUrl(), "image"));
                }

                // Setup the share button listener
                // setupShareButtonListener(); // You can add this logic

                // Step 2: Fetch associated media from the "Media" class
//                fetchMediaForProduct(productName, productCategory);
                ArrayList<ParseObject> mediaObjects = new ArrayList<>();
                if (product.getList("Media") != null){
                    mediaObjects.addAll(product.getList("Media"));
                    if (mediaObjects.size() > 0){
                        for (ParseObject media : mediaObjects) {
                            ParseFile file = media.getParseFile("File");
                            String type = media.getString("Type"); // Get the type ("image" or "video")

                            if (file != null && type != null) {
                                // Create a MediaItem object with both URL and type
                                mediaItems.add(new MediaItem(file.getUrl(), type));
                            }
                        }
                    }
                }
                setupMediaSlider();

            } else {
                Log.e("ParseError", "Error fetching product: " + (e != null ? e.getMessage() : "Product is null"));
                Toast.makeText(ProductDetail.this, "Failed to load product details.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void populateTextViews(ParseObject product) {
        nameTextView.setText(product.getString("Name"));
        descriptionTextView.setText(product.getString("Detail"));

        if (ParseUser.getCurrentUser() != null) {
            // If logged in, show the price
            NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));
            currencyFormat.setMaximumFractionDigits(0);
            priceTextView.setText(currencyFormat.format(product.getNumber("Price")));
            priceTextView.setVisibility(View.VISIBLE);
        } else {
            // If not logged in, hide the price
            priceTextView.setVisibility(View.GONE);
        }
    }

    private void checkFavoriteStatus(String productId) {
        // Run database check on a background thread
        AppDatabase.databaseWriteExecutor.execute(() -> {
            AppDatabase db = AppDatabase.getDatabase(this);
            FavoriteProduct favorite = db.favoriteProductDao().getFavoriteById(productId);
            isCurrentlyFavorite = (favorite != null);

            // Update UI on the main thread
            runOnUiThread(this::updateFavoriteIcon);
        });
    }

    private void updateFavoriteIcon() {
        favoriteButton.setImageResource(
                isCurrentlyFavorite ? R.drawable.ic_favourite_filled : R.drawable.ic_favourite_outline
        );
    }

    private void setupFavoriteButtonListener() {
        favoriteButton.setOnClickListener(v -> {
            if (currentProduct == null) return;

            // 1. Instantly toggle the state and update the UI
            isCurrentlyFavorite = !isCurrentlyFavorite;
            updateFavoriteIcon();

            // 2. Perform the database operation in the background
            AppDatabase.databaseWriteExecutor.execute(() -> {
                AppDatabase db = AppDatabase.getDatabase(this);
                FavoriteProductDao dao = db.favoriteProductDao();
                String productId = currentProduct.getObjectId();

                if (isCurrentlyFavorite) {
                    // If it's now a favorite, insert it
                    ParseFile imageFile = currentProduct.getParseFile("Image");
                    FavoriteProduct newFavorite = new FavoriteProduct(
                            productId,
                            currentProduct.getString("Name"),
                            currentProduct.getString("Detail"),
                            String.valueOf(currentProduct.getNumber("Price")),
                            imageFile != null ? imageFile.getUrl() : "", currentProduct.getString("Category"), // ✨ ADDED
                            currentProduct.getString("Brand")

                    );
                    dao.insert(newFavorite);
                } else {
                    // If it's no longer a favorite, create a dummy object with the same ID to delete
                    FavoriteProduct toDelete = new FavoriteProduct(productId, "", "", "", "", "", "");
                    dao.delete(toDelete);
                }
            });
        });
    }

    private void fetchMediaForProduct(String productName, String productCategory) {
        ParseQuery<ParseObject> mediaQuery = ParseQuery.getQuery("Media");
        mediaQuery.whereEqualTo("Product", productName);
        mediaQuery.whereEqualTo("Category", productCategory);
        mediaQuery.orderByAscending("Name");

        mediaQuery.findInBackground((mediaObjects, e) -> {
            if (e == null) {
                for (ParseObject media : mediaObjects) {
                    ParseFile file = media.getParseFile("File");
                    String type = media.getString("Type"); // Get the type ("image" or "video")

                    if (file != null && type != null) {
                        // Create a MediaItem object with both URL and type
                        mediaItems.add(new MediaItem(file.getUrl(), type));
                    }
                }
                setupMediaSlider();
            } else {
                Log.e("ParseError", "Error fetching media: " + e.getMessage());
            }
        });
    }

    private void setupMediaSlider() {

        Log.d("ProductDetailActivity", "Setting up slider. Number of media items: " + mediaItems.size());

        sliderAdapter = new MediaSliderAdapter(this, mediaItems);
        viewPager.setAdapter(sliderAdapter);

        dotsIndicator.setViewPager2(viewPager);
    }
}