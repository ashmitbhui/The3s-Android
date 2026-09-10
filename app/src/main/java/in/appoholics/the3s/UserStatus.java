package in.appoholics.the3s;

import com.parse.ParseUser;

public class UserStatus {
    /**
     * Checks if a user is currently logged in.
     * @return true if a user session exists, false otherwise.
     */
    public static boolean isLoggedIn() {
        return ParseUser.getCurrentUser() != null;
    }

    /** Portal switch (Basic.ShowWhatsApp), cached at launch. Defaults to visible. */
    public static boolean showWhatsApp(android.content.Context ctx) {
        return ctx.getSharedPreferences("AppSettings", android.content.Context.MODE_PRIVATE)
                .getBoolean("SHOW_WHATSAPP", true);
    }
}