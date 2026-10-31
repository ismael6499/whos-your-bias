package com.nitsutech.whosyourbias.util;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.nitsutech.whosyourbias.R;
import com.nitsutech.whosyourbias.data.BiasRepository;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class ThemeManager {
    private static final String PREF_ACTIVE_THEME = "theme_active_v2";
    private static final String PREF_USER_PRESETS = "theme_user_presets_v2";

    public static class AppTheme {
        public String id;
        public String name;
        public int backgroundColor;
        public int surfaceColor;
        public int primaryColor;
        public int secondaryColor;
        public int textColor;
        public int cardBorderColor = Color.parseColor("#E5E7EB");
        public boolean isCustomUserPreset;

        public AppTheme(String id, String name, int backgroundColor, int surfaceColor,
                        int primaryColor, int secondaryColor, int textColor, boolean isCustomUserPreset) {
            this.id = id;
            this.name = name;
            this.backgroundColor = backgroundColor;
            this.surfaceColor = surfaceColor;
            this.primaryColor = primaryColor;
            this.secondaryColor = secondaryColor;
            this.textColor = textColor;
            this.isCustomUserPreset = isCustomUserPreset;
        }

        public int getMutedTextColor() {
            boolean isDark = isColorDark(backgroundColor);
            return isDark ? Color.parseColor("#CBD5E1") : Color.parseColor("#6B7280");
        }

        public int getButtonTextColor(int btnBg) {
            return isColorDark(btnBg) ? Color.WHITE : Color.parseColor("#111827");
        }

        public JSONObject toJson() {
            JSONObject o = new JSONObject();
            try {
                o.put("id", id);
                o.put("name", name);
                o.put("bg", backgroundColor);
                o.put("surface", surfaceColor);
                o.put("primary", primaryColor);
                o.put("secondary", secondaryColor);
                o.put("text", textColor);
                o.put("custom", isCustomUserPreset);
            } catch (Exception ignored) {
            }
            return o;
        }

        public static AppTheme fromJson(JSONObject o) {
            if (o == null) return getBuiltInPresets().get(0);
            return new AppTheme(
                    o.optString("id", "pastel_pink"),
                    o.optString("name", "🌸 Pastel Pink & Gold"),
                    o.optInt("bg", Color.parseColor("#FFF9FB")),
                    o.optInt("surface", Color.parseColor("#FFF0F5")),
                    o.optInt("primary", Color.parseColor("#FF85A2")),
                    o.optInt("secondary", Color.parseColor("#FFC700")),
                    o.optInt("text", Color.parseColor("#1F2937")),
                    o.optBoolean("custom", false)
            );
        }
    }

    public static List<AppTheme> getBuiltInPresets() {
        List<AppTheme> list = new ArrayList<>();
        list.add(new AppTheme("pastel_pink", "🌸 Pastel Pink & Gold",
                Color.parseColor("#FFFBFD"), Color.parseColor("#FFF0F6"),
                Color.parseColor("#FF85A2"), Color.parseColor("#FFC700"),
                Color.parseColor("#1F2937"), false));
        list.add(new AppTheme("vibrary_gold", "🌼 Vibrary Cream & Gold",
                Color.parseColor("#FFFFFF"), Color.parseColor("#F8F9FB"),
                Color.parseColor("#FFC700"), Color.parseColor("#FF85A2"),
                Color.parseColor("#111827"), false));
        list.add(new AppTheme("dreamy_lavender", "💜 Dreamy Lavender (ILLIT)",
                Color.parseColor("#FAF7FF"), Color.parseColor("#F3E8FF"),
                Color.parseColor("#A855F7"), Color.parseColor("#FF85A2"),
                Color.parseColor("#1E1B4B"), false));
        list.add(new AppTheme("peach_coquette", "🍑 Peach Coquette",
                Color.parseColor("#FFFDF9"), Color.parseColor("#FFEDD5"),
                Color.parseColor("#FB7185"), Color.parseColor("#FBBF24"),
                Color.parseColor("#1F2937"), false));
        list.add(new AppTheme("mint_matcha", "🌿 Mint & Matcha Pastel",
                Color.parseColor("#F6FEF9"), Color.parseColor("#DCFCE7"),
                Color.parseColor("#34D399"), Color.parseColor("#FBBF24"),
                Color.parseColor("#064E3B"), false));
        list.add(new AppTheme("sky_cotton", "☁️ Cotton Candy Sky",
                Color.parseColor("#F8FAFC"), Color.parseColor("#E0F2FE"),
                Color.parseColor("#38BDF8"), Color.parseColor("#F472B6"),
                Color.parseColor("#0F172A"), false));
        list.add(new AppTheme("midnight_rose", "🌙 Midnight Pastel (Oscuro)",
                Color.parseColor("#14111D"), Color.parseColor("#221D30"),
                Color.parseColor("#FF85A2"), Color.parseColor("#FFC700"),
                Color.parseColor("#F9FAFB"), false));
        return list;
    }

    public static AppTheme getActiveTheme(Context context) {
        SharedPreferences prefs = BiasRepository.getInstance(context).getPrefs();
        String raw = prefs.getString(PREF_ACTIVE_THEME, null);
        if (raw != null && !raw.isEmpty()) {
            try {
                return AppTheme.fromJson(new JSONObject(raw));
            } catch (Exception ignored) {
            }
        }
        return getBuiltInPresets().get(0);
    }

    public static void saveActiveTheme(Context context, AppTheme theme) {
        SharedPreferences prefs = BiasRepository.getInstance(context).getPrefs();
        prefs.edit().putString(PREF_ACTIVE_THEME, theme.toJson().toString()).apply();
    }

    public static List<AppTheme> getUserPresets(Context context) {
        List<AppTheme> list = new ArrayList<>();
        SharedPreferences prefs = BiasRepository.getInstance(context).getPrefs();
        String raw = prefs.getString(PREF_USER_PRESETS, "[]");
        try {
            JSONArray arr = new JSONArray(raw);
            for (int i = 0; i < arr.length(); i++) {
                list.add(AppTheme.fromJson(arr.getJSONObject(i)));
            }
        } catch (Exception ignored) {
        }
        return list;
    }

    public static void saveUserPreset(Context context, AppTheme preset) {
        preset.isCustomUserPreset = true;
        List<AppTheme> list = getUserPresets(context);
        boolean replaced = false;
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).id.equals(preset.id)) {
                list.set(i, preset);
                replaced = true;
                break;
            }
        }
        if (!replaced) list.add(0, preset);
        persistUserPresets(context, list);
    }

    public static void deleteUserPreset(Context context, String id) {
        List<AppTheme> list = getUserPresets(context);
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).id.equals(id)) {
                list.remove(i);
                break;
            }
        }
        persistUserPresets(context, list);
    }

    private static void persistUserPresets(Context context, List<AppTheme> list) {
        JSONArray arr = new JSONArray();
        for (AppTheme t : list) arr.put(t.toJson());
        BiasRepository.getInstance(context).getPrefs().edit()
                .putString(PREF_USER_PRESETS, arr.toString()).apply();
    }

    public static boolean isColorDark(int color) {
        double darkness = 1 - (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color)) / 255;
        return darkness >= 0.45;
    }

    public static GradientDrawable createPillDrawable(Context ctx, int fillColor, int radiusDp) {
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(fillColor);
        gd.setCornerRadius(dp(ctx, radiusDp));
        return gd;
    }

    public static GradientDrawable createCardDrawable(Context ctx, int fillColor, int strokeColor, int radiusDp) {
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(fillColor);
        gd.setCornerRadius(dp(ctx, radiusDp));
        gd.setStroke(dp(ctx, 1), strokeColor);
        return gd;
    }

    /**
     * Full Theme Studio Dialog: Choose Built-In Presets, Saved User Presets, or Customize Every Part with a Color Picker!
     */
    public static void showThemeStudioDialog(Activity activity, Runnable onThemeApplied) {
        final AppTheme working = getActiveTheme(activity);
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        ScrollView scroll = new ScrollView(activity);
        LinearLayout root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(activity, 18);
        root.setPadding(pad, pad, pad, pad);
        root.setBackgroundColor(Color.WHITE);

        AlertDialog[] dialogRef = new AlertDialog[1];

        TextView tvTitle = new TextView(activity);
        tvTitle.setText(activity.getString(R.string.theme_studio_title));
        tvTitle.setTextColor(Color.parseColor("#111827"));
        tvTitle.setTextSize(19f);
        tvTitle.setTypeface(Typeface.create("sans-serif-black", Typeface.BOLD));
        root.addView(tvTitle);

        TextView tvSub = new TextView(activity);
        tvSub.setText(activity.getString(R.string.theme_studio_sub));
        tvSub.setTextColor(Color.parseColor("#6B7280"));
        tvSub.setTextSize(12.5f);
        tvSub.setPadding(0, dp(activity, 4), 0, dp(activity, 12));
        root.addView(tvSub);

        // 1. Live Theme Preview Card
        final LinearLayout previewCard = new LinearLayout(activity);
        previewCard.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams pvLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        pvLp.setMargins(0, 0, 0, dp(activity, 14));
        previewCard.setLayoutParams(pvLp);

        final TextView pvHeader = new TextView(activity);
        pvHeader.setTextSize(15f);
        pvHeader.setTypeface(Typeface.DEFAULT_BOLD);
        previewCard.addView(pvHeader);

        final TextView pvButton = new TextView(activity);
        pvButton.setText(activity.getString(R.string.theme_sample_button));
        pvButton.setGravity(Gravity.CENTER);
        pvButton.setTypeface(Typeface.DEFAULT_BOLD);
        pvButton.setTextSize(12.5f);
        LinearLayout.LayoutParams pvbLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        pvbLp.topMargin = dp(activity, 8);
        pvButton.setLayoutParams(pvbLp);
        previewCard.addView(pvButton);

        root.addView(previewCard);

        final LinearLayout partsContainer = new LinearLayout(activity);
        partsContainer.setOrientation(LinearLayout.VERTICAL);

        final Runnable refreshPreviewAndParts = new Runnable() {
            @Override
            public void run() {
                GradientDrawable pvBg = createCardDrawable(activity, working.surfaceColor, working.primaryColor, 18);
                previewCard.setBackground(pvBg);
                previewCard.setPadding(dp(activity, 14), dp(activity, 12), dp(activity, 14), dp(activity, 12));
                pvHeader.setText(activity.getString(R.string.theme_preview_label, working.name));
                pvHeader.setTextColor(working.textColor);

                pvButton.setBackground(createPillDrawable(activity, working.primaryColor, 16));
                pvButton.setPadding(dp(activity, 12), dp(activity, 9), dp(activity, 12), dp(activity, 9));
                pvButton.setTextColor(working.getButtonTextColor(working.primaryColor));

                partsContainer.removeAllViews();
                addPartColorRow(activity, partsContainer, activity.getString(R.string.theme_part_primary), working.primaryColor, newColor -> {
                    working.primaryColor = newColor;
                    working.name = activity.getString(R.string.theme_custom_name);
                    run();
                });
                addPartColorRow(activity, partsContainer, activity.getString(R.string.theme_part_secondary), working.secondaryColor, newColor -> {
                    working.secondaryColor = newColor;
                    working.name = activity.getString(R.string.theme_custom_name);
                    run();
                });
                addPartColorRow(activity, partsContainer, activity.getString(R.string.theme_part_background), working.backgroundColor, newColor -> {
                    working.backgroundColor = newColor;
                    working.name = activity.getString(R.string.theme_custom_name);
                    run();
                });
                addPartColorRow(activity, partsContainer, activity.getString(R.string.theme_part_surface), working.surfaceColor, newColor -> {
                    working.surfaceColor = newColor;
                    working.name = activity.getString(R.string.theme_custom_name);
                    run();
                });
                addPartColorRow(activity, partsContainer, activity.getString(R.string.theme_part_text), working.textColor, newColor -> {
                    working.textColor = newColor;
                    working.name = activity.getString(R.string.theme_custom_name);
                    run();
                });
            }
        };

        // 2. Built-in Presets Section
        TextView tvPresetsHeader = new TextView(activity);
        tvPresetsHeader.setText(activity.getString(R.string.theme_presets_header));
        tvPresetsHeader.setTextColor(Color.parseColor("#111827"));
        tvPresetsHeader.setTextSize(14f);
        tvPresetsHeader.setTypeface(Typeface.DEFAULT_BOLD);
        tvPresetsHeader.setPadding(0, 0, 0, dp(activity, 6));
        root.addView(tvPresetsHeader);

        LinearLayout presetsBox = new LinearLayout(activity);
        presetsBox.setOrientation(LinearLayout.VERTICAL);
        root.addView(presetsBox);

        final Runnable rebuildPresetsList = new Runnable() {
            @Override
            public void run() {
                presetsBox.removeAllViews();
                List<AppTheme> allPresets = new ArrayList<>(getUserPresets(activity));
                allPresets.addAll(getBuiltInPresets());

                for (final AppTheme p : allPresets) {
                    LinearLayout row = new LinearLayout(activity);
                    row.setOrientation(LinearLayout.HORIZONTAL);
                    row.setGravity(Gravity.CENTER_VERTICAL);
                    GradientDrawable rBg = createCardDrawable(activity, p.surfaceColor, p.primaryColor, 14);
                    row.setBackground(rBg);
                    row.setPadding(dp(activity, 12), dp(activity, 9), dp(activity, 12), dp(activity, 9));
                    LinearLayout.LayoutParams rLp = new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                    rLp.setMargins(0, 0, 0, dp(activity, 6));
                    row.setLayoutParams(rLp);

                    // Swatch dots
                    row.addView(makeColorDot(activity, p.primaryColor));
                    row.addView(makeColorDot(activity, p.secondaryColor));
                    row.addView(makeColorDot(activity, p.backgroundColor));

                    TextView tvPName = new TextView(activity);
                    tvPName.setText(p.name + (p.isCustomUserPreset ? " (" + activity.getString(R.string.theme_custom_tag) + ")" : ""));
                    tvPName.setTextColor(p.textColor);
                    tvPName.setTextSize(13f);
                    tvPName.setTypeface(Typeface.DEFAULT_BOLD);
                    tvPName.setPadding(dp(activity, 6), 0, 0, 0);
                    tvPName.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
                    row.addView(tvPName);

                    if (p.isCustomUserPreset) {
                        TextView btnDel = new TextView(activity);
                        btnDel.setText("🗑️");
                        btnDel.setTextSize(13f);
                        btnDel.setPadding(dp(activity, 6), dp(activity, 2), dp(activity, 6), dp(activity, 2));
                        btnDel.setOnClickListener(v -> {
                            deleteUserPreset(activity, p.id);
                            run();
                        });
                        row.addView(btnDel);
                    }

                    row.setOnClickListener(v -> {
                        working.id = p.id;
                        working.name = p.name;
                        working.backgroundColor = p.backgroundColor;
                        working.surfaceColor = p.surfaceColor;
                        working.primaryColor = p.primaryColor;
                        working.secondaryColor = p.secondaryColor;
                        working.textColor = p.textColor;
                        refreshPreviewAndParts.run();
                    });
                    presetsBox.addView(row);
                }
            }
        };
        rebuildPresetsList.run();

        // 3. Individual Part Customization Section
        TextView tvCustomHeader = new TextView(activity);
        tvCustomHeader.setText(activity.getString(R.string.theme_picker_header));
        tvCustomHeader.setTextColor(Color.parseColor("#111827"));
        tvCustomHeader.setTextSize(14f);
        tvCustomHeader.setTypeface(Typeface.DEFAULT_BOLD);
        tvCustomHeader.setPadding(0, dp(activity, 10), 0, dp(activity, 6));
        root.addView(tvCustomHeader);

        root.addView(partsContainer);
        refreshPreviewAndParts.run();

        // 4. Save Custom Preset Button + Apply Theme Button
        LinearLayout actionsRow = new LinearLayout(activity);
        actionsRow.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams actLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        actLp.topMargin = dp(activity, 14);
        actionsRow.setLayoutParams(actLp);

        TextView btnSavePreset = new TextView(activity);
        btnSavePreset.setText(activity.getString(R.string.theme_btn_save_preset));
        btnSavePreset.setTextColor(Color.parseColor("#111827"));
        btnSavePreset.setTypeface(Typeface.DEFAULT_BOLD);
        btnSavePreset.setGravity(Gravity.CENTER);
        btnSavePreset.setTextSize(12.5f);
        btnSavePreset.setBackground(createPillDrawable(activity, Color.parseColor("#F3F4F6"), 16));
        btnSavePreset.setPadding(dp(activity, 12), dp(activity, 12), dp(activity, 12), dp(activity, 12));
        LinearLayout.LayoutParams spLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        spLp.rightMargin = dp(activity, 6);
        btnSavePreset.setLayoutParams(spLp);
        btnSavePreset.setOnClickListener(v -> {
            EditText etName = new EditText(activity);
            etName.setHint(activity.getString(R.string.theme_preset_name_hint));
            etName.setText(working.name);
            new AlertDialog.Builder(activity)
                    .setTitle(activity.getString(R.string.theme_save_dialog_title))
                    .setView(etName)
                    .setPositiveButton(activity.getString(R.string.btn_save), (d, w) -> {
                        String n = etName.getText().toString().trim();
                        if (n.isEmpty()) n = activity.getString(R.string.theme_default_name);
                        AppTheme saved = new AppTheme(
                                "user_" + System.currentTimeMillis(),
                                n,
                                working.backgroundColor,
                                working.surfaceColor,
                                working.primaryColor,
                                working.secondaryColor,
                                working.textColor,
                                true
                        );
                        saveUserPreset(activity, saved);
                        working.id = saved.id;
                        working.name = saved.name;
                        rebuildPresetsList.run();
                        refreshPreviewAndParts.run();
                        Toast.makeText(activity, activity.getString(R.string.theme_saved_toast), Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton(activity.getString(R.string.btn_cancel), null)
                    .show();
        });
        actionsRow.addView(btnSavePreset);

        TextView btnApply = new TextView(activity);
        btnApply.setText(activity.getString(R.string.theme_btn_apply));
        btnApply.setTextColor(Color.parseColor("#111827"));
        btnApply.setTypeface(Typeface.DEFAULT_BOLD);
        btnApply.setGravity(Gravity.CENTER);
        btnApply.setTextSize(13f);
        btnApply.setBackground(createPillDrawable(activity, Color.parseColor("#FFC700"), 16));
        btnApply.setPadding(dp(activity, 12), dp(activity, 12), dp(activity, 12), dp(activity, 12));
        LinearLayout.LayoutParams apLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        apLp.leftMargin = dp(activity, 6);
        btnApply.setLayoutParams(apLp);
        btnApply.setOnClickListener(v -> {
            saveActiveTheme(activity, working);
            if (dialogRef[0] != null) dialogRef[0].dismiss();
            if (onThemeApplied != null) onThemeApplied.run();
            Toast.makeText(activity, activity.getString(R.string.theme_applied_toast, working.name), Toast.LENGTH_SHORT).show();
        });
        actionsRow.addView(btnApply);

        root.addView(actionsRow);
        scroll.addView(root);
        builder.setView(scroll);
        dialogRef[0] = builder.create();
        dialogRef[0].show();
    }

    public interface OnColorPickedListener {
        void onColorPicked(int color);
    }

    private static void addPartColorRow(Activity activity, LinearLayout parent, String label, int currentColor, OnColorPickedListener listener) {
        LinearLayout row = new LinearLayout(activity);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setBackground(createPillDrawable(activity, Color.parseColor("#F8F9FB"), 14));
        row.setPadding(dp(activity, 12), dp(activity, 10), dp(activity, 12), dp(activity, 10));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(activity, 6);
        row.setLayoutParams(lp);

        TextView tv = new TextView(activity);
        tv.setText(label);
        tv.setTextColor(Color.parseColor("#1F2937"));
        tv.setTextSize(12.5f);
        tv.setTypeface(Typeface.DEFAULT_BOLD);
        tv.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        row.addView(tv);

        TextView swatch = new TextView(activity);
        swatch.setText(String.format("#%06X", (0xFFFFFF & currentColor)));
        swatch.setTextSize(11f);
        swatch.setTypeface(Typeface.DEFAULT_BOLD);
        swatch.setTextColor(isColorDark(currentColor) ? Color.WHITE : Color.parseColor("#111827"));
        GradientDrawable swGd = createCardDrawable(activity, currentColor, Color.parseColor("#CBD5E1"), 12);
        swatch.setBackground(swGd);
        swatch.setPadding(dp(activity, 12), dp(activity, 6), dp(activity, 12), dp(activity, 6));
        row.addView(swatch);

        row.setOnClickListener(v -> showInteractiveColorPickerDialog(activity, label, currentColor, listener));
        parent.addView(row);
    }

    /**
     * Interactive Color Picker Dialog with Touch Spectrum Bar, RGB Sliders, Hex Input, and Pastel Swatches!
     */
    public static void showInteractiveColorPickerDialog(Activity activity, String partTitle, int initialColor, OnColorPickedListener listener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        ScrollView scroll = new ScrollView(activity);
        LinearLayout root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(activity, 18);
        root.setPadding(pad, pad, pad, pad);
        root.setBackgroundColor(Color.WHITE);

        final int[] chosenColor = {initialColor};
        final boolean[] updatingHex = {false};

        TextView tvTitle = new TextView(activity);
        tvTitle.setText("🎨 Elegir Color: " + partTitle);
        tvTitle.setTextColor(Color.parseColor("#111827"));
        tvTitle.setTextSize(16f);
        tvTitle.setTypeface(Typeface.DEFAULT_BOLD);
        tvTitle.setPadding(0, 0, 0, dp(activity, 10));
        root.addView(tvTitle);

        // Live Color Preview Banner
        final TextView previewBox = new TextView(activity);
        previewBox.setGravity(Gravity.CENTER);
        previewBox.setTypeface(Typeface.DEFAULT_BOLD);
        previewBox.setTextSize(14f);
        LinearLayout.LayoutParams pbLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(activity, 54));
        pbLp.bottomMargin = dp(activity, 12);
        previewBox.setLayoutParams(pbLp);
        root.addView(previewBox);

        // Hex Input
        final EditText etHex = new EditText(activity);
        etHex.setSingleLine(true);
        etHex.setTextSize(13.5f);
        etHex.setTextColor(Color.parseColor("#111827"));
        etHex.setBackground(createPillDrawable(activity, Color.parseColor("#F3F4F6"), 12));
        etHex.setPadding(dp(activity, 14), dp(activity, 8), dp(activity, 14), dp(activity, 8));
        root.addView(etHex);

        // Interactive Rainbow Hue Touch Bar
        TextView tvHueHint = new TextView(activity);
        tvHueHint.setText(activity.getString(R.string.theme_hue_hint));
        tvHueHint.setTextColor(Color.parseColor("#6B7280"));
        tvHueHint.setTextSize(12f);
        tvHueHint.setPadding(0, dp(activity, 10), 0, dp(activity, 6));
        root.addView(tvHueHint);

        final SeekBar sbR = new SeekBar(activity);
        final SeekBar sbG = new SeekBar(activity);
        final SeekBar sbB = new SeekBar(activity);
        sbR.setMax(255);
        sbG.setMax(255);
        sbB.setMax(255);

        final Runnable syncUiFromColor = () -> {
            int c = chosenColor[0];
            previewBox.setBackground(createCardDrawable(activity, c, Color.parseColor("#CBD5E1"), 14));
            String hex = String.format("#%06X", (0xFFFFFF & c));
            previewBox.setText(hex);
            previewBox.setTextColor(isColorDark(c) ? Color.WHITE : Color.parseColor("#111827"));
            updatingHex[0] = true;
            etHex.setText(hex);
            etHex.setSelection(hex.length());
            sbR.setProgress(Color.red(c));
            sbG.setProgress(Color.green(c));
            sbB.setProgress(Color.blue(c));
            updatingHex[0] = false;
        };

        View hueStrip = new View(activity) {
            private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            @Override
            protected void onDraw(Canvas canvas) {
                super.onDraw(canvas);
                int w = getWidth();
                int h = getHeight();
                int[] rainbow = new int[]{
                        0xFFFF0044, 0xFFFF85A2, 0xFFFF9900, 0xFFFFC700,
                        0xFF34D399, 0xFF00F0FF, 0xFF3B82F6, 0xFFA855F7, 0xFFFF0080
                };
                paint.setShader(new LinearGradient(0, 0, w, 0, rainbow, null, Shader.TileMode.CLAMP));
                canvas.drawRoundRect(0, 0, w, h, dp(activity, 12), dp(activity, 12), paint);
            }
        };
        LinearLayout.LayoutParams hsLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(activity, 38));
        hsLp.bottomMargin = dp(activity, 10);
        hueStrip.setLayoutParams(hsLp);
        hueStrip.setOnTouchListener((v, ev) -> {
            if (ev.getAction() == MotionEvent.ACTION_DOWN || ev.getAction() == MotionEvent.ACTION_MOVE) {
                float frac = Math.max(0f, Math.min(1f, ev.getX() / Math.max(1f, v.getWidth())));
                float[] hsv = new float[]{frac * 360f, 0.55f, 0.98f};
                chosenColor[0] = Color.HSVToColor(hsv);
                syncUiFromColor.run();
                return true;
            }
            return false;
        });
        root.addView(hueStrip);

        // RGB Sliders for fine tuning
        addRgbSliderRow(activity, root, activity.getString(R.string.color_red), sbR, () -> {
            if (!updatingHex[0]) {
                chosenColor[0] = Color.rgb(sbR.getProgress(), sbG.getProgress(), sbB.getProgress());
                syncUiFromColor.run();
            }
        });
        addRgbSliderRow(activity, root, activity.getString(R.string.color_green), sbG, () -> {
            if (!updatingHex[0]) {
                chosenColor[0] = Color.rgb(sbR.getProgress(), sbG.getProgress(), sbB.getProgress());
                syncUiFromColor.run();
            }
        });
        addRgbSliderRow(activity, root, activity.getString(R.string.color_blue), sbB, () -> {
            if (!updatingHex[0]) {
                chosenColor[0] = Color.rgb(sbR.getProgress(), sbG.getProgress(), sbB.getProgress());
                syncUiFromColor.run();
            }
        });

        etHex.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (updatingHex[0]) return;
                String str = s.toString().trim();
                if (str.length() == 7 && str.startsWith("#")) {
                    try {
                        chosenColor[0] = Color.parseColor(str);
                        updatingHex[0] = true;
                        previewBox.setBackground(createCardDrawable(activity, chosenColor[0], Color.parseColor("#CBD5E1"), 14));
                        previewBox.setText(str.toUpperCase());
                        previewBox.setTextColor(isColorDark(chosenColor[0]) ? Color.WHITE : Color.parseColor("#111827"));
                        sbR.setProgress(Color.red(chosenColor[0]));
                        sbG.setProgress(Color.green(chosenColor[0]));
                        sbB.setProgress(Color.blue(chosenColor[0]));
                        updatingHex[0] = false;
                    } catch (Exception ignored) {
                    }
                }
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        // Pastel Swatch Grid
        TextView tvSwatches = new TextView(activity);
        tvSwatches.setText(activity.getString(R.string.theme_swatches_hint));
        tvSwatches.setTextColor(Color.parseColor("#6B7280"));
        tvSwatches.setTextSize(12f);
        tvSwatches.setPadding(0, dp(activity, 8), 0, dp(activity, 6));
        root.addView(tvSwatches);

        String[] pastelHexes = new String[]{
                "#FF85A2", "#FFB3C6", "#FF94B4", "#F43F5E", "#FFC700", "#FDE047",
                "#A855F7", "#C084FC", "#DDD6FE", "#38BDF8", "#7DD3FC", "#34D399",
                "#6EE7B7", "#FB923C", "#FDBA74", "#FFFFFF", "#FFFBFD", "#FFF0F6",
                "#FAF7FF", "#F8F9FB", "#F3E8FF", "#111827", "#1F2937", "#14111D"
        };
        GridLayout grid = new GridLayout(activity);
        grid.setColumnCount(6);
        for (String hx : pastelHexes) {
            final int swColor = Color.parseColor(hx);
            View sw = new View(activity);
            GridLayout.LayoutParams glp = new GridLayout.LayoutParams();
            glp.width = dp(activity, 38);
            glp.height = dp(activity, 38);
            glp.setMargins(dp(activity, 4), dp(activity, 4), dp(activity, 4), dp(activity, 4));
            sw.setLayoutParams(glp);
            sw.setBackground(createCardDrawable(activity, swColor, Color.parseColor("#CBD5E1"), 12));
            sw.setOnClickListener(v -> {
                chosenColor[0] = swColor;
                syncUiFromColor.run();
            });
            grid.addView(sw);
        }
        root.addView(grid);

        syncUiFromColor.run();
        scroll.addView(root);

        builder.setView(scroll);
        builder.setPositiveButton("Elegir este Color", (d, w) -> {
            if (listener != null) listener.onColorPicked(chosenColor[0]);
        });
        builder.setNegativeButton("Cancelar", null);
        builder.show();
    }

    private static void addRgbSliderRow(Activity activity, LinearLayout parent, String label, SeekBar sb, Runnable onChange) {
        LinearLayout row = new LinearLayout(activity);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(activity, 2), 0, dp(activity, 2));

        TextView tv = new TextView(activity);
        tv.setText(label);
        tv.setTextColor(Color.parseColor("#4B5563"));
        tv.setTextSize(11.5f);
        tv.setLayoutParams(new LinearLayout.LayoutParams(dp(activity, 68), ViewGroup.LayoutParams.WRAP_CONTENT));
        row.addView(tv);

        sb.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) onChange.run();
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        row.addView(sb);
        parent.addView(row);
    }

    private static View makeColorDot(Context ctx, int color) {
        View v = new View(ctx);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(ctx, 18), dp(ctx, 18));
        lp.rightMargin = dp(ctx, 4);
        v.setLayoutParams(lp);
        GradientDrawable gd = new GradientDrawable();
        gd.setShape(GradientDrawable.OVAL);
        gd.setColor(color);
        gd.setStroke(dp(ctx, 1), Color.parseColor("#CBD5E1"));
        v.setBackground(gd);
        return v;
    }

    private static int dp(Context ctx, int v) {
        return Math.round(v * ctx.getResources().getDisplayMetrics().density);
    }
}
