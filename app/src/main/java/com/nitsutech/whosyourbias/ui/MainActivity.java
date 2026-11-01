package com.nitsutech.whosyourbias.ui;

import android.Manifest;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.ColorDrawable;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Vibrator;
import android.os.VibrationEffect;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.FileProvider;

import com.nitsutech.whosyourbias.R;
import com.nitsutech.whosyourbias.data.BiasRepository;
import com.nitsutech.whosyourbias.data.CustomMatch;
import com.nitsutech.whosyourbias.data.IdolCard;
import com.nitsutech.whosyourbias.util.AppUpdateManager;
import com.nitsutech.whosyourbias.util.CardImageLoader;
import com.nitsutech.whosyourbias.util.KpopWikiImporter;
import com.nitsutech.whosyourbias.util.LocaleManager;
import com.nitsutech.whosyourbias.util.ShareImportManager;
import com.nitsutech.whosyourbias.util.ThemeManager;

import java.io.File;
import java.io.FileOutputStream;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public class MainActivity extends AppCompatActivity {
    private static final int REQ_PICK_CARD_PHOTO = 1001;
    private static final int REQ_PICK_EXTRA_PHOTOCARD = 1002;
    private static final int REQ_PICK_WYB_FILE = 1003;
    private static final int REQ_PICK_USER_AVATAR = 1004;

    private BiasRepository repo;
    private YoutubeBgmController bgmController;
    private FrameLayout mainContentContainer;

    private TextView tabBtnArena, tabBtnIdols, tabBtnGroups, tabBtnHallOfFame;
    private int currentTab = 0;

    private String idolSearchQuery = "";
    private String idolCategoryFilter = "ALL";
    private String groupSearchQuery = "";

    private EditText pendingPhotoUrlField;
    private ImageView pendingPhotoPreviewView;
    private IdolCard pendingPhotocardTargetIdol;
    private Runnable pendingPhotocardRefreshCallback;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleManager.applyLocale(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (Build.VERSION.SDK_INT >= 33) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 5001);
            }
        }

        repo = BiasRepository.getInstance(this);
        mainContentContainer = findViewById(R.id.mainContentContainer);

        tabBtnArena = findViewById(R.id.tabBtnArena);
        tabBtnIdols = findViewById(R.id.tabBtnIdols);
        tabBtnGroups = findViewById(R.id.tabBtnGroups);
        tabBtnHallOfFame = findViewById(R.id.tabBtnHallOfFame);

        tabBtnArena.setOnClickListener(v -> switchTab(0));
        tabBtnIdols.setOnClickListener(v -> switchTab(1));
        tabBtnGroups.setOnClickListener(v -> switchTab(2));
        tabBtnHallOfFame.setOnClickListener(v -> switchTab(3));

        View btnOpenThemeStudio = findViewById(R.id.btnOpenThemeStudio);
        if (btnOpenThemeStudio != null) {
            btnOpenThemeStudio.setOnClickListener(v -> ThemeManager.showThemeStudioDialog(this, () -> {
                applyThemeToShell();
                refreshCurrentTab();
            }));
        }

        View btnCenterPlus = findViewById(R.id.btnCenterPlus);
        if (btnCenterPlus != null) {
            btnCenterPlus.setOnClickListener(v -> showCenterPlusMenuDialog());
        }

        FrameLayout bgmWeb = findViewById(R.id.bgmWebViewContainer);
        View bgmVideoContainer = findViewById(R.id.bgmVideoContainer);
        TextView tvBgmTitle = findViewById(R.id.tvBgmTitle);
        TextView btnPlayPause = findViewById(R.id.btnBgmPlayPause);
        TextView btnMute = findViewById(R.id.btnBgmMute);
        TextView btnVideoToggle = findViewById(R.id.btnBgmVideoToggle);
        TextView btnChangeSong = findViewById(R.id.btnBgmChangeSong);
        bgmController = new YoutubeBgmController(
                this, bgmWeb, bgmVideoContainer, tvBgmTitle,
                btnPlayPause, btnMute, btnVideoToggle, btnChangeSong);

        applyThemeToShell();
        switchTab(0);
        handleIncomingIntent(getIntent());
        AppUpdateManager.checkOnAppLaunch(this);
    }

    private void applyThemeToShell() {
        ThemeManager.AppTheme theme = ThemeManager.getActiveTheme(this);
        View rootShell = findViewById(R.id.rootMainLayout);
        if (rootShell != null) rootShell.setBackgroundColor(theme.backgroundColor);

        View topHeader = findViewById(R.id.topHeaderLayout);
        if (topHeader != null) topHeader.setBackgroundColor(theme.backgroundColor);

        if (mainContentContainer != null) mainContentContainer.setBackgroundColor(theme.backgroundColor);

        TextView tvTopTitle = findViewById(R.id.tvAppTitleHeader);
        if (tvTopTitle != null) tvTopTitle.setTextColor(theme.textColor);

        View topUnderline = findViewById(R.id.viewHeaderUnderline);
        if (topUnderline != null) topUnderline.setBackgroundColor(theme.primaryColor);

        TextView tvBgmTitle = findViewById(R.id.tvBgmTitle);
        if (tvBgmTitle != null) {
            tvBgmTitle.setTextColor(theme.textColor);
            if (tvBgmTitle.getParent() instanceof View) {
                View bgmBar = (View) tvBgmTitle.getParent();
                bgmBar.setBackground(ThemeManager.createPillDrawable(this, theme.surfaceColor, 18));
                bgmBar.setPadding(dp(12), dp(6), dp(8), dp(6));
            }
        }

        TextView btnPlayPause = findViewById(R.id.btnBgmPlayPause);
        if (btnPlayPause != null) {
            GradientDrawable oval = new GradientDrawable();
            oval.setShape(GradientDrawable.OVAL);
            oval.setColor(theme.primaryColor);
            btnPlayPause.setBackground(oval);
            btnPlayPause.setTextColor(theme.getButtonTextColor(theme.primaryColor));
        }

        View btnOpenThemeStudio = findViewById(R.id.btnOpenThemeStudio);
        if (btnOpenThemeStudio != null) {
            btnOpenThemeStudio.setBackground(ThemeManager.createCardDrawable(this, theme.surfaceColor, theme.primaryColor, 17));
        }

        View bottomNavBar = findViewById(R.id.bottomNavLayout);
        if (bottomNavBar != null) bottomNavBar.setBackgroundColor(theme.backgroundColor);

        TextView btnCenterPlus = findViewById(R.id.btnCenterPlus);
        if (btnCenterPlus != null) {
            GradientDrawable plusOval = new GradientDrawable();
            plusOval.setShape(GradientDrawable.OVAL);
            plusOval.setColor(theme.secondaryColor);
            btnCenterPlus.setBackground(plusOval);
            btnCenterPlus.setTextColor(theme.getButtonTextColor(theme.secondaryColor));
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIncomingIntent(intent);
    }

    private String lastImportedClip = "";

    private void checkClipboardForWybCode() {
        try {
            ClipboardManager cb = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            if (cb != null && cb.hasPrimaryClip() && cb.getPrimaryClip() != null && cb.getPrimaryClip().getItemCount() > 0) {
                CharSequence text = cb.getPrimaryClip().getItemAt(0).getText();
                if (text != null) {
                    String str = text.toString().trim();
                    if (str.contains("WYB1-") && !str.equals(lastImportedClip)) {
                        lastImportedClip = str;
                        ShareImportManager.SharedPayload payload = ShareImportManager.decodeFromTextOrCode(this, str);
                        if (payload != null && (!payload.cards.isEmpty() || payload.customMatch != null)) {
                            dispatchImportPayload(payload);
                        }
                    }
                }
            }
        } catch (Exception ignored) {}
    }

    @Override
    protected void onResume() {
        super.onResume();
        applyThemeToShell();
        if (bgmController != null) bgmController.syncState();
        refreshCurrentTab();
        AppUpdateManager.resumePendingInstallIfAllowed(this);
        checkClipboardForWybCode();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (bgmController != null) {
            bgmController.onAppBackground();
        }
    }

    private void switchTab(int index) {
        currentTab = index;
        ThemeManager.AppTheme theme = ThemeManager.getActiveTheme(this);
        int activeColor = theme.textColor;
        int inactiveColor = theme.getMutedTextColor();

        tabBtnArena.setTextColor(index == 0 ? activeColor : inactiveColor);
        tabBtnIdols.setTextColor(index == 1 ? activeColor : inactiveColor);
        tabBtnGroups.setTextColor(index == 2 ? activeColor : inactiveColor);
        tabBtnHallOfFame.setTextColor(index == 3 ? activeColor : inactiveColor);

        tabBtnArena.setTypeface(null, index == 0 ? Typeface.BOLD : Typeface.NORMAL);
        tabBtnIdols.setTypeface(null, index == 1 ? Typeface.BOLD : Typeface.NORMAL);
        tabBtnGroups.setTypeface(null, index == 2 ? Typeface.BOLD : Typeface.NORMAL);
        tabBtnHallOfFame.setTypeface(null, index == 3 ? Typeface.BOLD : Typeface.NORMAL);

        refreshCurrentTab();
    }

    private void refreshCurrentTab() {
        mainContentContainer.removeAllViews();
        switch (currentTab) {
            case 0:
                renderArenaTab();
                break;
            case 1:
                renderIdolsTab();
                break;
            case 2:
                renderGroupsTab();
                break;
            case 3:
                renderHallOfFameTab();
                break;
        }
    }

    // =========================================================================
    // TAB 1: HOME / ARENA (Clean Moshicam + Vibrary Layout)
    // =========================================================================
    private void renderArenaTab() {
        ThemeManager.AppTheme theme = ThemeManager.getActiveTheme(this);
        ScrollView scroll = new ScrollView(this);
        scroll.setVerticalScrollBarEnabled(false);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(8), dp(18), dp(28));

        // 1. Clean Pill Search Bar (Moshicam / Vibrary style)
        TextView searchBarPill = new TextView(this);
        searchBarPill.setText("🔍  " + getString(R.string.search_arena_hint));
        searchBarPill.setTextColor(theme.getMutedTextColor());
        searchBarPill.setTextSize(13.5f);
        searchBarPill.setGravity(Gravity.CENTER_VERTICAL);
        searchBarPill.setBackground(ThemeManager.createPillDrawable(this, theme.surfaceColor, 16));
        searchBarPill.setPadding(dp(16), dp(12), dp(16), dp(12));
        LinearLayout.LayoutParams sbLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        sbLp.setMargins(0, 0, 0, dp(16));
        searchBarPill.setLayoutParams(sbLp);
        searchBarPill.setOnClickListener(v -> switchTab(1));
        root.addView(searchBarPill);

        // 2. Hero Banner Card (Moshicam style with Polaroid preview & big CTA)
        LinearLayout heroBanner = new LinearLayout(this);
        heroBanner.setOrientation(LinearLayout.HORIZONTAL);
        heroBanner.setGravity(Gravity.CENTER_VERTICAL);
        GradientDrawable heroGd = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{theme.surfaceColor, theme.backgroundColor});
        heroGd.setCornerRadius(dp(22));
        heroGd.setStroke(dp(1), theme.primaryColor);
        heroBanner.setBackground(heroGd);
        heroBanner.setPadding(dp(18), dp(18), dp(16), dp(18));
        LinearLayout.LayoutParams hbLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        hbLp.setMargins(0, 0, 0, dp(16));
        heroBanner.setLayoutParams(hbLp);

        LinearLayout heroLeft = new LinearLayout(this);
        heroLeft.setOrientation(LinearLayout.VERTICAL);
        heroLeft.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView tvHeroBadge = new TextView(this);
        tvHeroBadge.setText("✨ WHO IS YOUR BIAS? IT'S ME!");
        tvHeroBadge.setTextColor(theme.primaryColor);
        tvHeroBadge.setTextSize(10.5f);
        tvHeroBadge.setTypeface(Typeface.DEFAULT_BOLD);
        heroLeft.addView(tvHeroBadge);

        TextView tvHeroTitle = new TextView(this);
        tvHeroTitle.setText(getString(R.string.hero_title));
        tvHeroTitle.setTextColor(theme.textColor);
        tvHeroTitle.setTextSize(20f);
        tvHeroTitle.setTypeface(Typeface.create("sans-serif-black", Typeface.BOLD));
        tvHeroTitle.setPadding(0, dp(4), 0, dp(4));
        heroLeft.addView(tvHeroTitle);

        List<IdolCard> allIdols = repo.getAllIdols();
        TextView tvHeroSub = new TextView(this);
        tvHeroSub.setText(getString(R.string.hero_subtitle, allIdols.size(), repo.getAllGroups().size()));
        tvHeroSub.setTextColor(theme.getMutedTextColor());
        tvHeroSub.setTextSize(12f);
        tvHeroSub.setPadding(0, 0, 0, dp(12));
        heroLeft.addView(tvHeroSub);

        TextView btnHeroPlay = new TextView(this);
        btnHeroPlay.setText(getString(R.string.hero_btn_play));
        btnHeroPlay.setTextColor(theme.getButtonTextColor(theme.primaryColor));
        btnHeroPlay.setTypeface(Typeface.DEFAULT_BOLD);
        btnHeroPlay.setTextSize(13f);
        btnHeroPlay.setGravity(Gravity.CENTER);
        btnHeroPlay.setBackground(ThemeManager.createPillDrawable(this, theme.primaryColor, 20));
        btnHeroPlay.setPadding(dp(18), dp(10), dp(18), dp(10));
        btnHeroPlay.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        btnHeroPlay.setOnClickListener(v -> showQuickMatchSetupSheet());
        heroLeft.addView(btnHeroPlay);

        heroBanner.addView(heroLeft);

        // Tilted Polaroid Preview on Hero Right
        if (!allIdols.isEmpty()) {
            CardView polaroid = new CardView(this);
            polaroid.setRadius(dp(16));
            polaroid.setCardElevation(dp(4));
            polaroid.setRotation(5f);
            LinearLayout.LayoutParams polLp = new LinearLayout.LayoutParams(dp(92), dp(116));
            polLp.setMargins(dp(10), dp(4), dp(4), dp(4));
            polaroid.setLayoutParams(polLp);

            ImageView ivPol = new ImageView(this);
            ivPol.setLayoutParams(new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            ivPol.setScaleType(ImageView.ScaleType.CENTER_CROP);
            IdolCard heroIdol = repo.getIdolById("idol_illit_minju");
            if (heroIdol == null) heroIdol = allIdols.get(0);
            CardImageLoader.loadCardImage(this, heroIdol, ivPol);
            polaroid.addView(ivPol);
            heroBanner.addView(polaroid);
        }
        heroBanner.setOnClickListener(v -> showQuickMatchSetupSheet());
        root.addView(heroBanner);

        // 3. Three Clean Rounded Square Tiles (Moshicam style: [Partida CPU] [Crear Partida] [WhatsApp])
        LinearLayout tilesRow = new LinearLayout(this);
        tilesRow.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams trLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        trLp.setMargins(0, 0, 0, dp(16));
        tilesRow.setLayoutParams(trLp);

        tilesRow.addView(buildMoshiActionTile("⚡", getString(R.string.tile_cpu_match), "#FFF0F5", "#F43F5E", null, this::showQuickMatchSetupSheet));
        View spacer1 = new View(this);
        spacer1.setLayoutParams(new LinearLayout.LayoutParams(dp(10), 1));
        tilesRow.addView(spacer1);
        tilesRow.addView(buildMoshiActionTile("🎯", getString(R.string.tile_create_match), "#FFFBEB", "#D97706", null, this::showVibraryStyleCustomMatchBuilder));
        View spacer2 = new View(this);
        spacer2.setLayoutParams(new LinearLayout.LayoutParams(dp(10), 1));
        tilesRow.addView(spacer2);
        tilesRow.addView(buildMoshiActionTile("📥", "WhatsApp", "#EFF6FF", "#2563EB", "NUEVO", this::showImportHubDialog));
        root.addView(tilesRow);

        // 4. Promo Strip Banner (Moshicam "Moshi Cards NUEVO — Hay cartas nuevas" style)
        LinearLayout gachaStrip = new LinearLayout(this);
        gachaStrip.setOrientation(LinearLayout.HORIZONTAL);
        gachaStrip.setGravity(Gravity.CENTER_VERTICAL);
        GradientDrawable stripGd = new GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{theme.surfaceColor, theme.backgroundColor});
        stripGd.setCornerRadius(dp(16));
        stripGd.setStroke(dp(1), theme.primaryColor);
        gachaStrip.setBackground(stripGd);
        gachaStrip.setPadding(dp(16), dp(13), dp(16), dp(13));
        LinearLayout.LayoutParams gsLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        gsLp.setMargins(0, 0, 0, dp(22));
        gachaStrip.setLayoutParams(gsLp);

        TextView tvGachaIcon = new TextView(this);
        tvGachaIcon.setText(getString(R.string.title_photocard_gacha));
        tvGachaIcon.setTextColor(theme.textColor);
        tvGachaIcon.setTypeface(Typeface.DEFAULT_BOLD);
        tvGachaIcon.setTextSize(13.5f);
        gachaStrip.addView(tvGachaIcon);

        TextView tvNewBadge = new TextView(this);
        tvNewBadge.setText(getString(R.string.badge_new));
        tvNewBadge.setTextColor(theme.getButtonTextColor(theme.secondaryColor));
        tvNewBadge.setTextSize(9.5f);
        tvNewBadge.setTypeface(Typeface.DEFAULT_BOLD);
        tvNewBadge.setGravity(Gravity.CENTER);
        tvNewBadge.setBackground(ThemeManager.createPillDrawable(this, theme.secondaryColor, 10));
        tvNewBadge.setPadding(dp(7), dp(2), dp(7), dp(2));
        LinearLayout.LayoutParams nbLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        nbLp.setMargins(dp(8), 0, 0, 0);
        tvNewBadge.setLayoutParams(nbLp);
        gachaStrip.addView(tvNewBadge);

        TextView tvGachaRight = new TextView(this);
        SharedPreferences wybPrefs = getSharedPreferences("wyb_prefs", MODE_PRIVATE);
        long gachaCooldown = 100 * 1000L; // Free gacha every 100 seconds!
        Runnable updateGachaTimer = new Runnable() {
            @Override
            public void run() {
                if (isFinishing() || (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 && isDestroyed())) return;
                long lastGacha = wybPrefs.getLong("last_gacha_open_time", 0);
                long gachaRem = (lastGacha + gachaCooldown) - System.currentTimeMillis();
                if (gachaRem > 0) {
                    long gm = gachaRem / (60 * 1000L);
                    long gs = (gachaRem % (60 * 1000L)) / 1000L;
                    String timeStr = gm > 0 ? String.format(Locale.ROOT, "%02dm %02ds", gm, gs) : String.format(Locale.ROOT, "%ds", gs);
                    tvGachaRight.setText(getString(R.string.gacha_cooldown_strip, timeStr));
                    tvGachaRight.postDelayed(this, 1000L);
                } else {
                    tvGachaRight.setText(getString(R.string.gacha_open_ready));
                }
            }
        };
        tvGachaRight.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
            @Override
            public void onViewAttachedToWindow(View v) {
                tvGachaRight.post(updateGachaTimer);
            }
            @Override
            public void onViewDetachedFromWindow(View v) {
                tvGachaRight.removeCallbacks(updateGachaTimer);
            }
        });
        updateGachaTimer.run();
        tvGachaRight.setTextColor(theme.primaryColor);
        tvGachaRight.setTypeface(Typeface.DEFAULT_BOLD);
        tvGachaRight.setTextSize(12.5f);
        tvGachaRight.setGravity(Gravity.END);
        tvGachaRight.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        gachaStrip.addView(tvGachaRight);

        // Instant tactile bounce & haptic feedback on touch
        gachaStrip.setOnClickListener(v -> {
            triggerHapticFeedback();
            v.animate()
                    .scaleX(0.96f).scaleY(0.96f).setDuration(70)
                    .withEndAction(() -> v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(120).start())
                    .start();
            showPhotocardGachaDialog();
        });

        // Secret Dev option: long press to enter developer passcode (2468)
        gachaStrip.setOnLongClickListener(v -> {
            triggerHapticFeedback();
            v.animate()
                    .scaleX(0.94f).scaleY(0.94f).setDuration(80)
                    .withEndAction(() -> v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(120).start())
                    .start();
            showSecretDevGachaDialog();
            return true;
        });
        root.addView(gachaStrip);

        // 5. Section: Matches & Challenges (Rotating daily with daySeed)
        addSectionHeaderRow(root, getString(R.string.section_matches_title), getString(R.string.section_create_btn), this::showVibraryStyleCustomMatchBuilder);

        HorizontalScrollView matchesScroll = new HorizontalScrollView(this);
        matchesScroll.setHorizontalScrollBarEnabled(false);
        LinearLayout matchesRow = new LinearLayout(this);
        matchesRow.setOrientation(LinearLayout.HORIZONTAL);
        matchesRow.setPadding(0, 0, 0, dp(20));

        Calendar dayCal = Calendar.getInstance();
        long daySeed = dayCal.get(Calendar.YEAR) * 1000L + dayCal.get(Calendar.DAY_OF_YEAR);

        List<CustomMatch> allMatches = new ArrayList<>(repo.getCustomMatches());
        List<CustomMatch> userMatches = new ArrayList<>();
        List<CustomMatch> defaultMatches = new ArrayList<>();
        for (CustomMatch cm : allMatches) {
            String mid = cm.getId();
            if (mid != null && (mid.startsWith("custom_") || mid.startsWith("match_") || !mid.startsWith("default_"))) {
                userMatches.add(cm);
            } else {
                defaultMatches.add(cm);
            }
        }
        // Most recent user & imported matches appear right first!
        Collections.reverse(userMatches);
        Collections.shuffle(defaultMatches, new Random(daySeed));

        List<CustomMatch> dailyMatches = new ArrayList<>(userMatches);
        dailyMatches.addAll(defaultMatches);
        for (CustomMatch cm : dailyMatches) {
            matchesRow.addView(buildMoshiDeckCard(cm));
        }
        matchesScroll.addView(matchesRow);
        root.addView(matchesScroll);

        // 6. Section: Batallas por Compañía & Posición (Rotating daily with daySeed)
        addSectionHeaderRow(root, getString(R.string.special_battles_title), getString(R.string.special_battles_view_groups), () -> switchTab(2));

        HorizontalScrollView specialScroll = new HorizontalScrollView(this);
        specialScroll.setHorizontalScrollBarEnabled(false);
        LinearLayout specialRow = new LinearLayout(this);
        specialRow.setOrientation(LinearLayout.HORIZONTAL);
        specialRow.setPadding(0, 0, 0, dp(10));

        class SpecialBattleDef {
            final String title, desc, filter, heroIdol;
            SpecialBattleDef(String t, String d, String f, String h) {
                title = t; desc = d; filter = f; heroIdol = h;
            }
        }
        List<SpecialBattleDef> specials = new ArrayList<>();
        specials.add(new SpecialBattleDef("HYBE Labels", "ILLIT • NewJeans • BTS • ENHYPEN", "HYBE", "idol_illit_wonhee"));
        specials.add(new SpecialBattleDef("YG Entertainment", "BLACKPINK • BABYMONSTER • BIGBANG", "YG Entertainment", "idol_bp_jennie"));
        specials.add(new SpecialBattleDef("JYP Entertainment", "TWICE • Stray Kids • NEXZ • NMIXX", "JYP Entertainment", "idol_skz_felix"));
        specials.add(new SpecialBattleDef("SM Entertainment", "aespa • RIIZE • NCT • Red Velvet", "SM Entertainment", "idol_aespa_karina"));
        specials.add(new SpecialBattleDef("THEBLACKLABEL", "MEOVV • Somi • izna • Rosé", "THEBLACKLABEL", "idol_meovv_ella"));
        specials.add(new SpecialBattleDef("5th Gen Queens & Kings", "ILLIT • NEXZ • KATSEYE • KiiiKiii", "5th Gen", "idol_rescene_woni"));
        Collections.shuffle(specials, new Random(daySeed + 77L));

        for (SpecialBattleDef s : specials) {
            addSpecialCarouselCard(specialRow, s.title, s.desc, s.filter, s.heroIdol);
        }

        specialScroll.addView(specialRow);
        root.addView(specialScroll);

        scroll.addView(root);
        mainContentContainer.addView(scroll);
    }

    private View buildMoshiActionTile(String emoji, String title, String bgHex, String strokeHex, String badgeText, Runnable onClick) {
        ThemeManager.AppTheme theme = ThemeManager.getActiveTheme(this);
        FrameLayout tile = new FrameLayout(this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(90), 1f);
        tile.setLayoutParams(lp);

        GradientDrawable gd = new GradientDrawable();
        gd.setColor(ThemeManager.isColorDark(theme.backgroundColor) ? theme.surfaceColor : Color.parseColor(bgHex));
        gd.setCornerRadius(dp(18));
        gd.setStroke(dp(1), Color.parseColor("#22000000"));
        tile.setBackground(gd);

        LinearLayout inner = new LinearLayout(this);
        inner.setOrientation(LinearLayout.VERTICAL);
        inner.setGravity(Gravity.CENTER);
        inner.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, Gravity.CENTER));
        inner.setPadding(dp(6), dp(6), dp(6), dp(6));

        TextView tvEmoji = new TextView(this);
        tvEmoji.setText(emoji);
        tvEmoji.setTextSize(24f);
        tvEmoji.setGravity(Gravity.CENTER);
        tvEmoji.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        inner.addView(tvEmoji);

        TextView tvLabel = new TextView(this);
        tvLabel.setText(title);
        tvLabel.setTextColor(ThemeManager.isColorDark(theme.backgroundColor) ? theme.textColor : Color.parseColor("#111827"));
        tvLabel.setTextSize(12.5f);
        tvLabel.setTypeface(Typeface.DEFAULT_BOLD);
        tvLabel.setGravity(Gravity.CENTER);
        tvLabel.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        tvLabel.setSingleLine(true);
        tvLabel.setPadding(0, dp(4), 0, 0);
        tvLabel.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        inner.addView(tvLabel);

        tile.addView(inner);

        if (badgeText != null) {
            TextView tvBadge = new TextView(this);
            tvBadge.setText(badgeText);
            tvBadge.setTextColor(theme.getButtonTextColor(theme.secondaryColor));
            tvBadge.setTextSize(8.5f);
            tvBadge.setTypeface(Typeface.DEFAULT_BOLD);
            tvBadge.setGravity(Gravity.CENTER);
            tvBadge.setBackground(ThemeManager.createPillDrawable(this, theme.secondaryColor, 10));
            tvBadge.setPadding(dp(6), dp(2), dp(6), dp(2));
            FrameLayout.LayoutParams bLp = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP | Gravity.END);
            bLp.setMargins(0, dp(6), dp(6), 0);
            tvBadge.setLayoutParams(bLp);
            tile.addView(tvBadge);
        }

        tile.setOnClickListener(v -> onClick.run());
        return tile;
    }

    private void addSectionHeaderRow(LinearLayout parent, String title, String actionText, Runnable onAction) {
        ThemeManager.AppTheme theme = ThemeManager.getActiveTheme(this);
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, 0, 0, dp(10));

        TextView tvTitle = new TextView(this);
        tvTitle.setText(title);
        tvTitle.setTextColor(theme.textColor);
        tvTitle.setTextSize(17f);
        tvTitle.setTypeface(Typeface.DEFAULT_BOLD);
        tvTitle.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        row.addView(tvTitle);

        TextView tvAction = new TextView(this);
        tvAction.setText(actionText);
        tvAction.setTextColor(theme.primaryColor);
        tvAction.setTextSize(13f);
        tvAction.setTypeface(Typeface.DEFAULT_BOLD);
        tvAction.setOnClickListener(v -> onAction.run());
        row.addView(tvAction);

        parent.addView(row);
    }

    private View buildMoshiDeckCard(CustomMatch cm) {
        ThemeManager.AppTheme theme = ThemeManager.getActiveTheme(this);
        CardView card = new CardView(this);
        card.setRadius(dp(18));
        card.setCardElevation(dp(2));
        card.setCardBackgroundColor(theme.surfaceColor);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(168), ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, dp(12), 0);
        card.setLayoutParams(lp);

        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);

        FrameLayout imgBox = new FrameLayout(this);
        imgBox.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(156)));

        ImageView iv = new ImageView(this);
        iv.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
        IdolCard coverIdol = null;
        if (!cm.getCardIds().isEmpty()) {
            coverIdol = repo.getIdolById(cm.getCardIds().get(0));
        }
        if (coverIdol != null) {
            CardImageLoader.loadCardImage(this, coverIdol, iv);
        }
        imgBox.addView(iv);

        // Pill badge on top-left (Moshicam style)
        TextView tvTag = new TextView(this);
        tvTag.setText(getString(R.string.deck_cards_badge, cm.getCardIds().size()));
        tvTag.setTextColor(theme.getButtonTextColor(theme.secondaryColor));
        tvTag.setTextSize(10f);
        tvTag.setTypeface(Typeface.DEFAULT_BOLD);
        GradientDrawable tagGd = new GradientDrawable();
        tagGd.setColor(theme.secondaryColor);
        tagGd.setCornerRadius(dp(12));
        tvTag.setBackground(tagGd);
        tvTag.setPadding(dp(8), dp(3), dp(8), dp(3));
        FrameLayout.LayoutParams tLp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP | Gravity.START);
        tLp.setMargins(dp(8), dp(8), 0, 0);
        tvTag.setLayoutParams(tLp);
        imgBox.addView(tvTag);

        // Pill badge on top-right with Match Short ID (e.g. #B8A1)
        TextView tvIdBadge = new TextView(this);
        tvIdBadge.setText(cm.getShortId());
        tvIdBadge.setTextColor(Color.WHITE);
        tvIdBadge.setTextSize(9.5f);
        tvIdBadge.setTypeface(Typeface.DEFAULT_BOLD);
        GradientDrawable idGd = new GradientDrawable();
        idGd.setColor(Color.parseColor("#99111827"));
        idGd.setCornerRadius(dp(12));
        tvIdBadge.setBackground(idGd);
        tvIdBadge.setPadding(dp(7), dp(3), dp(7), dp(3));
        FrameLayout.LayoutParams idLp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP | Gravity.END);
        idLp.setMargins(0, dp(8), dp(8), 0);
        tvIdBadge.setLayoutParams(idLp);
        imgBox.addView(tvIdBadge);

        col.addView(imgBox);

        LinearLayout footer = new LinearLayout(this);
        footer.setOrientation(LinearLayout.VERTICAL);
        footer.setPadding(dp(10), dp(10), dp(10), dp(10));

        TextView tvTitle = new TextView(this);
        tvTitle.setText(cm.getTitle());
        tvTitle.setTextColor(theme.textColor);
        tvTitle.setTextSize(12.5f);
        tvTitle.setTypeface(Typeface.DEFAULT_BOLD);
        tvTitle.setMaxLines(1);
        tvTitle.setEllipsize(TextUtils.TruncateAt.END);
        footer.addView(tvTitle);

        TextView tvSubId = new TextView(this);
        tvSubId.setText(cm.getShortId() + " • " + cm.getCardIds().size() + " Idols");
        tvSubId.setTextColor(theme.getMutedTextColor());
        tvSubId.setTextSize(10.5f);
        footer.addView(tvSubId);

        LinearLayout actRow = new LinearLayout(this);
        actRow.setOrientation(LinearLayout.HORIZONTAL);
        actRow.setGravity(Gravity.CENTER_VERTICAL);
        actRow.setPadding(0, dp(4), 0, 0);

        TextView tvSub = new TextView(this);
        tvSub.setText(getString(R.string.deck_play));
        tvSub.setTextColor(theme.primaryColor);
        tvSub.setTextSize(11.5f);
        tvSub.setTypeface(Typeface.DEFAULT_BOLD);
        tvSub.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        actRow.addView(tvSub);

        TextView btnShare = new TextView(this);
        btnShare.setText("📤");
        btnShare.setTextSize(13f);
        btnShare.setPadding(dp(4), 0, dp(4), 0);
        btnShare.setOnClickListener(v -> {
            List<IdolCard> cards = new ArrayList<>();
            for (String cid : cm.getCardIds()) {
                IdolCard c = repo.getIdolById(cid);
                if (c != null) cards.add(c);
            }
            showShareChoiceDialog(cm.getTitle(), cm, cards);
        });
        actRow.addView(btnShare);

        footer.addView(actRow);
        col.addView(footer);
        card.addView(col);

        card.setOnClickListener(v -> {
            List<IdolCard> roster = new ArrayList<>();
            for (String cid : cm.getCardIds()) {
                IdolCard c = repo.getIdolById(cid);
                if (c != null) roster.add(c);
            }
            launchMatch(cm.getTitle(), cm.getGameMode(), roster);
        });
        card.setOnLongClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle(cm.getTitle())
                    .setItems(new CharSequence[]{
                            getString(R.string.menu_play_match),
                            getString(R.string.menu_share_whatsapp),
                            getString(R.string.menu_delete_match)
                    }, (d, which) -> {
                        if (which == 0) card.performClick();
                        else if (which == 1) btnShare.performClick();
                        else if (which == 2) {
                            repo.deleteCustomMatch(cm.getId());
                            refreshCurrentTab();
                        }
                    }).show();
            return true;
        });

        return card;
    }

    private void addSpecialCarouselCard(LinearLayout parent, String title, String subtitle, String filterKey, String coverIdolId) {
        ThemeManager.AppTheme theme = ThemeManager.getActiveTheme(this);
        CardView card = new CardView(this);
        card.setRadius(dp(18));
        card.setCardElevation(dp(2));
        card.setCardBackgroundColor(theme.surfaceColor);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(158), ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, dp(12), 0);
        card.setLayoutParams(lp);

        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);

        ImageView iv = new ImageView(this);
        iv.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(148)));
        iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
        IdolCard cover = repo.getIdolById(coverIdolId);
        if (cover != null) CardImageLoader.loadCardImage(this, cover, iv);
        col.addView(iv);

        LinearLayout footer = new LinearLayout(this);
        footer.setOrientation(LinearLayout.VERTICAL);
        footer.setPadding(dp(10), dp(8), dp(10), dp(10));

        TextView tvT = new TextView(this);
        tvT.setText(title);
        tvT.setTextColor(theme.textColor);
        tvT.setTextSize(12.5f);
        tvT.setTypeface(Typeface.DEFAULT_BOLD);
        tvT.setSingleLine(true);
        footer.addView(tvT);

        TextView tvS = new TextView(this);
        tvS.setText(subtitle);
        tvS.setTextColor(theme.getMutedTextColor());
        tvS.setTextSize(10.5f);
        tvS.setSingleLine(true);
        tvS.setEllipsize(TextUtils.TruncateAt.END);
        footer.addView(tvS);

        col.addView(footer);
        card.addView(col);

        card.setOnClickListener(v -> {
            List<IdolCard> pool = repo.getIdolsByFilter(filterKey);
            Collections.shuffle(pool);
            if (pool.size() > 16) pool = pool.subList(0, 16);
            launchMatch(title + " Battle", "BRACKET", pool);
        });
        parent.addView(card);
    }

    // =========================================================================
    // TAB 2: IDOLS & PHOTOCARD FEED (Exact Vibrary 2-Column Photo Grid!)
    // =========================================================================
    private void renderIdolsTab() {
        ThemeManager.AppTheme theme = ThemeManager.getActiveTheme(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        root.setPadding(dp(16), dp(6), dp(16), 0);

        // 1. Clean Pill Search Bar with Clear '✕' Button
        EditText etSearch = new EditText(this);
        etSearch.setHint(getString(R.string.hint_search_idols));
        etSearch.setText(idolSearchQuery);
        etSearch.setTextColor(theme.textColor);
        etSearch.setHintTextColor(theme.getMutedTextColor());
        etSearch.setTextSize(13.5f);
        etSearch.setSingleLine(true);
        etSearch.setGravity(Gravity.CENTER_VERTICAL);
        etSearch.setBackground(ThemeManager.createPillDrawable(this, theme.surfaceColor, 16));
        etSearch.setPadding(dp(16), dp(11), dp(16), dp(11));
        root.addView(wrapSearchBarWithClearButton(etSearch, theme));

        // 2. Clean Vibrary Underlined Category Tabs (Todo | Favoritos | Girl Groups | Boy Groups | Solistas | Grupos)
        HorizontalScrollView filterScroll = new HorizontalScrollView(this);
        filterScroll.setHorizontalScrollBarEnabled(false);
        LinearLayout filterRow = new LinearLayout(this);
        filterRow.setOrientation(LinearLayout.HORIZONTAL);
        filterRow.setPadding(0, dp(10), 0, dp(8));

        List<String> chips = new ArrayList<>();
        chips.add("ALL");
        chips.add("Favorites");
        chips.add("Girl Groups");
        chips.add("Boy Groups");
        chips.add("Soloists");
        chips.add("HYBE");
        chips.add("YG Entertainment");
        chips.add("JYP Entertainment");
        chips.add("SM Entertainment");
        chips.add("THEBLACKLABEL");
        chips.addAll(repo.getAllGroups());

        for (String ch : chips) {
            boolean active = ch.equalsIgnoreCase(idolCategoryFilter);
            LinearLayout tabItem = new LinearLayout(this);
            tabItem.setOrientation(LinearLayout.VERTICAL);
            tabItem.setGravity(Gravity.CENTER_HORIZONTAL);
            tabItem.setPadding(dp(10), dp(4), dp(10), dp(4));

            TextView tvTab = new TextView(this);
            tvTab.setText("ALL".equalsIgnoreCase(ch) ? getString(R.string.filter_all) : ("Favorites".equalsIgnoreCase(ch) ? getString(R.string.filter_favorites) : ch));
            tvTab.setTextColor(active ? theme.textColor : theme.getMutedTextColor());
            tvTab.setTypeface(null, active ? Typeface.BOLD : Typeface.NORMAL);
            tvTab.setTextSize(13.5f);
            tabItem.addView(tvTab);

            View underline = new View(this);
            LinearLayout.LayoutParams ulLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, dp(3));
            ulLp.topMargin = dp(4);
            underline.setLayoutParams(ulLp);
            underline.setBackgroundColor(active ? theme.primaryColor : Color.TRANSPARENT);
            tabItem.addView(underline);

            tabItem.setOnClickListener(v -> {
                idolCategoryFilter = ch;
                refreshCurrentTab();
            });
            filterRow.addView(tabItem);
        }
        filterScroll.addView(filterRow);
        root.addView(filterScroll);

        // 3. Vibrary 2-Column Photo Grid (Ultra-smooth 60/120fps scrolling)
        ScrollView gridScroll = new ScrollView(this);
        gridScroll.setVerticalScrollBarEnabled(false);
        gridScroll.setSmoothScrollingEnabled(true);
        gridScroll.setNestedScrollingEnabled(true);
        gridScroll.setOverScrollMode(View.OVER_SCROLL_IF_CONTENT_SCROLLS);
        gridScroll.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        LinearLayout gridRowsContainer = new LinearLayout(this);
        gridRowsContainer.setOrientation(LinearLayout.VERTICAL);
        gridRowsContainer.setPadding(0, dp(4), 0, dp(24));

        populateTwoColumnPhotoGrid(gridRowsContainer);

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                idolSearchQuery = s.toString();
                populateTwoColumnPhotoGrid(gridRowsContainer);
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        gridScroll.addView(gridRowsContainer);
        root.addView(gridScroll);
        mainContentContainer.addView(root);
    }

    private List<IdolCard> getFilteredIdolsList() {
        List<IdolCard> base = repo.getIdolsByFilter(idolCategoryFilter);
        if (idolSearchQuery == null || idolSearchQuery.trim().isEmpty()) {
            return base;
        }
        String q = idolSearchQuery.toLowerCase(Locale.ROOT).trim();
        List<IdolCard> filtered = new ArrayList<>();
        for (IdolCard c : base) {
            if (c.getStageName().toLowerCase(Locale.ROOT).contains(q)
                    || c.getFullName().toLowerCase(Locale.ROOT).contains(q)
                    || c.getGroupName().toLowerCase(Locale.ROOT).contains(q)
                    || c.getCountry().toLowerCase(Locale.ROOT).contains(q)
                    || c.getRole().toLowerCase(Locale.ROOT).contains(q)
                    || c.getCollections().toLowerCase(Locale.ROOT).contains(q)) {
                filtered.add(c);
            }
        }
        return filtered;
    }

    private int idolsGridRenderToken = 0;
    private int groupsTabRenderToken = 0;

    private void populateTwoColumnPhotoGrid(LinearLayout container) {
        final int token = ++idolsGridRenderToken;
        container.removeAllViews();
        final List<IdolCard> list = getFilteredIdolsList();

        // Render first 24 cards (12 rows) immediately so screen is full (<5ms)
        final int initialLimit = Math.min(list.size(), 24);
        appendTwoColumnRowsChunk(container, list, 0, initialLimit);

        if (initialLimit < list.size()) {
            container.post(() -> {
                if (token != idolsGridRenderToken || isFinishing()) return;
                appendTwoColumnRowsChunk(container, list, initialLimit, list.size());
            });
        }
    }

    private void appendTwoColumnRowsChunk(LinearLayout container, List<IdolCard> list, int startIdx, int endIdx) {
        for (int i = startIdx; i < endIdx; i += 2) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            LinearLayout.LayoutParams rLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            rLp.setMargins(0, 0, 0, dp(12));
            row.setLayoutParams(rLp);

            View leftCard = buildVibraryPhotoGridCell(list.get(i));
            LinearLayout.LayoutParams lLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            lLp.setMargins(0, 0, dp(6), 0);
            leftCard.setLayoutParams(lLp);
            row.addView(leftCard);

            if (i + 1 < list.size()) {
                View rightCard = buildVibraryPhotoGridCell(list.get(i + 1));
                LinearLayout.LayoutParams rgLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
                rgLp.setMargins(dp(6), 0, 0, 0);
                rightCard.setLayoutParams(rgLp);
                row.addView(rightCard);
            } else {
                View empty = new View(this);
                empty.setLayoutParams(new LinearLayout.LayoutParams(0, 1, 1f));
                row.addView(empty);
            }
            container.addView(row);
        }
    }

    private View buildVibraryPhotoGridCell(IdolCard card) {
        ThemeManager.AppTheme theme = ThemeManager.getActiveTheme(this);
        LinearLayout cell = new LinearLayout(this);
        cell.setOrientation(LinearLayout.VERTICAL);

        CardView photoCard = new CardView(this);
        photoCard.setRadius(dp(18));
        photoCard.setCardElevation(dp(2));
        photoCard.setCardBackgroundColor(theme.surfaceColor);
        photoCard.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(220)));

        FrameLayout frame = new FrameLayout(this);
        frame.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        ImageView iv = new ImageView(this);
        iv.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
        CardImageLoader.loadCardImage(this, card, iv);
        frame.addView(iv);

        if (card.getPhotocards().size() > 1) {
            TextView tvPcBadge = new TextView(this);
            tvPcBadge.setText("📸 " + card.getPhotocards().size());
            tvPcBadge.setTextColor(Color.WHITE);
            tvPcBadge.setTextSize(10f);
            GradientDrawable gd = new GradientDrawable();
            gd.setColor(Color.parseColor("#88000000"));
            gd.setCornerRadius(dp(12));
            tvPcBadge.setBackground(gd);
            tvPcBadge.setPadding(dp(7), dp(3), dp(7), dp(3));
            FrameLayout.LayoutParams bLp = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP | Gravity.END);
            bLp.setMargins(0, dp(8), dp(8), 0);
            tvPcBadge.setLayoutParams(bLp);
            frame.addView(tvPcBadge);
        }

        photoCard.addView(frame);
        cell.addView(photoCard);

        // Minimalist Footer under Photo (Name • Group + Favorite Heart)
        LinearLayout footer = new LinearLayout(this);
        footer.setOrientation(LinearLayout.HORIZONTAL);
        footer.setGravity(Gravity.CENTER_VERTICAL);
        footer.setPadding(dp(4), dp(6), dp(4), dp(2));

        LinearLayout nameCol = new LinearLayout(this);
        nameCol.setOrientation(LinearLayout.VERTICAL);
        nameCol.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView tvName = new TextView(this);
        tvName.setText(card.getStageName());
        tvName.setTextColor(theme.textColor);
        tvName.setTextSize(14f);
        tvName.setTypeface(Typeface.DEFAULT_BOLD);
        tvName.setSingleLine(true);
        nameCol.addView(tvName);

        TextView tvGrp = new TextView(this);
        tvGrp.setText(card.isSoloist() ? "Solista" : card.getGroupName());
        tvGrp.setTextColor(theme.getMutedTextColor());
        tvGrp.setTextSize(11.5f);
        tvGrp.setSingleLine(true);
        nameCol.addView(tvGrp);

        footer.addView(nameCol);

        TextView tvFav = new TextView(this);
        tvFav.setText(card.isFavorite() ? "♥" : "♡");
        tvFav.setTextColor(card.isFavorite() ? theme.primaryColor : theme.getMutedTextColor());
        tvFav.setTextSize(17f);
        tvFav.setPadding(dp(6), dp(2), dp(4), dp(2));
        tvFav.setOnClickListener(v -> {
            card.setFavorite(!card.isFavorite());
            repo.saveOrUpdateIdol(card);
            tvFav.setText(card.isFavorite() ? "♥" : "♡");
            tvFav.setTextColor(card.isFavorite() ? theme.primaryColor : theme.getMutedTextColor());
        });
        footer.addView(tvFav);

        cell.addView(footer);
        cell.setOnClickListener(v -> showIdolDetailAndPhotocardBinderDialog(card));
        cell.setOnLongClickListener(v -> {
            showVibraryStyleShareIdolsDialog();
            return true;
        });
        return cell;
    }

    // =========================================================================
    // TAB 3: ARTISTAS / GRUPOS (Exact Vibrary Squircle Member Grid per Group!)
    // =========================================================================
    private void renderGroupsTab() {
        ThemeManager.AppTheme theme = ThemeManager.getActiveTheme(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        root.setPadding(dp(18), dp(6), dp(18), 0);

        EditText etGroupSearch = new EditText(this);
        etGroupSearch.setHint(getString(R.string.hint_search_groups));
        etGroupSearch.setText(groupSearchQuery);
        etGroupSearch.setTextColor(theme.textColor);
        etGroupSearch.setHintTextColor(theme.getMutedTextColor());
        etGroupSearch.setTextSize(13.5f);
        etGroupSearch.setSingleLine(true);
        etGroupSearch.setGravity(Gravity.CENTER_VERTICAL);
        etGroupSearch.setBackground(ThemeManager.createPillDrawable(this, theme.surfaceColor, 16));
        etGroupSearch.setPadding(dp(16), dp(11), dp(16), dp(11));
        View wrappedGroupSearch = wrapSearchBarWithClearButton(etGroupSearch, theme);
        LinearLayout.LayoutParams sLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        sLp.setMargins(0, 0, 0, dp(12));
        wrappedGroupSearch.setLayoutParams(sLp);
        root.addView(wrappedGroupSearch);

        ScrollView scroll = new ScrollView(this);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.setSmoothScrollingEnabled(true);
        scroll.setNestedScrollingEnabled(true);
        scroll.setOverScrollMode(View.OVER_SCROLL_IF_CONTENT_SCROLLS);
        scroll.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        LinearLayout groupsContainer = new LinearLayout(this);
        groupsContainer.setOrientation(LinearLayout.VERTICAL);
        groupsContainer.setPadding(0, dp(4), 0, dp(28));

        populateVibraryGroupSections(groupsContainer);

        etGroupSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                groupSearchQuery = s.toString();
                populateVibraryGroupSections(groupsContainer);
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        scroll.addView(groupsContainer);
        root.addView(scroll);
        mainContentContainer.addView(root);
    }

    private void populateVibraryGroupSections(LinearLayout container) {
        final int token = ++groupsTabRenderToken;
        container.removeAllViews();
        String q = groupSearchQuery != null ? groupSearchQuery.toLowerCase(Locale.ROOT).trim() : "";

        List<String> groups = repo.getAllGroups();
        groups.add("Soloists");

        final List<String> matchedGroups = new ArrayList<>();
        final List<List<IdolCard>> matchedMembersList = new ArrayList<>();

        for (String groupName : groups) {
            List<IdolCard> members = repo.getIdolsByFilter(groupName);
            if (members.isEmpty()) continue;

            if (!q.isEmpty()) {
                boolean matchesGroup = groupName.toLowerCase(Locale.ROOT).contains(q);
                if (!matchesGroup) {
                    List<IdolCard> matchingMembers = new ArrayList<>();
                    for (IdolCard m : members) {
                        if (m.getStageName().toLowerCase(Locale.ROOT).contains(q)
                                || m.getCollections().toLowerCase(Locale.ROOT).contains(q)) {
                            matchingMembers.add(m);
                        }
                    }
                    if (matchingMembers.isEmpty()) continue;
                }
            }

            matchedGroups.add(groupName);
            matchedMembersList.add(members);
        }

        // Render first 8 groups immediately so switching to Artistas tab is instant (<10ms)
        int initialCount = Math.min(8, matchedGroups.size());
        for (int i = 0; i < initialCount; i++) {
            container.addView(buildVibraryGroupSection(matchedGroups.get(i), matchedMembersList.get(i)));
        }

        if (initialCount < matchedGroups.size()) {
            container.post(() -> {
                if (token != groupsTabRenderToken || isFinishing()) return;
                for (int i = initialCount; i < matchedGroups.size(); i++) {
                    container.addView(buildVibraryGroupSection(matchedGroups.get(i), matchedMembersList.get(i)));
                }
            });
        }
    }

    /**
     * Exact Vibrary Screenshot #1 & #4 layout:
     * Bold Group Title on left, pill button on right, and 4-column Squircle Avatar Grid below!
     */
    private View buildVibraryGroupSection(String groupName, List<IdolCard> members) {
        ThemeManager.AppTheme theme = ThemeManager.getActiveTheme(this);
        LinearLayout section = new LinearLayout(this);
        section.setOrientation(LinearLayout.VERTICAL);
        section.setPadding(0, dp(10), 0, dp(16));

        // Header Row: Group Name + [✓ Jugar] + [📤]
        LinearLayout headerRow = new LinearLayout(this);
        headerRow.setOrientation(LinearLayout.HORIZONTAL);
        headerRow.setGravity(Gravity.CENTER_VERTICAL);
        headerRow.setPadding(0, 0, 0, dp(12));

        TextView tvGroupTitle = new TextView(this);
        tvGroupTitle.setText(groupName);
        tvGroupTitle.setTextColor(theme.textColor);
        tvGroupTitle.setTextSize(18f);
        tvGroupTitle.setTypeface(Typeface.create("sans-serif-black", Typeface.BOLD));
        tvGroupTitle.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        headerRow.addView(tvGroupTitle);

        TextView btnPlayPill = new TextView(this);
        btnPlayPill.setText(getString(R.string.btn_play_group_count, members.size()));
        btnPlayPill.setTextColor(theme.textColor);
        btnPlayPill.setTextSize(11.5f);
        btnPlayPill.setTypeface(Typeface.DEFAULT_BOLD);
        btnPlayPill.setGravity(Gravity.CENTER);
        btnPlayPill.setBackground(ThemeManager.createPillDrawable(this, theme.surfaceColor, 16));
        btnPlayPill.setPadding(dp(12), dp(6), dp(12), dp(6));
        LinearLayout.LayoutParams ppLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        ppLp.setMargins(0, 0, dp(6), 0);
        btnPlayPill.setLayoutParams(ppLp);
        btnPlayPill.setOnClickListener(v -> {
            String mode = "KING_OF_THE_HILL";
            launchMatch("Who is your bias in " + groupName + "?", mode, members);
        });
        headerRow.addView(btnPlayPill);

        TextView btnSharePill = new TextView(this);
        btnSharePill.setText("📤");
        btnSharePill.setTextSize(12.5f);
        btnSharePill.setGravity(Gravity.CENTER);
        btnSharePill.setBackground(ThemeManager.createPillDrawable(this, theme.surfaceColor, 16));
        btnSharePill.setPadding(dp(10), dp(6), dp(10), dp(6));
        btnSharePill.setOnClickListener(v -> {
            List<String> ids = new ArrayList<>();
            for (IdolCard m : members) ids.add(m.getId());
            CustomMatch gm = new CustomMatch(null, "Who is your bias in " + groupName + "?", "Friend",
                    "KING_OF_THE_HILL", ids, System.currentTimeMillis());
            showShareChoiceDialog(groupName + " Pack", gm, members);
        });
        headerRow.addView(btnSharePill);

        section.addView(headerRow);

        // 4-Column Squircle Member Grid (Vibrary style!)
        int cols = 4;
        for (int i = 0; i < members.size(); i += cols) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            LinearLayout.LayoutParams rLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            rLp.setMargins(0, 0, 0, dp(12));
            row.setLayoutParams(rLp);

            for (int c = 0; c < cols; c++) {
                int idx = i + c;
                if (idx < members.size()) {
                    View cell = buildVibrarySquircleMemberCell(members.get(idx), null);
                    row.addView(cell);
                } else {
                    View empty = new View(this);
                    empty.setLayoutParams(new LinearLayout.LayoutParams(0, 1, 1f));
                    row.addView(empty);
                }
            }
            section.addView(row);
        }

        // Subtle divider line at bottom of group section
        View div = new View(this);
        div.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(1)));
        div.setBackgroundColor(theme.surfaceColor);
        section.addView(div);

        return section;
    }

    private View buildVibrarySquircleMemberCell(IdolCard member, Set<String> selectionSet) {
        return buildVibrarySquircleMemberCell(member, selectionSet, null);
    }

    private View buildVibrarySquircleMemberCell(IdolCard member, Set<String> selectionSet, Runnable onSelectionChanged) {
        ThemeManager.AppTheme theme = ThemeManager.getActiveTheme(this);
        LinearLayout cell = new LinearLayout(this);
        cell.setOrientation(LinearLayout.VERTICAL);
        cell.setGravity(Gravity.CENTER_HORIZONTAL);
        cell.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        cell.setPadding(dp(4), 0, dp(4), 0);

        FrameLayout avatarWrap = new FrameLayout(this);
        avatarWrap.setLayoutParams(new LinearLayout.LayoutParams(dp(74), dp(86)));

        CardView squircle = new CardView(this);
        squircle.setRadius(dp(22)); // Vibrary rounded squircle shape!
        squircle.setCardElevation(0);
        squircle.setCardBackgroundColor(theme.surfaceColor);
        squircle.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        ImageView iv = new ImageView(this);
        iv.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
        CardImageLoader.loadCardImage(this, member, iv);
        squircle.addView(iv);
        avatarWrap.addView(squircle);

        final View selBorder;
        final TextView checkBadge;
        if (selectionSet != null) {
            selBorder = new View(this);
            FrameLayout.LayoutParams borderLp = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            selBorder.setLayoutParams(borderLp);
            GradientDrawable borderGd = new GradientDrawable();
            borderGd.setCornerRadius(dp(22));
            borderGd.setStroke(dp(3), theme.primaryColor);
            borderGd.setColor(Color.TRANSPARENT);
            selBorder.setBackground(borderGd);
            selBorder.setElevation(dp(4));
            avatarWrap.addView(selBorder);

            checkBadge = new TextView(this);
            checkBadge.setGravity(Gravity.CENTER);
            checkBadge.setTextSize(11f);
            checkBadge.setTypeface(Typeface.DEFAULT_BOLD);
            FrameLayout.LayoutParams cbLp = new FrameLayout.LayoutParams(dp(24), dp(24), Gravity.BOTTOM | Gravity.END);
            cbLp.setMargins(0, 0, dp(3), dp(3));
            checkBadge.setLayoutParams(cbLp);
            checkBadge.setElevation(dp(8));
            avatarWrap.addView(checkBadge);

            updateSquircleSelectionVisuals(checkBadge, selBorder, selectionSet.contains(member.getId()));
        } else {
            selBorder = null;
            checkBadge = null;
        }

        cell.addView(avatarWrap);

        TextView tvName = new TextView(this);
        tvName.setText(member.getStageName());
        tvName.setTextColor(theme.textColor);
        tvName.setTextSize(12f);
        tvName.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        tvName.setGravity(Gravity.CENTER);
        tvName.setSingleLine(true);
        tvName.setEllipsize(TextUtils.TruncateAt.END);
        tvName.setPadding(0, dp(6), 0, 0);
        cell.addView(tvName);

        // Make whole cell and children clickable
        View.OnClickListener toggleClick = v -> {
            if (selectionSet != null) {
                if (selectionSet.contains(member.getId())) {
                    selectionSet.remove(member.getId());
                } else {
                    selectionSet.add(member.getId());
                }
                updateSquircleSelectionVisuals(checkBadge, selBorder, selectionSet.contains(member.getId()));
                if (onSelectionChanged != null) onSelectionChanged.run();
            } else {
                showIdolDetailAndPhotocardBinderDialog(member);
            }
        };

        cell.setOnClickListener(toggleClick);
        avatarWrap.setOnClickListener(toggleClick);
        squircle.setOnClickListener(toggleClick);
        iv.setOnClickListener(toggleClick);
        if (selBorder != null) selBorder.setOnClickListener(toggleClick);
        if (checkBadge != null) checkBadge.setOnClickListener(toggleClick);
        tvName.setOnClickListener(toggleClick);

        if (selectionSet == null) {
            View.OnLongClickListener longShare = v -> {
                showVibraryStyleShareIdolsDialog();
                return true;
            };
            cell.setOnLongClickListener(longShare);
            avatarWrap.setOnLongClickListener(longShare);
            squircle.setOnLongClickListener(longShare);
            iv.setOnLongClickListener(longShare);
            tvName.setOnLongClickListener(longShare);
        }

        return cell;
    }

    private void updateSquircleSelectionVisuals(TextView badge, View selBorder, boolean checked) {
        if (badge == null) return;
        ThemeManager.AppTheme theme = ThemeManager.getActiveTheme(this);
        if (selBorder != null) {
            selBorder.setVisibility(checked ? View.VISIBLE : View.GONE);
            selBorder.bringToFront();
        }
        GradientDrawable gd = new GradientDrawable();
        gd.setShape(GradientDrawable.OVAL);
        if (checked) {
            gd.setColor(theme.primaryColor);
            gd.setStroke(dp(2), Color.WHITE);
            badge.setBackground(gd);
            badge.setText("✓");
            badge.setTextColor(Color.WHITE);
        } else {
            gd.setColor(Color.parseColor("#991F2937"));
            gd.setStroke(dp(2), Color.WHITE);
            badge.setBackground(gd);
            badge.setText("");
        }
        badge.bringToFront();
    }

    // =========================================================================
    // TAB 4: MI PÁGINA (Exact Vibrary Screenshot #1 "Mi página" Layout + Custom Profile Photo & Theme Button!)
    // =========================================================================
    private void renderHallOfFameTab() {
        ThemeManager.AppTheme theme = ThemeManager.getActiveTheme(this);
        ScrollView scroll = new ScrollView(this);
        scroll.setVerticalScrollBarEnabled(false);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(10), dp(18), dp(28));

        // Centered Profile Header (Vibrary "Mi página" style)
        LinearLayout profileHeader = new LinearLayout(this);
        profileHeader.setOrientation(LinearLayout.VERTICAL);
        profileHeader.setGravity(Gravity.CENTER_HORIZONTAL);
        profileHeader.setPadding(0, dp(6), 0, dp(18));

        // Top Champion or Custom User Profile Avatar in Uncropped Squircle
        List<IdolCard> all = repo.getAllIdols();
        all.sort((a, b) -> {
            if (b.getTournamentsWon() != a.getTournamentsWon()) return Integer.compare(b.getTournamentsWon(), a.getTournamentsWon());
            if (b.isFavorite() != a.isFavorite()) return Boolean.compare(b.isFavorite(), a.isFavorite());
            return Integer.compare(b.getMatchesWon(), a.getMatchesWon());
        });
        IdolCard topBias = all.isEmpty() ? null : all.get(0);

        FrameLayout avatarOuter = new FrameLayout(this);
        LinearLayout.LayoutParams aoLp = new LinearLayout.LayoutParams(dp(106), dp(122));
        aoLp.bottomMargin = dp(10);
        avatarOuter.setLayoutParams(aoLp);

        CardView profileSquircle = new CardView(this);
        profileSquircle.setRadius(dp(26));
        profileSquircle.setCardElevation(dp(3));
        profileSquircle.setCardBackgroundColor(theme.surfaceColor);
        profileSquircle.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        ImageView ivTop = new ImageView(this);
        ivTop.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        ivTop.setScaleType(ImageView.ScaleType.CENTER_CROP);

        String customAvatarUri = repo.getCustomProfileAvatarUri();
        if (customAvatarUri != null && !customAvatarUri.trim().isEmpty()) {
            CardImageLoader.loadUriIntoImageView(this, customAvatarUri, ivTop, "Perfil", theme.primaryColor);
        } else if (topBias != null) {
            CardImageLoader.loadCardImage(this, topBias, ivTop);
        }
        profileSquircle.addView(ivTop);
        avatarOuter.addView(profileSquircle);

        // Small camera badge on corner of profile photo
        TextView camBadge = new TextView(this);
        camBadge.setText("📷");
        camBadge.setTextSize(11f);
        camBadge.setGravity(Gravity.CENTER);
        GradientDrawable camGd = new GradientDrawable();
        camGd.setShape(GradientDrawable.OVAL);
        camGd.setColor(theme.primaryColor);
        camGd.setStroke(dp(2), Color.WHITE);
        camBadge.setBackground(camGd);
        FrameLayout.LayoutParams cmLp = new FrameLayout.LayoutParams(dp(28), dp(28), Gravity.BOTTOM | Gravity.END);
        camBadge.setLayoutParams(cmLp);
        avatarOuter.addView(camBadge);

        avatarOuter.setOnClickListener(v -> showChangeUserProfilePhotoDialog());
        profileHeader.addView(avatarOuter);

        TextView tvMyBiasLabel = new TextView(this);
        tvMyBiasLabel.setText(topBias != null
                ? getString(R.string.profile_my_bias_label, topBias.getStageName(), topBias.getGroupName())
                : getString(R.string.nav_tab_profile));
        tvMyBiasLabel.setTextColor(theme.textColor);
        tvMyBiasLabel.setTextSize(16f);
        tvMyBiasLabel.setTypeface(Typeface.DEFAULT_BOLD);
        tvMyBiasLabel.setGravity(Gravity.CENTER);
        profileHeader.addView(tvMyBiasLabel);

        // Row 1 of Profile Actions: [📋 Change Photo] + [🎨 Theme Studio]
        LinearLayout profActRow1 = new LinearLayout(this);
        profActRow1.setOrientation(LinearLayout.HORIZONTAL);
        profActRow1.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams parLp1 = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        parLp1.topMargin = dp(8);
        profActRow1.setLayoutParams(parLp1);

        TextView btnChangeAvatar = new TextView(this);
        btnChangeAvatar.setText(getString(R.string.btn_change_avatar));
        btnChangeAvatar.setTextColor(theme.textColor);
        btnChangeAvatar.setTextSize(11.5f);
        btnChangeAvatar.setTypeface(Typeface.DEFAULT_BOLD);
        btnChangeAvatar.setGravity(Gravity.CENTER);
        btnChangeAvatar.setBackground(ThemeManager.createCardDrawable(this, theme.surfaceColor, theme.primaryColor, 16));
        btnChangeAvatar.setPadding(dp(10), dp(8), dp(10), dp(8));
        LinearLayout.LayoutParams bcaLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        bcaLp.rightMargin = dp(6);
        btnChangeAvatar.setLayoutParams(bcaLp);
        btnChangeAvatar.setOnClickListener(v -> showChangeUserProfilePhotoDialog());
        profActRow1.addView(btnChangeAvatar);

        TextView btnThemeColors = new TextView(this);
        btnThemeColors.setText(getString(R.string.btn_customize_theme));
        btnThemeColors.setTextColor(theme.textColor);
        btnThemeColors.setTextSize(11.5f);
        btnThemeColors.setTypeface(Typeface.DEFAULT_BOLD);
        btnThemeColors.setGravity(Gravity.CENTER);
        btnThemeColors.setBackground(ThemeManager.createCardDrawable(this, theme.surfaceColor, theme.secondaryColor, 16));
        btnThemeColors.setPadding(dp(10), dp(8), dp(10), dp(8));
        LinearLayout.LayoutParams btcLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        btcLp.leftMargin = dp(6);
        btnThemeColors.setLayoutParams(btcLp);
        btnThemeColors.setOnClickListener(v -> ThemeManager.showThemeStudioDialog(this, () -> {
            applyThemeToShell();
            refreshCurrentTab();
        }));
        profActRow1.addView(btnThemeColors);
        profileHeader.addView(profActRow1);

        // Row 2 of Profile Actions: [🔄 Check for Updates] + [🌐 Language]
        LinearLayout profActRow2 = new LinearLayout(this);
        profActRow2.setOrientation(LinearLayout.HORIZONTAL);
        profActRow2.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams parLp2 = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        parLp2.topMargin = dp(8);
        profActRow2.setLayoutParams(parLp2);

        String appVer = getAppVersionName();
        TextView btnCheckUpd = new TextView(this);
        btnCheckUpd.setText(getString(R.string.btn_check_updates) + " (v" + appVer + ")");
        btnCheckUpd.setTextColor(theme.textColor);
        btnCheckUpd.setTextSize(11.5f);
        btnCheckUpd.setTypeface(Typeface.DEFAULT_BOLD);
        btnCheckUpd.setGravity(Gravity.CENTER);
        btnCheckUpd.setBackground(ThemeManager.createCardDrawable(this, theme.surfaceColor, theme.cardBorderColor, 16));
        btnCheckUpd.setPadding(dp(10), dp(8), dp(10), dp(8));
        LinearLayout.LayoutParams bcuLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        bcuLp.rightMargin = dp(6);
        btnCheckUpd.setLayoutParams(bcuLp);
        btnCheckUpd.setOnClickListener(v -> AppUpdateManager.checkForUpdates(this, true));
        profActRow2.addView(btnCheckUpd);

        TextView btnLang = new TextView(this);
        btnLang.setText(getString(R.string.btn_select_language));
        btnLang.setTextColor(theme.textColor);
        btnLang.setTextSize(11.5f);
        btnLang.setTypeface(Typeface.DEFAULT_BOLD);
        btnLang.setGravity(Gravity.CENTER);
        btnLang.setBackground(ThemeManager.createCardDrawable(this, theme.surfaceColor, theme.cardBorderColor, 16));
        btnLang.setPadding(dp(10), dp(8), dp(10), dp(8));
        LinearLayout.LayoutParams blLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        blLp.leftMargin = dp(6);
        btnLang.setLayoutParams(blLp);
        btnLang.setOnClickListener(v -> LocaleManager.showLanguageDialog(this, () -> recreate()));
        profActRow2.addView(btnLang);
        profileHeader.addView(profActRow2);

        // Quick Share Idols Pill Button
        TextView btnShareMyIdols = new TextView(this);
        btnShareMyIdols.setText(getString(R.string.btn_share_idols_whatsapp));
        btnShareMyIdols.setTextColor(theme.getButtonTextColor(theme.primaryColor));
        btnShareMyIdols.setTextSize(12f);
        btnShareMyIdols.setTypeface(Typeface.DEFAULT_BOLD);
        btnShareMyIdols.setGravity(Gravity.CENTER);
        btnShareMyIdols.setBackground(ThemeManager.createPillDrawable(this, theme.primaryColor, 18));
        btnShareMyIdols.setPadding(dp(16), dp(9), dp(16), dp(9));
        LinearLayout.LayoutParams smLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        smLp.topMargin = dp(10);
        btnShareMyIdols.setLayoutParams(smLp);
        btnShareMyIdols.setOnClickListener(v -> showVibraryStyleShareIdolsDialog());
        profileHeader.addView(btnShareMyIdols);

        TextView tvAppVerBadge = new TextView(this);
        tvAppVerBadge.setText("Who's your Bias v" + appVer);
        tvAppVerBadge.setTextColor(theme.getMutedTextColor());
        tvAppVerBadge.setTextSize(11.5f);
        tvAppVerBadge.setTypeface(Typeface.DEFAULT_BOLD);
        tvAppVerBadge.setGravity(Gravity.CENTER);
        tvAppVerBadge.setPadding(0, dp(10), 0, dp(2));
        tvAppVerBadge.setOnClickListener(v -> {
            triggerHapticFeedback();
            showSecretDevGachaDialog();
        });
        profileHeader.addView(tvAppVerBadge);

        root.addView(profileHeader);

        // Vibrary Underlined Sub-Header ("Mi artista" / Hall of Fame grouped by Group)
        LinearLayout tabHeader = new LinearLayout(this);
        tabHeader.setOrientation(LinearLayout.VERTICAL);
        tabHeader.setPadding(0, 0, 0, dp(12));

        TextView tvSubTab = new TextView(this);
        tvSubTab.setText(getString(R.string.profile_my_artists_header));
        tvSubTab.setTextColor(theme.textColor);
        tvSubTab.setTextSize(15f);
        tvSubTab.setTypeface(Typeface.DEFAULT_BOLD);
        tabHeader.addView(tvSubTab);

        View goldBar = new View(this);
        LinearLayout.LayoutParams gbLp = new LinearLayout.LayoutParams(dp(80), dp(3));
        gbLp.topMargin = dp(4);
        goldBar.setLayoutParams(gbLp);
        goldBar.setBackgroundColor(theme.primaryColor);
        tabHeader.addView(goldBar);
        root.addView(tabHeader);

        // Collect favorites & tournament winners, grouped by Group just like Vibrary screenshot #1!
        Map<String, List<IdolCard>> groupedMyArtists = new LinkedHashMap<>();
        for (IdolCard c : all) {
            if (c.isFavorite() || c.getTournamentsWon() > 0 || c.getMatchesWon() > 0) {
                String g = c.getGroupName();
                if (!groupedMyArtists.containsKey(g)) groupedMyArtists.put(g, new ArrayList<>());
                groupedMyArtists.get(g).add(c);
            }
        }
        if (groupedMyArtists.isEmpty()) {
            for (String starterGrp : new String[]{"ILLIT", "NEXZ", "&TEAM", "(G)I-DLE", "aespa"}) {
                List<IdolCard> m = repo.getIdolsByFilter(starterGrp);
                if (!m.isEmpty()) groupedMyArtists.put(starterGrp, m);
            }
        }

        for (Map.Entry<String, List<IdolCard>> entry : groupedMyArtists.entrySet()) {
            root.addView(buildVibraryGroupSection(entry.getKey(), entry.getValue()));
        }

        scroll.addView(root);
        mainContentContainer.addView(scroll);
    }

    private void showChangeUserProfilePhotoDialog() {
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.dialog_change_avatar_title))
                .setItems(new String[]{
                        getString(R.string.dialog_avatar_option_paste),
                        getString(R.string.dialog_avatar_option_gallery),
                        getString(R.string.dialog_avatar_option_top_bias)
                }, (d, which) -> {
                    if (which == 0) {
                        String pasted = pasteImageOrUrlFromClipboard();
                        if (pasted != null && !pasted.isEmpty()) {
                            repo.setCustomProfileAvatarUri(pasted);
                            refreshCurrentTab();
                            Toast.makeText(this, "✨ " + getString(R.string.btn_change_avatar), Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(this, getString(R.string.dialog_avatar_option_paste), Toast.LENGTH_SHORT).show();
                        }
                    } else if (which == 1) {
                        Intent pick = new Intent(Intent.ACTION_GET_CONTENT);
                        pick.setType("image/*");
                        startActivityForResult(Intent.createChooser(pick, getString(R.string.dialog_change_avatar_title)), REQ_PICK_USER_AVATAR);
                    } else if (which == 2) {
                        repo.setCustomProfileAvatarUri("");
                        refreshCurrentTab();
                        Toast.makeText(this, getString(R.string.toast_avatar_using_top_bias), Toast.LENGTH_SHORT).show();
                    }
                })
                .show();
    }

    /**
     * Reads either a copied image URI (e.g. from Chrome, Gallery, WhatsApp, Gboard clipboard)
     * or a copied image URL string from the Android Clipboard!
     */
    private String pasteImageOrUrlFromClipboard() {
        ClipboardManager cb = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (cb == null || !cb.hasPrimaryClip() || cb.getPrimaryClip() == null || cb.getPrimaryClip().getItemCount() == 0) {
            return null;
        }
        ClipData clip = cb.getPrimaryClip();
        for (int i = 0; i < clip.getItemCount(); i++) {
            ClipData.Item item = clip.getItemAt(i);
            if (item.getUri() != null) {
                String saved = CardImageLoader.copyUriToInternalStorage(this, item.getUri());
                if (saved != null && !saved.isEmpty()) {
                    return saved;
                }
            }
            CharSequence cs = item.getText();
            if (cs == null) {
                cs = item.coerceToText(this);
            }
            if (cs != null) {
                String txt = cs.toString().trim();
                if (txt.startsWith("http://") || txt.startsWith("https://") || txt.startsWith("content://") || txt.startsWith("file://")) {
                    if (txt.startsWith("content://")) {
                        String saved = CardImageLoader.copyUriToInternalStorage(this, Uri.parse(txt));
                        if (saved != null && !saved.isEmpty()) return saved;
                    }
                    return txt;
                }
            }
        }
        return null;
    }

    // =========================================================================
    // DIALOGS: QUICK MATCH, VIBRARY MULTI-SELECT BUILDER, IDOL BINDER, SHARE/IMPORT
    // =========================================================================

    private View buildPlusActionCard(String emoji, String title, String subtitle, String iconBgHex, Runnable action) {
        ThemeManager.AppTheme theme = ThemeManager.getActiveTheme(this);
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setBackground(ThemeManager.createCardDrawable(this, theme.surfaceColor, theme.cardBorderColor, 18));
        card.setPadding(dp(14), dp(12), dp(14), dp(12));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, dp(10));
        card.setLayoutParams(lp);

        TextView tvIcon = new TextView(this);
        tvIcon.setText(emoji);
        tvIcon.setTextSize(18f);
        tvIcon.setGravity(Gravity.CENTER);
        GradientDrawable iBg = new GradientDrawable();
        iBg.setShape(GradientDrawable.OVAL);
        iBg.setColor(Color.parseColor(iconBgHex));
        tvIcon.setBackground(iBg);
        LinearLayout.LayoutParams icLp = new LinearLayout.LayoutParams(dp(42), dp(42));
        icLp.setMargins(0, 0, dp(14), 0);
        tvIcon.setLayoutParams(icLp);
        card.addView(tvIcon);

        LinearLayout textCol = new LinearLayout(this);
        textCol.setOrientation(LinearLayout.VERTICAL);
        textCol.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView tvT = new TextView(this);
        tvT.setText(title);
        tvT.setTextColor(theme.textColor);
        tvT.setTextSize(14f);
        tvT.setTypeface(Typeface.DEFAULT_BOLD);
        textCol.addView(tvT);

        TextView tvSub = new TextView(this);
        tvSub.setText(subtitle);
        tvSub.setTextColor(theme.getMutedTextColor());
        tvSub.setTextSize(11.5f);
        textCol.addView(tvSub);

        card.addView(textCol);
        card.setOnClickListener(v -> action.run());
        return card;
    }

    private void showWikiSearchAndImportDialog() {
        ThemeManager.AppTheme theme = ThemeManager.getActiveTheme(this);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackground(ThemeManager.createCardDrawable(this, theme.surfaceColor, theme.cardBorderColor, 26));
        int pad = dp(20);
        root.setPadding(pad, pad, pad, pad);

        AlertDialog[] ref = new AlertDialog[1];

        TextView tvTitle = new TextView(this);
        tvTitle.setText(getString(R.string.wiki_search_title));
        tvTitle.setTextColor(theme.textColor);
        tvTitle.setTextSize(17f);
        tvTitle.setTypeface(Typeface.create("sans-serif-black", Typeface.BOLD));
        tvTitle.setPadding(0, 0, 0, dp(4));
        root.addView(tvTitle);

        TextView tvSub = new TextView(this);
        tvSub.setText(getString(R.string.wiki_search_sub));
        tvSub.setTextColor(theme.getMutedTextColor());
        tvSub.setTextSize(12f);
        tvSub.setPadding(0, 0, 0, dp(12));
        root.addView(tvSub);

        EditText etSearch = addFormInput(root, getString(R.string.wiki_search_hint), "");

        LinearLayout resultsBox = new LinearLayout(this);
        resultsBox.setOrientation(LinearLayout.VERTICAL);
        ScrollView resultsScroll = new ScrollView(this);
        resultsScroll.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(220)));
        resultsScroll.addView(resultsBox);
        root.addView(resultsScroll);

        ProgressBar progress = new ProgressBar(this);
        progress.setVisibility(View.GONE);
        LinearLayout.LayoutParams pgLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        pgLp.gravity = Gravity.CENTER_HORIZONTAL;
        progress.setLayoutParams(pgLp);
        root.addView(progress);

        Runnable doSearch = () -> {
            String q = etSearch.getText().toString().trim();
            if (q.isEmpty()) return;
            progress.setVisibility(View.VISIBLE);
            resultsBox.removeAllViews();
            KpopWikiImporter.searchWiki(q, new KpopWikiImporter.SearchCallback() {
                @Override
                public void onSuccess(List<KpopWikiImporter.WikiResult> results) {
                    progress.setVisibility(View.GONE);
                    if (results.isEmpty()) {
                        TextView tvEmpty = new TextView(MainActivity.this);
                        tvEmpty.setText(getString(R.string.wiki_search_empty));
                        tvEmpty.setTextColor(Color.parseColor("#9CA3AF"));
                        tvEmpty.setPadding(0, dp(14), 0, 0);
                        resultsBox.addView(tvEmpty);
                        return;
                    }
                    for (KpopWikiImporter.WikiResult r : results) {
                        TextView item = new TextView(MainActivity.this);
                        item.setText("✦ " + r.title);
                        item.setTextColor(theme.textColor);
                        item.setTextSize(13.5f);
                        item.setTypeface(Typeface.DEFAULT_BOLD);
                        item.setBackground(ThemeManager.createPillDrawable(MainActivity.this, Color.parseColor("#F9FAFB"), 14));
                        item.setPadding(dp(14), dp(10), dp(14), dp(10));
                        LinearLayout.LayoutParams iLp = new LinearLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                        iLp.setMargins(0, 0, 0, dp(8));
                        item.setLayoutParams(iLp);
                        item.setOnClickListener(v -> {
                            progress.setVisibility(View.VISIBLE);
                            KpopWikiImporter.fetchIdolCard(r.title, new KpopWikiImporter.IdolImportCallback() {
                                @Override
                                public void onSuccess(IdolCard card) {
                                    progress.setVisibility(View.GONE);
                                    if (ref[0] != null) ref[0].dismiss();
                                    showCreateOrEditIdolDialog(card, MainActivity.this::refreshCurrentTab);
                                }

                                @Override
                                public void onError(String message) {
                                    progress.setVisibility(View.GONE);
                                    Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
                                }
                            });
                        });
                        resultsBox.addView(item);
                    }
                }

                @Override
                public void onError(String message) {
                    progress.setVisibility(View.GONE);
                    Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
                }
            });
        };

        etSearch.setOnEditorActionListener((v, actionId, event) -> {
            doSearch.run();
            return true;
        });

        TextView btnSearch = new TextView(this);
        btnSearch.setText(getString(R.string.btn_search_wiki));
        btnSearch.setTextColor(theme.getButtonTextColor(theme.primaryColor));
        btnSearch.setTextSize(13f);
        btnSearch.setTypeface(Typeface.DEFAULT_BOLD);
        btnSearch.setGravity(Gravity.CENTER);
        btnSearch.setBackground(ThemeManager.createPillDrawable(this, theme.primaryColor, 20));
        btnSearch.setPadding(dp(14), dp(10), dp(14), dp(10));
        LinearLayout.LayoutParams bLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        bLp.setMargins(0, dp(10), 0, 0);
        btnSearch.setLayoutParams(bLp);
        btnSearch.setOnClickListener(v -> doSearch.run());
        root.addView(btnSearch);

        builder.setView(root);
        ref[0] = builder.create();
        if (ref[0].getWindow() != null) {
            ref[0].getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        ref[0].show();
    }

    private void showCenterPlusMenuDialog() {
        ThemeManager.AppTheme theme = ThemeManager.getActiveTheme(this);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackground(ThemeManager.createCardDrawable(this, theme.surfaceColor, theme.cardBorderColor, 26));
        int pad = dp(20);
        root.setPadding(pad, dp(14), pad, dp(20));

        AlertDialog[] ref = new AlertDialog[1];

        // Drag handle indicator
        View handle = new View(this);
        handle.setBackground(ThemeManager.createPillDrawable(this, Color.parseColor("#D1D5DB"), 3));
        LinearLayout.LayoutParams hLp = new LinearLayout.LayoutParams(dp(40), dp(4));
        hLp.gravity = Gravity.CENTER_HORIZONTAL;
        hLp.setMargins(0, 0, 0, dp(14));
        handle.setLayoutParams(hLp);
        root.addView(handle);

        TextView tvTitle = new TextView(this);
        tvTitle.setText(getString(R.string.fab_center_title));
        tvTitle.setTextColor(theme.textColor);
        tvTitle.setTextSize(18f);
        tvTitle.setTypeface(Typeface.create("sans-serif-black", Typeface.BOLD));
        tvTitle.setPadding(0, 0, 0, dp(4));
        root.addView(tvTitle);

        TextView tvSub = new TextView(this);
        tvSub.setText(getString(R.string.fab_center_sub));
        tvSub.setTextColor(theme.getMutedTextColor());
        tvSub.setTextSize(12f);
        tvSub.setPadding(0, 0, 0, dp(16));
        root.addView(tvSub);

        root.addView(buildPlusActionCard("🃏", getString(R.string.fab_create_idol_title), getString(R.string.fab_create_idol_desc), "#FFF0F5", () -> {
            if (ref[0] != null) ref[0].dismiss();
            showCreateOrEditIdolDialog(null, this::refreshCurrentTab);
        }));

        root.addView(buildPlusActionCard("🌐", getString(R.string.fab_wiki_title), getString(R.string.fab_wiki_desc), "#EFF6FF", () -> {
            if (ref[0] != null) ref[0].dismiss();
            showWikiSearchAndImportDialog();
        }));

        root.addView(buildPlusActionCard("🎯", getString(R.string.fab_custom_match_title), getString(R.string.fab_custom_match_desc), "#FFFBEB", () -> {
            if (ref[0] != null) ref[0].dismiss();
            showVibraryStyleCustomMatchBuilder();
        }));

        root.addView(buildPlusActionCard("🎨", getString(R.string.fab_theme_title), getString(R.string.fab_theme_desc), "#F3E8FF", () -> {
            if (ref[0] != null) ref[0].dismiss();
            ThemeManager.showThemeStudioDialog(this, () -> {
                applyThemeToShell();
                refreshCurrentTab();
            });
        }));

        root.addView(buildPlusActionCard("📤", getString(R.string.fab_share_title), getString(R.string.fab_share_desc), "#ECFDF5", () -> {
            if (ref[0] != null) ref[0].dismiss();
            showVibraryStyleShareIdolsDialog();
        }));

        root.addView(buildPlusActionCard("📥", getString(R.string.fab_import_title), getString(R.string.fab_import_desc), "#F1F5F9", () -> {
            if (ref[0] != null) ref[0].dismiss();
            showImportHubDialog();
        }));

        scroll.addView(root);
        builder.setView(scroll);
        ref[0] = builder.create();
        if (ref[0].getWindow() != null) {
            ref[0].getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        ref[0].show();
    }

    private void showQuickMatchSetupSheet() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(20);
        root.setPadding(pad, pad, pad, pad);
        root.setBackgroundColor(Color.WHITE);

        AlertDialog[] ref = new AlertDialog[1];

        TextView tvTitle = new TextView(this);
        tvTitle.setText(getString(R.string.cpu_dialog_title));
        tvTitle.setTextColor(Color.parseColor("#111827"));
        tvTitle.setTextSize(18f);
        tvTitle.setTypeface(Typeface.DEFAULT_BOLD);
        tvTitle.setPadding(0, 0, 0, dp(12));
        root.addView(tvTitle);

        addSmallLabel(root, getString(R.string.label_game_mode));
        final String[] modeValues = {"KING_OF_THE_HILL", "BRACKET", "ROUND_ROBIN", "DOUBLE_ELIMINATION"};
        final String[] modeLabels = {
                getString(R.string.mode_koth),
                getString(R.string.mode_bracket),
                getString(R.string.mode_round_robin),
                getString(R.string.mode_double_elim)
        };
        Spinner spMode = createStyledSpinner(modeLabels);
        root.addView(spMode);

        addSmallLabel(root, getString(R.string.label_filter_deck));
        List<String> filterOptions = new ArrayList<>();
        filterOptions.add(getString(R.string.filter_all_idols, repo.getAllIdols().size()));
        for (String col : repo.getAllCollections()) filterOptions.add("📁 " + col);
        for (String grp : repo.getAllGroups()) filterOptions.add("✦ " + grp);
        Spinner spFilter = createStyledSpinner(filterOptions.toArray(new String[0]));
        root.addView(spFilter);

        addSmallLabel(root, getString(R.string.label_card_count));
        final int[] selectedCount = {16};
        final int[] countOptions = {4, 8, 16, 32, 64, 999};
        final String[] countLabels = {"4", "8", "16", "32", "64", "ALL"};
        LinearLayout countRow = new LinearLayout(this);
        countRow.setOrientation(LinearLayout.HORIZONTAL);
        final List<TextView> countChipViews = new ArrayList<>();

        for (int i = 0; i < countOptions.length; i++) {
            final int val = countOptions[i];
            TextView chip = new TextView(this);
            chip.setText(countLabels[i]);
            chip.setGravity(Gravity.CENTER);
            chip.setTextSize(13f);
            chip.setTypeface(Typeface.DEFAULT_BOLD);
            chip.setPadding(0, dp(10), 0, dp(10));
            LinearLayout.LayoutParams chLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            if (i < countOptions.length - 1) chLp.setMargins(0, 0, dp(6), 0);
            chip.setLayoutParams(chLp);
            countChipViews.add(chip);
            chip.setOnClickListener(v -> {
                selectedCount[0] = val;
                updateCountChipsHighlight(countChipViews, countOptions, selectedCount[0]);
            });
            countRow.addView(chip);
        }
        updateCountChipsHighlight(countChipViews, countOptions, selectedCount[0]);
        root.addView(countRow);

        TextView btnStart = new TextView(this);
        btnStart.setText(getString(R.string.btn_start_match));
        btnStart.setTextColor(Color.parseColor("#111827"));
        btnStart.setTypeface(Typeface.DEFAULT_BOLD);
        btnStart.setTextSize(14f);
        btnStart.setGravity(Gravity.CENTER);
        btnStart.setBackgroundResource(R.drawable.bg_btn_primary);
        btnStart.setPadding(dp(16), dp(14), dp(16), dp(14));
        LinearLayout.LayoutParams sLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        sLp.setMargins(0, dp(18), 0, 0);
        btnStart.setLayoutParams(sLp);
        btnStart.setOnClickListener(v -> {
            if (ref[0] != null) ref[0].dismiss();
            String mode = modeValues[spMode.getSelectedItemPosition()];
            String rawFilter = filterOptions.get(spFilter.getSelectedItemPosition());
            String cleanFilter = "ALL";
            if (rawFilter.startsWith("📁 ")) cleanFilter = rawFilter.substring(3);
            else if (rawFilter.startsWith("✦ ")) cleanFilter = rawFilter.substring(2);

            List<IdolCard> pool = repo.getIdolsByFilter(cleanFilter);
            Collections.shuffle(pool);
            int limit = Math.min(selectedCount[0], pool.size());
            launchMatch("ALL".equals(cleanFilter) ? "Torneo Aleatorio (" + limit + ")" : cleanFilter + " (" + limit + ")",
                    mode, pool.subList(0, limit));
        });
        root.addView(btnStart);

        builder.setView(root);
        ref[0] = builder.create();
        ref[0].show();
    }

    /**
     * Exact Vibrary Screenshot #4 Visual Squircle Grid Selector for building a Custom Match!
     */
    private void showVibraryStyleCustomMatchBuilder() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        ThemeManager.AppTheme themeVib = ThemeManager.getActiveTheme(this);
        root.setBackground(ThemeManager.createCardDrawable(this, themeVib.surfaceColor, themeVib.cardBorderColor, 24));
        int pad = dp(18);
        root.setPadding(pad, pad, pad, 0);

        AlertDialog[] ref = new AlertDialog[1];
        final Set<String> chosenIds = new LinkedHashSet<>();

        EditText etTitle = addFormInput(root, getString(R.string.hint_match_custom_title), "");

        final String[] modeValues = {"KING_OF_THE_HILL", "BRACKET", "ROUND_ROBIN", "DOUBLE_ELIMINATION"};
        final String[] modeLabels = {
                getString(R.string.mode_koth),
                getString(R.string.mode_bracket),
                getString(R.string.mode_round_robin),
                getString(R.string.mode_double_elim)
        };
        Spinner spMode = createStyledSpinner(modeLabels);
        root.addView(spMode);

        EditText etFilter = addFormInput(root, getString(R.string.filter_group_or_name_hint), "");

        TextView tvCounter = new TextView(this);
        tvCounter.setGravity(Gravity.CENTER);
        tvCounter.setTextSize(13f);
        tvCounter.setTypeface(Typeface.DEFAULT_BOLD);
        tvCounter.setPadding(0, dp(6), 0, dp(6));
        root.addView(tvCounter);

        ScrollView gridScroll = new ScrollView(this);
        gridScroll.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(310)));
        LinearLayout groupsBox = new LinearLayout(this);
        groupsBox.setOrientation(LinearLayout.VERTICAL);
        gridScroll.addView(groupsBox);
        root.addView(gridScroll);

        TextView btnSavePlay = new TextView(this);

        Runnable updateCounter = () -> {
            int count = chosenIds.size();
            if (count < 2) {
                tvCounter.setText(getString(R.string.custom_match_selected_counter_hint, count));
                tvCounter.setTextColor(Color.parseColor("#EF4444"));
            } else {
                tvCounter.setText(getString(R.string.custom_match_selected_counter, count));
                tvCounter.setTextColor(themeVib.primaryColor);
            }
            btnSavePlay.setText("🚀 " + getString(R.string.btn_save_and_play) + " (" + count + ")");
        };

        Runnable renderGrid = new Runnable() {
            @Override
            public void run() {
                groupsBox.removeAllViews();
                ThemeManager.AppTheme theme = ThemeManager.getActiveTheme(MainActivity.this);
                String q = etFilter.getText().toString().toLowerCase(Locale.ROOT).trim();
                List<String> groups = repo.getAllGroups();
                groups.add("Soloists");

                for (String gName : groups) {
                    List<IdolCard> members = repo.getIdolsByFilter(gName);
                    if (members.isEmpty()) continue;
                    if (!q.isEmpty() && !gName.toLowerCase(Locale.ROOT).contains(q)) {
                        List<IdolCard> sub = new ArrayList<>();
                        for (IdolCard m : members) {
                            if (m.getStageName().toLowerCase(Locale.ROOT).contains(q)) sub.add(m);
                        }
                        if (sub.isEmpty()) continue;
                        members = sub;
                    }

                    // Group Header + [✓ Todo] button just like Vibrary screenshot #4!
                    LinearLayout hRow = new LinearLayout(MainActivity.this);
                    hRow.setOrientation(LinearLayout.HORIZONTAL);
                    hRow.setGravity(Gravity.CENTER_VERTICAL);
                    hRow.setPadding(0, dp(8), 0, dp(8));

                    TextView tvG = new TextView(MainActivity.this);
                    tvG.setText(gName);
                    tvG.setTextColor(Color.parseColor("#111827"));
                    tvG.setTextSize(15f);
                    tvG.setTypeface(Typeface.DEFAULT_BOLD);
                    tvG.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
                    hRow.addView(tvG);

                    final List<IdolCard> finalMembers = members;
                    boolean allSelected = true;
                    for (IdolCard m : finalMembers) {
                        if (!chosenIds.contains(m.getId())) {
                            allSelected = false;
                            break;
                        }
                    }
                    final boolean groupAllSelected = allSelected;
                    TextView btnTodo = new TextView(MainActivity.this);
                    btnTodo.setText(groupAllSelected ? "✕ " + getString(R.string.btn_deselect_all) : "✓ " + getString(R.string.btn_select_all));
                    btnTodo.setTextColor(groupAllSelected ? theme.primaryColor : Color.parseColor("#6B7280"));
                    btnTodo.setTextSize(11.5f);
                    btnTodo.setPadding(dp(12), dp(6), dp(12), dp(6));
                    btnTodo.setBackgroundResource(R.drawable.bg_btn_secondary);
                    btnTodo.setOnClickListener(v -> {
                        if (groupAllSelected) {
                            for (IdolCard m : finalMembers) chosenIds.remove(m.getId());
                        } else {
                            for (IdolCard m : finalMembers) chosenIds.add(m.getId());
                        }
                        updateCounter.run();
                        run();
                    });
                    hRow.addView(btnTodo);
                    groupsBox.addView(hRow);

                    for (int i = 0; i < members.size(); i += 4) {
                        LinearLayout r = new LinearLayout(MainActivity.this);
                        r.setOrientation(LinearLayout.HORIZONTAL);
                        r.setPadding(0, 0, 0, dp(8));
                        for (int c = 0; c < 4; c++) {
                            if (i + c < members.size()) {
                                r.addView(buildVibrarySquircleMemberCell(members.get(i + c), chosenIds, updateCounter));
                            } else {
                                View emp = new View(MainActivity.this);
                                emp.setLayoutParams(new LinearLayout.LayoutParams(0, 1, 1f));
                                r.addView(emp);
                            }
                        }
                        groupsBox.addView(r);
                    }
                }
            }
        };
        renderGrid.run();
        updateCounter.run();

        etFilter.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { renderGrid.run(); }
            @Override public void afterTextChanged(Editable s) {}
        });

        // Bottom Vibrary Rounded Action Bar: [ ↻ Inicialización ] | [ 🚀 Guardar y Jugar ]
        ThemeManager.AppTheme theme = ThemeManager.getActiveTheme(this);
        LinearLayout bottomBar = new LinearLayout(this);
        bottomBar.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams bbLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        bbLp.topMargin = dp(12);
        bbLp.bottomMargin = dp(12);
        bottomBar.setLayoutParams(bbLp);

        TextView btnReset = new TextView(this);
        btnReset.setText("↻ " + getString(R.string.btn_reset_selection));
        btnReset.setTextColor(Color.parseColor("#374151"));
        btnReset.setBackground(ThemeManager.createPillDrawable(this, Color.parseColor("#E5E7EB"), 22));
        btnReset.setGravity(Gravity.CENTER);
        btnReset.setTypeface(Typeface.DEFAULT_BOLD);
        btnReset.setTextSize(12.5f);
        btnReset.setPadding(dp(12), dp(11), dp(12), dp(11));
        LinearLayout.LayoutParams rmLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        rmLp.setMargins(0, 0, dp(8), 0);
        btnReset.setLayoutParams(rmLp);
        btnReset.setOnClickListener(v -> {
            chosenIds.clear();
            updateCounter.run();
            renderGrid.run();
        });
        bottomBar.addView(btnReset);

        btnSavePlay.setTextColor(theme.getButtonTextColor(theme.primaryColor));
        btnSavePlay.setBackground(ThemeManager.createPillDrawable(this, theme.primaryColor, 22));
        btnSavePlay.setGravity(Gravity.CENTER);
        btnSavePlay.setTypeface(Typeface.DEFAULT_BOLD);
        btnSavePlay.setTextSize(13f);
        btnSavePlay.setPadding(dp(14), dp(11), dp(14), dp(11));
        LinearLayout.LayoutParams spLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.5f);
        btnSavePlay.setLayoutParams(spLp);
        btnSavePlay.setOnClickListener(v -> {
            if (chosenIds.size() < 2) {
                Toast.makeText(this, getString(R.string.toast_pick_min_2_idols), Toast.LENGTH_SHORT).show();
                return;
            }
            if (ref[0] != null) ref[0].dismiss();
            String title = etTitle.getText().toString().trim();
            if (title.isEmpty()) title = getString(R.string.title_custom_match_default) + " (" + chosenIds.size() + ")";
            String mode = modeValues[spMode.getSelectedItemPosition()];

            CustomMatch cm = new CustomMatch(null, title, "Player", mode, new ArrayList<>(chosenIds), System.currentTimeMillis());
            repo.saveOrUpdateCustomMatch(cm);
            refreshCurrentTab();

            List<IdolCard> cards = new ArrayList<>();
            for (String cid : chosenIds) {
                IdolCard c = repo.getIdolById(cid);
                if (c != null) cards.add(c);
            }
            new AlertDialog.Builder(this)
                    .setTitle(getString(R.string.dialog_match_created_title, title))
                    .setItems(new String[]{getString(R.string.btn_play_now), getString(R.string.action_share_whatsapp)}, (d2, w2) -> {
                        if (w2 == 0) launchMatch(cm.getTitle(), cm.getGameMode(), cards);
                        else showShareChoiceDialog(cm.getTitle(), cm, cards);
                    }).show();
        });
        bottomBar.addView(btnSavePlay);
        root.addView(bottomBar);

        builder.setView(root);
        ref[0] = builder.create();
        if (ref[0].getWindow() != null) {
            ref[0].getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        ref[0].show();
    }

    private void showVibraryStyleShareIdolsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        ThemeManager.AppTheme themeVib = ThemeManager.getActiveTheme(this);
        root.setBackground(ThemeManager.createCardDrawable(this, themeVib.surfaceColor, themeVib.cardBorderColor, 24));
        int pad = dp(18);
        root.setPadding(pad, pad, pad, 0);

        AlertDialog[] ref = new AlertDialog[1];
        final Set<String> chosenIds = new LinkedHashSet<>();

        TextView tvHeader = new TextView(this);
        tvHeader.setText(getString(R.string.share_idols_header));
        tvHeader.setTextColor(Color.parseColor("#111827"));
        tvHeader.setTextSize(16f);
        tvHeader.setTypeface(Typeface.DEFAULT_BOLD);
        tvHeader.setPadding(0, 0, 0, dp(8));
        root.addView(tvHeader);

        EditText etFilter = addFormInput(root, getString(R.string.filter_group_or_name_hint), "");

        ScrollView gridScroll = new ScrollView(this);
        gridScroll.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(330)));
        LinearLayout groupsBox = new LinearLayout(this);
        groupsBox.setOrientation(LinearLayout.VERTICAL);
        gridScroll.addView(groupsBox);
        root.addView(gridScroll);

        Runnable renderGrid = new Runnable() {
            @Override
            public void run() {
                groupsBox.removeAllViews();
                ThemeManager.AppTheme theme = ThemeManager.getActiveTheme(MainActivity.this);
                String q = etFilter.getText().toString().toLowerCase(Locale.ROOT).trim();
                List<String> groups = repo.getAllGroups();
                groups.add("Soloists");

                for (String gName : groups) {
                    List<IdolCard> members = repo.getIdolsByFilter(gName);
                    if (members.isEmpty()) continue;
                    if (!q.isEmpty() && !gName.toLowerCase(Locale.ROOT).contains(q)) {
                        List<IdolCard> sub = new ArrayList<>();
                        for (IdolCard m : members) {
                            if (m.getStageName().toLowerCase(Locale.ROOT).contains(q)) sub.add(m);
                        }
                        if (sub.isEmpty()) continue;
                        members = sub;
                    }

                    LinearLayout hRow = new LinearLayout(MainActivity.this);
                    hRow.setOrientation(LinearLayout.HORIZONTAL);
                    hRow.setGravity(Gravity.CENTER_VERTICAL);
                    hRow.setPadding(0, dp(8), 0, dp(8));

                    TextView tvG = new TextView(MainActivity.this);
                    tvG.setText(gName);
                    tvG.setTextColor(Color.parseColor("#111827"));
                    tvG.setTextSize(15f);
                    tvG.setTypeface(Typeface.DEFAULT_BOLD);
                    tvG.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
                    hRow.addView(tvG);

                    final List<IdolCard> finalMembers = members;
                    boolean allSelected = true;
                    for (IdolCard m : finalMembers) {
                        if (!chosenIds.contains(m.getId())) {
                            allSelected = false;
                            break;
                        }
                    }
                    final boolean groupAllSelected = allSelected;
                    TextView btnTodo = new TextView(MainActivity.this);
                    btnTodo.setText(groupAllSelected ? "✕ " + getString(R.string.btn_deselect_all) : "✓ " + getString(R.string.btn_select_all));
                    btnTodo.setTextColor(groupAllSelected ? theme.primaryColor : Color.parseColor("#6B7280"));
                    btnTodo.setTextSize(11.5f);
                    btnTodo.setPadding(dp(12), dp(6), dp(12), dp(6));
                    btnTodo.setBackgroundResource(R.drawable.bg_btn_secondary);
                    btnTodo.setOnClickListener(v -> {
                        if (groupAllSelected) {
                            for (IdolCard m : finalMembers) chosenIds.remove(m.getId());
                        } else {
                            for (IdolCard m : finalMembers) chosenIds.add(m.getId());
                        }
                        run();
                    });
                    hRow.addView(btnTodo);
                    groupsBox.addView(hRow);

                    for (int i = 0; i < members.size(); i += 4) {
                        LinearLayout r = new LinearLayout(MainActivity.this);
                        r.setOrientation(LinearLayout.HORIZONTAL);
                        r.setPadding(0, 0, 0, dp(8));
                        for (int c = 0; c < 4; c++) {
                            if (i + c < members.size()) {
                                r.addView(buildVibrarySquircleMemberCell(members.get(i + c), chosenIds));
                            } else {
                                View emp = new View(MainActivity.this);
                                emp.setLayoutParams(new LinearLayout.LayoutParams(0, 1, 1f));
                                r.addView(emp);
                            }
                        }
                        groupsBox.addView(r);
                    }
                }
            }
        };
        renderGrid.run();

        etFilter.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { renderGrid.run(); }
            @Override public void afterTextChanged(Editable s) {}
        });

        LinearLayout bottomBar = new LinearLayout(this);
        bottomBar.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams bbLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(50));
        bbLp.topMargin = dp(10);
        bottomBar.setLayoutParams(bbLp);

        ThemeManager.AppTheme themeShare = ThemeManager.getActiveTheme(this);
        TextView btnSend = new TextView(this);
        btnSend.setText("📤 " + getString(R.string.action_share_whatsapp));
        btnSend.setTextColor(themeShare.getButtonTextColor(themeShare.primaryColor));
        btnSend.setBackground(ThemeManager.createPillDrawable(this, themeShare.primaryColor, 22));
        btnSend.setGravity(Gravity.CENTER);
        btnSend.setTypeface(Typeface.DEFAULT_BOLD);
        btnSend.setTextSize(13.5f);
        btnSend.setPadding(dp(14), dp(11), dp(14), dp(11));
        LinearLayout.LayoutParams sndLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        sndLp.bottomMargin = dp(12);
        btnSend.setLayoutParams(sndLp);
        btnSend.setOnClickListener(v -> {
            if (chosenIds.isEmpty()) {
                Toast.makeText(this, getString(R.string.toast_pick_min_1_idol), Toast.LENGTH_SHORT).show();
                return;
            }
            if (ref[0] != null) ref[0].dismiss();
            List<IdolCard> toShare = new ArrayList<>();
            for (String id : chosenIds) {
                IdolCard c = repo.getIdolById(id);
                if (c != null) toShare.add(c);
            }
            showShareChoiceDialog("Pack (" + toShare.size() + ")", null, toShare);
        });
        bottomBar.addView(btnSend);
        root.addView(bottomBar);

        builder.setView(root);
        ref[0] = builder.create();
        if (ref[0].getWindow() != null) {
            ref[0].getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        ref[0].show();
    }

    private void showIdolDetailAndPhotocardBinderDialog(IdolCard card) {
        card.deduplicatePhotocards();
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        ThemeManager.AppTheme themeBinder = ThemeManager.getActiveTheme(this);
        int pad = dp(18);

        LinearLayout dialogCard = new LinearLayout(this);
        dialogCard.setOrientation(LinearLayout.VERTICAL);
        dialogCard.setPadding(pad, pad, pad, pad);
        dialogCard.setBackground(ThemeManager.createCardDrawable(this, themeBinder.surfaceColor, themeBinder.cardBorderColor, 26));

        ScrollView scroll = new ScrollView(this);
        scroll.setVerticalScrollBarEnabled(false);
        LinearLayout.LayoutParams scrollLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
        scroll.setLayoutParams(scrollLp);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);

        AlertDialog[] dialogRef = new AlertDialog[1];

        FrameLayout photoHeaderFrame = new FrameLayout(this);
        LinearLayout.LayoutParams mpLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(245));
        mpLp.setMargins(0, 0, 0, dp(12));
        photoHeaderFrame.setLayoutParams(mpLp);

        CardView mainPhotoCard = new CardView(this);
        mainPhotoCard.setRadius(dp(22));
        mainPhotoCard.setCardElevation(dp(2));
        mainPhotoCard.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        ImageView ivMain = new ImageView(this);
        ivMain.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        ivMain.setScaleType(ImageView.ScaleType.CENTER_CROP);
        CardImageLoader.loadCardImage(this, card, ivMain);
        mainPhotoCard.addView(ivMain);
        photoHeaderFrame.addView(mainPhotoCard);

        // Floating top-right close button (✕) so closing is always accessible with 1 tap
        TextView btnTopClose = new TextView(this);
        btnTopClose.setText("✕");
        btnTopClose.setTextColor(Color.WHITE);
        btnTopClose.setTextSize(14f);
        btnTopClose.setTypeface(Typeface.DEFAULT_BOLD);
        btnTopClose.setGravity(Gravity.CENTER);
        GradientDrawable topCloseGd = new GradientDrawable();
        topCloseGd.setShape(GradientDrawable.OVAL);
        topCloseGd.setColor(Color.parseColor("#99111827"));
        topCloseGd.setStroke(dp(1), Color.parseColor("#66FFFFFF"));
        btnTopClose.setBackground(topCloseGd);
        btnTopClose.setElevation(dp(6));
        FrameLayout.LayoutParams tcLp = new FrameLayout.LayoutParams(dp(34), dp(34), Gravity.TOP | Gravity.END);
        tcLp.setMargins(dp(10), dp(10), dp(10), dp(10));
        btnTopClose.setLayoutParams(tcLp);
        btnTopClose.setOnClickListener(v -> {
            if (dialogRef[0] != null) dialogRef[0].dismiss();
        });
        photoHeaderFrame.addView(btnTopClose);

        root.addView(photoHeaderFrame);

        TextView tvName = new TextView(this);
        tvName.setText(card.getStageName() + "  •  " + (card.isSoloist() ? "Soloist" : card.getGroupName()));
        tvName.setTextColor(themeBinder.textColor);
        tvName.setTextSize(21f);
        tvName.setTypeface(Typeface.create("sans-serif-black", Typeface.BOLD));
        root.addView(tvName);

        if (card.getNativeName() != null && !card.getNativeName().isEmpty()) {
            TextView tvNative = new TextView(this);
            tvNative.setText(card.getNativeName());
            tvNative.setTextColor(themeBinder.primaryColor);
            tvNative.setTextSize(14.5f);
            tvNative.setTypeface(Typeface.DEFAULT_BOLD);
            tvNative.setPadding(0, 0, 0, dp(4));
            root.addView(tvNative);
        }

        TextView tvDetails = new TextView(this);
        StringBuilder dsb = new StringBuilder();
        dsb.append(getString(R.string.card_back_real_name)).append(" ").append(card.getFullName()).append("\n");
        dsb.append(getString(R.string.card_back_country)).append(" ").append(card.getCountryWithFlag()).append("\n");
        dsb.append(getString(R.string.card_back_age_zodiac)).append(" ").append(card.getCalculatedAge()).append(" (").append(card.getBirthDate()).append(" • ").append(card.getZodiacSign()).append(")\n");
        dsb.append(getString(R.string.card_back_role)).append(" ").append(card.getRole()).append("\n");
        List<IdolCard> mates = repo.getFellowGroupMembers(card);
        if (!mates.isEmpty()) {
            dsb.append(getString(R.string.card_back_group_mates)).append(" ");
            for (int i = 0; i < mates.size(); i++) {
                if (i > 0) dsb.append(", ");
                dsb.append(mates.get(i).getStageName());
            }
            dsb.append("\n");
        }
        dsb.append("\n").append(card.getBio());
        tvDetails.setText(dsb.toString());
        tvDetails.setTextColor(Color.parseColor("#4B5563"));
        tvDetails.setTextSize(12.5f);
        tvDetails.setPadding(0, dp(4), 0, dp(10));
        root.addView(tvDetails);

        TextView btnRandomToggle = new TextView(this);
        btnRandomToggle.setText(card.isUseRandomPhotocard() ? getString(R.string.random_pc_on) : getString(R.string.random_pc_off));
        btnRandomToggle.setTextColor(Color.parseColor("#111827"));
        btnRandomToggle.setTextSize(11.5f);
        btnRandomToggle.setTypeface(Typeface.DEFAULT_BOLD);
        btnRandomToggle.setGravity(Gravity.CENTER);
        btnRandomToggle.setBackground(ThemeManager.createPillDrawable(this, card.isUseRandomPhotocard() ? Color.parseColor("#FEF3C7") : Color.parseColor("#F3F4F6"), 16));
        btnRandomToggle.setPadding(dp(12), dp(8), dp(12), dp(8));
        LinearLayout.LayoutParams rtLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rtLp.setMargins(0, 0, 0, dp(12));
        btnRandomToggle.setLayoutParams(rtLp);
        btnRandomToggle.setOnClickListener(v -> {
            card.setUseRandomPhotocard(!card.isUseRandomPhotocard());
            repo.saveOrUpdateIdol(card);
            btnRandomToggle.setText(card.isUseRandomPhotocard() ? getString(R.string.random_pc_on) : getString(R.string.random_pc_off));
            btnRandomToggle.setBackground(ThemeManager.createPillDrawable(this, card.isUseRandomPhotocard() ? Color.parseColor("#FEF3C7") : Color.parseColor("#F3F4F6"), 16));
        });
        root.addView(btnRandomToggle);

        // Photocard Binder Section
        TextView tvPcTitle = new TextView(this);
        tvPcTitle.setText(getString(R.string.binder_title_format, card.getStageName(), card.getUnlockedPhotocards().size(), card.getPhotocards().size()));
        tvPcTitle.setTextColor(Color.parseColor("#111827"));
        tvPcTitle.setTextSize(14.5f);
        tvPcTitle.setTypeface(Typeface.DEFAULT_BOLD);
        tvPcTitle.setPadding(0, 0, 0, dp(2));
        root.addView(tvPcTitle);

        TextView tvPcSubHint = new TextView(this);
        tvPcSubHint.setText(getString(R.string.binder_zoom_hint));
        tvPcSubHint.setTextColor(themeBinder.getMutedTextColor());
        tvPcSubHint.setTextSize(11f);
        tvPcSubHint.setPadding(0, 0, 0, dp(6));
        root.addView(tvPcSubHint);

        HorizontalScrollView pcScroll = new HorizontalScrollView(this);
        pcScroll.setHorizontalScrollBarEnabled(false);
        LinearLayout pcRow = new LinearLayout(this);
        pcRow.setOrientation(LinearLayout.HORIZONTAL);
        pcRow.setPadding(0, 0, 0, dp(10));

        Runnable rebuildPhotocardsRow = new Runnable() {
            @Override
            public void run() {
                pcRow.removeAllViews();
                List<String> allPcs = new ArrayList<>(card.getPhotocards());
                int unlockedCount = 0;
                for (String p : allPcs) {
                    if (card.isPhotocardUnlocked(p)) unlockedCount++;
                }
                tvPcTitle.setText(getString(R.string.binder_title_format, card.getStageName(), unlockedCount, allPcs.size()));

                int firstBatch = Math.min(8, allPcs.size());
                for (int i = 0; i < firstBatch; i++) {
                    pcRow.addView(buildPhotocardBinderCell(card, allPcs.get(i), i, allPcs.size(), ivMain, this));
                }
                if (firstBatch < allPcs.size()) {
                    pcRow.post(() -> {
                        if (isFinishing() || (dialogRef[0] != null && !dialogRef[0].isShowing())) return;
                        for (int i = firstBatch; i < allPcs.size(); i++) {
                            pcRow.addView(buildPhotocardBinderCell(card, allPcs.get(i), i, allPcs.size(), ivMain, this));
                        }
                    });
                }
            }
        };

        ivMain.setOnClickListener(v -> showPhotocardZoomPreviewDialog(card, card.getPhotoUri(), 1, ivMain, rebuildPhotocardsRow));

        rebuildPhotocardsRow.run();
        pcScroll.addView(pcRow);
        root.addView(pcScroll);

        KpopWikiImporter.resolveAllPhotocardSlotsAsync(card, repo, rebuildPhotocardsRow);

        // Consolidated Add Photocard Menu (Upload Gallery, Paste Clipboard, or Image Link)
        TextView btnAddPcMenu = new TextView(this);
        btnAddPcMenu.setText(getString(R.string.btn_add_photocard_menu));
        btnAddPcMenu.setTypeface(Typeface.DEFAULT_BOLD);
        btnAddPcMenu.setTextSize(12.5f);
        btnAddPcMenu.setGravity(Gravity.CENTER);
        btnAddPcMenu.setBackground(ThemeManager.createPillDrawable(this, themeBinder.primaryColor, 18));
        btnAddPcMenu.setTextColor(themeBinder.getButtonTextColor(themeBinder.primaryColor));
        btnAddPcMenu.setPadding(dp(14), dp(10), dp(14), dp(10));
        LinearLayout.LayoutParams apcLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        apcLp.setMargins(0, 0, 0, dp(10));
        btnAddPcMenu.setLayoutParams(apcLp);
        btnAddPcMenu.setOnClickListener(v -> {
            CharSequence[] options = new CharSequence[]{
                    getString(R.string.menu_upload_gallery),
                    getString(R.string.menu_paste_clipboard),
                    getString(R.string.menu_enter_url)
            };
            new AlertDialog.Builder(this)
                    .setTitle(getString(R.string.btn_add_photocard_menu))
                    .setItems(options, (dlg, which) -> {
                        if (which == 0) {
                            // Gallery Upload
                            pendingPhotocardTargetIdol = card;
                            pendingPhotocardRefreshCallback = () -> {
                                CardImageLoader.loadCardImage(MainActivity.this, card, ivMain);
                                rebuildPhotocardsRow.run();
                                refreshCurrentTab();
                            };
                            Intent pick = new Intent(Intent.ACTION_GET_CONTENT);
                            pick.setType("image/*");
                            startActivityForResult(Intent.createChooser(pick, getString(R.string.menu_upload_gallery)), REQ_PICK_EXTRA_PHOTOCARD);
                        } else if (which == 1) {
                            // Paste from Clipboard with Validation & Confirmation Dialog
                            String pasted = pasteImageOrUrlFromClipboard();
                            if (pasted == null || pasted.trim().isEmpty()) {
                                Toast.makeText(this, getString(R.string.toast_copy_first), Toast.LENGTH_SHORT).show();
                            } else {
                                showConfirmAddPastedPhotocardDialog(card, pasted.trim(), ivMain, rebuildPhotocardsRow);
                            }
                        } else if (which == 2) {
                            // Enter Image URL
                            EditText urlIn = new EditText(this);
                            urlIn.setHint(getString(R.string.hint_photo_url));
                            new AlertDialog.Builder(this)
                                    .setTitle(getString(R.string.menu_enter_url))
                                    .setView(urlIn)
                                    .setPositiveButton(getString(R.string.btn_add_photocard), (d, w) -> {
                                        String u = urlIn.getText() != null ? urlIn.getText().toString().trim() : "";
                                        if (!u.isEmpty()) {
                                            showConfirmAddPastedPhotocardDialog(card, u, ivMain, rebuildPhotocardsRow);
                                        }
                                    })
                                    .setNegativeButton(getString(R.string.btn_cancel), null)
                                    .show();
                        }
                    })
                    .show();
        });
        root.addView(btnAddPcMenu);

        // Edit & Share Row
        LinearLayout bottomRow = new LinearLayout(this);
        bottomRow.setOrientation(LinearLayout.HORIZONTAL);

        TextView btnEdit = new TextView(this);
        btnEdit.setText(getString(R.string.btn_edit_data));
        btnEdit.setTextColor(Color.parseColor("#111827"));
        btnEdit.setTypeface(Typeface.DEFAULT_BOLD);
        btnEdit.setGravity(Gravity.CENTER);
        btnEdit.setBackground(ThemeManager.createPillDrawable(this, Color.parseColor("#F3F4F6"), 18));
        btnEdit.setPadding(dp(12), dp(9), dp(12), dp(9));
        LinearLayout.LayoutParams edLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        edLp.setMargins(0, 0, dp(6), 0);
        btnEdit.setLayoutParams(edLp);
        btnEdit.setOnClickListener(v -> {
            if (dialogRef[0] != null) dialogRef[0].dismiss();
            showCreateOrEditIdolDialog(card, this::refreshCurrentTab);
        });
        bottomRow.addView(btnEdit);

        TextView btnShareOne = new TextView(this);
        btnShareOne.setText(getString(R.string.btn_share_photocard));
        btnShareOne.setTextColor(Color.parseColor("#111827"));
        btnShareOne.setTypeface(Typeface.DEFAULT_BOLD);
        btnShareOne.setGravity(Gravity.CENTER);
        btnShareOne.setBackground(ThemeManager.createPillDrawable(this, themeBinder.secondaryColor, 18));
        btnShareOne.setPadding(dp(12), dp(9), dp(12), dp(9));
        btnShareOne.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        btnShareOne.setOnClickListener(v -> {
            shareIdolPhotocardInvite(card, card.getPhotoUri());
        });
        bottomRow.addView(btnShareOne);
        root.addView(bottomRow);

        scroll.addView(root);
        dialogCard.addView(scroll);

        // Sticky Close Button pinned outside the ScrollView at the bottom of dialogCard so it is NEVER hidden or cut off!
        TextView btnCloseModal = new TextView(this);
        btnCloseModal.setText(getString(R.string.btn_close));
        btnCloseModal.setTextColor(Color.parseColor("#1F2937"));
        btnCloseModal.setTypeface(Typeface.DEFAULT_BOLD);
        btnCloseModal.setTextSize(13f);
        btnCloseModal.setGravity(Gravity.CENTER);
        btnCloseModal.setBackground(ThemeManager.createPillDrawable(this, Color.parseColor("#E5E7EB"), 20));
        btnCloseModal.setPadding(dp(14), dp(10), dp(14), dp(10));
        LinearLayout.LayoutParams clLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        clLp.topMargin = dp(10);
        btnCloseModal.setLayoutParams(clLp);
        btnCloseModal.setOnClickListener(v -> {
            if (dialogRef[0] != null) dialogRef[0].dismiss();
        });
        dialogCard.addView(btnCloseModal);

        builder.setView(dialogCard);
        dialogRef[0] = builder.create();
        if (dialogRef[0].getWindow() != null) {
            dialogRef[0].getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        dialogRef[0].show();
    }

    private void showConfirmAddPastedPhotocardDialog(IdolCard card, String photoUri, ImageView ivMain, Runnable rebuildCallback) {
        if (card == null || photoUri == null || photoUri.trim().isEmpty()) return;
        ThemeManager.AppTheme theme = ThemeManager.getActiveTheme(this);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER_HORIZONTAL);
        int pad = dp(20);
        box.setPadding(pad, pad, pad, pad);
        box.setBackground(ThemeManager.createCardDrawable(this, theme.surfaceColor, theme.cardBorderColor, 26));

        TextView tvTitle = new TextView(this);
        tvTitle.setText(getString(R.string.dialog_paste_pc_confirm_title));
        tvTitle.setTextColor(theme.textColor);
        tvTitle.setTextSize(18f);
        tvTitle.setTypeface(Typeface.create("sans-serif-black", Typeface.BOLD));
        box.addView(tvTitle);

        TextView tvMsg = new TextView(this);
        tvMsg.setText(getString(R.string.dialog_paste_pc_confirm_msg, card.getStageName()));
        tvMsg.setTextColor(theme.getMutedTextColor());
        tvMsg.setTextSize(12f);
        tvMsg.setGravity(Gravity.CENTER);
        tvMsg.setPadding(0, dp(4), 0, dp(12));
        box.addView(tvMsg);

        CardView pcCard = new CardView(this);
        pcCard.setRadius(dp(18));
        pcCard.setCardElevation(dp(4));
        LinearLayout.LayoutParams ivLp = new LinearLayout.LayoutParams(dp(160), dp(220));
        ivLp.setMargins(0, 0, 0, dp(16));
        pcCard.setLayoutParams(ivLp);

        ImageView iv = new ImageView(this);
        iv.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
        CardImageLoader.loadUriIntoImageView(this, photoUri, iv, card.getStageName(), theme.primaryColor);
        pcCard.addView(iv);
        box.addView(pcCard);

        LinearLayout btnsRow = new LinearLayout(this);
        btnsRow.setOrientation(LinearLayout.HORIZONTAL);
        btnsRow.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        AlertDialog[] ref = new AlertDialog[1];

        TextView btnAdd = new TextView(this);
        btnAdd.setText(getString(R.string.btn_add_photocard));
        btnAdd.setTextColor(theme.getButtonTextColor(theme.primaryColor));
        btnAdd.setBackground(ThemeManager.createPillDrawable(this, theme.primaryColor, 20));
        btnAdd.setTextSize(13f);
        btnAdd.setTypeface(Typeface.DEFAULT_BOLD);
        btnAdd.setGravity(Gravity.CENTER);
        btnAdd.setPadding(dp(12), dp(10), dp(12), dp(10));
        LinearLayout.LayoutParams baLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.2f);
        baLp.setMargins(0, 0, dp(6), 0);
        btnAdd.setLayoutParams(baLp);
        btnAdd.setOnClickListener(v -> {
            card.addPhotocard(photoUri);
            card.setPhotoUri(photoUri);
            repo.saveOrUpdateIdol(card);
            if (ivMain != null) {
                CardImageLoader.loadCardImage(this, card, ivMain);
            }
            if (rebuildCallback != null) rebuildCallback.run();
            refreshCurrentTab();
            if (ref[0] != null) ref[0].dismiss();
            Toast.makeText(this, getString(R.string.toast_pc_added), Toast.LENGTH_SHORT).show();
        });
        btnsRow.addView(btnAdd);

        TextView btnCancel = new TextView(this);
        btnCancel.setText(getString(R.string.btn_cancel));
        btnCancel.setTextColor(Color.parseColor("#374151"));
        btnCancel.setBackground(ThemeManager.createPillDrawable(this, Color.parseColor("#E5E7EB"), 20));
        btnCancel.setTextSize(13f);
        btnCancel.setTypeface(Typeface.DEFAULT_BOLD);
        btnCancel.setGravity(Gravity.CENTER);
        btnCancel.setPadding(dp(12), dp(10), dp(12), dp(10));
        btnCancel.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        btnCancel.setOnClickListener(v -> {
            if (ref[0] != null) ref[0].dismiss();
        });
        btnsRow.addView(btnCancel);

        box.addView(btnsRow);

        builder.setView(box);
        ref[0] = builder.create();
        if (ref[0].getWindow() != null) {
            ref[0].getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        ref[0].show();
    }

    private View buildPhotocardBinderCell(IdolCard card, String pcUri, int idx, int totalPcs, ImageView ivMain, Runnable rebuildCallback) {
        LinearLayout pcItem = new LinearLayout(this);
        pcItem.setOrientation(LinearLayout.VERTICAL);
        pcItem.setGravity(Gravity.CENTER_HORIZONTAL);
        LinearLayout.LayoutParams pLp = new LinearLayout.LayoutParams(dp(102), ViewGroup.LayoutParams.WRAP_CONTENT);
        pLp.setMargins(0, 0, dp(8), 0);
        pcItem.setLayoutParams(pLp);

        boolean isUnlocked = card.isPhotocardUnlocked(pcUri);
        boolean isPrimaryCover = IdolCard.isSamePhoto(pcUri, card.getPhotoUri());

        CardView thumbCard = new CardView(this);
        thumbCard.setRadius(dp(14));
        thumbCard.setCardElevation(dp(isPrimaryCover ? 3 : 1));
        thumbCard.setLayoutParams(new LinearLayout.LayoutParams(dp(96), dp(126)));

        FrameLayout frame = new FrameLayout(this);
        frame.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        if (isUnlocked) {
            ImageView ivThumb = new ImageView(this);
            ivThumb.setLayoutParams(new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            ivThumb.setScaleType(ImageView.ScaleType.FIT_XY);
            IdolCard temp = new IdolCard(card.getId(), card.getStageName(),
                    card.getFullName(), card.getGroupName(), card.isSoloist(), card.getCountry(),
                    card.getBirthDate(), card.getRole(), pcUri, card.getAccentColor(), card.getBio(), card.getCollections());
            CardImageLoader.loadCardImageWithSize(this, temp, ivThumb, dp(192), dp(252));
            frame.addView(ivThumb);

            if (isPrimaryCover) {
                View goldBorder = new View(this);
                GradientDrawable gd = new GradientDrawable();
                gd.setCornerRadius(dp(14));
                gd.setStroke(dp(2), Color.parseColor("#F59E0B"));
                gd.setColor(Color.TRANSPARENT);
                goldBorder.setBackground(gd);
                goldBorder.setLayoutParams(new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                frame.addView(goldBorder);
            }

            // Tapping the photocard opens the Full-HD Pinch-to-Zoom Preview (does NOT overwrite main cover)
            frame.setOnClickListener(v -> showPhotocardZoomPreviewDialog(card, pcUri, idx + 1, ivMain, rebuildCallback));
            frame.setOnLongClickListener(v -> {
                if (card.getPhotocards().size() > 1) {
                    new AlertDialog.Builder(this)
                            .setTitle(getString(R.string.dialog_delete_pc_title))
                            .setMessage(getString(R.string.dialog_delete_pc_msg, card.getStageName()))
                            .setPositiveButton(getString(R.string.btn_delete_card), (d, w) -> {
                                boolean removed = card.removePhotocard(pcUri);
                                if (removed) {
                                    repo.saveOrUpdateIdol(card);
                                    if (ivMain != null) {
                                        CardImageLoader.loadCardImage(this, card, ivMain);
                                    }
                                    if (rebuildCallback != null) rebuildCallback.run();
                                    refreshCurrentTab();
                                    Toast.makeText(this, getString(R.string.toast_pc_deleted), Toast.LENGTH_SHORT).show();
                                }
                            })
                            .setNegativeButton(getString(R.string.btn_cancel), null)
                            .show();
                    return true;
                } else {
                    Toast.makeText(this, getString(R.string.toast_cannot_delete_only_pc), Toast.LENGTH_SHORT).show();
                    return true;
                }
            });
        } else {
            frame.setBackgroundColor(Color.parseColor("#1F2937"));
            TextView tvLock = new TextView(this);
            tvLock.setText("🔒\n#" + (idx + 1) + "\nGacha");
            tvLock.setTextColor(Color.parseColor("#E5E7EB"));
            tvLock.setTextSize(10.5f);
            tvLock.setTypeface(Typeface.DEFAULT_BOLD);
            tvLock.setGravity(Gravity.CENTER);
            FrameLayout.LayoutParams lkLp = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER);
            tvLock.setLayoutParams(lkLp);
            frame.addView(tvLock);
            frame.setOnClickListener(v -> Toast.makeText(this, getString(R.string.binder_locked_gacha_toast), Toast.LENGTH_SHORT).show());
        }

        thumbCard.addView(frame);
        pcItem.addView(thumbCard);

        // Separate dedicated button below each unlocked photocard to set it as Primary Cover Photo
        if (isUnlocked) {
            TextView btnSetCover = new TextView(this);
            btnSetCover.setText(isPrimaryCover ? getString(R.string.binder_cover_active) : getString(R.string.binder_cover_set));
            btnSetCover.setTextColor(Color.parseColor(isPrimaryCover ? "#B45309" : "#4B5563"));
            btnSetCover.setTextSize(10f);
            btnSetCover.setTypeface(Typeface.DEFAULT_BOLD);
            btnSetCover.setGravity(Gravity.CENTER);
            btnSetCover.setBackground(ThemeManager.createPillDrawable(this,
                    Color.parseColor(isPrimaryCover ? "#FEF3C7" : "#F3F4F6"), 12));
            btnSetCover.setPadding(dp(8), dp(4), dp(8), dp(4));
            LinearLayout.LayoutParams scLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            scLp.topMargin = dp(5);
            btnSetCover.setLayoutParams(scLp);
            btnSetCover.setOnClickListener(v -> {
                card.setPhotoUri(pcUri);
                repo.saveOrUpdateIdol(card);
                CardImageLoader.loadCardImage(this, card, ivMain);
                if (rebuildCallback != null) rebuildCallback.run();
                refreshCurrentTab();
                Toast.makeText(this, getString(R.string.binder_cover_updated_toast, card.getStageName()), Toast.LENGTH_SHORT).show();
            });
            pcItem.addView(btnSetCover);
        } else {
            TextView tvLockedSub = new TextView(this);
            tvLockedSub.setText(getString(R.string.binder_locked));
            tvLockedSub.setTextColor(Color.parseColor("#9CA3AF"));
            tvLockedSub.setTextSize(9.5f);
            tvLockedSub.setPadding(0, dp(5), 0, 0);
            pcItem.addView(tvLockedSub);
        }

        // Reorder buttons (◀ / ▶) to customize photocard positions!
        if (totalPcs > 1) {
            LinearLayout reorderRow = new LinearLayout(this);
            reorderRow.setOrientation(LinearLayout.HORIZONTAL);
            reorderRow.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams roLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            roLp.topMargin = dp(4);
            reorderRow.setLayoutParams(roLp);

            TextView btnLeft = new TextView(this);
            btnLeft.setText("◀");
            btnLeft.setTextSize(11f);
            btnLeft.setTextColor(idx > 0 ? Color.parseColor("#374151") : Color.parseColor("#D1D5DB"));
            btnLeft.setTypeface(Typeface.DEFAULT_BOLD);
            btnLeft.setGravity(Gravity.CENTER);
            btnLeft.setBackground(ThemeManager.createPillDrawable(this,
                    idx > 0 ? Color.parseColor("#E5E7EB") : Color.parseColor("#F9FAFB"), 10));
            btnLeft.setPadding(dp(8), dp(3), dp(8), dp(3));
            LinearLayout.LayoutParams bllp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            bllp.rightMargin = dp(2);
            btnLeft.setLayoutParams(bllp);
            if (idx > 0) {
                btnLeft.setOnClickListener(v -> {
                    card.swapPhotocards(idx, idx - 1);
                    repo.saveOrUpdateIdol(card);
                    if (rebuildCallback != null) rebuildCallback.run();
                    refreshCurrentTab();
                    Toast.makeText(this, getString(R.string.reorder_pc_moved, idx), Toast.LENGTH_SHORT).show();
                });
            }
            reorderRow.addView(btnLeft);

            TextView btnRight = new TextView(this);
            btnRight.setText("▶");
            btnRight.setTextSize(11f);
            btnRight.setTextColor(idx < totalPcs - 1 ? Color.parseColor("#374151") : Color.parseColor("#D1D5DB"));
            btnRight.setTypeface(Typeface.DEFAULT_BOLD);
            btnRight.setGravity(Gravity.CENTER);
            btnRight.setBackground(ThemeManager.createPillDrawable(this,
                    idx < totalPcs - 1 ? Color.parseColor("#E5E7EB") : Color.parseColor("#F9FAFB"), 10));
            btnRight.setPadding(dp(8), dp(3), dp(8), dp(3));
            LinearLayout.LayoutParams brlp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            brlp.leftMargin = dp(2);
            btnRight.setLayoutParams(brlp);
            if (idx < totalPcs - 1) {
                btnRight.setOnClickListener(v -> {
                    card.swapPhotocards(idx, idx + 1);
                    repo.saveOrUpdateIdol(card);
                    if (rebuildCallback != null) rebuildCallback.run();
                    refreshCurrentTab();
                    Toast.makeText(this, getString(R.string.reorder_pc_moved, idx + 2), Toast.LENGTH_SHORT).show();
                });
            }
            reorderRow.addView(btnRight);

            pcItem.addView(reorderRow);
        }

        return pcItem;
    }

    private void showPhotocardZoomPreviewDialog(IdolCard card, String pcUri, int pcNumber,
                                                ImageView ivMainInBinder, Runnable rebuildBinderCallback) {
        ThemeManager.AppTheme theme = ThemeManager.getActiveTheme(this);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        AlertDialog[] ref = new AlertDialog[1];

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(18);
        root.setPadding(pad, pad, pad, pad);
        root.setBackground(ThemeManager.createCardDrawable(this, theme.surfaceColor, theme.cardBorderColor, 26));

        TextView tvTitle = new TextView(this);
        tvTitle.setText(getString(R.string.zoom_preview_title_format, card.getStageName(), pcNumber));
        tvTitle.setTextColor(theme.textColor);
        tvTitle.setTextSize(17f);
        tvTitle.setTypeface(Typeface.create("sans-serif-black", Typeface.BOLD));
        root.addView(tvTitle);

        TextView tvHint = new TextView(this);
        tvHint.setText(getString(R.string.zoom_preview_hint));
        tvHint.setTextColor(theme.getMutedTextColor());
        tvHint.setTextSize(11.5f);
        tvHint.setPadding(0, dp(2), 0, dp(12));
        root.addView(tvHint);

        CardView zoomCard = new CardView(this);
        zoomCard.setRadius(dp(22));
        zoomCard.setCardElevation(dp(3));
        zoomCard.setCardBackgroundColor(Color.parseColor("#111827"));
        LinearLayout.LayoutParams zLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(420));
        zLp.setMargins(0, 0, 0, dp(14));
        zoomCard.setLayoutParams(zLp);

        ZoomablePhotocardView zoomView = new ZoomablePhotocardView(this);
        zoomView.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        CardImageLoader.loadHighResForZoomPreview(this, card, pcUri, zoomView);
        zoomCard.addView(zoomView);
        root.addView(zoomCard);

        boolean isCurrentCover = IdolCard.isSamePhoto(pcUri, card.getPhotoUri());

        TextView btnSetAsMain = new TextView(this);
        btnSetAsMain.setText(isCurrentCover ? getString(R.string.zoom_btn_current_cover) : getString(R.string.zoom_btn_set_cover));
        btnSetAsMain.setTextColor(isCurrentCover ? Color.parseColor("#B45309") : theme.getButtonTextColor(theme.primaryColor));
        btnSetAsMain.setTypeface(Typeface.DEFAULT_BOLD);
        btnSetAsMain.setTextSize(13f);
        btnSetAsMain.setGravity(Gravity.CENTER);
        btnSetAsMain.setBackground(ThemeManager.createPillDrawable(this,
                isCurrentCover ? Color.parseColor("#FEF3C7") : theme.primaryColor, 20));
        btnSetAsMain.setPadding(dp(14), dp(12), dp(14), dp(12));
        LinearLayout.LayoutParams smLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        smLp.bottomMargin = dp(8);
        btnSetAsMain.setLayoutParams(smLp);
        btnSetAsMain.setOnClickListener(v -> {
            if (pcUri != null && !pcUri.isEmpty()) {
                card.setPhotoUri(pcUri);
                repo.saveOrUpdateIdol(card);
                if (ivMainInBinder != null) {
                    CardImageLoader.loadCardImage(this, card, ivMainInBinder);
                }
                if (rebuildBinderCallback != null) {
                    rebuildBinderCallback.run();
                }
                refreshCurrentTab();
                btnSetAsMain.setText(getString(R.string.zoom_btn_current_cover));
                btnSetAsMain.setTextColor(Color.parseColor("#B45309"));
                btnSetAsMain.setBackground(ThemeManager.createPillDrawable(this, Color.parseColor("#FEF3C7"), 20));
                Toast.makeText(this, getString(R.string.binder_cover_updated_toast, card.getStageName()), Toast.LENGTH_SHORT).show();
            }
        });
        root.addView(btnSetAsMain);

        // Motivating invite share button for photocard (with app link at the end)
        TextView btnSharePc = new TextView(this);
        btnSharePc.setText(getString(R.string.btn_share_photocard));
        btnSharePc.setTextColor(Color.WHITE);
        btnSharePc.setTypeface(Typeface.DEFAULT_BOLD);
        btnSharePc.setTextSize(13f);
        btnSharePc.setGravity(Gravity.CENTER);
        btnSharePc.setBackground(ThemeManager.createPillDrawable(this, theme.secondaryColor, 20));
        btnSharePc.setPadding(dp(14), dp(11), dp(14), dp(11));
        LinearLayout.LayoutParams spLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        spLp.bottomMargin = dp(8);
        btnSharePc.setLayoutParams(spLp);
        btnSharePc.setOnClickListener(v -> shareIdolPhotocardInvite(card, pcUri));
        root.addView(btnSharePc);

        if (card.getPhotocards().size() > 1) {
            TextView btnDeletePc = new TextView(this);
            btnDeletePc.setText(getString(R.string.zoom_btn_delete_photocard));
            btnDeletePc.setTextColor(Color.parseColor("#DC2626"));
            btnDeletePc.setTypeface(Typeface.DEFAULT_BOLD);
            btnDeletePc.setTextSize(13f);
            btnDeletePc.setGravity(Gravity.CENTER);
            btnDeletePc.setBackground(ThemeManager.createPillDrawable(this, Color.parseColor("#FEE2E2"), 20));
            btnDeletePc.setPadding(dp(14), dp(11), dp(14), dp(11));
            LinearLayout.LayoutParams dpLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            dpLp.bottomMargin = dp(8);
            btnDeletePc.setLayoutParams(dpLp);
            btnDeletePc.setOnClickListener(v -> {
                new AlertDialog.Builder(this)
                        .setTitle(getString(R.string.dialog_delete_pc_title))
                        .setMessage(getString(R.string.dialog_delete_pc_msg, card.getStageName()))
                        .setPositiveButton(getString(R.string.btn_delete_card), (d, w) -> {
                            boolean removed = card.removePhotocard(pcUri);
                            if (removed) {
                                repo.saveOrUpdateIdol(card);
                                if (ivMainInBinder != null) {
                                    CardImageLoader.loadCardImage(this, card, ivMainInBinder);
                                }
                                if (rebuildBinderCallback != null) rebuildBinderCallback.run();
                                refreshCurrentTab();
                                if (ref[0] != null) ref[0].dismiss();
                                Toast.makeText(this, getString(R.string.toast_pc_deleted), Toast.LENGTH_SHORT).show();
                            }
                        })
                        .setNegativeButton(getString(R.string.btn_cancel), null)
                        .show();
            });
            root.addView(btnDeletePc);
        }

        TextView btnClose = new TextView(this);
        btnClose.setText(getString(R.string.zoom_btn_close));
        btnClose.setTextColor(Color.parseColor("#374151"));
        btnClose.setTypeface(Typeface.DEFAULT_BOLD);
        btnClose.setTextSize(13f);
        btnClose.setGravity(Gravity.CENTER);
        btnClose.setBackground(ThemeManager.createPillDrawable(this, Color.parseColor("#F3F4F6"), 20));
        btnClose.setPadding(dp(14), dp(11), dp(14), dp(11));
        btnClose.setOnClickListener(v -> {
            if (ref[0] != null) ref[0].dismiss();
        });
        root.addView(btnClose);

        builder.setView(root);
        ref[0] = builder.create();
        if (ref[0].getWindow() != null) {
            ref[0].getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        ref[0].show();
    }

    private void shareIdolPhotocardInvite(IdolCard card, String pcUri) {
        shareIdolPhotocardInvite(card, pcUri, null);
    }

    private void shareIdolPhotocardInvite(IdolCard card, String pcUri, ImageView ivSource) {
        if (card == null) return;
        String appUrl = "https://is.gd/whosyourbias";
        String groupLabel = card.isSoloist() ? getString(R.string.label_soloist) : card.getGroupName();
        String inviteText = getString(R.string.share_photocard_invite_text,
                card.getStageName(), groupLabel, appUrl);

        try {
            Bitmap bmp = null;
            if (pcUri != null && !pcUri.trim().isEmpty()) {
                bmp = CardImageLoader.getCachedBitmap(this, pcUri);
            }
            if (bmp == null && card.getPhotoUri() != null) {
                bmp = CardImageLoader.getCachedBitmap(this, card.getPhotoUri());
            }
            if (bmp == null && ivSource != null && ivSource.getDrawable() instanceof android.graphics.drawable.BitmapDrawable) {
                bmp = ((android.graphics.drawable.BitmapDrawable) ivSource.getDrawable()).getBitmap();
            }

            File sharedDir = new File(getCacheDir(), "shared");
            if (!sharedDir.exists()) sharedDir.mkdirs();
            File shareFile = new File(sharedDir, "photocard_" + card.getStageName().replaceAll("[^a-zA-Z0-9]", "_") + ".jpg");

            if (bmp != null) {
                try (FileOutputStream fos = new FileOutputStream(shareFile)) {
                    bmp.compress(Bitmap.CompressFormat.JPEG, 92, fos);
                }
            }

            if (shareFile.exists() && shareFile.length() > 0) {
                Uri contentUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", shareFile);
                Intent share = new Intent(Intent.ACTION_SEND);
                share.setType("image/jpeg");
                share.putExtra(Intent.EXTRA_STREAM, contentUri);
                share.putExtra(Intent.EXTRA_TEXT, inviteText);
                share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                startActivity(Intent.createChooser(share, getString(R.string.btn_share_photocard)));
                return;
            }
        } catch (Exception ignored) {
        }

        String fallbackMsg = inviteText;
        if (pcUri != null && pcUri.startsWith("http")) {
            fallbackMsg = "📸 " + pcUri + "\n\n" + inviteText;
        }
        Intent txtShare = new Intent(Intent.ACTION_SEND);
        txtShare.setType("text/plain");
        txtShare.putExtra(Intent.EXTRA_TEXT, fallbackMsg);
        startActivity(Intent.createChooser(txtShare, getString(R.string.btn_share_photocard)));
    }

    public void showCreateOrEditIdolDialog(IdolCard existing, Runnable onSavedCallback) {
        final boolean isEditing = (existing != null);
        final IdolCard card = isEditing ? existing : new IdolCard();
        ThemeManager.AppTheme theme = ThemeManager.getActiveTheme(this);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        ScrollView scroll = new ScrollView(this);
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(20);
        form.setPadding(pad, pad, pad, pad);
        form.setBackground(ThemeManager.createCardDrawable(this, theme.surfaceColor, theme.cardBorderColor, 26));

        AlertDialog[] ref = new AlertDialog[1];

        TextView tvTitle = new TextView(this);
        tvTitle.setText(isEditing ? getString(R.string.title_edit_idol) : getString(R.string.title_new_idol));
        tvTitle.setTextColor(theme.textColor);
        tvTitle.setTextSize(18f);
        tvTitle.setTypeface(Typeface.create("sans-serif-black", Typeface.BOLD));
        tvTitle.setPadding(0, 0, 0, dp(4));
        form.addView(tvTitle);

        TextView tvSub = new TextView(this);
        tvSub.setText(getString(isEditing ? R.string.idol_edit_sub_edit : R.string.idol_edit_sub_create));
        tvSub.setTextColor(theme.getMutedTextColor());
        tvSub.setTextSize(12f);
        tvSub.setPadding(0, 0, 0, dp(12));
        form.addView(tvSub);

        // Quick Autocomplete from K-Pop Wiki Pill Button
        if (!isEditing) {
            TextView btnWikiAuto = new TextView(this);
            btnWikiAuto.setText(getString(R.string.btn_wiki_autocomplete));
            btnWikiAuto.setTextColor(Color.parseColor("#2563EB"));
            btnWikiAuto.setBackground(ThemeManager.createPillDrawable(this, Color.parseColor("#EFF6FF"), 16));
            btnWikiAuto.setTextSize(12.5f);
            btnWikiAuto.setTypeface(Typeface.DEFAULT_BOLD);
            btnWikiAuto.setGravity(Gravity.CENTER);
            btnWikiAuto.setPadding(dp(12), dp(9), dp(12), dp(9));
            LinearLayout.LayoutParams waLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            waLp.setMargins(0, 0, 0, dp(14));
            btnWikiAuto.setLayoutParams(waLp);
            btnWikiAuto.setOnClickListener(v -> {
                if (ref[0] != null) ref[0].dismiss();
                showWikiSearchAndImportDialog();
            });
            form.addView(btnWikiAuto);
        }

        CardView prevCard = new CardView(this);
        prevCard.setRadius(dp(16));
        prevCard.setCardElevation(dp(2));
        LinearLayout.LayoutParams prevLp = new LinearLayout.LayoutParams(dp(120), dp(150));
        prevLp.gravity = Gravity.CENTER_HORIZONTAL;
        prevLp.setMargins(0, 0, 0, dp(14));
        prevCard.setLayoutParams(prevLp);

        ImageView ivPreview = new ImageView(this);
        ivPreview.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        ivPreview.setScaleType(ImageView.ScaleType.CENTER_CROP);
        if (card.getPhotoUri() != null && !card.getPhotoUri().isEmpty()) {
            CardImageLoader.loadCardImage(this, card, ivPreview);
        }
        prevCard.addView(ivPreview);
        form.addView(prevCard);

        addSmallLabel(form, getString(R.string.label_stage_name));
        EditText etStage = addFormInput(form, getString(R.string.hint_stage_name), card.getStageName());

        addSmallLabel(form, getString(R.string.label_full_name));
        EditText etFull = addFormInput(form, getString(R.string.hint_full_name), card.getFullName());

        addSmallLabel(form, getString(R.string.label_native_name));
        EditText etNative = addFormInput(form, "ej: 이원희 • 사카이 모카 / 堺 萌花", card.getNativeName());

        addSmallLabel(form, getString(R.string.label_group));
        EditText etGroup = addFormInput(form, getString(R.string.hint_group_name), card.isSoloist() ? "" : card.getGroupName());

        CheckBox cbSoloist = new CheckBox(this);
        cbSoloist.setText(getString(R.string.check_is_soloist));
        cbSoloist.setTextColor(Color.parseColor("#111827"));
        cbSoloist.setChecked(card.isSoloist());
        LinearLayout.LayoutParams cbLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cbLp.setMargins(0, 0, 0, dp(8));
        cbSoloist.setLayoutParams(cbLp);
        form.addView(cbSoloist);

        addSmallLabel(form, getString(R.string.label_country));
        EditText etCountry = addFormInput(form, getString(R.string.hint_country), card.getCountry());

        addSmallLabel(form, getString(R.string.label_birth));
        EditText etBirth = addFormInput(form, getString(R.string.hint_birth_date), card.getBirthDate());

        addSmallLabel(form, getString(R.string.label_role));
        EditText etRole = addFormInput(form, getString(R.string.hint_role), card.getRole());

        addSmallLabel(form, getString(R.string.label_photo_url));
        EditText etPhoto = addFormInput(form, getString(R.string.hint_photo_url), card.getPhotoUri());

        LinearLayout photoBtnsRow = new LinearLayout(this);
        photoBtnsRow.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams pfLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        pfLp.setMargins(0, 0, 0, dp(12));
        photoBtnsRow.setLayoutParams(pfLp);

        TextView btnPickFile = new TextView(this);
        btnPickFile.setText(getString(R.string.btn_gallery));
        btnPickFile.setTextColor(theme.getButtonTextColor(theme.primaryColor));
        btnPickFile.setTypeface(Typeface.DEFAULT_BOLD);
        btnPickFile.setTextSize(12f);
        btnPickFile.setGravity(Gravity.CENTER);
        btnPickFile.setBackground(ThemeManager.createPillDrawable(this, theme.primaryColor, 16));
        btnPickFile.setPadding(dp(10), dp(10), dp(10), dp(10));
        LinearLayout.LayoutParams b1p = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        b1p.rightMargin = dp(6);
        btnPickFile.setLayoutParams(b1p);
        btnPickFile.setOnClickListener(v -> {
            pendingPhotoUrlField = etPhoto;
            pendingPhotoPreviewView = ivPreview;
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            startActivityForResult(Intent.createChooser(intent, getString(R.string.btn_gallery)), REQ_PICK_CARD_PHOTO);
        });
        photoBtnsRow.addView(btnPickFile);

        TextView btnPastePhoto = new TextView(this);
        btnPastePhoto.setText(getString(R.string.btn_paste_photo));
        btnPastePhoto.setTextColor(Color.parseColor("#374151"));
        btnPastePhoto.setTypeface(Typeface.DEFAULT_BOLD);
        btnPastePhoto.setTextSize(12f);
        btnPastePhoto.setGravity(Gravity.CENTER);
        btnPastePhoto.setBackground(ThemeManager.createPillDrawable(this, Color.parseColor("#F3F4F6"), 16));
        btnPastePhoto.setPadding(dp(10), dp(10), dp(10), dp(10));
        btnPastePhoto.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.3f));
        btnPastePhoto.setOnClickListener(v -> {
            String pasted = pasteImageOrUrlFromClipboard();
            if (pasted != null && !pasted.isEmpty()) {
                etPhoto.setText(pasted);
                CardImageLoader.loadUriIntoImageView(this, pasted, ivPreview, etStage.getText().toString(), Color.parseColor("#FF85A2"));
                Toast.makeText(this, getString(R.string.toast_photo_pasted), Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, getString(R.string.toast_copy_first), Toast.LENGTH_SHORT).show();
            }
        });
        photoBtnsRow.addView(btnPastePhoto);
        form.addView(photoBtnsRow);

        addSmallLabel(form, getString(R.string.label_collections));
        EditText etCols = addFormInput(form, getString(R.string.hint_collections), card.getCollections());

        addSmallLabel(form, getString(R.string.label_bio));
        EditText etBio = addFormInput(form, getString(R.string.hint_bio), card.getBio());

        // Bottom Action Buttons (Rounded Pills)
        LinearLayout actionsRow = new LinearLayout(this);
        actionsRow.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams actLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        actLp.topMargin = dp(14);
        actionsRow.setLayoutParams(actLp);

        TextView btnSave = new TextView(this);
        btnSave.setText("💾 " + getString(R.string.btn_save));
        btnSave.setTextColor(theme.getButtonTextColor(theme.primaryColor));
        btnSave.setTypeface(Typeface.DEFAULT_BOLD);
        btnSave.setTextSize(13f);
        btnSave.setGravity(Gravity.CENTER);
        btnSave.setBackground(ThemeManager.createPillDrawable(this, theme.primaryColor, 20));
        btnSave.setPadding(dp(14), dp(11), dp(14), dp(11));
        LinearLayout.LayoutParams svLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.3f);
        svLp.rightMargin = dp(6);
        btnSave.setLayoutParams(svLp);
        btnSave.setOnClickListener(v -> {
            String stage = etStage.getText().toString().trim();
            if (stage.isEmpty()) return;
            card.setStageName(stage);
            card.setFullName(etFull.getText().toString().trim());
            card.setNativeName(etNative.getText().toString().trim());
            boolean solo = cbSoloist.isChecked() || etGroup.getText().toString().trim().isEmpty();
            card.setSoloist(solo);
            card.setGroupName(solo ? "Soloist" : etGroup.getText().toString().trim());
            card.setCountry(etCountry.getText().toString().trim());
            card.setBirthDate(etBirth.getText().toString().trim());
            card.setRole(etRole.getText().toString().trim());
            String pUri = etPhoto.getText().toString().trim();
            card.setPhotoUri(pUri);
            if (!pUri.isEmpty()) card.addPhotocard(pUri);
            card.setCollections(etCols.getText().toString().trim());
            card.setBio(etBio.getText().toString().trim());

            repo.saveOrUpdateIdol(card);
            if (onSavedCallback != null) onSavedCallback.run();
            if (ref[0] != null) ref[0].dismiss();
        });
        actionsRow.addView(btnSave);

        if (isEditing) {
            TextView btnDelete = new TextView(this);
            btnDelete.setText(getString(R.string.btn_delete_card));
            btnDelete.setTextColor(Color.parseColor("#DC2626"));
            btnDelete.setTypeface(Typeface.DEFAULT_BOLD);
            btnDelete.setTextSize(12f);
            btnDelete.setGravity(Gravity.CENTER);
            btnDelete.setBackground(ThemeManager.createPillDrawable(this, Color.parseColor("#FEE2E2"), 20));
            btnDelete.setPadding(dp(10), dp(11), dp(10), dp(11));
            LinearLayout.LayoutParams dlLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            dlLp.rightMargin = dp(6);
            btnDelete.setLayoutParams(dlLp);
            btnDelete.setOnClickListener(v -> {
                repo.deleteIdol(card.getId());
                if (onSavedCallback != null) onSavedCallback.run();
                if (ref[0] != null) ref[0].dismiss();
            });
            actionsRow.addView(btnDelete);
        }

        TextView btnCancel = new TextView(this);
        btnCancel.setText(getString(R.string.btn_cancel));
        btnCancel.setTextColor(Color.parseColor("#374151"));
        btnCancel.setTypeface(Typeface.DEFAULT_BOLD);
        btnCancel.setTextSize(12f);
        btnCancel.setGravity(Gravity.CENTER);
        btnCancel.setBackground(ThemeManager.createPillDrawable(this, Color.parseColor("#E5E7EB"), 20));
        btnCancel.setPadding(dp(12), dp(11), dp(12), dp(11));
        LinearLayout.LayoutParams cnLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        btnCancel.setLayoutParams(cnLp);
        btnCancel.setOnClickListener(v -> {
            if (ref[0] != null) ref[0].dismiss();
        });
        actionsRow.addView(btnCancel);

        form.addView(actionsRow);

        scroll.addView(form);
        builder.setView(scroll);
        ref[0] = builder.create();
        if (ref[0].getWindow() != null) {
            ref[0].getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        ref[0].show();
    }

    private void showShareChoiceDialog(String title, CustomMatch match, List<IdolCard> cards) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.share_mode_title));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(18);
        root.setPadding(pad, pad, pad, pad);
        ThemeManager.AppTheme themeShareChoice = ThemeManager.getActiveTheme(this);
        root.setBackground(ThemeManager.createCardDrawable(this, themeShareChoice.surfaceColor, themeShareChoice.cardBorderColor, 24));

        AlertDialog[] ref = new AlertDialog[1];

        TextView btnFile = new TextView(this);
        btnFile.setText("📁 " + getString(R.string.share_option_file));
        btnFile.setTextColor(themeShareChoice.getButtonTextColor(themeShareChoice.primaryColor));
        btnFile.setTypeface(Typeface.DEFAULT_BOLD);
        btnFile.setTextSize(13f);
        btnFile.setPadding(dp(14), dp(13), dp(14), dp(13));
        btnFile.setBackground(ThemeManager.createPillDrawable(this, themeShareChoice.primaryColor, 20));
        LinearLayout.LayoutParams fLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        fLp.setMargins(0, 0, 0, dp(10));
        btnFile.setLayoutParams(fLp);
        btnFile.setOnClickListener(v -> {
            if (ref[0] != null) ref[0].dismiss();
            ShareImportManager.shareAsWybFile(this, title, match, cards);
        });
        root.addView(btnFile);

        TextView btnCode = new TextView(this);
        btnCode.setText(getString(R.string.share_option_code));
        btnCode.setTextColor(Color.parseColor("#111827"));
        btnCode.setTypeface(Typeface.DEFAULT_BOLD);
        btnCode.setTextSize(13f);
        btnCode.setPadding(dp(14), dp(14), dp(14), dp(14));
        ThemeManager.AppTheme themeShareChoice2 = ThemeManager.getActiveTheme(this);
        btnCode.setBackground(ThemeManager.createPillDrawable(this, themeShareChoice2.secondaryColor, 20));
        btnCode.setTextColor(themeShareChoice2.getButtonTextColor(themeShareChoice2.secondaryColor));
        btnCode.setOnClickListener(v -> {
            if (ref[0] != null) ref[0].dismiss();
            ShareImportManager.shareAsWhatsAppCode(this, title, match, cards);
        });
        root.addView(btnCode);

        builder.setView(root);
        builder.setNegativeButton(getString(R.string.btn_cancel), null);
        ref[0] = builder.create();
        if (ref[0].getWindow() != null) {
            ref[0].getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        ref[0].show();
    }

    private void showImportHubDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.btn_import_whatsapp));

        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(18);
        root.setPadding(pad, pad, pad, pad);
        ThemeManager.AppTheme themeHub = ThemeManager.getActiveTheme(this);
        root.setBackground(ThemeManager.createCardDrawable(this, themeHub.surfaceColor, themeHub.cardBorderColor, 24));
        scroll.addView(root);

        AlertDialog[] ref = new AlertDialog[1];

        TextView btnPickWyb = new TextView(this);
        btnPickWyb.setText("📂 " + getString(R.string.btn_import_file));
        btnPickWyb.setTextColor(themeHub.getButtonTextColor(themeHub.primaryColor));
        btnPickWyb.setTypeface(Typeface.DEFAULT_BOLD);
        btnPickWyb.setGravity(Gravity.CENTER);
        btnPickWyb.setPadding(dp(14), dp(13), dp(14), dp(13));
        btnPickWyb.setBackground(ThemeManager.createPillDrawable(this, themeHub.primaryColor, 20));
        LinearLayout.LayoutParams pLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        pLp.setMargins(0, 0, 0, dp(12));
        btnPickWyb.setLayoutParams(pLp);
        btnPickWyb.setOnClickListener(v -> {
            if (ref[0] != null) ref[0].dismiss();
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("*/*");
            startActivityForResult(Intent.createChooser(intent, getString(R.string.btn_import_file)), REQ_PICK_WYB_FILE);
        });
        root.addView(btnPickWyb);

        ClipboardManager cb = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        String clipCode = null;
        if (cb != null && cb.hasPrimaryClip() && cb.getPrimaryClip() != null && cb.getPrimaryClip().getItemCount() > 0) {
            CharSequence clipText = cb.getPrimaryClip().getItemAt(0).getText();
            if (clipText != null && clipText.toString().contains("WYB1-")) {
                clipCode = clipText.toString();
            }
        }

        if (clipCode != null) {
            final String finalClip = clipCode;
            TextView btnPasteClip = new TextView(this);
            btnPasteClip.setText(getString(R.string.import_paste_clipboard));
            btnPasteClip.setTextColor(themeHub.getButtonTextColor(themeHub.secondaryColor));
            btnPasteClip.setBackground(ThemeManager.createPillDrawable(this, themeHub.secondaryColor, 20));
            btnPasteClip.setTypeface(Typeface.DEFAULT_BOLD);
            btnPasteClip.setGravity(Gravity.CENTER);
            btnPasteClip.setPadding(dp(14), dp(12), dp(14), dp(12));
            LinearLayout.LayoutParams pcLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            pcLp.setMargins(0, 0, 0, dp(12));
            btnPasteClip.setLayoutParams(pcLp);
            btnPasteClip.setOnClickListener(v -> {
                ShareImportManager.SharedPayload payload = ShareImportManager.decodeFromTextOrCode(this, finalClip);
                if (payload != null && (!payload.cards.isEmpty() || payload.customMatch != null)) {
                    if (ref[0] != null) ref[0].dismiss();
                    dispatchImportPayload(payload);
                } else {
                    Toast.makeText(this, getString(R.string.toast_invalid_wyb_clipboard), Toast.LENGTH_SHORT).show();
                }
            });
            root.addView(btnPasteClip);
        }

        EditText etCode = addFormInput(root, getString(R.string.import_paste_clipboard), clipCode != null ? clipCode : "");
        etCode.setMaxLines(3);
        etCode.setLines(3);
        etCode.setGravity(Gravity.TOP | Gravity.START);
        etCode.setVerticalScrollBarEnabled(true);

        TextView btnDoImport = new TextView(this);
        btnDoImport.setText(getString(R.string.import_btn_action));
        btnDoImport.setTextColor(themeHub.getButtonTextColor(themeHub.primaryColor));
        btnDoImport.setBackground(ThemeManager.createPillDrawable(this, themeHub.primaryColor, 20));
        btnDoImport.setTypeface(Typeface.DEFAULT_BOLD);
        btnDoImport.setGravity(Gravity.CENTER);
        btnDoImport.setPadding(dp(14), dp(13), dp(14), dp(13));
        LinearLayout.LayoutParams diLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        diLp.setMargins(0, dp(4), 0, dp(6));
        btnDoImport.setLayoutParams(diLp);
        btnDoImport.setOnClickListener(v -> {
            String raw = etCode.getText().toString().trim();
            ShareImportManager.SharedPayload payload = ShareImportManager.decodeFromTextOrCode(this, raw);
            if (payload == null || (payload.cards.isEmpty() && payload.customMatch == null)) {
                Toast.makeText(this, getString(R.string.toast_invalid_wyb_code), Toast.LENGTH_SHORT).show();
                return;
            }
            if (ref[0] != null) ref[0].dismiss();
            dispatchImportPayload(payload);
        });
        root.addView(btnDoImport);

        builder.setView(scroll);
        builder.setPositiveButton(getString(R.string.import_btn_action), (d, w) -> {
            String raw = etCode.getText().toString().trim();
            ShareImportManager.SharedPayload payload = ShareImportManager.decodeFromTextOrCode(this, raw);
            if (payload == null || (payload.cards.isEmpty() && payload.customMatch == null)) {
                Toast.makeText(this, getString(R.string.toast_invalid_wyb_code), Toast.LENGTH_SHORT).show();
                return;
            }
            dispatchImportPayload(payload);
        });
        builder.setNegativeButton(getString(R.string.btn_cancel), null);
        ref[0] = builder.create();
        if (ref[0].getWindow() != null) {
            ref[0].getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        ref[0].show();
    }

    private void dispatchImportPayload(ShareImportManager.SharedPayload payload) {
        if (payload == null) return;
        if (payload.customMatch != null) {
            showImportMatchActionDialog(payload);
        } else if (!payload.cards.isEmpty()) {
            showSelectiveImportDialog(payload);
        }
    }

    private void showImportMatchActionDialog(ShareImportManager.SharedPayload payload) {
        if (payload == null) return;
        if (payload.customMatch == null) {
            showSelectiveImportDialog(payload);
            return;
        }

        CustomMatch cm = payload.customMatch;
        ThemeManager.AppTheme theme = ThemeManager.getActiveTheme(this);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(20);
        box.setPadding(pad, pad, pad, pad);
        box.setBackground(ThemeManager.createCardDrawable(this, theme.surfaceColor, theme.cardBorderColor, 24));

        AlertDialog[] ref = new AlertDialog[1];

        TextView tvTitle = new TextView(this);
        tvTitle.setText(getString(R.string.dialog_import_match_title));
        tvTitle.setTextColor(theme.textColor);
        tvTitle.setTextSize(18f);
        tvTitle.setTypeface(Typeface.create("sans-serif-black", Typeface.BOLD));
        box.addView(tvTitle);

        TextView tvMsg = new TextView(this);
        tvMsg.setText(getString(R.string.dialog_import_match_msg,
                cm.getTitle(),
                cm.getShortId(),
                cm.getGameMode(),
                payload.cards != null ? payload.cards.size() : cm.getCardIds().size()));
        tvMsg.setTextColor(Color.parseColor("#4B5563"));
        tvMsg.setTextSize(13f);
        tvMsg.setPadding(0, dp(8), 0, dp(16));
        box.addView(tvMsg);

        // 1. [ 🚀 Import & Play ]
        TextView btnImportPlay = new TextView(this);
        btnImportPlay.setText(getString(R.string.btn_import_and_play));
        btnImportPlay.setTextColor(theme.getButtonTextColor(theme.primaryColor));
        btnImportPlay.setBackground(ThemeManager.createPillDrawable(this, theme.primaryColor, 20));
        btnImportPlay.setTextSize(13.5f);
        btnImportPlay.setTypeface(Typeface.DEFAULT_BOLD);
        btnImportPlay.setGravity(Gravity.CENTER);
        btnImportPlay.setPadding(dp(14), dp(12), dp(14), dp(12));
        LinearLayout.LayoutParams ipLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        ipLp.bottomMargin = dp(8);
        btnImportPlay.setLayoutParams(ipLp);
        btnImportPlay.setOnClickListener(v -> {
            if (ref[0] != null) ref[0].dismiss();
            if (payload.cards != null) {
                for (IdolCard c : payload.cards) repo.saveOrUpdateIdol(c);
            }
            repo.saveOrUpdateCustomMatch(cm);
            refreshCurrentTab();
            List<IdolCard> matchCards = new ArrayList<>();
            for (String cid : cm.getCardIds()) {
                IdolCard c = repo.getIdolById(cid);
                if (c != null) matchCards.add(c);
            }
            if (matchCards.isEmpty() && payload.cards != null) matchCards.addAll(payload.cards);
            launchMatch(cm.getTitle(), cm.getGameMode(), matchCards);
        });
        box.addView(btnImportPlay);

        // 2. [ ▶ Play Only ]
        TextView btnPlayOnly = new TextView(this);
        btnPlayOnly.setText(getString(R.string.btn_play_only));
        btnPlayOnly.setTextColor(theme.getButtonTextColor(theme.secondaryColor));
        btnPlayOnly.setBackground(ThemeManager.createPillDrawable(this, theme.secondaryColor, 20));
        btnPlayOnly.setTextSize(13f);
        btnPlayOnly.setTypeface(Typeface.DEFAULT_BOLD);
        btnPlayOnly.setGravity(Gravity.CENTER);
        btnPlayOnly.setPadding(dp(14), dp(11), dp(14), dp(11));
        LinearLayout.LayoutParams poLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        poLp.bottomMargin = dp(8);
        btnPlayOnly.setLayoutParams(poLp);
        btnPlayOnly.setOnClickListener(v -> {
            if (ref[0] != null) ref[0].dismiss();
            List<IdolCard> matchCards = new ArrayList<>();
            if (payload.cards != null && !payload.cards.isEmpty()) {
                matchCards.addAll(payload.cards);
            } else {
                for (String cid : cm.getCardIds()) {
                    IdolCard c = repo.getIdolById(cid);
                    if (c != null) matchCards.add(c);
                }
            }
            launchMatch(cm.getTitle(), cm.getGameMode(), matchCards);
        });
        box.addView(btnPlayOnly);

        // 3. [ 💾 Save to Matches ]
        TextView btnImportOnly = new TextView(this);
        btnImportOnly.setText(getString(R.string.btn_import_only));
        btnImportOnly.setTextColor(Color.parseColor("#374151"));
        btnImportOnly.setBackground(ThemeManager.createPillDrawable(this, Color.parseColor("#F3F4F6"), 20));
        btnImportOnly.setTextSize(12.5f);
        btnImportOnly.setTypeface(Typeface.DEFAULT_BOLD);
        btnImportOnly.setGravity(Gravity.CENTER);
        btnImportOnly.setPadding(dp(14), dp(10), dp(14), dp(10));
        LinearLayout.LayoutParams ioLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        ioLp.bottomMargin = dp(8);
        btnImportOnly.setLayoutParams(ioLp);
        btnImportOnly.setOnClickListener(v -> {
            if (ref[0] != null) ref[0].dismiss();
            if (payload.cards != null) {
                for (IdolCard c : payload.cards) repo.saveOrUpdateIdol(c);
            }
            repo.saveOrUpdateCustomMatch(cm);
            refreshCurrentTab();
            Toast.makeText(this, getString(R.string.toast_match_saved_with_id, cm.getShortId()), Toast.LENGTH_SHORT).show();
        });
        box.addView(btnImportOnly);

        // 4. [ Cancel ]
        TextView btnCancel = new TextView(this);
        btnCancel.setText(getString(R.string.btn_cancel));
        btnCancel.setTextColor(Color.parseColor("#6B7280"));
        btnCancel.setTextSize(12.5f);
        btnCancel.setGravity(Gravity.CENTER);
        btnCancel.setPadding(dp(12), dp(8), dp(12), dp(8));
        btnCancel.setOnClickListener(v -> {
            if (ref[0] != null) ref[0].dismiss();
        });
        box.addView(btnCancel);

        builder.setView(box);
        ref[0] = builder.create();
        if (ref[0].getWindow() != null) {
            ref[0].getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        ref[0].show();
        ref[0].show();
    }

    private void showSelectiveImportDialog(ShareImportManager.SharedPayload payload) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.import_preview_title));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(16);
        root.setPadding(pad, pad, pad, pad);
        root.setBackgroundColor(Color.WHITE);

        TextView tvPackName = new TextView(this);
        tvPackName.setText("📦 " + payload.packTitle + " (" + payload.cards.size() + " Idols)");
        tvPackName.setTextColor(Color.parseColor("#111827"));
        tvPackName.setTextSize(15f);
        tvPackName.setTypeface(Typeface.DEFAULT_BOLD);
        root.addView(tvPackName);

        TextView tvDesc = new TextView(this);
        tvDesc.setText(getString(R.string.import_preview_desc));
        tvDesc.setTextColor(Color.parseColor("#6B7280"));
        tvDesc.setTextSize(12f);
        tvDesc.setPadding(0, dp(4), 0, dp(8));
        root.addView(tvDesc);

        final Set<String> checkedCardIds = new LinkedHashSet<>();
        for (IdolCard c : payload.cards) checkedCardIds.add(c.getId());

        ScrollView listScroll = new ScrollView(this);
        listScroll.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(270)));
        LinearLayout itemsContainer = new LinearLayout(this);
        itemsContainer.setOrientation(LinearLayout.VERTICAL);
        listScroll.addView(itemsContainer);

        Runnable rebuildImportRows = new Runnable() {
            @Override
            public void run() {
                itemsContainer.removeAllViews();
                for (IdolCard inc : payload.cards) {
                    LinearLayout row = new LinearLayout(MainActivity.this);
                    row.setOrientation(LinearLayout.HORIZONTAL);
                    row.setGravity(Gravity.CENTER_VERTICAL);
                    row.setPadding(dp(8), dp(6), dp(8), dp(6));
                    row.setBackgroundResource(R.drawable.bg_chip_tag);
                    LinearLayout.LayoutParams rLp = new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                    rLp.setMargins(0, 0, 0, dp(6));
                    row.setLayoutParams(rLp);

                    CheckBox cb = new CheckBox(MainActivity.this);
                    cb.setChecked(checkedCardIds.contains(inc.getId()));
                    cb.setOnCheckedChangeListener((b, ch) -> {
                        if (ch) checkedCardIds.add(inc.getId());
                        else checkedCardIds.remove(inc.getId());
                    });
                    row.addView(cb);

                    ImageView iv = new ImageView(MainActivity.this);
                    LinearLayout.LayoutParams ivLp = new LinearLayout.LayoutParams(dp(44), dp(50));
                    ivLp.setMargins(0, 0, dp(8), 0);
                    iv.setLayoutParams(ivLp);
                    iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
                    CardImageLoader.loadCardImage(MainActivity.this, inc, iv);
                    row.addView(iv);

                    TextView tvInfo = new TextView(MainActivity.this);
                    tvInfo.setText(inc.getStageName() + " (" + inc.getGroupName() + ")\n" + inc.getRole());
                    tvInfo.setTextColor(Color.parseColor("#111827"));
                    tvInfo.setTextSize(12f);
                    tvInfo.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
                    row.addView(tvInfo);

                    TextView btnEditInc = new TextView(MainActivity.this);
                    btnEditInc.setText(getString(R.string.btn_edit_data));
                    btnEditInc.setTextColor(Color.parseColor("#111827"));
                    btnEditInc.setTextSize(11f);
                    btnEditInc.setTypeface(Typeface.DEFAULT_BOLD);
                    btnEditInc.setPadding(dp(10), dp(6), dp(10), dp(6));
                    btnEditInc.setBackgroundResource(R.drawable.bg_btn_primary);
                    btnEditInc.setOnClickListener(v -> showCreateOrEditIdolDialog(inc, this));
                    row.addView(btnEditInc);

                    itemsContainer.addView(row);
                }
            }
        };
        rebuildImportRows.run();
        root.addView(listScroll);

        builder.setView(root);
        builder.setPositiveButton(getString(R.string.btn_import_selected), (d, w) -> {
            int savedCount = 0;
            for (IdolCard c : payload.cards) {
                if (checkedCardIds.contains(c.getId())) {
                    repo.saveOrUpdateIdol(c);
                    savedCount++;
                }
            }
            if (payload.customMatch != null) repo.saveOrUpdateCustomMatch(payload.customMatch);
            refreshCurrentTab();
            Toast.makeText(this, getString(R.string.toast_saved_idols, savedCount), Toast.LENGTH_SHORT).show();
        });

        if (payload.customMatch != null || payload.cards.size() >= 2) {
            builder.setNeutralButton(getString(R.string.btn_play_imported_match), (d, w) -> {
                for (IdolCard c : payload.cards) {
                    if (checkedCardIds.contains(c.getId())) repo.saveOrUpdateIdol(c);
                }
                if (payload.customMatch != null) repo.saveOrUpdateCustomMatch(payload.customMatch);
                List<IdolCard> playList = new ArrayList<>();
                for (IdolCard c : payload.cards) {
                    if (checkedCardIds.contains(c.getId())) playList.add(c);
                }
                if (playList.size() < 2) playList = payload.cards;
                String mode = payload.customMatch != null ? payload.customMatch.getGameMode() : "KING_OF_THE_HILL";
                launchMatch(payload.packTitle, mode, playList);
            });
        }
        builder.setNegativeButton(getString(R.string.btn_cancel), null);
        builder.show();
    }

    private void triggerHapticFeedback() {
        try {
            Vibrator vib = (Vibrator) getSystemService(VIBRATOR_SERVICE);
            if (vib != null && vib.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vib.vibrate(VibrationEffect.createOneShot(55, VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    vib.vibrate(55);
                }
            }
        } catch (Exception ignored) {}
    }

    private void triggerGachaConfetti() {
        try {
            ViewGroup decor = (ViewGroup) getWindow().getDecorView();
            ConfettiView cv = new ConfettiView(this);
            decor.addView(cv, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            cv.burstConfetti();
            cv.postDelayed(() -> {
                try { decor.removeView(cv); } catch (Exception ignored) {}
            }, 3200L);
        } catch (Exception ignored) {}
    }

    private void showSecretDevGachaDialog() {
        ThemeManager.AppTheme theme = ThemeManager.getActiveTheme(this);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER_HORIZONTAL);
        int pad = dp(22);
        box.setPadding(pad, pad, pad, pad);
        box.setBackground(ThemeManager.createCardDrawable(this, theme.surfaceColor, theme.cardBorderColor, 24));

        TextView tvTitle = new TextView(this);
        tvTitle.setText(getString(R.string.dev_dialog_title));
        tvTitle.setTextColor(theme.textColor);
        tvTitle.setTextSize(18f);
        tvTitle.setTypeface(Typeface.create("sans-serif-black", Typeface.BOLD));
        tvTitle.setPadding(0, 0, 0, dp(6));
        box.addView(tvTitle);

        TextView tvMsg = new TextView(this);
        tvMsg.setText(getString(R.string.dev_dialog_msg));
        tvMsg.setTextColor(theme.getMutedTextColor());
        tvMsg.setTextSize(12.5f);
        tvMsg.setGravity(Gravity.CENTER);
        tvMsg.setPadding(0, 0, 0, dp(14));
        box.addView(tvMsg);

        EditText etPass = new EditText(this);
        etPass.setHint(getString(R.string.dev_dialog_hint));
        etPass.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        etPass.setTextSize(18f);
        etPass.setGravity(Gravity.CENTER);
        etPass.setTextColor(theme.textColor);
        etPass.setHintTextColor(Color.parseColor("#9CA3AF"));
        etPass.setBackground(ThemeManager.createCardDrawable(this, Color.parseColor("#10000000"), theme.cardBorderColor, 16));
        etPass.setPadding(dp(16), dp(12), dp(16), dp(12));
        LinearLayout.LayoutParams etLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        etLp.setMargins(0, 0, 0, dp(18));
        etPass.setLayoutParams(etLp);
        box.addView(etPass);

        AlertDialog[] ref = new AlertDialog[1];

        TextView btnUnlock = new TextView(this);
        btnUnlock.setText(getString(R.string.dev_btn_unlock));
        btnUnlock.setTextColor(theme.getButtonTextColor(theme.primaryColor));
        btnUnlock.setBackground(ThemeManager.createPillDrawable(this, theme.primaryColor, 20));
        btnUnlock.setTextSize(13.5f);
        btnUnlock.setTypeface(Typeface.DEFAULT_BOLD);
        btnUnlock.setGravity(Gravity.CENTER);
        btnUnlock.setPadding(dp(16), dp(12), dp(16), dp(12));
        btnUnlock.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        btnUnlock.setOnClickListener(v -> {
            String entered = etPass.getText() != null ? etPass.getText().toString().trim() : "";
            if ("2468".equals(entered)) {
                if (ref[0] != null) ref[0].dismiss();
                triggerHapticFeedback();
                int total = repo.unlockAllIdolsPhotocards();
                SharedPreferences wybPrefs = getSharedPreferences("wyb_prefs", MODE_PRIVATE);
                wybPrefs.edit().putLong("last_gacha_open_time", 0).apply();
                triggerGachaConfetti();
                Toast.makeText(this, getString(R.string.dev_toast_success, total), Toast.LENGTH_LONG).show();
                refreshCurrentTab();
            } else {
                Toast.makeText(this, getString(R.string.dev_toast_invalid), Toast.LENGTH_SHORT).show();
            }
        });
        box.addView(btnUnlock);

        builder.setView(box);
        ref[0] = builder.create();
        if (ref[0].getWindow() != null) {
            ref[0].getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        ref[0].show();
    }

    private void showPhotocardGachaDialog() {
        SharedPreferences wybPrefs = getSharedPreferences("wyb_prefs", MODE_PRIVATE);
        long lastTime = wybPrefs.getLong("last_gacha_open_time", 0);
        long now = System.currentTimeMillis();
        long cooldown = 100 * 1000L; // 100 seconds!
        long remaining = (lastTime + cooldown) - now;

        if (remaining > 0) {
            long minutes = remaining / (60 * 1000L);
            long seconds = (remaining % (60 * 1000L)) / 1000L;
            String timeStr = minutes > 0 ? (minutes + "m " + seconds + "s") : (seconds + "s");
            new AlertDialog.Builder(this)
                    .setTitle(getString(R.string.gacha_cooldown_title))
                    .setMessage(getString(R.string.gacha_cooldown_msg, timeStr))
                    .setPositiveButton(getString(R.string.btn_understood), null)
                    .show();
            return;
        }

        List<IdolCard> all = repo.getAllIdols();
        if (all.isEmpty()) return;

        // Immediate tactile & haptic response
        triggerHapticFeedback();
        Toast.makeText(this, getString(R.string.gacha_opening_pack), Toast.LENGTH_SHORT).show();

        Random rnd = new Random();
        int roll = rnd.nextInt(100); // 0..99

        List<IdolCard> candidatesWithLocked = new ArrayList<>();
        for (IdolCard c : all) {
            if (!c.getLockedPhotocards().isEmpty()) {
                candidatesWithLocked.add(c);
            }
        }

        String tierTitle;
        List<IdolCard> unlockedCardsList = new ArrayList<>();
        IdolCard showcaseCard = null;

        if (candidatesWithLocked.isEmpty()) {
            // All cards in collection already unlocked!
            tierTitle = getString(R.string.gacha_tier_max_collection);
            showcaseCard = all.get(rnd.nextInt(all.size()));
            unlockedCardsList.add(showcaseCard);
        } else if (roll < 3) {
            // === 3% JACKPOT: ALL REMAINING PHOTOCARDS OF ONE IDOL UNLOCKED! ===
            IdolCard luckyIdol = candidatesWithLocked.get(rnd.nextInt(candidatesWithLocked.size()));
            List<String> lockedBefore = new ArrayList<>(luckyIdol.getLockedPhotocards());
            luckyIdol.unlockAllPhotocards();
            if (!lockedBefore.isEmpty()) {
                luckyIdol.setPhotoUri(lockedBefore.get(0));
            }
            repo.saveOrUpdateIdol(luckyIdol);
            tierTitle = getString(R.string.gacha_tier_jackpot, luckyIdol.getStageName());
            showcaseCard = luckyIdol;
            for (String uri : lockedBefore) {
                unlockedCardsList.add(new IdolCard(luckyIdol.getId(), luckyIdol.getStageName(),
                        luckyIdol.getFullName(), luckyIdol.getGroupName(), luckyIdol.isSoloist(),
                        luckyIdol.getCountry(), luckyIdol.getBirthDate(), luckyIdol.getRole(),
                        uri, luckyIdol.getAccentColor(), luckyIdol.getBio(), luckyIdol.getCollections()));
            }
            if (unlockedCardsList.isEmpty()) unlockedCardsList.add(luckyIdol);
            triggerGachaConfetti();
        } else if (roll < 13) {
            // === 10% MEGA DROP: 10 RANDOM PHOTOCARDS UNLOCKED! ===
            tierTitle = getString(R.string.gacha_tier_mega);
            int targetCount = 10;
            while (unlockedCardsList.size() < targetCount) {
                List<IdolCard> currentAvailable = new ArrayList<>();
                for (IdolCard c : all) {
                    if (!c.getLockedPhotocards().isEmpty()) currentAvailable.add(c);
                }
                if (currentAvailable.isEmpty()) break;
                IdolCard pickedIdol = currentAvailable.get(rnd.nextInt(currentAvailable.size()));
                List<String> locked = pickedIdol.getLockedPhotocards();
                String uri = locked.get(rnd.nextInt(locked.size()));
                pickedIdol.unlockPhotocard(uri);
                pickedIdol.setPhotoUri(uri);
                repo.saveOrUpdateIdol(pickedIdol);
                IdolCard singleCard = new IdolCard(pickedIdol.getId(), pickedIdol.getStageName(),
                        pickedIdol.getFullName(), pickedIdol.getGroupName(), pickedIdol.isSoloist(),
                        pickedIdol.getCountry(), pickedIdol.getBirthDate(), pickedIdol.getRole(),
                        uri, pickedIdol.getAccentColor(), pickedIdol.getBio(), pickedIdol.getCollections());
                unlockedCardsList.add(singleCard);
                if (showcaseCard == null) showcaseCard = singleCard;
            }
            if (showcaseCard == null) showcaseCard = all.get(rnd.nextInt(all.size()));
            triggerGachaConfetti();
        } else if (roll < 46) {
            // === 33% LUCKY PACK: 5 RANDOM PHOTOCARDS UNLOCKED! ===
            tierTitle = getString(R.string.gacha_tier_lucky);
            int targetCount = 5;
            while (unlockedCardsList.size() < targetCount) {
                List<IdolCard> currentAvailable = new ArrayList<>();
                for (IdolCard c : all) {
                    if (!c.getLockedPhotocards().isEmpty()) currentAvailable.add(c);
                }
                if (currentAvailable.isEmpty()) break;
                IdolCard pickedIdol = currentAvailable.get(rnd.nextInt(currentAvailable.size()));
                List<String> locked = pickedIdol.getLockedPhotocards();
                String uri = locked.get(rnd.nextInt(locked.size()));
                pickedIdol.unlockPhotocard(uri);
                pickedIdol.setPhotoUri(uri);
                repo.saveOrUpdateIdol(pickedIdol);
                IdolCard singleCard = new IdolCard(pickedIdol.getId(), pickedIdol.getStageName(),
                        pickedIdol.getFullName(), pickedIdol.getGroupName(), pickedIdol.isSoloist(),
                        pickedIdol.getCountry(), pickedIdol.getBirthDate(), pickedIdol.getRole(),
                        uri, pickedIdol.getAccentColor(), pickedIdol.getBio(), pickedIdol.getCollections());
                unlockedCardsList.add(singleCard);
                if (showcaseCard == null) showcaseCard = singleCard;
            }
            if (showcaseCard == null) showcaseCard = all.get(rnd.nextInt(all.size()));
        } else {
            // === 54% STANDARD: 1 RANDOM PHOTOCARD ===
            tierTitle = getString(R.string.gacha_tier_single);
            IdolCard pickedIdol = candidatesWithLocked.get(rnd.nextInt(candidatesWithLocked.size()));
            List<String> locked = pickedIdol.getLockedPhotocards();
            String uri = locked.get(rnd.nextInt(locked.size()));
            pickedIdol.unlockPhotocard(uri);
            pickedIdol.setPhotoUri(uri);
            repo.saveOrUpdateIdol(pickedIdol);
            showcaseCard = new IdolCard(pickedIdol.getId(), pickedIdol.getStageName(),
                    pickedIdol.getFullName(), pickedIdol.getGroupName(), pickedIdol.isSoloist(),
                    pickedIdol.getCountry(), pickedIdol.getBirthDate(), pickedIdol.getRole(),
                    uri, pickedIdol.getAccentColor(), pickedIdol.getBio(), pickedIdol.getCollections());
            unlockedCardsList.add(showcaseCard);
        }

        wybPrefs.edit().putLong("last_gacha_open_time", now).apply();
        refreshCurrentTab();

        final IdolCard finalShowcase = showcaseCard;
        final String finalTier = tierTitle;
        final List<IdolCard> finalUnlockedList = unlockedCardsList;

        CardImageLoader.preloadSingleCardAsync(this, finalShowcase, () -> {
            if (isFinishing()) return;
            ThemeManager.AppTheme theme = ThemeManager.getActiveTheme(this);
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            LinearLayout box = new LinearLayout(this);
            box.setOrientation(LinearLayout.VERTICAL);
            box.setGravity(Gravity.CENTER_HORIZONTAL);
            int pad = dp(20);
            box.setPadding(pad, pad, pad, pad);
            box.setBackground(ThemeManager.createCardDrawable(this, theme.surfaceColor, theme.cardBorderColor, 26));

            TextView tvTier = new TextView(this);
            tvTier.setText(finalTier);
            tvTier.setTextColor(theme.getButtonTextColor(theme.primaryColor));
            tvTier.setTextSize(11f);
            tvTier.setTypeface(Typeface.DEFAULT_BOLD);
            tvTier.setGravity(Gravity.CENTER);
            tvTier.setPadding(dp(14), dp(6), dp(14), dp(6));
            tvTier.setBackground(ThemeManager.createPillDrawable(this, theme.primaryColor, 14));
            box.addView(tvTier);

            CardView pcCard = new CardView(this);
            pcCard.setRadius(dp(22));
            pcCard.setCardElevation(dp(6));
            LinearLayout.LayoutParams ivLp = new LinearLayout.LayoutParams(dp(210), dp(280));
            ivLp.setMargins(0, dp(14), 0, dp(14));
            pcCard.setLayoutParams(ivLp);

            ImageView iv = new ImageView(this);
            iv.setLayoutParams(new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
            CardImageLoader.loadCardImage(this, finalShowcase, iv);
            pcCard.addView(iv);
            box.addView(pcCard);

            TextView tvName = new TextView(this);
            tvName.setText(finalShowcase.getStageName() + " (" + finalShowcase.getGroupName() + ")");
            tvName.setTextColor(theme.textColor);
            tvName.setTextSize(20f);
            tvName.setTypeface(Typeface.create("sans-serif-black", Typeface.BOLD));
            box.addView(tvName);

            if (finalShowcase.getNativeName() != null && !finalShowcase.getNativeName().isEmpty()) {
                TextView tvNative = new TextView(this);
                tvNative.setText(finalShowcase.getNativeName());
                tvNative.setTextColor(theme.primaryColor);
                tvNative.setTextSize(14f);
                tvNative.setTypeface(Typeface.DEFAULT_BOLD);
                box.addView(tvNative);
            }

            TextView tvSub = new TextView(this);
            if (finalUnlockedList.size() > 1) {
                tvSub.setText(getString(R.string.gacha_unlocked_multi_sub, finalUnlockedList.size()));
            } else {
                tvSub.setText(getString(R.string.gacha_unlocked_sub, finalShowcase.getUnlockedPhotocards().size(), finalShowcase.getPhotocards().size()));
            }
            tvSub.setTextColor(theme.getMutedTextColor());
            tvSub.setTextSize(12f);
            tvSub.setGravity(Gravity.CENTER);
            tvSub.setPadding(0, dp(4), 0, dp(10));
            box.addView(tvSub);

            final IdolCard[] currentSelected = new IdolCard[]{finalShowcase};

            TextView btnAlbum = new TextView(this);
            btnAlbum.setText(finalUnlockedList.size() > 1
                    ? getString(R.string.gacha_btn_view_binder_named, finalShowcase.getStageName())
                    : getString(R.string.gacha_btn_view_binder));
            btnAlbum.setTextColor(theme.getButtonTextColor(theme.primaryColor));
            btnAlbum.setBackground(ThemeManager.createPillDrawable(this, theme.primaryColor, 20));
            btnAlbum.setTextSize(12.5f);
            btnAlbum.setTypeface(Typeface.DEFAULT_BOLD);
            btnAlbum.setGravity(Gravity.CENTER);
            btnAlbum.setPadding(dp(12), dp(10), dp(12), dp(10));
            LinearLayout.LayoutParams baLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.2f);
            baLp.setMargins(0, 0, dp(6), 0);
            btnAlbum.setLayoutParams(baLp);

            // If multiple photocards unlocked (5, 10, or all remaining of an idol), show horizontal thumbnail drawer!
            if (finalUnlockedList.size() > 1) {
                HorizontalScrollView pcThumbScroll = new HorizontalScrollView(this);
                pcThumbScroll.setHorizontalScrollBarEnabled(false);
                LinearLayout thumbsRow = new LinearLayout(this);
                thumbsRow.setOrientation(LinearLayout.HORIZONTAL);
                thumbsRow.setPadding(0, 0, 0, dp(10));

                for (IdolCard uc : finalUnlockedList) {
                    CardView thumbCard = new CardView(this);
                    thumbCard.setRadius(dp(12));
                    thumbCard.setCardElevation(dp(2));
                    LinearLayout.LayoutParams tLp = new LinearLayout.LayoutParams(dp(48), dp(64));
                    tLp.setMargins(dp(3), 0, dp(3), 0);
                    thumbCard.setLayoutParams(tLp);

                    ImageView thumbIv = new ImageView(this);
                    thumbIv.setLayoutParams(new FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                    thumbIv.setScaleType(ImageView.ScaleType.CENTER_CROP);
                    CardImageLoader.loadCardImage(this, uc, thumbIv);
                    thumbCard.addView(thumbIv);

                    thumbCard.setOnClickListener(v -> {
                        triggerHapticFeedback();
                        currentSelected[0] = uc;
                        CardImageLoader.loadCardImage(this, uc, iv);
                        tvName.setText(uc.getStageName() + " (" + uc.getGroupName() + ")");
                        btnAlbum.setText(getString(R.string.gacha_btn_view_binder_named, uc.getStageName()));
                    });
                    thumbsRow.addView(thumbCard);
                }
                pcThumbScroll.addView(thumbsRow);
                box.addView(pcThumbScroll);
            }

            // Share Photocard button from Gacha Result!
            TextView btnShareGacha = new TextView(this);
            btnShareGacha.setText(getString(R.string.btn_share_photocard));
            btnShareGacha.setTextColor(Color.WHITE);
            btnShareGacha.setBackground(ThemeManager.createPillDrawable(this, theme.secondaryColor, 20));
            btnShareGacha.setTextSize(13f);
            btnShareGacha.setTypeface(Typeface.DEFAULT_BOLD);
            btnShareGacha.setGravity(Gravity.CENTER);
            btnShareGacha.setPadding(dp(14), dp(11), dp(14), dp(11));
            LinearLayout.LayoutParams sgLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            sgLp.bottomMargin = dp(8);
            btnShareGacha.setLayoutParams(sgLp);
            btnShareGacha.setOnClickListener(v -> {
                IdolCard c = currentSelected[0];
                shareIdolPhotocardInvite(c, c.getPhotoUri(), iv);
            });
            box.addView(btnShareGacha);

            LinearLayout btnsRow = new LinearLayout(this);
            btnsRow.setOrientation(LinearLayout.HORIZONTAL);
            LinearLayout.LayoutParams brLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            btnsRow.setLayoutParams(brLp);

            AlertDialog[] gRef = new AlertDialog[1];

            btnAlbum.setOnClickListener(v -> {
                if (gRef[0] != null) gRef[0].dismiss();
                Map<String, IdolCard> distinctIdols = new LinkedHashMap<>();
                for (IdolCard uc : finalUnlockedList) {
                    IdolCard real = repo.getIdolById(uc.getId());
                    if (real != null && !distinctIdols.containsKey(real.getId())) {
                        distinctIdols.put(real.getId(), real);
                    }
                }
                if (distinctIdols.size() > 1) {
                    List<IdolCard> idolList = new ArrayList<>(distinctIdols.values());
                    CharSequence[] names = new CharSequence[idolList.size()];
                    for (int i = 0; i < idolList.size(); i++) {
                        IdolCard idl = idolList.get(i);
                        names[i] = "📖 " + idl.getStageName() + " (" + (idl.isSoloist() ? "Soloist" : idl.getGroupName()) + ")";
                    }
                    new AlertDialog.Builder(MainActivity.this)
                            .setTitle(getString(R.string.gacha_choose_binder_title))
                            .setItems(names, (dialogInterface, which) -> {
                                showIdolDetailAndPhotocardBinderDialog(idolList.get(which));
                            })
                            .setNegativeButton(getString(R.string.btn_cancel), null)
                            .show();
                } else {
                    IdolCard fullIdol = repo.getIdolById(currentSelected[0].getId());
                    showIdolDetailAndPhotocardBinderDialog(fullIdol != null ? fullIdol : currentSelected[0]);
                }
            });
            btnsRow.addView(btnAlbum);

            TextView btnClose = new TextView(this);
            btnClose.setText(getString(R.string.btn_close));
            btnClose.setTextColor(Color.parseColor("#374151"));
            btnClose.setBackground(ThemeManager.createPillDrawable(this, Color.parseColor("#E5E7EB"), 20));
            btnClose.setTextSize(12.5f);
            btnClose.setTypeface(Typeface.DEFAULT_BOLD);
            btnClose.setGravity(Gravity.CENTER);
            btnClose.setPadding(dp(12), dp(10), dp(12), dp(10));
            LinearLayout.LayoutParams bcLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            btnClose.setLayoutParams(bcLp);
            btnClose.setOnClickListener(v -> {
                if (gRef[0] != null) gRef[0].dismiss();
            });
            btnsRow.addView(btnClose);

            box.addView(btnsRow);

            builder.setView(box);
            gRef[0] = builder.create();
            if (gRef[0].getWindow() != null) {
                gRef[0].getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            }
            gRef[0].show();
        });
    }

    private void handleIncomingIntent(Intent intent) {
        if (intent == null) return;
        String action = intent.getAction();
        Uri dataUri = intent.getData();

        if (Intent.ACTION_VIEW.equals(action) && dataUri != null) {
            ShareImportManager.SharedPayload payload = ShareImportManager.decodeFromUri(this, dataUri);
            if (payload != null && (!payload.cards.isEmpty() || payload.customMatch != null)) {
                new Handler(Looper.getMainLooper()).post(() -> dispatchImportPayload(payload));
            }
        } else if (Intent.ACTION_SEND.equals(action)) {
            String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
            Uri streamUri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
            if (streamUri != null) {
                ShareImportManager.SharedPayload payload = ShareImportManager.decodeFromUri(this, streamUri);
                if (payload != null && (!payload.cards.isEmpty() || payload.customMatch != null)) {
                    new Handler(Looper.getMainLooper()).post(() -> dispatchImportPayload(payload));
                    return;
                }
            }
            if (sharedText != null) {
                ShareImportManager.SharedPayload payload = ShareImportManager.decodeFromTextOrCode(this, sharedText);
                if (payload != null && (!payload.cards.isEmpty() || payload.customMatch != null)) {
                    new Handler(Looper.getMainLooper()).post(() -> dispatchImportPayload(payload));
                }
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null || data.getData() == null) return;
        Uri uri = data.getData();

        if (requestCode == REQ_PICK_CARD_PHOTO) {
            String savedPath = CardImageLoader.copyUriToInternalStorage(this, uri);
            if (pendingPhotoUrlField != null) pendingPhotoUrlField.setText(savedPath);
            if (pendingPhotoPreviewView != null) CardImageLoader.loadUriIntoImageView(this, savedPath, pendingPhotoPreviewView, "Idol", Color.parseColor("#FF85A2"));
        } else if (requestCode == REQ_PICK_EXTRA_PHOTOCARD && pendingPhotocardTargetIdol != null) {
            String savedPath = CardImageLoader.copyUriToInternalStorage(this, uri);
            pendingPhotocardTargetIdol.addPhotocard(savedPath);
            pendingPhotocardTargetIdol.setPhotoUri(savedPath);
            repo.saveOrUpdateIdol(pendingPhotocardTargetIdol);
            if (pendingPhotocardRefreshCallback != null) pendingPhotocardRefreshCallback.run();
        } else if (requestCode == REQ_PICK_USER_AVATAR) {
            String savedPath = CardImageLoader.copyUriToInternalStorage(this, uri);
            if (savedPath != null && !savedPath.isEmpty()) {
                repo.setCustomProfileAvatarUri(savedPath);
                refreshCurrentTab();
                Toast.makeText(this, getString(R.string.toast_profile_updated), Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == REQ_PICK_WYB_FILE) {
            ShareImportManager.SharedPayload payload = ShareImportManager.decodeFromUri(this, uri);
            if (payload != null && (!payload.cards.isEmpty() || payload.customMatch != null)) {
                dispatchImportPayload(payload);
            }
        }
    }

    private String getAppVersionName() {
        try {
            android.content.pm.PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            return pInfo.versionName != null ? pInfo.versionName : "1.2.25";
        } catch (Exception e) {
            return "1.2.25";
        }
    }

    private void launchMatch(String title, String gameMode, List<IdolCard> cards) {
        if (cards == null || cards.size() < 2) {
            Toast.makeText(this, getString(R.string.toast_min_two_idols), Toast.LENGTH_SHORT).show();
            return;
        }
        ArrayList<String> ids = new ArrayList<>();
        for (IdolCard c : cards) ids.add(c.getId());

        Intent intent = new Intent(this, MatchActivity.class);
        intent.putExtra(MatchActivity.EXTRA_TITLE, title);
        intent.putExtra(MatchActivity.EXTRA_MODE, gameMode);
        intent.putStringArrayListExtra(MatchActivity.EXTRA_CARD_IDS, ids);

        CardImageLoader.preloadMatchCardsAsync(this, cards, new CardImageLoader.PreloadProgressListener() {
            @Override
            public void onProgress(int current, int total) {
            }

            @Override
            public void onComplete() {
                if (!isFinishing()) {
                    startActivity(intent);
                }
            }
        });
    }

    private void updateCountChipsHighlight(List<TextView> views, int[] values, int selected) {
        for (int i = 0; i < views.size(); i++) {
            boolean active = (values[i] == selected);
            views.get(i).setBackgroundResource(active ? R.drawable.bg_btn_primary : R.drawable.bg_chip_tag);
            views.get(i).setPadding(0, dp(10), 0, dp(10));
            views.get(i).setTextColor(Color.parseColor("#111827"));
        }
    }

    private void addSmallLabel(LinearLayout parent, String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(Color.parseColor("#6B7280"));
        tv.setTextSize(12f);
        tv.setTypeface(Typeface.DEFAULT_BOLD);
        tv.setPadding(0, dp(8), 0, dp(4));
        parent.addView(tv);
    }

    private Spinner createStyledSpinner(String[] items) {
        Spinner sp = new Spinner(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, items);
        sp.setAdapter(adapter);
        sp.setBackgroundResource(R.drawable.bg_chip_tag);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, dp(6));
        sp.setLayoutParams(lp);
        return sp;
    }

    private EditText addFormInput(LinearLayout parent, String hint, String value) {
        EditText et = new EditText(this);
        et.setHint(hint);
        et.setText(value != null ? value : "");
        et.setTextColor(Color.parseColor("#111827"));
        et.setHintTextColor(Color.parseColor("#9CA3AF"));
        et.setTextSize(13.5f);
        et.setBackgroundResource(R.drawable.bg_chip_tag);
        et.setPadding(dp(14), dp(11), dp(14), dp(11));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, dp(8));
        et.setLayoutParams(lp);
        parent.addView(et);
        return et;
    }

    public View wrapSearchBarWithClearButton(EditText et, ThemeManager.AppTheme theme) {
        FrameLayout wrap = new FrameLayout(this);
        wrap.setLayoutParams(et.getLayoutParams() != null ? et.getLayoutParams() :
                new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        et.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        // Reserve space on the right for ✕
        et.setPadding(et.getPaddingLeft(), et.getPaddingTop(), et.getPaddingRight() + dp(28), et.getPaddingBottom());
        wrap.addView(et);

        TextView btnClear = new TextView(this);
        btnClear.setText("✕");
        btnClear.setTextSize(14f);
        btnClear.setTypeface(Typeface.DEFAULT_BOLD);
        btnClear.setTextColor(theme != null ? theme.getMutedTextColor() : Color.parseColor("#9CA3AF"));
        btnClear.setGravity(Gravity.CENTER);
        int p = dp(8);
        btnClear.setPadding(p, p, p, p);
        FrameLayout.LayoutParams clp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.END | Gravity.CENTER_VERTICAL);
        clp.setMargins(0, 0, dp(10), 0);
        btnClear.setLayoutParams(clp);
        btnClear.setVisibility(et.getText().length() > 0 ? View.VISIBLE : View.GONE);

        et.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                btnClear.setVisibility(s != null && s.length() > 0 ? View.VISIBLE : View.GONE);
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        btnClear.setOnClickListener(v -> et.setText(""));
        wrap.addView(btnClear);
        return wrap;
    }

    @Override
    protected void onDestroy() {
        if (bgmController != null) bgmController.destroy();
        super.onDestroy();
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
