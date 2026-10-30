package com.nitsutech.whosyourbias.ui;

import android.app.AlertDialog;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
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
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.nitsutech.whosyourbias.R;
import com.nitsutech.whosyourbias.data.BiasRepository;
import com.nitsutech.whosyourbias.data.CustomMatch;
import com.nitsutech.whosyourbias.data.IdolCard;
import com.nitsutech.whosyourbias.util.CardImageLoader;
import com.nitsutech.whosyourbias.util.ShareImportManager;

import java.util.ArrayList;
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
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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

        View btnCenterPlus = findViewById(R.id.btnCenterPlus);
        if (btnCenterPlus != null) {
            btnCenterPlus.setOnClickListener(v -> showCenterPlusMenuDialog());
        }

        WebView bgmWeb = findViewById(R.id.bgmWebView);
        View bgmVideoContainer = findViewById(R.id.bgmVideoContainer);
        TextView tvBgmTitle = findViewById(R.id.tvBgmTitle);
        TextView btnPlayPause = findViewById(R.id.btnBgmPlayPause);
        TextView btnMute = findViewById(R.id.btnBgmMute);
        TextView btnVideoToggle = findViewById(R.id.btnBgmVideoToggle);
        TextView btnChangeSong = findViewById(R.id.btnBgmChangeSong);
        bgmController = new YoutubeBgmController(
                this, bgmWeb, bgmVideoContainer, tvBgmTitle,
                btnPlayPause, btnMute, btnVideoToggle, btnChangeSong);

        switchTab(0);
        handleIncomingIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIncomingIntent(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (bgmController != null) bgmController.updateButtonsUi();
        refreshCurrentTab();
    }

    private void switchTab(int index) {
        currentTab = index;
        int activeColor = Color.parseColor("#111827");
        int inactiveColor = Color.parseColor("#9CA3AF");

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
        ScrollView scroll = new ScrollView(this);
        scroll.setVerticalScrollBarEnabled(false);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(8), dp(18), dp(28));

        // 1. Clean Pill Search Bar (Moshicam / Vibrary style)
        TextView searchBarPill = new TextView(this);
        searchBarPill.setText("🔍  Buscar idols, grupos o colecciones...");
        searchBarPill.setTextColor(Color.parseColor("#6B7280"));
        searchBarPill.setTextSize(13.5f);
        searchBarPill.setPadding(dp(16), dp(12), dp(16), dp(12));
        searchBarPill.setBackgroundResource(R.drawable.bg_chip_tag);
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
        heroBanner.setPadding(dp(18), dp(18), dp(16), dp(18));
        heroBanner.setBackgroundResource(R.drawable.bg_glass_header);
        LinearLayout.LayoutParams hbLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        hbLp.setMargins(0, 0, 0, dp(16));
        heroBanner.setLayoutParams(hbLp);

        LinearLayout heroLeft = new LinearLayout(this);
        heroLeft.setOrientation(LinearLayout.VERTICAL);
        heroLeft.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView tvHeroBadge = new TextView(this);
        tvHeroBadge.setText("✨ WHO IS YOUR BIAS? IT'S ME!");
        tvHeroBadge.setTextColor(Color.parseColor("#EC4899"));
        tvHeroBadge.setTextSize(10.5f);
        tvHeroBadge.setTypeface(Typeface.DEFAULT_BOLD);
        heroLeft.addView(tvHeroBadge);

        TextView tvHeroTitle = new TextView(this);
        tvHeroTitle.setText("Elegí tu Bias\nSupremo 1 vs 1");
        tvHeroTitle.setTextColor(Color.parseColor("#111827"));
        tvHeroTitle.setTextSize(20f);
        tvHeroTitle.setTypeface(Typeface.create("sans-serif-black", Typeface.BOLD));
        tvHeroTitle.setPadding(0, dp(4), 0, dp(4));
        heroLeft.addView(tvHeroTitle);

        TextView tvHeroSub = new TextView(this);
        tvHeroSub.setText("263 idols y 53 grupos listos para jugar");
        tvHeroSub.setTextColor(Color.parseColor("#6B7280"));
        tvHeroSub.setTextSize(12f);
        tvHeroSub.setPadding(0, 0, 0, dp(12));
        heroLeft.addView(tvHeroSub);

        TextView btnHeroPlay = new TextView(this);
        btnHeroPlay.setText("▶  Jugar Torneo");
        btnHeroPlay.setTextColor(Color.parseColor("#111827"));
        btnHeroPlay.setTypeface(Typeface.DEFAULT_BOLD);
        btnHeroPlay.setTextSize(13f);
        btnHeroPlay.setPadding(dp(18), dp(10), dp(18), dp(10));
        btnHeroPlay.setBackgroundResource(R.drawable.bg_btn_primary);
        btnHeroPlay.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        btnHeroPlay.setOnClickListener(v -> showQuickMatchSetupSheet());
        heroLeft.addView(btnHeroPlay);

        heroBanner.addView(heroLeft);

        // Tilted Polaroid Preview on Hero Right
        List<IdolCard> allIdols = repo.getAllIdols();
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

        // 3. Three Clean Rounded Square Tiles (Moshicam style: [Torneo CPU] [Armar Partida] [WhatsApp])
        LinearLayout tilesRow = new LinearLayout(this);
        tilesRow.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams trLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        trLp.setMargins(0, 0, 0, dp(16));
        tilesRow.setLayoutParams(trLp);

        tilesRow.addView(buildMoshiActionTile("⚡", "Partida CPU", "#FFF0F5", "#F43F5E", null, this::showQuickMatchSetupSheet));
        View spacer1 = new View(this);
        spacer1.setLayoutParams(new LinearLayout.LayoutParams(dp(10), 1));
        tilesRow.addView(spacer1);
        tilesRow.addView(buildMoshiActionTile("🎯", "Crear Partida", "#FFFBEB", "#D97706", null, this::showVibraryStyleCustomMatchBuilder));
        View spacer2 = new View(this);
        spacer2.setLayoutParams(new LinearLayout.LayoutParams(dp(10), 1));
        tilesRow.addView(spacer2);
        tilesRow.addView(buildMoshiActionTile("📥", "WhatsApp", "#EFF6FF", "#2563EB", "NUEVO", this::showImportHubDialog));
        root.addView(tilesRow);

        // 4. Promo Strip Banner (Moshicam "Moshi Cards NUEVO — Hay cartas nuevas" style)
        LinearLayout gachaStrip = new LinearLayout(this);
        gachaStrip.setOrientation(LinearLayout.HORIZONTAL);
        gachaStrip.setGravity(Gravity.CENTER_VERTICAL);
        gachaStrip.setPadding(dp(16), dp(13), dp(16), dp(13));
        GradientDrawable stripGd = new GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{Color.parseColor("#EEF2FF"), Color.parseColor("#FDF2F8")});
        stripGd.setCornerRadius(dp(16));
        gachaStrip.setBackground(stripGd);
        LinearLayout.LayoutParams gsLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        gsLp.setMargins(0, 0, 0, dp(22));
        gachaStrip.setLayoutParams(gsLp);

        TextView tvGachaIcon = new TextView(this);
        tvGachaIcon.setText("🃏  Photocard Gacha");
        tvGachaIcon.setTextColor(Color.parseColor("#111827"));
        tvGachaIcon.setTypeface(Typeface.DEFAULT_BOLD);
        tvGachaIcon.setTextSize(13.5f);
        gachaStrip.addView(tvGachaIcon);

        TextView tvNewBadge = new TextView(this);
        tvNewBadge.setText("NUEVO");
        tvNewBadge.setTextColor(Color.parseColor("#111827"));
        tvNewBadge.setTextSize(9.5f);
        tvNewBadge.setTypeface(Typeface.DEFAULT_BOLD);
        tvNewBadge.setPadding(dp(7), dp(2), dp(7), dp(2));
        tvNewBadge.setBackgroundResource(R.drawable.bg_btn_primary);
        LinearLayout.LayoutParams nbLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        nbLp.setMargins(dp(8), 0, 0, 0);
        tvNewBadge.setLayoutParams(nbLp);
        gachaStrip.addView(tvNewBadge);

        TextView tvGachaRight = new TextView(this);
        tvGachaRight.setText("Abrir sobre ›");
        tvGachaRight.setTextColor(Color.parseColor("#EC4899"));
        tvGachaRight.setTypeface(Typeface.DEFAULT_BOLD);
        tvGachaRight.setTextSize(12.5f);
        tvGachaRight.setGravity(Gravity.END);
        tvGachaRight.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        gachaStrip.addView(tvGachaRight);

        gachaStrip.setOnClickListener(v -> showPhotocardGachaDialog());
        root.addView(gachaStrip);

        // 5. Section: Tus Partidas y Desafíos (Clean Horizontal Cards like Moshicam "Marcos nuevos")
        addSectionHeaderRow(root, "Partidas y Desafíos", "Crear +", this::showVibraryStyleCustomMatchBuilder);

        HorizontalScrollView matchesScroll = new HorizontalScrollView(this);
        matchesScroll.setHorizontalScrollBarEnabled(false);
        LinearLayout matchesRow = new LinearLayout(this);
        matchesRow.setOrientation(LinearLayout.HORIZONTAL);
        matchesRow.setPadding(0, 0, 0, dp(20));

        for (CustomMatch cm : repo.getCustomMatches()) {
            matchesRow.addView(buildMoshiDeckCard(cm));
        }
        matchesScroll.addView(matchesRow);
        root.addView(matchesScroll);

        // 6. Section: Batallas por Compañía & Posición (Clean Horizontal Cards)
        addSectionHeaderRow(root, "Batallas Especiales", "Ver grupos", () -> switchTab(2));

        HorizontalScrollView specialScroll = new HorizontalScrollView(this);
        specialScroll.setHorizontalScrollBarEnabled(false);
        LinearLayout specialRow = new LinearLayout(this);
        specialRow.setOrientation(LinearLayout.HORIZONTAL);
        specialRow.setPadding(0, 0, 0, dp(10));

        addSpecialCarouselCard(specialRow, "HYBE Labels", "ILLIT • NewJeans • BTS • ENHYPEN", "HYBE", "idol_illit_wonhee");
        addSpecialCarouselCard(specialRow, "YG Entertainment", "BLACKPINK • BABYMONSTER • BIGBANG", "YG Entertainment", "idol_bp_jennie");
        addSpecialCarouselCard(specialRow, "JYP Entertainment", "TWICE • Stray Kids • ITZY • NMIXX", "JYP Entertainment", "idol_skz_felix");
        addSpecialCarouselCard(specialRow, "SM Entertainment", "aespa • RIIZE • NCT • Red Velvet", "SM Entertainment", "idol_aespa_karina");
        addSpecialCarouselCard(specialRow, "THEBLACKLABEL", "MEOVV • Somi • izna • Rosé", "THEBLACKLABEL", "idol_meovv_ella");
        addSpecialCarouselCard(specialRow, "5th Gen Queens & Kings", "ILLIT • RESCENE • KATSEYE • KiiiKiii", "5th Gen", "idol_rescene_woni");

        specialScroll.addView(specialRow);
        root.addView(specialScroll);

        scroll.addView(root);
        mainContentContainer.addView(scroll);
    }

    private View buildMoshiActionTile(String emoji, String title, String bgHex, String strokeHex, String badgeText, Runnable onClick) {
        FrameLayout tile = new FrameLayout(this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(90), 1f);
        tile.setLayoutParams(lp);

        GradientDrawable gd = new GradientDrawable();
        gd.setColor(Color.parseColor(bgHex));
        gd.setCornerRadius(dp(18));
        gd.setStroke(dp(1), Color.parseColor("#22000000"));
        tile.setBackground(gd);

        LinearLayout inner = new LinearLayout(this);
        inner.setOrientation(LinearLayout.VERTICAL);
        inner.setGravity(Gravity.CENTER);
        inner.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        TextView tvEmoji = new TextView(this);
        tvEmoji.setText(emoji);
        tvEmoji.setTextSize(24f);
        tvEmoji.setGravity(Gravity.CENTER);
        inner.addView(tvEmoji);

        TextView tvLabel = new TextView(this);
        tvLabel.setText(title);
        tvLabel.setTextColor(Color.parseColor("#111827"));
        tvLabel.setTextSize(12.5f);
        tvLabel.setTypeface(Typeface.DEFAULT_BOLD);
        tvLabel.setPadding(0, dp(4), 0, 0);
        inner.addView(tvLabel);

        tile.addView(inner);

        if (badgeText != null) {
            TextView tvBadge = new TextView(this);
            tvBadge.setText(badgeText);
            tvBadge.setTextColor(Color.parseColor("#111827"));
            tvBadge.setTextSize(8.5f);
            tvBadge.setTypeface(Typeface.DEFAULT_BOLD);
            tvBadge.setPadding(dp(6), dp(2), dp(6), dp(2));
            tvBadge.setBackgroundResource(R.drawable.bg_btn_primary);
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
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, 0, 0, dp(10));

        TextView tvTitle = new TextView(this);
        tvTitle.setText(title);
        tvTitle.setTextColor(Color.parseColor("#111827"));
        tvTitle.setTextSize(17f);
        tvTitle.setTypeface(Typeface.DEFAULT_BOLD);
        tvTitle.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        row.addView(tvTitle);

        TextView tvAction = new TextView(this);
        tvAction.setText(actionText);
        tvAction.setTextColor(Color.parseColor("#EC4899"));
        tvAction.setTextSize(13f);
        tvAction.setTypeface(Typeface.DEFAULT_BOLD);
        tvAction.setOnClickListener(v -> onAction.run());
        row.addView(tvAction);

        parent.addView(row);
    }

    private View buildMoshiDeckCard(CustomMatch cm) {
        CardView card = new CardView(this);
        card.setRadius(dp(18));
        card.setCardElevation(dp(2));
        card.setCardBackgroundColor(Color.parseColor("#F8F9FB"));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(168), ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, dp(12), 0);
        card.setLayoutParams(lp);

        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);

        FrameLayout imgBox = new FrameLayout(this);
        imgBox.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(145)));

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
        tvTag.setText("✨ " + cm.getCardIds().size() + " Cartas");
        tvTag.setTextColor(Color.parseColor("#111827"));
        tvTag.setTextSize(10f);
        tvTag.setTypeface(Typeface.DEFAULT_BOLD);
        tvTag.setPadding(dp(8), dp(3), dp(8), dp(3));
        GradientDrawable tagGd = new GradientDrawable();
        tagGd.setColor(Color.parseColor("#EEFFC700"));
        tagGd.setCornerRadius(dp(12));
        tvTag.setBackground(tagGd);
        FrameLayout.LayoutParams tLp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP | Gravity.START);
        tLp.setMargins(dp(8), dp(8), 0, 0);
        tvTag.setLayoutParams(tLp);
        imgBox.addView(tvTag);

        col.addView(imgBox);

        LinearLayout footer = new LinearLayout(this);
        footer.setOrientation(LinearLayout.VERTICAL);
        footer.setPadding(dp(10), dp(10), dp(10), dp(10));

        TextView tvTitle = new TextView(this);
        tvTitle.setText(cm.getTitle());
        tvTitle.setTextColor(Color.parseColor("#111827"));
        tvTitle.setTextSize(12.5f);
        tvTitle.setTypeface(Typeface.DEFAULT_BOLD);
        tvTitle.setMaxLines(1);
        tvTitle.setEllipsize(TextUtils.TruncateAt.END);
        footer.addView(tvTitle);

        LinearLayout actRow = new LinearLayout(this);
        actRow.setOrientation(LinearLayout.HORIZONTAL);
        actRow.setGravity(Gravity.CENTER_VERTICAL);
        actRow.setPadding(0, dp(4), 0, 0);

        TextView tvSub = new TextView(this);
        tvSub.setText("▶ Jugar");
        tvSub.setTextColor(Color.parseColor("#EC4899"));
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
                    .setItems(new String[]{"▶ Jugar Partida", "📤 Compartir por WhatsApp", "🗑️ Eliminar Partida"}, (d, which) -> {
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
        CardView card = new CardView(this);
        card.setRadius(dp(18));
        card.setCardElevation(dp(2));
        card.setCardBackgroundColor(Color.parseColor("#F8F9FB"));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(158), ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, dp(12), 0);
        card.setLayoutParams(lp);

        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);

        ImageView iv = new ImageView(this);
        iv.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(135)));
        iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
        IdolCard cover = repo.getIdolById(coverIdolId);
        if (cover != null) CardImageLoader.loadCardImage(this, cover, iv);
        col.addView(iv);

        LinearLayout footer = new LinearLayout(this);
        footer.setOrientation(LinearLayout.VERTICAL);
        footer.setPadding(dp(10), dp(8), dp(10), dp(10));

        TextView tvT = new TextView(this);
        tvT.setText(title);
        tvT.setTextColor(Color.parseColor("#111827"));
        tvT.setTextSize(12.5f);
        tvT.setTypeface(Typeface.DEFAULT_BOLD);
        tvT.setSingleLine(true);
        footer.addView(tvT);

        TextView tvS = new TextView(this);
        tvS.setText(subtitle);
        tvS.setTextColor(Color.parseColor("#6B7280"));
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
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        root.setPadding(dp(16), dp(6), dp(16), 0);

        // 1. Clean Pill Search Bar (Vibrary screenshot #4)
        EditText etSearch = new EditText(this);
        etSearch.setHint("🔍  Ingrese la palabra clave (idol, grupo, país)...");
        etSearch.setText(idolSearchQuery);
        etSearch.setTextColor(Color.parseColor("#111827"));
        etSearch.setHintTextColor(Color.parseColor("#9CA3AF"));
        etSearch.setTextSize(13.5f);
        etSearch.setSingleLine(true);
        etSearch.setPadding(dp(16), dp(11), dp(16), dp(11));
        etSearch.setBackgroundResource(R.drawable.bg_chip_tag);
        root.addView(etSearch);

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
            tvTab.setText("ALL".equals(ch) ? "Todo" : ("Favorites".equals(ch) ? "Favoritos" : ch));
            tvTab.setTextColor(active ? Color.parseColor("#111827") : Color.parseColor("#9CA3AF"));
            tvTab.setTypeface(null, active ? Typeface.BOLD : Typeface.NORMAL);
            tvTab.setTextSize(13.5f);
            tabItem.addView(tvTab);

            View underline = new View(this);
            LinearLayout.LayoutParams ulLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, dp(3));
            ulLp.topMargin = dp(4);
            underline.setLayoutParams(ulLp);
            underline.setBackgroundColor(active ? Color.parseColor("#FFC700") : Color.TRANSPARENT);
            tabItem.addView(underline);

            tabItem.setOnClickListener(v -> {
                idolCategoryFilter = ch;
                refreshCurrentTab();
            });
            filterRow.addView(tabItem);
        }
        filterScroll.addView(filterRow);
        root.addView(filterScroll);

        // 3. Vibrary 2-Column Photo Grid (Screenshot #5 style!)
        ScrollView gridScroll = new ScrollView(this);
        gridScroll.setVerticalScrollBarEnabled(false);
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

    private void populateTwoColumnPhotoGrid(LinearLayout container) {
        container.removeAllViews();
        List<IdolCard> list = getFilteredIdolsList();

        for (int i = 0; i < list.size(); i += 2) {
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
        LinearLayout cell = new LinearLayout(this);
        cell.setOrientation(LinearLayout.VERTICAL);

        CardView photoCard = new CardView(this);
        photoCard.setRadius(dp(18));
        photoCard.setCardElevation(dp(2));
        photoCard.setCardBackgroundColor(Color.parseColor("#F3F4F6"));
        photoCard.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(215)));

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
            tvPcBadge.setPadding(dp(7), dp(3), dp(7), dp(3));
            GradientDrawable gd = new GradientDrawable();
            gd.setColor(Color.parseColor("#88000000"));
            gd.setCornerRadius(dp(12));
            tvPcBadge.setBackground(gd);
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
        tvName.setTextColor(Color.parseColor("#111827"));
        tvName.setTextSize(14f);
        tvName.setTypeface(Typeface.DEFAULT_BOLD);
        tvName.setSingleLine(true);
        nameCol.addView(tvName);

        TextView tvGrp = new TextView(this);
        tvGrp.setText(card.isSoloist() ? "Solista" : card.getGroupName());
        tvGrp.setTextColor(Color.parseColor("#6B7280"));
        tvGrp.setTextSize(11.5f);
        tvGrp.setSingleLine(true);
        nameCol.addView(tvGrp);

        footer.addView(nameCol);

        TextView tvFav = new TextView(this);
        tvFav.setText(card.isFavorite() ? "♥" : "♡");
        tvFav.setTextColor(card.isFavorite() ? Color.parseColor("#F43F5E") : Color.parseColor("#9CA3AF"));
        tvFav.setTextSize(17f);
        tvFav.setPadding(dp(6), dp(2), dp(4), dp(2));
        tvFav.setOnClickListener(v -> {
            card.setFavorite(!card.isFavorite());
            repo.saveOrUpdateIdol(card);
            tvFav.setText(card.isFavorite() ? "♥" : "♡");
            tvFav.setTextColor(card.isFavorite() ? Color.parseColor("#F43F5E") : Color.parseColor("#9CA3AF"));
        });
        footer.addView(tvFav);

        cell.addView(footer);
        cell.setOnClickListener(v -> showIdolDetailAndPhotocardBinderDialog(card));
        return cell;
    }

    // =========================================================================
    // TAB 3: ARTISTAS / GRUPOS (Exact Vibrary Squircle Member Grid per Group!)
    // =========================================================================
    private void renderGroupsTab() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        root.setPadding(dp(18), dp(6), dp(18), 0);

        EditText etGroupSearch = new EditText(this);
        etGroupSearch.setHint("🔍  Buscar grupo o artista (ej: ILLIT, &TEAM, aespa, YG)...");
        etGroupSearch.setText(groupSearchQuery);
        etGroupSearch.setTextColor(Color.parseColor("#111827"));
        etGroupSearch.setHintTextColor(Color.parseColor("#9CA3AF"));
        etGroupSearch.setTextSize(13.5f);
        etGroupSearch.setSingleLine(true);
        etGroupSearch.setPadding(dp(16), dp(11), dp(16), dp(11));
        etGroupSearch.setBackgroundResource(R.drawable.bg_chip_tag);
        LinearLayout.LayoutParams sLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        sLp.setMargins(0, 0, 0, dp(12));
        etGroupSearch.setLayoutParams(sLp);
        root.addView(etGroupSearch);

        ScrollView scroll = new ScrollView(this);
        scroll.setVerticalScrollBarEnabled(false);
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
        container.removeAllViews();
        String q = groupSearchQuery != null ? groupSearchQuery.toLowerCase(Locale.ROOT).trim() : "";

        List<String> groups = repo.getAllGroups();
        groups.add("Soloists");

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

            container.addView(buildVibraryGroupSection(groupName, members));
        }
    }

    /**
     * Exact Vibrary Screenshot #1 & #4 layout:
     * Bold Group Title on left, pill button on right, and 4-column Squircle Avatar Grid below!
     */
    private View buildVibraryGroupSection(String groupName, List<IdolCard> members) {
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
        tvGroupTitle.setTextColor(Color.parseColor("#111827"));
        tvGroupTitle.setTextSize(18f);
        tvGroupTitle.setTypeface(Typeface.create("sans-serif-black", Typeface.BOLD));
        tvGroupTitle.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        headerRow.addView(tvGroupTitle);

        TextView btnPlayPill = new TextView(this);
        btnPlayPill.setText("▶ Jugar (" + members.size() + ")");
        btnPlayPill.setTextColor(Color.parseColor("#111827"));
        btnPlayPill.setTextSize(11.5f);
        btnPlayPill.setTypeface(Typeface.DEFAULT_BOLD);
        btnPlayPill.setPadding(dp(12), dp(6), dp(12), dp(6));
        btnPlayPill.setBackgroundResource(R.drawable.bg_btn_secondary);
        LinearLayout.LayoutParams ppLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        ppLp.setMargins(0, 0, dp(6), 0);
        btnPlayPill.setLayoutParams(ppLp);
        btnPlayPill.setOnClickListener(v -> {
            String mode = members.size() <= 6 ? "KING_OF_THE_HILL" : "BRACKET";
            launchMatch("Who is your bias in " + groupName + "?", mode, members);
        });
        headerRow.addView(btnPlayPill);

        TextView btnSharePill = new TextView(this);
        btnSharePill.setText("📤");
        btnSharePill.setTextSize(12.5f);
        btnSharePill.setPadding(dp(10), dp(6), dp(10), dp(6));
        btnSharePill.setBackgroundResource(R.drawable.bg_btn_secondary);
        btnSharePill.setOnClickListener(v -> {
            List<String> ids = new ArrayList<>();
            for (IdolCard m : members) ids.add(m.getId());
            CustomMatch gm = new CustomMatch(null, "Who is your bias in " + groupName + "?", "Friend",
                    members.size() <= 6 ? "KING_OF_THE_HILL" : "BRACKET", ids, System.currentTimeMillis());
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
        div.setBackgroundColor(Color.parseColor("#F1F5F9"));
        section.addView(div);

        return section;
    }

    private View buildVibrarySquircleMemberCell(IdolCard member, Set<String> selectionSet) {
        LinearLayout cell = new LinearLayout(this);
        cell.setOrientation(LinearLayout.VERTICAL);
        cell.setGravity(Gravity.CENTER_HORIZONTAL);
        cell.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        cell.setPadding(dp(4), 0, dp(4), 0);

        FrameLayout avatarWrap = new FrameLayout(this);
        avatarWrap.setLayoutParams(new LinearLayout.LayoutParams(dp(74), dp(74)));

        CardView squircle = new CardView(this);
        squircle.setRadius(dp(24)); // Vibrary rounded squircle shape!
        squircle.setCardElevation(dp(1));
        squircle.setCardBackgroundColor(Color.parseColor("#F3F4F6"));
        squircle.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        ImageView iv = new ImageView(this);
        iv.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
        CardImageLoader.loadCardImage(this, member, iv);
        squircle.addView(iv);
        avatarWrap.addView(squircle);

        // Vibrary yellow check circle badge on bottom-right when in selection mode
        TextView checkBadge = new TextView(this);
        checkBadge.setGravity(Gravity.CENTER);
        checkBadge.setTextSize(10f);
        checkBadge.setTypeface(Typeface.DEFAULT_BOLD);
        FrameLayout.LayoutParams cbLp = new FrameLayout.LayoutParams(dp(22), dp(22), Gravity.BOTTOM | Gravity.END);
        checkBadge.setLayoutParams(cbLp);

        if (selectionSet != null) {
            updateSquircleCheckBadge(checkBadge, selectionSet.contains(member.getId()));
            avatarWrap.addView(checkBadge);
        }

        cell.addView(avatarWrap);

        TextView tvName = new TextView(this);
        tvName.setText(member.getStageName());
        tvName.setTextColor(Color.parseColor("#111827"));
        tvName.setTextSize(12f);
        tvName.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        tvName.setGravity(Gravity.CENTER);
        tvName.setSingleLine(true);
        tvName.setEllipsize(TextUtils.TruncateAt.END);
        tvName.setPadding(0, dp(6), 0, 0);
        cell.addView(tvName);

        cell.setOnClickListener(v -> {
            if (selectionSet != null) {
                if (selectionSet.contains(member.getId())) selectionSet.remove(member.getId());
                else selectionSet.add(member.getId());
                updateSquircleCheckBadge(checkBadge, selectionSet.contains(member.getId()));
            } else {
                showIdolDetailAndPhotocardBinderDialog(member);
            }
        });

        return cell;
    }

    private void updateSquircleCheckBadge(TextView badge, boolean checked) {
        GradientDrawable gd = new GradientDrawable();
        gd.setShape(GradientDrawable.OVAL);
        gd.setColor(checked ? Color.parseColor("#FFC700") : Color.parseColor("#E5E7EB"));
        gd.setStroke(dp(2), Color.WHITE);
        badge.setBackground(gd);
        badge.setText("✓");
        badge.setTextColor(Color.WHITE);
    }

    // =========================================================================
    // TAB 4: MI PÁGINA (Exact Vibrary Screenshot #1 "Mi página" Layout!)
    // =========================================================================
    private void renderHallOfFameTab() {
        ScrollView scroll = new ScrollView(this);
        scroll.setVerticalScrollBarEnabled(false);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(10), dp(18), dp(28));

        // Centered Profile Header (Vibrary "Mi página" style)
        LinearLayout profileHeader = new LinearLayout(this);
        profileHeader.setOrientation(LinearLayout.VERTICAL);
        profileHeader.setGravity(Gravity.CENTER_HORIZONTAL);
        profileHeader.setPadding(0, dp(8), 0, dp(18));

        // Top Champion or Favorite Avatar in Rounded Squircle
        List<IdolCard> all = repo.getAllIdols();
        all.sort((a, b) -> {
            if (b.getTournamentsWon() != a.getTournamentsWon()) return Integer.compare(b.getTournamentsWon(), a.getTournamentsWon());
            if (b.isFavorite() != a.isFavorite()) return Boolean.compare(b.isFavorite(), a.isFavorite());
            return Integer.compare(b.getMatchesWon(), a.getMatchesWon());
        });
        IdolCard topBias = all.isEmpty() ? null : all.get(0);

        CardView profileSquircle = new CardView(this);
        profileSquircle.setRadius(dp(28));
        profileSquircle.setCardElevation(dp(2));
        profileSquircle.setCardBackgroundColor(Color.parseColor("#E0E7FF"));
        LinearLayout.LayoutParams psLp = new LinearLayout.LayoutParams(dp(86), dp(86));
        psLp.bottomMargin = dp(10);
        profileSquircle.setLayoutParams(psLp);

        if (topBias != null) {
            ImageView ivTop = new ImageView(this);
            ivTop.setLayoutParams(new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            ivTop.setScaleType(ImageView.ScaleType.CENTER_CROP);
            CardImageLoader.loadCardImage(this, topBias, ivTop);
            profileSquircle.addView(ivTop);
        }
        profileHeader.addView(profileSquircle);

        TextView tvMyBiasLabel = new TextView(this);
        tvMyBiasLabel.setText(topBias != null ? "👑 Mi Bias #1: " + topBias.getStageName() + " (" + topBias.getGroupName() + ")" : "Mi página");
        tvMyBiasLabel.setTextColor(Color.parseColor("#111827"));
        tvMyBiasLabel.setTextSize(16f);
        tvMyBiasLabel.setTypeface(Typeface.DEFAULT_BOLD);
        profileHeader.addView(tvMyBiasLabel);

        // Quick Share Idols Pill Button
        TextView btnShareMyIdols = new TextView(this);
        btnShareMyIdols.setText("📤 Compartir mis Idols por WhatsApp");
        btnShareMyIdols.setTextColor(Color.parseColor("#111827"));
        btnShareMyIdols.setTextSize(12f);
        btnShareMyIdols.setTypeface(Typeface.DEFAULT_BOLD);
        btnShareMyIdols.setPadding(dp(16), dp(8), dp(16), dp(8));
        btnShareMyIdols.setBackgroundResource(R.drawable.bg_btn_primary);
        LinearLayout.LayoutParams smLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        smLp.topMargin = dp(10);
        btnShareMyIdols.setLayoutParams(smLp);
        btnShareMyIdols.setOnClickListener(v -> showVibraryStyleShareIdolsDialog());
        profileHeader.addView(btnShareMyIdols);

        root.addView(profileHeader);

        // Vibrary Underlined Sub-Header ("Mi artista" / Hall of Fame grouped by Group)
        LinearLayout tabHeader = new LinearLayout(this);
        tabHeader.setOrientation(LinearLayout.VERTICAL);
        tabHeader.setPadding(0, 0, 0, dp(12));

        TextView tvSubTab = new TextView(this);
        tvSubTab.setText("Mi artista (Favoritos y Ganadores)");
        tvSubTab.setTextColor(Color.parseColor("#111827"));
        tvSubTab.setTextSize(15f);
        tvSubTab.setTypeface(Typeface.DEFAULT_BOLD);
        tabHeader.addView(tvSubTab);

        View goldBar = new View(this);
        LinearLayout.LayoutParams gbLp = new LinearLayout.LayoutParams(dp(80), dp(3));
        gbLp.topMargin = dp(4);
        goldBar.setLayoutParams(gbLp);
        goldBar.setBackgroundColor(Color.parseColor("#FFC700"));
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
        // If user hasn't favorited or played yet, show ILLIT, (G)I-DLE, aespa, &TEAM as starter showcase
        if (groupedMyArtists.isEmpty()) {
            for (String starterGrp : new String[]{"ILLIT", "&TEAM", "(G)I-DLE", "aespa", "BLACKPINK"}) {
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

    // =========================================================================
    // DIALOGS: QUICK MATCH, VIBRARY MULTI-SELECT BUILDER, IDOL BINDER, SHARE/IMPORT
    // =========================================================================

    private void showCenterPlusMenuDialog() {
        new AlertDialog.Builder(this)
                .setTitle("✨ ¿Qué querés crear o importar?")
                .setItems(new String[]{
                        "🃏 Crear Nueva Carta de Idol / Cantante",
                        "🎯 Armar Partida Personalizada (Elegir Cartas)",
                        "📤 Compartir Idols por WhatsApp",
                        "📥 Importar Archivo (.wyb) o Código de WhatsApp"
                }, (d, which) -> {
                    if (which == 0) showCreateOrEditIdolDialog(null, this::refreshCurrentTab);
                    else if (which == 1) showVibraryStyleCustomMatchBuilder();
                    else if (which == 2) showVibraryStyleShareIdolsDialog();
                    else if (which == 3) showImportHubDialog();
                })
                .show();
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
        tvTitle.setText("⚡ Configurar Partida Aleatoria CPU");
        tvTitle.setTextColor(Color.parseColor("#111827"));
        tvTitle.setTextSize(18f);
        tvTitle.setTypeface(Typeface.DEFAULT_BOLD);
        tvTitle.setPadding(0, 0, 0, dp(12));
        root.addView(tvTitle);

        addSmallLabel(root, getString(R.string.label_game_mode));
        final String[] modeValues = {"BRACKET", "KING_OF_THE_HILL", "ROUND_ROBIN", "DOUBLE_ELIMINATION"};
        final String[] modeLabels = {
                getString(R.string.mode_bracket),
                getString(R.string.mode_koth),
                getString(R.string.mode_round_robin),
                getString(R.string.mode_double_elim)
        };
        Spinner spMode = createStyledSpinner(modeLabels);
        root.addView(spMode);

        addSmallLabel(root, getString(R.string.label_filter_deck));
        List<String> filterOptions = new ArrayList<>();
        filterOptions.add("All Idols (" + repo.getAllIdols().size() + ")");
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
        btnStart.setText("🚀  Iniciar Partida");
        btnStart.setTextColor(Color.parseColor("#111827"));
        btnStart.setTypeface(Typeface.DEFAULT_BOLD);
        btnStart.setTextSize(14f);
        btnStart.setGravity(Gravity.CENTER);
        btnStart.setPadding(dp(16), dp(14), dp(16), dp(14));
        btnStart.setBackgroundResource(R.drawable.bg_btn_primary);
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
        root.setBackgroundColor(Color.WHITE);
        int pad = dp(16);
        root.setPadding(pad, pad, pad, 0);

        AlertDialog[] ref = new AlertDialog[1];
        final Set<String> chosenIds = new LinkedHashSet<>();

        EditText etTitle = addFormInput(root, "Nombre de la partida (ej: Desafío de Agustín)", "");

        final String[] modeValues = {"BRACKET", "KING_OF_THE_HILL", "ROUND_ROBIN", "DOUBLE_ELIMINATION"};
        final String[] modeLabels = {
                getString(R.string.mode_bracket),
                getString(R.string.mode_koth),
                getString(R.string.mode_round_robin),
                getString(R.string.mode_double_elim)
        };
        Spinner spMode = createStyledSpinner(modeLabels);
        root.addView(spMode);

        EditText etFilter = addFormInput(root, "🔍 Filtrar por grupo o nombre...", "");

        ScrollView gridScroll = new ScrollView(this);
        gridScroll.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(310)));
        LinearLayout groupsBox = new LinearLayout(this);
        groupsBox.setOrientation(LinearLayout.VERTICAL);
        gridScroll.addView(groupsBox);
        root.addView(gridScroll);

        Runnable renderGrid = new Runnable() {
            @Override
            public void run() {
                groupsBox.removeAllViews();
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
                    TextView btnTodo = new TextView(MainActivity.this);
                    btnTodo.setText("✓ Todo");
                    btnTodo.setTextColor(Color.parseColor("#6B7280"));
                    btnTodo.setTextSize(11.5f);
                    btnTodo.setPadding(dp(10), dp(4), dp(10), dp(4));
                    btnTodo.setBackgroundResource(R.drawable.bg_btn_secondary);
                    btnTodo.setOnClickListener(v -> {
                        for (IdolCard m : finalMembers) chosenIds.add(m.getId());
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

        // Bottom Vibrary Dual Split Bar: [ ↻ Inicialización ] | [ Guardar y Compartir / Jugar ]
        LinearLayout bottomBar = new LinearLayout(this);
        bottomBar.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams bbLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(50));
        bbLp.topMargin = dp(10);
        bottomBar.setLayoutParams(bbLp);

        TextView btnReset = new TextView(this);
        btnReset.setText("↻ Inicialización");
        btnReset.setTextColor(Color.WHITE);
        btnReset.setBackgroundColor(Color.parseColor("#6B7280"));
        btnReset.setGravity(Gravity.CENTER);
        btnReset.setTypeface(Typeface.DEFAULT_BOLD);
        btnReset.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f));
        btnReset.setOnClickListener(v -> {
            chosenIds.clear();
            renderGrid.run();
        });
        bottomBar.addView(btnReset);

        TextView btnSavePlay = new TextView(this);
        btnSavePlay.setText("Guardar y Jugar / Enviar");
        btnSavePlay.setTextColor(Color.WHITE);
        btnSavePlay.setBackgroundColor(Color.parseColor("#FFC700"));
        btnSavePlay.setGravity(Gravity.CENTER);
        btnSavePlay.setTypeface(Typeface.DEFAULT_BOLD);
        btnSavePlay.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1.5f));
        btnSavePlay.setOnClickListener(v -> {
            if (chosenIds.size() < 2) {
                Toast.makeText(this, "Elegí al menos 2 idols tocando sus fotos", Toast.LENGTH_SHORT).show();
                return;
            }
            if (ref[0] != null) ref[0].dismiss();
            String title = etTitle.getText().toString().trim();
            if (title.isEmpty()) title = "Partida Personalizada (" + chosenIds.size() + ")";
            String mode = modeValues[spMode.getSelectedItemPosition()];

            CustomMatch cm = new CustomMatch(null, title, "Agustín", mode, new ArrayList<>(chosenIds), System.currentTimeMillis());
            repo.saveOrUpdateCustomMatch(cm);
            refreshCurrentTab();

            List<IdolCard> cards = new ArrayList<>();
            for (String cid : chosenIds) {
                IdolCard c = repo.getIdolById(cid);
                if (c != null) cards.add(c);
            }
            new AlertDialog.Builder(this)
                    .setTitle("✅ Partida Creada: " + title)
                    .setItems(new String[]{"▶ Jugar Ahora", "📤 Compartir por WhatsApp"}, (d2, w2) -> {
                        if (w2 == 0) launchMatch(cm.getTitle(), cm.getGameMode(), cards);
                        else showShareChoiceDialog(cm.getTitle(), cm, cards);
                    }).show();
        });
        bottomBar.addView(btnSavePlay);
        root.addView(bottomBar);

        builder.setView(root);
        ref[0] = builder.create();
        ref[0].show();
    }

    private void showVibraryStyleShareIdolsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.WHITE);
        int pad = dp(16);
        root.setPadding(pad, pad, pad, 0);

        AlertDialog[] ref = new AlertDialog[1];
        final Set<String> chosenIds = new LinkedHashSet<>();

        TextView tvHeader = new TextView(this);
        tvHeader.setText("📤 Elegí los Idols para enviar por WhatsApp");
        tvHeader.setTextColor(Color.parseColor("#111827"));
        tvHeader.setTextSize(16f);
        tvHeader.setTypeface(Typeface.DEFAULT_BOLD);
        tvHeader.setPadding(0, 0, 0, dp(8));
        root.addView(tvHeader);

        EditText etFilter = addFormInput(root, "🔍 Buscar grupo o idol...", "");

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
                    TextView btnTodo = new TextView(MainActivity.this);
                    btnTodo.setText("✓ Todo");
                    btnTodo.setTextColor(Color.parseColor("#6B7280"));
                    btnTodo.setTextSize(11.5f);
                    btnTodo.setPadding(dp(10), dp(4), dp(10), dp(4));
                    btnTodo.setBackgroundResource(R.drawable.bg_btn_secondary);
                    btnTodo.setOnClickListener(v -> {
                        for (IdolCard m : finalMembers) chosenIds.add(m.getId());
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

        TextView btnSend = new TextView(this);
        btnSend.setText("📤 Compartir por WhatsApp");
        btnSend.setTextColor(Color.parseColor("#111827"));
        btnSend.setBackgroundColor(Color.parseColor("#FFC700"));
        btnSend.setGravity(Gravity.CENTER);
        btnSend.setTypeface(Typeface.DEFAULT_BOLD);
        btnSend.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        btnSend.setOnClickListener(v -> {
            if (chosenIds.isEmpty()) {
                Toast.makeText(this, "Tocá al menos un idol para compartir", Toast.LENGTH_SHORT).show();
                return;
            }
            if (ref[0] != null) ref[0].dismiss();
            List<IdolCard> toShare = new ArrayList<>();
            for (String id : chosenIds) {
                IdolCard c = repo.getIdolById(id);
                if (c != null) toShare.add(c);
            }
            showShareChoiceDialog("Pack de Idols (" + toShare.size() + ")", null, toShare);
        });
        bottomBar.addView(btnSend);
        root.addView(bottomBar);

        builder.setView(root);
        ref[0] = builder.create();
        ref[0].show();
    }

    private void showIdolDetailAndPhotocardBinderDialog(IdolCard card) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(18);
        root.setPadding(pad, pad, pad, pad);
        root.setBackgroundColor(Color.WHITE);

        AlertDialog[] dialogRef = new AlertDialog[1];

        CardView mainPhotoCard = new CardView(this);
        mainPhotoCard.setRadius(dp(22));
        mainPhotoCard.setCardElevation(dp(2));
        LinearLayout.LayoutParams mpLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(290));
        mpLp.setMargins(0, 0, 0, dp(14));
        mainPhotoCard.setLayoutParams(mpLp);

        ImageView ivMain = new ImageView(this);
        ivMain.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        ivMain.setScaleType(ImageView.ScaleType.CENTER_CROP);
        CardImageLoader.loadCardImage(this, card, ivMain);
        mainPhotoCard.addView(ivMain);
        root.addView(mainPhotoCard);

        TextView tvName = new TextView(this);
        tvName.setText(card.getStageName() + "  •  " + (card.isSoloist() ? "Solista" : card.getGroupName()));
        tvName.setTextColor(Color.parseColor("#111827"));
        tvName.setTextSize(22f);
        tvName.setTypeface(Typeface.create("sans-serif-black", Typeface.BOLD));
        root.addView(tvName);

        TextView tvDetails = new TextView(this);
        StringBuilder dsb = new StringBuilder();
        dsb.append("Nombre completo: ").append(card.getFullName()).append("\n");
        dsb.append("Origen: ").append(card.getCountryWithFlag()).append("\n");
        dsb.append("Edad y Signo: ").append(card.getCalculatedAge()).append(" años (").append(card.getBirthDate()).append(" • ").append(card.getZodiacSign()).append(")\n");
        dsb.append("Rol: ").append(card.getRole()).append("\n");
        List<IdolCard> mates = repo.getFellowGroupMembers(card);
        if (!mates.isEmpty()) {
            dsb.append("Compañeros en ").append(card.getGroupName()).append(": ");
            for (int i = 0; i < mates.size(); i++) {
                if (i > 0) dsb.append(", ");
                dsb.append(mates.get(i).getStageName());
            }
            dsb.append("\n");
        }
        dsb.append("\n").append(card.getBio());
        tvDetails.setText(dsb.toString());
        tvDetails.setTextColor(Color.parseColor("#4B5563"));
        tvDetails.setTextSize(13f);
        tvDetails.setPadding(0, dp(6), 0, dp(14));
        root.addView(tvDetails);

        // Photocard Binder Section
        TextView tvPcTitle = new TextView(this);
        tvPcTitle.setText("📸 Photocards de " + card.getStageName() + " (" + card.getPhotocards().size() + ")");
        tvPcTitle.setTextColor(Color.parseColor("#111827"));
        tvPcTitle.setTextSize(15f);
        tvPcTitle.setTypeface(Typeface.DEFAULT_BOLD);
        tvPcTitle.setPadding(0, 0, 0, dp(8));
        root.addView(tvPcTitle);

        HorizontalScrollView pcScroll = new HorizontalScrollView(this);
        LinearLayout pcRow = new LinearLayout(this);
        pcRow.setOrientation(LinearLayout.HORIZONTAL);
        pcRow.setPadding(0, 0, 0, dp(12));

        Runnable rebuildPhotocardsRow = new Runnable() {
            @Override
            public void run() {
                pcRow.removeAllViews();
                tvPcTitle.setText("📸 Photocards de " + card.getStageName() + " (" + card.getPhotocards().size() + ")");
                for (int i = 0; i < card.getPhotocards().size(); i++) {
                    final String pcUri = card.getPhotocards().get(i);
                    final int idx = i;
                    LinearLayout pcItem = new LinearLayout(MainActivity.this);
                    pcItem.setOrientation(LinearLayout.VERTICAL);
                    pcItem.setGravity(Gravity.CENTER_HORIZONTAL);
                    LinearLayout.LayoutParams pLp = new LinearLayout.LayoutParams(dp(100), ViewGroup.LayoutParams.WRAP_CONTENT);
                    pLp.setMargins(0, 0, dp(8), 0);
                    pcItem.setLayoutParams(pLp);

                    CardView thumbCard = new CardView(MainActivity.this);
                    thumbCard.setRadius(dp(14));
                    thumbCard.setCardElevation(dp(1));
                    thumbCard.setLayoutParams(new LinearLayout.LayoutParams(dp(96), dp(124)));

                    ImageView ivThumb = new ImageView(MainActivity.this);
                    ivThumb.setLayoutParams(new FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                    ivThumb.setScaleType(ImageView.ScaleType.CENTER_CROP);
                    IdolCard temp = new IdolCard(card.getId() + "_thumb_" + idx, card.getStageName(),
                            card.getFullName(), card.getGroupName(), card.isSoloist(), card.getCountry(),
                            card.getBirthDate(), card.getRole(), pcUri, card.getAccentColor(), card.getBio(), card.getCollections());
                    CardImageLoader.loadCardImage(MainActivity.this, temp, ivThumb);
                    ivThumb.setOnClickListener(v -> {
                        card.setPhotoUri(pcUri);
                        repo.saveOrUpdateIdol(card);
                        CardImageLoader.loadCardImage(MainActivity.this, card, ivMain);
                        Toast.makeText(MainActivity.this, "✨ Photocard principal actualizada", Toast.LENGTH_SHORT).show();
                    });
                    thumbCard.addView(ivThumb);
                    pcItem.addView(thumbCard);

                    if (card.getPhotocards().size() > 1) {
                        TextView btnRemovePc = new TextView(MainActivity.this);
                        btnRemovePc.setText("Quitar");
                        btnRemovePc.setTextColor(Color.parseColor("#EF4444"));
                        btnRemovePc.setTextSize(10.5f);
                        btnRemovePc.setPadding(0, dp(4), 0, 0);
                        btnRemovePc.setOnClickListener(v -> {
                            card.getPhotocards().remove(idx);
                            if (pcUri.equals(card.getPhotoUri())) {
                                card.setPhotoUri(card.getPhotocards().isEmpty() ? "" : card.getPhotocards().get(0));
                            }
                            repo.saveOrUpdateIdol(card);
                            CardImageLoader.loadCardImage(MainActivity.this, card, ivMain);
                            run();
                        });
                        pcItem.addView(btnRemovePc);
                    }
                    pcRow.addView(pcItem);
                }
            }
        };
        rebuildPhotocardsRow.run();
        pcScroll.addView(pcRow);
        root.addView(pcScroll);

        // Add Photocard Buttons
        LinearLayout addPcRow = new LinearLayout(this);
        addPcRow.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams apcLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        apcLp.setMargins(0, 0, 0, dp(12));
        addPcRow.setLayoutParams(apcLp);

        TextView btnAddPcFile = new TextView(this);
        btnAddPcFile.setText("📁 Subir Photocard");
        btnAddPcFile.setTextColor(Color.parseColor("#111827"));
        btnAddPcFile.setTypeface(Typeface.DEFAULT_BOLD);
        btnAddPcFile.setTextSize(12f);
        btnAddPcFile.setGravity(Gravity.CENTER);
        btnAddPcFile.setPadding(dp(12), dp(10), dp(12), dp(10));
        btnAddPcFile.setBackgroundResource(R.drawable.bg_btn_primary);
        LinearLayout.LayoutParams b1Lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        b1Lp.setMargins(0, 0, dp(6), 0);
        btnAddPcFile.setLayoutParams(b1Lp);
        btnAddPcFile.setOnClickListener(v -> {
            pendingPhotocardTargetIdol = card;
            pendingPhotocardRefreshCallback = () -> {
                CardImageLoader.loadCardImage(MainActivity.this, card, ivMain);
                rebuildPhotocardsRow.run();
                refreshCurrentTab();
            };
            Intent pick = new Intent(Intent.ACTION_GET_CONTENT);
            pick.setType("image/*");
            startActivityForResult(Intent.createChooser(pick, "Elegir Photocard"), REQ_PICK_EXTRA_PHOTOCARD);
        });
        addPcRow.addView(btnAddPcFile);

        TextView btnAddPcUrl = new TextView(this);
        btnAddPcUrl.setText("🔗 Pegar Link");
        btnAddPcUrl.setTextColor(Color.parseColor("#111827"));
        btnAddPcUrl.setTypeface(Typeface.DEFAULT_BOLD);
        btnAddPcUrl.setTextSize(12f);
        btnAddPcUrl.setGravity(Gravity.CENTER);
        btnAddPcUrl.setPadding(dp(12), dp(10), dp(12), dp(10));
        btnAddPcUrl.setBackgroundResource(R.drawable.bg_btn_secondary);
        btnAddPcUrl.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        btnAddPcUrl.setOnClickListener(v -> {
            EditText urlIn = new EditText(this);
            urlIn.setHint("Pegá URL de imagen...");
            new AlertDialog.Builder(this)
                    .setTitle("Agregar Photocard por Link")
                    .setView(urlIn)
                    .setPositiveButton("Agregar", (d, w) -> {
                        String u = urlIn.getText().toString().trim();
                        if (!u.isEmpty()) {
                            card.addPhotocard(u);
                            card.setPhotoUri(u);
                            repo.saveOrUpdateIdol(card);
                            CardImageLoader.loadCardImage(this, card, ivMain);
                            rebuildPhotocardsRow.run();
                            refreshCurrentTab();
                        }
                    })
                    .setNegativeButton("Cancelar", null)
                    .show();
        });
        addPcRow.addView(btnAddPcUrl);
        root.addView(addPcRow);

        // Edit & Share Row
        LinearLayout bottomRow = new LinearLayout(this);
        bottomRow.setOrientation(LinearLayout.HORIZONTAL);

        TextView btnEdit = new TextView(this);
        btnEdit.setText("✏️ Editar Datos");
        btnEdit.setTextColor(Color.parseColor("#111827"));
        btnEdit.setTypeface(Typeface.DEFAULT_BOLD);
        btnEdit.setGravity(Gravity.CENTER);
        btnEdit.setPadding(dp(12), dp(10), dp(12), dp(10));
        btnEdit.setBackgroundResource(R.drawable.bg_btn_secondary);
        LinearLayout.LayoutParams edLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        edLp.setMargins(0, 0, dp(6), 0);
        btnEdit.setLayoutParams(edLp);
        btnEdit.setOnClickListener(v -> {
            if (dialogRef[0] != null) dialogRef[0].dismiss();
            showCreateOrEditIdolDialog(card, this::refreshCurrentTab);
        });
        bottomRow.addView(btnEdit);

        TextView btnShareOne = new TextView(this);
        btnShareOne.setText("📤 Compartir");
        btnShareOne.setTextColor(Color.parseColor("#111827"));
        btnShareOne.setTypeface(Typeface.DEFAULT_BOLD);
        btnShareOne.setGravity(Gravity.CENTER);
        btnShareOne.setPadding(dp(12), dp(10), dp(12), dp(10));
        btnShareOne.setBackgroundResource(R.drawable.bg_btn_gold);
        btnShareOne.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        btnShareOne.setOnClickListener(v -> {
            List<IdolCard> single = Collections.singletonList(card);
            showShareChoiceDialog(card.getStageName() + " (" + card.getGroupName() + ")", null, single);
        });
        bottomRow.addView(btnShareOne);
        root.addView(bottomRow);

        scroll.addView(root);
        builder.setView(scroll);
        builder.setNegativeButton("Cerrar", null);
        dialogRef[0] = builder.create();
        dialogRef[0].show();
    }

    public void showCreateOrEditIdolDialog(IdolCard existing, Runnable onSavedCallback) {
        final boolean isEditing = (existing != null);
        final IdolCard card = isEditing ? existing : new IdolCard();

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.dialog_create_idol));

        ScrollView scroll = new ScrollView(this);
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(18);
        form.setPadding(pad, pad, pad, pad);
        form.setBackgroundColor(Color.WHITE);

        ImageView ivPreview = new ImageView(this);
        LinearLayout.LayoutParams prevLp = new LinearLayout.LayoutParams(dp(110), dp(130));
        prevLp.gravity = Gravity.CENTER_HORIZONTAL;
        prevLp.setMargins(0, 0, 0, dp(10));
        ivPreview.setLayoutParams(prevLp);
        ivPreview.setScaleType(ImageView.ScaleType.CENTER_CROP);
        if (isEditing) CardImageLoader.loadCardImage(this, card, ivPreview);
        form.addView(ivPreview);

        EditText etStage = addFormInput(form, getString(R.string.hint_stage_name), card.getStageName());
        EditText etFull = addFormInput(form, getString(R.string.hint_full_name), card.getFullName());
        EditText etGroup = addFormInput(form, getString(R.string.hint_group_name), card.isSoloist() ? "" : card.getGroupName());

        CheckBox cbSoloist = new CheckBox(this);
        cbSoloist.setText(getString(R.string.check_is_soloist));
        cbSoloist.setTextColor(Color.parseColor("#111827"));
        cbSoloist.setChecked(card.isSoloist());
        form.addView(cbSoloist);

        EditText etCountry = addFormInput(form, getString(R.string.hint_country), card.getCountry());
        EditText etBirth = addFormInput(form, getString(R.string.hint_birth_date), card.getBirthDate());
        EditText etRole = addFormInput(form, getString(R.string.hint_role), card.getRole());
        EditText etPhoto = addFormInput(form, getString(R.string.hint_photo_url), card.getPhotoUri());

        TextView btnPickFile = new TextView(this);
        btnPickFile.setText(getString(R.string.btn_pick_photo_file));
        btnPickFile.setTextColor(Color.parseColor("#111827"));
        btnPickFile.setTypeface(Typeface.DEFAULT_BOLD);
        btnPickFile.setGravity(Gravity.CENTER);
        btnPickFile.setPadding(dp(12), dp(10), dp(12), dp(10));
        btnPickFile.setBackgroundResource(R.drawable.bg_btn_primary);
        LinearLayout.LayoutParams pfLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        pfLp.setMargins(0, 0, 0, dp(10));
        btnPickFile.setLayoutParams(pfLp);
        btnPickFile.setOnClickListener(v -> {
            pendingPhotoUrlField = etPhoto;
            pendingPhotoPreviewView = ivPreview;
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            startActivityForResult(Intent.createChooser(intent, "Elegir Foto"), REQ_PICK_CARD_PHOTO);
        });
        form.addView(btnPickFile);

        EditText etCols = addFormInput(form, getString(R.string.hint_collections), card.getCollections());
        EditText etBio = addFormInput(form, getString(R.string.hint_bio), card.getBio());

        scroll.addView(form);
        builder.setView(scroll);

        builder.setPositiveButton(getString(R.string.btn_save), (d, w) -> {
            String stage = etStage.getText().toString().trim();
            if (stage.isEmpty()) return;
            card.setStageName(stage);
            card.setFullName(etFull.getText().toString().trim());
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
        });

        if (isEditing) {
            builder.setNeutralButton(getString(R.string.btn_delete), (d, w) -> {
                repo.deleteIdol(card.getId());
                if (onSavedCallback != null) onSavedCallback.run();
            });
        }
        builder.setNegativeButton(getString(R.string.btn_cancel), null);
        builder.show();
    }

    private void showShareChoiceDialog(String title, CustomMatch match, List<IdolCard> cards) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.share_mode_title));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(18);
        root.setPadding(pad, pad, pad, pad);
        root.setBackgroundColor(Color.WHITE);

        AlertDialog[] ref = new AlertDialog[1];

        TextView btnFile = new TextView(this);
        btnFile.setText(getString(R.string.share_option_file));
        btnFile.setTextColor(Color.parseColor("#111827"));
        btnFile.setTypeface(Typeface.DEFAULT_BOLD);
        btnFile.setTextSize(13f);
        btnFile.setPadding(dp(14), dp(14), dp(14), dp(14));
        btnFile.setBackgroundResource(R.drawable.bg_btn_primary);
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
        btnCode.setBackgroundResource(R.drawable.bg_btn_secondary);
        btnCode.setOnClickListener(v -> {
            if (ref[0] != null) ref[0].dismiss();
            ShareImportManager.shareAsWhatsAppCode(this, title, match, cards);
        });
        root.addView(btnCode);

        builder.setView(root);
        builder.setNegativeButton(getString(R.string.btn_cancel), null);
        ref[0] = builder.create();
        ref[0].show();
    }

    private void showImportHubDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("📥 Importar Archivo (.wyb) o Código");

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(18);
        root.setPadding(pad, pad, pad, pad);
        root.setBackgroundColor(Color.WHITE);

        AlertDialog[] ref = new AlertDialog[1];

        TextView btnPickWyb = new TextView(this);
        btnPickWyb.setText("📂 Elegir archivo .wyb de tus Archivos");
        btnPickWyb.setTextColor(Color.parseColor("#111827"));
        btnPickWyb.setTypeface(Typeface.DEFAULT_BOLD);
        btnPickWyb.setGravity(Gravity.CENTER);
        btnPickWyb.setPadding(dp(14), dp(13), dp(14), dp(13));
        btnPickWyb.setBackgroundResource(R.drawable.bg_btn_primary);
        LinearLayout.LayoutParams pLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        pLp.setMargins(0, 0, 0, dp(12));
        btnPickWyb.setLayoutParams(pLp);
        btnPickWyb.setOnClickListener(v -> {
            if (ref[0] != null) ref[0].dismiss();
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("*/*");
            startActivityForResult(Intent.createChooser(intent, "Elegir archivo .wyb"), REQ_PICK_WYB_FILE);
        });
        root.addView(btnPickWyb);

        EditText etCode = addFormInput(root, "O pegá acá el mensaje de WhatsApp / código WYB1-...", "");

        ClipboardManager cb = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (cb != null && cb.hasPrimaryClip() && cb.getPrimaryClip() != null && cb.getPrimaryClip().getItemCount() > 0) {
            CharSequence clipText = cb.getPrimaryClip().getItemAt(0).getText();
            if (clipText != null && clipText.toString().contains("WYB1-")) {
                etCode.setText(clipText.toString());
            }
        }

        builder.setView(root);
        builder.setPositiveButton("Ver Vista Previa", (d, w) -> {
            String raw = etCode.getText().toString().trim();
            ShareImportManager.SharedPayload payload = ShareImportManager.decodeFromTextOrCode(this, raw);
            if (payload == null || payload.cards.isEmpty()) {
                Toast.makeText(this, "No se encontró un código WYB1 válido", Toast.LENGTH_SHORT).show();
                return;
            }
            showSelectiveImportDialog(payload);
        });
        builder.setNegativeButton(getString(R.string.btn_cancel), null);
        ref[0] = builder.create();
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
                    btnEditInc.setText("✏️ Editar");
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
            Toast.makeText(this, "✅ Guardados " + savedCount + " idols", Toast.LENGTH_SHORT).show();
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
                String mode = payload.customMatch != null ? payload.customMatch.getGameMode() : "BRACKET";
                launchMatch(payload.packTitle, mode, playList);
            });
        }
        builder.setNegativeButton(getString(R.string.btn_cancel), null);
        builder.show();
    }

    private void showPhotocardGachaDialog() {
        List<IdolCard> all = repo.getAllIdols();
        if (all.isEmpty()) return;
        Random rnd = new Random();
        IdolCard pulled = all.get(rnd.nextInt(all.size()));
        String[] tiers = {"🌟 SSR HOLOGRAPHIC PHOTOCARD", "✨ SSR LIMITED LUCKY DRAW", "💎 SR SPECIAL POB PHOTOCARD"};
        String tier = tiers[rnd.nextInt(tiers.length)];

        pulled.setFavorite(true);
        repo.saveOrUpdateIdol(pulled);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER_HORIZONTAL);
        int pad = dp(20);
        box.setPadding(pad, pad, pad, pad);
        box.setBackgroundColor(Color.WHITE);

        TextView tvTier = new TextView(this);
        tvTier.setText(tier);
        tvTier.setTextColor(Color.parseColor("#111827"));
        tvTier.setTextSize(11.5f);
        tvTier.setTypeface(Typeface.DEFAULT_BOLD);
        tvTier.setPadding(dp(12), dp(5), dp(12), dp(5));
        tvTier.setBackgroundResource(R.drawable.bg_btn_primary);
        box.addView(tvTier);

        CardView pcCard = new CardView(this);
        pcCard.setRadius(dp(20));
        pcCard.setCardElevation(dp(4));
        LinearLayout.LayoutParams ivLp = new LinearLayout.LayoutParams(dp(200), dp(250));
        ivLp.setMargins(0, dp(12), 0, dp(12));
        pcCard.setLayoutParams(ivLp);

        ImageView iv = new ImageView(this);
        iv.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
        CardImageLoader.loadCardImage(this, pulled, iv);
        pcCard.addView(iv);
        box.addView(pcCard);

        TextView tvName = new TextView(this);
        tvName.setText(pulled.getStageName() + " (" + pulled.getGroupName() + ")");
        tvName.setTextColor(Color.parseColor("#111827"));
        tvName.setTextSize(21f);
        tvName.setTypeface(Typeface.DEFAULT_BOLD);
        box.addView(tvName);

        TextView tvSub = new TextView(this);
        tvSub.setText(pulled.getFullName() + " • " + pulled.getCountryWithFlag());
        tvSub.setTextColor(Color.parseColor("#6B7280"));
        tvSub.setTextSize(12.5f);
        tvSub.setGravity(Gravity.CENTER);
        tvSub.setPadding(0, dp(4), 0, dp(6));
        box.addView(tvSub);

        builder.setView(box);
        builder.setPositiveButton("🔄 Abrir Otra", (d, w) -> showPhotocardGachaDialog());
        builder.setNeutralButton("📸 Ver Álbum", (d, w) -> showIdolDetailAndPhotocardBinderDialog(pulled));
        builder.setNegativeButton("Cerrar", (d, w) -> refreshCurrentTab());
        builder.show();
    }

    private void handleIncomingIntent(Intent intent) {
        if (intent == null) return;
        String action = intent.getAction();
        Uri dataUri = intent.getData();

        if (Intent.ACTION_VIEW.equals(action) && dataUri != null) {
            ShareImportManager.SharedPayload payload = ShareImportManager.decodeFromUri(this, dataUri);
            if (payload != null && !payload.cards.isEmpty()) {
                showSelectiveImportDialog(payload);
            }
        } else if (Intent.ACTION_SEND.equals(action)) {
            String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
            Uri streamUri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
            if (streamUri != null) {
                ShareImportManager.SharedPayload payload = ShareImportManager.decodeFromUri(this, streamUri);
                if (payload != null && !payload.cards.isEmpty()) {
                    showSelectiveImportDialog(payload);
                    return;
                }
            }
            if (sharedText != null) {
                ShareImportManager.SharedPayload payload = ShareImportManager.decodeFromTextOrCode(this, sharedText);
                if (payload != null && !payload.cards.isEmpty()) {
                    showSelectiveImportDialog(payload);
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
            if (pendingPhotoPreviewView != null) pendingPhotoPreviewView.setImageURI(Uri.parse(savedPath));
        } else if (requestCode == REQ_PICK_EXTRA_PHOTOCARD && pendingPhotocardTargetIdol != null) {
            String savedPath = CardImageLoader.copyUriToInternalStorage(this, uri);
            pendingPhotocardTargetIdol.addPhotocard(savedPath);
            pendingPhotocardTargetIdol.setPhotoUri(savedPath);
            repo.saveOrUpdateIdol(pendingPhotocardTargetIdol);
            if (pendingPhotocardRefreshCallback != null) pendingPhotocardRefreshCallback.run();
        } else if (requestCode == REQ_PICK_WYB_FILE) {
            ShareImportManager.SharedPayload payload = ShareImportManager.decodeFromUri(this, uri);
            if (payload != null && !payload.cards.isEmpty()) {
                showSelectiveImportDialog(payload);
            }
        }
    }

    private void launchMatch(String title, String gameMode, List<IdolCard> cards) {
        if (cards == null || cards.size() < 2) {
            Toast.makeText(this, "Se necesitan al menos 2 idols", Toast.LENGTH_SHORT).show();
            return;
        }
        ArrayList<String> ids = new ArrayList<>();
        for (IdolCard c : cards) ids.add(c.getId());

        Intent intent = new Intent(this, MatchActivity.class);
        intent.putExtra(MatchActivity.EXTRA_TITLE, title);
        intent.putExtra(MatchActivity.EXTRA_MODE, gameMode);
        intent.putStringArrayListExtra(MatchActivity.EXTRA_CARD_IDS, ids);
        startActivity(intent);
    }

    private void updateCountChipsHighlight(List<TextView> views, int[] values, int selected) {
        for (int i = 0; i < views.size(); i++) {
            boolean active = (values[i] == selected);
            views.get(i).setBackgroundResource(active ? R.drawable.bg_btn_primary : R.drawable.bg_chip_tag);
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
        et.setPadding(dp(14), dp(11), dp(14), dp(11));
        et.setBackgroundResource(R.drawable.bg_chip_tag);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, dp(8));
        et.setLayoutParams(lp);
        parent.addView(et);
        return et;
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
