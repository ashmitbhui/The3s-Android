package in.appoholics.the3s;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;
import com.parse.GetCallback;
import com.parse.ParseException;
import com.parse.ParseInstallation;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;

public class HomeFrame extends AppCompatActivity {

    BottomNavigationView bottomNavigationView;
    Fragment_Home home = new Fragment_Home();
    Fragment_Search search = new Fragment_Search();
    Fragment_Favourite favourite = new Fragment_Favourite();
    Fragment_Videos videos = new Fragment_Videos();

    boolean doubleBackToExitPressedOnce = false;

    @SuppressLint("GestureBackNavigation")
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        if (keyCode == KeyEvent.KEYCODE_BACK) {

            if (doubleBackToExitPressedOnce) {
                Intent startMain = new Intent(Intent.ACTION_MAIN);
                startMain.addCategory(Intent.CATEGORY_HOME);
                startActivity(startMain);
            }

            this.doubleBackToExitPressedOnce = true;
            Toast.makeText(this, "Please click BACK again to exit", Toast.LENGTH_SHORT).show();

            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    doubleBackToExitPressedOnce = false;
                }
            }, 2000);
            return false;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_frame);

//        ParseInstallation installation = ParseInstallation.getCurrentInstallation();
//        installation.put("User", ParseUser.getCurrentUser());
//        installation.saveInBackground();

//        ParseQuery<ParseObject> updateQuery = new ParseQuery<ParseObject>("UpdateCheck");
//        updateQuery.getFirstInBackground(new GetCallback<ParseObject>() {
//            @Override
//            public void done(ParseObject object, ParseException e) {
//                String latestVersion = object.getString("Android_Version");
//                if (!latestVersion.equals(installation.getString("appVersion"))) {
//
//                    AlertDialog.Builder builder = new AlertDialog.Builder(HomeFrame.this);
//                    builder.setTitle("New Update Available");
//                    builder.setCancelable(true);
//                    builder.setMessage(object.getString("Message_Android"));
//                    builder.setPositiveButton("Update Now", new DialogInterface.OnClickListener() {
//                        @Override
//                        public void onClick(DialogInterface dialog, int which) {
//                            dialog.cancel();
//                            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(object.getString("Android_Link")));
//                            startActivity(browserIntent);
//                        }
//                    });
//                    builder.setNeutralButton("Later", new DialogInterface.OnClickListener() {
//                        @Override
//                        public void onClick(DialogInterface dialogInterface, int i) {
//                            dialogInterface.cancel();
//                        }
//                    });
//                    AlertDialog alert = builder.create();
//                    alert.show();
//                }
//            }
//        });

        bottomNavigationView = (BottomNavigationView) findViewById(R.id.bottomNav);
        getSupportFragmentManager().beginTransaction().replace(R.id.container, home).commit();
        bottomNavigationView.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(MenuItem item) {

                if (item.getItemId() == R.id.nav_home) {
                    getSupportFragmentManager().beginTransaction().replace(R.id.container, home).commit();
                } else if (item.getItemId() == R.id.nav_search) {
                    getSupportFragmentManager().beginTransaction().replace(R.id.container, search).commit();
                } else if (item.getItemId() == R.id.nav_fav) {
                    getSupportFragmentManager().beginTransaction().replace(R.id.container, favourite).commit();
                } else {
                    getSupportFragmentManager().beginTransaction().replace(R.id.container, videos).commit();
                }

                return true;
            }
        });

    }
}