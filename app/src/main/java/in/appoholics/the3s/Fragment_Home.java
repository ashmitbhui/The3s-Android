package in.appoholics.the3s;

import static androidx.core.content.ContextCompat.getSystemService;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.textfield.TextInputEditText;
import com.parse.FindCallback;
import com.parse.GetDataCallback;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import java.util.ArrayList;
import java.util.List;

public class Fragment_Home extends Fragment {

    private Button loginButton;
    private Button currencyButton;
    private TextView headerLabel;
    private View threeSButton, trophyverseButton, verboseButton;
    private TextView threeSLabel, trophyverseLabel, verboseLabel;
    private LinearLayout brandPanel;
    private EditText searchInput;
    private AppCompatButton createCatalogueButton;
    private GridView gridView;
    private ImageButton whatsappFab;

    private String currentBrand = "3S";


    private categoryAdapter gridAdapter;
    ArrayList<ParseObject> all_category_data = new ArrayList<>();
    ArrayList<ParseObject> displayed_data = new ArrayList<>();


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);

    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initializeViews(view);
        setupClickListeners();
        setupGridView();
        loadCategoriesFromParse();
        updateLoginButtonState();
    }

    @Override
    public void onResume() {
        super.onResume();
        updateLoginButtonState(); // ✨ Also check when fragment resumes
    }


    /**
     * Finds and assigns all the views from the fragment's layout.
     * Note: It now takes a 'View' parameter.
     */
    private void initializeViews(View view) {
        loginButton = view.findViewById(R.id.login);
        currencyButton = view.findViewById(R.id.currency);
        currencyButton.setOnClickListener(v -> showCurrencyPicker());
        headerLabel = view.findViewById(R.id.headerLabel);
        threeSButton = view.findViewById(R.id.three_s_button);
        trophyverseButton = view.findViewById(R.id.trophyverse_button);
        verboseButton = view.findViewById(R.id.verbose_button);
        threeSLabel = view.findViewById(R.id.three_s_label);
        trophyverseLabel = view.findViewById(R.id.trophyverse_label);
        verboseLabel = view.findViewById(R.id.verbose_label);
        brandPanel = view.findViewById(R.id.brand_panel);
        searchInput = view.findViewById(R.id.search_input);
        createCatalogueButton = view.findViewById(R.id.create_catalogue_button);
        gridView = view.findViewById(R.id.gridView);
        whatsappFab = view.findViewById(R.id.whatsapp_fab);
    }

    /**
     * Sets up OnClickListeners for buttons and other interactive views.
     */
    private void setupClickListeners() {
        createCatalogueButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(requireActivity(), CatalogueBuilder.class);
                startActivity(intent);
            }
        });
        threeSButton.setOnClickListener(v -> selectBrand("3S"));
        trophyverseButton.setOnClickListener(v -> selectBrand("Trophyverse"));
        verboseButton.setOnClickListener(v -> selectBrand("Verbose"));

        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                applyFilters();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
        whatsappFab.setOnClickListener(v -> showWhatsAppBottomSheet());
        loginButton.setOnClickListener(v -> {

            if (ParseUser.getCurrentUser() != null) {
                // If yes, perform logout
                DealerCurrency.clearSelection();
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

    /** Pick the currency every price is shown in (grid, detail, builder, PDF, share). */
    private void showCurrencyPicker() {
        if (getContext() == null) return;
        java.util.List<DealerCurrency.Entry> opts = DealerCurrency.options();
        String[] labels = new String[opts.size()];
        int checked = 0;
        for (int i = 0; i < opts.size(); i++) {
            labels[i] = opts.get(i).label();
            if (opts.get(i).code.equals(DealerCurrency.current().code)) checked = i;
        }
        new androidx.appcompat.app.AlertDialog.Builder(getContext())
                .setTitle("Show prices in")
                .setSingleChoiceItems(labels, checked, (d, which) -> {
                    DealerCurrency.select(opts.get(which).code);
                    currencyButton.setText(opts.get(which).code);
                    d.dismiss();
                })
                .show();
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
        boolean loggedIn = ParseUser.getCurrentUser() != null;
        loginButton.setText(loggedIn ? "Logout" : "Login");
        // Currency switcher: only dealers the portal gave more than one currency
        boolean multi = loggedIn && DealerCurrency.canSwitch();
        currencyButton.setVisibility(multi ? View.VISIBLE : View.GONE);
        if (multi) currencyButton.setText(DealerCurrency.current().code);
        // WhatsApp (channels + enquiry chats): dealer-only + portal switch
        boolean showWa = loggedIn && getContext() != null && UserStatus.showWhatsApp(getContext());
        if (whatsappFab != null) whatsappFab.setVisibility(showWa ? View.VISIBLE : View.GONE);
    }

    /**
     * NEW: Fetches category data from the Parse "Category" class.
     */
    private void loadCategoriesFromParse() {
        ParseQuery<ParseObject> query = new ParseQuery<>("Category");
        query.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> objects, ParseException e) {

                if (!isAdded()) {
                    return; // Stop execution if the fragment is not attached
                }

                if (e == null) {
                    all_category_data.clear();
                    // Success: We got the objects
                    all_category_data.addAll(objects);
                    // Initially, show the "3S" categories by default
                    selectBrand("3S");
                } else if (SessionGuard.recover(e, () -> { if (isAdded()) { updateLoginButtonState(); loadCategoriesFromParse(); } })) {
                    return;   // stale dealer session cleared; reloading
                } else {
                    // Failure: Handle the error
                    Log.e("ParseError", "Error fetching categories: " + e.getMessage());
                    Toast.makeText(requireContext(), "Failed to load categories", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    /**
     * Sets up the GridView with an adapter and handles item clicks.
     */
    private void setupGridView() {
        // Use requireContext() to get the context in a Fragment

        gridAdapter = new categoryAdapter(requireContext(), displayed_data);
        gridView.setAdapter(gridAdapter);

        gridView.setOnItemClickListener((parent, view, position, id) -> {

            ParseObject selectedCategory = (ParseObject) parent.getItemAtPosition(position);

            Intent intent = new Intent(requireActivity(), CategoryDetail.class);
            intent.putExtra("CATEGORY_NAME", selectedCategory.getString("Name"));
            intent.putExtra("BRAND_NAME", currentBrand);

            startActivity(intent);

        });
    }

    /**
     * Highlights the tapped brand tab, fuses it into the brand-coloured panel
     * below (Swiggy-style), and refreshes search hint + category grid.
     */
    private void selectBrand(String brand) {
        View[] tabs = {threeSButton, trophyverseButton, verboseButton};
        TextView[] labels = {threeSLabel, trophyverseLabel, verboseLabel};
        String[] brands = {"3S", "Trophyverse", "Verbose"};
        // Brand colours: 3S deep green, Trophyverse burgundy, Verbose royal blue.
        // Unselected tabs keep a soft tint of their own colour so switching feels intuitive.
        int[] selectedBg = {R.drawable.tab_top_3s, R.drawable.tab_top_trophyverse, R.drawable.tab_top_verbose};
        int[] unselectedBg = {R.drawable.tab_unsel_3s, R.drawable.tab_unsel_trophy, R.drawable.tab_unsel_verbose};
        int[] panelBg = {R.drawable.bg_panel_3s, R.drawable.bg_panel_trophy, R.drawable.bg_panel_verbose};
        int[] brandColors = {0xFF16604A, 0xFF5E0021, 0xFF0032D2};

        currentBrand = brand;

        for (int i = 0; i < tabs.length; i++) {
            if (brands[i].equals(brand)) {
                tabs[i].setBackgroundResource(selectedBg[i]);
                labels[i].setTextColor(ContextCompat.getColor(requireContext(), R.color.white));
                brandPanel.setBackgroundResource(panelBg[i]);
                createCatalogueButton.getBackground().mutate().setTint(brandColors[i]);
            } else {
                tabs[i].setBackgroundResource(unselectedBg[i]);
                labels[i].setTextColor(brandColors[i]);
            }
        }

        headerLabel.setText(brand + " Categories");
        searchInput.setHint("Search in " + brand);
        if (searchInput.getText().length() > 0) {
            searchInput.setText(""); // triggers applyFilters() via the watcher
        } else {
            applyFilters();
        }
    }

    /**
     * Shows categories of the current brand whose name matches the search text.
     */
    private void applyFilters() {
        String query = searchInput.getText().toString().trim().toLowerCase();

        displayed_data.clear();
        for (ParseObject category : all_category_data) {
            if (!currentBrand.equals(category.getString("Brand"))) {
                continue;
            }
            String name = category.getString("Name");
            if (query.isEmpty() || (name != null && name.toLowerCase().contains(query))) {
                displayed_data.add(category);
            }
        }
        gridAdapter.notifyDataSetChanged();
    }


    private void showWhatsAppBottomSheet() {
        // Use BottomSheetDialog for the material design sliding panel
        final BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(requireContext());
        // Inflate the custom layout
        View sheetView = getLayoutInflater().inflate(R.layout.bottom_sheet_whatsapp, null);
        bottomSheetDialog.setContentView(sheetView);

        // Broadcast channels
        View channelTrophy = sheetView.findViewById(R.id.option_channel_trophyverse);
        View channelBranding = sheetView.findViewById(R.id.option_channel_branding);
        channelTrophy.setOnClickListener(v -> {
            openLink("https://whatsapp.com/channel/0029Vb9IWhYEKyZORh3Tp647");
            bottomSheetDialog.dismiss();
        });
        channelBranding.setOnClickListener(v -> {
            openLink("https://whatsapp.com/channel/0029VbDLYkX23n3mI81HrB21");
            bottomSheetDialog.dismiss();
        });

        // Find the options inside the bottom sheet
        // Broadcast channels are for everyone; the enquiry chats are per-dealer (_User.ShowChats)
        boolean chats = UserStatus.showChats();
        sheetView.findViewById(R.id.chats_divider).setVisibility(chats ? View.VISIBLE : View.GONE);
        sheetView.findViewById(R.id.chats_caption).setVisibility(chats ? View.VISIBLE : View.GONE);
        sheetView.findViewById(R.id.option_3s_enquiry).setVisibility(chats ? View.VISIBLE : View.GONE);
        sheetView.findViewById(R.id.option_verbose_enquiry).setVisibility(chats ? View.VISIBLE : View.GONE);
        sheetView.findViewById(R.id.option_auto_response).setVisibility(chats ? View.VISIBLE : View.GONE);
        TextView option3S = sheetView.findViewById(R.id.option_3s_enquiry);
        TextView optionVerbose = sheetView.findViewById(R.id.option_verbose_enquiry);
        TextView optionAuto = sheetView.findViewById(R.id.option_auto_response);

        // Set click listeners for each option
        option3S.setOnClickListener(v -> {
            openWhatsApp("919312249445", "Hello! I'm interested in your catalogue.");
            bottomSheetDialog.dismiss();
        });

        optionVerbose.setOnClickListener(v -> {
            openWhatsApp("917428942791", "Hello! I'm interested in your catalogue.");
            bottomSheetDialog.dismiss();
        });

        optionAuto.setOnClickListener(v -> {
            openWhatsApp("918699997467", "Menu");
            bottomSheetDialog.dismiss();
        });

        bottomSheetDialog.show();
    }

    private void openLink(String url) {
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
    }

    private void openWhatsApp(String whatsAppRedirect, String message) {

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse("https://wa.me/" + whatsAppRedirect + "?text=" + Uri.encode(message)));
        startActivity(intent);
    }

    /**
     * UPDATED: A highly efficient adapter using the ViewHolder pattern and Glide for image loading.
     */
    class categoryAdapter extends BaseAdapter {
        private final Context context;
        private final ArrayList<ParseObject> categories_data;
        private final LayoutInflater inflater;

        public categoryAdapter(Context context, ArrayList<ParseObject> categories_data) {
            this.context = context;
            this.categories_data = categories_data;
            this.inflater = LayoutInflater.from(context);
        }

        @Override
        public int getCount() {
            return categories_data.size();
        }

        @Override
        public Object getItem(int position) {
            return categories_data.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (convertView == null) {
                convertView = inflater.inflate(R.layout.item_category_card, parent, false);
                holder = new ViewHolder();
                holder.icon = convertView.findViewById(R.id.category_icon);
                holder.title = convertView.findViewById(R.id.category_title);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            ParseObject currentCategory = categories_data.get(position);
            holder.title.setText(currentCategory.getString("Name"));

            // Use Glide to load the image from the ParseFile URL
            ParseFile imageFile = currentCategory.getParseFile("Icon");
            Log.d("ASHMIT", currentCategory.getString("Name"));

            if (imageFile != null) {
                Log.d("ASHMIT", imageFile.getUrl());
                Glide.with(context)
                        .load(imageFile.getUrl())
                        .into(holder.icon);
            } else {
                Log.d("ASHMIT", "Position - " + position);
            }
            return convertView;
        }

        // ViewHolder class to hold view references for recycling
        private class ViewHolder {
            ImageView icon;
            TextView title;
        }
    }
}
