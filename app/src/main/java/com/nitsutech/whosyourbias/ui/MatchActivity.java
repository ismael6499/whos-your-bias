package com.nitsutech.whosyourbias.ui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.webkit.WebView;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.nitsutech.whosyourbias.R;
import com.nitsutech.whosyourbias.data.BiasRepository;
import com.nitsutech.whosyourbias.data.IdolCard;
import com.nitsutech.whosyourbias.game.TournamentEngine;
import com.nitsutech.whosyourbias.util.CardImageLoader;
import com.nitsutech.whosyourbias.util.LocaleManager;
import com.nitsutech.whosyourbias.util.ShareImportManager;
import com.nitsutech.whosyourbias.util.ThemeManager;

import java.util.ArrayList;
import java.util.List;

public class MatchActivity extends AppCompatActivity {
    public static final String EXTRA_TITLE = "extra_title";
    public static final String EXTRA_MODE = "extra_mode";
    public static final String EXTRA_CARD_IDS = "extra_card_ids";

    private BiasRepository repo;
    private TournamentEngine engine;
    private YoutubeBgmController bgmController;

    private TextView tvTournamentTitle;
    private TextView tvRoundBadge;
    private TextView btnUndoMatch;
    private ProgressBar progressBar;
    private View arenaDuelContainer;
    private FrameLayout cardSlotTop;
    private FrameLayout cardSlotBottom;
    private ScrollView victoryScrollContainer;
    private LinearLayout victoryContentBox;
    private ConfettiView confettiOverlay;

    private boolean isAnimatingPick = false;
    private int duelCount = 0;

