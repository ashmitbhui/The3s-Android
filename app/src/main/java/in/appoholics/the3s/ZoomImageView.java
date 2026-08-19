package in.appoholics.the3s;

import android.content.Context;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;

import androidx.appcompat.widget.AppCompatImageView;

/** ImageView with pinch-to-zoom, double-tap zoom and panning (used by the full-screen viewer). */
public class ZoomImageView extends AppCompatImageView {
    private final Matrix matrix = new Matrix();
    private final float[] m = new float[9];
    private float scale = 1f;
    private static final float MIN = 1f, MAX = 5f;
    private final PointF last = new PointF();
    private ScaleGestureDetector scaleDetector;
    private GestureDetector tapDetector;
    private boolean sized = false;

    public ZoomImageView(Context c) { super(c); init(); }
    public ZoomImageView(Context c, AttributeSet a) { super(c, a); init(); }

    private void init() {
        setScaleType(ScaleType.MATRIX);
        scaleDetector = new ScaleGestureDetector(getContext(), new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override public boolean onScale(ScaleGestureDetector d) {
                float f = d.getScaleFactor();
                float target = Math.max(MIN, Math.min(MAX, scale * f));
                f = target / scale; scale = target;
                matrix.postScale(f, f, d.getFocusX(), d.getFocusY());
                clamp(); setImageMatrix(matrix); return true;
            }
        });
        tapDetector = new GestureDetector(getContext(), new GestureDetector.SimpleOnGestureListener() {
            @Override public boolean onDoubleTap(MotionEvent e) {
                float target = scale > 1.01f ? 1f : 2.5f, f = target / scale; scale = target;
                matrix.postScale(f, f, e.getX(), e.getY());
                if (scale == 1f) fit(); else { clamp(); setImageMatrix(matrix); }
                return true;
            }
        });
    }

    /** True when zoomed in — the pager should not intercept horizontal drags. */
    public boolean isZoomed() { return scale > 1.01f; }

    @Override protected void onSizeChanged(int w, int h, int ow, int oh) { super.onSizeChanged(w, h, ow, oh); sized = true; fit(); }
    @Override public void setImageDrawable(Drawable d) { super.setImageDrawable(d); if (sized) fit(); }

    private void fit() {
        Drawable d = getDrawable(); if (d == null || getWidth() == 0) return;
        scale = 1f; matrix.reset();
        float dw = d.getIntrinsicWidth(), dh = d.getIntrinsicHeight();
        float s = Math.min(getWidth() / dw, getHeight() / dh);
        matrix.postScale(s, s);
        matrix.postTranslate((getWidth() - dw * s) / 2f, (getHeight() - dh * s) / 2f);
        setImageMatrix(matrix);
    }

    private void clamp() {
        Drawable d = getDrawable(); if (d == null) return;
        RectF r = new RectF(0, 0, d.getIntrinsicWidth(), d.getIntrinsicHeight());
        matrix.mapRect(r);
        float dx = 0, dy = 0;
        if (r.width() <= getWidth()) dx = (getWidth() - r.width()) / 2f - r.left;
        else if (r.left > 0) dx = -r.left; else if (r.right < getWidth()) dx = getWidth() - r.right;
        if (r.height() <= getHeight()) dy = (getHeight() - r.height()) / 2f - r.top;
        else if (r.top > 0) dy = -r.top; else if (r.bottom < getHeight()) dy = getHeight() - r.bottom;
        matrix.postTranslate(dx, dy);
    }

    @Override public boolean onTouchEvent(MotionEvent e) {
        scaleDetector.onTouchEvent(e); tapDetector.onTouchEvent(e);
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN: last.set(e.getX(), e.getY()); break;
            case MotionEvent.ACTION_MOVE:
                if (isZoomed() && !scaleDetector.isInProgress() && e.getPointerCount() == 1) {
                    matrix.postTranslate(e.getX() - last.x, e.getY() - last.y);
                    clamp(); setImageMatrix(matrix);
                }
                last.set(e.getX(), e.getY()); break;
        }
        if (isZoomed()) getParent().requestDisallowInterceptTouchEvent(true);
        return true;
    }
}
