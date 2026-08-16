package in.appoholics.the3s;

import android.util.Log;

import androidx.annotation.Nullable;

import com.parse.ParseException;
import com.parse.ParseUser;

/**
 * A dealer account deleted (or its session revoked) in the portal leaves the app
 * with a stale session token; every query then fails with error 209 and the
 * catalogue looks empty. Drop the local session and retry once, like iOS does.
 */
public final class SessionGuard {
    private SessionGuard() {}

    /** @return true if the error was a stale session that has now been cleared (and retry was run). */
    public static boolean recover(@Nullable ParseException e, @Nullable Runnable retry) {
        if (e == null || e.getCode() != ParseException.INVALID_SESSION_TOKEN) return false;
        if (ParseUser.getCurrentUser() == null) return false;
        Log.w("SessionGuard", "Stale session token — signing out locally and retrying");
        ParseUser.logOutInBackground(err -> { if (retry != null) retry.run(); });
        return true;
    }
}
