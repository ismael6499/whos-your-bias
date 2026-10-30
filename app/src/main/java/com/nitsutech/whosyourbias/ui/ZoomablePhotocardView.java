package com.nitsutech.whosyourbias.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.drawable.Drawable;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.ViewParent;
import android.widget.ImageView;

/**
 * Interactive Full-HD Photocard ImageView with Pinch-to-Zoom (1.0x to 4.5x),
 * Double-Tap to Zoom (1.0x <-> 2.5x), and smooth Pan/Drag with boundary clamping.
 */
public class ZoomablePhotocardView extends ImageView {

    private final Matrix matrix = new Matrix();
    private final float[] matrixValues = new float[9];

    private float minScale = 1.0f;
    private float maxScale = 4.5f;
    private float currentScale = 1.0f;

    private final PointF lastTouch = new PointF();
    private boolean isDragging = false;

    private final ScaleGestureDetector scaleDetector;
    private final GestureDetector gestureDetector;

    public ZoomablePhotocardView(Context context) {
        super(context);
        super.setScaleType(ScaleType.MATRIX);
        setImageMatrix(matrix);

        scaleDetector = new ScaleGestureDetector(context, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                float factor = detector.getScaleFactor();
                applyZoom(factor, detector.getFocusX(), detector.getFocusY());
                return true;
            }
        });

        gestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDoubleTap(MotionEvent e) {
                if (currentScale > minScale * 1.15f) {
                    fitImageToView();
                } else {
                    float targetFactor = (minScale * 2.5f) / Math.max(0.01f, currentScale);
                    applyZoom(targetFactor, e.getX(), e.getY());
                }
                return true;
            }
        });
    }

    @Override
    public void setImageBitmap(Bitmap bm) {
        super.setImageBitmap(bm);
        post(this::fitImageToView);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        fitImageToView();
    }

    public void fitImageToView() {
        Drawable d = getDrawable();
        if (d == null) return;
        int viewW = getWidth();
        int viewH = getHeight();
        int dW = d.getIntrinsicWidth();
        int dH = d.getIntrinsicHeight();
        if (viewW <= 0 || viewH <= 0 || dW <= 0 || dH <= 0) return;

        matrix.reset();
        float scale = Math.min((float) viewW / dW, (float) viewH / dH);
        minScale = scale;
        maxScale = scale * 4.5f;
        currentScale = scale;

        float dx = (viewW - dW * scale) * 0.5f;
        float dy = (viewH - dH * scale) * 0.5f;
        matrix.postScale(scale, scale);
        matrix.postTranslate(dx, dy);
        setImageMatrix(matrix);
        invalidate();
    }

    private void applyZoom(float factor, float focusX, float focusY) {
        float newScale = currentScale * factor;
        if (newScale > maxScale) {
            factor = maxScale / currentScale;
            newScale = maxScale;
        } else if (newScale < minScale) {
            factor = minScale / currentScale;
            newScale = minScale;
        }
        currentScale = newScale;
        matrix.postScale(factor, factor, focusX, focusY);
        clampTranslation();
        setImageMatrix(matrix);
        invalidate();
    }

    private void clampTranslation() {
        Drawable d = getDrawable();
        if (d == null) return;
        int viewW = getWidth();
        int viewH = getHeight();
        int dW = d.getIntrinsicWidth();
        int dH = d.getIntrinsicHeight();
        if (viewW <= 0 || viewH <= 0 || dW <= 0 || dH <= 0) return;

        matrix.getValues(matrixValues);
        float transX = matrixValues[Matrix.MTRANS_X];
        float transY = matrixValues[Matrix.MTRANS_Y];
        float scaledW = dW * matrixValues[Matrix.MSCALE_X];
        float scaledH = dH * matrixValues[Matrix.MSCALE_Y];

        float fixX = getFixTranslation(transX, viewW, scaledW);
        float fixY = getFixTranslation(transY, viewH, scaledH);
        if (fixX != 0 || fixY != 0) {
            matrix.postTranslate(fixX, fixY);
        }
    }

    private float getFixTranslation(float trans, float viewSize, float contentSize) {
        float minTrans, maxTrans;
        if (contentSize <= viewSize) {
            minTrans = (viewSize - contentSize) * 0.5f;
            maxTrans = minTrans;
        } else {
            minTrans = viewSize - contentSize;
            maxTrans = 0f;
        }
        if (trans < minTrans) return minTrans - trans;
        if (trans > maxTrans) return maxTrans - trans;
        return 0f;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        ViewParent parent = getParent();
        if (parent != null && (event.getPointerCount() > 1 || currentScale > minScale * 1.04f)) {
            parent.requestDisallowInterceptTouchEvent(true);
        }

        gestureDetector.onTouchEvent(event);
        scaleDetector.onTouchEvent(event);

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                lastTouch.set(event.getX(), event.getY());
                isDragging = true;
                break;

            case MotionEvent.ACTION_MOVE:
                if (isDragging && !scaleDetector.isInProgress() && currentScale > minScale * 1.02f) {
                    float dx = event.getX() - lastTouch.x;
                    float dy = event.getY() - lastTouch.y;
                    matrix.postTranslate(dx, dy);
                    clampTranslation();
                    setImageMatrix(matrix);
                    invalidate();
                    lastTouch.set(event.getX(), event.getY());
                }
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_POINTER_UP:
                isDragging = false;
                break;
        }
        return true;
    }
}
