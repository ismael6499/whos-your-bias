package com.nitsutech.whosyourbias.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
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

import com.nitsutech.whosyourbias.R;
import com.nitsutech.whosyourbias.data.BiasRepository;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class YoutubeBgmController {
    private static final String PREF_BGM_URL = "bgm_stream_url_v2";
    private static final String PREF_BGM_TITLE = "bgm_stream_title_v2";
    private static final String PREF_BGM_AUTOPLAY = "bgm_autoplay_v2";

    // Verified high-speed studio audio streams (.m4a)
    public static final String[][] PRESETS = new String[][]{
            {"🍬 ILLIT - Jellyous", "https://audio-ssl.itunes.apple.com/itunes-assets/AudioPreview211/v4/13/13/20/13132098-7412-2b3b-f54b-5f9515c4a075/mzaf_12702295383222688240.plus.aac.p.m4a"},
            {"🎵 ILLIT - Tick-Tack (It's Me Theme)", "https://audio-ssl.itunes.apple.com/itunes-assets/AudioPreview221/v4/68/e9/78/68e978c1-6d32-713d-c91b-836669da6ee3/mzaf_14472264782207748518.plus.aac.p.m4a"},
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
            {"👑 BLACKPINK - Shut Down", "https://audio-ssl.itunes.apple.com/itunes-assets/AudioPreview221/v4/c3/15/06/c31506be-4ea9-81b8-4f7f-803ee7305721/mzaf_5655789092813624412.plus.aac.p.m4a"},
            {"😈 BABYMONSTER - DRIP", "https://audio-ssl.itunes.apple.com/itunes-assets/AudioPreview221/v4/11/49/8d/11498d26-33f8-d33e-9641-0931f48c1530/mzaf_16154189721053536181.plus.aac.p.m4a"}
    };

    // Shared static MediaPlayer so music continues seamlessly between MainActivity and MatchActivity!
    private static MediaPlayer sharedPlayer;
    private static String activeStreamUrl = "";
    private static String activeTitle = PRESETS[0][0];
    private static boolean isPlaying = false;
    private static boolean isMuted = false;
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    private final Activity activity;
    private final TextView titleLabel;
    private final TextView btnPlayPause;
    private final TextView btnMute;

    public YoutubeBgmController(Activity activity, WebView unusedWebView, View unusedVideoContainer,
                                TextView titleLabel, TextView btnPlayPause, TextView btnMute,
                                TextView btnToggleVideo, TextView btnChangeSong) {
        this.activity = activity;
        this.titleLabel = titleLabel;
        this.btnPlayPause = btnPlayPause;
        this.btnMute = btnMute;

        if (unusedVideoContainer != null) unusedVideoContainer.setVisibility(View.GONE);
        if (btnToggleVideo != null) btnToggleVideo.setVisibility(View.GONE);

        SharedPreferences prefs = BiasRepository.getInstance(activity).getPrefs();
        String savedUrl = prefs.getString(PREF_BGM_URL, PRESETS[0][1]);
        String savedTitle = prefs.getString(PREF_BGM_TITLE, PRESETS[0][0]);

        if (activeStreamUrl.isEmpty()) {
            activeStreamUrl = savedUrl;
            activeTitle = savedTitle;
        }

        titleLabel.setText(activeTitle);
        updateButtonsUi();

        btnPlayPause.setOnClickListener(v -> togglePlayPause());
        btnMute.setOnClickListener(v -> toggleMute());
        btnChangeSong.setOnClickListener(v -> showSongPickerDialog());
        titleLabel.setOnClickListener(v -> showSongPickerDialog());

        boolean auto = prefs.getBoolean(PREF_BGM_AUTOPLAY, true);
        if (auto && sharedPlayer == null) {
            loadAndPlay(activeStreamUrl, activeTitle, true);
        }
    }

    public synchronized void loadAndPlay(String streamUrl, String title, boolean autoStart) {
        activeStreamUrl = streamUrl;
        activeTitle = title;
        titleLabel.setText(title);

        SharedPreferences prefs = BiasRepository.getInstance(activity).getPrefs();
        prefs.edit()
                .putString(PREF_BGM_URL, activeStreamUrl)
                .putString(PREF_BGM_TITLE, activeTitle)
                .putBoolean(PREF_BGM_AUTOPLAY, autoStart)
                .apply();

        if (!autoStart) {
            stopCurrentPlayer();
            isPlaying = false;
            updateButtonsUi();
            return;
        }

        stopCurrentPlayer();
        isPlaying = true;
        updateButtonsUi();

        executor.execute(() -> {
            try {
                MediaPlayer mp = new MediaPlayer();
                mp.setAudioAttributes(new AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build());
                mp.setDataSource(streamUrl);
                mp.setLooping(true);
                float vol = isMuted ? 0f : 1f;
                mp.setVolume(vol, vol);
                mp.setOnPreparedListener(player -> {
                    sharedPlayer = player;
                    if (isPlaying) {
                        player.start();
                    }
                    mainHandler.post(this::updateButtonsUi);
                });
                mp.setOnErrorListener((player, what, extra) -> {
                    isPlaying = false;
                    mainHandler.post(this::updateButtonsUi);
                    return true;
                });
                mp.prepareAsync();
            } catch (Exception e) {
                isPlaying = false;
                mainHandler.post(this::updateButtonsUi);
            }
        });
    }

    private static synchronized void stopCurrentPlayer() {
        if (sharedPlayer != null) {
            try {
                if (sharedPlayer.isPlaying()) sharedPlayer.stop();
                sharedPlayer.release();
            } catch (Exception ignored) {
            }
            sharedPlayer = null;
        }
    }

    public void togglePlayPause() {
        SharedPreferences prefs = BiasRepository.getInstance(activity).getPrefs();
        if (sharedPlayer != null) {
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
                updateButtonsUi();
                return;
            } catch (Exception ignored) {
            }
        }
        loadAndPlay(activeStreamUrl, activeTitle, !isPlaying);
    }

    public void toggleMute() {
        isMuted = !isMuted;
        if (sharedPlayer != null) {
            try {
                float vol = isMuted ? 0f : 1f;
                sharedPlayer.setVolume(vol, vol);
            } catch (Exception ignored) {
            }
        }
        updateButtonsUi();
    }

    public void updateButtonsUi() {
        titleLabel.setText(activeTitle);
        btnPlayPause.setText(isPlaying ? "⏸" : "▶");
        btnMute.setText(isMuted ? "🔇" : "🔊");
    }

    public void showSongPickerDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        ScrollView scroll = new ScrollView(activity);
        LinearLayout root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(18);
        root.setPadding(pad, pad, pad, pad);
        root.setBackgroundColor(Color.WHITE);

        AlertDialog[] dialogRef = new AlertDialog[1];

        TextView tvHeader = new TextView(activity);
        tvHeader.setText("🎵 Background Music (K-Pop Studio Audio)");
        tvHeader.setTextColor(Color.parseColor("#111827"));
        tvHeader.setTextSize(17f);
        tvHeader.setTypeface(Typeface.DEFAULT_BOLD);
        root.addView(tvHeader);

        TextView desc = new TextView(activity);
        desc.setText("Pick any song below or search any K-Pop song/artist to stream in the background.");
        desc.setTextColor(Color.parseColor("#6B7280"));
        desc.setTextSize(12.5f);
        desc.setPadding(0, dp(4), 0, dp(12));
        root.addView(desc);

        // Live Song Search Bar
        LinearLayout searchRow = new LinearLayout(activity);
        searchRow.setOrientation(LinearLayout.HORIZONTAL);
        searchRow.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams sLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        sLp.setMargins(0, 0, 0, dp(12));
        searchRow.setLayoutParams(sLp);

        EditText etSearchSong = new EditText(activity);
        etSearchSong.setHint("🔍 Search any K-Pop song (e.g. TWICE, G-Dragon, YENA)...");
        etSearchSong.setTextColor(Color.parseColor("#111827"));
        etSearchSong.setHintTextColor(Color.parseColor("#9CA3AF"));
        etSearchSong.setTextSize(12.5f);
        etSearchSong.setSingleLine(true);
        etSearchSong.setPadding(dp(12), dp(10), dp(12), dp(10));
        etSearchSong.setBackgroundResource(R.drawable.bg_chip_tag);
        LinearLayout.LayoutParams etLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        etLp.setMargins(0, 0, dp(8), 0);
        etSearchSong.setLayoutParams(etLp);
        searchRow.addView(etSearchSong);

        TextView btnFind = new TextView(activity);
        btnFind.setText("Play");
        btnFind.setTextColor(Color.parseColor("#111827"));
        btnFind.setTypeface(Typeface.DEFAULT_BOLD);
        btnFind.setTextSize(12.5f);
        btnFind.setPadding(dp(14), dp(10), dp(14), dp(10));
        btnFind.setBackgroundResource(R.drawable.bg_btn_gold);
        searchRow.addView(btnFind);
        root.addView(searchRow);

        btnFind.setOnClickListener(v -> {
            String q = etSearchSong.getText().toString().trim();
            if (q.isEmpty()) return;
            Toast.makeText(activity, "🎵 Searching \"" + q + "\"...", Toast.LENGTH_SHORT).show();
            searchAndPlayItunesSong(q, () -> {
                if (dialogRef[0] != null) dialogRef[0].dismiss();
            });
        });

        for (String[] preset : PRESETS) {
            TextView item = new TextView(activity);
            boolean active = preset[0].equals(activeTitle);
            item.setText((active ? "🔊 " : "") + preset[0]);
            item.setTextColor(Color.parseColor("#111827"));
            item.setTypeface(null, active ? Typeface.BOLD : Typeface.NORMAL);
            item.setTextSize(13.5f);
            item.setPadding(dp(14), dp(12), dp(14), dp(12));
            item.setBackgroundResource(active ? R.drawable.bg_btn_gold : R.drawable.bg_chip_tag);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0, 0, 0, dp(8));
            item.setLayoutParams(lp);
            item.setOnClickListener(v -> {
                loadAndPlay(preset[1], preset[0], true);
                if (dialogRef[0] != null) dialogRef[0].dismiss();
            });
            root.addView(item);
        }

        scroll.addView(root);
        builder.setView(scroll);
        builder.setNegativeButton(activity.getString(R.string.btn_cancel), null);
        dialogRef[0] = builder.create();
        dialogRef[0].show();
    }

    private void searchAndPlayItunesSong(String query, Runnable onSuccess) {
        executor.execute(() -> {
            try {
                String u = "https://itunes.apple.com/search?term=" + URLEncoder.encode(query, "UTF-8") + "&entity=song&limit=1";
                HttpURLConnection conn = (HttpURLConnection) new URL(u).openConnection();
                conn.setConnectTimeout(4000);
                conn.setReadTimeout(4000);
                if (conn.getResponseCode() == 200) {
                    StringBuilder sb = new StringBuilder();
                    try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                        String line;
                        while ((line = br.readLine()) != null) sb.append(line);
                    }
                    JSONObject j = new JSONObject(sb.toString());
                    JSONArray res = j.optJSONArray("results");
                    if (res != null && res.length() > 0) {
                        JSONObject song = res.getJSONObject(0);
                        String preview = song.optString("previewUrl", "");
                        String trackName = song.optString("trackName", query);
                        String artistName = song.optString("artistName", "");
                        if (!preview.isEmpty()) {
                            String label = "🎵 " + artistName + " - " + trackName;
                            mainHandler.post(() -> {
                                loadAndPlay(preview, label, true);
                                if (onSuccess != null) onSuccess.run();
                            });
                            return;
                        }
                    }
                }
            } catch (Exception ignored) {
            }
            mainHandler.post(() -> Toast.makeText(activity, "Could not find audio stream for: " + query, Toast.LENGTH_SHORT).show());
        });
    }

    public void destroy() {
        // Keep sharedPlayer alive across screens so music never cuts off!
    }

    private int dp(int v) {
        return Math.round(v * activity.getResources().getDisplayMetrics().density);
    }
}
