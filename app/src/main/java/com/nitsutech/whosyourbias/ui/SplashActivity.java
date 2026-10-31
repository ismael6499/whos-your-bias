package com.nitsutech.whosyourbias.ui;

import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.nitsutech.whosyourbias.R;
import com.nitsutech.whosyourbias.data.BiasRepository;
import com.nitsutech.whosyourbias.data.IdolCard;
import com.nitsutech.whosyourbias.util.CardImageLoader;
import com.nitsutech.whosyourbias.util.ThemeManager;

import java.util.List;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ThemeManager.AppTheme theme = ThemeManager.getActiveTheme(this);

        // Soft Pastel Gradient Splash Screen
        FrameLayout root = new FrameLayout(this);
        GradientDrawable splashBg = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{theme.backgroundColor, theme.surfaceColor, Color.parseColor("#F3E8FF")}
        );
        root.setBackground(splashBg);

        // Soft decorative pastel circles in background
        View haloOuter = new View(this);
        GradientDrawable haloDrawable = new GradientDrawable();
        haloDrawable.setShape(GradientDrawable.OVAL);
        haloDrawable.setColor(Color.parseColor("#66FFE4EF"));
        haloOuter.setBackground(haloDrawable);
        FrameLayout.LayoutParams haloLp = new FrameLayout.LayoutParams(dp(230), dp(230), Gravity.CENTER);
        haloLp.bottomMargin = dp(30);
        haloOuter.setLayoutParams(haloLp);
        haloOuter.setScaleX(0.2f);
        haloOuter.setScaleY(0.2f);
        haloOuter.setAlpha(0f);
        root.addView(haloOuter);

        LinearLayout centerBox = new LinearLayout(this);
        centerBox.setOrientation(LinearLayout.VERTICAL);
        centerBox.setGravity(Gravity.CENTER);
        FrameLayout.LayoutParams cbLp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER);
        cbLp.leftMargin = dp(24);
        cbLp.rightMargin = dp(24);
        centerBox.setLayoutParams(cbLp);

        // Rounded Squircle Pastel Icon Container
        FrameLayout logoCard = new FrameLayout(this);
        GradientDrawable cardBg = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{Color.parseColor("#FFF5F8"), Color.parseColor("#FFE4EF"), Color.parseColor("#F3E8FF")}
        );
        cardBg.setCornerRadius(dp(36));
        cardBg.setStroke(dp(3), theme.primaryColor);
        logoCard.setBackground(cardBg);
        LinearLayout.LayoutParams lcLp = new LinearLayout.LayoutParams(dp(120), dp(120));
        lcLp.bottomMargin = dp(20);
        logoCard.setLayoutParams(lcLp);
        logoCard.setElevation(dp(8));

        ImageView ivLogo = new ImageView(this);
        ivLogo.setImageResource(R.drawable.ic_launcher_foreground);
        FrameLayout.LayoutParams ivLp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, Gravity.CENTER);
        ivLogo.setLayoutParams(ivLp);
        logoCard.addView(ivLogo);

        logoCard.setScaleX(0f);
        logoCard.setScaleY(0f);
        logoCard.setRotation(-18f);
        centerBox.addView(logoCard);

        // App Title: "Who's your Bias" with "Bias" highlighted in theme color
        TextView tvTitle = new TextView(this);
        SpannableStringBuilder titleSsb = new SpannableStringBuilder("Who's your Bias");
        titleSsb.setSpan(new ForegroundColorSpan(theme.primaryColor), 11, 15, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        tvTitle.setText(titleSsb);
        tvTitle.setTextColor(theme.textColor);
        tvTitle.setTextSize(28f);
        tvTitle.setSingleLine(true);
        tvTitle.setMaxLines(1);
        tvTitle.setTypeface(Typeface.create("sans-serif-black", Typeface.BOLD));
        tvTitle.setGravity(Gravity.CENTER);
        tvTitle.setAlpha(0f);
        tvTitle.setTranslationY(dp(20));
        centerBox.addView(tvTitle);

        // Pastel Accent Bar under Title
        View goldUnderline = new View(this);
        GradientDrawable lineGd = new GradientDrawable();
        lineGd.setColor(theme.primaryColor);
        lineGd.setCornerRadius(dp(4));
        goldUnderline.setBackground(lineGd);
        LinearLayout.LayoutParams lineLp = new LinearLayout.LayoutParams(dp(54), dp(4));
        lineLp.topMargin = dp(8);
        lineLp.bottomMargin = dp(10);
        goldUnderline.setLayoutParams(lineLp);
        goldUnderline.setScaleX(0f);
        centerBox.addView(goldUnderline);

        // Tagline Subtitle: "K-Pop Bias Tournament & Photocards"
        TextView tvSub = new TextView(this);
        tvSub.setText(getString(R.string.splash_subtitle));
        tvSub.setTextColor(theme.getMutedTextColor());
        tvSub.setTextSize(13.5f);
        tvSub.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        tvSub.setGravity(Gravity.CENTER);
        tvSub.setAlpha(0f);
        tvSub.setTranslationY(dp(16));
        centerBox.addView(tvSub);

        // First-time Preload Progress Box (shown when downloading initial idol gallery)
        LinearLayout preloadBox = new LinearLayout(this);
        preloadBox.setOrientation(LinearLayout.VERTICAL);
        preloadBox.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams pbBoxLp = new LinearLayout.LayoutParams(dp(260), ViewGroup.LayoutParams.WRAP_CONTENT);
        pbBoxLp.topMargin = dp(24);
        preloadBox.setLayoutParams(pbBoxLp);
        preloadBox.setVisibility(View.GONE);

        TextView tvPreloadStatus = new TextView(this);
        tvPreloadStatus.setText(getString(R.string.splash_preloading_title));
        tvPreloadStatus.setTextColor(theme.primaryColor);
        tvPreloadStatus.setTextSize(12.5f);
        tvPreloadStatus.setTypeface(Typeface.DEFAULT_BOLD);
        tvPreloadStatus.setGravity(Gravity.CENTER);
        preloadBox.addView(tvPreloadStatus);

        FrameLayout barTrack = new FrameLayout(this);
        GradientDrawable trackBg = new GradientDrawable();
        trackBg.setColor(Color.parseColor("#E5E7EB"));
        trackBg.setCornerRadius(dp(8));
        barTrack.setBackground(trackBg);
        LinearLayout.LayoutParams trLp = new LinearLayout.LayoutParams(dp(230), dp(10));
        trLp.topMargin = dp(8);
        trLp.bottomMargin = dp(6);
        barTrack.setLayoutParams(trLp);

        View barFill = new View(this);
        GradientDrawable fillBg = new GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{theme.primaryColor, Color.parseColor("#A855F7")}
        );
        fillBg.setCornerRadius(dp(8));
        barFill.setBackground(fillBg);
        FrameLayout.LayoutParams fillLp = new FrameLayout.LayoutParams(dp(8), ViewGroup.LayoutParams.MATCH_PARENT);
        barFill.setLayoutParams(fillLp);
        barTrack.addView(barFill);
        preloadBox.addView(barTrack);

        TextView tvPreloadCount = new TextView(this);
        tvPreloadCount.setText(getString(R.string.splash_preloading_progress, 0, 287, 0));
        tvPreloadCount.setTextColor(theme.getMutedTextColor());
        tvPreloadCount.setTextSize(11.5f);
        tvPreloadCount.setGravity(Gravity.CENTER);
        preloadBox.addView(tvPreloadCount);

        centerBox.addView(preloadBox);

        root.addView(centerBox);

        // Footer Studio Brand
        TextView tvFooter = new TextView(this);
        tvFooter.setText("✨ K-POP PHOTOCARD & BIAS ARENA ✨");
        tvFooter.setTextColor(theme.getMutedTextColor());
        tvFooter.setTextSize(11f);
        tvFooter.setLetterSpacing(0.10f);
        tvFooter.setGravity(Gravity.CENTER);
        FrameLayout.LayoutParams ftLp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        ftLp.bottomMargin = dp(32);
        tvFooter.setLayoutParams(ftLp);
        tvFooter.setAlpha(0f);
        root.addView(tvFooter);

        setContentView(root);

        // Run Spring & Fade Animations
        haloOuter.animate()
                .scaleX(1.35f)
                .scaleY(1.35f)
                .alpha(1f)
                .setDuration(900)
                .setInterpolator(new DecelerateInterpolator())
                .start();

        logoCard.animate()
                .scaleX(1f)
                .scaleY(1f)
                .rotation(0f)
                .setDuration(750)
                .setInterpolator(new OvershootInterpolator(1.6f))
                .start();

        ObjectAnimator pulse = ObjectAnimator.ofPropertyValuesHolder(
                ivLogo,
                PropertyValuesHolder.ofFloat("scaleX", 1f, 1.12f, 1f),
                PropertyValuesHolder.ofFloat("scaleY", 1f, 1.12f, 1f)
        );
        pulse.setStartDelay(650);
        pulse.setDuration(700);
        pulse.start();

        tvTitle.animate()
                .alpha(1f)
                .translationY(0f)
                .setStartDelay(280)
                .setDuration(500)
                .setInterpolator(new DecelerateInterpolator())
                .start();

        goldUnderline.animate()
                .scaleX(1f)
                .setStartDelay(450)
                .setDuration(450)
                .setInterpolator(new OvershootInterpolator())
                .start();

        tvSub.animate()
                .alpha(1f)
                .translationY(0f)
                .setStartDelay(520)
                .setDuration(500)
                .start();

        tvFooter.animate()
                .alpha(1f)
                .setStartDelay(650)
                .setDuration(500)
                .start();

        Runnable goNext = () -> {
            if (isFinishing()) return;
            Intent intent = new Intent(SplashActivity.this, MainActivity.class);
            if (getIntent() != null && getIntent().getData() != null) {
                intent.setAction(getIntent().getAction());
                intent.setData(getIntent().getData());
            }
            startActivity(intent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            finish();
        };

        List<IdolCard> allIdols = BiasRepository.getInstance(this).getAllIdols();
        if (CardImageLoader.needsInitialPreload(this, allIdols)) {
            preloadBox.setVisibility(View.VISIBLE);
            preloadBox.setAlpha(0f);
            preloadBox.animate().alpha(1f).setStartDelay(550).setDuration(400).start();

            ObjectAnimator loopPulse = ObjectAnimator.ofPropertyValuesHolder(
                    logoCard,
                    PropertyValuesHolder.ofFloat("scaleX", 1f, 1.06f, 1f),
                    PropertyValuesHolder.ofFloat("scaleY", 1f, 1.06f, 1f)
            );
            loopPulse.setDuration(1100);
            loopPulse.setRepeatCount(ObjectAnimator.INFINITE);
            loopPulse.setStartDelay(850);
            loopPulse.start();

            final int maxTrackWidth = dp(230);
            CardImageLoader.preloadAllInitialImages(this, allIdols, new CardImageLoader.PreloadProgressListener() {
                @Override
                public void onProgress(int loaded, int total) {
                    int pct = total > 0 ? Math.min(100, (loaded * 100) / total) : 100;
                    tvPreloadCount.setText(getString(R.string.splash_preloading_progress, loaded, total, pct));
                    FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) barFill.getLayoutParams();
                    lp.width = Math.max(dp(10), (maxTrackWidth * pct) / 100);
                    barFill.setLayoutParams(lp);
                }

                @Override
                public void onComplete() {
                    tvPreloadStatus.setText(getString(R.string.splash_preloading_ready));
                    new Handler(Looper.getMainLooper()).postDelayed(goNext, 280);
                }
            });
        } else {
            new Handler(Looper.getMainLooper()).postDelayed(goNext, 1350);
        }
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