    @Override
    protected void attachBaseContext(android.content.Context newBase) {
        super.attachBaseContext(LocaleManager.applyLocale(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_match);

        repo = BiasRepository.getInstance(this);

        tvTournamentTitle = findViewById(R.id.tvMatchTournamentTitle);
        tvRoundBadge = findViewById(R.id.tvMatchRoundBadge);
        progressBar = findViewById(R.id.matchProgressBar);
        arenaDuelContainer = findViewById(R.id.arenaDuelContainer);
        cardSlotTop = findViewById(R.id.cardSlotTop);
        cardSlotBottom = findViewById(R.id.cardSlotBottom);
        victoryScrollContainer = findViewById(R.id.victoryScrollContainer);
        victoryContentBox = findViewById(R.id.victoryContentBox);
        confettiOverlay = findViewById(R.id.confettiOverlay);

        findViewById(R.id.btnExitMatch).setOnClickListener(v -> finish());
        btnUndoMatch = findViewById(R.id.btnUndoMatch);
        if (btnUndoMatch != null) {
            btnUndoMatch.setOnClickListener(v -> {
                if (engine != null && engine.canUndo()) {
                    boolean undone = engine.undoLastPick();
                    if (undone) {
                        isAnimatingPick = false;
                        arenaDuelContainer.setVisibility(View.VISIBLE);
                        victoryScrollContainer.setVisibility(View.GONE);
                        renderCurrentDuel();
                        Toast.makeText(this, R.string.match_undone_toast, Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }

        WebView bgmWeb = findViewById(R.id.matchBgmWebView);
        View bgmVidBox = findViewById(R.id.matchBgmVideoContainer);
        TextView tvBgm = findViewById(R.id.tvMatchBgmTitle);
        TextView btnPlay = findViewById(R.id.btnMatchBgmPlayPause);
        TextView btnMute = findViewById(R.id.btnMatchBgmMute);
        TextView btnVid = findViewById(R.id.btnMatchBgmVideo);
        TextView btnSong = findViewById(R.id.btnMatchBgmSong);
        bgmController = new YoutubeBgmController(this, bgmWeb, bgmVidBox, tvBgm, btnPlay, btnMute, btnVid, btnSong);

        startTournamentFromIntent();
    }

    private void startTournamentFromIntent() {
        String title = getIntent().getStringExtra(EXTRA_TITLE);
        String mode = getIntent().getStringExtra(EXTRA_MODE);
        ArrayList<String> ids = getIntent().getStringArrayListExtra(EXTRA_CARD_IDS);

        List<IdolCard> roster = new ArrayList<>();
        if (ids != null) {
            for (String id : ids) {
                IdolCard c = repo.getIdolById(id);
                if (c != null) roster.add(c);
            }
        }
        if (roster.size() < 2) {
            roster = repo.getAllIdols();
            if (roster.size() > 16) roster = new ArrayList<>(roster.subList(0, 16));
        }

        tvTournamentTitle.setText(title != null ? title : getString(R.string.app_name));
        engine = new TournamentEngine(title, mode, roster);
        renderCurrentDuel();
    }

    private void renderCurrentDuel() {
        if (engine.isFinished()) {
            showVictoryScreen();
            return;
        }

        isAnimatingPick = false;
        arenaDuelContainer.setVisibility(View.VISIBLE);
        victoryScrollContainer.setVisibility(View.GONE);
        if (confettiOverlay != null) {
            confettiOverlay.stopConfetti();
        }

        if (btnUndoMatch != null) {
            btnUndoMatch.setVisibility(engine.canUndo() ? View.VISIBLE : View.GONE);
        }

        tvRoundBadge.setText(engine.getCurrentRoundTitle());
        progressBar.setProgress(engine.getProgressPercent());

        cardSlotTop.removeAllViews();
        cardSlotBottom.removeAllViews();

        cardSlotTop.addView(buildCleanPhotoDuelCard(engine.getCurrentLeft(), true));
        cardSlotBottom.addView(buildCleanPhotoDuelCard(engine.getCurrentRight(), false));
    }

    /**
     * Clean, spacious Vibrary-style full-photo Duel Card with Slide-to-3D-Rotate Info & Dedicated Pick Button.
     */
    private View buildCleanPhotoDuelCard(IdolCard card, boolean isTopSlot) {
        float density = getResources().getDisplayMetrics().density;
        ThemeManager.AppTheme theme = ThemeManager.getActiveTheme(this);

        FrameLayout wrapper = new FrameLayout(this);
        wrapper.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        wrapper.setCameraDistance(8500f * density);

        // ================= FRONT: CLEAN FULL-FIT UNCROPPED ROUNDED PHOTO CARD =================
        CardView frontCard = new CardView(this);
        frontCard.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        frontCard.setRadius(dp(24));
        frontCard.setCardElevation(dp(4));
        frontCard.setCardBackgroundColor(theme.surfaceColor);

        FrameLayout frontInner = new FrameLayout(this);
        frontInner.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        ImageView ivPhoto = new ImageView(this);
        ivPhoto.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        String initialPhotoUri = card.isUseRandomPhotocard() ? card.getRandomOrPrimaryPhotocardUri() : card.getPhotoUri();
        IdolCard displayCard = card;
        if (initialPhotoUri != null && !initialPhotoUri.equals(card.getPhotoUri())) {
            displayCard = new IdolCard(card.getId() + "_rnd", card.getStageName(), card.getFullName(),
                    card.getGroupName(), card.isSoloist(), card.getCountry(), card.getBirthDate(),
                    card.getRole(), initialPhotoUri, card.getAccentColor(), card.getBio(), card.getCollections());
        }
        CardImageLoader.loadCardImageWithSize(this, displayCard, ivPhoto, 480, 380);
        frontInner.addView(ivPhoto);

        // Soft bottom gradient overlay for crisp text readability
        View bottomScrim = new View(this);
        GradientDrawable scrimGd = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{Color.TRANSPARENT, Color.parseColor("#B0000000")});
        bottomScrim.setBackground(scrimGd);
        FrameLayout.LayoutParams scrimLp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(100), Gravity.BOTTOM);
        bottomScrim.setLayoutParams(scrimLp);
        frontInner.addView(bottomScrim);

        // Top-Left Photocard Switcher Pill (only shown when random or selected photocards mode is active)
        boolean isRandomOrSelected = card.isUseRandomPhotocard();
        if (isRandomOrSelected) {
            final int[] pcIndex = {0};
            List<String> allPhotos = new ArrayList<>();
            List<String> unlockedList = card.getUnlockedPhotocards();
            if (unlockedList != null && !unlockedList.isEmpty()) {
                allPhotos.addAll(unlockedList);
            } else {
                if (card.getPhotoUri() != null && !card.getPhotoUri().isEmpty()) {
                    allPhotos.add(card.getPhotoUri());
                }
                for (String p : card.getPhotocards()) {
                    if (!allPhotos.contains(p)) allPhotos.add(p);
                }
            }
            if (allPhotos.size() > 1) {
                // Find current photo's 1-based number in idol's full collection
                int currentNumber = 1;
                List<String> fullCollection = card.getPhotocards();
                for (int i = 0; i < fullCollection.size(); i++) {
                    if (IdolCard.isSamePhoto(fullCollection.get(i), initialPhotoUri)) {
                        currentNumber = i + 1;
                        break;
                    }
                }
                TextView btnCyclePc = new TextView(this);
                btnCyclePc.setText("📸 #" + currentNumber);
                btnCyclePc.setTextColor(Color.WHITE);
                btnCyclePc.setTextSize(11f);
                btnCyclePc.setTypeface(Typeface.DEFAULT_BOLD);
                GradientDrawable pillBg = new GradientDrawable();
                pillBg.setColor(Color.parseColor("#88111827"));
                pillBg.setCornerRadius(dp(16));
                btnCyclePc.setBackground(pillBg);
                btnCyclePc.setPadding(dp(10), dp(5), dp(10), dp(5));
                FrameLayout.LayoutParams pcLp = new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP | Gravity.START);
                pcLp.setMargins(dp(12), dp(12), 0, 0);
                btnCyclePc.setLayoutParams(pcLp);
                btnCyclePc.setOnClickListener(v -> {
                    pcIndex[0] = (pcIndex[0] + 1) % allPhotos.size();
                    String chosenUri = allPhotos.get(pcIndex[0]);
                    int numInColl = 1;
                    for (int i = 0; i < fullCollection.size(); i++) {
                        if (IdolCard.isSamePhoto(fullCollection.get(i), chosenUri)) {
                            numInColl = i + 1;
                            break;
                        }
                    }
                    btnCyclePc.setText("📸 #" + numInColl);
                    IdolCard temp = new IdolCard(card.getId() + "_pc" + pcIndex[0], card.getStageName(),
                            card.getFullName(), card.getGroupName(), card.isSoloist(), card.getCountry(),
                            card.getBirthDate(), card.getRole(), chosenUri, card.getAccentColor(), card.getBio(), card.getCollections());
                    CardImageLoader.loadCardImageWithSize(this, temp, ivPhoto, 480, 380);
                });
                frontInner.addView(btnCyclePc);
            }
        }

        // Top-Right 3D Info Flip Pill: full hint on first round, minimalist icon thereafter
        TextView btnFlipInfo = new TextView(this);
        if (duelCount == 0) {
            btnFlipInfo.setText("🔄 " + getString(R.string.tap_or_slide_hint));
        } else {
            btnFlipInfo.setText("🔄");
        }
        btnFlipInfo.setTextColor(Color.parseColor("#111827"));
        btnFlipInfo.setTextSize(10.5f);
        btnFlipInfo.setTypeface(Typeface.DEFAULT_BOLD);
        GradientDrawable infoPillBg = new GradientDrawable();
        infoPillBg.setColor(Color.parseColor("#EEFFFFFF"));
        infoPillBg.setCornerRadius(dp(18));
        btnFlipInfo.setBackground(infoPillBg);
        btnFlipInfo.setPadding(dp(10), dp(5), dp(10), dp(5));
        FrameLayout.LayoutParams flipLp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP | Gravity.END);
        flipLp.setMargins(0, dp(12), dp(12), 0);
        btnFlipInfo.setLayoutParams(flipLp);
        frontInner.addView(btnFlipInfo);

        // Bottom Name & Group Overlay
        LinearLayout bottomInfo = new LinearLayout(this);
        bottomInfo.setOrientation(LinearLayout.HORIZONTAL);
        bottomInfo.setGravity(Gravity.BOTTOM | Gravity.CENTER_VERTICAL);
        bottomInfo.setPadding(dp(16), dp(8), dp(16), dp(14));
        FrameLayout.LayoutParams biLp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM);
        bottomInfo.setLayoutParams(biLp);

        LinearLayout nameCol = new LinearLayout(this);
        nameCol.setOrientation(LinearLayout.VERTICAL);
        nameCol.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView tvGroupTag = new TextView(this);
        tvGroupTag.setText((card.isSoloist() ? "SOLOIST" : card.getGroupName()) + "  •  " + card.getCountryWithFlag());
        tvGroupTag.setTextColor(theme.secondaryColor);
        tvGroupTag.setTextSize(11.5f);
        tvGroupTag.setTypeface(Typeface.DEFAULT_BOLD);
        nameCol.addView(tvGroupTag);

        TextView tvStageName = new TextView(this);
        tvStageName.setText(card.getStageName());
        tvStageName.setTextColor(Color.WHITE);
        tvStageName.setTextSize(23f);
        tvStageName.setTypeface(Typeface.create("sans-serif-black", Typeface.BOLD));
        tvStageName.setSingleLine(true);
        nameCol.addView(tvStageName);

        bottomInfo.addView(nameCol);

        // Dedicated Pick Pill Button on bottom right: full text on first duel, minimalist thereafter
        TextView tvPickBadge = new TextView(this);
        if (duelCount == 0) {
            tvPickBadge.setText("⚡ " + getString(R.string.btn_pick_this_bias));
        } else {
            tvPickBadge.setText("⚡");
        }
        tvPickBadge.setGravity(Gravity.CENTER);
        tvPickBadge.setTextColor(theme.getButtonTextColor(theme.primaryColor));
        tvPickBadge.setTextSize(13f);
        tvPickBadge.setTypeface(Typeface.DEFAULT_BOLD);
        tvPickBadge.setBackground(ThemeManager.createPillDrawable(this, theme.primaryColor, 20));
        tvPickBadge.setPadding(dp(18), dp(10), dp(18), dp(10));
        bottomInfo.addView(tvPickBadge);

        frontInner.addView(bottomInfo);
        frontCard.addView(frontInner);

        // ================= BACK: CLEAN 3D DOSSIER CARD =================
        CardView backCard = new CardView(this);
        backCard.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        backCard.setRadius(dp(24));
        backCard.setCardElevation(dp(4));
        backCard.setCardBackgroundColor(theme.surfaceColor);
        backCard.setVisibility(View.GONE);

        LinearLayout backInner = new LinearLayout(this);
        backInner.setOrientation(LinearLayout.VERTICAL);
        backInner.setPadding(dp(18), dp(14), dp(18), dp(14));

        LinearLayout backTopRow = new LinearLayout(this);
        backTopRow.setOrientation(LinearLayout.HORIZONTAL);
        backTopRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView tvBackTitle = new TextView(this);
        tvBackTitle.setText(card.getStageName() + " (" + card.getFullName() + ")");
        tvBackTitle.setTextColor(theme.textColor);
        tvBackTitle.setTextSize(17f);
        tvBackTitle.setTypeface(Typeface.DEFAULT_BOLD);
        tvBackTitle.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        backTopRow.addView(tvBackTitle);

        TextView btnFancam = new TextView(this);
        btnFancam.setText("🎬 Fancam");
        btnFancam.setGravity(Gravity.CENTER);
        btnFancam.setTextColor(theme.getButtonTextColor(theme.secondaryColor));
        btnFancam.setTextSize(11f);
        btnFancam.setTypeface(Typeface.DEFAULT_BOLD);
        btnFancam.setBackground(ThemeManager.createPillDrawable(this, theme.secondaryColor, 16));
        btnFancam.setPadding(dp(12), dp(6), dp(12), dp(6));
        btnFancam.setOnClickListener(v -> openYoutubeFancamSearch(card));
        backTopRow.addView(btnFancam);
        backInner.addView(backTopRow);

        ScrollView backScroll = new ScrollView(this);
        backScroll.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        LinearLayout backDetails = new LinearLayout(this);
        backDetails.setOrientation(LinearLayout.VERTICAL);
        backDetails.setPadding(0, dp(8), 0, dp(8));

        addCleanInfoRow(backDetails, "Grupo / Artista", card.isSoloist() ? "Solista" : card.getGroupName());
        if (card.getNativeName() != null && !card.getNativeName().isEmpty()) {
            addCleanInfoRow(backDetails, "Nombre Nativo", card.getNativeName());
        }
        addCleanInfoRow(backDetails, "Origen y Edad", card.getCountryWithFlag() + " • " + card.getCalculatedAge() + " años (" + card.getZodiacSign() + ")");
        addCleanInfoRow(backDetails, "Posición / Rol", card.getRole());

        List<IdolCard> groupMates = repo.getFellowGroupMembers(card);
        if (!groupMates.isEmpty()) {
            StringBuilder matesSb = new StringBuilder();
            for (int i = 0; i < groupMates.size(); i++) {
                if (i > 0) matesSb.append(", ");
                matesSb.append(groupMates.get(i).getStageName());
            }
            addCleanInfoRow(backDetails, "Compañeros en " + card.getGroupName(), matesSb.toString());
        }

        addCleanInfoRow(backDetails, "Descripción", card.getBio());
        backScroll.addView(backDetails);
        backInner.addView(backScroll);

        LinearLayout backActions = new LinearLayout(this);
        backActions.setOrientation(LinearLayout.HORIZONTAL);

        TextView btnFlipBack = new TextView(this);
        btnFlipBack.setText("🔄 Ver Foto");
        btnFlipBack.setTextColor(Color.parseColor("#111827"));
        btnFlipBack.setTypeface(Typeface.DEFAULT_BOLD);
        btnFlipBack.setGravity(Gravity.CENTER);
        btnFlipBack.setBackgroundResource(R.drawable.bg_btn_secondary);
        btnFlipBack.setPadding(dp(14), dp(10), dp(14), dp(10));
        LinearLayout.LayoutParams fbLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        fbLp.setMargins(0, 0, dp(8), 0);
        btnFlipBack.setLayoutParams(fbLp);
        backActions.addView(btnFlipBack);

        TextView btnPickBack = new TextView(this);
        btnPickBack.setText("✓ Elegir a " + card.getStageName());
        btnPickBack.setTextColor(theme.getButtonTextColor(theme.primaryColor));
        btnPickBack.setTypeface(Typeface.DEFAULT_BOLD);
        btnPickBack.setGravity(Gravity.CENTER);
        btnPickBack.setBackground(ThemeManager.createPillDrawable(this, theme.primaryColor, 20));
        btnPickBack.setPadding(dp(14), dp(10), dp(14), dp(10));
        btnPickBack.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        backActions.addView(btnPickBack);

        backInner.addView(backActions);
        backCard.addView(backInner);

        wrapper.addView(frontCard);
        wrapper.addView(backCard);

        btnFlipInfo.setOnClickListener(v -> flipCard3D(wrapper, frontCard, backCard));
        btnFlipBack.setOnClickListener(v -> flipCard3D(wrapper, backCard, frontCard));

        View.OnClickListener pickAction = v -> onCardPicked(wrapper, isTopSlot);
        tvPickBadge.setOnClickListener(pickAction);
        btnPickBack.setOnClickListener(pickAction);

        // Slide finger on card rotates the card in 3D to see info.
        // A simple tap on the front card chooses that card as the winning pick!
        attachCardGestures(frontInner, () -> flipCard3D(wrapper, frontCard, backCard), () -> onCardPicked(wrapper, isTopSlot));
        attachCardGestures(backCard, () -> flipCard3D(wrapper, backCard, frontCard), null);
        attachCardGestures(backInner, () -> flipCard3D(wrapper, backCard, frontCard), null);
        attachCardGestures(backScroll, () -> flipCard3D(wrapper, backCard, frontCard), null);
        attachCardGestures(backDetails, () -> flipCard3D(wrapper, backCard, frontCard), null);

        return wrapper;
    }

    private void attachCardGestures(View targetView, Runnable onSlideFlip, Runnable onTapPick) {
        final float[] downXY = new float[2];
        final boolean[] isSlide = new boolean[1];
        final long[] downTime = new long[1];
        targetView.setOnTouchListener((v, ev) -> {
            switch (ev.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    downXY[0] = ev.getRawX();
                    downXY[1] = ev.getRawY();
                    downTime[0] = System.currentTimeMillis();
                    isSlide[0] = false;
                    return true;
                case MotionEvent.ACTION_MOVE:
                    if (!isSlide[0]) {
                        float dx = Math.abs(ev.getRawX() - downXY[0]);
                        float dy = Math.abs(ev.getRawY() - downXY[1]);
                        if (dx > dp(20) || dy > dp(20)) {
                            isSlide[0] = true;
                            if (onSlideFlip != null) onSlideFlip.run();
                        }
                    }
                    return true;
                case MotionEvent.ACTION_UP:
                    if (!isSlide[0]) {
                        long elapsed = System.currentTimeMillis() - downTime[0];
                        if (elapsed < 600 && onTapPick != null) {
                            onTapPick.run();
                        }
                    }
                    return true;
                case MotionEvent.ACTION_CANCEL:
                    return true;
            }
            return false;
        });
    }

    private void addCleanInfoRow(LinearLayout parent, String label, String value) {
        ThemeManager.AppTheme theme = ThemeManager.getActiveTheme(this);
        TextView tv = new TextView(this);
        tv.setText(label + ": " + value);
        tv.setTextColor(theme.textColor);
        tv.setTextSize(12.5f);
        tv.setPadding(0, dp(3), 0, dp(3));
        parent.addView(tv);
    }

    private void flipCard3D(View container, View visibleView, View hiddenView) {
        ObjectAnimator outAnim = ObjectAnimator.ofFloat(container, "rotationY", 0f, 90f);
        outAnim.setDuration(160);
        outAnim.setInterpolator(new AccelerateInterpolator());
        outAnim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                visibleView.setVisibility(View.GONE);
                hiddenView.setVisibility(View.VISIBLE);
                container.setRotationY(-90f);
                ObjectAnimator inAnim = ObjectAnimator.ofFloat(container, "rotationY", -90f, 0f);
                inAnim.setDuration(160);
                inAnim.setInterpolator(new DecelerateInterpolator());
                inAnim.start();
            }
        });
        outAnim.start();
    }

    private void onCardPicked(View pickedView, boolean pickedTopLeft) {
        if (isAnimatingPick || engine.isFinished()) return;
        isAnimatingPick = true;

        IdolCard w = pickedTopLeft ? engine.getCurrentLeft() : engine.getCurrentRight();
        IdolCard l = pickedTopLeft ? engine.getCurrentRight() : engine.getCurrentLeft();
        if (w != null && l != null) {
            repo.recordMatchOutcome(w.getId(), l.getId());
        }

        pickedView.animate()
                .scaleX(1.03f)
                .scaleY(1.03f)
                .setDuration(120)
                .withEndAction(() -> pickedView.animate()
                        .scaleX(1.0f)
                        .scaleY(1.0f)
                        .setDuration(90)
                        .withEndAction(() -> {
                            duelCount++;
                            engine.pickWinner(pickedTopLeft);
                            renderCurrentDuel();
                        }).start())
                .start();
    }

    private void showVictoryScreen() {
        ThemeManager.AppTheme theme = ThemeManager.getActiveTheme(this);
        arenaDuelContainer.setVisibility(View.GONE);
        victoryScrollContainer.setVisibility(View.VISIBLE);
        if (btnUndoMatch != null) {
            btnUndoMatch.setVisibility(engine.canUndo() ? View.VISIBLE : View.GONE);
        }
        victoryContentBox.removeAllViews();
        progressBar.setProgress(100);
        tvRoundBadge.setText("👑 BIAS SUPREMO");

        IdolCard champ = engine.getChampion();
        if (champ == null) {
            finish();
            return;
        }
        repo.recordTournamentChampion(champ.getId());
        confettiOverlay.burstConfetti();

        TextView tvSlogan = new TextView(this);
        tvSlogan.setText("“Who is your bias? I am your bias, it's me!”");
        tvSlogan.setTextColor(theme.getMutedTextColor());
        tvSlogan.setTextSize(13f);
        tvSlogan.setGravity(Gravity.CENTER);
        tvSlogan.setPadding(0, dp(4), 0, dp(12));
        victoryContentBox.addView(tvSlogan);

        // Large Clean Vibrary-style Champion Card
        CardView champCard = new CardView(this);
        champCard.setRadius(dp(26));
        champCard.setCardElevation(dp(4));
        champCard.setCardBackgroundColor(theme.surfaceColor);
        LinearLayout.LayoutParams ccLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        ccLp.setMargins(0, 0, 0, dp(16));
        champCard.setLayoutParams(ccLp);

        LinearLayout champInner = new LinearLayout(this);
        champInner.setOrientation(LinearLayout.VERTICAL);
        champInner.setGravity(Gravity.CENTER_HORIZONTAL);
        champInner.setPadding(dp(18), dp(18), dp(18), dp(20));

        CardView photoCard = new CardView(this);
        photoCard.setRadius(dp(22));
        photoCard.setCardElevation(0f);
        LinearLayout.LayoutParams pcLp = new LinearLayout.LayoutParams(dp(230), dp(270));
        pcLp.setMargins(0, 0, 0, dp(14));
        photoCard.setLayoutParams(pcLp);

        ImageView ivChamp = new ImageView(this);
        ivChamp.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        CardImageLoader.loadCardImageWithSize(this, champ, ivChamp, 400, 470);
        photoCard.addView(ivChamp);
        champInner.addView(photoCard);

        TextView tvCrownTag = new TextView(this);
        tvCrownTag.setText("👑 BIAS GANADOR • " + (champ.isSoloist() ? "SOLISTA" : champ.getGroupName().toUpperCase()));
        tvCrownTag.setGravity(Gravity.CENTER);
        tvCrownTag.setTextColor(theme.getButtonTextColor(theme.primaryColor));
        tvCrownTag.setTextSize(11.5f);
        tvCrownTag.setTypeface(Typeface.DEFAULT_BOLD);
        tvCrownTag.setBackground(ThemeManager.createPillDrawable(this, theme.primaryColor, 18));
        tvCrownTag.setPadding(dp(14), dp(6), dp(14), dp(6));
        champInner.addView(tvCrownTag);

        TextView tvChampName = new TextView(this);
        tvChampName.setText(champ.getStageName());
        tvChampName.setTextColor(theme.textColor);
        tvChampName.setTextSize(28f);
        tvChampName.setTypeface(Typeface.create("sans-serif-black", Typeface.BOLD));
        tvChampName.setPadding(0, dp(6), 0, dp(2));
        champInner.addView(tvChampName);

        TextView tvChampMeta = new TextView(this);
        tvChampMeta.setText(champ.getFullName() + " • " + champ.getCountryWithFlag() + " • " + champ.getCalculatedAge() + " años");
        tvChampMeta.setTextColor(theme.getMutedTextColor());
        tvChampMeta.setTextSize(13f);
        tvChampMeta.setGravity(Gravity.CENTER);
        champInner.addView(tvChampMeta);

        champCard.addView(champInner);
        victoryContentBox.addView(champCard);

        // Clean Action Buttons
        TextView btnSharePoster = createActionButton(getString(R.string.btn_share_victory_poster), R.drawable.bg_btn_primary, theme.getButtonTextColor(theme.primaryColor));
        btnSharePoster.setBackground(ThemeManager.createPillDrawable(this, theme.primaryColor, 20));
        btnSharePoster.setPadding(dp(16), dp(13), dp(16), dp(13));
        btnSharePoster.setOnClickListener(v -> ShareImportManager.shareVictoryPoster(
                this, champ, engine.getRunnerUp(), engine.getSemiFinalists(),
                engine.getTournamentTitle(), engine.getGameMode()));
        victoryContentBox.addView(btnSharePoster);

        LinearLayout rowRematch = new LinearLayout(this);
        rowRematch.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams rrLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rrLp.setMargins(0, 0, 0, dp(16));
        rowRematch.setLayoutParams(rrLp);

        TextView btnRematch = createActionButton(getString(R.string.btn_rematch_same), R.drawable.bg_btn_secondary, Color.parseColor("#111827"));
        LinearLayout.LayoutParams rmLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        rmLp.setMargins(0, 0, dp(6), 0);
        btnRematch.setLayoutParams(rmLp);
        btnRematch.setOnClickListener(v -> {
            duelCount = 0;
            engine = new TournamentEngine(engine.getTournamentTitle(), engine.getGameMode(), engine.getInitialRoster());
            renderCurrentDuel();
        });
        rowRematch.addView(btnRematch);

        TextView btnHome = createActionButton(getString(R.string.btn_finish_home), R.drawable.bg_btn_secondary, Color.parseColor("#111827"));
        LinearLayout.LayoutParams hmLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        hmLp.setMargins(dp(6), 0, 0, 0);
        btnHome.setLayoutParams(hmLp);
        btnHome.setOnClickListener(v -> finish());
        rowRematch.addView(btnHome);

        victoryContentBox.addView(rowRematch);

        // Podium Summary Box (1° Puesto, 2° Puesto, 3° Puesto)
        LinearLayout podiumBox = new LinearLayout(this);
        podiumBox.setOrientation(LinearLayout.VERTICAL);
        podiumBox.setBackground(ThemeManager.createCardDrawable(this, theme.surfaceColor, theme.primaryColor, 20));
        podiumBox.setPadding(dp(16), dp(16), dp(16), dp(16));
        LinearLayout.LayoutParams podLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        podLp.setMargins(0, 0, 0, dp(14));
        podiumBox.setLayoutParams(podLp);

        TextView tvPodTitle = new TextView(this);
        tvPodTitle.setText(getString(R.string.victory_podium_title));
        tvPodTitle.setTextColor(theme.textColor);
        tvPodTitle.setTextSize(15f);
        tvPodTitle.setTypeface(Typeface.DEFAULT_BOLD);
        tvPodTitle.setPadding(0, 0, 0, dp(6));
        podiumBox.addView(tvPodTitle);

        addCleanInfoRow(podiumBox, "🥇 1° Puesto", champ.getStageName() + " (" + champ.getGroupName() + ")");
        if (engine.getRunnerUp() != null) {
            IdolCard ru = engine.getRunnerUp();
            addCleanInfoRow(podiumBox, "🥈 2° Puesto", ru.getStageName() + " (" + ru.getGroupName() + ")");
        }
        if (!engine.getSemiFinalists().isEmpty()) {
            StringBuilder sfSb = new StringBuilder();
            for (int i = 0; i < engine.getSemiFinalists().size(); i++) {
                if (i > 0) sfSb.append(" • ");
                IdolCard sf = engine.getSemiFinalists().get(i);
                sfSb.append(sf.getStageName()).append(" (").append(sf.getGroupName()).append(")");
            }
            addCleanInfoRow(podiumBox, "🥉 3° Puesto", sfSb.toString());
        }
        victoryContentBox.addView(podiumBox);
    }

    private TextView createActionButton(String text, int bgRes, int textColor) {
        TextView btn = new TextView(this);
        btn.setText(text);
        btn.setTextColor(textColor);
        btn.setTypeface(Typeface.DEFAULT_BOLD);
        btn.setTextSize(13.5f);
        btn.setGravity(Gravity.CENTER);
        btn.setBackgroundResource(bgRes);
        btn.setPadding(dp(16), dp(13), dp(16), dp(13));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, dp(10));
        btn.setLayoutParams(lp);
        return btn;
    }

    private void openYoutubeFancamSearch(IdolCard card) {
        try {
            String query = card.getStageName() + " " + (card.isSoloist() ? "" : card.getGroupName()) + " fancam 4K";
            Uri uri = Uri.parse("https://www.youtube.com/results?search_query=" + Uri.encode(query.trim()));
            startActivity(new Intent(Intent.ACTION_VIEW, uri));
        } catch (Exception e) {
            Toast.makeText(this, "Could not open YouTube", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (bgmController != null) bgmController.onAppBackground();
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
