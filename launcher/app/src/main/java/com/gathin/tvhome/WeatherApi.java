package com.gathin.tvhome;

import org.json.JSONArray;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import javax.net.ssl.HttpsURLConnection;

final class WeatherApi {
    static final class City {
        final String name, label;
        final double latitude, longitude;
        City(JSONObject json) throws Exception {
            name = json.getString("name");
            String state = json.optString("admin1"), district = json.optString("admin2"), country = json.optString("country");
            label = name + (state.isEmpty() ? "" : " — " + state)
                    + (district.isEmpty() || district.equals(name) ? "" : ", " + district)
                    + (country.isEmpty() ? "" : " · " + country);
            latitude = json.getDouble("latitude"); longitude = json.getDouble("longitude");
            if (!WeatherRules.coordinates(latitude, longitude)) throw new IOException("Invalid coordinates");
        }
    }
    static List<City> search(String name) throws Exception {
        JSONObject result = get("https://geocoding-api.open-meteo.com/v1/search?count=8&language=pt&format=json&name="
                + URLEncoder.encode(name, StandardCharsets.UTF_8.name()));
        JSONArray array = result.optJSONArray("results");
        List<City> cities = new ArrayList<>();
        if (array != null) for (int i = 0; i < array.length(); i++) cities.add(new City(array.getJSONObject(i)));
        return cities;
    }
    static JSONObject current(double lat, double lon) throws Exception {
        if (!WeatherRules.coordinates(lat, lon)) throw new IOException("Invalid coordinates");
        return get("https://api.open-meteo.com/v1/forecast?latitude=" + lat + "&longitude=" + lon
                + "&current=temperature_2m,weather_code,is_day&timezone=auto&forecast_days=1").getJSONObject("current");
    }
    private static JSONObject get(String address) throws Exception {
        HttpsURLConnection connection = (HttpsURLConnection) new URL(address).openConnection();
        connection.setConnectTimeout(8000); connection.setReadTimeout(8000);
        connection.setRequestProperty("Accept", "application/json");
        try {
            if (connection.getResponseCode() != 200) throw new IOException("Weather service unavailable");
            try (InputStream input = connection.getInputStream(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
                byte[] buffer = new byte[4096]; int count;
                while ((count = input.read(buffer)) != -1) {
                    if (Thread.currentThread().isInterrupted() || output.size() + count > 131072) throw new IOException("Request cancelled or too large");
                    output.write(buffer, 0, count);
                }
                return new JSONObject(output.toString(StandardCharsets.UTF_8.name()));
            }
        } finally { connection.disconnect(); }
    }
}
