package in.appoholics.the3s;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.parse.Parse;
import com.parse.ParseUser;

import org.json.JSONObject;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Currency conversion for display. Prices are stored and computed in INR
 * (list price → dealer markup, see {@link DealerPricing}); conversion to the
 * dealer's selected currency happens only when a price is shown, shared or exported.
 *
 * Portal → Dashboard → Currencies sets the rate table (Basic.Currencies, cached at
 * launch); Users → currencies sets which ones a dealer may use (_User.Currencies /
 * DefaultCurrency). Dealers with one currency never see a switcher. Mirrors iOS Pricing.
 */
public final class DealerCurrency {
    private DealerCurrency() {}

    public static final String INR = "INR";
    private static final String PREFS = "AppSettings";
    private static final String TABLE_KEY = "CURRENCIES", SELECTED_KEY = "SELECTED_CURRENCY";

    public static final class Entry {
        public final String code, symbol;
        public final double rate;   // units per ₹1
        Entry(String code, String symbol, double rate) { this.code = code; this.symbol = symbol; this.rate = rate; }
        static final Entry INR_ENTRY = new Entry(INR, "₹", 1);
        /** "USD  $" for the switcher; just the code when the symbol repeats it. */
        public String label() { return symbol.equals(code) ? code : code + "  " + symbol; }
    }

    private static SharedPreferences prefs() {
        Context ctx = Parse.getApplicationContext();
        return ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    /** Called at launch with Basic.Currencies ({ code: {rate, symbol} }). */
    public static void storeTable(@Nullable JSONObject table) {
        prefs().edit().putString(TABLE_KEY, table == null ? "" : table.toString()).apply();
    }

    /** The global rate table, INR always present. */
    @NonNull
    public static Map<String, Entry> table() {
        Map<String, Entry> out = new LinkedHashMap<>();
        out.put(INR, Entry.INR_ENTRY);
        try {
            JSONObject t = new JSONObject(prefs().getString(TABLE_KEY, ""));
            for (Iterator<String> it = t.keys(); it.hasNext(); ) {
                String code = it.next().toUpperCase(Locale.ROOT);
                if (INR.equals(code)) continue;
                JSONObject row = t.optJSONObject(code);
                if (row == null) continue;
                double rate = row.optDouble("rate", 0);
                if (rate <= 0) continue;
                String symbol = row.optString("symbol", code).trim();
                out.put(code, new Entry(code, symbol.isEmpty() ? code : symbol, rate));
            }
        } catch (Exception ignored) { }
        return out;
    }

    /**
     * Currencies the signed-in dealer may price in, in portal order. INR is not implied —
     * a dealer can be USD-only. Just INR when logged out or nothing is assigned.
     */
    @NonNull
    public static List<Entry> options() {
        List<Entry> out = new ArrayList<>();
        ParseUser u = ParseUser.getCurrentUser();
        List<String> codes = u == null ? null : u.<String>getList("Currencies");
        if (codes != null) {
            Map<String, Entry> t = table();
            for (String c : codes) {
                if (c == null) continue;
                Entry e = t.get(c.toUpperCase(Locale.ROOT));
                if (e != null && !out.contains(e)) out.add(e);
            }
        }
        if (out.isEmpty()) out.add(Entry.INR_ENTRY);
        return out;
    }

    public static boolean canSwitch() { return options().size() > 1; }

    /** The currency prices are shown in: the dealer's pick, else their portal default, else the first allowed one. */
    @NonNull
    public static Entry current() {
        List<Entry> opts = options();
        String sel = prefs().getString(SELECTED_KEY, null);
        if (sel != null) for (Entry e : opts) if (e.code.equals(sel)) return e;
        ParseUser u = ParseUser.getCurrentUser();
        String def = u == null ? null : u.getString("DefaultCurrency");
        if (def != null) for (Entry e : opts) if (e.code.equalsIgnoreCase(def)) return e;
        return opts.get(0);
    }

    public static void select(@NonNull String code) { prefs().edit().putString(SELECTED_KEY, code).apply(); }
    public static void clearSelection() { prefs().edit().remove(SELECTED_KEY).apply(); }

    public static String symbol() { return current().symbol; }
    public static boolean isINR() { return INR.equals(current().code); }

    public static double convert(double inr) { return inr * current().rate; }
    /** An amount typed in the selected currency (margin, budget) back to INR. */
    public static double toINR(double amount) { return amount / current().rate; }

    /** "₹1,250" · "$15.00" · "AED 55.20" — INR keeps whole rupees, others carry 2 decimals. */
    public static String format(double inr) { return format(inr, -1); }

    public static String format(double inr, int fractionDigits) {
        Entry c = current();
        double v = inr * c.rate;
        boolean isInr = INR.equals(c.code);
        int digits = fractionDigits >= 0 ? fractionDigits : (isInr ? (v == Math.rint(v) ? 0 : 2) : 2);
        DecimalFormat f = new DecimalFormat(digits > 0 ? "#,##0." + "00".substring(0, digits) : "#,##0",
                DecimalFormatSymbols.getInstance(isInr ? new Locale("en", "IN") : Locale.US));
        if (isInr) f.setMinimumFractionDigits(0);
        String num = f.format(v);
        // Letter symbols (AED, CHF) read better with a space: "AED 55" vs "$15".
        boolean letter = !c.symbol.isEmpty() && Character.isLetter(c.symbol.charAt(c.symbol.length() - 1));
        return c.symbol + (letter ? " " : "") + num;
    }

    /** ASCII-safe label for the PDF: "Rs. 1,250" / "USD 15.38". */
    public static String pdfLabel(double inr) {
        Entry c = current();
        boolean isInr = INR.equals(c.code);
        DecimalFormat f = new DecimalFormat(isInr ? "#,##0" : "#,##0.00",
                DecimalFormatSymbols.getInstance(isInr ? new Locale("en", "IN") : Locale.US));
        return (isInr ? "Rs. " : c.code + " ") + f.format(inr * c.rate);
    }
}
