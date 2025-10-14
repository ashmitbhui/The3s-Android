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
}