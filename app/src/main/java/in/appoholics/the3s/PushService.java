package in.appoholics.the3s;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Intent;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.parse.ParseInstallation;

import org.json.JSONObject;

import java.util.Map;

/**
 * Receives FCM messages sent through Back4App and shows them as notifications.
 * Token changes are mirrored onto the ParseInstallation so the server can
 * target this device.
 */
public class PushService extends FirebaseMessagingService {

    public static final String CHANNEL_ID = "the3s_default";

    @Override
    public void onNewToken(String token) {
        registerToken(token);
    }

    static void registerToken(String token) {
        ParseInstallation installation = ParseInstallation.getCurrentInstallation();
        installation.setDeviceToken(token);   // deviceType "android" is set by the SDK
        installation.saveInBackground(e -> {
            if (e != null) Log.w("Push", "Could not save device token: " + e.getMessage());
        });
    }

    @Override
    public void onMessageReceived(RemoteMessage message) {
        String title = "The 3S";
        String body = null;

        // Parse's push adapter delivers the payload as a JSON string under "data".
        Map<String, String> data = message.getData();
        String raw = data.get("data");
        if (raw != null) {
            try {
                JSONObject json = new JSONObject(raw);
                body = json.optString("alert", null);
                title = json.optString("title", title);
            } catch (Exception ignored) { }
        }
        if (body == null) body = data.get("alert");
        if (data.get("title") != null) title = data.get("title");
        if (body == null && message.getNotification() != null) {
            body = message.getNotification().getBody();
            if (message.getNotification().getTitle() != null) {
                title = message.getNotification().getTitle();
            }
        }
        if (body == null) return;

        Intent open = new Intent(this, SplashScreen.class);
        open.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pending = PendingIntent.getActivity(this, 0, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                .setAutoCancel(true)
                .setContentIntent(pending)
                .build();

        try {
            NotificationManagerCompat.from(this)
                    .notify((int) (System.currentTimeMillis() % Integer.MAX_VALUE), notification);
        } catch (SecurityException ignored) {
            // Notification permission not granted yet.
        }
    }
}
