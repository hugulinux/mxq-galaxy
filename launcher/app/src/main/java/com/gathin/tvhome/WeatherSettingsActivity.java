package com.gathin.tvhome;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Small TV settings page; city searches run only after the user presses Search. */
public final class WeatherSettingsActivity extends Activity {
    private SharedPreferences prefs;
    private TextView enabledRow, cityRow, unitRow;
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private int searchRevision;
    private AlertDialog pending;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        prefs = getSharedPreferences(WeatherWidget.PREFS, MODE_PRIVATE);
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(0xFF101519);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(56), dp(36), dp(56), dp(36));
        scroll.addView(content);
        TextView title = new TextView(this);
        title.setText("Clima"); title.setTextSize(30); title.setTextColor(0xFFFFFFFF);
        content.addView(title);
        enabledRow = row(content, () -> {
            if (!prefs.contains("latitude")) { search(); return; }
            prefs.edit().putBoolean("enabled", !prefs.getBoolean("enabled", false)).apply(); refresh();
        });
        cityRow = row(content, this::search);
        unitRow = row(content, () -> {
            prefs.edit().putBoolean("fahrenheit", !prefs.getBoolean("fahrenheit", false)).apply(); refresh();
        });
        TextView note = new TextView(this);
        note.setText("Atualização a cada 30 minutos enquanto a tela inicial estiver aberta. Sem conexão, mantém a última consulta.\n\nA busca informa a cidade escolhida ao serviço de clima. Não utiliza localização automática.\n\nDados: Open-Meteo.com (CC BY 4.0). Cidades: GeoNames.org.");
        note.setTextColor(0xFFACB8C1); note.setTextSize(16);
        LinearLayout.LayoutParams noteParams = new LinearLayout.LayoutParams(-1, -2); noteParams.topMargin = dp(24);
        content.addView(note, noteParams);
        setContentView(scroll); refresh(); enabledRow.requestFocus();
    }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
    private TextView row(LinearLayout content, Runnable action) {
        TextView row = new TextView(this, null, 0, R.style.SettingsRow);
        row.setSingleLine(false);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.topMargin = dp(18); row.setMinHeight(dp(56));
        row.setPadding(dp(18), dp(12), dp(18), dp(12));
        content.addView(row, params); row.setOnClickListener(v -> action.run()); return row;
    }
    private void refresh() {
        enabledRow.setText("Mostrar clima: " + (prefs.getBoolean("enabled", false) ? "Ativado" : "Desativado"));
        cityRow.setText("Cidade: " + prefs.getString("label", "Escolher cidade…"));
        unitRow.setText("Unidade: " + (prefs.getBoolean("fahrenheit", false) ? "°F" : "°C"));
    }
    private void search() {
        EditText query = new EditText(this);
        query.setSingleLine(true); query.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        query.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH);
        query.setHint("Cidade, estado ou país"); query.setText(prefs.getString("name", "")); query.selectAll();
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle("Buscar cidade").setView(query)
                .setPositiveButton("Buscar", null).setNegativeButton("Cancelar", null).create();
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String name = query.getText().toString().trim();
            if (name.length() < 2) { query.setError("Digite pelo menos duas letras"); return; }
            dialog.dismiss(); findCities(name);
        }));
        query.setOnEditorActionListener((v, action, event) -> {
            if (action != android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH) return false;
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick(); return true;
        });
        dialog.show(); query.requestFocus();
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
    }
    private void findCities(String name) {
        int revision = ++searchRevision;
        pending = new AlertDialog.Builder(this).setMessage("Buscando cidades…").setNegativeButton("Cancelar", (d, w) -> searchRevision++).create();
        pending.setOnCancelListener(d -> searchRevision++); pending.show();
        worker.execute(() -> {
            List<WeatherApi.City> cities;
            try { cities = WeatherApi.search(name); }
            catch (Exception failure) { cities = null; }
            final List<WeatherApi.City> found = cities;
            runOnUiThread(() -> {
                if (isDestroyed() || revision != searchRevision) return;
                pending.dismiss();
                if (found == null) { message("Não foi possível buscar. Confira a conexão e tente novamente."); return; }
                if (found.isEmpty()) { message("Nenhuma cidade encontrada. Tente o nome seguido do estado ou país."); return; }
                String[] labels = new String[found.size()];
                for (int i = 0; i < labels.length; i++) labels[i] = found.get(i).label;
                new AlertDialog.Builder(this).setTitle("Escolher cidade").setItems(labels, (d, which) -> {
                    WeatherApi.City city = found.get(which);
                    prefs.edit().putString("name", city.name).putString("label", city.label)
                            .putLong("latitude", Double.doubleToLongBits(city.latitude)).putLong("longitude", Double.doubleToLongBits(city.longitude))
                            .putBoolean("enabled", true).remove("temperature").remove("code").remove("updated").remove("attempted").remove("failed").apply();
                    refresh(); Toast.makeText(this, "Cidade salva", Toast.LENGTH_SHORT).show();
                }).setNegativeButton("Cancelar", null).show();
            });
        });
    }
    private void message(String message) { new AlertDialog.Builder(this).setMessage(message).setPositiveButton("OK", null).show(); }
    @Override protected void onStop() {
        searchRevision++; if (pending != null) pending.dismiss(); super.onStop();
    }
    @Override protected void onDestroy() {
        searchRevision++; if (pending != null) pending.dismiss(); worker.shutdownNow(); super.onDestroy();
    }
}
