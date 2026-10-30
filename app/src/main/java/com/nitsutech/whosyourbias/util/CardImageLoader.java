package com.nitsutech.whosyourbias.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.util.LruCache;
import android.view.ViewGroup;
import android.widget.ImageView;

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
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    public static void loadCardImage(Context context, IdolCard card, ImageView imageView) {
        if (card == null || imageView == null) return;
        int[] ratio = inferTargetCanvasSize(imageView);
        loadCardImageWithSize(context, card, imageView, ratio[0], ratio[1]);
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
        final String rawKey = temp.getId() + "_" + effectiveUri;
        Bitmap cachedRaw = rawMemoryCache.get(rawKey);
        if (cachedRaw != null && !cachedRaw.isRecycled()) {
            zoomImageView.setImageBitmap(cachedRaw);
            return;
        }
        zoomImageView.setImageBitmap(createCuteKawaiiLoadingArt(temp, 480, 620));
        final Context appCtx = context.getApplicationContext();
        networkExecutor.execute(() -> {
            Bitmap loaded = fetchOrResolveBitmap(appCtx, temp);
            if (loaded != null) {
                rawMemoryCache.put(rawKey, loaded);
                mainHandler.post(() -> zoomImageView.setImageBitmap(loaded));
            }
        });
    }

    public static void loadCardImageWithSize(Context context, IdolCard card, ImageView imageView, int targetW, int targetH) {
        if (card == null || imageView == null) return;
        imageView.setScaleType(ImageView.ScaleType.FIT_XY);

        final String rawKey = card.getId() + "_" + (card.getPhotoUri() != null ? card.getPhotoUri() : "");
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
        Bitmap cuteLoader = createCuteKawaiiLoadingArt(card, targetW, targetH);
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

    public static Bitmap createPhysicalPhotocardBitmap(Bitmap src, int targetW, int targetH) {
        if (src == null) return null;
        int sW = src.getWidth();
        int sH = src.getHeight();
        if (sW <= 0 || sH <= 0) return src;

        Bitmap out = Bitmap.createBitmap(targetW, targetH, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(out);

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG | Paint.DITHER_FLAG);

        // Smart scale to fill full card edge-to-edge (Full HD Physical Photocard)
        float scale = Math.max((float) targetW / (float) sW, (float) targetH / (float) sH);
        float drawW = sW * scale;
        float drawH = sH * scale;

        // Horizontally center
        float left = (targetW - drawW) / 2f;

        // Vertically frame with slight upper bias (22%) so idol hair/head isn't cut off
        float top;
        if (drawH > targetH) {
            top = (targetH - drawH) * 0.22f;
        } else {
            top = (targetH - drawH) / 2f;
        }

        RectF dst = new RectF(left, top, left + drawW, top + drawH);
        canvas.drawBitmap(src, null, dst, paint);

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
        String rawKey = card.getId() + "_" + (card.getPhotoUri() != null ? card.getPhotoUri() : "");
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

    private static File getExistingDiskCacheFile(Context context, IdolCard card) {
        try {
            String uri = card.getPhotoUri();
            File cacheDir = new File(context.getCacheDir(), "idol_img_cache");
            if (uri != null && !uri.trim().isEmpty()) {
                uri = uri.trim();
                if (uri.startsWith("/") || uri.startsWith("file://")) {
                    File f = new File(uri.startsWith("file://") ? uri.substring(7) : uri);
                    if (f.exists() && f.length() > 200) return f;
                } else if (uri.startsWith("http://") || uri.startsWith("https://")) {
                    File f = new File(cacheDir, "url_" + Math.abs(uri.hashCode()) + ".jpg");
                    if (f.exists() && f.length() > 500) return f;
                }
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
                    File cachedFile = new File(cacheDir, "url_" + Math.abs(uri.hashCode()) + ".jpg");
                    if (cachedFile.exists() && cachedFile.length() > 500) {
                        return decodeSampledFile(cachedFile, 600, 600);
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
     * Cute Kawaii Pastel Kitten (Gatito ILLIT-style) Loading Illustration, cached by size for 0ms UI performance!
     */
    public static Bitmap createCuteKawaiiLoadingArt(IdolCard card, int width, int height) {
        int w = Math.max(240, width);
        int h = Math.max(240, height);
        String key = "kitten_loader_" + w + "x" + h;
        Bitmap cached = kawaiiLoaderCache.get(key);
        if (cached != null && !cached.isRecycled()) {
            return cached;
        }
        Bitmap bmp = drawCuteKittenCardArt(w, h, "✨ Cargando... ✨", "🐾 ILLIT Vibe");
        kawaiiLoaderCache.put(key, bmp);
        return bmp;
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
