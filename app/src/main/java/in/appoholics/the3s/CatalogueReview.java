package in.appoholics.the3s;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RadioGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.parse.ParseUser;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class CatalogueReview extends AppCompatActivity implements CatalogueReviewAdapter.OnItemRemovedListener {

    private CatalogueReviewAdapter adapter;
    private List<Product> masterSelectedProducts;
    private final List<Product> displayedProducts = new ArrayList<>();
    private ChipGroup categoryChipGroup;
    private RecyclerView recyclerView;
    private View loggedInOptionsContainer;
    ImageButton back;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_catalogue_review);
        EdgeToEdgeHelper.apply(this, R.id.catalogue_review_root, R.id.app_bar_layout, R.id.confirm_button_container);

        masterSelectedProducts = getIntent().getParcelableArrayListExtra("SELECTED_PRODUCTS");
        if (masterSelectedProducts == null || masterSelectedProducts.isEmpty()) {
            Toast.makeText(this, "No products selected.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initializeViews();
        setupToolbar();
        setupRecyclerView();
        populateCategoryChips();

        // Show extra options only if the user is logged in
        if (ParseUser.getCurrentUser() != null) {
            loggedInOptionsContainer.setVisibility(View.VISIBLE);
        }
    }

    private void initializeViews() {
        categoryChipGroup = findViewById(R.id.category_chip_group);
        recyclerView = findViewById(R.id.review_recycler_view);
        loggedInOptionsContainer = findViewById(R.id.logged_in_options_container);
        MaterialButton confirmButton = findViewById(R.id.confirm_button);
        MaterialButton addMarginButton = findViewById(R.id.add_margin_button);
        back = findViewById(R.id.back_button);

        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
        confirmButton.setOnClickListener(v -> showNameFileDialog());
        addMarginButton.setOnClickListener(v -> showAddMarginDialog());
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void setupRecyclerView() {
        adapter = new CatalogueReviewAdapter(this, displayedProducts, this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    private void populateCategoryChips() {
        categoryChipGroup.removeAllViews();
        Set<String> categories = new HashSet<>();
        for (Product product : masterSelectedProducts) {
            categories.add(product.getCategory());
        }

        addChipToGroup("All");
        List<String> sortedCategories = new ArrayList<>(categories);
        Collections.sort(sortedCategories);
        for (String category : sortedCategories) {
            addChipToGroup(category);
        }

        categoryChipGroup.setOnCheckedChangeListener((group, checkedId) -> {
            Chip selectedChip = group.findViewById(checkedId);
            if (selectedChip != null) {
                filterProductsByCategory(selectedChip.getText().toString());
            } else if (group.getChildCount() > 0) {
                // Default back to "All" if nothing is selected
                ((Chip) group.getChildAt(0)).setChecked(true);
            }
        });

        if (categoryChipGroup.getChildCount() > 0) {
            ((Chip) categoryChipGroup.getChildAt(0)).setChecked(true);
        }
    }

    private void addChipToGroup(String text) {
        Chip chip = (Chip) getLayoutInflater().inflate(R.layout.item_category_chip, categoryChipGroup, false);
        chip.setText(text);
        categoryChipGroup.addView(chip);
    }

    private void filterProductsByCategory(String category) {
        List<Product> filteredList;
        if (category.equals("All")) {
            filteredList = new ArrayList<>(masterSelectedProducts);
        } else {
            filteredList = masterSelectedProducts.stream()
                    .filter(p -> category.equals(p.getCategory()))
                    .collect(Collectors.toList());
        }
        adapter.updateList(filteredList);
    }

    @Override
    public void onItemRemoved(int position) {
        Product productToRemove = displayedProducts.get(position);
        masterSelectedProducts.remove(productToRemove);

        if (masterSelectedProducts.isEmpty()) {
            Toast.makeText(this, "Catalogue is now empty.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        // Refresh the chips and the list
        populateCategoryChips();
    }

    private void showAddMarginDialog() {
        final Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_add_margin);
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        RadioGroup marginTypeGroup = dialog.findViewById(R.id.margin_type_group);
        EditText marginValueEditText = dialog.findViewById(R.id.margin_value_edittext);
        ((android.widget.RadioButton) dialog.findViewById(R.id.radio_value)).setText("Fixed Value (" + DealerCurrency.symbol() + ")");
        Button applyMarginButton = dialog.findViewById(R.id.apply_margin_button);

        applyMarginButton.setOnClickListener(v -> {
            String valueStr = marginValueEditText.getText().toString();
            if (valueStr.isEmpty()) {
                Toast.makeText(this, "Please enter a value.", Toast.LENGTH_SHORT).show();
                return;
            }
            double value = Double.parseDouble(valueStr);
            boolean isPercentage = marginTypeGroup.getCheckedRadioButtonId() == R.id.radio_percentage;

            for (Product product : masterSelectedProducts) {
                double originalPrice = product.getPrice();
                // a fixed margin is typed in the currency the dealer is viewing in; prices stay in INR
                double newPrice = isPercentage ? (originalPrice * (1 + (value / 100.0))) : (originalPrice + DealerCurrency.toINR(value));
                product.setPrice(newPrice);
            }

            adapter.notifyDataSetChanged();
            Toast.makeText(this, "Margin applied.", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });
        dialog.show();
    }

    private void showNameFileDialog() {
        final Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_name_file);
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        EditText filenameEditText = dialog.findViewById(R.id.filename_edittext);
        Button saveFilenameButton = dialog.findViewById(R.id.save_filename_button);
        String defaultName = "catalogue_" + System.currentTimeMillis();
        filenameEditText.setText(defaultName);

        saveFilenameButton.setOnClickListener(v -> {
            String filename = filenameEditText.getText().toString().trim();
            if (filename.isEmpty()) {
                filenameEditText.setError("Filename cannot be empty");
                return;
            }
            dialog.dismiss();
            createAndSharePdf(filename);
        });
        dialog.show();
    }

    private void createAndSharePdf(String filename) {
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Creating PDF Catalogue...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        boolean isLoggedIn = (ParseUser.getCurrentUser() != null);
        PdfGenerator.createCatalogue(this, masterSelectedProducts, isLoggedIn, filename, new PdfGenerator.PdfGenerationListener() {
            @Override
            public void onSuccess(Uri pdfUri) {
                progressDialog.dismiss();
                showPdfOptionsDialog(pdfUri);
            }
            @Override
            public void onFailure(Exception e) {
                progressDialog.dismiss();
                Toast.makeText(CatalogueReview.this, "Failed to create PDF.", Toast.LENGTH_SHORT).show();
                Log.e("PdfError", "PDF generation failed", e);
            }
        });
    }

    private void showPdfOptionsDialog(Uri pdfUri) {
        new AlertDialog.Builder(this)
                .setTitle("Catalogue Ready")
                .setPositiveButton("Share", (d, w) -> sharePdf(pdfUri))
                .setNeutralButton("Save to Downloads", (d, w) -> savePdfToDownloads(pdfUri))
                .setNegativeButton("Open", (d, w) -> openPdf(pdfUri))
                .show();
    }

    private void openPdf(Uri pdfUri) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(pdfUri, "application/pdf");
        // This flag is crucial for granting temporary permission to the viewer app
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        // Check if there is an app that can handle this intent
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
        } else {
            Toast.makeText(this, "No app found to open PDF.", Toast.LENGTH_SHORT).show();
        }
    }

    private void savePdfToDownloads(Uri pdfUri) {
        // File operations should happen on a background thread
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                ContentValues values = new ContentValues();
                values.put(MediaStore.MediaColumns.DISPLAY_NAME, "catalogue_" + System.currentTimeMillis() + ".pdf");
                values.put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf");

                // Scoped storage for Android 10 (Q) and above
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);
                }

                Uri downloadsUri = null;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    downloadsUri = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
                }

                if (downloadsUri != null) {
                    try (InputStream in = getContentResolver().openInputStream(pdfUri);
                         OutputStream out = getContentResolver().openOutputStream(downloadsUri)) {

                        byte[] buffer = new byte[1024];
                        int len;
                        while ((len = in.read(buffer)) > 0) {
                            out.write(buffer, 0, len);
                        }
                    }
                    // Notify user on the main thread
                    runOnUiThread(() -> Toast.makeText(this, "Saved to Downloads folder.", Toast.LENGTH_SHORT).show());
                }
            } catch (Exception e) {
                Log.e("SavePdfError", "Failed to save PDF", e);
                runOnUiThread(() -> Toast.makeText(this, "Failed to save file.", Toast.LENGTH_SHORT).show());
            }
        });
    }

    /**
     * Creates and launches an Intent to share the generated PDF file.
     * @param pdfUri The content URI of the PDF file provided by the FileProvider.
     */
    private void sharePdf(Uri pdfUri) {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);

        // Set the MIME type for a PDF file
        shareIntent.setType("application/pdf");

        // Add the PDF file's URI to the intent
        shareIntent.putExtra(Intent.EXTRA_STREAM, pdfUri);

        // Grant temporary read permission to the app the user chooses to share with
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        // Show the Android share sheet
        startActivity(Intent.createChooser(shareIntent, "Share Catalogue via"));
    }
}
