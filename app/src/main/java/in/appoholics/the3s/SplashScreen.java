package in.appoholics.the3s;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.parse.GetCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;

public class SplashScreen extends AppCompatActivity {

    private static final String PREFS_NAME = "AppSettings";
    private static final String SERIES_KEY = "CURRENT_SERIES";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen); // Your splash screen layout

        fetchBasicSettings();
    }

    private void fetchBasicSettings() {
        // We assume there is only one settings object, so we get the first one.
        // In your Parse Dashboard, you might have to get the specific objectId.
        ParseQuery<ParseObject> query = new ParseQuery<>("Basic");
        query.getFirstInBackground(new GetCallback<ParseObject>() {
            @Override
            public void done(ParseObject object, ParseException e) {
                if (e == null && object != null) {
                    // Success: We found the settings object
                    String currentSeries = object.getString("CurrentSeries");
                    if (currentSeries != null) {
                        // Save the fetched value to SharedPreferences
                        saveSeriesToPrefs(currentSeries);
                        Log.d("SplashScreen", "Current Series saved: " + currentSeries);
                    }
                } else {
                    // Failure: Handle the error
                    Log.e("SplashScreen", "Failed to fetch Basic settings", e);
                    Toast.makeText(SplashScreen.this, "Could not load app settings.", Toast.LENGTH_SHORT).show();
                }
                // Proceed to the main app after a short delay, regardless of success or failure
                navigateToMainApp();
            }
        });
    }

    private void saveSeriesToPrefs(String series) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(SERIES_KEY, series);
        editor.apply(); // Use apply() for asynchronous saving
    }

    private void navigateToMainApp() {
        // Use a Handler to navigate after a delay, allowing the splash screen to be visible
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            // Replace MainActivity.class with your actual main activity (e.g., the one holding Fragment_Home)
            Intent intent = new Intent(SplashScreen.this, HomeFrame.class);
            startActivity(intent);
            finish(); // Close the splash screen so the user can't navigate back to it
        }, 1500); // 1.5-second delay
    }
}