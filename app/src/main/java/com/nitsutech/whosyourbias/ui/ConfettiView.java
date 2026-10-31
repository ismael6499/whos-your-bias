package com.nitsutech.whosyourbias.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ConfettiView extends View {

    private static class Particle {
        float x, y, vx, vy, size, angle, vAngle;
        int color;
        boolean isCircle;
    }

    private final List<Particle> particles = new ArrayList<>();
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Random random = new Random();
    private boolean active = false;

    private static final int[] COLORS = new int[]{
            Color.parseColor("#FFD166"),
            Color.parseColor("#00F0FF"),
            Color.parseColor("#7B61FF"),
            Color.parseColor("#FF3366"),
            Color.parseColor("#06D6A0"),
            Color.parseColor("#F472B6")
    };

    public ConfettiView(Context context) {
        super(context);
    }

    public ConfettiView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void burstConfetti() {
        particles.clear();
        int w = getWidth() > 0 ? getWidth() : 1080;
        for (int i = 0; i < 85; i++) {
            Particle p = new Particle();
            p.x = random.nextFloat() * w;
            p.y = -random.nextFloat() * 600f;
            p.vx = (random.nextFloat() - 0.5f) * 8f;
            p.vy = 5f + random.nextFloat() * 11f;
            p.size = 10f + random.nextFloat() * 14f;
            p.angle = random.nextFloat() * 360f;
            p.vAngle = (random.nextFloat() - 0.5f) * 10f;
            p.color = COLORS[random.nextInt(COLORS.length)];
            p.isCircle = random.nextBoolean();
            particles.add(p);
        }
        active = true;
        invalidate();
    }

    public void stopConfetti() {
        active = false;
        particles.clear();
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (!active || particles.isEmpty()) return;

        int h = getHeight() > 0 ? getHeight() : 1920;
        int w = getWidth() > 0 ? getWidth() : 1080;
        boolean anyAlive = false;

        for (Particle p : particles) {
            p.x += p.vx;
            p.y += p.vy;
            p.angle += p.vAngle;
            if (p.y < h + 80) {
                anyAlive = true;
                paint.setColor(p.color);
                canvas.save();
                canvas.translate(p.x, p.y);
                canvas.rotate(p.angle);
                if (p.isCircle) {
                    canvas.drawCircle(0, 0, p.size / 2f, paint);
                } else {
                    canvas.drawRect(-p.size / 2f, -p.size / 3f, p.size / 2f, p.size / 3f, paint);
                }
                canvas.restore();
            } else if (random.nextFloat() < 0.04f) {
                // Recycle some particles for continuous gentle celebration
                p.y = -20f;
                p.x = random.nextFloat() * w;
                anyAlive = true;
            }
        }

        if (anyAlive) {
            postInvalidateOnAnimation();
        } else {
            active = false;
        }
    }
}
