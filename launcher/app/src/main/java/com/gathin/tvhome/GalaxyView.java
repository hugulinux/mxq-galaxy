package com.gathin.tvhome;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.os.SystemClock;
import android.view.View;

import java.util.Random;

/** Native starfield inspired by React Bits Galaxy. No browser, network or background service. */
final class GalaxyView extends View {
    private static final int STAR_COUNT = 100;
    private static final long FRAME_MS = 50; // 20 FPS; remote/UI animations remain independently scheduled.
    private static final int[] PALETTE = {0xFFDFEFFF, 0xFF6AD5EF, 0xFFB79AFF};
    private final Random random = new Random(24601);
    private final float[] x = new float[STAR_COUNT], y = new float[STAR_COUNT];
    private final float[] depth = new float[STAR_COUNT], phase = new float[STAR_COUNT];
    private final float[] size = new float[STAR_COUNT];
    private final int[] color = new int[STAR_COUNT];
    private final Bitmap[] sprites = new Bitmap[3];
    private final Paint background = new Paint(Paint.DITHER_FLAG);
    private Bitmap backgroundBitmap;
    private int backgroundRevision;
    private final Paint stars = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final Paint core = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF destination = new RectF();
    private final float[][] points = new float[27][STAR_COUNT * 2];
    private final int[] pointCounts = new int[27];
    private boolean running;
    private final Runnable nextFrame = () -> { if (running) invalidate(); };
    private long lastFrame;
    private float elapsed;

    GalaxyView(Context context) {
        super(context);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        setFocusable(false);
        setClickable(false);
        core.setColor(Color.WHITE);
        core.setStrokeCap(Paint.Cap.ROUND);
        for (int i = 0; i < sprites.length; i++) sprites[i] = sprite(PALETTE[i]);
        for (int i = 0; i < STAR_COUNT; i++) {
            reset(i, random.nextFloat() * 0.9f + 0.1f);
            phase[i] = random.nextFloat() * 6.283185f;
            size[i] = 0.65f + random.nextFloat() * 0.8f;
            color[i] = random.nextInt(PALETTE.length);
        }
    }

    private static Bitmap sprite(int color) {
        Bitmap bitmap = Bitmap.createBitmap(48, 48, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setShader(new RadialGradient(24, 24, 23,
                new int[]{0xFFFFFFFF, color, (color & 0x00FFFFFF) | 0x40000000, 0},
                new float[]{0, 0.06f, 0.22f, 1}, Shader.TileMode.CLAMP));
        canvas.drawCircle(24, 24, 23, paint);
        paint.setShader(null);
        paint.setColor(0xAAFFFFFF);
        paint.setStrokeWidth(0.65f);
        canvas.drawLine(15, 24, 33, 24, paint);
        canvas.drawLine(24, 15, 24, 33, paint);
        return bitmap;
    }

    private void reset(int i, float z) {
        x[i] = (random.nextFloat() - 0.5f) * 3.3f;
        y[i] = (random.nextFloat() - 0.5f) * 2.7f;
        depth[i] = z;
    }

    void start() {
        if (running) return;
        running = true;
        lastFrame = SystemClock.uptimeMillis();
        invalidate();
    }

    void stop() {
        running = false;
        removeCallbacks(nextFrame);
    }

    @Override public boolean hasOverlappingRendering() { return false; }

    @Override protected void onSizeChanged(int w, int h, int oldW, int oldH) {
        super.onSizeChanged(w, h, oldW, oldH);
        if (w <= 0 || h <= 0) return;
        final int revision = ++backgroundRevision;
        // Generated once, off the UI thread: no random-noise or gradient work on each animation frame.
        new Thread(() -> {
            Bitmap ready;
            try {
                int[] pixels = GalaxyBackdrop.pixels(w, h);
                ready = Bitmap.createBitmap(pixels, w, h, Bitmap.Config.ARGB_8888);
            } catch (IllegalArgumentException | OutOfMemoryError failure) {
                return; // The solid fallback stays available on a constrained device.
            }
            post(() -> {
                if (revision != backgroundRevision || !isAttachedToWindow()) {
                    ready.recycle();
                    return;
                }
                backgroundBitmap = ready;
                invalidate();
            });
        }, "galaxy-backdrop").start();
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        long now = SystemClock.uptimeMillis();
        float step = running ? Math.min(0.08f, Math.max(0, (now - lastFrame) / 1000f)) : 0;
        lastFrame = now;
        elapsed += step;
        int w = getWidth(), h = getHeight();
        if (backgroundBitmap != null) canvas.drawBitmap(backgroundBitmap, 0, 0, background);
        else canvas.drawColor(0xFF03050D);
        float cx = w * 0.52f, cy = h * 0.40f;
        float scale = Math.min(w / 1280f, h / 720f);
        java.util.Arrays.fill(pointCounts, 0);
        for (int i = 0; i < STAR_COUNT; i++) {
            depth[i] -= step * 0.045f;
            if (depth[i] < 0.08f) reset(i, 1);
            float px = cx + x[i] * w * 0.34f / depth[i];
            float py = cy + y[i] * h * 0.38f / depth[i];
            if (px < -20 || px > w + 20 || py < -20 || py > h + 20) {
                reset(i, 1);
                px = cx + x[i] * w * 0.34f;
                py = cy + y[i] * h * 0.38f;
            }
            float near = 1 - depth[i];
            float twinkle = 0.82f + 0.18f * (float) Math.sin(elapsed * 1.1f + phase[i]);
            int sizeGroup = Math.min(2, (int) (near * 3));
            int lightGroup = Math.min(2, (int) ((0.55f + near * 0.45f) * twinkle * 3));
            int bucket = color[i] * 9 + sizeGroup * 3 + lightGroup;
            int at = pointCounts[bucket];
            points[bucket][at] = px;
            points[bucket][at + 1] = py;
            pointCounts[bucket] += 2;
            // A few soft flares; the other stars are batched below instead of 200 separate draw calls.
            if (i % 12 == 0 && near > 0.25f) {
                stars.setAlpha(Math.round(255 * (0.38f + near * 0.62f) * twinkle));
                float radius = (2.5f + near * 5.5f) * size[i] * scale;
                destination.set(px - radius, py - radius, px + radius, py + radius);
                canvas.drawBitmap(sprites[color[i]], null, destination, stars);
            }
        }
        for (int bucket = 0; bucket < points.length; bucket++) {
            if (pointCounts[bucket] == 0) continue;
            core.setColor(PALETTE[bucket / 9]);
            core.setAlpha(105 + (bucket % 3) * 70);
            core.setStrokeWidth((1.2f + (bucket % 9 / 3) * 0.7f) * scale);
            canvas.drawPoints(points[bucket], 0, pointCounts[bucket], core);
        }
        removeCallbacks(nextFrame);
        if (running && getWindowVisibility() == VISIBLE) postDelayed(nextFrame, FRAME_MS);
    }

    @Override protected void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility);
        removeCallbacks(nextFrame);
        if (visibility == VISIBLE && running) {
            lastFrame = SystemClock.uptimeMillis();
            invalidate();
        }
    }

    @Override protected void onDetachedFromWindow() {
        stop();
        backgroundRevision++;
        super.onDetachedFromWindow();
    }
}
