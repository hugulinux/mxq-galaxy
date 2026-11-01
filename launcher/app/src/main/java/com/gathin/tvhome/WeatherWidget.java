package com.gathin.tvhome;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.View;
import android.widget.TextView;
import org.json.JSONObject;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Network work only while Home is visible; cache and preferences are independent of the theme. */
final class WeatherWidget {
    static final String PREFS = "weather";
    private final SharedPreferences prefs;
    private final TextView view;
    private final WeatherIconDrawable icon;
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private boolean active, loading, closed;
    WeatherWidget(Context context, TextView view) {
        prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE); this.view = view;
        icon = new WeatherIconDrawable(context);
        icon.setBounds(0, 0, icon.getIntrinsicWidth(), icon.getIntrinsicHeight());
        view.setSingleLine(true);
        view.setEllipsize(android.text.TextUtils.TruncateAt.END);
        view.setCompoundDrawablePadding(Math.round(8 * context.getResources().getDisplayMetrics().density));
        view.setCompoundDrawablesRelative(icon, null, null, null);
    }
    void start() { active = true; tick(); }
    void stop() { active = false; }
    void close() { closed = true; active = false; worker.shutdownNow(); }
    void tick() {
        if (!active || closed) return;
        boolean enabled = prefs.getBoolean("enabled", false) && prefs.contains("latitude");
        view.setVisibility(enabled ? View.VISIBLE : View.GONE);
        if (!enabled) return;
        render();
        long now = System.currentTimeMillis();
        if (loading || !WeatherRules.due(now, prefs.getLong("updated", 0), prefs.getLong("attempted", 0))) return;
        loading = true;
        prefs.edit().putLong("attempted", now).apply();
        long cityLatitude = prefs.getLong("latitude", 0), cityLongitude = prefs.getLong("longitude", 0);
        double lat = Double.longBitsToDouble(prefs.getLong("latitude", 0)), lon = Double.longBitsToDouble(prefs.getLong("longitude", 0));
        worker.execute(() -> {
            double temperature = Double.NaN; int code = -1; boolean day = true;
            try {
                JSONObject current = WeatherApi.current(lat, lon);
                temperature = current.getDouble("temperature_2m"); code = current.getInt("weather_code"); day = current.getInt("is_day") == 1;
                if (!Double.isFinite(temperature) || temperature < -100 || temperature > 70) temperature = Double.NaN;
            } catch (Exception ignored) { /* The last successful reading remains available. */ }
            final double result = temperature; final int weatherCode = code; final boolean daytime = day;
            view.post(() -> {
                loading = false;
                if (closed) return;
                if (cityLatitude != prefs.getLong("latitude", 0) || cityLongitude != prefs.getLong("longitude", 0)) {
                    if (active) tick();
                    return;
                }
                if (Double.isFinite(result)) prefs.edit().putFloat("temperature", (float) result).putInt("code", weatherCode)
                        .putBoolean("day", daytime).putLong("updated", System.currentTimeMillis()).putBoolean("failed", false).apply();
                else prefs.edit().putBoolean("failed", true).apply();
                if (active) render();
            });
        });
    }
    private void render() {
        String city = prefs.getString("name", "");
        long updated = prefs.getLong("updated", 0);
        if (updated == 0) {
            icon.setWeather(-1, true);
            view.setText(city + " · " + (prefs.getBoolean("failed", false) ? "Clima indisponível" : "Consultando clima…"));
            return;
        }
        int code = prefs.getInt("code", -1);
        boolean day = prefs.getBoolean("day", true);
        icon.setWeather(code, day);
        view.setText(city + " · " + WeatherRules.temperature(prefs.getFloat("temperature", 0), prefs.getBoolean("fahrenheit", false))
                + " · " + WeatherRules.condition(code, day));
    }
}
