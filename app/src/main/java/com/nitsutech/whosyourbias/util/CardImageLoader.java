package com.nitsutech.whosyourbias.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.media.FaceDetector;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.util.LruCache;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.nitsutech.whosyourbias.R;
import com.nitsutech.whosyourbias.data.IdolCard;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class CardImageLoader {
    // Large cache (600 entries) so loaded idol photos NEVER get evicted and never flicker back to placeholder
    private static final LruCache<String, Bitmap> rawMemoryCache = new LruCache<>(380);
    private static final LruCache<String, Bitmap> fittedMemoryCache = new LruCache<>(650);
    private static final LruCache<String, Bitmap> kawaiiLoaderCache = new LruCache<>(24);

    // Fast dedicated disk executor so disk cache hits never wait behind network downloads or block UI thread
    private static final ExecutorService diskExecutor = Executors.newFixedThreadPool(4);

    // LIFO (Last-In-First-Out) 10-thread network pool so currently visible cards on screen load FIRST one by one!
    private static final ExecutorService networkExecutor = new ThreadPoolExecutor(
            10, 10, 60L, TimeUnit.SECONDS,
            new LinkedBlockingDeque<Runnable>() {
                @Override
                public boolean offer(Runnable runnable) {
                    return super.offerFirst(runnable);
                }
            }
    );

    private static final ConcurrentHashMap<String, List<Runnable>> inFlightCallbacks = new ConcurrentHashMap<>();
    private static final ExecutorService preloadExecutor = Executors.newFixedThreadPool(16);
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    public interface PreloadProgressListener {
        void onProgress(int loaded, int total);
        void onComplete();
    }

    private static String getRawCacheKey(IdolCard card) {
        if (card == null) return "null";
        String uri = card.getPhotoUri();
        if (uri != null && !uri.trim().isEmpty()) {
            return "uri_" + IdolCard.normalizePhotoIdentity(uri);
        }
        return "id_" + card.getId();
    }

    public static void preloadSingleCardAsync(Context context, IdolCard card, Runnable onReadyUi) {
        if (context == null || card == null) {
            if (onReadyUi != null) mainHandler.post(onReadyUi);
            return;
        }
        final String rawKey = getRawCacheKey(card);
        Bitmap cached = rawMemoryCache.get(rawKey);
        if (cached != null && !cached.isRecycled()) {
            if (onReadyUi != null) mainHandler.post(onReadyUi);
            return;
        }
        final Context appCtx = context.getApplicationContext();
        networkExecutor.execute(() -> {
            try {
                File diskCached = getExistingDiskCacheFile(appCtx, card);
                if (diskCached != null) {
                    Bitmap diskBmp = decodeSampledFile(diskCached, 540, 540);
                    if (diskBmp != null) {
                        rawMemoryCache.put(rawKey, diskBmp);
                        if (onReadyUi != null) mainHandler.post(onReadyUi);
                        return;
                    }
                }
                Bitmap loaded = fetchOrResolveBitmap(appCtx, card);
                if (loaded != null) {
                    rawMemoryCache.put(rawKey, loaded);
                }
            } finally {
                if (onReadyUi != null) mainHandler.post(onReadyUi);
            }
        });
    }

    public static boolean needsInitialPreload(Context context, List<IdolCard> idols) {
        if (context == null || idols == null || idols.isEmpty()) return false;
        int cachedCount = 0;
        int checkLimit = Math.min(idols.size(), 60);
        for (int i = 0; i < checkLimit; i++) {
            if (getExistingDiskCacheFile(context, idols.get(i)) != null) {
                cachedCount++;
            }
        }
        return cachedCount < (checkLimit * 0.75);
    }

    public static void preloadAllInitialImages(Context context, List<IdolCard> idols, PreloadProgressListener listener) {
        if (context == null || idols == null || idols.isEmpty()) {
            if (listener != null) mainHandler.post(listener::onComplete);
            return;
        }
        final Context appCtx = context.getApplicationContext();
        preloadExecutor.execute(() -> {
            List<IdolCard> missing = new ArrayList<>();
            for (IdolCard c : idols) {
                if (getExistingDiskCacheFile(appCtx, c) == null) {
                    missing.add(c);
                }
            }
            final int total = idols.size();
            final int alreadyCached = total - missing.size();
            if (missing.isEmpty()) {
                if (listener != null) {
                    mainHandler.post(() -> {
                        listener.onProgress(total, total);
                        listener.onComplete();
                    });
                }
                return;
            }

            if (listener != null) {
                mainHandler.post(() -> listener.onProgress(alreadyCached, total));
            }

            final java.util.concurrent.atomic.AtomicInteger doneCounter = new java.util.concurrent.atomic.AtomicInteger(alreadyCached);
            final java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(missing.size());

            for (final IdolCard item : missing) {
                preloadExecutor.execute(() -> {
                    try {
                        Bitmap b = fetchOrResolveBitmap(appCtx, item);
                        if (b != null) {
                            rawMemoryCache.put(getRawCacheKey(item), b);
                        }
                    } catch (Exception ignored) {
                    } finally {
                        final int currentDone = doneCounter.incrementAndGet();
                        if (listener != null) {
                            mainHandler.post(() -> listener.onProgress(currentDone, total));
                        }
                        latch.countDown();
                    }
                });
            }

            try {
                // Wait up to 35 seconds max so offline connections never hang forever
                latch.await(35, TimeUnit.SECONDS);
            } catch (InterruptedException ignored) {
            }
            if (listener != null) {
                mainHandler.post(listener::onComplete);
            }
        });
    }

    public static void preloadMatchCardsAsync(Context context, List<IdolCard> matchCards, PreloadProgressListener listener) {
        if (context == null || matchCards == null || matchCards.isEmpty()) {
            if (listener != null) mainHandler.post(listener::onComplete);
            return;
        }
        final Context appCtx = context.getApplicationContext();
        preloadExecutor.execute(() -> {
            List<IdolCard> itemsToPreload = new ArrayList<>();
            for (IdolCard c : matchCards) {
                if (c == null) continue;
                // Primary cover photo
                if (getExistingDiskCacheFile(appCtx, c) == null) {
                    itemsToPreload.add(c);
                }
                // Preload unlocked photocards if random photocards are used
                if (c.isUseRandomPhotocard()) {
                    List<String> unl = c.getUnlockedPhotocards();
                    for (int i = 0; i < Math.min(unl.size(), 5); i++) {
                        String uri = unl.get(i);
                        if (uri != null && !uri.isEmpty() && !IdolCard.isSamePhoto(uri, c.getPhotoUri())) {
                            IdolCard dummy = new IdolCard(c.getId() + "_pc" + i, c.getStageName(), c.getFullName(),
                                    c.getGroupName(), c.isSoloist(), c.getCountry(), c.getBirthDate(), c.getRole(),
                                    uri, c.getAccentColor(), c.getBio(), c.getCollections());
                            if (getExistingDiskCacheFile(appCtx, dummy) == null) {
                                itemsToPreload.add(dummy);
                            }
                        }
                    }
                }
            }

            final int total = itemsToPreload.size();
            if (total == 0) {
                if (listener != null) mainHandler.post(listener::onComplete);
                return;
            }

            if (listener != null) {
                mainHandler.post(() -> listener.onProgress(0, total));
            }

            final java.util.concurrent.atomic.AtomicInteger done = new java.util.concurrent.atomic.AtomicInteger(0);
            final java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(total);

            for (IdolCard item : itemsToPreload) {
                preloadExecutor.execute(() -> {
                    try {
                        Bitmap b = fetchOrResolveBitmap(appCtx, item);
                        if (b != null) {
                            String rawKey = getRawCacheKey(item);
                            rawMemoryCache.put(rawKey, b);
                            Bitmap fitted = createUncroppedFittedPhoto(b, 480, 380);
                            fittedMemoryCache.put(rawKey + "_480x380", fitted);
                        }
                    } catch (Exception ignored) {
                    } finally {
                        int cur = done.incrementAndGet();
                        if (listener != null) {
                            mainHandler.post(() -> listener.onProgress(cur, total));
                        }
                        latch.countDown();
                    }
                });
            }

            try {
                // Wait up to 15 seconds max so it never hangs indefinitely
                latch.await(15, TimeUnit.SECONDS);
            } catch (InterruptedException ignored) {
            }
            if (listener != null) {
                mainHandler.post(listener::onComplete);
            }
        });
    }

    public static void loadCardImage(Context context, IdolCard card, ImageView imageView) {
        if (card == null || imageView == null) return;
        int[] ratio = inferTargetCanvasSize(imageView);
        loadCardImageWithSize(context, card, imageView, ratio[0], ratio[1]);
    }

    public static void loadUriIntoImageView(Context context, String uriStr, ImageView imageView) {
        loadUriIntoImageView(context, uriStr, imageView, "Song", Color.parseColor("#EC4899"));
    }

    public static void loadUriIntoImageView(Context context, String uriStr, ImageView imageView, String label, int accentColor) {
        if (uriStr == null || imageView == null) return;
        IdolCard temp = new IdolCard("custom_uri_" + Math.abs(uriStr.hashCode()),
                label != null && !label.isEmpty() ? label : "Bias",
                label != null ? label : "", "Who's your Bias", false,
                "South Korea", "2004-01-01", "Idol",
                uriStr, String.format("#%06X", (0xFFFFFF & accentColor)), "", "");
        loadCardImage(context, temp, imageView);
    }

    public static void loadHighResForZoomPreview(Context context, IdolCard baseCard, String uriStr, ImageView zoomImageView) {
        if (zoomImageView == null) return;
        final String effectiveUri = (uriStr != null && !uriStr.isEmpty()) ? uriStr : (baseCard != null ? baseCard.getPhotoUri() : "");
        final IdolCard temp = new IdolCard(
                baseCard != null ? baseCard.getId() : "zoom_preview",
                baseCard != null ? baseCard.getStageName() : "Idol",
                baseCard != null ? baseCard.getFullName() : "",
                baseCard != null ? baseCard.getGroupName() : "",
                baseCard != null && baseCard.isSoloist(),
                baseCard != null ? baseCard.getCountry() : "South Korea",
                baseCard != null ? baseCard.getBirthDate() : "2004-01-01",
                baseCard != null ? baseCard.getRole() : "Idol",
                effectiveUri,
                baseCard != null ? baseCard.getAccentColor() : "#FF85A2",
                "", "");
        final String rawKey = getRawCacheKey(temp);
        Bitmap cachedRaw = rawMemoryCache.get(rawKey);
        if (cachedRaw != null && !cachedRaw.isRecycled()) {
            zoomImageView.setImageBitmap(cachedRaw);
            return;
        }
        final Context appCtx = context.getApplicationContext();
        File diskCached = getExistingDiskCacheFile(appCtx, temp);
        if (diskCached != null) {
            Bitmap diskBmp = decodeSampledFile(diskCached, 720, 720);
            if (diskBmp != null) {
                rawMemoryCache.put(rawKey, diskBmp);
                zoomImageView.setImageBitmap(diskBmp);
                return;
            }
        }
        zoomImageView.setImageBitmap(createCuteKawaiiLoadingArt(context, temp, 480, 620));
        networkExecutor.execute(() -> {
            Bitmap loaded = fetchOrResolveBitmap(appCtx, temp);
            if (loaded != null) {
                rawMemoryCache.put(rawKey, loaded);
                mainHandler.post(() -> zoomImageView.setImageBitmap(loaded));
            } else {
                mainHandler.post(() -> zoomImageView.setImageBitmap(createStylizedPortrait(temp, 480, 620)));
            }
        });
    }

    public static void loadCardImageWithSize(Context context, IdolCard card, ImageView imageView, int targetW, int targetH) {
        if (card == null || imageView == null) return;
        imageView.setScaleType(ImageView.ScaleType.FIT_XY);

        final String rawKey = getRawCacheKey(card);
        final String fittedKey = rawKey + "_" + targetW + "x" + targetH;

        Bitmap cachedFitted = fittedMemoryCache.get(fittedKey);
        if (cachedFitted != null && !cachedFitted.isRecycled()) {
            imageView.setTag(fittedKey);
            imageView.setImageBitmap(cachedFitted);
            return;
        }

        Bitmap cachedRaw = rawMemoryCache.get(rawKey);
        if (cachedRaw != null && !cachedRaw.isRecycled()) {
            Bitmap fitted = createUncroppedFittedPhoto(cachedRaw, targetW, targetH);
            fittedMemoryCache.put(fittedKey, fitted);
            imageView.setTag(fittedKey);
            imageView.setImageBitmap(fitted);
            return;
        }

        // Instant 0ms cached cute kitten placeholder on UI thread
        Bitmap cuteLoader = createCuteKawaiiLoadingArt(context, card, targetW, targetH);
        imageView.setTag(fittedKey);
        imageView.setImageBitmap(cuteLoader);

        final Context appCtx = context.getApplicationContext();

        // Check fast local disk cache on diskExecutor first
        diskExecutor.execute(() -> {
            File diskCached = getExistingDiskCacheFile(appCtx, card);
            if (diskCached != null) {
                Bitmap diskBmp = decodeSampledFile(diskCached, 540, 540);
                if (diskBmp != null) {
                    rawMemoryCache.put(rawKey, diskBmp);
                    final Bitmap fitted = createUncroppedFittedPhoto(diskBmp, targetW, targetH);
                    fittedMemoryCache.put(fittedKey, fitted);
                    mainHandler.post(() -> {
                        if (fittedKey.equals(imageView.getTag())) {
                            imageView.setImageBitmap(fitted);
                        }
                    });
                    return;
                }
            }

            // Deduplicate network downloads per rawKey and update every waiting ImageView 1 by 1 immediately!
            Runnable uiUpdater = () -> {
                Bitmap raw = rawMemoryCache.get(rawKey);
                final Bitmap resultBmp;
                if (raw != null && !raw.isRecycled()) {
                    Bitmap f = fittedMemoryCache.get(fittedKey);
                    if (f == null || f.isRecycled()) {
                        f = createUncroppedFittedPhoto(raw, targetW, targetH);
                        fittedMemoryCache.put(fittedKey, f);
                    }
                    resultBmp = f;
                } else {
                    resultBmp = createStylizedPortrait(card, targetW, targetH);
                }
                mainHandler.post(() -> {
                    if (fittedKey.equals(imageView.getTag())) {
                        imageView.setAlpha(0.45f);
                        imageView.setImageBitmap(resultBmp);
                        imageView.animate().alpha(1f).setDuration(130).start();
                    }
                });
            };

            synchronized (inFlightCallbacks) {
                List<Runnable> waiters = inFlightCallbacks.get(rawKey);
                if (waiters != null) {
                    waiters.add(uiUpdater);
                    return;
                }
                waiters = new ArrayList<>();
                waiters.add(uiUpdater);
                inFlightCallbacks.put(rawKey, waiters);
            }

            networkExecutor.execute(() -> {
                try {
                    Bitmap loaded = fetchOrResolveBitmap(appCtx, card);
                    if (loaded != null) {
                        rawMemoryCache.put(rawKey, loaded);
                    }
                } finally {
                    List<Runnable> callbacks;
                    synchronized (inFlightCallbacks) {
                        callbacks = inFlightCallbacks.remove(rawKey);
                    }
                    if (callbacks != null) {
                        for (Runnable cb : callbacks) {
                            cb.run();
                        }
                    }
                }
            });
        });
    }

    private static int[] inferTargetCanvasSize(ImageView iv) {
        ViewGroup.LayoutParams lp = iv.getLayoutParams();
        if (lp != null && lp.width > 0 && lp.height > 0) {
            float ratio = (float) lp.width / (float) lp.height;
            if (Math.abs(ratio - 1.0f) < 0.15f) {
                return new int[]{360, 360}; // Square squircle
            } else if (ratio > 1.05f) {
                return new int[]{460, Math.max(280, Math.round(460f / ratio))};
            } else {
                return new int[]{380, Math.min(540, Math.round(380f / ratio))};
            }
        }
        if (lp != null && lp.height > 0 && lp.width == ViewGroup.LayoutParams.MATCH_PARENT) {
            // Estimate container width around 340dp
            float density = iv.getContext().getResources().getDisplayMetrics().density;
            float hDp = lp.height / Math.max(1f, density);
            if (hDp <= 160f) {
                return new int[]{400, 340}; // Carousel card
            } else if (hDp <= 230f) {
                return new int[]{360, 460}; // 2-col grid card
            } else {
                return new int[]{440, 440}; // Large profile photo card
            }
        }
        // Default for duel cards in MatchActivity (slightly wider than tall)
        return new int[]{480, 380};
    }

    /**
     * Renders high-definition physical photocard finish: full bleed edge-to-edge,
     * crisp center crop, zero blur, zero pillarboxes or ugly bars,
     * framed like a real physical K-Pop album photocard!
     */
    public static Bitmap createUncroppedFittedPhoto(Bitmap src, int targetW, int targetH) {
        return createPhysicalPhotocardBitmap(src, targetW, targetH);
    }

    public static class FaceLocation {
        public final float centerX;
        public final float centerY;
        public final float eyeDistance;

        public FaceLocation(float centerX, float centerY, float eyeDistance) {
            this.centerX = centerX;
            this.centerY = centerY;
            this.eyeDistance = eyeDistance;
        }
    }

    private static final LruCache<Integer, FaceLocation> faceLocationCache = new LruCache<>(400);

    public static FaceLocation detectFaceFast(Bitmap src) {
        if (src == null || src.isRecycled() || src.getWidth() <= 10 || src.getHeight() <= 10) return null;
        int cacheKey = src.hashCode() ^ ((src.getWidth() << 16) | (src.getHeight() & 0xFFFF));
        FaceLocation cached = faceLocationCache.get(cacheKey);
        if (cached != null) return cached;

        try {
            int origW = src.getWidth();
            int origH = src.getHeight();

            int maxDim = 320;
            float ratio = Math.min(1.0f, (float) maxDim / Math.max(origW, origH));
            int sw = Math.round(origW * ratio);
            int sh = Math.round(origH * ratio);
            if (sw % 2 != 0) sw++; // FaceDetector requires strictly even width

            Bitmap sample = Bitmap.createScaledBitmap(src, sw, sh, false);
            Bitmap rgb565 = (sample.getConfig() == Bitmap.Config.RGB_565) ? sample : sample.copy(Bitmap.Config.RGB_565, false);
            // CRITICAL: NEVER recycle 'src' if createScaledBitmap or copy returned 'src'!
            if (sample != null && sample != src && sample != rgb565) {
                sample.recycle();
            }

            FaceDetector detector = new FaceDetector(sw, sh, 1);
            FaceDetector.Face[] faces = new FaceDetector.Face[1];
            int count = detector.findFaces(rgb565, faces);
            // CRITICAL: NEVER recycle 'src'!
            if (rgb565 != null && rgb565 != src) {
                rgb565.recycle();
            }

            if (count > 0 && faces[0] != null && faces[0].confidence() > 0.30f) {
                PointF pt = new PointF();
                faces[0].getMidPoint(pt);
                float eyeDist = faces[0].eyesDistance();
                float scaleX = (float) origW / sw;
                float scaleY = (float) origH / sh;
                FaceLocation loc = new FaceLocation(pt.x * scaleX, pt.y * scaleY, eyeDist * scaleX);
                faceLocationCache.put(cacheKey, loc);
                return loc;
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    public static Bitmap createPhysicalPhotocardBitmap(Bitmap src, int targetW, int targetH) {
        if (src == null || src.isRecycled() || targetW <= 0 || targetH <= 0) return null;
        int sW = src.getWidth();
        int sH = src.getHeight();
        if (sW <= 0 || sH <= 0) return src;

        Bitmap out = Bitmap.createBitmap(targetW, targetH, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(out);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG | Paint.DITHER_FLAG);

        // 1. GORGEOUS BLURRED BACKGROUND (Fondo Difuminado):
        // Scale down to ~50px for fast, smooth Gaussian blur without any lag or memory overhead
        int bgW = Math.max(16, targetW / 8);
        int bgH = Math.max(16, targetH / 8);
        try {
            Bitmap smallBg = Bitmap.createScaledBitmap(src, bgW, bgH, true);
            Bitmap blurredBg = fastStackBlur(smallBg, 12);
            if (blurredBg != null && !blurredBg.isRecycled()) {
                RectF bgDst = new RectF(0, 0, targetW, targetH);
                canvas.drawBitmap(blurredBg, null, bgDst, paint);
                // Soft dark scrim overlay (35% darkness) so foreground photo pops with beautiful depth
                Paint dimPaint = new Paint();
                dimPaint.setColor(Color.parseColor("#4D000000"));
                canvas.drawRect(bgDst, dimPaint);
                if (blurredBg != smallBg && blurredBg != src) blurredBg.recycle();
            }
            if (smallBg != null && smallBg != src && !smallBg.isRecycled()) {
                smallBg.recycle();
            }
        } catch (Throwable ignored) {}

        // 2. FULL CRISP FOREGROUND IMAGE (Fit Center - 0% Zoom, Zero Cropping):
        // Displays the entire photo in full HD, perfectly centered, sharp and uncropped!
        float scale = Math.min((float) targetW / (float) sW, (float) targetH / (float) sH);
        float drawW = sW * scale;
        float drawH = sH * scale;
        float left = (targetW - drawW) / 2f;
        float top = (targetH - drawH) / 2f;

        RectF fgDst = new RectF(left, top, left + drawW, top + drawH);
        try {
            if (!src.isRecycled()) {
                canvas.drawBitmap(src, null, fgDst, paint);
            }
        } catch (Throwable ignored) {}

        return out;
    }

    public static Bitmap fastStackBlur(Bitmap sentBitmap, int radius) {
        if (sentBitmap == null || radius < 1) return sentBitmap;
        try {
            Bitmap bitmap = sentBitmap.copy(Bitmap.Config.ARGB_8888, true);
            int w = bitmap.getWidth();
            int h = bitmap.getHeight();
            int[] pix = new int[w * h];
            bitmap.getPixels(pix, 0, w, 0, 0, w, h);
            int wm = w - 1;
            int hm = h - 1;
            int wh = w * h;
            int div = radius + radius + 1;
            int[] r = new int[wh];
            int[] g = new int[wh];
            int[] b = new int[wh];
            int rsum, gsum, bsum, x, y, i, p, yp, yi, yw;
            int[] vmin = new int[Math.max(w, h)];
            int divsum = (div + 1) >> 1;
            divsum *= divsum;
            int[] dv = new int[256 * divsum];
            for (i = 0; i < 256 * divsum; i++) {
                dv[i] = (i / divsum);
            }
            yw = yi = 0;
            int[][] stack = new int[div][3];
            int stackpointer;
            int stackstart;
            int[] sir;
            int rbs;
            int r1 = radius + 1;
            int routsum, goutsum, boutsum;
            int rinsum, ginsum, binsum;
            for (y = 0; y < h; y++) {
                rinsum = ginsum = binsum = routsum = goutsum = boutsum = rsum = gsum = bsum = 0;
                for (i = -radius; i <= radius; i++) {
                    p = pix[yi + Math.min(wm, Math.max(i, 0))];
                    sir = stack[i + radius];
                    sir[0] = (p & 0xff0000) >> 16;
                    sir[1] = (p & 0x00ff00) >> 8;
                    sir[2] = (p & 0x0000ff);
                    rbs = r1 - Math.abs(i);
                    rsum += sir[0] * rbs;
                    gsum += sir[1] * rbs;
                    bsum += sir[2] * rbs;
                    if (i > 0) {
                        rinsum += sir[0];
                        ginsum += sir[1];
                        binsum += sir[2];
                    } else {
                        routsum += sir[0];
                        goutsum += sir[1];
                        boutsum += sir[2];
                    }
                }
                stackpointer = radius;
                for (x = 0; x < w; x++) {
                    r[yi] = dv[rsum];
                    g[yi] = dv[gsum];
                    b[yi] = dv[bsum];
                    rsum -= routsum;
                    gsum -= goutsum;
                    bsum -= boutsum;
                    stackstart = stackpointer - radius + div;
                    sir = stack[stackstart % div];
                    routsum -= sir[0];
                    goutsum -= sir[1];
                    boutsum -= sir[2];
                    if (y == 0) {
                        vmin[x] = Math.min(x + radius + 1, wm);
                    }
                    p = pix[yw + vmin[x]];
                    sir[0] = (p & 0xff0000) >> 16;
                    sir[1] = (p & 0x00ff00) >> 8;
                    sir[2] = (p & 0x0000ff);
                    rinsum += sir[0];
                    ginsum += sir[1];
                    binsum += sir[2];
                    rsum += rinsum;
                    gsum += ginsum;
                    bsum += binsum;
                    stackpointer = (stackpointer + 1) % div;
                    sir = stack[(stackpointer) % div];
                    routsum += sir[0];
                    goutsum += sir[1];
                    boutsum += sir[2];
                    rinsum -= sir[0];
                    ginsum -= sir[1];
                    binsum -= sir[2];
                    yi++;
                }
                yw += w;
            }
            for (x = 0; x < w; x++) {
                rinsum = ginsum = binsum = routsum = goutsum = boutsum = rsum = gsum = bsum = 0;
                yp = -radius * w;
                for (i = -radius; i <= radius; i++) {
                    yi = Math.max(0, yp) + x;
                    sir = stack[i + radius];
                    sir[0] = r[yi];
                    sir[1] = g[yi];
                    sir[2] = b[yi];
                    rbs = r1 - Math.abs(i);
                    rsum += r[yi] * rbs;
                    gsum += g[yi] * rbs;
                    bsum += b[yi] * rbs;
                    if (i > 0) {
                        rinsum += sir[0];
                        ginsum += sir[1];
                        binsum += sir[2];
                    } else {
                        routsum += sir[0];
                        goutsum += sir[1];
                        boutsum += sir[2];
                    }
                    if (i < hm) {
                        yp += w;
                    }
                }
                yi = x;
                stackpointer = radius;
                for (y = 0; y < h; y++) {
                    pix[yi] = (0xff000000 & pix[yi]) | (dv[rsum] << 16) | (dv[gsum] << 8) | dv[bsum];
                    rsum -= routsum;
                    gsum -= goutsum;
                    bsum -= boutsum;
                    stackstart = stackpointer - radius + div;
                    sir = stack[stackstart % div];
                    routsum -= sir[0];
                    goutsum -= sir[1];
                    boutsum -= sir[2];
                    if (x == 0) {
                        vmin[y] = Math.min(y + r1, hm) * w;
                    }
                    p = x + vmin[y];
                    sir[0] = r[p];
                    sir[1] = g[p];
                    sir[2] = b[p];
                    rinsum += sir[0];
                    ginsum += sir[1];
                    binsum += sir[2];
                    rsum += rinsum;
                    gsum += ginsum;
                    bsum += binsum;
                    stackpointer = (stackpointer + 1) % div;
                    sir = stack[stackpointer % div];
                    routsum += sir[0];
                    goutsum += sir[1];
                    boutsum += sir[2];
                    rinsum -= sir[0];
                    ginsum -= sir[1];
                    binsum -= sir[2];
                    yi += w;
                }
            }
            bitmap.setPixels(pix, 0, w, 0, 0, w, h);
            return bitmap;
        } catch (Exception e) {
            return sentBitmap;
        }
    }

    public static Bitmap getCardBitmapSync(Context context, IdolCard card, int width, int height) {
        if (card == null) return null;
        String rawKey = getRawCacheKey(card);
        Bitmap cached = rawMemoryCache.get(rawKey);
        if (cached != null && !cached.isRecycled()) {
            return createUncroppedFittedPhoto(cached, width, height);
        }
        Bitmap loaded = fetchOrResolveBitmap(context.getApplicationContext(), card);
        if (loaded != null) {
            rawMemoryCache.put(rawKey, loaded);
            return createUncroppedFittedPhoto(loaded, width, height);
        }
        return createStylizedPortrait(card, width, height);
    }

    public static void invalidateCacheForCard(IdolCard card) {
        if (card == null) return;
        rawMemoryCache.evictAll();
        fittedMemoryCache.evictAll();
    }

    private static final Map<String, String> bundledPhotoMap = new ConcurrentHashMap<>();
    private static volatile boolean manifestLoaded = false;

    private static void ensureManifestLoaded(Context context) {
        if (manifestLoaded || context == null) return;
        synchronized (bundledPhotoMap) {
            if (manifestLoaded) return;
            try (InputStream is = context.getAssets().open("bundled_photos/manifest.json");
                 BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
                String raw = sb.toString();
                if (raw.startsWith("\uFEFF")) raw = raw.substring(1);
                JSONObject json = new JSONObject(raw);
                java.util.Iterator<String> keys = json.keys();
                while (keys.hasNext()) {
                    String k = keys.next();
                    bundledPhotoMap.put(k.toLowerCase(java.util.Locale.ROOT), json.getString(k));
                }
            } catch (Exception ignored) {
            }
            manifestLoaded = true;
        }
    }

    private static String getBundledAssetName(Context context, String normalizedUri) {
        if (normalizedUri == null || normalizedUri.isEmpty()) return null;
        ensureManifestLoaded(context);
        return bundledPhotoMap.get(normalizedUri.toLowerCase(java.util.Locale.ROOT));
    }

    private static File getExistingDiskCacheFile(Context context, IdolCard card) {
        try {
            String uri = card.getPhotoUri();
            File cacheDir = new File(context.getCacheDir(), "idol_img_cache");
            if (uri != null && !uri.trim().isEmpty()) {
                uri = uri.trim();
                String norm = IdolCard.normalizePhotoIdentity(uri);
                String bundledAsset = getBundledAssetName(context, norm);
                if (bundledAsset != null) {
                    File bundledCached = new File(cacheDir, "bnd_" + bundledAsset);
                    if (bundledCached.exists() && bundledCached.length() > 500) {
                        return bundledCached;
                    }
                    if (!cacheDir.exists()) cacheDir.mkdirs();
                    try (InputStream is = context.getAssets().open("bundled_photos/" + bundledAsset);
                         FileOutputStream fos = new FileOutputStream(bundledCached)) {
                        byte[] buf = new byte[8192];
                        int r;
                        while ((r = is.read(buf)) != -1) {
                            fos.write(buf, 0, r);
                        }
                        return bundledCached;
                    } catch (Exception ignored) {}
                }
                if (uri.startsWith("/") || uri.startsWith("file://")) {
                    File f = new File(uri.startsWith("file://") ? uri.substring(7) : uri);
                    if (f.exists() && f.length() > 200) return f;
                } else if (uri.startsWith("http://") || uri.startsWith("https://")) {
                    File fNorm = new File(cacheDir, "url_" + Math.abs(norm.hashCode()) + ".jpg");
                    if (fNorm.exists() && fNorm.length() > 500) return fNorm;
                    File f = new File(cacheDir, "url_" + Math.abs(uri.hashCode()) + ".jpg");
                    if (f.exists() && f.length() > 500) return f;
                }
                return null;
            }
            File wikiCache = new File(cacheDir, "wiki_" + card.getId() + ".jpg");
            if (wikiCache.exists() && wikiCache.length() > 500) return wikiCache;
        } catch (Exception ignored) {
        }
        return null;
    }

    private static Bitmap fetchOrResolveBitmap(Context context, IdolCard card) {
        try {
            String uri = card.getPhotoUri();
            File cacheDir = new File(context.getCacheDir(), "idol_img_cache");
            if (!cacheDir.exists()) cacheDir.mkdirs();

            if (uri != null && uri.trim().startsWith("fandom_pc://")) {
                uri = KpopWikiImporter.resolvePhotocardSlotUrl(card, uri.trim());
            }

            if (uri != null && !uri.trim().isEmpty()) {
                uri = uri.trim();
                if (uri.startsWith("data:image")) {
                    int comma = uri.indexOf(',');
                    if (comma > 0) {
                        byte[] bytes = Base64.decode(uri.substring(comma + 1), Base64.DEFAULT);
                        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                    }
                }
                if (uri.startsWith("/") || uri.startsWith("file://")) {
                    String path = uri.startsWith("file://") ? uri.substring(7) : uri;
                    File f = new File(path);
                    if (f.exists()) {
                        return decodeSampledFile(f, 600, 600);
                    }
                }
                if (uri.startsWith("content://")) {
                    try (InputStream is = context.getContentResolver().openInputStream(Uri.parse(uri))) {
                        if (is != null) {
                            return BitmapFactory.decodeStream(is);
                        }
                    }
                }
                if (uri.startsWith("http://") || uri.startsWith("https://")) {
                    String norm = IdolCard.normalizePhotoIdentity(uri);
                    String bundledAsset = getBundledAssetName(context, norm);
                    if (bundledAsset != null) {
                        try (InputStream is = context.getAssets().open("bundled_photos/" + bundledAsset)) {
                            Bitmap b = BitmapFactory.decodeStream(is);
                            if (b != null) return b;
                        } catch (Exception ignored) {}
                    }
                    File cachedFile = new File(cacheDir, "url_" + Math.abs(norm.hashCode()) + ".jpg");
                    if (cachedFile.exists() && cachedFile.length() > 500) {
                        return decodeSampledFile(cachedFile, 600, 600);
                    }
                    File legacyFile = new File(cacheDir, "url_" + Math.abs(uri.hashCode()) + ".jpg");
                    if (legacyFile.exists() && legacyFile.length() > 500) {
                        return decodeSampledFile(legacyFile, 600, 600);
                    }
                    Bitmap dl = downloadBitmap(uri);
                    if (dl != null) {
                        saveBitmapToFile(dl, cachedFile, 88);
                        return dl;
                    }
                }
            }

            File wikiCache = new File(cacheDir, "wiki_" + card.getId() + ".jpg");
            if (wikiCache.exists() && wikiCache.length() > 500) {
                return decodeSampledFile(wikiCache, 600, 600);
            }
            Bitmap wikiBmp = tryFetchWikipediaPortrait(card);
            if (wikiBmp != null) {
                saveBitmapToFile(wikiBmp, wikiCache, 88);
                return wikiBmp;
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private static Bitmap tryFetchWikipediaPortrait(IdolCard card) {
        try {
            List<String> gallery = KpopWikiImporter.fetchUpTo30PhotocardsSync(card);
            for (int i = 0; i < Math.min(3, gallery.size()); i++) {
                Bitmap b = downloadBitmap(gallery.get(i));
                if (b != null) return b;
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private static Bitmap downloadBitmap(String urlStr) {
        if (urlStr == null || urlStr.trim().isEmpty()) return null;
        // 1. Direct download with Fandom Referer header
        Bitmap direct = downloadBitmapDirect(urlStr);
        if (direct != null) return direct;

        // 2. Automatic Cloudflare-bypass proxy fallback via wsrv.nl (fast 50KB JPEG)
        if (!urlStr.contains("wsrv.nl")) {
            try {
                String cleanTarget = urlStr.replaceFirst("^https?://", "");
                int qIdx = cleanTarget.indexOf('?');
                if (qIdx > 0) cleanTarget = cleanTarget.substring(0, qIdx);
                int revIdx = cleanTarget.indexOf("/revision/");
                if (revIdx > 0) cleanTarget = cleanTarget.substring(0, revIdx);
                String proxyUrl = "https://wsrv.nl/?url=" + URLEncoder.encode(cleanTarget, "UTF-8") + "&w=540&output=jpg";
                return downloadBitmapDirect(proxyUrl);
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    private static Bitmap downloadBitmapDirect(String urlStr) {
        HttpURLConnection conn = null;
        try {
            int redirects = 0;
            String currentUrl = urlStr;
            while (redirects < 4) {
                URL url = new URL(currentUrl);
                conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(4500);
                conn.setReadTimeout(5500);
                conn.setInstanceFollowRedirects(true);
                conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36");
                conn.setRequestProperty("Referer", "https://kpop.fandom.com/");
                conn.setRequestProperty("Origin", "https://kpop.fandom.com");
                conn.setRequestProperty("Accept", "image/jpeg,image/png,image/webp,image/*,*/*;q=0.8");
                conn.setRequestProperty("Accept-Language", "en-US,en;q=0.9,es;q=0.8");
                int code = conn.getResponseCode();
                if (code == HttpURLConnection.HTTP_MOVED_PERM || code == HttpURLConnection.HTTP_MOVED_TEMP || code == 307 || code == 308) {
                    String loc = conn.getHeaderField("Location");
                    if (loc != null && !loc.isEmpty()) {
                        currentUrl = loc;
                        redirects++;
                        conn.disconnect();
                        continue;
                    }
                }
                if (code == 200) {
                    try (InputStream is = conn.getInputStream()) {
                        return BitmapFactory.decodeStream(is);
                    }
                }
                break;
            }
        } catch (Exception ignored) {
        } finally {
            if (conn != null) {
                try { conn.disconnect(); } catch (Exception ignored) {}
            }
        }
        return null;
    }

    public static String copyUriToInternalStorage(Context context, Uri sourceUri) {
        try {
            File dir = new File(context.getFilesDir(), "idol_photos");
            if (!dir.exists()) dir.mkdirs();
            File dest = new File(dir, "photo_" + UUID.randomUUID().toString() + ".jpg");
            try (InputStream is = context.getContentResolver().openInputStream(sourceUri)) {
                Bitmap bmp = BitmapFactory.decodeStream(is);
                if (bmp != null) {
                    int maxDim = 800;
                    int w = bmp.getWidth();
                    int h = bmp.getHeight();
                    if (w > maxDim || h > maxDim) {
                        float ratio = Math.min((float) maxDim / w, (float) maxDim / h);
                        bmp = Bitmap.createScaledBitmap(bmp, Math.round(w * ratio), Math.round(h * ratio), true);
                    }
                    saveBitmapToFile(bmp, dest, 88);
                    return dest.getAbsolutePath();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return sourceUri.toString();
    }

    public static String encodeLocalPhotoAsDataUri(String photoUri) {
        if (photoUri == null || photoUri.isEmpty()) return "";
        if (photoUri.startsWith("http://") || photoUri.startsWith("https://") || photoUri.startsWith("data:")) {
            return photoUri;
        }
        try {
            String path = photoUri.startsWith("file://") ? photoUri.substring(7) : photoUri;
            File f = new File(path);
            if (!f.exists()) return photoUri;
            Bitmap bmp = decodeSampledFile(f, 360, 360);
            if (bmp == null) return photoUri;
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bmp.compress(Bitmap.CompressFormat.JPEG, 72, baos);
            String b64 = Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP);
            return "data:image/jpeg;base64," + b64;
        } catch (Exception e) {
            return photoUri;
        }
    }

    /**
     * Cute Kawaii Pastel Kitten Loading Illustration, cached by size for 0ms UI performance!
     */
    public static Bitmap createCuteKawaiiLoadingArt(Context context, IdolCard card, int width, int height) {
        int w = Math.max(240, width);
        int h = Math.max(240, height);
        String loadingText = context != null ? context.getString(R.string.card_loading_label) : "✨ Loading… ✨";
        String key = "kitten_loader_" + w + "x" + h + "_" + loadingText;
        Bitmap cached = kawaiiLoaderCache.get(key);
        if (cached != null && !cached.isRecycled()) {
            return cached;
        }
        Bitmap bmp = drawCuteKittenCardArt(w, h, loadingText, "");
        kawaiiLoaderCache.put(key, bmp);
        return bmp;
    }

    public static Bitmap createCuteKawaiiLoadingArt(IdolCard card, int width, int height) {
        return createCuteKawaiiLoadingArt(null, card, width, height);
    }

    /**
     * Soft Pastel Collectible Kitten Portrait when offline (never says "Cargando...").
     */
    public static Bitmap createStylizedPortrait(IdolCard card, int width, int height) {
        int w = Math.max(240, width);
        int h = Math.max(240, height);
        String title = (card != null && card.getStageName() != null) ? card.getStageName() : "Idol";
        String sub = (card != null && card.getGroupName() != null) ? card.getGroupName() : "";
        return drawCuteKittenCardArt(w, h, title, sub);
    }

    private static Bitmap drawCuteKittenCardArt(int w, int h, String primaryLabel, String secondaryLabel) {
        Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bmp);

        // Soft Pastel Pink to Butter Cream Gradient
        Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bgPaint.setShader(new LinearGradient(0, 0, w, h,
                new int[]{Color.parseColor("#FFF0F6"), Color.parseColor("#FFFBEB"), Color.parseColor("#F3E8FF")},
                null, Shader.TileMode.CLAMP));
        canvas.drawRect(0, 0, w, h, bgPaint);

        // Subtle Cute Polka Dots pattern
        Paint dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        dotPaint.setColor(Color.argb(85, 255, 255, 255));
        int step = Math.max(32, Math.min(w, h) / 6);
        for (int y = step / 2; y < h; y += step) {
            for (int x = step / 2; x < w; x += step) {
                canvas.drawCircle(x, y, step * 0.16f, dotPaint);
            }
        }

        float cx = w / 2f;
        float cy = h * 0.44f;
        float r = Math.min(w, h) * 0.21f;

        Paint whiteFill = new Paint(Paint.ANTI_ALIAS_FLAG);
        whiteFill.setColor(Color.WHITE);
        Paint pinkFill = new Paint(Paint.ANTI_ALIAS_FLAG);
        pinkFill.setColor(Color.parseColor("#FFB3C6"));
        Paint brownStroke = new Paint(Paint.ANTI_ALIAS_FLAG);
        brownStroke.setStyle(Paint.Style.STROKE);
        brownStroke.setStrokeJoin(Paint.Join.ROUND);
        brownStroke.setStrokeCap(Paint.Cap.ROUND);
        brownStroke.setStrokeWidth(Math.max(3f, r * 0.08f));
        brownStroke.setColor(Color.parseColor("#6D4C41"));

        // Pointed Triangular Cat Ears (Left & Right)
        Path leftEar = new Path();
        leftEar.moveTo(cx - r * 0.85f, cy - r * 0.30f);
        leftEar.lineTo(cx - r * 0.72f, cy - r * 1.25f);
        leftEar.lineTo(cx - r * 0.18f, cy - r * 0.68f);
        leftEar.close();
        canvas.drawPath(leftEar, whiteFill);
        canvas.drawPath(leftEar, brownStroke);

        Path rightEar = new Path();
        rightEar.moveTo(cx + r * 0.85f, cy - r * 0.30f);
        rightEar.lineTo(cx + r * 0.72f, cy - r * 1.25f);
        rightEar.lineTo(cx + r * 0.18f, cy - r * 0.68f);
        rightEar.close();
        canvas.drawPath(rightEar, whiteFill);
        canvas.drawPath(rightEar, brownStroke);

        // Pink Inner Cat Ears
        Path leftInner = new Path();
        leftInner.moveTo(cx - r * 0.72f, cy - r * 0.44f);
        leftInner.lineTo(cx - r * 0.65f, cy - r * 1.04f);
        leftInner.lineTo(cx - r * 0.30f, cy - r * 0.66f);
        leftInner.close();
        canvas.drawPath(leftInner, pinkFill);

        Path rightInner = new Path();
        rightInner.moveTo(cx + r * 0.72f, cy - r * 0.44f);
        rightInner.lineTo(cx + r * 0.65f, cy - r * 1.04f);
        rightInner.lineTo(cx + r * 0.30f, cy - r * 0.66f);
        rightInner.close();
        canvas.drawPath(rightInner, pinkFill);

        // Cute Kitten Round-Oval Head
        RectF headRect = new RectF(cx - r, cy - r * 0.80f, cx + r, cy + r * 0.80f);
        canvas.drawOval(headRect, whiteFill);
        canvas.drawOval(headRect, brownStroke);

        // Rosy Cheeks
        canvas.drawCircle(cx - r * 0.56f, cy + r * 0.10f, r * 0.17f, pinkFill);
        canvas.drawCircle(cx + r * 0.56f, cy + r * 0.10f, r * 0.17f, pinkFill);

        // Sparkling Cat Eyes
        Paint eyePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        eyePaint.setColor(Color.parseColor("#4E342E"));
        canvas.drawCircle(cx - r * 0.38f, cy - r * 0.06f, r * 0.11f, eyePaint);
        canvas.drawCircle(cx + r * 0.38f, cy - r * 0.06f, r * 0.11f, eyePaint);
        canvas.drawCircle(cx - r * 0.41f, cy - r * 0.10f, r * 0.038f, whiteFill);
        canvas.drawCircle(cx + r * 0.35f, cy - r * 0.10f, r * 0.038f, whiteFill);

        // Tiny Pink Triangle Cat Nose
        Path nose = new Path();
        nose.moveTo(cx - r * 0.09f, cy + r * 0.05f);
        nose.lineTo(cx + r * 0.09f, cy + r * 0.05f);
        nose.lineTo(cx, cy + r * 0.15f);
        nose.close();
        Paint nosePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        nosePaint.setColor(Color.parseColor("#FF758F"));
        canvas.drawPath(nose, nosePaint);

        // Cute Cat Mouth (ω shape)
        Paint mouthStroke = new Paint(brownStroke);
        mouthStroke.setStrokeWidth(Math.max(2.2f, r * 0.055f));
        RectF leftMouth = new RectF(cx - r * 0.22f, cy + r * 0.08f, cx, cy + r * 0.26f);
        RectF rightMouth = new RectF(cx, cy + r * 0.08f, cx + r * 0.22f, cy + r * 0.26f);
        canvas.drawArc(leftMouth, 15, 150, false, mouthStroke);
        canvas.drawArc(rightMouth, 15, 150, false, mouthStroke);

        // Cute Cat Whiskers (2 on each cheek)
        canvas.drawLine(cx - r * 0.78f, cy - r * 0.02f, cx - r * 1.12f, cy - r * 0.08f, mouthStroke);
        canvas.drawLine(cx - r * 0.78f, cy + r * 0.14f, cx - r * 1.12f, cy + r * 0.18f, mouthStroke);
        canvas.drawLine(cx + r * 0.78f, cy - r * 0.02f, cx + r * 1.12f, cy - r * 0.08f, mouthStroke);
        canvas.drawLine(cx + r * 0.78f, cy + r * 0.14f, cx + r * 1.12f, cy + r * 0.18f, mouthStroke);

        // Text below kitten
        Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        textPaint.setColor(Color.parseColor("#EC4899"));
        textPaint.setTextSize(Math.min(w, h) * 0.064f);
        if (primaryLabel != null && !primaryLabel.isEmpty()) {
            canvas.drawText(primaryLabel, cx, cy + r * 1.38f, textPaint);
        }
        if (secondaryLabel != null && !secondaryLabel.isEmpty()) {
            textPaint.setColor(Color.parseColor("#6B7280"));
            textPaint.setTextSize(Math.min(w, h) * 0.052f);
            canvas.drawText(secondaryLabel, cx, cy + r * 1.76f, textPaint);
        }

        return bmp;
    }

    public static int parseColorSafe(String hex, int fallback) {
        if (hex == null || hex.trim().isEmpty()) return fallback;
        try {
            return Color.parseColor(hex.trim());
        } catch (Exception e) {
            return fallback;
        }
    }

    private static Bitmap decodeSampledFile(File file, int reqWidth, int reqHeight) {
        try {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            try (FileInputStream fis = new FileInputStream(file)) {
                BitmapFactory.decodeStream(fis, null, options);
            }
            options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
            options.inJustDecodeBounds = false;
            try (FileInputStream fis2 = new FileInputStream(file)) {
                return BitmapFactory.decodeStream(fis2, null, options);
            }
        } catch (Exception e) {
            return null;
        }
    }

    private static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;
        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }

    public static void saveBitmapToFile(Bitmap bmp, File file, int quality) {
        try (FileOutputStream fos = new FileOutputStream(file)) {
            bmp.compress(Bitmap.CompressFormat.JPEG, quality, fos);
        } catch (Exception ignored) {
        }
    }
}
