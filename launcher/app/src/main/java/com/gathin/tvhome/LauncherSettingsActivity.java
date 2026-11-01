package com.gathin.tvhome;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.TextView;
import android.widget.LinearLayout;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Remote-friendly settings owned by the launcher; system settings stay one row away. */
public final class LauncherSettingsActivity extends Activity {
    @Override
    protected void attachBaseContext(android.content.Context base) {
        super.attachBaseContext(LauncherLocale.wrap(base));   // locale
    }

    static final String EXTRA_REORDER = "com.gathin.tvhome.extra.REORDER_FROM_SETTINGS";
    private SharedPreferences choices;
    private final TextView[] columnChoices = new TextView[4];
    private final TextView[] rowChoices = new TextView[4];
    private static final int[] DOCK_OPACITIES = {100, 70, 50, 30};
    private final TextView[] dockChoices = new TextView[DOCK_OPACITIES.length];
    private TextView wallpaperChoice;
    private TextView rotationChoice;
    private TextView galaxyChoice;
    private boolean galaxyEnabled;
    private TextView animationsChoice;
    private TextView advancedChoice;
    private TextView followThemeChoice;
    private TextView openOnStartChoice;
    private int columns;
    private int rowsBelowDock;
    private int dockOpacity;
    private boolean animations;
    private boolean advancedEffects;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_launcher_settings);
        choices = getSharedPreferences(Config.SETTINGS_NAME, MODE_PRIVATE);
        Config config = Config.load(this);
        View reorderChoice = findViewById(R.id.reorderChoice);
        reorderChoice.setVisibility(config.launcherSettingsButton ? View.VISIBLE : View.GONE);
        reorderChoice.setOnClickListener(v -> {
            setResult(RESULT_OK, new Intent().putExtra(EXTRA_REORDER, true));
            finish();
        });
        columns = config.columns;
        rowsBelowDock = config.rowsBelowDock;
        dockOpacity = config.dockOpacity;
        animations = config.animations;
        advancedEffects = config.animations && config.advancedEffects;

        int[] ids = {R.id.columns4, R.id.columns5, R.id.columns6, R.id.columns7};
        for (int i = 0; i < ids.length; i++) {
            final int value = i + 4;
            columnChoices[i] = findViewById(ids[i]);
            columnChoices[i].setOnClickListener(v -> {
                if (columns == value) return;
                columns = value;
                choices.edit().putInt("columns", value).apply();
                changed();
                refresh();
            });
        }
        int[] rowIds = {R.id.rows0, R.id.rows1, R.id.rows2, R.id.rows3};
        for (int i = 0; i < rowIds.length; i++) {
            final int value = i;
            rowChoices[i] = findViewById(rowIds[i]);
            rowChoices[i].setOnClickListener(v -> {
                if (rowsBelowDock == value) return;
                rowsBelowDock = value;
                choices.edit().putInt(Config.ROWS_BELOW_DOCK_KEY, value).apply();
                changed();
                refresh();
            });
        }
        int[] dockIds = {R.id.dock100, R.id.dock70, R.id.dock50, R.id.dock30};
        for (int i = 0; i < dockIds.length; i++) {
            final int value = DOCK_OPACITIES[i];
            dockChoices[i] = findViewById(dockIds[i]);
            dockChoices[i].setOnClickListener(v -> {
                if (dockOpacity == value) return;
                dockOpacity = value;
                choices.edit().putInt(Config.DOCK_OPACITY_KEY, value).apply();
                changed();
                refresh();
            });
        }
        // Mirror text alignment without globally opting the launcher into RTL. Numeric rows
        // still need explicit ordering in Android's no-supportsRtl compatibility mode.
        if (getResources().getBoolean(R.bool.rtl_menu_bar))
            findViewById(android.R.id.content).setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        if (getResources().getBoolean(R.bool.rtl_app_grid)) {
            reverseRow(columnChoices);
            reverseRow(rowChoices);
            reverseRow(dockChoices);
        }
        galaxyEnabled = config.galaxyBackground;
        galaxyChoice = findViewById(R.id.galaxyChoice);
        galaxyChoice.setOnClickListener(v -> {
            galaxyEnabled = !galaxyEnabled;
            SharedPreferences.Editor edit = choices.edit().putBoolean("galaxy_background", galaxyEnabled);
            if (galaxyEnabled) edit.remove(Config.WALLPAPER_CHOICE_KEY);
            edit.apply();
            changed();
            refresh();
        });
        wallpaperChoice = findViewById(R.id.wallpaperChoice);
        wallpaperChoice.setOnClickListener(v -> showWallpaperPicker());
        rotationChoice = findViewById(R.id.rotationChoice);
        rotationChoice.setOnClickListener(v -> showRotationSettings());
        findViewById(R.id.backChoice).setOnClickListener(v -> finish());
        animationsChoice = findViewById(R.id.animationsChoice);
        animationsChoice.setOnClickListener(v -> {
            animations = !animations;
            if (!animations) advancedEffects = false;
            choices.edit().putBoolean("animations", animations)
                    .putBoolean("advanced_effects", advancedEffects).apply();
            changed();
            refresh();
        });
        advancedChoice = findViewById(R.id.advancedChoice);
        advancedChoice.setOnClickListener(v -> {
            if (advancedEffects) {
                setAdvanced(false);
            } else {
                new AlertDialog.Builder(this)
                        .setTitle(R.string.advanced_effects)
                        .setMessage(R.string.advanced_effects_warning)
                        .setPositiveButton(R.string.enable, (dialog, which) -> setAdvanced(true))
                        .setNegativeButton(android.R.string.cancel, null)
                        .show();
            }
        });
        openOnStartChoice = findViewById(R.id.openOnStartChoice);
        findViewById(R.id.weatherChoice).setOnClickListener(v -> startActivity(new Intent(this, WeatherSettingsActivity.class)));
        if (config.openOnStartChoice) {
            openOnStartChoice.setVisibility(View.VISIBLE);
            openOnStartChoice.setOnClickListener(v -> showOpenOnStartPicker());
        }
        followThemeChoice = findViewById(R.id.followThemeChoice);
        followThemeChoice.setOnClickListener(v -> {
            if (!hasOnTvChoices()) return;
            choices.edit().remove("columns").remove(Config.ROWS_BELOW_DOCK_KEY).remove("animations")
                    .remove("advanced_effects").remove(Config.WALLPAPER_CHOICE_KEY)
                    .remove(Config.DOCK_OPACITY_KEY).remove(OpenOnStart.KEY).remove("galaxy_background")
                    .remove("wallpaper_rotation_minutes").remove("wallpaper_order").commit();
            Config current = Config.load(this);
            galaxyEnabled = current.galaxyBackground;
            columns = current.columns;
            rowsBelowDock = current.rowsBelowDock;
            dockOpacity = current.dockOpacity;
            animations = current.animations;
            advancedEffects = current.animations && current.advancedEffects;
            changed();
            refresh();
        });
        findViewById(R.id.defaultHomeChoice).setOnClickListener(v -> {
            Toast.makeText(this, R.string.home_settings_help, Toast.LENGTH_LONG).show();
            open(new Intent(Settings.ACTION_HOME_SETTINGS));
        });
        findViewById(R.id.tvSettingsChoice).setOnClickListener(v ->
                open(SystemSettings.intent(this, Settings.ACTION_SETTINGS)));
        findViewById(R.id.moreChoice).setOnClickListener(v -> {
            int[] extraIds = {R.id.weatherChoice, R.id.animationsChoice, R.id.advancedChoice,
                    R.id.openOnStartChoice, R.id.followThemeChoice, R.id.defaultHomeChoice, R.id.tvSettingsChoice};
            List<TextView> actions = new ArrayList<>();
            List<String> labels = new ArrayList<>();
            for (int id : extraIds) {
                TextView action = findViewById(id);
                if (action.getVisibility() == View.VISIBLE && action.isEnabled()) {
                    actions.add(action);
                    labels.add(action.getText().toString());
                }
            }
            new AlertDialog.Builder(this).setTitle("Mais configurações")
                    .setItems(labels.toArray(new String[0]), (dialog, which) -> actions.get(which).performClick())
                    .setNegativeButton("Voltar", null).show();
        });
        refresh();
        if (config.launcherSettingsButton) reorderChoice.requestFocus();
        else columnChoices[Math.max(0, Math.min(columns - 4, 3))].requestFocus();
    }

    private void reverseRow(TextView[] views) {
        LinearLayout row = (LinearLayout) views[0].getParent();
        row.removeAllViews();
        for (int i = views.length - 1; i >= 0; i--) row.addView(views[i]);
    }

    private void showWallpaperPicker() {
        File dir = Config.dir(this);
        List<File> images = new java.util.ArrayList<>(WallpaperRotation.images(
                dir == null ? null : new File(dir, "wallpapers")));
        // A theme with a single wallpaper.jpg offers that picture too (Config maps the choice back to it).
        File single = dir == null ? null : new File(dir, Config.WALLPAPER_NAME);
        if (single != null && single.isFile()) images.add(0, single);
        String[] labels = new String[images.size() + 1];
        for (int i = 0; i < images.size(); i++) labels[i] = wallpaperDisplayName(images.get(i).getName());
        labels[images.size()] = getString(R.string.wallpaper_none);
        String current = choices.getString(Config.WALLPAPER_CHOICE_KEY, null);
        int checked = "none".equals(current) ? images.size()
                : indexOf(images, current);
        new AlertDialog.Builder(this)
                .setTitle(R.string.wallpaper_label)
                .setSingleChoiceItems(labels, checked, (dialog, which) -> {
                    dialog.dismiss();
                    String selection = which == images.size() ? "none" : images.get(which).getName();
                    choices.edit().putString(Config.WALLPAPER_CHOICE_KEY, selection)
                            .putBoolean("galaxy_background", false).apply();
                    galaxyEnabled = false;
                    changed();
                    Toast.makeText(this, R.string.wallpaper_applied, Toast.LENGTH_SHORT).show();
                    refresh();
                })
                .setNeutralButton(R.string.wallpaper_theme_default, (dialog, which) -> {
                    choices.edit().remove(Config.WALLPAPER_CHOICE_KEY).remove("galaxy_background").apply();
                    galaxyEnabled = Config.load(this).galaxyBackground;
                    changed();
                    refresh();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void showRotationSettings() {
        Config current = Config.load(this);
        String order = WallpaperRotation.ORDER_SHUFFLE.equals(current.wallpaperOrder) ? "Aleatória" : "Sequencial";
        String interval = current.wallpaperRotationMinutes == 0 ? "Desativado" : current.wallpaperRotationMinutes + " minutos";
        new AlertDialog.Builder(this).setTitle("Troca automática de papel de parede")
                .setItems(new String[]{"Ordem: " + order, "Intervalo: " + interval}, (d, index) -> {
                    if (index == 0) {
                        new AlertDialog.Builder(this).setTitle("Ordem das imagens")
                                .setSingleChoiceItems(new String[]{"Aleatória", "Sequencial"},
                                        WallpaperRotation.ORDER_SHUFFLE.equals(current.wallpaperOrder) ? 0 : 1,
                                        (dialog, which) -> {
                                            choices.edit().putString("wallpaper_order", which == 0 ? WallpaperRotation.ORDER_SHUFFLE : WallpaperRotation.ORDER_SEQUENTIAL).apply();
                                            changed(); dialog.dismiss(); showRotationSettings();
                                        }).setNegativeButton(android.R.string.cancel, null).show();
                    } else {
                        new AlertDialog.Builder(this).setTitle("Intervalo de troca")
                                .setItems(new String[]{"Desativar", "10 minutos", "20 minutos", "30 minutos", "40 minutos", "50 minutos", "60 minutos", "Personalizar…"}, (dialog, which) -> {
                                    if (which < 7) setRotationMinutes(which * 10);
                                    else {
                                        android.widget.EditText input = new android.widget.EditText(this);
                                        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
                                        input.setText(String.valueOf(Math.max(1, current.wallpaperRotationMinutes)));
                                        AlertDialog custom = new AlertDialog.Builder(this).setTitle("Minutos (1 a 1440)")
                                                .setView(input).setPositiveButton("Salvar", null)
                                                .setNegativeButton(android.R.string.cancel, null).create();
                                        custom.setOnShowListener(unused -> custom.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(button -> {
                                            try {
                                                int minutes = Integer.parseInt(input.getText().toString());
                                                if (minutes < 1 || minutes > 1440) throw new NumberFormatException();
                                                setRotationMinutes(minutes); custom.dismiss();
                                            } catch (NumberFormatException error) { input.setError("Informe de 1 a 1440 minutos"); }
                                        }));
                                        custom.show();
                                    }
                                }).setNegativeButton(android.R.string.cancel, null).show();
                    }
                }).setNegativeButton("Fechar", null).show();
    }

    private void setRotationMinutes(int minutes) {
        File dir = Config.dir(this);
        if (minutes > 0 && WallpaperRotation.images(dir == null ? null : new File(dir, "wallpapers")).size() < 2) {
            Toast.makeText(this, "Adicione pelo menos dois papéis de parede", Toast.LENGTH_LONG).show();
            return;
        }
        choices.edit().putInt("wallpaper_rotation_minutes", minutes)
                .putString(Config.WALLPAPER_CHOICE_KEY, "rotation").putBoolean("galaxy_background", false).apply();
        galaxyEnabled = false;
        changed(); refresh();
        Toast.makeText(this, minutes == 0 ? "Troca automática desativada" : "Troca a cada " + minutes + " minutos", Toast.LENGTH_SHORT).show();
    }

    private static String wallpaperDisplayName(String fileName) {
        int extension = fileName.lastIndexOf('.');
        return extension > 0 ? fileName.substring(0, extension) : fileName;
    }

    /** Off, then every app with a TV entry point by name. Saved at once; it is read on the next boot. */
    private void showOpenOnStartPicker() {
        PackageManager pm = getPackageManager();
        List<String[]> apps = new ArrayList<>();
        Intent tv = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER);
        for (ResolveInfo r : pm.queryIntentActivities(tv, 0)) {
            String pkg = r.activityInfo.packageName;
            if (pkg.equals(getPackageName())) continue;
            boolean seen = false;
            for (String[] a : apps) seen |= a[1].equals(pkg);
            if (!seen) apps.add(new String[]{r.loadLabel(pm).toString(), pkg});
        }
        Collections.sort(apps, (a, b) -> a[0].compareToIgnoreCase(b[0]));
        String[] labels = new String[apps.size() + 1];
        labels[0] = getString(R.string.off);
        String current = choices.getString(OpenOnStart.KEY, "");
        int checked = 0;
        for (int i = 0; i < apps.size(); i++) {
            labels[i + 1] = apps.get(i)[0];
            if (apps.get(i)[1].equals(current)) checked = i + 1;
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.open_on_start_label)
                .setSingleChoiceItems(labels, checked, (dialog, which) -> {
                    dialog.dismiss();
                    if (which == 0) choices.edit().remove(OpenOnStart.KEY).apply();
                    else {
                        OpenOnStart.skipThisBoot(this);
                        choices.edit().putString(OpenOnStart.KEY, apps.get(which - 1)[1]).apply();
                    }
                    refresh();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    /** The picked app's name, its package when it is gone, or Off. */
    private String openOnStartLabel() {
        String pkg = choices.getString(OpenOnStart.KEY, "");
        if (pkg.isEmpty()) return getString(R.string.off);
        try {
            PackageManager pm = getPackageManager();
            return pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString();
        } catch (PackageManager.NameNotFoundException e) {
            return pkg;
        }
    }

    private static int indexOf(List<File> images, String name) {
        if (name == null) return -1;
        for (int i = 0; i < images.size(); i++) {
            if (images.get(i).getName().equals(name)) return i;
        }
        return -1;
    }

    /**
     * A choice was saved. Home rebuilds when it next resumes because the change count moved, whether these
     * settings are left with Back or with the remote's Home button (which cancels the activity result).
     */
    private void changed() {
        setResult(RESULT_OK);
        choices.edit().putLong(Config.SETTINGS_REVISION_KEY,
                choices.getLong(Config.SETTINGS_REVISION_KEY, 0) + 1).apply();
    }

    private void setAdvanced(boolean enabled) {
        advancedEffects = enabled;
        choices.edit().putBoolean("advanced_effects", enabled).apply();
        changed();
        refresh();
    }

    private void refresh() {
        galaxyChoice.setSelected(galaxyEnabled);
        setMenuLabel(galaxyChoice, "Fundo Galaxy", galaxyEnabled ? "Ativado" : "Desativado");
        Config effective = Config.load(this);
        boolean rotating = "rotation".equals(choices.getString(Config.WALLPAPER_CHOICE_KEY, null))
                && effective.wallpaperRotationMinutes > 0 && !galaxyEnabled;
        rotationChoice.setSelected(rotating);
        setMenuLabel(rotationChoice, "Troca automática", rotating
                ? (WallpaperRotation.ORDER_SHUFFLE.equals(effective.wallpaperOrder) ? "Aleatória" : "Sequencial") + " · " + effective.wallpaperRotationMinutes + " min"
                : "Desativada");
        for (int i = 0; i < columnChoices.length; i++) {
            columnChoices[i].setSelected(columns == i + 4);
        }
        for (int i = 0; i < rowChoices.length; i++) {
            rowChoices[i].setSelected(rowsBelowDock == i);
        }
        for (int i = 0; i < dockChoices.length; i++) {
            dockChoices[i].setSelected(dockOpacity == DOCK_OPACITIES[i]);
        }
        animationsChoice.setText(getString(R.string.animations_state,
                getString(animations ? R.string.on : R.string.off)));
        advancedChoice.setText(getString(R.string.advanced_effects_state,
                getString(advancedEffects ? R.string.on : R.string.off)));
        advancedChoice.setEnabled(animations);
        advancedChoice.setAlpha(animations ? 1f : 0.5f);
        String wallpaperCurrent = choices.getString(Config.WALLPAPER_CHOICE_KEY, null);
        setMenuLabel(wallpaperChoice, "Papel de parede", "rotation".equals(wallpaperCurrent) ? "Rotação"
                : "none".equals(wallpaperCurrent) ? getString(R.string.wallpaper_none)
                : wallpaperCurrent != null ? wallpaperDisplayName(wallpaperCurrent) : getString(R.string.wallpaper_theme_default));
        updateWallpaperThumbnail(effective);
        openOnStartChoice.setText(getString(R.string.open_on_start_state, openOnStartLabel()));
        followThemeChoice.setSelected(!hasOnTvChoices());
    }

    private void setMenuLabel(TextView view, String title, String detail) {
        android.text.SpannableString label = new android.text.SpannableString(title + "\n" + detail);
        label.setSpan(new android.text.style.RelativeSizeSpan(0.78f), title.length() + 1, label.length(), 0);
        label.setSpan(new android.text.style.ForegroundColorSpan(0xFFB3C2D6), title.length() + 1, label.length(), 0);
        view.setText(label);
    }

    private void updateWallpaperThumbnail(Config config) {
        File image = config.wallpaper;
        File directory = Config.dir(this);
        String shown = getSharedPreferences("wallpaper_rotation", MODE_PRIVATE).getString("image", "");
        if (directory != null && !shown.isEmpty()) {
            File current = new File(new File(directory, "wallpapers"), shown);
            if (current.isFile()) image = current;
        }
        if (image == null) { wallpaperChoice.setCompoundDrawablesRelative(null, null, null, null); return; }
        android.graphics.BitmapFactory.Options options = new android.graphics.BitmapFactory.Options();
        options.inSampleSize = 8;
        android.graphics.Bitmap bitmap = android.graphics.BitmapFactory.decodeFile(image.getAbsolutePath(), options);
        if (bitmap == null) return;
        float density = getResources().getDisplayMetrics().density;
        android.graphics.Bitmap thumbnail = android.media.ThumbnailUtils.extractThumbnail(bitmap,
                Math.round(142 * density), Math.round(72 * density));
        if (thumbnail != bitmap) bitmap.recycle();
        android.graphics.drawable.BitmapDrawable drawable = new android.graphics.drawable.BitmapDrawable(getResources(), thumbnail);
        drawable.setBounds(0, 0, Math.round(142 * density), Math.round(72 * density));
        wallpaperChoice.setCompoundDrawablesRelative(drawable, null, null, null);
    }

    private boolean hasOnTvChoices() {
        return choices.contains("columns") || choices.contains(Config.ROWS_BELOW_DOCK_KEY)
                || choices.contains("animations") || choices.contains("advanced_effects")
                || choices.contains(Config.WALLPAPER_CHOICE_KEY) || choices.contains(Config.DOCK_OPACITY_KEY)
                || choices.contains(OpenOnStart.KEY) || choices.contains("galaxy_background");
    }

    private void open(Intent intent) {
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException | SecurityException e) {
            Toast.makeText(this, R.string.not_available, Toast.LENGTH_LONG).show();
        }
    }
}
