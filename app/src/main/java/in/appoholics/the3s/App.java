package in.appoholics.the3s;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;
import android.util.Log;

import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.FirebaseMessaging;
import com.parse.Parse;
import com.parse.ParseInstallation;

public class App extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        Parse.initialize(new Parse.Configuration.Builder(this)
                .applicationId(getString(R.string.back4app_app_id))
                .clientKey(getString(R.string.back4app_client_key))
                .server(getString(R.string.back4app_server_url))
                .build());

        ParseInstallation.getCurrentInstallation().saveInBackground();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    PushService.CHANNEL_ID, "Updates & offers",
                    NotificationManager.IMPORTANCE_DEFAULT);
            getSystemService(NotificationManager.class).createNotificationChannel(channel);
        }

        // No-op until google-services.json is added to the app module; from
        // then on every install registers its push token with Parse.
        if (!FirebaseApp.getApps(this).isEmpty()) {
            FirebaseMessaging.getInstance().getToken().addOnCompleteListener(task -> {
                if (task.isSuccessful() && task.getResult() != null) {
                    PushService.registerToken(task.getResult());
                } else {
                    Log.w("Push", "FCM token fetch failed", task.getException());
                }
            });
        }
    }
}
