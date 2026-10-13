package in.appoholics.the3s;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CatalogueBuilder extends AppCompatActivity implements CatalogueAdapter.OnProductSelectionListener {

    ImageButton back;
    private RecyclerView recyclerView;
    private MaterialButton doneButton;
    private MaterialButtonToggleGroup brandToggleGroup;
    private ChipGroup categoryChipGroup;
    private Chip budgetChip;
    /** Customer budget per piece (on the dealer's price); null = no limit. */
    private Double budgetMin = null, budgetMax = null;
    private String currentCategory = null;
    private CatalogueAdapter adapter;


    private final List<Object> displayList = new ArrayList<>();
    private final Set<Product> selectedProducts = new HashSet<>();
    private Map<String, Map<String, List<Product>>> groupedProducts = new HashMap<>();
    private String selectedBrand = "3S";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_catalogue_builder);
        EdgeToEdgeHelper.apply(this, android.R.id.content, R.id.catalogue_builder_content, R.id.done_button_container);

        initializeViews();
        setupToolbar();
        setupRecyclerView();
        setupBrandToggle();
        loadAllProductsFromParse();
    }

    private void initializeViews() {
        back = findViewById(R.id.back_button);
        recyclerView = findViewById(R.id.product_recycler_view);
        doneButton = findViewById(R.id.done_button);
        brandToggleGroup = findViewById(R.id.brand_toggle_group);
        categoryChipGroup = findViewById(R.id.category_chip_group);
        budgetChip = findViewById(R.id.budget_chip);
        budgetChip.setOnClickListener(v -> showBudgetDialog());
        budgetChip.setOnCloseIconClickListener(v -> { budgetMin = null; budgetMax = null; onBudgetChanged(); });
        doneButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(CatalogueBuilder.this, CatalogueReview.class);
                intent.putParcelableArrayListExtra("SELECTED_PRODUCTS", new ArrayList<>(selectedProducts));
                startActivity(intent);
            }
        });
