package com.gathin.tvhome;

import org.junit.Test;
import static org.junit.Assert.*;

public class GalaxyBackdropTest {
    @Test public void fractionalColourIsDistributedWithoutChangingItsAverage() {
        double total = 0;
        int lower = 0, upper = 0;
        for (int i = 0; i < 4096; i++) {
            int value = GalaxyBackdrop.quantize(12.25f, GalaxyBackdrop.noise(i, 7));
            assertTrue(value == 12 || value == 13);
            if (value == 12) lower++; else upper++;
            total += value;
        }
        assertTrue(lower > 0 && upper > 0);
        assertEquals(12.25, total / 4096, 0.025);
    }

    @Test public void flatIntegerColourDoesNotAcquireVisibleNoise() {
        for (int i = 0; i < 1024; i++)
            assertEquals(13, GalaxyBackdrop.quantize(13, GalaxyBackdrop.noise(i, 19)));
    }

    @Test public void backdropStaysOpaqueAndInsideItsPalette() {
        for (int pixel : GalaxyBackdrop.pixels(256, 144)) {
            assertEquals(255, pixel >>> 24);
            int r = pixel >> 16 & 255, g = pixel >> 8 & 255, b = pixel & 255;
            assertTrue(r >= 1 && r <= 11);
            assertTrue(g >= 2 && g <= 16);
            assertTrue(b >= 7 && b <= 36);
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void zeroSizedSurfaceIsRejected() { GalaxyBackdrop.pixels(0, 720); }
}
