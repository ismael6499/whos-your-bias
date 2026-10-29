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
import com.nitsutech.whosyourbias.util.ThemeManager;

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
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER);
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
        lcLp.bottomMargin = dp(22);
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

        // App Title
        TextView tvTitle = new TextView(this);
        tvTitle.setText("Who's your Bias");
        tvTitle.setTextColor(theme.textColor);
        tvTitle.setTextSize(28f);
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
        LinearLayout.LayoutParams lineLp = new LinearLayout.LayoutParams(dp(68), dp(5));
        lineLp.topMargin = dp(8);
        lineLp.bottomMargin = dp(12);
        goldUnderline.setLayoutParams(lineLp);
        goldUnderline.setScaleX(0f);
        centerBox.addView(goldUnderline);

        // Lyric Subtitle
        TextView tvSub = new TextView(this);
        tvSub.setText("“Who is your bias? I am your bias, it's me!”");
        tvSub.setTextColor(theme.getMutedTextColor());
        tvSub.setTextSize(13.5f);
        tvSub.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        tvSub.setGravity(Gravity.CENTER);
        tvSub.setAlpha(0f);
        tvSub.setTranslationY(dp(16));
        centerBox.addView(tvSub);

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

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            Intent intent = new Intent(SplashActivity.this, MainActivity.class);
            if (getIntent() != null && getIntent().getData() != null) {
                intent.setAction(getIntent().getAction());
                intent.setData(getIntent().getData());
            }
            startActivity(intent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            finish();
        }, 1750);
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
