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
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.webkit.WebView;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.nitsutech.whosyourbias.R;
import com.nitsutech.whosyourbias.data.BiasRepository;
import com.nitsutech.whosyourbias.data.IdolCard;
import com.nitsutech.whosyourbias.game.TournamentEngine;
import com.nitsutech.whosyourbias.util.CardImageLoader;
import com.nitsutech.whosyourbias.util.ShareImportManager;

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
    private ProgressBar progressBar;
    private View arenaDuelContainer;
    private FrameLayout cardSlotTop;
    private FrameLayout cardSlotBottom;
    private ScrollView victoryScrollContainer;
    private LinearLayout victoryContentBox;
    private ConfettiView confettiOverlay;

    private boolean isAnimatingPick = false;
    private boolean lightstickActive = false;

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

        TextView btnLightstick = findViewById(R.id.btnLightstickCheer);
        btnLightstick.setOnClickListener(v -> {
            lightstickActive = !lightstickActive;
            confettiOverlay.burstConfetti();
            if (lightstickActive) {
                IdolCard left = engine != null ? engine.getCurrentLeft() : null;
                int c = left != null ? CardImageLoader.parseColorSafe(left.getAccentColor(), Color.parseColor("#00F0FF")) : Color.parseColor("#00F0FF");
                GradientDrawable gd = new GradientDrawable();
                gd.setColor(Color.parseColor("#0B0E17"));
                gd.setStroke(dp(3), c);
                arenaDuelContainer.setBackground(gd);
                Toast.makeText(this, "🪄 Lightstick Glow Activated!", Toast.LENGTH_SHORT).show();
            } else {
                arenaDuelContainer.setBackgroundColor(Color.TRANSPARENT);
            }
        });

        // Initialize YouTube BGM Controller in Arena
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

        tvRoundBadge.setText(engine.getCurrentRoundTitle());
        progressBar.setProgress(engine.getProgressPercent());

        IdolCard left = engine.getCurrentLeft();
        IdolCard right = engine.getCurrentRight();

        cardSlotTop.removeAllViews();
        cardSlotBottom.removeAllViews();

        cardSlotTop.addView(buildInteractiveDuelCardView(left, true));
        cardSlotBottom.addView(buildInteractiveDuelCardView(right, false));
    }

    private View buildInteractiveDuelCardView(IdolCard card, boolean isTopSlot) {
        float density = getResources().getDisplayMetrics().density;
        FrameLayout wrapper = new FrameLayout(this);
        wrapper.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        wrapper.setCameraDistance(8000f * density);

        int accent = CardImageLoader.parseColorSafe(card.getAccentColor(), Color.parseColor("#7B61FF"));

        // ================= FRONT OF CARD =================
        LinearLayout frontView = new LinearLayout(this);
        frontView.setOrientation(LinearLayout.HORIZONTAL);
        frontView.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        frontView.setPadding(dp(12), dp(10), dp(12), dp(10));
        frontView.setGravity(Gravity.CENTER_VERTICAL);

        GradientDrawable cardBg = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{Color.parseColor("#181F36"), Color.parseColor("#121728"), Color.parseColor("#1E1836")});
        cardBg.setCornerRadius(dp(20));
        cardBg.setStroke(dp(2), accent);
        frontView.setBackground(cardBg);

        // Idol Portrait Frame (Left side of card)
        FrameLayout photoFrame = new FrameLayout(this);
        LinearLayout.LayoutParams photoLp = new LinearLayout.LayoutParams(dp(132), ViewGroup.LayoutParams.MATCH_PARENT);
        photoLp.setMargins(0, 0, dp(12), 0);
        photoFrame.setLayoutParams(photoLp);

        ImageView ivPhoto = new ImageView(this);
        ivPhoto.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        ivPhoto.setScaleType(ImageView.ScaleType.CENTER_CROP);
        CardImageLoader.loadCardImage(this, card, ivPhoto);
        photoFrame.addView(ivPhoto);

        // Photocard Switcher Badge (cycles through idol's photocards + stylized art)
        final int[] pcIndex = {0};
        List<String> allPhotos = new ArrayList<>();
        if (card.getPhotoUri() != null && !card.getPhotoUri().isEmpty()) {
            allPhotos.add(card.getPhotoUri());
        }
        for (String p : card.getPhotocards()) {
            if (!allPhotos.contains(p)) allPhotos.add(p);
        }
        allPhotos.add("STYLIZED_ART"); // Always allow toggling to holographic card art too

        TextView btnCyclePc = new TextView(this);
        btnCyclePc.setText("📸 PC 1/" + allPhotos.size());
        btnCyclePc.setTextColor(Color.WHITE);
        btnCyclePc.setTextSize(9.5f);
        btnCyclePc.setPadding(dp(6), dp(3), dp(6), dp(3));
        btnCyclePc.setBackgroundColor(Color.parseColor("#AA0B0E17"));
        FrameLayout.LayoutParams pcBadgeLp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM | Gravity.START);
        pcBadgeLp.setMargins(dp(4), dp(4), dp(4), dp(4));
        btnCyclePc.setLayoutParams(pcBadgeLp);
        btnCyclePc.setOnClickListener(v -> {
            pcIndex[0] = (pcIndex[0] + 1) % allPhotos.size();
            btnCyclePc.setText("📸 PC " + (pcIndex[0] + 1) + "/" + allPhotos.size());
            String chosenUri = allPhotos.get(pcIndex[0]);
            if ("STYLIZED_ART".equals(chosenUri)) {
                ivPhoto.setImageBitmap(CardImageLoader.createStylizedPortrait(card, 480, 600));
            } else {
                IdolCard temp = new IdolCard(card.getId() + "_pc" + pcIndex[0], card.getStageName(),
                        card.getFullName(), card.getGroupName(), card.isSoloist(), card.getCountry(),
                        card.getBirthDate(), card.getRole(), chosenUri, card.getAccentColor(), card.getBio(), card.getCollections());
                CardImageLoader.loadCardImage(this, temp, ivPhoto);
            }
        });
        photoFrame.addView(btnCyclePc);

        frontView.addView(photoFrame);

        // Info & Actions Column (Right side of card)
        LinearLayout infoCol = new LinearLayout(this);
        infoCol.setOrientation(LinearLayout.VERTICAL);
        infoCol.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f));
        infoCol.setGravity(Gravity.CENTER_VERTICAL);

        // Group & Country Row
        LinearLayout badgeRow = new LinearLayout(this);
        badgeRow.setOrientation(LinearLayout.HORIZONTAL);
        badgeRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView tvGroup = new TextView(this);
        tvGroup.setText(card.isSoloist() ? "★ SOLOIST" : "✦ " + card.getGroupName());
        tvGroup.setTextColor(accent);
        tvGroup.setTextSize(11f);
        tvGroup.setTypeface(Typeface.DEFAULT_BOLD);
        badgeRow.addView(tvGroup);

        TextView tvAge = new TextView(this);
        tvAge.setText("  •  " + card.getCalculatedAge() + "y " + card.getZodiacSign());
        tvAge.setTextColor(Color.parseColor("#94A3B8"));
        tvAge.setTextSize(10.5f);
        badgeRow.addView(tvAge);
        infoCol.addView(badgeRow);

        // Stage Name
        TextView tvStage = new TextView(this);
        tvStage.setText(card.getStageName());
        tvStage.setTextColor(Color.WHITE);
        tvStage.setTextSize(22f);
        tvStage.setTypeface(Typeface.DEFAULT_BOLD);
        tvStage.setSingleLine(true);
        tvStage.setEllipsize(TextUtils.TruncateAt.END);
        infoCol.addView(tvStage);

        // Full Real Name + Country
        TextView tvFull = new TextView(this);
        tvFull.setText(card.getFullName() + " • " + card.getCountryWithFlag());
        tvFull.setTextColor(Color.parseColor("#CBD5E1"));
        tvFull.setTextSize(11.5f);
        tvFull.setSingleLine(true);
        tvFull.setEllipsize(TextUtils.TruncateAt.END);
        infoCol.addView(tvFull);

        // Role / Position
        TextView tvRole = new TextView(this);
        tvRole.setText("🎤 " + card.getRole());
        tvRole.setTextColor(Color.parseColor("#00F0FF"));
        tvRole.setTextSize(11f);
        tvRole.setSingleLine(true);
        tvRole.setEllipsize(TextUtils.TruncateAt.END);
        tvRole.setPadding(0, dp(2), 0, dp(6));
        infoCol.addView(tvRole);

        // Short Bio preview
        TextView tvBioShort = new TextView(this);
        tvBioShort.setText(card.getBio());
        tvBioShort.setTextColor(Color.parseColor("#94A3B8"));
        tvBioShort.setTextSize(10.5f);
        tvBioShort.setMaxLines(2);
        tvBioShort.setEllipsize(TextUtils.TruncateAt.END);
        infoCol.addView(tvBioShort);

        // Action Buttons Row (Flip 3D + Pick Bias)
        LinearLayout actionsRow = new LinearLayout(this);
        actionsRow.setOrientation(LinearLayout.HORIZONTAL);
        actionsRow.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams actLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        actLp.setMargins(0, dp(8), 0, 0);
        actionsRow.setLayoutParams(actLp);

        TextView btnFlipToBack = new TextView(this);
        btnFlipToBack.setText(getString(R.string.btn_flip_card));
        btnFlipToBack.setTextColor(Color.WHITE);
        btnFlipToBack.setTextSize(11f);
        btnFlipToBack.setGravity(Gravity.CENTER);
        btnFlipToBack.setPadding(dp(10), dp(8), dp(10), dp(8));
        btnFlipToBack.setBackgroundResource(R.drawable.bg_btn_secondary);
        LinearLayout.LayoutParams flipLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        flipLp.setMargins(0, 0, dp(8), 0);
        btnFlipToBack.setLayoutParams(flipLp);
        actionsRow.addView(btnFlipToBack);

        TextView btnPickFront = new TextView(this);
        btnPickFront.setText(getString(R.string.btn_pick_this_bias));
        btnPickFront.setTextColor(Color.WHITE);
        btnPickFront.setTypeface(Typeface.DEFAULT_BOLD);
        btnPickFront.setTextSize(12f);
        btnPickFront.setGravity(Gravity.CENTER);
        btnPickFront.setPadding(dp(12), dp(8), dp(12), dp(8));
        btnPickFront.setBackgroundResource(R.drawable.bg_btn_primary);
        btnPickFront.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        actionsRow.addView(btnPickFront);

        infoCol.addView(actionsRow);
        frontView.addView(infoCol);

        // ================= BACK OF CARD (3D FLIP BIO & GROUP MATES) =================
        LinearLayout backView = new LinearLayout(this);
        backView.setOrientation(LinearLayout.VERTICAL);
        backView.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        backView.setPadding(dp(14), dp(10), dp(14), dp(10));
        backView.setVisibility(View.GONE);

        GradientDrawable backBg = new GradientDrawable(
                GradientDrawable.Orientation.BL_TR,
                new int[]{Color.parseColor("#1D1838"), Color.parseColor("#11172A"), Color.parseColor("#162838")});
        backBg.setCornerRadius(dp(20));
        backBg.setStroke(dp(2), Color.parseColor("#00F0FF"));
        backView.setBackground(backBg);

        // Header on Back
        LinearLayout backHeader = new LinearLayout(this);
        backHeader.setOrientation(LinearLayout.HORIZONTAL);
        backHeader.setGravity(Gravity.CENTER_VERTICAL);

        TextView tvBackTitle = new TextView(this);
        tvBackTitle.setText("📋 " + card.getStageName() + " (" + card.getFullName() + ")");
        tvBackTitle.setTextColor(Color.parseColor("#00F0FF"));
        tvBackTitle.setTextSize(14f);
        tvBackTitle.setTypeface(Typeface.DEFAULT_BOLD);
        tvBackTitle.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        backHeader.addView(tvBackTitle);

        TextView btnFancam = new TextView(this);
        btnFancam.setText("🎬 Fancam");
        btnFancam.setTextColor(Color.parseColor("#FFD166"));
        btnFancam.setTextSize(10.5f);
        btnFancam.setPadding(dp(8), dp(4), dp(8), dp(4));
        btnFancam.setBackgroundResource(R.drawable.bg_chip_tag);
        btnFancam.setOnClickListener(v -> openYoutubeFancamSearch(card));
        backHeader.addView(btnFancam);
        backView.addView(backHeader);

        // Scrollable details inside Back View
        ScrollView backScroll = new ScrollView(this);
        backScroll.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        LinearLayout backDetails = new LinearLayout(this);
        backDetails.setOrientation(LinearLayout.VERTICAL);
        backDetails.setPadding(0, dp(4), 0, dp(4));

        addDetailLine(backDetails, "🎂 Birthdate & Zodiac:", card.getBirthDate() + " (" + card.getCalculatedAge() + " yrs • " + card.getZodiacSign() + ")");
        addDetailLine(backDetails, "🌍 Origin & Group:", card.getCountryWithFlag() + " • " + (card.isSoloist() ? "Soloist" : card.getGroupName()));
        addDetailLine(backDetails, "🎤 Role / Position:", card.getRole());

        // Automatic Group Member Linking!
        List<IdolCard> groupMates = repo.getFellowGroupMembers(card);
        if (!groupMates.isEmpty()) {
            StringBuilder matesSb = new StringBuilder();
            for (int i = 0; i < groupMates.size(); i++) {
                if (i > 0) matesSb.append(", ");
                matesSb.append(groupMates.get(i).getStageName());
            }
            addDetailLine(backDetails, "🤝 Fellow " + card.getGroupName() + " Members (" + groupMates.size() + "):", matesSb.toString());
        }

        addDetailLine(backDetails, "🏆 Career Record:", card.getTournamentsWon() + " Crowns • " + card.getMatchesWon() + "W / " + card.getMatchesPlayed() + "M (" + card.getWinRatePercent() + "%)");
        addDetailLine(backDetails, "✨ Profile:", card.getBio());

        backScroll.addView(backDetails);
        backView.addView(backScroll);

        // Bottom Buttons on Back View
        LinearLayout backBtns = new LinearLayout(this);
        backBtns.setOrientation(LinearLayout.HORIZONTAL);

        TextView btnFlipToFront = new TextView(this);
        btnFlipToFront.setText("🔄 Front");
        btnFlipToFront.setTextColor(Color.WHITE);
        btnFlipToFront.setTextSize(11f);
        btnFlipToFront.setGravity(Gravity.CENTER);
        btnFlipToFront.setPadding(dp(12), dp(7), dp(12), dp(7));
        btnFlipToFront.setBackgroundResource(R.drawable.bg_btn_secondary);
        LinearLayout.LayoutParams f2fLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        f2fLp.setMargins(0, 0, dp(8), 0);
        btnFlipToFront.setLayoutParams(f2fLp);
        backBtns.addView(btnFlipToFront);

        TextView btnPickBack = new TextView(this);
        btnPickBack.setText(getString(R.string.btn_pick_this_bias));
        btnPickBack.setTextColor(Color.WHITE);
        btnPickBack.setTypeface(Typeface.DEFAULT_BOLD);
        btnPickBack.setTextSize(12f);
        btnPickBack.setGravity(Gravity.CENTER);
        btnPickBack.setPadding(dp(12), dp(7), dp(12), dp(7));
        btnPickBack.setBackgroundResource(R.drawable.bg_btn_primary);
        btnPickBack.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        backBtns.addView(btnPickBack);

        backView.addView(backBtns);

        wrapper.addView(frontView);
        wrapper.addView(backView);

        // Wire 3D Flip Animation
        btnFlipToBack.setOnClickListener(v -> flipCard3D(wrapper, frontView, backView));
        btnFlipToFront.setOnClickListener(v -> flipCard3D(wrapper, backView, frontView));

        // Wire Pick Bias Action
        View.OnClickListener pickAction = v -> onCardPicked(wrapper, isTopSlot);
        btnPickFront.setOnClickListener(pickAction);
        btnPickBack.setOnClickListener(pickAction);
        frontView.setOnClickListener(pickAction);

        return wrapper;
    }

    private void addDetailLine(LinearLayout parent, String label, String value) {
        TextView tv = new TextView(this);
        tv.setText(label + " " + value);
        tv.setTextColor(Color.parseColor("#E2E8F0"));
        tv.setTextSize(11f);
        tv.setPadding(0, dp(2), 0, dp(2));
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
                .scaleX(1.04f)
                .scaleY(1.04f)
                .setDuration(130)
                .withEndAction(() -> pickedView.animate()
                        .scaleX(1.0f)
                        .scaleY(1.0f)
                        .setDuration(100)
                        .withEndAction(() -> {
                            engine.pickWinner(pickedTopLeft);
                            renderCurrentDuel();
                        }).start())
                .start();
    }

    private void showVictoryScreen() {
        arenaDuelContainer.setVisibility(View.GONE);
        victoryScrollContainer.setVisibility(View.VISIBLE);
        victoryContentBox.removeAllViews();
        progressBar.setProgress(100);
        tvRoundBadge.setText(getString(R.string.victory_champion_badge));

        IdolCard champ = engine.getChampion();
        if (champ == null) {
            finish();
            return;
        }
        repo.recordTournamentChampion(champ.getId());
        confettiOverlay.burstConfetti();

        int accent = CardImageLoader.parseColorSafe(champ.getAccentColor(), Color.parseColor("#FFD166"));

        // Slogan Banner ("WHO IS YOUR BIAS? I AM YOUR BIAS, IT'S ME!")
        TextView tvSlogan = new TextView(this);
        tvSlogan.setText(getString(R.string.victory_slogan));
        tvSlogan.setTextColor(Color.parseColor("#00F0FF"));
        tvSlogan.setTextSize(13f);
        tvSlogan.setTypeface(Typeface.DEFAULT_BOLD);
        tvSlogan.setGravity(Gravity.CENTER);
        tvSlogan.setPadding(0, dp(4), 0, dp(4));
        victoryContentBox.addView(tvSlogan);

        TextView tvCrownHeader = new TextView(this);
        tvCrownHeader.setText(getString(R.string.victory_champion_badge));
        tvCrownHeader.setTextColor(Color.parseColor("#FFD166"));
        tvCrownHeader.setTextSize(19f);
        tvCrownHeader.setTypeface(Typeface.DEFAULT_BOLD);
        tvCrownHeader.setGravity(Gravity.CENTER);
        tvCrownHeader.setPadding(0, 0, 0, dp(12));
        victoryContentBox.addView(tvCrownHeader);

        // Golden Champion Card
        LinearLayout champCard = new LinearLayout(this);
        champCard.setOrientation(LinearLayout.VERTICAL);
        champCard.setGravity(Gravity.CENTER_HORIZONTAL);
        champCard.setPadding(dp(18), dp(18), dp(18), dp(18));
        GradientDrawable goldCardBg = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{Color.parseColor("#241F3D"), Color.parseColor("#151A2E"), Color.parseColor("#292118")});
        goldCardBg.setCornerRadius(dp(24));
        goldCardBg.setStroke(dp(3), Color.parseColor("#FFD166"));
        champCard.setBackground(goldCardBg);
        LinearLayout.LayoutParams ccLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        ccLp.setMargins(0, 0, 0, dp(14));
        champCard.setLayoutParams(ccLp);

        ImageView ivChamp = new ImageView(this);
        LinearLayout.LayoutParams ivLp = new LinearLayout.LayoutParams(dp(200), dp(230));
        ivLp.setMargins(0, 0, 0, dp(12));
        ivChamp.setLayoutParams(ivLp);
        ivChamp.setScaleType(ImageView.ScaleType.CENTER_CROP);
        CardImageLoader.loadCardImage(this, champ, ivChamp);
        champCard.addView(ivChamp);

        TextView tvChampName = new TextView(this);
        tvChampName.setText("👑 " + champ.getStageName());
        tvChampName.setTextColor(Color.WHITE);
        tvChampName.setTextSize(26f);
        tvChampName.setTypeface(Typeface.DEFAULT_BOLD);
        champCard.addView(tvChampName);

        TextView tvChampGroup = new TextView(this);
        tvChampGroup.setText((champ.isSoloist() ? "Soloist" : champ.getGroupName()) + "  •  " + champ.getCountryWithFlag());
        tvChampGroup.setTextColor(accent);
        tvChampGroup.setTextSize(15f);
        tvChampGroup.setTypeface(Typeface.DEFAULT_BOLD);
        champCard.addView(tvChampGroup);

        TextView tvChampMeta = new TextView(this);
        tvChampMeta.setText(champ.getFullName() + " • Age " + champ.getCalculatedAge() + " (" + champ.getZodiacSign() + ")\n🎤 " + champ.getRole());
        tvChampMeta.setTextColor(Color.parseColor("#CBD5E1"));
        tvChampMeta.setTextSize(12.5f);
        tvChampMeta.setGravity(Gravity.CENTER);
        tvChampMeta.setPadding(0, dp(6), 0, dp(8));
        champCard.addView(tvChampMeta);

        // Cosmic Bias Chemistry Badge
        int chemistry = 88 + Math.abs((champ.getStageName().hashCode() + champ.getBirthDate().hashCode()) % 12);
        TextView tvChem = new TextView(this);
        tvChem.setText("✨ Bias Chemistry Score: " + chemistry + "% Soulmate Match  •  🏆 Total Wins: " + champ.getTournamentsWon());
        tvChem.setTextColor(Color.parseColor("#FFD166"));
        tvChem.setTextSize(11.5f);
        tvChem.setTypeface(Typeface.DEFAULT_BOLD);
        tvChem.setPadding(dp(12), dp(6), dp(12), dp(6));
        tvChem.setBackgroundResource(R.drawable.bg_chip_tag);
        champCard.addView(tvChem);

        victoryContentBox.addView(champCard);

        // Action Buttons (Share Poster, Watch Fancam, Rematch, Home)
        TextView btnSharePoster = createActionButton(getString(R.string.btn_share_victory_poster), R.drawable.bg_btn_gold, Color.parseColor("#0B0E17"));
        btnSharePoster.setOnClickListener(v -> ShareImportManager.shareVictoryPoster(
                this, champ, engine.getRunnerUp(), engine.getSemiFinalists(),
                engine.getTournamentTitle(), engine.getGameMode()));
        victoryContentBox.addView(btnSharePoster);

        TextView btnWatchFancam = createActionButton("🎬 Watch " + champ.getStageName() + " Fancam / MV on YouTube", R.drawable.bg_btn_secondary, Color.parseColor("#00F0FF"));
        btnWatchFancam.setOnClickListener(v -> openYoutubeFancamSearch(champ));
        victoryContentBox.addView(btnWatchFancam);

        LinearLayout rowRematch = new LinearLayout(this);
        rowRematch.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams rrLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rrLp.setMargins(0, 0, 0, dp(14));
        rowRematch.setLayoutParams(rrLp);

        TextView btnRematch = createActionButton(getString(R.string.btn_rematch_same), R.drawable.bg_btn_primary, Color.WHITE);
        LinearLayout.LayoutParams rmLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        rmLp.setMargins(0, 0, dp(6), 0);
        btnRematch.setLayoutParams(rmLp);
        btnRematch.setOnClickListener(v -> {
            engine = new TournamentEngine(engine.getTournamentTitle(), engine.getGameMode(), engine.getInitialRoster());
            renderCurrentDuel();
        });
        rowRematch.addView(btnRematch);

        TextView btnHome = createActionButton(getString(R.string.btn_finish_home), R.drawable.bg_btn_secondary, Color.WHITE);
        LinearLayout.LayoutParams hmLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        hmLp.setMargins(dp(6), 0, 0, 0);
        btnHome.setLayoutParams(hmLp);
        btnHome.setOnClickListener(v -> finish());
        rowRematch.addView(btnHome);

        victoryContentBox.addView(rowRematch);

        // Podium Box (2nd Place + Semifinalists)
        LinearLayout podiumBox = new LinearLayout(this);
        podiumBox.setOrientation(LinearLayout.VERTICAL);
        podiumBox.setPadding(dp(14), dp(14), dp(14), dp(14));
        podiumBox.setBackgroundResource(R.drawable.bg_glass_card);
        LinearLayout.LayoutParams podLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        podLp.setMargins(0, 0, 0, dp(14));
        podiumBox.setLayoutParams(podLp);

        TextView tvPodTitle = new TextView(this);
        tvPodTitle.setText(getString(R.string.victory_podium_title));
        tvPodTitle.setTextColor(Color.parseColor("#FFD166"));
        tvPodTitle.setTextSize(14f);
        tvPodTitle.setTypeface(Typeface.DEFAULT_BOLD);
        tvPodTitle.setPadding(0, 0, 0, dp(6));
        podiumBox.addView(tvPodTitle);

        addDetailLine(podiumBox, "🥇 1st Place (Bias):", champ.getStageName() + " (" + champ.getGroupName() + ")");
        if (engine.getRunnerUp() != null) {
            IdolCard ru = engine.getRunnerUp();
            addDetailLine(podiumBox, "🥈 2nd Place (Runner-Up):", ru.getStageName() + " (" + ru.getGroupName() + ")");
        }
        if (!engine.getSemiFinalists().isEmpty()) {
            StringBuilder sfSb = new StringBuilder();
            for (int i = 0; i < engine.getSemiFinalists().size(); i++) {
                if (i > 0) sfSb.append(", ");
                IdolCard sf = engine.getSemiFinalists().get(i);
                sfSb.append(sf.getStageName()).append(" (").append(sf.getGroupName()).append(")");
            }
            addDetailLine(podiumBox, "🏅 Top 4 Semifinalists:", sfSb.toString());
        }
        victoryContentBox.addView(podiumBox);

        // Championship Path (Complete Match History)
        LinearLayout pathBox = new LinearLayout(this);
        pathBox.setOrientation(LinearLayout.VERTICAL);
        pathBox.setPadding(dp(14), dp(14), dp(14), dp(14));
        pathBox.setBackgroundResource(R.drawable.bg_glass_card);
        pathBox.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView tvPathTitle = new TextView(this);
        tvPathTitle.setText(getString(R.string.victory_path_title));
        tvPathTitle.setTextColor(Color.parseColor("#00F0FF"));
        tvPathTitle.setTextSize(14f);
        tvPathTitle.setTypeface(Typeface.DEFAULT_BOLD);
        tvPathTitle.setPadding(0, 0, 0, dp(6));
        pathBox.addView(tvPathTitle);

        List<TournamentEngine.MatchHistoryItem> hist = engine.getHistory();
        for (int i = hist.size() - 1; i >= 0; i--) {
            TournamentEngine.MatchHistoryItem item = hist.get(i);
            TextView tvRow = new TextView(this);
            tvRow.setText("• " + item.roundLabel + "\n   ✅ " + item.winner.getStageName() + " (" + item.winner.getGroupName()
                    + ")  def.  ❌ " + item.loser.getStageName() + " (" + item.loser.getGroupName() + ")");
            tvRow.setTextColor(Color.parseColor("#CBD5E1"));
            tvRow.setTextSize(11.5f);
            tvRow.setPadding(0, dp(4), 0, dp(4));
            pathBox.addView(tvRow);
        }
        victoryContentBox.addView(pathBox);
    }

    private TextView createActionButton(String text, int bgRes, int textColor) {
        TextView btn = new TextView(this);
        btn.setText(text);
        btn.setTextColor(textColor);
        btn.setTypeface(Typeface.DEFAULT_BOLD);
        btn.setTextSize(13f);
        btn.setGravity(Gravity.CENTER);
        btn.setPadding(dp(14), dp(12), dp(14), dp(12));
        btn.setBackgroundResource(bgRes);
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
    protected void onDestroy() {
        if (bgmController != null) bgmController.destroy();
        super.onDestroy();
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
