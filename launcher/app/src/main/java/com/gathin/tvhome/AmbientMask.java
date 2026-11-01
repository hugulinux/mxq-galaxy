package com.gathin.tvhome;

/** Full-resolution, fixed-noise mask. Quantize after intensity, so tinting does not erase the dither. */
final class AmbientMask {
    private AmbientMask() { }

    static float opacity(int width, int height, int x, int y, boolean cinematic, boolean horizon, boolean rtl) {
        float alpha;
        if (cinematic) {
            alpha = ramp(distance(x - width * (rtl ? .20f : .80f), y - height * .06f) / (width * .78f), 166, 71, .5f);
        } else if (horizon) {
            float base = ramp(y / (height * .75f), 102, 36, .4f);
            float spot = 64 * Math.max(0, 1 - distance(x - width * .5f, y + height * .25f) / (width * .55f));
            alpha = base + spot * (1 - base / 255f);
        } else {
            alpha = ramp(distance(x - width * .32f, y - height * .42f) / (width * .62f), 92, 38, .45f);
        }
        return alpha * (82f / 255f);
    }

    private static float distance(float x, float y) { return (float) Math.sqrt(x * x + y * y); }

    private static float ramp(float t, float start, float middle, float stop) {
        t = Math.max(0, Math.min(1, t));
        return t < stop ? start + (middle - start) * t / stop : middle * (1 - t) / (1 - stop);
    }

    static int[] pixels(int width, int height, boolean cinematic, boolean horizon, boolean rtl) {
        if (width <= 0 || height <= 0 || (long) width * height > 8_400_000L)
            throw new IllegalArgumentException("invalid ambient mask size");
        int[] pixels = new int[width * height];
        for (int y = 0; y < height; y++) for (int x = 0; x < width; x++) {
            float value = opacity(width, height, x, y, cinematic, horizon, rtl);
            // Tinting premultiplied pixels can merge adjacent alpha steps. A decorrelated,
            // zero-mean one-step spread survives that rounding without shifting the mean glow.
            float spread = (GalaxyBackdrop.noise(x + 153, y + 791) - .5f) * 2f * Math.min(1f, value);
            int alpha = GalaxyBackdrop.quantize(value + spread, GalaxyBackdrop.noise(x, y));
            pixels[y * width + x] = alpha << 24 | 0x00FFFFFF;
        }
        return pixels;
    }
}
