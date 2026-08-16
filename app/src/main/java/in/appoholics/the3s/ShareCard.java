package in.appoholics.the3s;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;

import androidx.annotation.Nullable;

/**
 * Renders a product as a single shareable image: photo on top, name / price /
 * details underneath. Used for multi-select share so every picture carries its
 * own details (WhatsApp keeps only one caption per batch otherwise).
 */
public final class ShareCard {
    private ShareCard() {}

    private static final int WIDTH = 1080, PAD = 56, IMAGE_BOX = 1080 - 2 * 56;

    public static Bitmap render(@Nullable Bitmap photo, String title, @Nullable String price, String detail) {
        int textWidth = WIDTH - 2 * PAD;

        TextPaint titlePaint = paint(54, Typeface.create(Typeface.DEFAULT, Typeface.BOLD), 0xFF171A23);
        TextPaint pricePaint = paint(46, Typeface.create(Typeface.DEFAULT, Typeface.BOLD), 0xFF16604A);
        TextPaint detailPaint = paint(34, Typeface.DEFAULT, 0xFF6B7A71);

        StaticLayout titleL = layout(title == null ? "" : title, titlePaint, textWidth, 0);
        StaticLayout priceL = price == null || price.isEmpty() ? null : layout(price, pricePaint, textWidth, 0);
        StaticLayout detailL = detail == null || detail.trim().isEmpty() ? null : layout(detail.trim(), detailPaint, textWidth, 6);

        int total = PAD + IMAGE_BOX + 40 + titleL.getHeight()
                + (priceL != null ? 14 + priceL.getHeight() : 0)
                + (detailL != null ? 22 + detailL.getHeight() : 0) + PAD;

        Bitmap out = Bitmap.createBitmap(WIDTH, total, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(out);
        c.drawColor(Color.WHITE);

        RectF box = new RectF(PAD, PAD, PAD + IMAGE_BOX, PAD + IMAGE_BOX);
        if (photo != null && photo.getWidth() > 0 && photo.getHeight() > 0) {
            float s = Math.min(box.width() / photo.getWidth(), box.height() / photo.getHeight());
            float w = photo.getWidth() * s, h = photo.getHeight() * s;
            RectF dst = new RectF(box.centerX() - w / 2, box.centerY() - h / 2, box.centerX() + w / 2, box.centerY() + h / 2);
            Paint p = new Paint(Paint.FILTER_BITMAP_FLAG | Paint.ANTI_ALIAS_FLAG);
            c.drawBitmap(photo, null, dst, p);
        } else {
            Paint p = new Paint(Paint.ANTI_ALIAS_FLAG); p.setColor(0xFFE9F3EE);
            c.drawRoundRect(box, 24, 24, p);
        }

        float y = box.bottom + 40;
        c.save(); c.translate(PAD, y); titleL.draw(c); c.restore();
        y += titleL.getHeight();
        if (priceL != null) {
            y += 14;
            c.save(); c.translate(PAD, y); priceL.draw(c); c.restore();
            y += priceL.getHeight();
        }
        if (detailL != null) {
            y += 22;
            c.save(); c.translate(PAD, y); detailL.draw(c); c.restore();
        }
        return out;
    }

    private static TextPaint paint(float size, Typeface tf, int color) {
        TextPaint p = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        p.setTextSize(size); p.setTypeface(tf); p.setColor(color);
        return p;
    }

    private static StaticLayout layout(String text, TextPaint paint, int width, float extraSpacing) {
        return StaticLayout.Builder.obtain(text, 0, text.length(), paint, width)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(extraSpacing, 1f)
                .setIncludePad(false)
                .build();
    }
}
