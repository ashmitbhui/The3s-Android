package in.appoholics.the3s;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
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
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textfield.TextInputEditText;
import com.parse.ParseUser;

import java.util.ArrayList;
import java.util.List;

public class Fragment_Favourite extends Fragment implements ProductAdapter.OnProductSelectionChangedListener {

    private Button loginButton;
    private RecyclerView favoritesRecyclerView;
    private View emptyStateView;
    private ProductAdapter productAdapter;
    private final List<Product> favoriteProductList = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_favourite, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize views
        favoritesRecyclerView = view.findViewById(R.id.favorites_recycler_view);
        emptyStateView = view.findViewById(R.id.empty_state_view);
        loginButton = view.findViewById(R.id.login);

        // Setup the RecyclerView and its adapter
        setupRecyclerView();
        setupClickListeners();
        updateLoginButtonState();

        // Observe the database for the list of favorites
        observeFavorites();
    }

    @Override
    public void onResume() {
        super.onResume();
        updateLoginButtonState(); // ✨ Also check when fragment resumes
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
        // We can reuse the same ProductAdapter from the CategoryDetail screen
        productAdapter = new ProductAdapter(requireContext(), favoriteProductList, this);
        favoritesRecyclerView.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        favoritesRecyclerView.setAdapter(productAdapter);
    }

    @Override
    public void onSelectionChanged(int selectedCount) {
        Log.d("FavouriteFragment", "Number of selected items: " + selectedCount);
        // TODO: Add logic here to show a "Share Selected" button if desired.
    }

    private void observeFavorites() {
        // Get the database instance and the DAO
        AppDatabase db = AppDatabase.getDatabase(requireContext());
        FavoriteProductDao dao = db.favoriteProductDao();

        // Observe the LiveData returned by the DAO.
        // This will automatically trigger when the data in the favorites_table changes.
        dao.getAllFavorites().observe(getViewLifecycleOwner(), new Observer<List<FavoriteProduct>>() {
            @Override
            public void onChanged(List<FavoriteProduct> favoriteProducts) {
                // This block runs whenever the list of favorites is updated.

                // 1. Handle the empty state UI
                if (favoriteProducts == null || favoriteProducts.isEmpty()) {
                    favoritesRecyclerView.setVisibility(View.GONE);
                    emptyStateView.setVisibility(View.VISIBLE);
                } else {
                    favoritesRecyclerView.setVisibility(View.VISIBLE);
                    emptyStateView.setVisibility(View.GONE);
                }

                // 2. Convert the database objects (FavoriteProduct) to UI objects (Product)
                List<Product> newProductList = new ArrayList<>();
                for (FavoriteProduct fav : favoriteProducts) {
                    // Note: You need a way to get the double price if your Product class expects it.
                    // For now, we assume the price string is sufficient.
                    Product product = new Product(
                            fav.getProductId(),
                            fav.getName(),
                            fav.getDetail(),
                            Double.parseDouble(fav.getPrice().replaceAll("[^\\d.]", "")), // Convert price string to double
                            fav.getImageUrl(), fav.getCategory(), fav.getBrand()

                    );
                    product.setFavorite(true); // All products on this screen are favorites
                    newProductList.add(product);
                }

                // 3. Update the adapter's list
                favoriteProductList.clear();
                favoriteProductList.addAll(newProductList);
                productAdapter.notifyDataSetChanged();
            }
        });
    }
}