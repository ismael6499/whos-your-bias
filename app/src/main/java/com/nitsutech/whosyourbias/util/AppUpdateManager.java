package com.nitsutech.whosyourbias.util;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import com.nitsutech.whosyourbias.R;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Lightweight background updater that checks GitHub Releases
 * (https://api.github.com/repos/ismael6499/whos-your-bias/releases/latest)
 * and downloads + installs the new APK directly from inside the app.
 */
public class AppUpdateManager {

    private static final String GITHUB_LATEST_RELEASE_API = "https://api.github.com/repos/ismael6499/whos-your-bias/releases/latest";
    private static final String PREFS_UPDATE = "wyb_update_prefs";
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());
    private static boolean checkedThisSession = false;
    private static File pendingApkFile = null;

    public static void resumePendingInstallIfAllowed(Activity activity) {
        if (activity == null || pendingApkFile == null || !pendingApkFile.exists()) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (activity.getPackageManager().canRequestPackageInstalls()) {
                File f = pendingApkFile;
                pendingApkFile = null;
                triggerApkInstall(activity, f);
            }
        }
    }

    public static void checkOnAppLaunch(Activity activity) {
        if (checkedThisSession) return;
        checkedThisSession = true;
        checkForUpdates(activity, false);
    }

    public static void checkForUpdates(Activity activity, boolean manualCheck) {
        if (activity == null || activity.isFinishing()) return;
        final Context appCtx = activity.getApplicationContext();

        executor.execute(() -> {
            HttpURLConnection conn = null;
            try {
                String currentVersion = getInstalledVersionName(appCtx);
                URL url = new URL(GITHUB_LATEST_RELEASE_API);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(4000);
                conn.setReadTimeout(4500);
                conn.setRequestProperty("Accept", "application/vnd.github+json");
                conn.setRequestProperty("User-Agent", "WhosYourBias-Android/" + currentVersion);

                int code = conn.getResponseCode();
                if (code != 200) {
                    if (manualCheck) {
                        mainHandler.post(() -> Toast.makeText(activity, activity.getString(R.string.update_check_error) + " (HTTP " + code + ")", Toast.LENGTH_SHORT).show());
                    }
                    return;
                }

                StringBuilder sb = new StringBuilder();
                try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        sb.append(line).append("\n");
                    }
                }

                JSONObject rel = new JSONObject(sb.toString());
                String tagName = rel.optString("tag_name", "").trim();
                String releaseName = rel.optString("name", tagName).trim();
                String body = rel.optString("body", "").trim();

                JSONArray assets = rel.optJSONArray("assets");
                String apkDownloadUrl = "";
                long apkSize = 0;
                if (assets != null) {
                    for (int i = 0; i < assets.length(); i++) {
                        JSONObject asset = assets.optJSONObject(i);
                        if (asset == null) continue;
                        String name = asset.optString("name", "").toLowerCase(Locale.ROOT);
                        String dl = asset.optString("browser_download_url", "");
                        if (name.endsWith(".apk") && !dl.isEmpty()) {
                            apkDownloadUrl = dl;
                            apkSize = asset.optLong("size", 0);
                            break;
                        }
                    }
                }

                if (apkDownloadUrl.isEmpty()) {
                    if (manualCheck) {
                        mainHandler.post(() -> Toast.makeText(activity, activity.getString(R.string.update_already_latest, currentVersion), Toast.LENGTH_SHORT).show());
                    }
                    return;
                }

                boolean isNewer = isNewerVersion(tagName, currentVersion);
                if (!isNewer) {
                    if (manualCheck) {
                        mainHandler.post(() -> Toast.makeText(activity, activity.getString(R.string.update_already_latest, currentVersion), Toast.LENGTH_SHORT).show());
                    }
                    return;
                }

                final String finalTag = tagName;
                final String finalTitle = releaseName.isEmpty() ? tagName : releaseName;
                final String finalNotes = body;
                final String finalApkUrl = apkDownloadUrl;
                final long finalApkSize = apkSize;

                mainHandler.post(() -> {
                    if (!activity.isFinishing()) {
                        showUpdateAvailableDialog(activity, currentVersion, finalTag, finalTitle, finalNotes, finalApkUrl, finalApkSize);
                    }
                });
            } catch (Exception e) {
                if (manualCheck) {
                    mainHandler.post(() -> Toast.makeText(activity, activity.getString(R.string.update_check_error), Toast.LENGTH_SHORT).show());
                }
            } finally {
                if (conn != null) {
                    try { conn.disconnect(); } catch (Exception ignored) {}
                }
            }
        });
    }

    private static String getInstalledVersionName(Context context) {
        try {
            PackageInfo pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            return pInfo.versionName != null ? pInfo.versionName : "1.0.0";
        } catch (Exception e) {
            return "1.0.0";
        }
    }

    public static boolean isNewerVersion(String remoteTag, String localVersion) {
        if (remoteTag == null || remoteTag.isEmpty()) return false;
        String cleanRemote = remoteTag.trim().replaceFirst("^[vV]", "");
        String cleanLocal = (localVersion != null ? localVersion.trim() : "1.0.0").replaceFirst("^[vV]", "");
        if (cleanRemote.equalsIgnoreCase(cleanLocal)) return false;

        String[] rParts = cleanRemote.split("[^0-9]+");
        String[] lParts = cleanLocal.split("[^0-9]+");
        int max = Math.max(rParts.length, lParts.length);
        for (int i = 0; i < max; i++) {
            int r = 0;
            int l = 0;
            try { if (i < rParts.length && !rParts[i].isEmpty()) r = Integer.parseInt(rParts[i]); } catch (Exception ignored) {}
            try { if (i < lParts.length && !lParts[i].isEmpty()) l = Integer.parseInt(lParts[i]); } catch (Exception ignored) {}
            if (r > l) return true;
            if (r < l) return false;
        }
        return false;
    }

    private static void showUpdateAvailableDialog(Activity activity, String currentVer, String tag, String title, String notes, String apkUrl, long apkSize) {
        ThemeManager.AppTheme theme = ThemeManager.getActiveTheme(activity);
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);

        LinearLayout box = new LinearLayout(activity);
        box.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(activity, 20);
        box.setPadding(pad, pad, pad, pad);
        box.setBackground(ThemeManager.createCardDrawable(activity, theme.surfaceColor, theme.cardBorderColor, 26));

        TextView tvBadge = new TextView(activity);
        tvBadge.setText(activity.getString(R.string.update_badge_github));
        tvBadge.setTextColor(theme.getButtonTextColor(theme.primaryColor));
        tvBadge.setTextSize(11f);
        tvBadge.setTypeface(Typeface.DEFAULT_BOLD);
        tvBadge.setPadding(dp(activity, 12), dp(activity, 5), dp(activity, 12), dp(activity, 5));
        tvBadge.setBackground(ThemeManager.createPillDrawable(activity, theme.primaryColor, 14));
        LinearLayout.LayoutParams bLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        bLp.bottomMargin = dp(activity, 10);
        tvBadge.setLayoutParams(bLp);
        box.addView(tvBadge);

        TextView tvTitle = new TextView(activity);
        tvTitle.setText(activity.getString(R.string.update_title_available, tag));
        tvTitle.setTextColor(theme.textColor);
        tvTitle.setTextSize(19f);
        tvTitle.setTypeface(Typeface.create("sans-serif-black", Typeface.BOLD));
        box.addView(tvTitle);

        String sizeMb = apkSize > 0 ? String.format(Locale.ROOT, " • %.1f MB", apkSize / (1024f * 1024f)) : "";
        TextView tvVer = new TextView(activity);
        tvVer.setText(activity.getString(R.string.update_version_compare, currentVer, tag, sizeMb));
        tvVer.setTextColor(theme.primaryColor);
        tvVer.setTextSize(12.5f);
        tvVer.setTypeface(Typeface.DEFAULT_BOLD);
        tvVer.setPadding(0, dp(activity, 4), 0, dp(activity, 10));
        box.addView(tvVer);

        if (notes != null && !notes.isEmpty()) {
            TextView tvNotes = new TextView(activity);
            String cleanNotes = notes.length() > 360 ? notes.substring(0, 360) + "..." : notes;
            tvNotes.setText(cleanNotes);
            tvNotes.setTextColor(theme.getMutedTextColor());
            tvNotes.setTextSize(12.5f);
            tvNotes.setPadding(0, 0, 0, dp(activity, 14));
            box.addView(tvNotes);
        }

        AlertDialog[] ref = new AlertDialog[1];

        TextView btnUpdate = new TextView(activity);
        btnUpdate.setText(activity.getString(R.string.update_btn_download_install));
        btnUpdate.setTextColor(theme.getButtonTextColor(theme.primaryColor));
        btnUpdate.setBackground(ThemeManager.createPillDrawable(activity, theme.primaryColor, 20));
        btnUpdate.setTypeface(Typeface.DEFAULT_BOLD);
        btnUpdate.setTextSize(13.5f);
        btnUpdate.setGravity(Gravity.CENTER);
        btnUpdate.setPadding(dp(activity, 14), dp(activity, 12), dp(activity, 14), dp(activity, 12));
        LinearLayout.LayoutParams uLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        uLp.bottomMargin = dp(activity, 8);
        btnUpdate.setLayoutParams(uLp);
        btnUpdate.setOnClickListener(v -> {
            if (ref[0] != null) ref[0].dismiss();
            downloadAndInstallApk(activity, tag, apkUrl, apkSize);
        });
        box.addView(btnUpdate);

        TextView btnLater = new TextView(activity);
        btnLater.setText(activity.getString(R.string.update_btn_later));
        btnLater.setTextColor(Color.parseColor("#374151"));
        btnLater.setBackground(ThemeManager.createPillDrawable(activity, Color.parseColor("#F3F4F6"), 20));
        btnLater.setTypeface(Typeface.DEFAULT_BOLD);
        btnLater.setTextSize(13f);
        btnLater.setGravity(Gravity.CENTER);
        btnLater.setPadding(dp(activity, 14), dp(activity, 10), dp(activity, 14), dp(activity, 10));
        btnLater.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        btnLater.setOnClickListener(v -> {
            if (ref[0] != null) ref[0].dismiss();
        });
        box.addView(btnLater);

        builder.setView(box);
        ref[0] = builder.create();
        if (ref[0].getWindow() != null) {
            ref[0].getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        ref[0].show();
    }

    private static void downloadAndInstallApk(Activity activity, String tag, String apkUrl, long expectedSize) {
        ThemeManager.AppTheme theme = ThemeManager.getActiveTheme(activity);
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setCancelable(false);

        LinearLayout box = new LinearLayout(activity);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER_HORIZONTAL);
        int pad = dp(activity, 22);
        box.setPadding(pad, pad, pad, pad);
        box.setBackground(ThemeManager.createCardDrawable(activity, theme.surfaceColor, theme.cardBorderColor, 26));

        TextView tvTitle = new TextView(activity);
        tvTitle.setText(activity.getString(R.string.update_downloading_title, tag));
        tvTitle.setTextColor(theme.textColor);
        tvTitle.setTextSize(17f);
        tvTitle.setTypeface(Typeface.DEFAULT_BOLD);
        box.addView(tvTitle);

        ProgressBar pb = new ProgressBar(activity, null, android.R.attr.progressBarStyleHorizontal);
        pb.setMax(100);
        pb.setProgress(0);
        LinearLayout.LayoutParams pbLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(activity, 12));
        pbLp.setMargins(0, dp(activity, 16), 0, dp(activity, 10));
        pb.setLayoutParams(pbLp);
        box.addView(pb);

        TextView tvProgress = new TextView(activity);
        tvProgress.setText(activity.getString(R.string.update_connecting_github));
        tvProgress.setTextColor(theme.getMutedTextColor());
        tvProgress.setTextSize(12.5f);
        box.addView(tvProgress);

        builder.setView(box);
        AlertDialog progressDialog = builder.create();
        if (progressDialog.getWindow() != null) {
            progressDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        progressDialog.show();

        executor.execute(() -> {
            HttpURLConnection conn = null;
            try {
                File updatesDir = new File(activity.getCacheDir(), "updates");
                if (!updatesDir.exists()) updatesDir.mkdirs();
                File apkFile = new File(updatesDir, "WhosYourBias-update.apk");
                if (apkFile.exists()) apkFile.delete();

                String currentUrl = apkUrl;
                for (int redirects = 0; redirects < 5; redirects++) {
                    URL url = new URL(currentUrl);
                    conn = (HttpURLConnection) url.openConnection();
                    conn.setInstanceFollowRedirects(true);
                    conn.setConnectTimeout(10000);
                    conn.setReadTimeout(15000);
                    conn.setRequestProperty("User-Agent", "WhosYourBias-Android-Updater");
                    int code = conn.getResponseCode();
                    if (code == 301 || code == 302 || code == 303 || code == 307 || code == 308) {
                        String loc = conn.getHeaderField("Location");
                        conn.disconnect();
                        if (loc != null && !loc.isEmpty()) {
                            currentUrl = loc;
                            continue;
                        }
                    }
                    break;
                }

                int totalBytes = conn.getContentLength();
                if (totalBytes <= 0 && expectedSize > 0) {
                    totalBytes = (int) expectedSize;
                }
                try (InputStream is = conn.getInputStream();
                     FileOutputStream fos = new FileOutputStream(apkFile)) {
                    byte[] buffer = new byte[16384];
                    int read;
                    long downloaded = 0;
                    long lastUiUpdate = 0;
                    while ((read = is.read(buffer)) != -1) {
                        fos.write(buffer, 0, read);
                        downloaded += read;
                        long now = System.currentTimeMillis();
                        if (now - lastUiUpdate > 80) {
                            lastUiUpdate = now;
                            final int pct = totalBytes > 0 ? (int) ((downloaded * 100L) / totalBytes) : 50;
                            final String dlText = String.format(Locale.ROOT, "%.1f MB / %.1f MB (%d%%)",
                                    downloaded / (1024f * 1024f),
                                    totalBytes > 0 ? totalBytes / (1024f * 1024f) : 6.0f,
                                    pct);
                            mainHandler.post(() -> {
                                pb.setProgress(pct);
                                tvProgress.setText(dlText);
                            });
                        }
                    }
                    fos.flush();
                }

                mainHandler.post(() -> {
                    if (progressDialog.isShowing()) progressDialog.dismiss();
                    triggerApkInstall(activity, apkFile);
                });
            } catch (Exception e) {
                mainHandler.post(() -> {
                    if (progressDialog.isShowing()) progressDialog.dismiss();
                    Toast.makeText(activity, activity.getString(R.string.update_download_error, e.getMessage()), Toast.LENGTH_LONG).show();
                });
            } finally {
                if (conn != null) {
                    try { conn.disconnect(); } catch (Exception ignored) {}
                }
            }
        });
    }

    private static void triggerApkInstall(Activity activity, File apkFile) {
        if (activity == null || !apkFile.exists()) return;
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (!activity.getPackageManager().canRequestPackageInstalls()) {
                    pendingApkFile = apkFile;
                    Toast.makeText(activity, activity.getString(R.string.update_install_perm_toast), Toast.LENGTH_LONG).show();
                    Intent permIntent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                            Uri.parse("package:" + activity.getPackageName()));
                    activity.startActivity(permIntent);
                    return;
                }
            }
            Uri apkUri = FileProvider.getUriForFile(activity, activity.getPackageName() + ".fileprovider", apkFile);
            Intent installIntent = new Intent(Intent.ACTION_VIEW);
            installIntent.setDataAndType(apkUri, "application/vnd.android.package-archive");
            installIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            installIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            activity.startActivity(installIntent);
        } catch (Exception e) {
            Toast.makeText(activity, activity.getString(R.string.update_installer_error, e.getMessage()), Toast.LENGTH_LONG).show();
        }
    }

    private static int dp(Context ctx, int v) {
        return Math.round(v * ctx.getResources().getDisplayMetrics().density);
    }
}
