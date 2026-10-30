package com.nitsutech.whosyourbias.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;

import com.nitsutech.whosyourbias.R;
import com.nitsutech.whosyourbias.data.BiasRepository;
import com.nitsutech.whosyourbias.util.ThemeManager;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class YoutubeBgmController {
    private static final String PREF_BGM_URL = "bgm_stream_url_v2";
    private static final String PREF_BGM_TITLE = "bgm_stream_title_v2";
    private static final String PREF_BGM_AUTOPLAY = "bgm_autoplay_v2";
    private static final String PREF_SAVED_SONGS = "bgm_saved_songs_v2";

    private static final String NOTIF_CHANNEL_ID = "whos_your_bias_music_channel";
    private static final int NOTIF_ID = 7701;

    // Verified high-speed studio audio streams (.m4a)
    public static final String[][] PRESETS = new String[][]{
            {"✨ ILLIT - It's Me", "https://audio-ssl.itunes.apple.com/itunes-assets/AudioPreview221/v4/08/ae/11/08ae11aa-40b1-231d-4013-12ca6de70701/mzaf_12606446026212796138.plus.aac.p.m4a"},
            {"🍬 ILLIT - Jellyous", "https://audio-ssl.itunes.apple.com/itunes-assets/AudioPreview211/v4/13/13/20/13132098-7412-2b3b-f54b-5f9515c4a075/mzaf_12702295383222688240.plus.aac.p.m4a"},
            {"🎵 ILLIT - Tick-Tack", "https://audio-ssl.itunes.apple.com/itunes-assets/AudioPreview221/v4/68/e9/78/68e978c1-6d32-713d-c91b-836669da6ee3/mzaf_14472264782207748518.plus.aac.p.m4a"},
            {"🧲 ILLIT - Magnetic", "https://audio-ssl.itunes.apple.com/itunes-assets/AudioPreview211/v4/e8/7e/67/e87e6795-4ada-2f65-d562-1fd2a3eddfdf/mzaf_11263458879279601822.plus.aac.p.m4a"},
            {"🍒 ILLIT - Cherish (My Love)", "https://audio-ssl.itunes.apple.com/itunes-assets/AudioPreview221/v4/ff/c5/9d/ffc59d63-bd5d-0b93-4592-2dd7d797de8e/mzaf_7002868842246643213.plus.aac.p.m4a"},
            {"✨ ILLIT - IYKYK (If You Know You Know)", "https://audio-ssl.itunes.apple.com/itunes-assets/AudioPreview221/v4/eb/4a/ea/eb4aea2f-9b99-0c18-7f4e-5c9930a3d4b7/mzaf_17432026377570440264.plus.aac.p.m4a"},
            {"🍀 ILLIT - Lucky Girl Syndrome", "https://audio-ssl.itunes.apple.com/itunes-assets/AudioPreview221/v4/09/d9/7e/09d97e5a-d322-77f1-824f-17631d8c350b/mzaf_2514770446263700121.plus.aac.p.m4a"},
            {"💖 RESCENE - LOVE ATTACK", "https://audio-ssl.itunes.apple.com/itunes-assets/AudioPreview211/v4/6a/c5/ec/6ac5ecf5-6e26-e551-0b1d-d9f2fcb253a2/mzaf_18228858557779313654.plus.aac.p.m4a"},
            {"🌟 KATSEYE - Touch", "https://audio-ssl.itunes.apple.com/itunes-assets/AudioPreview211/v4/9f/1d/86/9f1d8632-0842-ba50-107d-1f3bc5b7b362/mzaf_6667750882234216216.plus.aac.p.m4a"},
            {"🐱 MEOVV - MEOW", "https://audio-ssl.itunes.apple.com/itunes-assets/AudioPreview221/v4/66/cc/27/66cc271e-8a4f-61d8-f60a-8950c8c7b9fa/mzaf_3451951594926395113.plus.aac.p.m4a"},
            {"🌿 KiiiKiii - I DO ME", "https://audio-ssl.itunes.apple.com/itunes-assets/AudioPreview221/v4/8e/57/4e/8e574e26-8147-8430-6ace-93a8eb7a01d6/mzaf_5228116227050500579.plus.aac.p.m4a"},
            {"⚡ aespa - Supernova", "https://audio-ssl.itunes.apple.com/itunes-assets/AudioPreview211/v4/f6/cc/58/f6cc5863-c60a-caac-e7f8-bd66b414a917/mzaf_8631809138360097160.plus.aac.p.m4a"},
            {"🔥 Stray Kids - Chk Chk Boom", "https://audio-ssl.itunes.apple.com/itunes-assets/AudioPreview221/v4/36/77/87/367787a9-a2f2-0684-6611-5a78d855782d/mzaf_1227971913921381507.plus.aac.p.m4a"},
            {"🛹 NEXZ - Ride the Vibe", "https://audio-ssl.itunes.apple.com/itunes-assets/AudioPreview221/v4/1d/90/b4/1d90b4a1-39f0-2cb7-e10b-4c7fdbc1453f/mzaf_7679262427092888355.plus.aac.p.m4a"},
            {"🐰 NewJeans - Super Shy", "https://audio-ssl.itunes.apple.com/itunes-assets/AudioPreview221/v4/a0/0c/47/a00c4790-3bbe-c669-fc8b-d8779508b512/mzaf_4170831274187670095.plus.aac.p.m4a"},
            {"⚡ LE SSERAFIM - CRAZY", "https://audio-ssl.itunes.apple.com/itunes-assets/AudioPreview211/v4/46/0b/ae/460bae7f-536e-9df1-8aa4-ef1ed6d3b314/mzaf_17541894071969246196.plus.aac.p.m4a"},
            {"🧛 ENHYPEN - XO (Only If You Say Yes)", "https://audio-ssl.itunes.apple.com/itunes-assets/AudioPreview221/v4/03/67/e1/0367e19c-00be-22c0-77e0-50395fa347fa/mzaf_5914760408694089352.plus.aac.p.m4a"},
            {"👑 BLACKPINK - Shut Down", "https://audio-ssl.itunes.apple.com/itunes-assets/AudioPreview211/v4/c3/15/06/c31506be-4ea9-81b8-4f7f-803ee7305721/mzaf_5655789092813624412.plus.aac.p.m4a"},
            {"😈 BABYMONSTER - DRIP", "https://audio-ssl.itunes.apple.com/itunes-assets/AudioPreview221/v4/11/49/8d/11498d26-33f8-d33e-9641-0931f48c1530/mzaf_16154189721053536181.plus.aac.p.m4a"}
    };

    private static MediaPlayer sharedPlayer;
    private static String activeStreamUrl = "";
    private static String activeSongTitle = PRESETS[0][0];
    private static boolean isPrepared = false;
    private static boolean isPlaying = false;
    private static boolean isMuted = false;
    private static WeakReference<YoutubeBgmController> activeControllerRef;

    private static final ExecutorService searchExecutor = Executors.newSingleThreadExecutor();
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    private final Activity activity;
    private final TextView tvNowPlaying;
    private final TextView btnPlayPause;
    private final TextView btnMute;
    private final SharedPreferences prefs;

    public YoutubeBgmController(Activity activity, WebView unusedWebView, View videoContainer,
                                TextView tvNowPlaying, TextView btnPlayPause, TextView btnMute,
                                TextView btnVideoToggle, TextView btnChangeSong) {
        this.activity = activity;
        this.tvNowPlaying = tvNowPlaying;
        this.btnPlayPause = btnPlayPause;
        this.btnMute = btnMute;
        this.prefs = BiasRepository.getInstance(activity).getPrefs();
        activeControllerRef = new WeakReference<>(this);

        if (videoContainer != null) videoContainer.setVisibility(View.GONE);
        if (btnVideoToggle != null) btnVideoToggle.setVisibility(View.GONE);

        String savedUrl = prefs.getString(PREF_BGM_URL, PRESETS[0][1]);
        String savedTitle = prefs.getString(PREF_BGM_TITLE, PRESETS[0][0]);
        boolean shouldAutoPlay = prefs.getBoolean(PREF_BGM_AUTOPLAY, true);

        this.tvNowPlaying.setText(savedTitle);

        this.btnPlayPause.setOnClickListener(v -> togglePlayPause());
        this.btnMute.setOnClickListener(v -> toggleMute());
        if (btnChangeSong != null) {
            btnChangeSong.setOnClickListener(v -> showSongSelectorDialog());
        }
        this.tvNowPlaying.setOnClickListener(v -> showSongSelectorDialog());

        if (sharedPlayer != null && isPrepared && savedUrl.equals(activeStreamUrl)) {
            updateButtonsUi();
        } else {
            startStream(savedTitle, savedUrl, shouldAutoPlay);
        }
    }

    private void startStream(String title, String streamUrl, boolean autoPlay) {
        activeSongTitle = title;
        activeStreamUrl = streamUrl;
        isPrepared = false;
        if (tvNowPlaying != null) tvNowPlaying.setText(title);

        try {
            if (sharedPlayer == null) {
                sharedPlayer = new MediaPlayer();
            } else {
                sharedPlayer.reset();
            }
            sharedPlayer.setAudioAttributes(new AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build());
            sharedPlayer.setDataSource(streamUrl);
            sharedPlayer.setLooping(true);
            float vol = isMuted ? 0f : 1f;
            sharedPlayer.setVolume(vol, vol);
            sharedPlayer.setOnPreparedListener(mp -> {
                isPrepared = true;
                if (autoPlay) {
                    mp.start();
                    isPlaying = true;
                    showOrUpdateMusicNotification(activity.getApplicationContext());
                } else {
                    isPlaying = false;
                }
                updateButtonsUi();
            });
            sharedPlayer.setOnErrorListener((mp, what, extra) -> {
                isPrepared = false;
                isPlaying = false;
                updateButtonsUi();
                return true;
            });
            sharedPlayer.prepareAsync();
        } catch (Exception e) {
            isPlaying = false;
            updateButtonsUi();
        }
    }

    public void togglePlayPause() {
        if (sharedPlayer == null || !isPrepared) {
            String url = prefs.getString(PREF_BGM_URL, PRESETS[0][1]);
            String title = prefs.getString(PREF_BGM_TITLE, PRESETS[0][0]);
            prefs.edit().putBoolean(PREF_BGM_AUTOPLAY, true).apply();
            startStream(title, url, true);
            return;
        }
        try {
            if (sharedPlayer.isPlaying()) {
                sharedPlayer.pause();
                isPlaying = false;
                prefs.edit().putBoolean(PREF_BGM_AUTOPLAY, false).apply();
            } else {
                sharedPlayer.start();
                isPlaying = true;
                prefs.edit().putBoolean(PREF_BGM_AUTOPLAY, true).apply();
            }
        } catch (Exception ignored) {
        }
        updateButtonsUi();
        showOrUpdateMusicNotification(activity.getApplicationContext());
    }

    public void toggleMute() {
        isMuted = !isMuted;
        if (sharedPlayer != null && isPrepared) {
            float vol = isMuted ? 0f : 1f;
            sharedPlayer.setVolume(vol, vol);
        }
        updateButtonsUi();
    }

    public void updateButtonsUi() {
        ThemeManager.AppTheme theme = ThemeManager.getActiveTheme(activity);
        if (btnPlayPause != null) {
            btnPlayPause.setText(isPlaying ? "⏸" : "▶");
            btnPlayPause.setBackground(ThemeManager.createPillDrawable(activity, theme.primaryColor, 14));
            btnPlayPause.setTextColor(theme.getButtonTextColor(theme.primaryColor));
        }
        if (btnMute != null) {
            btnMute.setText(isMuted ? "🔇" : "🔊");
        }
        if (tvNowPlaying != null && activeSongTitle != null && !activeSongTitle.isEmpty()) {
            tvNowPlaying.setText(activeSongTitle);
        }
    }

    public static void handleNotificationToggle(Context context) {
        if (sharedPlayer != null && isPrepared) {
            try {
                if (sharedPlayer.isPlaying()) {
                    sharedPlayer.pause();
                    isPlaying = false;
                } else {
                    sharedPlayer.start();
                    isPlaying = true;
                }
                BiasRepository.getInstance(context).getPrefs().edit().putBoolean(PREF_BGM_AUTOPLAY, isPlaying).apply();
                showOrUpdateMusicNotification(context);
                if (activeControllerRef != null && activeControllerRef.get() != null) {
                    mainHandler.post(() -> {
                        YoutubeBgmController c = activeControllerRef.get();
                        if (c != null) c.updateButtonsUi();
                    });
                }
            } catch (Exception ignored) {
            }
        }
    }

    public static void handleNotificationStop(Context context) {
        if (sharedPlayer != null && isPrepared) {
            try {
                if (sharedPlayer.isPlaying()) sharedPlayer.pause();
                isPlaying = false;
                BiasRepository.getInstance(context).getPrefs().edit().putBoolean(PREF_BGM_AUTOPLAY, false).apply();
            } catch (Exception ignored) {
            }
        }
        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) nm.cancel(NOTIF_ID);
        if (activeControllerRef != null && activeControllerRef.get() != null) {
            mainHandler.post(() -> {
                YoutubeBgmController c = activeControllerRef.get();
                if (c != null) c.updateButtonsUi();
            });
        }
    }

    public static void showOrUpdateMusicNotification(Context context) {
        try {
            NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm == null) return;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationChannel channel = new NotificationChannel(
                        NOTIF_CHANNEL_ID,
                        "Música de Fondo (Who's your Bias)",
                        NotificationManager.IMPORTANCE_LOW);
                channel.setDescription("Control rápido para pausar o reproducir la canción actual");
                channel.setShowBadge(false);
                nm.createNotificationChannel(channel);
            }

            Intent openIntent = new Intent(context, MainActivity.class);
            openIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            int flags = PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0);
            PendingIntent contentPi = PendingIntent.getActivity(context, 10, openIntent, flags);

            Intent toggleIntent = new Intent(context, BgmNotificationReceiver.class);
            toggleIntent.setAction(BgmNotificationReceiver.ACTION_TOGGLE_PLAY);
            PendingIntent togglePi = PendingIntent.getBroadcast(context, 11, toggleIntent, flags);

            Intent stopIntent = new Intent(context, BgmNotificationReceiver.class);
            stopIntent.setAction(BgmNotificationReceiver.ACTION_STOP_BGM);
            PendingIntent stopPi = PendingIntent.getBroadcast(context, 12, stopIntent, flags);

            NotificationCompat.Builder nb = new NotificationCompat.Builder(context, NOTIF_CHANNEL_ID)
                    .setSmallIcon(android.R.drawable.ic_media_play)
                    .setContentTitle(activeSongTitle != null ? activeSongTitle : "Who's your Bias")
                    .setContentText(isPlaying ? "🎵 Reproduciendo • Tocá Pausar para silenciar" : "⏸ En pausa • Tocá Reproducir para continuar")
                    .setContentIntent(contentPi)
                    .setOngoing(isPlaying)
                    .setOnlyAlertOnce(true)
                    .setPriority(NotificationCompat.PRIORITY_LOW)
                    .addAction(isPlaying ? android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play,
                            isPlaying ? "⏸ Pausar" : "▶ Reproducir", togglePi)
                    .addAction(android.R.drawable.ic_menu_close_clear_cancel, "⏹ Cerrar", stopPi);

            nm.notify(NOTIF_ID, nb.build());
        } catch (Exception ignored) {
        }
    }

    private List<String[]> getSavedCustomSongs() {
        List<String[]> list = new ArrayList<>();
        String raw = prefs.getString(PREF_SAVED_SONGS, "[]");
        try {
            JSONArray arr = new JSONArray(raw);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                list.add(new String[]{o.optString("title"), o.optString("url")});
            }
        } catch (Exception ignored) {
        }
        return list;
    }

    private void saveCustomSong(String title, String url) {
        List<String[]> list = getSavedCustomSongs();
        for (String[] item : list) {
            if (item[1].equals(url) || item[0].equalsIgnoreCase(title)) {
                return;
            }
        }
        list.add(0, new String[]{title, url});
        persistCustomSongs(list);
    }

    private void removeCustomSong(String url) {
        List<String[]> list = getSavedCustomSongs();
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i)[1].equals(url)) {
                list.remove(i);
                break;
            }
        }
        persistCustomSongs(list);
    }

    private void persistCustomSongs(List<String[]> list) {
        JSONArray arr = new JSONArray();
        try {
            for (String[] item : list) {
                JSONObject o = new JSONObject();
                o.put("title", item[0]);
                o.put("url", item[1]);
                arr.put(o);
            }
        } catch (Exception ignored) {
        }
        prefs.edit().putString(PREF_SAVED_SONGS, arr.toString()).apply();
    }

    public void showSongSelectorDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);

        LinearLayout root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(18);
        root.setPadding(pad, pad, pad, pad);
        root.setBackgroundColor(Color.WHITE);

        TextView header = new TextView(activity);
        header.setText("🎵 Elegir o Buscar Canción de Fondo");
        header.setTextColor(Color.parseColor("#111827"));
        header.setTextSize(17f);
        header.setTypeface(Typeface.DEFAULT_BOLD);
        header.setPadding(0, 0, 0, dp(10));
        root.addView(header);

        // Live Song Search Bar
        LinearLayout searchRow = new LinearLayout(activity);
        searchRow.setOrientation(LinearLayout.HORIZONTAL);
        searchRow.setGravity(Gravity.CENTER_VERTICAL);

        EditText etSearch = new EditText(activity);
        etSearch.setHint("🔍 Buscar canción (ej: NEXZ, TWICE, Jennie)...");
        etSearch.setTextColor(Color.parseColor("#111827"));
        etSearch.setHintTextColor(Color.parseColor("#9CA3AF"));
        etSearch.setTextSize(13f);
        etSearch.setSingleLine(true);
        etSearch.setBackgroundResource(R.drawable.bg_chip_tag);
        etSearch.setPadding(dp(12), dp(10), dp(12), dp(10));
        LinearLayout.LayoutParams etLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        etLp.setMargins(0, 0, dp(8), 0);
        etSearch.setLayoutParams(etLp);
        searchRow.addView(etSearch);

        TextView btnSearch = new TextView(activity);
        btnSearch.setText("Buscar");
        btnSearch.setTextColor(Color.parseColor("#111827"));
        btnSearch.setTypeface(Typeface.DEFAULT_BOLD);
        btnSearch.setTextSize(12.5f);
        btnSearch.setGravity(Gravity.CENTER);
        btnSearch.setBackgroundResource(R.drawable.bg_btn_primary);
        btnSearch.setPadding(dp(14), dp(10), dp(14), dp(10));
        searchRow.addView(btnSearch);

        root.addView(searchRow);

        ScrollView scroll = new ScrollView(activity);
        LinearLayout.LayoutParams scLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(360));
        scLp.setMargins(0, dp(12), 0, 0);
        scroll.setLayoutParams(scLp);

        LinearLayout listContainer = new LinearLayout(activity);
        listContainer.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(listContainer);
        root.addView(scroll);

        AlertDialog[] dialogRef = new AlertDialog[1];

        final Runnable populateSavedAndPresets = new Runnable() {
            @Override
            public void run() {
                listContainer.removeAllViews();
                List<String[]> saved = getSavedCustomSongs();
                if (!saved.isEmpty()) {
                    TextView tvSavedSec = new TextView(activity);
                    tvSavedSec.setText("⭐ Mis Canciones Guardadas");
                    tvSavedSec.setTextColor(Color.parseColor("#EC4899"));
                    tvSavedSec.setTextSize(12.5f);
                    tvSavedSec.setTypeface(Typeface.DEFAULT_BOLD);
                    tvSavedSec.setPadding(dp(4), dp(4), dp(4), dp(6));
                    listContainer.addView(tvSavedSec);

                    for (String[] s : saved) {
                        addSongRow(listContainer, s[0], s[1], dialogRef, false, true, this);
                    }

                    TextView tvPresetsSec = new TextView(activity);
                    tvPresetsSec.setText("🎵 Canciones Recomendadas K-Pop");
                    tvPresetsSec.setTextColor(Color.parseColor("#6B7280"));
                    tvPresetsSec.setTextSize(12f);
                    tvPresetsSec.setTypeface(Typeface.DEFAULT_BOLD);
                    tvPresetsSec.setPadding(dp(4), dp(10), dp(4), dp(6));
                    listContainer.addView(tvPresetsSec);
                }

                for (String[] preset : PRESETS) {
                    addSongRow(listContainer, preset[0], preset[1], dialogRef, false, false, this);
                }
            }
        };
        populateSavedAndPresets.run();

        btnSearch.setOnClickListener(v -> {
            String q = etSearch.getText().toString().trim();
            if (q.isEmpty()) {
                populateSavedAndPresets.run();
                return;
            }
            btnSearch.setText("✨...");
            searchExecutor.execute(() -> {
                try {
                    String url = "https://itunes.apple.com/search?term=" + URLEncoder.encode(q, "UTF-8") + "&entity=song&limit=10";
                    HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
                    conn.setConnectTimeout(4500);
                    conn.setReadTimeout(4500);
                    StringBuilder sb = new StringBuilder();
                    try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                        String line;
                        while ((line = br.readLine()) != null) sb.append(line);
                    }
                    JSONObject json = new JSONObject(sb.toString());
                    JSONArray results = json.optJSONArray("results");
                    mainHandler.post(() -> {
                        btnSearch.setText("Buscar");
                        listContainer.removeAllViews();

                        TextView btnBackToList = new TextView(activity);
                        btnBackToList.setText("← Volver a mi lista guardada");
                        btnBackToList.setTextColor(Color.parseColor("#EC4899"));
                        btnBackToList.setTextSize(12.5f);
                        btnBackToList.setTypeface(Typeface.DEFAULT_BOLD);
                        btnBackToList.setPadding(dp(6), dp(4), dp(6), dp(10));
                        btnBackToList.setOnClickListener(v2 -> populateSavedAndPresets.run());
                        listContainer.addView(btnBackToList);

                        if (results == null || results.length() == 0) {
                            Toast.makeText(activity, "No se encontraron canciones para: " + q, Toast.LENGTH_SHORT).show();
                            populateSavedAndPresets.run();
                            return;
                        }
                        for (int i = 0; i < results.length(); i++) {
                            JSONObject item = results.optJSONObject(i);
                            if (item != null) {
                                String artist = item.optString("artistName", "");
                                String track = item.optString("trackName", "");
                                String preview = item.optString("previewUrl", "");
                                if (!preview.isEmpty()) {
                                    addSongRow(listContainer, "🎵 " + artist + " - " + track, preview, dialogRef, true, false, populateSavedAndPresets);
                                }
                            }
                        }
                    });
                } catch (Exception e) {
                    mainHandler.post(() -> {
                        btnSearch.setText("Buscar");
                        Toast.makeText(activity, "Error buscando canción", Toast.LENGTH_SHORT).show();
                    });
                }
            });
        });

        builder.setView(root);
        builder.setNegativeButton(R.string.btn_cancel, null);
        dialogRef[0] = builder.create();
        dialogRef[0].show();
    }

    private void addSongRow(LinearLayout parent, String label, String streamUrl, AlertDialog[] dialogRef,
                            boolean canSaveToList, boolean canDeleteFromSaved, Runnable refreshListCallback) {
        LinearLayout row = new LinearLayout(activity);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setBackgroundResource(R.drawable.bg_chip_tag);
        row.setPadding(dp(12), dp(10), dp(10), dp(10));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, dp(8));
        row.setLayoutParams(lp);

        TextView tvTitle = new TextView(activity);
        tvTitle.setText(label);
        tvTitle.setTextColor(Color.parseColor("#111827"));
        tvTitle.setTextSize(13f);
        tvTitle.setTypeface(Typeface.DEFAULT_BOLD);
        tvTitle.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        tvTitle.setOnClickListener(v -> {
            prefs.edit()
                    .putString(PREF_BGM_TITLE, label)
                    .putString(PREF_BGM_URL, streamUrl)
                    .putBoolean(PREF_BGM_AUTOPLAY, true)
                    .apply();
            startStream(label, streamUrl, true);
            if (dialogRef[0] != null) dialogRef[0].dismiss();
        });
        row.addView(tvTitle);

        if (canSaveToList) {
            final TextView btnSave = new TextView(activity);
            btnSave.setText("⭐ Guardar");
            btnSave.setTextColor(Color.parseColor("#111827"));
            btnSave.setTextSize(11f);
            btnSave.setTypeface(Typeface.DEFAULT_BOLD);
            btnSave.setGravity(Gravity.CENTER);
            btnSave.setBackgroundResource(R.drawable.bg_btn_primary);
            btnSave.setPadding(dp(10), dp(6), dp(10), dp(6));
            btnSave.setOnClickListener(v -> {
                saveCustomSong(label, streamUrl);
                btnSave.setText("✅ Guardada");
                Toast.makeText(activity, "⭐ Canción agregada a tu lista guardada", Toast.LENGTH_SHORT).show();
            });
            row.addView(btnSave);
        } else if (canDeleteFromSaved) {
            TextView btnDel = new TextView(activity);
            btnDel.setText("🗑️");
            btnDel.setTextSize(13f);
            btnDel.setPadding(dp(8), dp(4), dp(6), dp(4));
            btnDel.setOnClickListener(v -> {
                removeCustomSong(streamUrl);
                if (refreshListCallback != null) refreshListCallback.run();
            });
            row.addView(btnDel);
        }

        parent.addView(row);
    }

    public void pause() {
    }

    public void resume() {
        updateButtonsUi();
    }

    public void destroy() {
    }

    private int dp(int val) {
        return Math.round(val * activity.getResources().getDisplayMetrics().density);
    }
}
