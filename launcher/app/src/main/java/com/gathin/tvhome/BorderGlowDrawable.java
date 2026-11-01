package com.gathin.tvhome;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;

/** Native, event-driven interpretation of the supplied React Bits BorderGlow reference.
 * Only strokes are drawn: the wallpaper and the original glass fill stay visible.
 * Shaders are reused until bounds or the focused dock position change. */
final class BorderGlowDrawable extends Drawable {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF outline = new RectF();
    private final float density;
    private final float corner;
    private boolean focused;
    private float position = 0.5f;
    private int opacity = 255;
    private Shader gradient;

    BorderGlowDrawable(float density, float corner) {
        this.density = density;
        this.corner = corner;
        paint.setStyle(Paint.Style.STROKE);
        paint.setDither(true);
    }

    void focus(boolean active, float fraction) {
        fraction = Math.max(0, Math.min(1, fraction));
        if (focused == active && Math.abs(position - fraction) < 0.005f) return;
        focused = active;
        position = fraction;
        gradient = null;
        invalidateSelf();
    }

    @Override protected void onBoundsChange(Rect bounds) { gradient = null; }
    @Override public boolean isStateful() { return true; }
    @Override protected boolean onStateChange(int[] state) {
        boolean active = false;
        for (int value : state) if (value == android.R.attr.state_focused) active = true;
        focus(active, 0.5f);
        return true;
    }

    @Override public void draw(Canvas canvas) {
        Rect bounds = getBounds();
        if (bounds.isEmpty()) return;
        if (gradient == null) {
            float center = bounds.left + bounds.width() * position;
            float reach = Math.max(bounds.height(), bounds.width() * 0.55f);
            gradient = new LinearGradient(center - reach, bounds.top, center + reach, bounds.bottom,
                    new int[]{0xFF38BDF8, 0xFFC084FC, 0xFFF472B6, 0xFF38BDF8},
                    new float[]{0, 0.35f, 0.7f, 1}, Shader.TileMode.CLAMP);
        }
        paint.setShader(gradient);
        // The halo stays within the available bounds; no software blur or temporary layers.
        float inset = 2 * density;
        outline.set(bounds.left + inset, bounds.top + inset, bounds.right - inset, bounds.bottom - inset);
        float radius = Math.max(0, corner - inset);
        for (int i = 3; i >= 1; i--) {
            paint.setStrokeWidth((1.2f + i * 1.5f) * density);
            paint.setAlpha((focused ? 18 : 5) * opacity / 255);
            canvas.drawRoundRect(outline, radius, radius, paint);
        }
        paint.setStrokeWidth((focused ? 1.5f : 0.8f) * density);
        paint.setAlpha((focused ? 225 : 72) * opacity / 255);
        canvas.drawRoundRect(outline, radius, radius, paint);
    }

    @Override public void setAlpha(int value) { opacity = value; invalidateSelf(); }
    @Override public void setColorFilter(ColorFilter filter) { paint.setColorFilter(filter); }
    @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
}
