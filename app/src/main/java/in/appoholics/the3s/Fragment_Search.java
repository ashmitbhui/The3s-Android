package in.appoholics.the3s;

import static android.content.Context.MODE_PRIVATE;

import android.app.Dialog;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
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
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textfield.TextInputEditText;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import java.util.ArrayList;
import java.util.List;

public class Fragment_Search extends Fragment implements ProductAdapter.OnProductSelectionChangedListener {

    private Button loginButton;
    private RecyclerView searchRecyclerView;
    private ProductAdapter productAdapter;
    private TextInputEditText searchEditText;
    private View emptyStateView;

    // Two lists are used: one for the complete data, one for the filtered results
    private final List<Product> masterProductList = new ArrayList<>();
    private final List<Product> displayedProductList = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_search, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initializeViews(view);
        setupRecyclerView();
        setupSearchListener();
        setupClickListeners();
        updateLoginButtonState();
        loadAllProductsFromParse();


    }

    @Override
    public void onResume() {
        super.onResume();
        updateLoginButtonState(); // ✨ Also check when fragment resumes
    }

    private void initializeViews(View view) {
        loginButton = view.findViewById(R.id.login);
        searchRecyclerView = view.findViewById(R.id.search_recycler_view);
        searchEditText = view.findViewById(R.id.search_edit_text);
        emptyStateView = view.findViewById(R.id.search_empty_state);
    }

    private void setupClickListeners() {
        loginButton.setOnClickListener(v -> {
            // Check if a user is currently logged in
            if (ParseUser.getCurrentUser() != null) {
                // If yes, perform logout
                ParseUser.logOutInBackground(e -> {
                    if (e == null) {
                        Toast.makeText(getContext(), "Logged out successfully.", Toast.LENGTH_SHORT).show();
                        updateLoginButtonState();
                    } else {
                        Toast.makeText(getContext(), "Logout failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                // If no, show the login dialog
                showLoginDialog();
            }
        });
    }

    private void showLoginDialog() {
        final Dialog dialog = new Dialog(requireContext());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_login);

        TextInputEditText usernameEditText = dialog.findViewById(R.id.edit_text_username);
        TextInputEditText passwordEditText = dialog.findViewById(R.id.edit_text_password);
        Button submitButton = dialog.findViewById(R.id.button_submit_login);

        submitButton.setOnClickListener(v -> {
            String username = usernameEditText.getText().toString().trim();
            String password = passwordEditText.getText().toString().trim();

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(getContext(), "Username and password cannot be empty.", Toast.LENGTH_SHORT).show();
                return;
            }

            ParseUser.logInInBackground(username, password, (user, e) -> {
                if (user != null) {
                    // Login successful
                    Toast.makeText(getContext(), "Login Successful!", Toast.LENGTH_SHORT).show();
                    updateLoginButtonState(); // Update the main button to "Logout"
                    dialog.dismiss();
                } else {
                    // Login failed
                    Toast.makeText(getContext(), "Login failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        });

        dialog.show();
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
    }

    private void updateLoginButtonState() {
        if (ParseUser.getCurrentUser() != null) {
            loginButton.setText("Logout");
        } else {
            loginButton.setText("Login");
        }
    }

    private void setupRecyclerView() {
        productAdapter = new ProductAdapter(requireContext(), displayedProductList, this);
        searchRecyclerView.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        searchRecyclerView.setAdapter(productAdapter);
    }

    @Override
    public void onSelectionChanged(int selectedCount) {
        Log.d("SearchFragment", "Number of selected items: " + selectedCount);
        // TODO: Add logic here to show a "Share Selected" button if desired.
    }

    private void loadAllProductsFromParse() {

        SharedPreferences prefs = getContext().getSharedPreferences("AppSettings", MODE_PRIVATE);
        String currentSeries = prefs.getString("CURRENT_SERIES", null);

        ParseQuery<ParseObject> query = new ParseQuery<>("Products");
        query.whereEqualTo("isEnabled", true);
        query.whereEqualTo("Series", currentSeries);
        query.setLimit(1000); // Set a reasonable limit for your master list

        query.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> objects, ParseException e) {
                if (e == null) {
                    masterProductList.clear();
                    for (ParseObject productObject : objects) {
                        String objectId = productObject.getObjectId();
                        String name = productObject.getString("Name");
                        String detail = productObject.getString("Detail");
                        double price = productObject.getNumber("Price").doubleValue();
                        String category = productObject.getString("Category");
                        String brand = productObject.getString("Brand");
                        String imageUrl = "";
                        ParseFile imageFile = productObject.getParseFile("Image");
                        if (imageFile != null) {
                            imageUrl = imageFile.getUrl();
                        }
                        masterProductList.add(new Product(objectId, name, detail, price, imageUrl, category, brand));
                    }
                    // Initially, display the entire list
                    filterList("");
                } else {
                    Log.e("ParseError", "Error loading master list: " + e.getMessage());
                    Toast.makeText(requireContext(), "Failed to load products.", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void setupSearchListener() {
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                // Trigger filtering every time the user types
                filterList(s.toString());
            }
        });
    }

    private void filterList(String query) {
        displayedProductList.clear();
        String lowerCaseQuery = query.toLowerCase().trim();

        if (lowerCaseQuery.isEmpty()) {
            // If the search bar is empty, show the full master list
            displayedProductList.addAll(masterProductList);
        } else {
            // Otherwise, filter the master list
            for (Product product : masterProductList) {
                // Check if the query matches the name, category, or brand (case-insensitive)
                if ((product.getName() != null && product.getName().toLowerCase().contains(lowerCaseQuery)) ||
                        (product.getCategory() != null && product.getCategory().toLowerCase().contains(lowerCaseQuery)) ||
                        (product.getBrand() != null && product.getBrand().toLowerCase().contains(lowerCaseQuery))) {
                    displayedProductList.add(product);
                }
            }
        }

        // Handle the visibility of the "No Results" message
        if (displayedProductList.isEmpty()) {
            searchRecyclerView.setVisibility(View.GONE);
            emptyStateView.setVisibility(View.VISIBLE);
        } else {
            searchRecyclerView.setVisibility(View.VISIBLE);
            emptyStateView.setVisibility(View.GONE);
        }

        // Notify the adapter that the data has changed
        productAdapter.notifyDataSetChanged();
    }
}