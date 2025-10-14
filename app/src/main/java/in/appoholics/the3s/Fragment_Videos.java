package in.appoholics.the3s;

import android.app.Dialog;
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
import androidx.recyclerview.widget.LinearLayoutManager;
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

public class Fragment_Videos extends Fragment {

    private Button loginButton;
    private RecyclerView videosRecyclerView;
    private VideoAdapter videoAdapter;
    private TextInputEditText searchEditText;
    private View emptyStateView;

    private final List<Video> masterVideoList = new ArrayList<>();
    private final List<Video> displayedVideoList = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_video, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initializeViews(view);
        setupRecyclerView();
        setupSearchListener();
        setupClickListeners();
        updateLoginButtonState();
        loadAllVideosFromParse();
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

    private void initializeViews(View view) {
        videosRecyclerView = view.findViewById(R.id.videos_recycler_view);
        searchEditText = view.findViewById(R.id.video_search_edit_text);
        emptyStateView = view.findViewById(R.id.videos_empty_state);
        loginButton = view.findViewById(R.id.login);
    }

    private void setupRecyclerView() {
        videoAdapter = new VideoAdapter(requireContext(), displayedVideoList);
        videosRecyclerView.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        videosRecyclerView.setAdapter(videoAdapter);
    }

    private void loadAllVideosFromParse() {
        ParseQuery<ParseObject> query = new ParseQuery<>("Videos");
        query.orderByAscending("Order");
        query.findInBackground((videoObjects, e) -> {
            if (e == null) {
                masterVideoList.clear();
                for (ParseObject videoObject : videoObjects) {
                    String name = videoObject.getString("Name");
                    String category = videoObject.getString("Category");
                    String thumbUrl = "";
                    String videoUrl = "";

                    ParseFile thumbFile = videoObject.getParseFile("Thumb");
                    if (thumbFile != null) thumbUrl = thumbFile.getUrl();

                    ParseFile videoFile = videoObject.getParseFile("Video");
                    if (videoFile != null) videoUrl = videoFile.getUrl();

                    masterVideoList.add(new Video(name, category, thumbUrl, videoUrl));
                }
                filterList("");
            } else {
                Log.e("ParseError", "Failed to fetch videos: " + e.getMessage());
                Toast.makeText(requireContext(), "Could not load videos.", Toast.LENGTH_SHORT).show();
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
                filterList(s.toString());
            }
        });
    }

    private void filterList(String query) {
        displayedVideoList.clear();
        String lowerCaseQuery = query.toLowerCase().trim();

        if (lowerCaseQuery.isEmpty()) {
            displayedVideoList.addAll(masterVideoList);
        } else {
            for (Video video : masterVideoList) {
                if ((video.getName() != null && video.getName().toLowerCase().contains(lowerCaseQuery)) ||
                        (video.getCategory() != null && video.getCategory().toLowerCase().contains(lowerCaseQuery))) {
                    displayedVideoList.add(video);
                }
            }
        }

        // Handle empty state
        emptyStateView.setVisibility(displayedVideoList.isEmpty() ? View.VISIBLE : View.GONE);
        videoAdapter.notifyDataSetChanged();
    }
}