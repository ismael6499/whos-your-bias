package com.nitsutech.whosyourbias.ui;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.nitsutech.whosyourbias.R;
import com.nitsutech.whosyourbias.data.BiasRepository;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class YoutubeBgmController {
    private static final String PREF_BGM_ID = "bgm_video_id";
    private static final String PREF_BGM_TITLE = "bgm_title";
    private static final String PREF_BGM_AUTOPLAY = "bgm_autoplay";

    // Presets: Video ID or "SYNTH_BEAT" for built-in copyright-free K-Pop synth loop
    public static final String[][] PRESETS = new String[][]{
            {"🎶 ILLIT - It's Me (Who is your bias?)", "utr3G6G3kI0"},
            {"🍬 ILLIT - Jellyous / I'LL LIKE YOU", "tbDGl7jEazA"},
            {"🧲 ILLIT - Magnetic", "Vk5-c_v4gMU"},
            {"🍒 ILLIT - Cherish (My Love)", "tbDGl7jEazA"},
            {"💖 RESCENE - LOVE ATTACK", "X9_n8jakvWU"},
            {"✨ KATSEYE - Touch", "l9CZykYZkOQ"},
            {"🐱 MEOVV - MEOW", "jpLHg-AsQV0"},
            {"🌿 KiiiKiii - I DO ME", "5K8W1_2wNRE"},
            {"⚡ aespa - Supernova", "phuiiNCxRMg"},
            {"🔥 Stray Kids - Chk Chk Boom", "0P0aQreFs8w"},
            {"🎹 K-Pop Jellyous Synth Beat (Built-in Offline BGM)", "SYNTH_BEAT"}
    };

    private final Activity activity;
    private final WebView webView;
    private final TextView titleLabel;
    private final TextView btnPlayPause;
    private final TextView btnMute;
    private final View videoContainer;

    private String currentVideoId;
    private String currentTitle;
    private boolean isPlaying = false;
    private boolean isMuted = false;
    private boolean isVideoExpanded = false;

    @SuppressLint("SetJavaScriptEnabled")
    public YoutubeBgmController(Activity activity, WebView webView, View videoContainer,
                                TextView titleLabel, TextView btnPlayPause, TextView btnMute,
                                TextView btnToggleVideo, TextView btnChangeSong) {
        this.activity = activity;
        this.webView = webView;
        this.videoContainer = videoContainer;
        this.titleLabel = titleLabel;
        this.btnPlayPause = btnPlayPause;
        this.btnMute = btnMute;

        SharedPreferences prefs = BiasRepository.getInstance(activity).getPrefs();
        this.currentVideoId = prefs.getString(PREF_BGM_ID, PRESETS[0][1]);
        this.currentTitle = prefs.getString(PREF_BGM_TITLE, PRESETS[0][0]);

        WebSettings ws = webView.getSettings();
        ws.setJavaScriptEnabled(true);
        ws.setDomStorageEnabled(true);
        ws.setMediaPlaybackRequiresUserGesture(false);
        webView.setBackgroundColor(Color.BLACK);
        webView.setWebChromeClient(new WebChromeClient());
        webView.setWebViewClient(new WebViewClient());

        titleLabel.setText(currentTitle);
        updateButtonsUi();

        btnPlayPause.setOnClickListener(v -> togglePlayPause());
        btnMute.setOnClickListener(v -> toggleMute());
        btnToggleVideo.setOnClickListener(v -> {
            isVideoExpanded = !isVideoExpanded;
            videoContainer.setVisibility(isVideoExpanded ? View.VISIBLE : View.GONE);
        });
        btnChangeSong.setOnClickListener(v -> showSongPickerDialog());

        if (prefs.getBoolean(PREF_BGM_AUTOPLAY, true)) {
            loadAndPlay(currentVideoId, currentTitle, true);
        }
    }

    public void loadAndPlay(String videoIdOrMode, String title, boolean autoStart) {
        this.currentVideoId = videoIdOrMode;
        this.currentTitle = title;
        this.isPlaying = autoStart;
        titleLabel.setText(title);
        updateButtonsUi();

        SharedPreferences prefs = BiasRepository.getInstance(activity).getPrefs();
        prefs.edit()
                .putString(PREF_BGM_ID, currentVideoId)
                .putString(PREF_BGM_TITLE, currentTitle)
                .putBoolean(PREF_BGM_AUTOPLAY, autoStart)
                .apply();

        if (!autoStart) {
            webView.loadUrl("about:blank");
            return;
        }

        if ("SYNTH_BEAT".equals(videoIdOrMode)) {
            webView.loadDataWithBaseURL("https://whosyourbias.local", buildWebAudioSynthHtml(), "text/html", "UTF-8", null);
        } else {
            String html = buildYoutubeIframeHtml(videoIdOrMode);
            webView.loadDataWithBaseURL("https://www.youtube.com", html, "text/html", "UTF-8", null);
        }
    }

    public void togglePlayPause() {
        if (!isPlaying) {
            loadAndPlay(currentVideoId, currentTitle, true);
        } else {
            isPlaying = false;
            webView.loadUrl("about:blank");
            BiasRepository.getInstance(activity).getPrefs().edit().putBoolean(PREF_BGM_AUTOPLAY, false).apply();
            updateButtonsUi();
        }
    }

    public void toggleMute() {
        isMuted = !isMuted;
        webView.evaluateJavascript("if(window.toggleMuteBgm) window.toggleMuteBgm(" + isMuted + ");", null);
        updateButtonsUi();
    }

    private void updateButtonsUi() {
        btnPlayPause.setText(isPlaying ? activity.getString(R.string.bgm_pause) : activity.getString(R.string.bgm_play));
        btnMute.setText(isMuted ? activity.getString(R.string.bgm_mute) : activity.getString(R.string.bgm_unmute));
    }

    public void showSongPickerDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity, AlertDialog.THEME_DEVICE_DEFAULT_DARK);
        builder.setTitle(activity.getString(R.string.bgm_dialog_title));

        ScrollView scroll = new ScrollView(activity);
        LinearLayout root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(16);
        root.setPadding(pad, pad, pad, pad);
        root.setBackgroundColor(Color.parseColor("#141827"));

        TextView desc = new TextView(activity);
        desc.setText(activity.getString(R.string.bgm_dialog_desc));
        desc.setTextColor(Color.parseColor("#94A3B8"));
        desc.setTextSize(13f);
        desc.setPadding(0, 0, 0, dp(12));
        root.addView(desc);

        AlertDialog[] dialogRef = new AlertDialog[1];

        for (String[] preset : PRESETS) {
            TextView item = new TextView(activity);
            item.setText(preset[0]);
            item.setTextColor(Color.WHITE);
            item.setTextSize(14f);
            item.setPadding(dp(14), dp(12), dp(14), dp(12));
            item.setBackgroundResource(R.drawable.bg_btn_secondary);
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

        // Custom YouTube link or Search input
        TextView customLabel = new TextView(activity);
        customLabel.setText("🔗 Custom YouTube URL or Video ID:");
        customLabel.setTextColor(Color.parseColor("#00F0FF"));
        customLabel.setTextSize(13f);
        customLabel.setPadding(0, dp(10), 0, dp(6));
        root.addView(customLabel);

        EditText input = new EditText(activity);
        input.setHint(activity.getString(R.string.bgm_custom_url_hint));
        input.setTextColor(Color.WHITE);
        input.setHintTextColor(Color.parseColor("#64748B"));
        input.setBackgroundResource(R.drawable.bg_chip_tag);
        input.setPadding(dp(12), dp(10), dp(12), dp(10));
        root.addView(input);

        TextView btnApply = new TextView(activity);
        btnApply.setText(activity.getString(R.string.bgm_apply));
        btnApply.setTextColor(Color.WHITE);
        btnApply.setGravity(Gravity.CENTER);
        btnApply.setBackgroundResource(R.drawable.bg_btn_primary);
        btnApply.setPadding(dp(14), dp(12), dp(14), dp(12));
        LinearLayout.LayoutParams applyLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        applyLp.setMargins(0, dp(10), 0, 0);
        btnApply.setLayoutParams(applyLp);
        btnApply.setOnClickListener(v -> {
            String raw = input.getText().toString().trim();
            if (!raw.isEmpty()) {
                String vid = extractYoutubeId(raw);
                loadAndPlay(vid, "🎵 Custom YouTube (" + vid + ")", true);
                if (dialogRef[0] != null) dialogRef[0].dismiss();
            }
        });
        root.addView(btnApply);

        scroll.addView(root);
        builder.setView(scroll);
        builder.setNegativeButton(activity.getString(R.string.btn_cancel), null);
        dialogRef[0] = builder.create();
        dialogRef[0].show();
    }

    public static String extractYoutubeId(String urlOrId) {
        if (urlOrId == null) return "utr3G6G3kI0";
        String s = urlOrId.trim();
        Pattern p = Pattern.compile("(?:v=|youtu\\.be/|embed/|shorts/)([A-Za-z0-9_\\-]{11})");
        Matcher m = p.matcher(s);
        if (m.find()) return m.group(1);
        if (s.length() == 11) return s;
        return s;
    }

    private String buildYoutubeIframeHtml(String videoId) {
        return "<!DOCTYPE html><html><head><meta name='viewport' content='width=device-width, initial-scale=1.0'>"
                + "<style>html,body{margin:0;padding:0;width:100%;height:100%;background:#0B0E17;overflow:hidden;}"
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
                + "      'onReady': function(e) { e.target.playVideo(); }"
                + "    }"
                + "  });"
                + "}"
                + "window.toggleMuteBgm = function(m) { if(!player) return; if(m) player.mute(); else player.unMute(); };"
                + "</script></body></html>";
    }

    /**
     * Built-in copyright-free K-Pop "Jellyous / Pluggnb" upbeat synth loop using WebAudio API.
     */
    private String buildWebAudioSynthHtml() {
        return "<!DOCTYPE html><html><body style='background:#0B0E17;color:#00F0FF;font-family:sans-serif;display:flex;align-items:center;justify-content:center;height:100%;margin:0;'>"
                + "<div style='font-size:12px;'>🎹 Playing K-Pop Jellyous Synth Loop</div>"
                + "<script>"
                + "var ctx = new (window.AudioContext || window.webkitAudioContext)();"
                + "var muted = false;"
                + "var notes = [523.25, 659.25, 783.99, 987.77, 1046.50, 783.99, 659.25, 587.33];"
                + "var step = 0;"
                + "setInterval(function(){"
                + "  if(muted) return;"
                + "  if(ctx.state === 'suspended') ctx.resume();"
                + "  var osc = ctx.createOscillator();"
                + "  var gain = ctx.createGain();"
                + "  osc.type = (step % 2 === 0) ? 'triangle' : 'sine';"
                + "  osc.frequency.setValueAtTime(notes[step % notes.length], ctx.currentTime);"
                + "  gain.gain.setValueAtTime(0.09, ctx.currentTime);"
                + "  gain.gain.exponentialRampToValueAtTime(0.001, ctx.currentTime + 0.22);"
                + "  osc.connect(gain); gain.connect(ctx.destination);"
                + "  osc.start(); osc.stop(ctx.currentTime + 0.23);"
                + "  step++;"
                + "}, 240);"
                + "window.toggleMuteBgm = function(m) { muted = m; };"
                + "</script></body></html>";
    }

    public void destroy() {
        try {
            webView.loadUrl("about:blank");
            webView.destroy();
        } catch (Exception ignored) {
        }
    }

    private int dp(int v) {
        return Math.round(v * activity.getResources().getDisplayMetrics().density);
    }
}
