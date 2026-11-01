package com.gathin.tvhome;

/** Builds the static dark glow with unbiased 1-LSB stochastic rounding, avoiding wide colour bands. */
final class GalaxyBackdrop {
    private GalaxyBackdrop() { }

    static float noise(int x, int y) {
        int hash = x * 0x1F1F1F1F ^ y * 0x045D9F3B;
        hash ^= hash >>> 16;
        hash *= 0x7FEB352D;
        hash ^= hash >>> 15;
        return (hash & 0xFFFF) / 65536f;
    }

    static int quantize(float value, float noise) {
        return Math.max(0, Math.min(255, (int) Math.floor(value + noise)));
    }

    static int[] pixels(int width, int height) {
        if (width <= 0 || height <= 0 || (long) width * height > 8_400_000L)
            throw new IllegalArgumentException("invalid backdrop size");
        int[] pixels = new int[width * height];
        float cx = width * 0.56f, cy = height * 0.35f;
        float radius = Math.max(width, height) * 0.72f;
        for (int y = 0; y < height; y++) {
            float dy = y - cy;
            for (int x = 0; x < width; x++) {
                float dx = x - cx;
                float t = Math.min(1, (float) Math.sqrt(dx * dx + dy * dy) / radius);
                float mix = t < 0.5f ? t * 2 : (t - 0.5f) * 2;
                int r0 = t < 0.5f ? 11 : 3, r1 = t < 0.5f ? 3 : 1;
                int g0 = t < 0.5f ? 16 : 5, g1 = t < 0.5f ? 5 : 2;
                int b0 = t < 0.5f ? 36 : 13, b1 = t < 0.5f ? 13 : 7;
                float dither = noise(x, y);
                int r = quantize(r0 + (r1 - r0) * mix, dither);
                int g = quantize(g0 + (g1 - g0) * mix, dither);
                int b = quantize(b0 + (b1 - b0) * mix, dither);
                pixels[y * width + x] = 0xFF000000 | r << 16 | g << 8 | b;
            }
        }
        return pixels;
    }
}
