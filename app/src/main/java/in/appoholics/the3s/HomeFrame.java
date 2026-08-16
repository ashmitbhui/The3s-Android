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

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;

public class HomeFrame extends AppCompatActivity {

    BottomNavigationView bottomNavigationView;
    Fragment_Home home = new Fragment_Home();
    Fragment_Search search = new Fragment_Search();
    Fragment_Favourite favourite = new Fragment_Favourite();
    Fragment_Marketing marketing = new Fragment_Marketing();

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
        EdgeToEdgeHelper.apply(this, R.id.main, R.id.container, R.id.bottomNav);
        DealerPricing.refresh();   // pick up markup changes made in the portal

        // Android 13+ needs runtime consent before notifications can show
        if (android.os.Build.VERSION.SDK_INT >= 33 &&
                checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                        != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 71);
        }

        // White status bar with dark icons to blend with the light headers
        getWindow().setStatusBarColor(android.graphics.Color.WHITE);
        androidx.core.view.WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView())
                .setAppearanceLightStatusBars(true);

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
                    getSupportFragmentManager().beginTransaction().replace(R.id.container, marketing).commit();
                }

                return true;
            }
        });

    }
}
