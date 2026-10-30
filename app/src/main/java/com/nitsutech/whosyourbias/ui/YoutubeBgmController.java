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
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.cardview.widget.CardView;
import androidx.core.app.NotificationCompat;

import com.nitsutech.whosyourbias.R;
import com.nitsutech.whosyourbias.data.BiasRepository;
import com.nitsutech.whosyourbias.util.CardImageLoader;
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
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class YoutubeBgmController {
    private static final String PREF_BGM_URL = "bgm_stream_url_v2";
    private static final String PREF_BGM_TITLE = "bgm_stream_title_v2";
    private static final String PREF_BGM_AUTOPLAY = "bgm_autoplay";
    private static final String PREF_SAVED_SONGS = "bgm_saved_songs_json";

    public static final String ACTION_TOGGLE_PLAY = "com.nitsutech.whosyourbias.ACTION_BGM_TOGGLE";
    public static final String ACTION_STOP = "com.nitsutech.whosyourbias.ACTION_BGM_STOP";
    private static final String CHANNEL_ID = "wyb_music_channel";
    private static final int NOTIF_ID = 4040;

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
    private final WebView webView;
    private final View videoContainer;
    private final TextView tvNowPlaying;
    private final TextView btnPlayPause;
    private final TextView btnMute;
    private final TextView btnVideoToggle;
    private final SharedPreferences prefs;

    public static class YoutubeSearchResult {
        public final String videoId;
        public final String title;
        public final String channel;
        public final String thumbUrl;

        public YoutubeSearchResult(String videoId, String title, String channel, String thumbUrl) {
            this.videoId = videoId;
            this.title = title;
            this.channel = channel;
            this.thumbUrl = thumbUrl;
        }
    }

    public YoutubeBgmController(Activity activity, WebView webView, View videoContainer,
                                TextView tvNowPlaying, TextView btnPlayPause, TextView btnMute,
                                TextView btnVideoToggle, TextView btnChangeSong) {
        this.activity = activity;
        this.webView = webView;
        this.videoContainer = videoContainer;
        this.tvNowPlaying = tvNowPlaying;
        this.btnPlayPause = btnPlayPause;
        this.btnMute = btnMute;
        this.btnVideoToggle = btnVideoToggle;
        this.prefs = BiasRepository.getInstance(activity).getPrefs();
        activeControllerRef = new WeakReference<>(this);

        if (this.webView != null) {
            WebSettings ws = this.webView.getSettings();
            ws.setJavaScriptEnabled(true);
            ws.setDomStorageEnabled(true);
            ws.setMediaPlaybackRequiresUserGesture(false);
            this.webView.setBackgroundColor(Color.BLACK);
            this.webView.setWebChromeClient(new WebChromeClient());
            this.webView.setWebViewClient(new WebViewClient());
        }

        if (videoContainer != null) videoContainer.setVisibility(View.GONE);
        if (btnVideoToggle != null) {
            btnVideoToggle.setVisibility(View.GONE);
            btnVideoToggle.setOnClickListener(v -> {
                if (videoContainer != null) {
                    boolean isVis = videoContainer.getVisibility() == View.VISIBLE;
                    videoContainer.setVisibility(isVis ? View.GONE : View.VISIBLE);
                    btnVideoToggle.setText(isVis ? "📺" : "✕");
                }
            });
        }

        String savedUrl = prefs.getString(PREF_BGM_URL, PRESETS[0][1]);
        String savedTitle = prefs.getString(PREF_BGM_TITLE, PRESETS[0][0]);
        boolean isMatch = (activity instanceof MatchActivity);
        // Only autoplay when entering a tournament/challenge (MatchActivity).
        // On app startup (MainActivity), music is NEVER started automatically.
        boolean shouldAutoPlay = isMatch;

        if (this.tvNowPlaying != null) this.tvNowPlaying.setText(savedTitle);

        this.btnPlayPause.setOnClickListener(v -> togglePlayPause());
        this.btnMute.setOnClickListener(v -> toggleMute());
        if (btnChangeSong != null) {
            btnChangeSong.setOnClickListener(v -> showSongSelectorDialog());
        }
        if (this.tvNowPlaying != null) {
            this.tvNowPlaying.setOnClickListener(v -> showSongSelectorDialog());
        }

        if (sharedPlayer != null && isPrepared && savedUrl.equals(activeStreamUrl)) {
            updateButtonsUi();
            if (isMatch && !isPlaying) {
                float vol = isMuted ? 0f : 0.85f;
                sharedPlayer.setVolume(vol, vol);
                sharedPlayer.start();
                isPlaying = true;
                updateButtonsUi();
                showOrUpdateMusicNotification(activity.getApplicationContext());
            }
        } else {
            startStream(savedTitle, savedUrl, shouldAutoPlay);
        }
    }

    public static boolean isYoutubeUrl(String url) {
        if (url == null) return false;
        String s = url.trim().toLowerCase(Locale.ROOT);
        return s.contains("youtube.com") || s.contains("youtu.be") || s.matches("^[a-za-z0-9_\\-]{11}$");
    }

    public static String extractYoutubeId(String urlOrId) {
        if (urlOrId == null) return "Vk5-c_v4gMU";
        String s = urlOrId.trim();
        Matcher m = Pattern.compile("(?:v=|youtu\\.be/|embed/|shorts/)([A-Za-z0-9_\\-]{11})").matcher(s);
        if (m.find()) return m.group(1);
        if (s.matches("^[A-Za-z0-9_\\-]{11}$")) return s;
        return s;
    }

    private void startStream(String title, String streamUrl, boolean autoPlay) {
        activeSongTitle = title;
        activeStreamUrl = streamUrl;
        isPrepared = false;
        if (tvNowPlaying != null) tvNowPlaying.setText(title);

        boolean isYt = isYoutubeUrl(streamUrl);
        if (btnVideoToggle != null) {
            btnVideoToggle.setVisibility(isYt ? View.VISIBLE : View.GONE);
            btnVideoToggle.setText("📺");
        }

        if (isYt) {
            // Stop any native audio
            if (sharedPlayer != null) {
                try {
                    sharedPlayer.reset();
                } catch (Exception ignored) {}
            }
            String vid = extractYoutubeId(streamUrl);
            if (webView != null) {
                if (autoPlay) {
                    String html = buildYoutubeIframeHtml(vid);
                    webView.loadDataWithBaseURL("https://www.youtube.com", html, "text/html", "UTF-8", null);
                    isPlaying = true;
                    isPrepared = true;
                    showOrUpdateMusicNotification(activity.getApplicationContext());
                } else {
                    webView.loadUrl("about:blank");
                    isPlaying = false;
                }
            }
            updateButtonsUi();
            return;
        }

        // Native .m4a / audio stream via MediaPlayer
        if (webView != null) {
            webView.loadUrl("about:blank");
        }
        if (videoContainer != null) {
            videoContainer.setVisibility(View.GONE);
        }

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
            boolean isMatch = (activity instanceof MatchActivity);
            float baseVol = isMatch ? 0.85f : 0.20f;
            float vol = isMuted ? 0f : baseVol;
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
        if (isYoutubeUrl(activeStreamUrl)) {
            if (webView != null) {
                if (isPlaying) {
                    webView.evaluateJavascript("if(window.pauseBgm){pauseBgm();}else if(player){player.pauseVideo();}", null);
                    isPlaying = false;
                    prefs.edit().putBoolean(PREF_BGM_AUTOPLAY, false).apply();
                } else {
                    webView.evaluateJavascript("if(window.playBgm){playBgm();}else if(player){player.playVideo();}", null);
                    isPlaying = true;
                    prefs.edit().putBoolean(PREF_BGM_AUTOPLAY, true).apply();
                }
            }
            updateButtonsUi();
            showOrUpdateMusicNotification(activity.getApplicationContext());
            return;
        }

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
                boolean isMatch = (activity instanceof MatchActivity);
                float baseVol = isMatch ? 0.85f : 0.20f;
                float vol = isMuted ? 0f : baseVol;
                sharedPlayer.setVolume(vol, vol);
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
        if (isYoutubeUrl(activeStreamUrl)) {
            if (webView != null) {
                webView.evaluateJavascript("if(window.toggleMuteBgm){toggleMuteBgm(" + isMuted + ");}", null);
            }
        } else if (sharedPlayer != null) {
            boolean isMatch = (activity instanceof MatchActivity);
            float baseVol = isMatch ? 0.85f : 0.20f;
            float vol = isMuted ? 0f : baseVol;
            sharedPlayer.setVolume(vol, vol);
        }
        updateButtonsUi();
    }

    public void updateButtonsUi() {
        if (btnPlayPause != null) {
            btnPlayPause.setText(isPlaying ? "⏸" : "▶");
        }
        if (btnMute != null) {
            btnMute.setText(isMuted ? "🔇" : "🔊");
        }
        if (tvNowPlaying != null) {
            tvNowPlaying.setText(activeSongTitle);
        }
    }

    private String buildYoutubeIframeHtml(String videoId) {
        boolean isMatch = (activity instanceof MatchActivity);
        int targetVol = isMatch ? 85 : 20;
        return "<!DOCTYPE html><html><head><meta name='viewport' content='width=device-width, initial-scale=1.0'>"
                + "<style>html,body{margin:0;padding:0;width:100%;height:100%;background:#000000;overflow:hidden;}"
                + "#player{width:100%;height:100%;}</style></head><body>"
                + "<div id='player'></div>"
                + "<script>"
                + "var tag = document.createElement('script');"
                + "tag.src = 'https://www.youtube.com/iframe_api';"
                + "var firstScriptTag = document.getElementsByTagName('script')[0];"
                + "firstScriptTag.parentNode.insertBefore(tag, firstScriptTag);"
                + "var player;"
                + "function onYouTubeIframeAPIReady() {"
                + "  player = new YT.Player('player', {"
                + "    height: '100%', width: '100%',"
                + "    videoId: '" + videoId + "',"
                + "    playerVars: { 'autoplay': 1, 'playsinline': 1, 'loop': 1, 'playlist': '" + videoId + "', 'controls': 1, 'rel': 0 },"
                + "    events: {"
                + "      'onReady': function(e) { if(e.target.setVolume) e.target.setVolume(" + targetVol + "); e.target.playVideo(); if(" + isMuted + ") e.target.mute(); }"
                + "    }"
                + "  });"
                + "}"
                + "window.toggleMuteBgm = function(m) { if(!player) return; if(m) player.mute(); else player.unMute(); };"
                + "window.playBgm = function() { if(player) { if(player.setVolume) player.setVolume(" + targetVol + "); player.playVideo(); } };"
                + "window.pauseBgm = function() { if(player) player.pauseVideo(); };"
                + "</script></body></html>";
    }

    public static void handleNotificationToggle(Context context) {
        handleMediaAction(context, ACTION_TOGGLE_PLAY);
    }

    public static void handleNotificationStop(Context context) {
        handleMediaAction(context, ACTION_STOP);
    }

    public static void handleMediaAction(Context context, String action) {
        if (ACTION_TOGGLE_PLAY.equals(action) || BgmNotificationReceiver.ACTION_TOGGLE_PLAY.equals(action)) {
            YoutubeBgmController active = activeControllerRef != null ? activeControllerRef.get() : null;
            if (active != null) {
                active.togglePlayPause();
            } else if (sharedPlayer != null && isPrepared) {
                if (sharedPlayer.isPlaying()) {
                    sharedPlayer.pause();
                    isPlaying = false;
                } else {
                    sharedPlayer.start();
                    isPlaying = true;
                }
                showOrUpdateMusicNotification(context);
            }
        } else if (ACTION_STOP.equals(action) || BgmNotificationReceiver.ACTION_STOP_BGM.equals(action)) {
            if (sharedPlayer != null) {
                try {
                    sharedPlayer.stop();
                    sharedPlayer.reset();
                } catch (Exception ignored) {
                }
            }
            isPlaying = false;
            isPrepared = false;
            NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null) nm.cancel(NOTIF_ID);
            YoutubeBgmController active = activeControllerRef != null ? activeControllerRef.get() : null;
            if (active != null) active.updateButtonsUi();
        }
    }

    public static void showOrUpdateMusicNotification(Context context) {
        if (context == null) return;
        try {
            NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm == null) return;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationChannel chan = new NotificationChannel(
                        CHANNEL_ID, "Who\'s your Bias BGM", NotificationManager.IMPORTANCE_LOW);
                chan.setDescription("Music playback controls for tournament matches");
                chan.setShowBadge(false);
                chan.enableVibration(false);
                nm.createNotificationChannel(chan);
            }

            Intent toggleIntent = new Intent(context, BgmNotificationReceiver.class).setAction(BgmNotificationReceiver.ACTION_TOGGLE_PLAY);
            int flags = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE : PendingIntent.FLAG_UPDATE_CURRENT;
            PendingIntent togglePi = PendingIntent.getBroadcast(context, 101, toggleIntent, flags);

            Intent stopIntent = new Intent(context, BgmNotificationReceiver.class).setAction(BgmNotificationReceiver.ACTION_STOP_BGM);
            PendingIntent stopPi = PendingIntent.getBroadcast(context, 102, stopIntent, flags);

            Intent openIntent = new Intent(context, MainActivity.class);
            PendingIntent openPi = PendingIntent.getActivity(context, 100, openIntent, flags);

            NotificationCompat.Builder nb = new NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentTitle("🎵 " + activeSongTitle)
                    .setContentText("Who\'s your Bias • BGM")
                    .setContentIntent(openPi)
                    .setOngoing(isPlaying)
                    .setAutoCancel(!isPlaying)
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

    public static List<YoutubeSearchResult> searchYoutubeSync(String query) {
        List<YoutubeSearchResult> list = new ArrayList<>();
        if (query == null || query.trim().isEmpty()) return list;
        String q = query.trim();

        // 1. Direct YouTube link or Video ID pasted by user!
        if (isYoutubeUrl(q)) {
            String vid = extractYoutubeId(q);
            String title = "YouTube Video (" + vid + ")";
            String channel = "YouTube";
            String thumb = "https://img.youtube.com/vi/" + vid + "/hqdefault.jpg";
            try {
                String oembedUrl = "https://www.youtube.com/oembed?url=https://www.youtube.com/watch?v=" + vid + "&format=json";
                HttpURLConnection conn = (HttpURLConnection) new URL(oembedUrl).openConnection();
                conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36");
                conn.setConnectTimeout(4500);
                conn.setReadTimeout(4500);
                if (conn.getResponseCode() == 200) {
                    StringBuilder sb = new StringBuilder();
                    try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                        String line;
                        while ((line = br.readLine()) != null) sb.append(line);
                    }
                    JSONObject json = new JSONObject(sb.toString());
                    title = json.optString("title", title);
                    channel = json.optString("author_name", channel);
                    thumb = json.optString("thumbnail_url", thumb);
                }
            } catch (Exception ignored) {}
            list.add(new YoutubeSearchResult(vid, title, channel, thumb));
            return list;
        }

        // 2. Official YouTube InnerTube JSON Endpoint (Zero redirects, instant response)
        try {
            JSONObject body = new JSONObject();
            JSONObject context = new JSONObject();
            JSONObject client = new JSONObject();
            client.put("clientName", "WEB");
            client.put("clientVersion", "2.20240401.00.00");
            client.put("hl", "en");
            client.put("gl", "US");
            context.put("client", client);
            body.put("context", context);
            body.put("query", q);

            byte[] postData = body.toString().getBytes(StandardCharsets.UTF_8);
            HttpURLConnection conn = (HttpURLConnection) new URL("https://www.youtube.com/youtubei/v1/search").openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Content-Length", String.valueOf(postData.length));
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36");
            conn.setDoOutput(true);
            conn.setConnectTimeout(6000);
            conn.setReadTimeout(6000);
            conn.getOutputStream().write(postData);

            if (conn.getResponseCode() == 200) {
                StringBuilder sb = new StringBuilder();
                try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = br.readLine()) != null) sb.append(line);
                }
                JSONObject json = new JSONObject(sb.toString());
                JSONObject twoCol = json.optJSONObject("contents") != null ?
                        json.optJSONObject("contents").optJSONObject("twoColumnSearchResultsRenderer") : null;
                if (twoCol != null) {
                    JSONArray sections = twoCol.optJSONObject("primaryContents") != null ?
                            twoCol.optJSONObject("primaryContents").optJSONObject("sectionListRenderer") != null ?
                                    twoCol.optJSONObject("primaryContents").optJSONObject("sectionListRenderer").optJSONArray("contents") : null : null;
                    if (sections != null) {
                        for (int s = 0; s < sections.length(); s++) {
                            JSONObject sec = sections.optJSONObject(s);
                            if (sec == null) continue;
                            JSONArray items = sec.optJSONObject("itemSectionRenderer") != null ?
                                    sec.optJSONObject("itemSectionRenderer").optJSONArray("contents") : null;
                            if (items == null) continue;
                            for (int i = 0; i < items.length(); i++) {
                                JSONObject item = items.optJSONObject(i);
                                if (item == null) continue;
                                JSONObject vr = item.optJSONObject("videoRenderer");
                                if (vr != null) {
                                    String id = vr.optString("videoId", "");
                                    String title = "";
                                    JSONObject tObj = vr.optJSONObject("title");
                                    if (tObj != null) {
                                        JSONArray runs = tObj.optJSONArray("runs");
                                        if (runs != null && runs.length() > 0) {
                                            title = runs.getJSONObject(0).optString("text", "");
                                        }
                                    }
                                    String channel = "";
                                    JSONObject cObj = vr.optJSONObject("ownerText");
                                    if (cObj != null) {
                                        JSONArray runs = cObj.optJSONArray("runs");
                                        if (runs != null && runs.length() > 0) {
                                            channel = runs.getJSONObject(0).optString("text", "");
                                        }
                                    }
                                    if (!id.isEmpty() && !title.isEmpty()) {
                                        String thumb = "https://img.youtube.com/vi/" + id + "/mqdefault.jpg";
                                        list.add(new YoutubeSearchResult(id, title, channel, thumb));
                                        if (list.size() >= 12) break;
                                    }
                                }
                            }
                            if (!list.isEmpty()) break;
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 3. Fallback: Desktop Scrape if InnerTube was empty
        if (list.isEmpty()) {
            try {
                String enc = URLEncoder.encode(q, "UTF-8");
                String url = "https://www.youtube.com/results?search_query=" + enc;
                HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
                conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36");
                conn.setConnectTimeout(6000);
                conn.setReadTimeout(6000);
                conn.setInstanceFollowRedirects(true);
                StringBuilder sb = new StringBuilder();
                try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = br.readLine()) != null) sb.append(line);
                }
                String html = sb.toString();
                Matcher matcher = Pattern.compile("ytInitialData\\s*=\\s*(\\{.+?\\});<\\/script>").matcher(html);
                if (matcher.find()) {
                    JSONObject json = new JSONObject(matcher.group(1));
                    JSONObject twoCol = json.optJSONObject("contents") != null ?
                            json.optJSONObject("contents").optJSONObject("twoColumnSearchResultsRenderer") : null;
                    if (twoCol != null) {
                        JSONArray sections = twoCol.optJSONObject("primaryContents") != null ?
                                twoCol.optJSONObject("primaryContents").optJSONObject("sectionListRenderer") != null ?
                                        twoCol.optJSONObject("primaryContents").optJSONObject("sectionListRenderer").optJSONArray("contents") : null : null;
                        if (sections != null) {
                            for (int s = 0; s < sections.length(); s++) {
                                JSONObject sec = sections.optJSONObject(s);
                                if (sec == null) continue;
                                JSONArray items = sec.optJSONObject("itemSectionRenderer") != null ?
                                        sec.optJSONObject("itemSectionRenderer").optJSONArray("contents") : null;
                                if (items == null) continue;
                                for (int i = 0; i < items.length(); i++) {
                                    JSONObject item = items.optJSONObject(i);
                                    if (item == null) continue;
                                    JSONObject vr = item.optJSONObject("videoRenderer");
                                    if (vr != null) {
                                        String id = vr.optString("videoId", "");
                                        String title = "";
                                        JSONObject tObj = vr.optJSONObject("title");
                                        if (tObj != null) {
                                            JSONArray runs = tObj.optJSONArray("runs");
                                            if (runs != null && runs.length() > 0) {
                                                title = runs.getJSONObject(0).optString("text", "");
                                            }
                                        }
                                        String channel = "";
                                        JSONObject cObj = vr.optJSONObject("ownerText");
                                        if (cObj != null) {
                                            JSONArray runs = cObj.optJSONArray("runs");
                                            if (runs != null && runs.length() > 0) {
                                                channel = runs.getJSONObject(0).optString("text", "");
                                            }
                                        }
                                        if (!id.isEmpty() && !title.isEmpty()) {
                                            String thumb = "https://img.youtube.com/vi/" + id + "/mqdefault.jpg";
                                            list.add(new YoutubeSearchResult(id, title, channel, thumb));
                                            if (list.size() >= 12) break;
                                        }
                                    }
                                }
                                if (!list.isEmpty()) break;
                            }
                        }
                    }
                }
            } catch (Exception ignored) {}
        }

        return list;
    }

    public void showSongSelectorDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);

        LinearLayout root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(18);
        root.setPadding(pad, pad, pad, pad);
        root.setBackgroundColor(Color.WHITE);

        TextView header = new TextView(activity);
        header.setText("🎵 " + activity.getString(R.string.bgm_presets_section));
        header.setTextColor(Color.parseColor("#111827"));
        header.setTextSize(17f);
        header.setTypeface(Typeface.DEFAULT_BOLD);
        header.setPadding(0, 0, 0, dp(10));
        root.addView(header);

        // Search Bar with Clear '✕' Button
        LinearLayout searchRow = new LinearLayout(activity);
        searchRow.setOrientation(LinearLayout.HORIZONTAL);
        searchRow.setGravity(Gravity.CENTER_VERTICAL);

        FrameLayout searchWrap = new FrameLayout(activity);
        LinearLayout.LayoutParams swLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        swLp.setMargins(0, 0, dp(8), 0);
        searchWrap.setLayoutParams(swLp);

        EditText etSearch = new EditText(activity);
        etSearch.setHint(activity.getString(R.string.bgm_search_hint));
        etSearch.setTextColor(Color.parseColor("#111827"));
        etSearch.setHintTextColor(Color.parseColor("#9CA3AF"));
        etSearch.setTextSize(13f);
        etSearch.setSingleLine(true);
        etSearch.setBackgroundResource(R.drawable.bg_chip_tag);
        etSearch.setPadding(dp(14), dp(10), dp(36), dp(10));
        etSearch.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        searchWrap.addView(etSearch);

        TextView btnClear = new TextView(activity);
        btnClear.setText("✕");
        btnClear.setTextSize(14f);
        btnClear.setTypeface(Typeface.DEFAULT_BOLD);
        btnClear.setTextColor(Color.parseColor("#9CA3AF"));
        btnClear.setGravity(Gravity.CENTER);
        int cp = dp(8);
        btnClear.setPadding(cp, cp, cp, cp);
        FrameLayout.LayoutParams clp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.END | Gravity.CENTER_VERTICAL);
        clp.setMargins(0, 0, dp(8), 0);
        btnClear.setLayoutParams(clp);
        btnClear.setVisibility(View.GONE);
        btnClear.setOnClickListener(v -> etSearch.setText(""));
        searchWrap.addView(btnClear);

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                btnClear.setVisibility(s != null && s.length() > 0 ? View.VISIBLE : View.GONE);
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        searchRow.addView(searchWrap);

        TextView btnSearch = new TextView(activity);
        btnSearch.setText(activity.getString(R.string.bgm_search_btn));
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
                    tvSavedSec.setText(activity.getString(R.string.bgm_saved_section));
                    tvSavedSec.setTextColor(Color.parseColor("#EC4899"));
                    tvSavedSec.setTextSize(12.5f);
                    tvSavedSec.setTypeface(Typeface.DEFAULT_BOLD);
                    tvSavedSec.setPadding(dp(4), dp(4), dp(4), dp(6));
                    listContainer.addView(tvSavedSec);

                    for (String[] s : saved) {
                        if (isYoutubeUrl(s[1])) {
                            String vid = extractYoutubeId(s[1]);
                            String thumb = "https://img.youtube.com/vi/" + vid + "/mqdefault.jpg";
                            addYoutubeSongRow(listContainer, vid, s[0], "YouTube", thumb, dialogRef, false, this);
                        } else {
                            addSongRow(listContainer, s[0], s[1], dialogRef, false, true, this);
                        }
                    }

                    TextView tvPresetsSec = new TextView(activity);
                    tvPresetsSec.setText(activity.getString(R.string.bgm_presets_section));
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
                List<YoutubeSearchResult> ytResults = searchYoutubeSync(q);

                // Fetch iTunes previews as studio fallback only if not a direct YouTube link
                List<String[]> itunesResults = new ArrayList<>();
                if (!isYoutubeUrl(q)) {
                    try {
                        String url = "https://itunes.apple.com/search?term=" + URLEncoder.encode(q, "UTF-8") + "&entity=song&limit=6";
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
                        if (results != null) {
                            for (int i = 0; i < results.length(); i++) {
                                JSONObject item = results.optJSONObject(i);
                                if (item != null) {
                                    String artist = item.optString("artistName", "");
                                    String track = item.optString("trackName", "");
                                    String preview = item.optString("previewUrl", "");
                                    if (!preview.isEmpty()) {
                                        itunesResults.add(new String[]{"🎵 " + artist + " - " + track, preview});
                                    }
                                }
                            }
                        }
                    } catch (Exception ignored) {}
                }

                mainHandler.post(() -> {
                    btnSearch.setText(activity.getString(R.string.bgm_search_btn));
                    listContainer.removeAllViews();

                    TextView btnBackToList = new TextView(activity);
                    btnBackToList.setText(activity.getString(R.string.bgm_back_saved));
                    btnBackToList.setTextColor(Color.parseColor("#EC4899"));
                    btnBackToList.setTextSize(12.5f);
                    btnBackToList.setTypeface(Typeface.DEFAULT_BOLD);
                    btnBackToList.setPadding(dp(6), dp(4), dp(6), dp(10));
                    btnBackToList.setOnClickListener(v2 -> populateSavedAndPresets.run());
                    listContainer.addView(btnBackToList);

                    if (ytResults.isEmpty() && itunesResults.isEmpty()) {
                        Toast.makeText(activity, activity.getString(R.string.bgm_no_results, q), Toast.LENGTH_SHORT).show();
                        populateSavedAndPresets.run();
                        return;
                    }

                    // 1. YouTube Videos
                    if (!ytResults.isEmpty()) {
                        TextView tvYtHeader = new TextView(activity);
                        tvYtHeader.setText("▶️ Videos de YouTube");
                        tvYtHeader.setTextColor(Color.parseColor("#DC2626"));
                        tvYtHeader.setTextSize(12f);
                        tvYtHeader.setTypeface(Typeface.DEFAULT_BOLD);
                        tvYtHeader.setPadding(dp(4), dp(4), dp(4), dp(6));
                        listContainer.addView(tvYtHeader);

                        for (YoutubeSearchResult ytr : ytResults) {
                            addYoutubeSongRow(listContainer, ytr.videoId, ytr.title, ytr.channel, ytr.thumbUrl, dialogRef, true, populateSavedAndPresets);
                        }
                    }

                    // 2. Audio Previews
                    if (!itunesResults.isEmpty()) {
                        TextView tvAudioHeader = new TextView(activity);
                        tvAudioHeader.setText("🎧 Audio Studio Previews");
                        tvAudioHeader.setTextColor(Color.parseColor("#6B7280"));
                        tvAudioHeader.setTextSize(12f);
                        tvAudioHeader.setTypeface(Typeface.DEFAULT_BOLD);
                        tvAudioHeader.setPadding(dp(4), dp(10), dp(4), dp(6));
                        listContainer.addView(tvAudioHeader);

                        for (String[] it : itunesResults) {
                            addSongRow(listContainer, it[0], it[1], dialogRef, true, false, populateSavedAndPresets);
                        }
                    }
                });
            });
        });

        builder.setView(root);
        builder.setNegativeButton(R.string.btn_cancel, null);
        dialogRef[0] = builder.create();
        dialogRef[0].show();
    }

    private void addYoutubeSongRow(LinearLayout parent, String videoId, String title, String channel, String thumbUrl,
                                   AlertDialog[] dialogRef, boolean canSaveToList, Runnable refreshListCallback) {
        LinearLayout row = new LinearLayout(activity);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setBackgroundResource(R.drawable.bg_chip_tag);
        row.setPadding(dp(8), dp(8), dp(8), dp(8));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, dp(8));
        row.setLayoutParams(lp);

        // 16:9 Thumbnail Card
        CardView thumbCard = new CardView(activity);
        thumbCard.setRadius(dp(8));
        thumbCard.setCardElevation(dp(1));
        LinearLayout.LayoutParams tLp = new LinearLayout.LayoutParams(dp(76), dp(48));
        tLp.setMargins(0, 0, dp(10), 0);
        thumbCard.setLayoutParams(tLp);

        ImageView ivThumb = new ImageView(activity);
        ivThumb.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        ivThumb.setScaleType(ImageView.ScaleType.CENTER_CROP);
        thumbCard.addView(ivThumb);
        CardImageLoader.loadUriIntoImageView(activity, thumbUrl, ivThumb);
        row.addView(thumbCard);

        // Title and Channel Text Column
        LinearLayout textCol = new LinearLayout(activity);
        textCol.setOrientation(LinearLayout.VERTICAL);
        textCol.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView tvTitle = new TextView(activity);
        tvTitle.setText(title);
        tvTitle.setTextColor(Color.parseColor("#111827"));
        tvTitle.setTextSize(12.5f);
        tvTitle.setMaxLines(2);
        tvTitle.setEllipsize(TextUtils.TruncateAt.END);
        tvTitle.setTypeface(Typeface.DEFAULT_BOLD);
        textCol.addView(tvTitle);

        TextView tvChannel = new TextView(activity);
        tvChannel.setText("▶️ " + channel);
        tvChannel.setTextColor(Color.parseColor("#6B7280"));
        tvChannel.setTextSize(11f);
        tvChannel.setPadding(0, dp(2), 0, 0);
        textCol.addView(tvChannel);

        row.addView(textCol);

        final String fullLabel = "▶️ " + title;
        final String ytUrl = "https://www.youtube.com/watch?v=" + videoId;

        row.setOnClickListener(v -> {
            prefs.edit()
                    .putString(PREF_BGM_TITLE, fullLabel)
                    .putString(PREF_BGM_URL, ytUrl)
                    .putBoolean(PREF_BGM_AUTOPLAY, true)
                    .apply();
            startStream(fullLabel, ytUrl, true);
            if (dialogRef[0] != null) dialogRef[0].dismiss();
        });

        if (canSaveToList) {
            TextView btnSave = new TextView(activity);
            btnSave.setText("⭐");
            btnSave.setTextSize(14f);
            btnSave.setPadding(dp(8), dp(6), dp(8), dp(6));
            btnSave.setOnClickListener(v -> {
                saveCustomSong(fullLabel, ytUrl);
                Toast.makeText(activity, activity.getString(R.string.bgm_video_saved_toast), Toast.LENGTH_SHORT).show();
            });
            row.addView(btnSave);
        }

        parent.addView(row);
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
            btnSave.setText("⭐ " + activity.getString(R.string.btn_save_and_play));
            btnSave.setTextColor(Color.parseColor("#111827"));
            btnSave.setTextSize(11f);
            btnSave.setTypeface(Typeface.DEFAULT_BOLD);
            btnSave.setGravity(Gravity.CENTER);
            btnSave.setBackgroundResource(R.drawable.bg_btn_primary);
            btnSave.setPadding(dp(10), dp(6), dp(10), dp(6));
            btnSave.setOnClickListener(v -> {
                saveCustomSong(label, streamUrl);
                btnSave.setText("✅");
                Toast.makeText(activity, activity.getString(R.string.bgm_video_saved_toast), Toast.LENGTH_SHORT).show();
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
        if (isYoutubeUrl(activeStreamUrl) && webView != null) {
            webView.evaluateJavascript("if(window.pauseBgm){pauseBgm();}else if(player){player.pauseVideo();}", null);
        } else if (sharedPlayer != null && sharedPlayer.isPlaying()) {
            try {
                sharedPlayer.pause();
            } catch (Exception ignored) {}
        }
        isPlaying = false;
        updateButtonsUi();
        try {
            NotificationManager nm = (NotificationManager) activity.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null) nm.cancel(NOTIF_ID);
        } catch (Exception ignored) {}
    }

    public void resume() {
        updateButtonsUi();
    }

    public void destroy() {
        if (activity instanceof MatchActivity) {
            pause();
        }
    }

    private int dp(int val) {
        return Math.round(val * activity.getResources().getDisplayMetrics().density);
    }
}
