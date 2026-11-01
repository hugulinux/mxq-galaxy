package com.gathin.tvhome;

import org.junit.Test;
import static org.junit.Assert.*;

public class AmbientMaskTest {
    @Test public void ditherPreservesSoftIntensityAndAverage() {
        int w = 320, h = 180;
        int[] pixels = AmbientMask.pixels(w, h, true, false, false);
        double expected = 0, actual = 0;
        int roundedUp = 0, roundedDown = 0;
        for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) {
            float alpha = AmbientMask.opacity(w, h, x, y, true, false, false);
            int value = pixels[y * w + x] >>> 24;
            assertTrue(value >= 0 && value <= 55);
            assertTrue(Math.abs(value - alpha) <= 2f);
            assertEquals(0xFFFFFF, pixels[y * w + x] & 0xFFFFFF);
            if (value > alpha) roundedUp++;
            if (value < alpha) roundedDown++;
            expected += alpha;
            actual += value;
        }
        assertTrue(roundedUp > 1000 && roundedDown > 1000);
        assertEquals(expected / pixels.length, actual / pixels.length, .02);
    }

    @Test public void maskIsFixedAndTransparentOutsideTheGlow() {
        assertArrayEquals(AmbientMask.pixels(80, 45, false, false, false), AmbientMask.pixels(80, 45, false, false, false));
        assertEquals(0, AmbientMask.pixels(80, 45, false, false, false)[80 * 45 - 1] >>> 24);
    }

    @Test public void horizonAndMirroredCinematicStayBounded() {
        for (int pixel : AmbientMask.pixels(120, 68, false, true, false)) assertTrue((pixel >>> 24) <= 54);
        assertEquals(AmbientMask.opacity(320, 180, 256, 30, true, false, false),
                AmbientMask.opacity(320, 180, 64, 30, true, false, true), .0001f);
    }

    @Test(expected = IllegalArgumentException.class)
    public void invalidSurfaceIsRejected() { AmbientMask.pixels(0, 720, true, false, false); }
}
