package com.gathin.tvhome;

import org.junit.Test;
import static org.junit.Assert.*;

public class WeatherRulesTest {
    @Test public void freshCacheAndFailedAttemptDoNotFloodTheService() {
        long now = 10_000_000;
        assertFalse(WeatherRules.due(now, now - 60_000, 0));
        assertFalse(WeatherRules.due(now, 0, now - 60_000));
        assertTrue(WeatherRules.due(now, now - WeatherRules.REFRESH_MS, now - 6 * 60_000));
        assertTrue(WeatherRules.due(now, 0, 0));
    }
    @Test public void clockMovingBackwardsDoesNotPermanentlyBlockUpdates() {
        assertTrue(WeatherRules.due(100, 200, 200));
    }
    @Test public void unitConversionUsesTheSameCachedReading() {
        assertEquals("0°C", WeatherRules.temperature(0, false));
        assertEquals("32°F", WeatherRules.temperature(0, true));
        assertEquals("212°F", WeatherRules.temperature(100, true));
        assertEquals("-40°F", WeatherRules.temperature(-40, true));
    }
    @Test public void invalidCoordinatesAreRejected() {
        assertFalse(WeatherRules.coordinates(Double.NaN, 0));
        assertFalse(WeatherRules.coordinates(91, 0));
        assertFalse(WeatherRules.coordinates(0, Double.POSITIVE_INFINITY));
        assertTrue(WeatherRules.coordinates(-19.93167, -44.05361));
    }
    @Test public void conditionsHandleNightRainAndUnknownCodes() {
        assertEquals("Noite limpa", WeatherRules.condition(0, false));
        assertEquals("Chuva forte", WeatherRules.condition(65, true));
        assertEquals("Condição indisponível", WeatherRules.condition(-1, true));
    }
}
