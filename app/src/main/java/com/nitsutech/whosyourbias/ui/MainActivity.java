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

import com.nitsutech.whosyourbias.R;
import com.nitsutech.whosyourbias.data.BiasRepository;
import com.nitsutech.whosyourbias.data.CustomMatch;
import com.nitsutech.whosyourbias.data.IdolCard;
import com.nitsutech.whosyourbias.util.CardImageLoader;
import com.nitsutech.whosyourbias.util.ShareImportManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
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

    // State for Idol Tab Filtering & Multi-Select Share
    private String idolSearchQuery = "";
    private String idolCategoryFilter = "ALL";
    private boolean isMultiSelectShareMode = false;
    private final Set<String> selectedIdolIdsForShare = new LinkedHashSet<>();

    // Pending photo picker callbacks
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

        findViewById(R.id.btnHeaderGacha).setOnClickListener(v -> showPhotocardGachaDialog());
        findViewById(R.id.btnHeaderImport).setOnClickListener(v -> showImportHubDialog());

        // Initialize YouTube Background Music Player
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
        refreshCurrentTab();
    }

    private void switchTab(int index) {
        currentTab = index;
        int activeColor = Color.parseColor("#00F0FF");
        int inactiveColor = Color.parseColor("#94A3B8");

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
    // TAB 1: ARENA & PLAY (Quick CPU Match, Role Battles, Custom Matches)
    // =========================================================================
    private void renderArenaTab() {
        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(14), dp(14), dp(14), dp(24));

        // 1. CPU Quick Random Match Card
        LinearLayout cpuCard = new LinearLayout(this);
        cpuCard.setOrientation(LinearLayout.VERTICAL);
        cpuCard.setPadding(dp(16), dp(16), dp(16), dp(16));
        cpuCard.setBackgroundResource(R.drawable.bg_glass_header);
        LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cardLp.setMargins(0, 0, 0, dp(16));
        cpuCard.setLayoutParams(cardLp);

        TextView tvQuickTitle = new TextView(this);
        tvQuickTitle.setText(getString(R.string.arena_quick_title));
        tvQuickTitle.setTextColor(Color.WHITE);
        tvQuickTitle.setTextSize(17f);
        tvQuickTitle.setTypeface(Typeface.DEFAULT_BOLD);
        cpuCard.addView(tvQuickTitle);

        TextView tvQuickDesc = new TextView(this);
        tvQuickDesc.setText(getString(R.string.arena_quick_desc));
        tvQuickDesc.setTextColor(Color.parseColor("#94A3B8"));
        tvQuickDesc.setTextSize(12f);
        tvQuickDesc.setPadding(0, dp(4), 0, dp(12));
        cpuCard.addView(tvQuickDesc);

        // Game Mode Spinner
        addSmallLabel(cpuCard, getString(R.string.label_game_mode));
        final String[] modeValues = {"BRACKET", "KING_OF_THE_HILL", "ROUND_ROBIN", "DOUBLE_ELIMINATION"};
        final String[] modeLabels = {
                getString(R.string.mode_bracket),
                getString(R.string.mode_koth),
                getString(R.string.mode_round_robin),
                getString(R.string.mode_double_elim)
        };
        Spinner spMode = createStyledSpinner(modeLabels);
        cpuCard.addView(spMode);

        // Pool Filter Spinner (All, Girl Groups, Boy Groups, Soloists, Companies, or Specific Group)
        addSmallLabel(cpuCard, getString(R.string.label_filter_deck));
        List<String> filterOptions = new ArrayList<>();
        filterOptions.add("All Idols (" + repo.getAllIdols().size() + ")");
        for (String col : repo.getAllCollections()) {
            filterOptions.add("📁 Collection: " + col);
        }
        for (String grp : repo.getAllGroups()) {
            filterOptions.add("✦ Group: " + grp);
        }
        Spinner spFilter = createStyledSpinner(filterOptions.toArray(new String[0]));
        cpuCard.addView(spFilter);

        // Card Count Selector Chips (4, 8, 16, 32, 64, All)
        addSmallLabel(cpuCard, getString(R.string.label_card_count));
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
            chip.setTextSize(12.5f);
            chip.setTypeface(Typeface.DEFAULT_BOLD);
            chip.setPadding(0, dp(9), 0, dp(9));
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
        cpuCard.addView(countRow);

        // Start Random Match Button
        TextView btnStartCpu = new TextView(this);
        btnStartCpu.setText(getString(R.string.btn_start_cpu_match));
        btnStartCpu.setTextColor(Color.WHITE);
        btnStartCpu.setTypeface(Typeface.DEFAULT_BOLD);
        btnStartCpu.setTextSize(14f);
        btnStartCpu.setGravity(Gravity.CENTER);
        btnStartCpu.setPadding(dp(16), dp(13), dp(16), dp(13));
        btnStartCpu.setBackgroundResource(R.drawable.bg_btn_primary);
        LinearLayout.LayoutParams startLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        startLp.setMargins(0, dp(14), 0, 0);
        btnStartCpu.setLayoutParams(startLp);
        btnStartCpu.setOnClickListener(v -> {
            String mode = modeValues[spMode.getSelectedItemPosition()];
            String rawFilter = filterOptions.get(spFilter.getSelectedItemPosition());
            String cleanFilter = "ALL";
            if (rawFilter.startsWith("📁 Collection: ")) {
                cleanFilter = rawFilter.substring("📁 Collection: ".length());
            } else if (rawFilter.startsWith("✦ Group: ")) {
                cleanFilter = rawFilter.substring("✦ Group: ".length());
            }

            List<IdolCard> pool = repo.getIdolsByFilter(cleanFilter);
            if (pool.size() < 2) {
                Toast.makeText(this, "Need at least 2 idols in this filter!", Toast.LENGTH_SHORT).show();
                return;
            }
            Collections.shuffle(pool);
            int limit = Math.min(selectedCount[0], pool.size());
            List<IdolCard> chosen = pool.subList(0, limit);
            String title = "ALL".equals(cleanFilter) ? "CPU Random Showdown (" + limit + ")" : cleanFilter + " Showdown (" + limit + ")";
            launchMatch(title, mode, chosen);
        });
        cpuCard.addView(btnStartCpu);
        root.addView(cpuCard);

        // 2. K-Pop Special Position & Agency Showdowns (1-Tap Presets)
        TextView tvSpecialHeader = new TextView(this);
        tvSpecialHeader.setText("🔥 K-Pop Position & Agency Showdowns (1-Tap)");
        tvSpecialHeader.setTextColor(Color.parseColor("#FFD166"));
        tvSpecialHeader.setTextSize(14f);
        tvSpecialHeader.setTypeface(Typeface.DEFAULT_BOLD);
        tvSpecialHeader.setPadding(0, 0, 0, dp(8));
        root.addView(tvSpecialHeader);

        HorizontalScrollView specialScroll = new HorizontalScrollView(this);
        specialScroll.setHorizontalScrollBarEnabled(false);
        LinearLayout specialRow = new LinearLayout(this);
        specialRow.setOrientation(LinearLayout.HORIZONTAL);
        specialRow.setPadding(0, 0, 0, dp(14));

        addSpecialShowdownChip(specialRow, "🎤 Main Vocals Battle", "Vocal", "BRACKET", 16);
        addSpecialShowdownChip(specialRow, "💃 Main Dancers Battle", "Dancer", "BRACKET", 16);
        addSpecialShowdownChip(specialRow, "✨ Visuals & Centers", "Visual", "KING_OF_THE_HILL", 16);
        addSpecialShowdownChip(specialRow, "🔥 Rappers Cipher", "Rapper", "BRACKET", 16);
        addSpecialShowdownChip(specialRow, "👑 Leaders Summit", "Leader", "BRACKET", 16);
        addSpecialShowdownChip(specialRow, "🐣 Maknaes on Top", "Maknae", "KING_OF_THE_HILL", 16);
        specialScroll.addView(specialRow);
        root.addView(specialScroll);

        // 3. Custom Matches & WhatsApp Challenges Section
        LinearLayout customBox = new LinearLayout(this);
        customBox.setOrientation(LinearLayout.VERTICAL);
        customBox.setPadding(dp(16), dp(16), dp(16), dp(16));
        customBox.setBackgroundResource(R.drawable.bg_glass_card);
        root.addView(customBox);

        TextView tvCustomTitle = new TextView(this);
        tvCustomTitle.setText(getString(R.string.arena_custom_title));
        tvCustomTitle.setTextColor(Color.WHITE);
        tvCustomTitle.setTextSize(16f);
        tvCustomTitle.setTypeface(Typeface.DEFAULT_BOLD);
        customBox.addView(tvCustomTitle);

        TextView tvCustomDesc = new TextView(this);
        tvCustomDesc.setText(getString(R.string.arena_custom_desc));
        tvCustomDesc.setTextColor(Color.parseColor("#94A3B8"));
        tvCustomDesc.setTextSize(12f);
        tvCustomDesc.setPadding(0, dp(4), 0, dp(12));
        customBox.addView(tvCustomDesc);

        // Buttons: Create Custom Match & Import from WhatsApp
        LinearLayout customBtnRow = new LinearLayout(this);
        customBtnRow.setOrientation(LinearLayout.VERTICAL);

        TextView btnCreateMatch = new TextView(this);
        btnCreateMatch.setText(getString(R.string.btn_create_custom_match));
        btnCreateMatch.setTextColor(Color.WHITE);
        btnCreateMatch.setTypeface(Typeface.DEFAULT_BOLD);
        btnCreateMatch.setGravity(Gravity.CENTER);
        btnCreateMatch.setPadding(dp(12), dp(11), dp(12), dp(11));
        btnCreateMatch.setBackgroundResource(R.drawable.bg_btn_primary);
        btnCreateMatch.setOnClickListener(v -> showCreateCustomMatchDialog());
        customBtnRow.addView(btnCreateMatch);

        TextView btnImportMatch = new TextView(this);
        btnImportMatch.setText(getString(R.string.btn_import_whatsapp));
        btnImportMatch.setTextColor(Color.parseColor("#00F0FF"));
        btnImportMatch.setTypeface(Typeface.DEFAULT_BOLD);
        btnImportMatch.setGravity(Gravity.CENTER);
        btnImportMatch.setPadding(dp(12), dp(10), dp(12), dp(10));
        btnImportMatch.setBackgroundResource(R.drawable.bg_btn_secondary);
        LinearLayout.LayoutParams impLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        impLp.setMargins(0, dp(8), 0, dp(14));
        btnImportMatch.setLayoutParams(impLp);
        btnImportMatch.setOnClickListener(v -> showImportHubDialog());
        customBtnRow.addView(btnImportMatch);

        customBox.addView(customBtnRow);

        // Saved Custom Matches List
        List<CustomMatch> matches = repo.getCustomMatches();
        for (CustomMatch cm : matches) {
            customBox.addView(buildCustomMatchItemView(cm));
        }

        scroll.addView(root);
        mainContentContainer.addView(scroll);
    }

    private void addSpecialShowdownChip(LinearLayout parent, String title, String roleKeyword, String mode, int maxCount) {
        TextView chip = new TextView(this);
        chip.setText(title);
        chip.setTextColor(Color.WHITE);
        chip.setTextSize(12f);
        chip.setTypeface(Typeface.DEFAULT_BOLD);
        chip.setPadding(dp(14), dp(10), dp(14), dp(10));
        chip.setBackgroundResource(R.drawable.bg_btn_secondary);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, dp(8), 0);
        chip.setLayoutParams(lp);
        chip.setOnClickListener(v -> {
            List<IdolCard> pool = new ArrayList<>();
            for (IdolCard c : repo.getAllIdols()) {
                if (c.getRole() != null && c.getRole().toLowerCase(Locale.ROOT).contains(roleKeyword.toLowerCase(Locale.ROOT))) {
                    pool.add(c);
                }
            }
            Collections.shuffle(pool);
            if (pool.size() > maxCount) pool = pool.subList(0, maxCount);
            launchMatch(title, mode, pool);
        });
        parent.addView(chip);
    }

    private View buildCustomMatchItemView(CustomMatch cm) {
        LinearLayout item = new LinearLayout(this);
        item.setOrientation(LinearLayout.VERTICAL);
        item.setPadding(dp(12), dp(12), dp(12), dp(12));
        item.setBackgroundResource(R.drawable.bg_chip_tag);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, dp(10));
        item.setLayoutParams(lp);

        TextView tvTitle = new TextView(this);
        tvTitle.setText(cm.getTitle());
        tvTitle.setTextColor(Color.WHITE);
        tvTitle.setTextSize(14f);
        tvTitle.setTypeface(Typeface.DEFAULT_BOLD);
        item.addView(tvTitle);

        TextView tvMeta = new TextView(this);
        tvMeta.setText("By " + cm.getCreatorName() + "  •  Mode: " + cm.getGameMode() + "  •  " + cm.getCardIds().size() + " Cards");
        tvMeta.setTextColor(Color.parseColor("#94A3B8"));
        tvMeta.setTextSize(11.5f);
        tvMeta.setPadding(0, dp(2), 0, dp(8));
        item.addView(tvMeta);

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);

        TextView btnPlay = new TextView(this);
        btnPlay.setText("▶ Play");
        btnPlay.setTextColor(Color.WHITE);
        btnPlay.setTypeface(Typeface.DEFAULT_BOLD);
        btnPlay.setTextSize(12f);
        btnPlay.setGravity(Gravity.CENTER);
        btnPlay.setPadding(dp(14), dp(7), dp(14), dp(7));
        btnPlay.setBackgroundResource(R.drawable.bg_btn_primary);
        LinearLayout.LayoutParams playLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        playLp.setMargins(0, 0, dp(6), 0);
        btnPlay.setLayoutParams(playLp);
        btnPlay.setOnClickListener(v -> {
            List<IdolCard> roster = new ArrayList<>();
            for (String cid : cm.getCardIds()) {
                IdolCard c = repo.getIdolById(cid);
                if (c != null) roster.add(c);
            }
            launchMatch(cm.getTitle(), cm.getGameMode(), roster);
        });
        actions.addView(btnPlay);

        TextView btnShare = new TextView(this);
        btnShare.setText("📤 WhatsApp");
        btnShare.setTextColor(Color.parseColor("#00F0FF"));
        btnShare.setTypeface(Typeface.DEFAULT_BOLD);
        btnShare.setTextSize(12f);
        btnShare.setGravity(Gravity.CENTER);
        btnShare.setPadding(dp(12), dp(7), dp(12), dp(7));
        btnShare.setBackgroundResource(R.drawable.bg_btn_secondary);
        LinearLayout.LayoutParams shLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        shLp.setMargins(0, 0, dp(6), 0);
        btnShare.setLayoutParams(shLp);
        btnShare.setOnClickListener(v -> {
            List<IdolCard> cards = new ArrayList<>();
            for (String cid : cm.getCardIds()) {
                IdolCard c = repo.getIdolById(cid);
                if (c != null) cards.add(c);
            }
            showShareChoiceDialog(cm.getTitle(), cm, cards);
        });
        actions.addView(btnShare);

        TextView btnDel = new TextView(this);
        btnDel.setText("🗑️");
        btnDel.setTextColor(Color.parseColor("#FF3366"));
        btnDel.setTextSize(12f);
        btnDel.setGravity(Gravity.CENTER);
        btnDel.setPadding(dp(10), dp(7), dp(10), dp(7));
        btnDel.setBackgroundResource(R.drawable.bg_btn_secondary);
        btnDel.setOnClickListener(v -> {
            repo.deleteCustomMatch(cm.getId());
            refreshCurrentTab();
        });
        actions.addView(btnDel);

        item.addView(actions);
        return item;
    }

    // =========================================================================
    // TAB 2: IDOL CARDS & PHOTOCARD BINDER
    // =========================================================================
    private void renderIdolsTab() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        root.setPadding(dp(12), dp(10), dp(12), 0);

        // Top Action Row (+ New Idol Card & Share Idols to WhatsApp)
        LinearLayout topActions = new LinearLayout(this);
        topActions.setOrientation(LinearLayout.HORIZONTAL);
        topActions.setPadding(0, 0, 0, dp(8));

        if (!isMultiSelectShareMode) {
            TextView btnAddIdol = new TextView(this);
            btnAddIdol.setText(getString(R.string.btn_add_idol));
            btnAddIdol.setTextColor(Color.WHITE);
            btnAddIdol.setTypeface(Typeface.DEFAULT_BOLD);
            btnAddIdol.setTextSize(12f);
            btnAddIdol.setGravity(Gravity.CENTER);
            btnAddIdol.setPadding(dp(12), dp(9), dp(12), dp(9));
            btnAddIdol.setBackgroundResource(R.drawable.bg_btn_primary);
            LinearLayout.LayoutParams addLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            addLp.setMargins(0, 0, dp(8), 0);
            btnAddIdol.setLayoutParams(addLp);
            btnAddIdol.setOnClickListener(v -> showCreateOrEditIdolDialog(null, this::refreshCurrentTab));
            topActions.addView(btnAddIdol);

            TextView btnShareMode = new TextView(this);
            btnShareMode.setText(getString(R.string.btn_share_idols));
            btnShareMode.setTextColor(Color.parseColor("#00F0FF"));
            btnShareMode.setTypeface(Typeface.DEFAULT_BOLD);
            btnShareMode.setTextSize(12f);
            btnShareMode.setGravity(Gravity.CENTER);
            btnShareMode.setPadding(dp(12), dp(9), dp(12), dp(9));
            btnShareMode.setBackgroundResource(R.drawable.bg_btn_secondary);
            btnShareMode.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            btnShareMode.setOnClickListener(v -> {
                isMultiSelectShareMode = true;
                selectedIdolIdsForShare.clear();
                refreshCurrentTab();
            });
            topActions.addView(btnShareMode);
        } else {
            TextView btnSendSelected = new TextView(this);
            btnSendSelected.setText("📤 Send (" + selectedIdolIdsForShare.size() + ") via WhatsApp");
            btnSendSelected.setTextColor(Color.parseColor("#0B0E17"));
            btnSendSelected.setTypeface(Typeface.DEFAULT_BOLD);
            btnSendSelected.setTextSize(12f);
            btnSendSelected.setGravity(Gravity.CENTER);
            btnSendSelected.setPadding(dp(10), dp(9), dp(10), dp(9));
            btnSendSelected.setBackgroundResource(R.drawable.bg_btn_gold);
            LinearLayout.LayoutParams sendLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            sendLp.setMargins(0, 0, dp(6), 0);
            btnSendSelected.setLayoutParams(sendLp);
            btnSendSelected.setOnClickListener(v -> {
                if (selectedIdolIdsForShare.isEmpty()) {
                    Toast.makeText(this, "Tap idols below to select them first!", Toast.LENGTH_SHORT).show();
                    return;
                }
                List<IdolCard> toShare = new ArrayList<>();
                for (String id : selectedIdolIdsForShare) {
                    IdolCard c = repo.getIdolById(id);
                    if (c != null) toShare.add(c);
                }
                isMultiSelectShareMode = false;
                refreshCurrentTab();
                showShareChoiceDialog("Idol Pack (" + toShare.size() + " Cards)", null, toShare);
            });
            topActions.addView(btnSendSelected);

            TextView btnSelectAll = new TextView(this);
            btnSelectAll.setText(getString(R.string.btn_select_all));
            btnSelectAll.setTextColor(Color.WHITE);
            btnSelectAll.setTextSize(11.5f);
            btnSelectAll.setPadding(dp(10), dp(9), dp(10), dp(9));
            btnSelectAll.setBackgroundResource(R.drawable.bg_btn_secondary);
            LinearLayout.LayoutParams saLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            saLp.setMargins(0, 0, dp(6), 0);
            btnSelectAll.setLayoutParams(saLp);
            btnSelectAll.setOnClickListener(v -> {
                for (IdolCard c : getFilteredIdolsList()) {
                    selectedIdolIdsForShare.add(c.getId());
                }
                refreshCurrentTab();
            });
            topActions.addView(btnSelectAll);

            TextView btnCancelMulti = new TextView(this);
            btnCancelMulti.setText("✕");
            btnCancelMulti.setTextColor(Color.parseColor("#FF3366"));
            btnCancelMulti.setTextSize(12f);
            btnCancelMulti.setPadding(dp(10), dp(9), dp(10), dp(9));
            btnCancelMulti.setBackgroundResource(R.drawable.bg_btn_secondary);
            btnCancelMulti.setOnClickListener(v -> {
                isMultiSelectShareMode = false;
                selectedIdolIdsForShare.clear();
                refreshCurrentTab();
            });
            topActions.addView(btnCancelMulti);
        }
        root.addView(topActions);

        // Search Input
        EditText etSearch = new EditText(this);
        etSearch.setHint(getString(R.string.search_idols_hint));
        etSearch.setText(idolSearchQuery);
        etSearch.setTextColor(Color.WHITE);
        etSearch.setHintTextColor(Color.parseColor("#64748B"));
        etSearch.setTextSize(13f);
        etSearch.setSingleLine(true);
        etSearch.setPadding(dp(12), dp(9), dp(12), dp(9));
        etSearch.setBackgroundResource(R.drawable.bg_chip_tag);
        root.addView(etSearch);

        // Horizontal Filter Chips (All, Favorites, Soloists, Girl Groups, Boy Groups, Groups...)
        HorizontalScrollView filterScroll = new HorizontalScrollView(this);
        filterScroll.setHorizontalScrollBarEnabled(false);
        LinearLayout filterRow = new LinearLayout(this);
        filterRow.setOrientation(LinearLayout.HORIZONTAL);
        filterRow.setPadding(0, dp(8), 0, dp(8));

        List<String> chips = new ArrayList<>();
        chips.add("ALL");
        chips.add("Favorites");
        chips.add("Soloists");
        chips.add("Girl Groups");
        chips.add("Boy Groups");
        chips.add("HYBE");
        chips.add("JYP Entertainment");
        chips.add("SM Entertainment");
        chips.add("THEBLACKLABEL");
        chips.add("YG Entertainment");
        chips.addAll(repo.getAllGroups());

        for (String ch : chips) {
            TextView chipView = new TextView(this);
            boolean active = ch.equalsIgnoreCase(idolCategoryFilter);
            chipView.setText("ALL".equals(ch) ? "All (" + repo.getAllIdols().size() + ")" : ch);
            chipView.setTextColor(active ? Color.WHITE : Color.parseColor("#94A3B8"));
            chipView.setTypeface(null, active ? Typeface.BOLD : Typeface.NORMAL);
            chipView.setTextSize(11.5f);
            chipView.setPadding(dp(11), dp(6), dp(11), dp(6));
            chipView.setBackgroundResource(active ? R.drawable.bg_btn_primary : R.drawable.bg_chip_tag);
            LinearLayout.LayoutParams cLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            cLp.setMargins(0, 0, dp(6), 0);
            chipView.setLayoutParams(cLp);
            chipView.setOnClickListener(v -> {
                idolCategoryFilter = ch;
                refreshCurrentTab();
            });
            filterRow.addView(chipView);
        }
        filterScroll.addView(filterRow);
        root.addView(filterScroll);

        // Scrollable Idol Cards List
        ScrollView listScroll = new ScrollView(this);
        listScroll.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        LinearLayout cardsContainer = new LinearLayout(this);
        cardsContainer.setOrientation(LinearLayout.VERTICAL);
        cardsContainer.setPadding(0, dp(4), 0, dp(20));

        populateIdolCardsList(cardsContainer);

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                idolSearchQuery = s.toString();
                populateIdolCardsList(cardsContainer);
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        listScroll.addView(cardsContainer);
        root.addView(listScroll);
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

    private void populateIdolCardsList(LinearLayout container) {
        container.removeAllViews();
        List<IdolCard> list = getFilteredIdolsList();
        for (IdolCard c : list) {
            container.addView(buildIdolListRow(c));
        }
    }

    private View buildIdolListRow(IdolCard card) {
        int accent = CardImageLoader.parseColorSafe(card.getAccentColor(), Color.parseColor("#7B61FF"));
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(10), dp(10), dp(10), dp(10));
        row.setBackgroundResource(R.drawable.bg_glass_card);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, dp(8));
        row.setLayoutParams(lp);

        if (isMultiSelectShareMode) {
            CheckBox cb = new CheckBox(this);
            cb.setChecked(selectedIdolIdsForShare.contains(card.getId()));
            cb.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) selectedIdolIdsForShare.add(card.getId());
                else selectedIdolIdsForShare.remove(card.getId());
            });
            row.addView(cb);
        }

        ImageView iv = new ImageView(this);
        LinearLayout.LayoutParams ivLp = new LinearLayout.LayoutParams(dp(64), dp(76));
        ivLp.setMargins(0, 0, dp(12), 0);
        iv.setLayoutParams(ivLp);
        iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
        CardImageLoader.loadCardImage(this, card, iv);
        row.addView(iv);

        LinearLayout info = new LinearLayout(this);
        info.setOrientation(LinearLayout.VERTICAL);
        info.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView tvTop = new TextView(this);
        tvTop.setText((card.isSoloist() ? "★ SOLOIST" : "✦ " + card.getGroupName()) + "  •  " + card.getCountryWithFlag());
        tvTop.setTextColor(accent);
        tvTop.setTextSize(10.5f);
        tvTop.setTypeface(Typeface.DEFAULT_BOLD);
        info.addView(tvTop);

        TextView tvName = new TextView(this);
        String pcCount = card.getPhotocards().size() > 1 ? "  📸" + card.getPhotocards().size() : "";
        tvName.setText(card.getStageName() + " (" + card.getFullName() + ")" + pcCount);
        tvName.setTextColor(Color.WHITE);
        tvName.setTextSize(15f);
        tvName.setTypeface(Typeface.DEFAULT_BOLD);
        info.addView(tvName);

        TextView tvSub = new TextView(this);
        tvSub.setText("Age " + card.getCalculatedAge() + " (" + card.getZodiacSign() + ") • " + card.getRole());
        tvSub.setTextColor(Color.parseColor("#94A3B8"));
        tvSub.setTextSize(11.5f);
        info.addView(tvSub);

        List<IdolCard> mates = repo.getFellowGroupMembers(card);
        if (!mates.isEmpty()) {
            StringBuilder msb = new StringBuilder("🤝 Group: ");
            for (int i = 0; i < Math.min(5, mates.size()); i++) {
                if (i > 0) msb.append(", ");
                msb.append(mates.get(i).getStageName());
            }
            if (mates.size() > 5) msb.append(" +").append(mates.size() - 5);
            TextView tvMates = new TextView(this);
            tvMates.setText(msb.toString());
            tvMates.setTextColor(Color.parseColor("#00F0FF"));
            tvMates.setTextSize(10.5f);
            info.addView(tvMates);
        }

        row.addView(info);

        TextView btnFav = new TextView(this);
        btnFav.setText(card.isFavorite() ? "⭐" : "☆");
        btnFav.setTextColor(card.isFavorite() ? Color.parseColor("#FFD166") : Color.parseColor("#64748B"));
        btnFav.setTextSize(19f);
        btnFav.setPadding(dp(8), dp(8), dp(8), dp(8));
        btnFav.setOnClickListener(v -> {
            card.setFavorite(!card.isFavorite());
            repo.saveOrUpdateIdol(card);
            btnFav.setText(card.isFavorite() ? "⭐" : "☆");
            btnFav.setTextColor(card.isFavorite() ? Color.parseColor("#FFD166") : Color.parseColor("#64748B"));
        });
        row.addView(btnFav);

        row.setOnClickListener(v -> {
            if (isMultiSelectShareMode) {
                if (selectedIdolIdsForShare.contains(card.getId())) selectedIdolIdsForShare.remove(card.getId());
                else selectedIdolIdsForShare.add(card.getId());
                refreshCurrentTab();
            } else {
                showIdolDetailAndPhotocardBinderDialog(card);
            }
        });

        return row;
    }

    // =========================================================================
    // TAB 3: GROUPS & COLLECTIONS
    // =========================================================================
    private void renderGroupsTab() {
        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(14), dp(12), dp(14), dp(24));

        TextView tvHeader = new TextView(this);
        tvHeader.setText("✨ K-Pop Groups & Agencies — Tap to Play or Share Group");
        tvHeader.setTextColor(Color.parseColor("#00F0FF"));
        tvHeader.setTextSize(14f);
        tvHeader.setTypeface(Typeface.DEFAULT_BOLD);
        tvHeader.setPadding(0, 0, 0, dp(10));
        root.addView(tvHeader);

        // Soloists Card first
        List<IdolCard> soloists = repo.getIdolsByFilter("Soloists");
        if (!soloists.isEmpty()) {
            root.addView(buildGroupDeckCard("Soloists", "🎤 Iconic K-Pop Soloists", soloists));
        }

        // Every Group Card
        for (String groupName : repo.getAllGroups()) {
            List<IdolCard> members = repo.getIdolsByFilter(groupName);
            if (!members.isEmpty()) {
                root.addView(buildGroupDeckCard(groupName, "✦ " + groupName, members));
            }
        }

        scroll.addView(root);
        mainContentContainer.addView(scroll);
    }

    private View buildGroupDeckCard(String filterKey, String displayTitle, List<IdolCard> members) {
        int accent = CardImageLoader.parseColorSafe(members.get(0).getAccentColor(), Color.parseColor("#7B61FF"));
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(14), dp(14), dp(14), dp(14));
        box.setBackgroundResource(R.drawable.bg_glass_card);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, dp(10));
        box.setLayoutParams(lp);

        TextView tvTitle = new TextView(this);
        tvTitle.setText(displayTitle + " (" + members.size() + " Members)");
        tvTitle.setTextColor(accent);
        tvTitle.setTextSize(16f);
        tvTitle.setTypeface(Typeface.DEFAULT_BOLD);
        box.addView(tvTitle);

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < members.size(); i++) {
            if (i > 0) sb.append(" • ");
            sb.append(members.get(i).getStageName());
        }
        TextView tvMembers = new TextView(this);
        tvMembers.setText(sb.toString());
        tvMembers.setTextColor(Color.WHITE);
        tvMembers.setTextSize(12.5f);
        tvMembers.setPadding(0, dp(4), 0, dp(10));
        box.addView(tvMembers);

        LinearLayout btnRow = new LinearLayout(this);
        btnRow.setOrientation(LinearLayout.HORIZONTAL);

        TextView btnPlayGroup = new TextView(this);
        btnPlayGroup.setText("▶ Play Bias Battle");
        btnPlayGroup.setTextColor(Color.WHITE);
        btnPlayGroup.setTypeface(Typeface.DEFAULT_BOLD);
        btnPlayGroup.setTextSize(12f);
        btnPlayGroup.setGravity(Gravity.CENTER);
        btnPlayGroup.setPadding(dp(12), dp(8), dp(12), dp(8));
        btnPlayGroup.setBackgroundResource(R.drawable.bg_btn_primary);
        LinearLayout.LayoutParams pLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        pLp.setMargins(0, 0, dp(8), 0);
        btnPlayGroup.setLayoutParams(pLp);
        btnPlayGroup.setOnClickListener(v -> {
            String mode = members.size() <= 6 ? "KING_OF_THE_HILL" : "BRACKET";
            launchMatch("Who is your bias in " + filterKey + "?", mode, members);
        });
        btnRow.addView(btnPlayGroup);

        TextView btnShareGroup = new TextView(this);
        btnShareGroup.setText("📤 Share Group");
        btnShareGroup.setTextColor(Color.parseColor("#00F0FF"));
        btnShareGroup.setTypeface(Typeface.DEFAULT_BOLD);
        btnShareGroup.setTextSize(12f);
        btnShareGroup.setGravity(Gravity.CENTER);
        btnShareGroup.setPadding(dp(12), dp(8), dp(12), dp(8));
        btnShareGroup.setBackgroundResource(R.drawable.bg_btn_secondary);
        btnShareGroup.setOnClickListener(v -> {
            List<String> ids = new ArrayList<>();
            for (IdolCard m : members) ids.add(m.getId());
            CustomMatch groupMatch = new CustomMatch(
                    null, "Who is your bias in " + filterKey + "?", "Friend",
                    members.size() <= 6 ? "KING_OF_THE_HILL" : "BRACKET", ids, System.currentTimeMillis());
            showShareChoiceDialog(filterKey + " Pack & Challenge", groupMatch, members);
        });
        btnRow.addView(btnShareGroup);

        box.addView(btnRow);
        return box;
    }

    // =========================================================================
    // TAB 4: HALL OF FAME, GACHA & BIAS CHEMISTRY
    // =========================================================================
    private void renderHallOfFameTab() {
        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(14), dp(14), dp(14), dp(24));

        // Photocard Gacha Banner
        LinearLayout gachaBox = new LinearLayout(this);
        gachaBox.setOrientation(LinearLayout.VERTICAL);
        gachaBox.setPadding(dp(16), dp(16), dp(16), dp(16));
        gachaBox.setBackgroundResource(R.drawable.bg_glass_header);
        LinearLayout.LayoutParams gLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        gLp.setMargins(0, 0, 0, dp(16));
        gachaBox.setLayoutParams(gLp);

        TextView tvGachaTitle = new TextView(this);
        tvGachaTitle.setText("🎁 Photocard Mystery Gacha Unboxer");
        tvGachaTitle.setTextColor(Color.parseColor("#FFD166"));
        tvGachaTitle.setTextSize(16f);
        tvGachaTitle.setTypeface(Typeface.DEFAULT_BOLD);
        gachaBox.addView(tvGachaTitle);

        TextView tvGachaDesc = new TextView(this);
        tvGachaDesc.setText("Pull a random SSR / SR Holographic Idol Photocard and test your daily K-Pop luck!");
        tvGachaDesc.setTextColor(Color.parseColor("#CBD5E1"));
        tvGachaDesc.setTextSize(12f);
        tvGachaDesc.setPadding(0, dp(4), 0, dp(10));
        gachaBox.addView(tvGachaDesc);

        TextView btnPullGacha = new TextView(this);
        btnPullGacha.setText("✨ UNBOX MYSTERY PHOTOCARD");
        btnPullGacha.setTextColor(Color.parseColor("#0B0E17"));
        btnPullGacha.setTypeface(Typeface.DEFAULT_BOLD);
        btnPullGacha.setGravity(Gravity.CENTER);
        btnPullGacha.setPadding(dp(14), dp(11), dp(14), dp(11));
        btnPullGacha.setBackgroundResource(R.drawable.bg_btn_gold);
        btnPullGacha.setOnClickListener(v -> showPhotocardGachaDialog());
        gachaBox.addView(btnPullGacha);
        root.addView(gachaBox);

        // Hall of Fame Leaderboard
        TextView tvHofTitle = new TextView(this);
        tvHofTitle.setText("👑 Hall of Fame — Your Top Crowned Biases");
        tvHofTitle.setTextColor(Color.WHITE);
        tvHofTitle.setTextSize(16f);
        tvHofTitle.setTypeface(Typeface.DEFAULT_BOLD);
        tvHofTitle.setPadding(0, 0, 0, dp(10));
        root.addView(tvHofTitle);

        List<IdolCard> ranked = repo.getAllIdols();
        ranked.sort((a, b) -> {
            if (b.getTournamentsWon() != a.getTournamentsWon()) {
                return Integer.compare(b.getTournamentsWon(), a.getTournamentsWon());
            }
            if (b.getMatchesWon() != a.getMatchesWon()) {
                return Integer.compare(b.getMatchesWon(), a.getMatchesWon());
            }
            return Boolean.compare(b.isFavorite(), a.isFavorite());
        });

        for (int i = 0; i < Math.min(25, ranked.size()); i++) {
            IdolCard c = ranked.get(i);
            LinearLayout row = (LinearLayout) buildIdolListRow(c);
            TextView badge = new TextView(this);
            badge.setText("#" + (i + 1) + "\n🏆" + c.getTournamentsWon());
            badge.setTextColor(i < 3 ? Color.parseColor("#FFD166") : Color.parseColor("#00F0FF"));
            badge.setTypeface(Typeface.DEFAULT_BOLD);
            badge.setTextSize(12f);
            badge.setGravity(Gravity.CENTER);
            badge.setPadding(dp(6), 0, dp(10), 0);
            row.addView(badge, 0);
            root.addView(row);
        }

        scroll.addView(root);
        mainContentContainer.addView(scroll);
    }

    // =========================================================================
    // DIALOGS: IDOL DOSSIER & PHOTOCARD BINDER, CREATE/EDIT IDOL, SHARE & IMPORT
    // =========================================================================

    private void showIdolDetailAndPhotocardBinderDialog(IdolCard card) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, AlertDialog.THEME_DEVICE_DEFAULT_DARK);
        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(16);
        root.setPadding(pad, pad, pad, pad);
        root.setBackgroundColor(Color.parseColor("#131829"));

        AlertDialog[] dialogRef = new AlertDialog[1];

        // Main Portrait Preview (tap to cycle photocards)
        ImageView ivMain = new ImageView(this);
        LinearLayout.LayoutParams ivLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(250));
        ivLp.setMargins(0, 0, 0, dp(10));
        ivMain.setLayoutParams(ivLp);
        ivMain.setScaleType(ImageView.ScaleType.CENTER_CROP);
        CardImageLoader.loadCardImage(this, card, ivMain);
        root.addView(ivMain);

        TextView tvName = new TextView(this);
        tvName.setText(card.getStageName() + "  •  " + (card.isSoloist() ? "Soloist" : card.getGroupName()));
        tvName.setTextColor(Color.WHITE);
        tvName.setTextSize(20f);
        tvName.setTypeface(Typeface.DEFAULT_BOLD);
        root.addView(tvName);

        TextView tvDetails = new TextView(this);
        StringBuilder dsb = new StringBuilder();
        dsb.append("Full Name: ").append(card.getFullName()).append("\n");
        dsb.append("Origin: ").append(card.getCountryWithFlag()).append("\n");
        dsb.append("Birthdate & Age: ").append(card.getBirthDate()).append(" (").append(card.getCalculatedAge()).append(" yrs • ").append(card.getZodiacSign()).append(")\n");
        dsb.append("Role: ").append(card.getRole()).append("\n");
        dsb.append("Collections: ").append(card.getCollections()).append("\n");
        List<IdolCard> mates = repo.getFellowGroupMembers(card);
        if (!mates.isEmpty()) {
            dsb.append("Fellow ").append(card.getGroupName()).append(" Members: ");
            for (int i = 0; i < mates.size(); i++) {
                if (i > 0) dsb.append(", ");
                dsb.append(mates.get(i).getStageName());
            }
            dsb.append("\n");
        }
        dsb.append("\n").append(card.getBio());
        tvDetails.setText(dsb.toString());
        tvDetails.setTextColor(Color.parseColor("#CBD5E1"));
        tvDetails.setTextSize(12.5f);
        tvDetails.setPadding(0, dp(6), 0, dp(12));
        root.addView(tvDetails);

        // ================= PHOTOCARD BINDER SECTION =================
        TextView tvPcTitle = new TextView(this);
        tvPcTitle.setText("📸 Favorite Photocards for " + card.getStageName() + " (" + card.getPhotocards().size() + ")");
        tvPcTitle.setTextColor(Color.parseColor("#FFD166"));
        tvPcTitle.setTextSize(14f);
        tvPcTitle.setTypeface(Typeface.DEFAULT_BOLD);
        tvPcTitle.setPadding(0, 0, 0, dp(6));
        root.addView(tvPcTitle);

        HorizontalScrollView pcScroll = new HorizontalScrollView(this);
        LinearLayout pcRow = new LinearLayout(this);
        pcRow.setOrientation(LinearLayout.HORIZONTAL);
        pcRow.setPadding(0, 0, 0, dp(10));

        Runnable rebuildPhotocardsRow = new Runnable() {
            @Override
            public void run() {
                pcRow.removeAllViews();
                tvPcTitle.setText("📸 Favorite Photocards for " + card.getStageName() + " (" + card.getPhotocards().size() + ")");
                for (int i = 0; i < card.getPhotocards().size(); i++) {
                    final String pcUri = card.getPhotocards().get(i);
                    final int idx = i;
                    LinearLayout pcItem = new LinearLayout(MainActivity.this);
                    pcItem.setOrientation(LinearLayout.VERTICAL);
                    pcItem.setGravity(Gravity.CENTER_HORIZONTAL);
                    pcItem.setPadding(dp(6), dp(6), dp(6), dp(6));
                    pcItem.setBackgroundResource(R.drawable.bg_chip_tag);
                    LinearLayout.LayoutParams pLp = new LinearLayout.LayoutParams(dp(105), ViewGroup.LayoutParams.WRAP_CONTENT);
                    pLp.setMargins(0, 0, dp(8), 0);
                    pcItem.setLayoutParams(pLp);

                    ImageView ivThumb = new ImageView(MainActivity.this);
                    ivThumb.setLayoutParams(new LinearLayout.LayoutParams(dp(90), dp(115)));
                    ivThumb.setScaleType(ImageView.ScaleType.CENTER_CROP);
                    IdolCard temp = new IdolCard(card.getId() + "_thumb_" + idx, card.getStageName(),
                            card.getFullName(), card.getGroupName(), card.isSoloist(), card.getCountry(),
                            card.getBirthDate(), card.getRole(), pcUri, card.getAccentColor(), card.getBio(), card.getCollections());
                    CardImageLoader.loadCardImage(MainActivity.this, temp, ivThumb);
                    ivThumb.setOnClickListener(v -> {
                        card.setPhotoUri(pcUri);
                        repo.saveOrUpdateIdol(card);
                        CardImageLoader.loadCardImage(MainActivity.this, card, ivMain);
                        Toast.makeText(MainActivity.this, "✨ Set as main cover photocard!", Toast.LENGTH_SHORT).show();
                    });
                    pcItem.addView(ivThumb);

                    TextView btnRemovePc = new TextView(MainActivity.this);
                    btnRemovePc.setText("🗑️ Remove");
                    btnRemovePc.setTextColor(Color.parseColor("#FF3366"));
                    btnRemovePc.setTextSize(10f);
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
                    pcRow.addView(pcItem);
                }
            }
        };
        rebuildPhotocardsRow.run();
        pcScroll.addView(pcRow);
        root.addView(pcScroll);

        // Add Photocard from Files or URL Buttons
        LinearLayout addPcRow = new LinearLayout(this);
        addPcRow.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams apcLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        apcLp.setMargins(0, 0, 0, dp(12));
        addPcRow.setLayoutParams(apcLp);

        TextView btnAddPcFile = new TextView(this);
        btnAddPcFile.setText("📁 + Photocard (Files)");
        btnAddPcFile.setTextColor(Color.WHITE);
        btnAddPcFile.setTextSize(11.5f);
        btnAddPcFile.setGravity(Gravity.CENTER);
        btnAddPcFile.setPadding(dp(10), dp(8), dp(10), dp(8));
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
            startActivityForResult(Intent.createChooser(pick, "Pick Photocard Image"), REQ_PICK_EXTRA_PHOTOCARD);
        });
        addPcRow.addView(btnAddPcFile);

        TextView btnAddPcUrl = new TextView(this);
        btnAddPcUrl.setText("🔗 + Photocard (URL)");
        btnAddPcUrl.setTextColor(Color.parseColor("#00F0FF"));
        btnAddPcUrl.setTextSize(11.5f);
        btnAddPcUrl.setGravity(Gravity.CENTER);
        btnAddPcUrl.setPadding(dp(10), dp(8), dp(10), dp(8));
        btnAddPcUrl.setBackgroundResource(R.drawable.bg_btn_secondary);
        btnAddPcUrl.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        btnAddPcUrl.setOnClickListener(v -> {
            EditText urlIn = new EditText(this);
            urlIn.setHint("Paste Photocard image URL…");
            urlIn.setTextColor(Color.WHITE);
            new AlertDialog.Builder(this, AlertDialog.THEME_DEVICE_DEFAULT_DARK)
                    .setTitle("Add Photocard URL")
                    .setView(urlIn)
                    .setPositiveButton("Add", (d, w) -> {
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
                    .setNegativeButton("Cancel", null)
                    .show();
        });
        addPcRow.addView(btnAddPcUrl);
        root.addView(addPcRow);

        // Edit Idol & Share Idol Buttons
        LinearLayout bottomRow = new LinearLayout(this);
        bottomRow.setOrientation(LinearLayout.HORIZONTAL);

        TextView btnEdit = new TextView(this);
        btnEdit.setText("✏️ Edit Idol Data");
        btnEdit.setTextColor(Color.WHITE);
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
        btnShareOne.setText("📤 Share via WhatsApp");
        btnShareOne.setTextColor(Color.parseColor("#0B0E17"));
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
        builder.setNegativeButton("Close", null);
        dialogRef[0] = builder.create();
        dialogRef[0].show();
    }

    /**
     * Full Idol Card Creator / Editor Dialog (supports both local gallery files & pasted URLs).
     */
    public void showCreateOrEditIdolDialog(IdolCard existing, Runnable onSavedCallback) {
        final boolean isEditing = (existing != null);
        final IdolCard card = isEditing ? existing : new IdolCard();

        AlertDialog.Builder builder = new AlertDialog.Builder(this, AlertDialog.THEME_DEVICE_DEFAULT_DARK);
        builder.setTitle(getString(R.string.dialog_create_idol));

        ScrollView scroll = new ScrollView(this);
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(16);
        form.setPadding(pad, pad, pad, pad);
        form.setBackgroundColor(Color.parseColor("#131829"));

        ImageView ivPreview = new ImageView(this);
        LinearLayout.LayoutParams prevLp = new LinearLayout.LayoutParams(dp(120), dp(140));
        prevLp.gravity = Gravity.CENTER_HORIZONTAL;
        prevLp.setMargins(0, 0, 0, dp(10));
        ivPreview.setLayoutParams(prevLp);
        ivPreview.setScaleType(ImageView.ScaleType.CENTER_CROP);
        if (isEditing) {
            CardImageLoader.loadCardImage(this, card, ivPreview);
        }
        form.addView(ivPreview);

        EditText etStage = addFormInput(form, getString(R.string.hint_stage_name), card.getStageName());
        EditText etFull = addFormInput(form, getString(R.string.hint_full_name), card.getFullName());
        EditText etGroup = addFormInput(form, getString(R.string.hint_group_name), card.isSoloist() ? "" : card.getGroupName());

        CheckBox cbSoloist = new CheckBox(this);
        cbSoloist.setText(getString(R.string.check_is_soloist));
        cbSoloist.setTextColor(Color.WHITE);
        cbSoloist.setChecked(card.isSoloist());
        form.addView(cbSoloist);

        EditText etCountry = addFormInput(form, getString(R.string.hint_country), card.getCountry());
        EditText etBirth = addFormInput(form, getString(R.string.hint_birth_date), card.getBirthDate());
        EditText etRole = addFormInput(form, getString(R.string.hint_role), card.getRole());
        EditText etPhoto = addFormInput(form, getString(R.string.hint_photo_url), card.getPhotoUri());

        // Button to pick photo from device gallery/files
        TextView btnPickFile = new TextView(this);
        btnPickFile.setText(getString(R.string.btn_pick_photo_file));
        btnPickFile.setTextColor(Color.parseColor("#00F0FF"));
        btnPickFile.setTypeface(Typeface.DEFAULT_BOLD);
        btnPickFile.setGravity(Gravity.CENTER);
        btnPickFile.setPadding(dp(12), dp(9), dp(12), dp(9));
        btnPickFile.setBackgroundResource(R.drawable.bg_btn_secondary);
        LinearLayout.LayoutParams pfLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        pfLp.setMargins(0, 0, 0, dp(10));
        btnPickFile.setLayoutParams(pfLp);
        btnPickFile.setOnClickListener(v -> {
            pendingPhotoUrlField = etPhoto;
            pendingPhotoPreviewView = ivPreview;
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            startActivityForResult(Intent.createChooser(intent, "Select Idol Photo"), REQ_PICK_CARD_PHOTO);
        });
        form.addView(btnPickFile);

        EditText etCols = addFormInput(form, getString(R.string.hint_collections), card.getCollections());
        EditText etBio = addFormInput(form, getString(R.string.hint_bio), card.getBio());
        EditText etColor = addFormInput(form, "Card Accent Hex Color (e.g. #00F0FF, #7B61FF, #FF3366)", card.getAccentColor());

        scroll.addView(form);
        builder.setView(scroll);

        builder.setPositiveButton(getString(R.string.btn_save), (d, w) -> {
            String stage = etStage.getText().toString().trim();
            if (stage.isEmpty()) {
                Toast.makeText(this, "Stage name is required!", Toast.LENGTH_SHORT).show();
                return;
            }
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
            card.setAccentColor(etColor.getText().toString().trim());

            repo.saveOrUpdateIdol(card);
            if (onSavedCallback != null) onSavedCallback.run();
            Toast.makeText(this, "✅ Saved " + card.getStageName() + "!", Toast.LENGTH_SHORT).show();
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

    /**
     * Custom Match Builder Dialog (pick title, mode, and hand-pick the participating Idols).
     */
    private void showCreateCustomMatchDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, AlertDialog.THEME_DEVICE_DEFAULT_DARK);
        builder.setTitle("🎯 Build Custom Match / Friend Challenge");

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(14);
        root.setPadding(pad, pad, pad, pad);
        root.setBackgroundColor(Color.parseColor("#131829"));

        EditText etTitle = addFormInput(root, "Match Title (e.g. Agustín's Ultimate K-Pop Bias)", "");
        EditText etCreator = addFormInput(root, "Your Name (e.g. Agustín)", "Agustín");

        final String[] modeValues = {"BRACKET", "KING_OF_THE_HILL", "ROUND_ROBIN", "DOUBLE_ELIMINATION"};
        final String[] modeLabels = {
                getString(R.string.mode_bracket),
                getString(R.string.mode_koth),
                getString(R.string.mode_round_robin),
                getString(R.string.mode_double_elim)
        };
        Spinner spMode = createStyledSpinner(modeLabels);
        root.addView(spMode);

        EditText etFilter = addFormInput(root, "Search / Filter Idols to add…", "");

        final Set<String> chosenIds = new LinkedHashSet<>();
        TextView tvCount = new TextView(this);
        tvCount.setText("Selected Idols: 0 (Select at least 2)");
        tvCount.setTextColor(Color.parseColor("#00F0FF"));
        tvCount.setTypeface(Typeface.DEFAULT_BOLD);
        tvCount.setPadding(0, dp(4), 0, dp(6));
        root.addView(tvCount);

        ScrollView listScroll = new ScrollView(this);
        listScroll.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(250)));
        LinearLayout checkContainer = new LinearLayout(this);
        checkContainer.setOrientation(LinearLayout.VERTICAL);
        listScroll.addView(checkContainer);
        root.addView(listScroll);

        final List<IdolCard> allIdols = repo.getAllIdols();
        Runnable renderChecklist = () -> {
            checkContainer.removeAllViews();
            String q = etFilter.getText().toString().toLowerCase(Locale.ROOT).trim();
            for (IdolCard c : allIdols) {
                if (!q.isEmpty()
                        && !c.getStageName().toLowerCase(Locale.ROOT).contains(q)
                        && !c.getGroupName().toLowerCase(Locale.ROOT).contains(q)
                        && !c.getCollections().toLowerCase(Locale.ROOT).contains(q)) {
                    continue;
                }
                CheckBox cb = new CheckBox(this);
                cb.setText(c.getStageName() + " (" + c.getGroupName() + ") • " + c.getRole());
                cb.setTextColor(Color.WHITE);
                cb.setTextSize(12.5f);
                cb.setChecked(chosenIds.contains(c.getId()));
                cb.setOnCheckedChangeListener((btn, checked) -> {
                    if (checked) chosenIds.add(c.getId());
                    else chosenIds.remove(c.getId());
                    tvCount.setText("Selected Idols: " + chosenIds.size());
                });
                checkContainer.addView(cb);
            }
        };
        renderChecklist.run();

        etFilter.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { renderChecklist.run(); }
            @Override public void afterTextChanged(Editable s) {}
        });

        builder.setView(root);
        builder.setPositiveButton("💾 Save & Share", (d, w) -> {
            if (chosenIds.size() < 2) {
                Toast.makeText(this, "Pick at least 2 idols!", Toast.LENGTH_SHORT).show();
                return;
            }
            String title = etTitle.getText().toString().trim();
            if (title.isEmpty()) title = "Custom Bias Challenge (" + chosenIds.size() + ")";
            String creator = etCreator.getText().toString().trim();
            String mode = modeValues[spMode.getSelectedItemPosition()];

            CustomMatch cm = new CustomMatch(null, title, creator, mode, new ArrayList<>(chosenIds), System.currentTimeMillis());
            repo.saveOrUpdateCustomMatch(cm);
            refreshCurrentTab();

            List<IdolCard> cards = new ArrayList<>();
            for (String cid : chosenIds) {
                IdolCard c = repo.getIdolById(cid);
                if (c != null) cards.add(c);
            }
            showShareChoiceDialog(cm.getTitle(), cm, cards);
        });
        builder.setNeutralButton("▶ Play Now", (d, w) -> {
            if (chosenIds.size() < 2) {
                Toast.makeText(this, "Pick at least 2 idols!", Toast.LENGTH_SHORT).show();
                return;
            }
            String title = etTitle.getText().toString().trim();
            if (title.isEmpty()) title = "Custom Bias Challenge";
            String mode = modeValues[spMode.getSelectedItemPosition()];
            CustomMatch cm = new CustomMatch(null, title, etCreator.getText().toString().trim(), mode, new ArrayList<>(chosenIds), System.currentTimeMillis());
            repo.saveOrUpdateCustomMatch(cm);

            List<IdolCard> cards = new ArrayList<>();
            for (String cid : chosenIds) {
                IdolCard c = repo.getIdolById(cid);
                if (c != null) cards.add(c);
            }
            launchMatch(title, mode, cards);
        });
        builder.setNegativeButton(getString(R.string.btn_cancel), null);
        builder.show();
    }

    /**
     * Dialog letting the user choose between sharing a 1-tap .wyb File OR a WhatsApp Message with Link/Code.
     */
    private void showShareChoiceDialog(String title, CustomMatch match, List<IdolCard> cards) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, AlertDialog.THEME_DEVICE_DEFAULT_DARK);
        builder.setTitle(getString(R.string.share_mode_title));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(16);
        root.setPadding(pad, pad, pad, pad);
        root.setBackgroundColor(Color.parseColor("#131829"));

        TextView desc = new TextView(this);
        desc.setText(getString(R.string.share_mode_desc));
        desc.setTextColor(Color.parseColor("#94A3B8"));
        desc.setTextSize(13f);
        desc.setPadding(0, 0, 0, dp(12));
        root.addView(desc);

        AlertDialog[] ref = new AlertDialog[1];

        TextView btnFile = new TextView(this);
        btnFile.setText(getString(R.string.share_option_file));
        btnFile.setTextColor(Color.WHITE);
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
        btnCode.setTextColor(Color.parseColor("#00F0FF"));
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

    /**
     * Hub dialog to either pick a .wyb file from storage OR paste a WhatsApp message/code.
     */
    private void showImportHubDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, AlertDialog.THEME_DEVICE_DEFAULT_DARK);
        builder.setTitle("📥 Import Challenge or Idols Pack");

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(16);
        root.setPadding(pad, pad, pad, pad);
        root.setBackgroundColor(Color.parseColor("#131829"));

        AlertDialog[] ref = new AlertDialog[1];

        TextView btnPickWyb = new TextView(this);
        btnPickWyb.setText("📂 Open .wyb File from Device / Downloads");
        btnPickWyb.setTextColor(Color.WHITE);
        btnPickWyb.setTypeface(Typeface.DEFAULT_BOLD);
        btnPickWyb.setGravity(Gravity.CENTER);
        btnPickWyb.setPadding(dp(14), dp(12), dp(14), dp(12));
        btnPickWyb.setBackgroundResource(R.drawable.bg_btn_primary);
        LinearLayout.LayoutParams pLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        pLp.setMargins(0, 0, 0, dp(12));
        btnPickWyb.setLayoutParams(pLp);
        btnPickWyb.setOnClickListener(v -> {
            if (ref[0] != null) ref[0].dismiss();
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("*/*");
            startActivityForResult(Intent.createChooser(intent, "Select .wyb File"), REQ_PICK_WYB_FILE);
        });
        root.addView(btnPickWyb);

        EditText etCode = addFormInput(root, "Or paste WhatsApp message / WYB1-... code here:", "");

        // Auto-fill if clipboard contains WYB1-
        ClipboardManager cb = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (cb != null && cb.hasPrimaryClip() && cb.getPrimaryClip() != null && cb.getPrimaryClip().getItemCount() > 0) {
            CharSequence clipText = cb.getPrimaryClip().getItemAt(0).getText();
            if (clipText != null && clipText.toString().contains("WYB1-")) {
                etCode.setText(clipText.toString());
            }
        }

        builder.setView(root);
        builder.setPositiveButton("Preview & Import", (d, w) -> {
            String raw = etCode.getText().toString().trim();
            ShareImportManager.SharedPayload payload = ShareImportManager.decodeFromTextOrCode(this, raw);
            if (payload == null || payload.cards.isEmpty()) {
                Toast.makeText(this, "No valid WYB1 code found in text!", Toast.LENGTH_SHORT).show();
                return;
            }
            showSelectiveImportDialog(payload);
        });
        builder.setNegativeButton(getString(R.string.btn_cancel), null);
        ref[0] = builder.create();
        ref[0].show();
    }

    /**
     * Selective Import & Pre-Import Edit Dialog!
     * Lets the recipient choose which incoming Idols to import/save and edit any of them on the spot.
     */
    private void showSelectiveImportDialog(ShareImportManager.SharedPayload payload) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, AlertDialog.THEME_DEVICE_DEFAULT_DARK);
        builder.setTitle(getString(R.string.import_preview_title));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(14);
        root.setPadding(pad, pad, pad, pad);
        root.setBackgroundColor(Color.parseColor("#131829"));

        TextView tvPackName = new TextView(this);
        tvPackName.setText("📦 " + payload.packTitle + " (" + payload.cards.size() + " Idols)");
        tvPackName.setTextColor(Color.parseColor("#00F0FF"));
        tvPackName.setTextSize(15f);
        tvPackName.setTypeface(Typeface.DEFAULT_BOLD);
        root.addView(tvPackName);

        TextView tvDesc = new TextView(this);
        tvDesc.setText(getString(R.string.import_preview_desc));
        tvDesc.setTextColor(Color.parseColor("#94A3B8"));
        tvDesc.setTextSize(11.5f);
        tvDesc.setPadding(0, dp(4), 0, dp(8));
        root.addView(tvDesc);

        final Set<String> checkedCardIds = new LinkedHashSet<>();
        for (IdolCard c : payload.cards) checkedCardIds.add(c.getId());

        // Select All / Deselect All toggle row
        LinearLayout selRow = new LinearLayout(this);
        selRow.setOrientation(LinearLayout.HORIZONTAL);
        selRow.setPadding(0, 0, 0, dp(6));

        ScrollView listScroll = new ScrollView(this);
        listScroll.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(260)));
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
                    LinearLayout.LayoutParams ivLp = new LinearLayout.LayoutParams(dp(42), dp(50));
                    ivLp.setMargins(0, 0, dp(8), 0);
                    iv.setLayoutParams(ivLp);
                    iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
                    CardImageLoader.loadCardImage(MainActivity.this, inc, iv);
                    row.addView(iv);

                    boolean exists = (repo.getIdolById(inc.getId()) != null);
                    TextView tvInfo = new TextView(MainActivity.this);
                    tvInfo.setText(inc.getStageName() + " (" + inc.getGroupName() + ")\n"
                            + inc.getRole() + (exists ? " • [Update Existing]" : " • [✨ NEW]"));
                    tvInfo.setTextColor(Color.WHITE);
                    tvInfo.setTextSize(11.5f);
                    tvInfo.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
                    row.addView(tvInfo);

                    TextView btnEditInc = new TextView(MainActivity.this);
                    btnEditInc.setText("✏️ Edit");
                    btnEditInc.setTextColor(Color.parseColor("#FFD166"));
                    btnEditInc.setTextSize(11f);
                    btnEditInc.setTypeface(Typeface.DEFAULT_BOLD);
                    btnEditInc.setPadding(dp(8), dp(6), dp(8), dp(6));
                    btnEditInc.setBackgroundResource(R.drawable.bg_btn_secondary);
                    btnEditInc.setOnClickListener(v -> showCreateOrEditIdolDialog(inc, this));
                    row.addView(btnEditInc);

                    itemsContainer.addView(row);
                }
            }
        };
        rebuildImportRows.run();

        TextView btnAll = new TextView(this);
        btnAll.setText("☑ Select All");
        btnAll.setTextColor(Color.parseColor("#00F0FF"));
        btnAll.setTextSize(11.5f);
        btnAll.setPadding(0, 0, dp(16), 0);
        btnAll.setOnClickListener(v -> {
            for (IdolCard c : payload.cards) checkedCardIds.add(c.getId());
            rebuildImportRows.run();
        });
        selRow.addView(btnAll);

        TextView btnNone = new TextView(this);
        btnNone.setText("☐ Deselect All");
        btnNone.setTextColor(Color.parseColor("#94A3B8"));
        btnNone.setTextSize(11.5f);
        btnNone.setOnClickListener(v -> {
            checkedCardIds.clear();
            rebuildImportRows.run();
        });
        selRow.addView(btnNone);

        root.addView(selRow);
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
            if (payload.customMatch != null) {
                repo.saveOrUpdateCustomMatch(payload.customMatch);
            }
            refreshCurrentTab();
            Toast.makeText(this, "✅ Imported " + savedCount + " idols!", Toast.LENGTH_SHORT).show();
        });

        if (payload.customMatch != null || payload.cards.size() >= 2) {
            builder.setNeutralButton(getString(R.string.btn_play_imported_match), (d, w) -> {
                for (IdolCard c : payload.cards) {
                    if (checkedCardIds.contains(c.getId())) {
                        repo.saveOrUpdateIdol(c);
                    }
                }
                if (payload.customMatch != null) {
                    repo.saveOrUpdateCustomMatch(payload.customMatch);
                }
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

    /**
     * Mystery Photocard Gacha Pull Dialog!
     */
    private void showPhotocardGachaDialog() {
        List<IdolCard> all = repo.getAllIdols();
        if (all.isEmpty()) return;
        Random rnd = new Random();
        IdolCard pulled = all.get(rnd.nextInt(all.size()));
        String[] tiers = {"🌟 SSR HOLOGRAPHIC CONCEPT CARD", "✨ SSR LIMITED LUCKY DRAW", "💎 SR SPECIAL POB PHOTOCARD"};
        String tier = tiers[rnd.nextInt(tiers.length)];

        pulled.setFavorite(true);
        repo.saveOrUpdateIdol(pulled);

        AlertDialog.Builder builder = new AlertDialog.Builder(this, AlertDialog.THEME_DEVICE_DEFAULT_DARK);
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER_HORIZONTAL);
        int pad = dp(18);
        box.setPadding(pad, pad, pad, pad);
        box.setBackgroundColor(Color.parseColor("#14192D"));

        TextView tvTier = new TextView(this);
        tvTier.setText(tier);
        tvTier.setTextColor(Color.parseColor("#FFD166"));
        tvTier.setTextSize(13f);
        tvTier.setTypeface(Typeface.DEFAULT_BOLD);
        tvTier.setGravity(Gravity.CENTER);
        tvTier.setPadding(0, 0, 0, dp(10));
        box.addView(tvTier);

        ImageView iv = new ImageView(this);
        LinearLayout.LayoutParams ivLp = new LinearLayout.LayoutParams(dp(200), dp(240));
        ivLp.setMargins(0, 0, 0, dp(12));
        iv.setLayoutParams(ivLp);
        iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
        CardImageLoader.loadCardImage(this, pulled, iv);
        box.addView(iv);

        TextView tvName = new TextView(this);
        tvName.setText(pulled.getStageName() + " (" + pulled.getGroupName() + ")");
        tvName.setTextColor(Color.WHITE);
        tvName.setTextSize(21f);
        tvName.setTypeface(Typeface.DEFAULT_BOLD);
        box.addView(tvName);

        TextView tvSub = new TextView(this);
        tvSub.setText(pulled.getFullName() + " • " + pulled.getCountryWithFlag() + "\n" + pulled.getRole());
        tvSub.setTextColor(Color.parseColor("#00F0FF"));
        tvSub.setTextSize(12.5f);
        tvSub.setGravity(Gravity.CENTER);
        tvSub.setPadding(0, dp(4), 0, dp(8));
        box.addView(tvSub);

        builder.setView(box);
        builder.setPositiveButton("🔄 Pull Another!", (d, w) -> showPhotocardGachaDialog());
        builder.setNeutralButton("📋 View Idol Binder", (d, w) -> showIdolDetailAndPhotocardBinderDialog(pulled));
        builder.setNegativeButton("Awesome!", (d, w) -> refreshCurrentTab());
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
            if (pendingPhotoUrlField != null) {
                pendingPhotoUrlField.setText(savedPath);
            }
            if (pendingPhotoPreviewView != null) {
                pendingPhotoPreviewView.setImageURI(Uri.parse(savedPath));
            }
        } else if (requestCode == REQ_PICK_EXTRA_PHOTOCARD && pendingPhotocardTargetIdol != null) {
            String savedPath = CardImageLoader.copyUriToInternalStorage(this, uri);
            pendingPhotocardTargetIdol.addPhotocard(savedPath);
            pendingPhotocardTargetIdol.setPhotoUri(savedPath);
            repo.saveOrUpdateIdol(pendingPhotocardTargetIdol);
            if (pendingPhotocardRefreshCallback != null) {
                pendingPhotocardRefreshCallback.run();
            }
            Toast.makeText(this, "📸 Added Photocard to " + pendingPhotocardTargetIdol.getStageName() + "!", Toast.LENGTH_SHORT).show();
        } else if (requestCode == REQ_PICK_WYB_FILE) {
            ShareImportManager.SharedPayload payload = ShareImportManager.decodeFromUri(this, uri);
            if (payload != null && !payload.cards.isEmpty()) {
                showSelectiveImportDialog(payload);
            } else {
                Toast.makeText(this, "Could not parse .wyb file", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void launchMatch(String title, String gameMode, List<IdolCard> cards) {
        if (cards == null || cards.size() < 2) {
            Toast.makeText(this, "Select at least 2 cards to start a match!", Toast.LENGTH_SHORT).show();
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
            views.get(i).setTextColor(active ? Color.WHITE : Color.parseColor("#94A3B8"));
        }
    }

    private void addSmallLabel(LinearLayout parent, String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(Color.parseColor("#00F0FF"));
        tv.setTextSize(11.5f);
        tv.setTypeface(Typeface.DEFAULT_BOLD);
        tv.setPadding(0, dp(6), 0, dp(4));
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
        et.setTextColor(Color.WHITE);
        et.setHintTextColor(Color.parseColor("#64748B"));
        et.setTextSize(13f);
        et.setPadding(dp(12), dp(10), dp(12), dp(10));
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
