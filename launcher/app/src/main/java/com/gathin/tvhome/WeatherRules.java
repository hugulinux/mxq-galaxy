package com.gathin.tvhome;

import java.util.Locale;

final class WeatherRules {
    static final long REFRESH_MS = 30 * 60_000L;
    static boolean due(long now, long updated, long attempted) {
        boolean old = updated <= 0 || now < updated || now - updated >= REFRESH_MS;
        return old && (attempted <= 0 || now < attempted || now - attempted >= 5 * 60_000L);
    }
    static boolean coordinates(double lat, double lon) {
        return Double.isFinite(lat) && Double.isFinite(lon) && Math.abs(lat) <= 90 && Math.abs(lon) <= 180;
    }
    static String temperature(double celsius, boolean fahrenheit) {
        return String.format(Locale.getDefault(), "%.0f°%s", fahrenheit ? celsius * 1.8 + 32 : celsius, fahrenheit ? "F" : "C");
    }
    static String condition(int code, boolean day) {
        switch (code) {
            case 0: return day ? "Céu limpo" : "Noite limpa";
            case 1: return "Poucas nuvens";
            case 2: return "Parcialmente nublado";
            case 3: return "Nublado";
            case 45: case 48: return "Neblina";
            case 51: case 53: case 55: return "Garoa";
            case 56: case 57: case 66: case 67: return "Chuva congelante";
            case 61: case 63: return "Chuva";
            case 65: return "Chuva forte";
            case 71: case 73: case 75: case 77: case 85: case 86: return "Neve";
            case 80: case 81: case 82: return "Pancadas de chuva";
            case 95: return "Trovoadas";
            case 96: case 99: return "Trovoadas com granizo";
            default: return "Condição indisponível";
        }
    }
}
