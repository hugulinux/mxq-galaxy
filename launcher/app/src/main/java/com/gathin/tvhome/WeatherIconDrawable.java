package com.gathin.tvhome;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;

/** Microsoft Fluent System weather glyph for the home-screen temperature row. */
final class WeatherIconDrawable extends Drawable {
    private final Context context;
    private final int size;
    private int resourceId;
    private Drawable icon;

    WeatherIconDrawable(Context context) {
        this.context = context;
        size = Math.round(22 * context.getResources().getDisplayMetrics().density);
        setWeather(-1, true);
    }

    void setWeather(int code, boolean isDay) {
        int next;
        if (code == 0) next = isDay ? com.gathin.tvhome.R.drawable.ic_fluent_weather_sunny_24_regular : com.gathin.tvhome.R.drawable.ic_fluent_weather_moon_24_regular;
        else if (code == 1 || code == 2) next = isDay ? com.gathin.tvhome.R.drawable.ic_fluent_weather_partly_cloudy_day_24_regular : com.gathin.tvhome.R.drawable.ic_fluent_weather_partly_cloudy_night_24_regular;
        else if (code == 3) next = com.gathin.tvhome.R.drawable.ic_fluent_weather_cloudy_24_regular;
        else if (code == 45 || code == 48) next = com.gathin.tvhome.R.drawable.ic_fluent_weather_fog_24_regular;
        else if (code == 51 || code == 53 || code == 55) next = com.gathin.tvhome.R.drawable.ic_fluent_weather_drizzle_24_regular;
        else if (code == 56 || code == 57 || code == 66 || code == 67) next = isDay ? com.gathin.tvhome.R.drawable.ic_fluent_weather_hail_day_24_regular : com.gathin.tvhome.R.drawable.ic_fluent_weather_hail_night_24_regular;
        else if (code == 61 || code == 63 || code == 65) next = com.gathin.tvhome.R.drawable.ic_fluent_weather_rain_24_regular;
        else if (code == 71 || code == 73 || code == 75 || code == 77) next = com.gathin.tvhome.R.drawable.ic_fluent_weather_snow_24_regular;
        else if (code == 80 || code == 81 || code == 82) next = isDay ? com.gathin.tvhome.R.drawable.ic_fluent_weather_rain_showers_day_24_regular : com.gathin.tvhome.R.drawable.ic_fluent_weather_rain_showers_night_24_regular;
        else if (code == 85 || code == 86) next = isDay ? com.gathin.tvhome.R.drawable.ic_fluent_weather_snow_shower_day_24_regular : com.gathin.tvhome.R.drawable.ic_fluent_weather_snow_shower_night_24_regular;
        else if (code == 95 || code == 96 || code == 99) next = com.gathin.tvhome.R.drawable.ic_fluent_weather_thunderstorm_24_regular;
        else next = com.gathin.tvhome.R.drawable.ic_fluent_weather_cloudy_24_regular;
        if (resourceId == next) return;
        resourceId = next;
        icon = context.getDrawable(next).mutate();
        icon.setTint(Color.WHITE);
        icon.setBounds(0, 0, size, size);
        invalidateSelf();
    }

    @Override public void draw(Canvas canvas) {
        if (icon == null || getBounds().isEmpty()) return;
        icon.setBounds(getBounds());
        icon.draw(canvas);
    }
    @Override public int getIntrinsicWidth() { return size; }
    @Override public int getIntrinsicHeight() { return size; }
    @Override public void setAlpha(int alpha) { if (icon != null) icon.setAlpha(alpha); invalidateSelf(); }
    @Override public void setColorFilter(android.graphics.ColorFilter colorFilter) { if (icon != null) icon.setColorFilter(colorFilter); invalidateSelf(); }
    @Override public int getOpacity() { return icon == null ? PixelFormat.TRANSLUCENT : icon.getOpacity(); }
}