//        doneButton.setOnClickListener(v -> createAndSharePdf());
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
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
        adapter = new CatalogueAdapter(this, displayList, selectedProducts, this);
        GridLayoutManager layoutManager = new GridLayoutManager(this, 2);
        layoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                // Safety check to prevent crashing during layout changes
                if (position >= adapter.getItemCount()) {
                    return 1; // Return a default span size
                }

                // The original logic is now inside the safety check
                switch (adapter.getItemViewType(position)) {
                    case CatalogueAdapter.TYPE_BRAND:
                    case CatalogueAdapter.TYPE_CATEGORY:
                        return 2; // Headers span both columns
                    case CatalogueAdapter.TYPE_PRODUCT:
                    default:
                        return 1; // Products take 1 column
                }
            }
        });

        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(adapter);
    }

    private void setupBrandToggle() {
        brandToggleGroup.check(R.id.button_3s);
        brandToggleGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                if (checkedId == R.id.button_3s) {
                    selectedBrand = "3S";
                } else if (checkedId == R.id.button_trophyverse) {
                    selectedBrand = "Trophyverse";
                } else if (checkedId == R.id.button_verbose) {
                    selectedBrand = "Verbose";
                }
                updateCategoryChipsForBrand(selectedBrand);
            }
        });
    }

    private void loadAllProductsFromParse() {
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Loading Products...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        ParseQuery<ParseObject> query = new ParseQuery<>("Products");
        query.whereEqualTo("isEnabled", true);
        query.setLimit(1000);
        query.findInBackground((objects, e) -> {
            progressDialog.dismiss();
            if (e == null) {
                List<Product> allProducts = new ArrayList<>();
                for (ParseObject po : objects) {
                    allProducts.add(new Product(po.getObjectId(), po.getString("Name"), po.getString("Detail"), DealerPricing.apply(po.getNumber("Price").doubleValue()), po.getParseFile("Image") != null ? po.getParseFile("Image").getUrl() : "", po.getString("Category"), po.getString("Brand")));
                }
                processAndGroupProducts(allProducts);
            } else {
                Toast.makeText(this, "Failed to load products.", Toast.LENGTH_SHORT).show();
                Log.e("ParseError", "Error loading all products: " + e.getMessage());
            }
        });
    }

    private void processAndGroupProducts(List<Product> allProducts) {
        groupedProducts = new HashMap<>();
        for (Product product : allProducts) {
            String brand = product.getBrand() != null ? product.getBrand() : "Unbranded";
            String category = product.getCategory() != null ? product.getCategory() : "Uncategorized";
            groupedProducts.computeIfAbsent(brand, k -> new HashMap<>())
                    .computeIfAbsent(category, k -> new ArrayList<>())
                    .add(product);
        }
        updateCategoryChipsForBrand(selectedBrand);
    }

    private void updateCategoryChipsForBrand(String brand) {
        categoryChipGroup.removeAllViews();
        Map<String, List<Product>> categoriesForBrand = groupedProducts.get(brand);
        if (categoriesForBrand == null || categoriesForBrand.isEmpty()) {
            updateProductGrid(null);
            return;
        }

        List<String> sortedCategories = new ArrayList<>(categoriesForBrand.keySet());
        Collections.sort(sortedCategories);

        for (String categoryName : sortedCategories) {
            Chip chip = (Chip) getLayoutInflater().inflate(R.layout.item_category_chip, categoryChipGroup, false);
            chip.setText(categoryName);
            categoryChipGroup.addView(chip);
        }

        categoryChipGroup.setOnCheckedChangeListener((group, checkedId) -> {
            Chip selectedChip = group.findViewById(checkedId);
            updateProductGrid(selectedChip != null ? selectedChip.getText().toString() : null);
        });

        if (categoryChipGroup.getChildCount() > 0) {
            ((Chip) categoryChipGroup.getChildAt(0)).setChecked(true);
        }
    }

    private void updateProductGrid(@Nullable String category) {
        currentCategory = category;
        displayList.clear();
        if (category != null && groupedProducts.containsKey(selectedBrand)) {
            Map<String, List<Product>> categoriesMap = groupedProducts.get(selectedBrand);
            if (categoriesMap != null && categoriesMap.containsKey(category)) {
                // Since this is the Catalogue Builder, we add the Category Header first
                displayList.add(new CategoryHeader(category));
                for (Product p : categoriesMap.get(category)) {
                    if (inBudget(p)) displayList.add(p);
                }
            }
        }
        // This is a simplified logic. A full implementation would rebuild the whole
        // displayList with Brand and all Category headers for the selected brand.
        // For simplicity, we'll just show one category at a time.
        adapter.notifyDataSetChanged();
    }

    // ---- customer budget filter ----

    private boolean inBudget(Product p) {
        if (budgetMin == null && budgetMax == null) return true;
        double price = p.getPrice();
        if (budgetMin != null && price < budgetMin) return false;
        if (budgetMax != null && price > budgetMax) return false;
        return true;
    }

    private void onBudgetChanged() {
        boolean on = budgetMin != null || budgetMax != null;
        String label;
        if (budgetMin != null && budgetMax != null) label = DealerCurrency.format(budgetMin, 0) + " – " + DealerCurrency.format(budgetMax, 0);
        else if (budgetMin != null) label = "From " + DealerCurrency.format(budgetMin, 0);
        else if (budgetMax != null) label = "Up to " + DealerCurrency.format(budgetMax, 0);
        else label = "Budget";
        budgetChip.setText(label);
        budgetChip.setCloseIconVisible(on);
        budgetChip.setChipBackgroundColorResource(on ? R.color.primaryColor : android.R.color.white);
        budgetChip.setTextColor(getResources().getColor(on ? android.R.color.white : R.color.chipText, getTheme()));
        budgetChip.setChipIconTintResource(on ? android.R.color.white : R.color.chipText);
        budgetChip.setCloseIconTintResource(android.R.color.white);
        updateProductGrid(currentCategory);
    }

    private void showBudgetDialog() {
        final android.app.Dialog dialog = new android.app.Dialog(this);
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_budget);
        android.widget.EditText minField = dialog.findViewById(R.id.budget_min);
        android.widget.EditText maxField = dialog.findViewById(R.id.budget_max);
        minField.setHint("From " + DealerCurrency.symbol());
        maxField.setHint("Up to " + DealerCurrency.symbol());
        if (budgetMin != null) minField.setText(String.valueOf(Math.round(DealerCurrency.convert(budgetMin))));
        if (budgetMax != null) maxField.setText(String.valueOf(Math.round(DealerCurrency.convert(budgetMax))));
        dialog.findViewById(R.id.budget_clear).setOnClickListener(v -> {
            budgetMin = null; budgetMax = null; onBudgetChanged(); dialog.dismiss();
        });
        dialog.findViewById(R.id.budget_apply).setOnClickListener(v -> {
            Double lo = parseBudget(minField.getText().toString());
            Double hi = parseBudget(maxField.getText().toString());
            if (lo != null && hi != null && lo > hi) {
                maxField.setError("Must be at least the From amount");
                return;
            }
            budgetMin = lo == null ? null : DealerCurrency.toINR(lo);
            budgetMax = hi == null ? null : DealerCurrency.toINR(hi);
            onBudgetChanged(); dialog.dismiss();
        });
        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(android.view.ViewGroup.LayoutParams.MATCH_PARENT, android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        }
        maxField.requestFocus();
    }

    @Nullable
    private static Double parseBudget(String s) {
        try {
            String t = s.trim().replace(",", "");
            if (t.isEmpty()) return null;
            double v = Double.parseDouble(t);
            return v >= 0 ? v : null;
        } catch (NumberFormatException e) { return null; }
    }

    @Override
    public void onProductSelected(Product product, boolean isSelected) {
        if (isSelected) {
            selectedProducts.add(product);
        } else {
            selectedProducts.remove(product);
        }
        doneButton.setEnabled(!selectedProducts.isEmpty());
        doneButton.setText(selectedProducts.isEmpty() ? "Done" : "Done (" + selectedProducts.size() + ")");
    }

}
