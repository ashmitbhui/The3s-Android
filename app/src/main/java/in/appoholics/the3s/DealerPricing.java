package in.appoholics.the3s;

import androidx.annotation.Nullable;

import com.parse.ParseUser;

/**
 * Per-dealer price markup for international dealers, set in the portal
 * (Users → Price markup) and stored on the Parse user as Markup + MarkupType
 * ("percent" | "fixed"). Every price the dealer sees, shares or exports goes
 * through {@link #apply(double)}. Mirrors iOS DealerMarkup.
 */
public final class DealerPricing {
    private DealerPricing() {}

    /** List price raised by the signed-in dealer's markup (unchanged if none). */
    public static double apply(double listPrice) {
        ParseUser u = ParseUser.getCurrentUser();
        if (u == null) return listPrice;
        Number m = u.getNumber("Markup");
        if (m == null || m.doubleValue() <= 0) return listPrice;
        boolean fixed = "fixed".equals(u.getString("MarkupType"));
        return fixed ? listPrice + m.doubleValue() : listPrice * (1 + m.doubleValue() / 100.0);
    }

    @Nullable
    public static Double apply(@Nullable Number listPrice) {
        return listPrice == null ? null : apply(listPrice.doubleValue());
    }

    /** Re-reads the dealer record so markup changes made in the portal apply on next launch. */
    public static void refresh() {
        ParseUser u = ParseUser.getCurrentUser();
        if (u != null) u.fetchInBackground();
    }
}
