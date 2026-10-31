package com.nitsutech.whosyourbias.util;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.os.LocaleList;

import com.nitsutech.whosyourbias.R;

import java.util.Locale;

public class LocaleManager {
    public static final String PREF_NAME = "whos_your_bias_prefs";
    public static final String PREF_APP_LANGUAGE = "app_language";

    public static final String LANG_SYSTEM = "system";
    public static final String LANG_EN = "en";
    public static final String LANG_ES = "es";

    public static Context applyLocale(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String lang = prefs.getString(PREF_APP_LANGUAGE, LANG_SYSTEM);

        Locale targetLocale;
        if (LANG_EN.equalsIgnoreCase(lang)) {
            targetLocale = Locale.ENGLISH;
        } else if (LANG_ES.equalsIgnoreCase(lang)) {
            targetLocale = new Locale("es");
        } else {
            // System default: use system's current default locale
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                LocaleList systemLocales = Resources.getSystem().getConfiguration().getLocales();
                targetLocale = (systemLocales != null && !systemLocales.isEmpty())
                        ? systemLocales.get(0)
                        : Locale.getDefault();
            } else {
                targetLocale = Locale.getDefault();
            }
        }

        Locale.setDefault(targetLocale);
        Configuration config = new Configuration(context.getResources().getConfiguration());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.setLocales(new LocaleList(targetLocale));
        } else {
            config.setLocale(targetLocale);
        }
        config.setLayoutDirection(targetLocale);
        return context.createConfigurationContext(config);
    }

    public static String getLanguage(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getString(PREF_APP_LANGUAGE, LANG_SYSTEM);
    }

    public static void setLanguage(Context context, String langCode) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(PREF_APP_LANGUAGE, langCode).apply();
    }

    public static void showLanguageDialog(Activity activity, Runnable onUpdated) {
        String current = getLanguage(activity);
        String[] options = new String[]{
                activity.getString(R.string.lang_system),
                activity.getString(R.string.lang_english),
                activity.getString(R.string.lang_spanish)
        };

        int selectedIndex = 0;
        if (LANG_EN.equalsIgnoreCase(current)) selectedIndex = 1;
        else if (LANG_ES.equalsIgnoreCase(current)) selectedIndex = 2;

        new AlertDialog.Builder(activity)
                .setTitle(activity.getString(R.string.dialog_language_title))
                .setSingleChoiceItems(options, selectedIndex, (dialog, which) -> {
                    String chosen = LANG_SYSTEM;
                    if (which == 1) chosen = LANG_EN;
                    else if (which == 2) chosen = LANG_ES;

                    setLanguage(activity, chosen);
                    applyLocale(activity);
                    dialog.dismiss();
                    if (onUpdated != null) {
                        onUpdated.run();
                    } else {
                        activity.recreate();
                    }
                })
                .setNegativeButton(activity.getString(R.string.btn_cancel), null)
                .show();
    }
}
