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
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CardImageLoader {
    // Large cache (600 entries) so loaded idol photos NEVER get evicted and never flicker back to placeholder
    private static final LruCache<String, Bitmap> rawMemoryCache = new LruCache<>(350);
    private static final LruCache<String, Bitmap> fittedMemoryCache = new LruCache<>(600);
    private static final ExecutorService executor = Executors.newFixedThreadPool(6);
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

    public static void loadCardImageWithSize(Context context, IdolCard card, ImageView imageView, int targetW, int targetH) {
        if (card == null || imageView == null) return;
        imageView.setScaleType(ImageView.ScaleType.FIT_XY);

        final String rawKey = card.getId() + "_" + (card.getPhotoUri() != null ? card.getPhotoUri() : "");
        final String fittedKey = rawKey + "_" + targetW + "x" + targetH;

        Bitmap cachedFitted = fittedMemoryCache.get(fittedKey);
        if (cachedFitted != null && !cachedFitted.isRecycled()) {
            imageView.setImageBitmap(cachedFitted);
            return;
        }

        Bitmap cachedRaw = rawMemoryCache.get(rawKey);
        if (cachedRaw != null && !cachedRaw.isRecycled()) {
            Bitmap fitted = createUncroppedFittedPhoto(cachedRaw, targetW, targetH);
            fittedMemoryCache.put(fittedKey, fitted);
            imageView.setImageBitmap(fitted);
            return;
        }

        // Fast synchronous check if already on disk cache so we don't even show a loading screen for cached files
        final Context appCtx = context.getApplicationContext();
        File diskCached = getExistingDiskCacheFile(appCtx, card);
        if (diskCached != null) {
            Bitmap diskBmp = decodeSampledFile(diskCached, 540, 540);
            if (diskBmp != null) {
                rawMemoryCache.put(rawKey, diskBmp);
                Bitmap fitted = createUncroppedFittedPhoto(diskBmp, targetW, targetH);
                fittedMemoryCache.put(fittedKey, fitted);
                imageView.setImageBitmap(fitted);
                return;
            }
        }

        // Only show cute kawaii loading art when downloading from network for the first time
        Bitmap cuteLoader = createCuteKawaiiLoadingArt(card, targetW, targetH);
        imageView.setImageBitmap(cuteLoader);

        executor.execute(() -> {
            Bitmap loaded = fetchOrResolveBitmap(appCtx, card);
            if (loaded != null) {
                rawMemoryCache.put(rawKey, loaded);
                final Bitmap fitted = createUncroppedFittedPhoto(loaded, targetW, targetH);
                fittedMemoryCache.put(fittedKey, fitted);
                mainHandler.post(() -> {
                    if (fittedKey.equals(imageView.getTag())) {
                        imageView.setImageBitmap(fitted);
                    }
                });
            } else {
                final Bitmap fallback = createStylizedPortrait(card, targetW, targetH);
                mainHandler.post(() -> {
                    if (fittedKey.equals(imageView.getTag())) {
                        imageView.setImageBitmap(fallback);
                    }
                });
            }
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
     * Renders 100% of the original photo (`fitCenter`) over a soft, dreamy blurred backdrop of the same photo
     * so NO head, forehead, eyes, or body is EVER cut off in any aspect ratio!
     */
    public static Bitmap createUncroppedFittedPhoto(Bitmap src, int targetW, int targetH) {
        if (src == null) return null;
        int sW = src.getWidth();
        int sH = src.getHeight();
        if (sW <= 0 || sH <= 0) return src;

        Bitmap out = Bitmap.createBitmap(targetW, targetH, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(out);

        // 1. Fast soft blurred background fill from tiny downscaled bitmap (takes < 0.3ms)
        Bitmap tiny = Bitmap.createScaledBitmap(src, 18, 18, true);
        Bitmap blurredBg = Bitmap.createScaledBitmap(tiny, targetW, targetH, true);
        Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        canvas.drawBitmap(blurredBg, 0, 0, bgPaint);

        // Soft pastel veil over the blurred background so the full photo pops cleanly
        Paint veil = new Paint();
        veil.setColor(Color.argb(65, 255, 248, 252));
        canvas.drawRect(0, 0, targetW, targetH, veil);

        // 2. Fit 100% of the source image inside targetW x targetH without cropping a single pixel!
        float scale = Math.min((float) targetW / (float) sW, (float) targetH / (float) sH);
        float drawW = sW * scale;
        float drawH = sH * scale;
        float left = (targetW - drawW) / 2f;
        float top = (targetH - drawH) / 2f;

        RectF dst = new RectF(left, top, left + drawW, top + drawH);
        canvas.drawBitmap(src, null, dst, bgPaint);

        return out;
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
        String cleanStage = card.getStageName().replaceAll("\\s*\\([^)]*\\)", "").trim();
        String[] candidates = card.isSoloist()
                ? new String[]{cleanStage, card.getFullName()}
                : new String[]{cleanStage + " (" + card.getGroupName() + ")", card.getGroupName() + " " + cleanStage, cleanStage};

        for (String query : candidates) {
            if (query == null || query.trim().isEmpty()) continue;
            try {
                String encoded = URLEncoder.encode(query.trim(), "UTF-8");
                String apiUrl = "https://kpop.fandom.com/api.php?action=query&generator=search&gsrsearch="
                        + encoded + "&gsrlimit=2&prop=pageimages&piprop=thumbnail|original&pithumbsize=600&format=json";
                HttpURLConnection conn = (HttpURLConnection) new URL(apiUrl).openConnection();
                conn.setConnectTimeout(4500);
                conn.setReadTimeout(4500);
                conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 14)");
                if (conn.getResponseCode() == 200) {
                    StringBuilder sb = new StringBuilder();
                    try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                        String line;
                        while ((line = br.readLine()) != null) sb.append(line);
                    }
                    JSONObject json = new JSONObject(sb.toString());
                    JSONObject qObj = json.optJSONObject("query");
                    if (qObj != null) {
                        JSONObject pages = qObj.optJSONObject("pages");
                        if (pages != null && pages.names() != null) {
                            for (int i = 0; i < pages.names().length(); i++) {
                                JSONObject page = pages.optJSONObject(pages.names().optString(i));
                                if (page != null) {
                                    JSONObject thumb = page.optJSONObject("thumbnail");
                                    String src = thumb != null ? thumb.optString("source", "") : "";
                                    if (!src.isEmpty() && !src.contains("Placeholder")) {
                                        Bitmap b = downloadBitmap(src);
                                        if (b != null) return b;
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    private static Bitmap downloadBitmap(String urlStr) {
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            conn.setInstanceFollowRedirects(true);
            conn.setRequestProperty("User-Agent", "WhosYourBiasApp/2.0 (Android)");
            if (conn.getResponseCode() == 200) {
                try (InputStream is = conn.getInputStream()) {
                    return BitmapFactory.decodeStream(is);
                }
            }
        } catch (Exception ignored) {
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
     * Cute Kawaii Pastel Bunny & Sparkles Loading Illustration (shown only while downloading a photo for the first time).
     */
    public static Bitmap createCuteKawaiiLoadingArt(IdolCard card, int width, int height) {
        int w = Math.max(240, width);
        int h = Math.max(240, height);
        Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bmp);

        // Soft Pastel Pink to Butter Cream Gradient
        Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bgPaint.setShader(new LinearGradient(0, 0, w, h,
                new int[]{Color.parseColor("#FFF0F6"), Color.parseColor("#FFFBEB"), Color.parseColor("#F3E8FF")},
                null, Shader.TileMode.CLAMP));
        canvas.drawRect(0, 0, w, h, bgPaint);

        // Cute Polka Dots pattern
        Paint dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        dotPaint.setColor(Color.argb(90, 255, 255, 255));
        int step = Math.max(28, Math.min(w, h) / 7);
        for (int y = step / 2; y < h; y += step) {
            for (int x = step / 2; x < w; x += step) {
                canvas.drawCircle(x, y, step * 0.18f, dotPaint);
            }
        }

        float cx = w / 2f;
        float cy = h * 0.44f;
        float r = Math.min(w, h) * 0.20f;

        // Cute Kawaii Bunny Ears & Head
        Paint whiteFill = new Paint(Paint.ANTI_ALIAS_FLAG);
        whiteFill.setColor(Color.WHITE);
        Paint pinkFill = new Paint(Paint.ANTI_ALIAS_FLAG);
        pinkFill.setColor(Color.parseColor("#FFB3C6"));
        Paint brownStroke = new Paint(Paint.ANTI_ALIAS_FLAG);
        brownStroke.setStyle(Paint.Style.STROKE);
        brownStroke.setStrokeWidth(Math.max(3f, r * 0.08f));
        brownStroke.setColor(Color.parseColor("#7C4D3A"));

        // Left & Right Bunny Ears
        RectF leftEar = new RectF(cx - r * 0.72f, cy - r * 1.65f, cx - r * 0.16f, cy - r * 0.35f);
        RectF rightEar = new RectF(cx + r * 0.16f, cy - r * 1.65f, cx + r * 0.72f, cy - r * 0.35f);
        canvas.drawRoundRect(leftEar, r * 0.3f, r * 0.3f, whiteFill);
        canvas.drawRoundRect(leftEar, r * 0.3f, r * 0.3f, brownStroke);
        canvas.drawRoundRect(rightEar, r * 0.3f, r * 0.3f, whiteFill);
        canvas.drawRoundRect(rightEar, r * 0.3f, r * 0.3f, brownStroke);

        // Inner pink ears
        RectF leftInner = new RectF(cx - r * 0.58f, cy - r * 1.42f, cx - r * 0.30f, cy - r * 0.55f);
        RectF rightInner = new RectF(cx + r * 0.30f, cy - r * 1.42f, cx + r * 0.58f, cy - r * 0.55f);
        canvas.drawRoundRect(leftInner, r * 0.2f, r * 0.2f, pinkFill);
        canvas.drawRoundRect(rightInner, r * 0.2f, r * 0.2f, pinkFill);

        // Bunny Face Oval
        RectF headRect = new RectF(cx - r, cy - r * 0.78f, cx + r, cy + r * 0.78f);
        canvas.drawOval(headRect, whiteFill);
        canvas.drawOval(headRect, brownStroke);

        // Cute Rosy Cheeks
        canvas.drawCircle(cx - r * 0.55f, cy + r * 0.08f, r * 0.18f, pinkFill);
        canvas.drawCircle(cx + r * 0.55f, cy + r * 0.08f, r * 0.18f, pinkFill);

        // Eyes & Nose
        Paint eyePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        eyePaint.setColor(Color.parseColor("#5C3728"));
        canvas.drawCircle(cx - r * 0.36f, cy - r * 0.08f, r * 0.10f, eyePaint);
        canvas.drawCircle(cx + r * 0.36f, cy - r * 0.08f, r * 0.10f, eyePaint);

        // Text below bunny
        Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        textPaint.setColor(Color.parseColor("#EC4899"));
        textPaint.setTextSize(Math.min(w, h) * 0.065f);
        canvas.drawText("✨ Cargando... ✨", cx, cy + r * 1.38f, textPaint);

        if (card != null && card.getStageName() != null) {
            textPaint.setColor(Color.parseColor("#4B5563"));
            textPaint.setTextSize(Math.min(w, h) * 0.055f);
            canvas.drawText(card.getStageName(), cx, cy + r * 1.78f, textPaint);
        }

        return bmp;
    }

    /**
     * Soft Pastel Collectible Portrait when offline.
     */
    public static Bitmap createStylizedPortrait(IdolCard card, int width, int height) {
        return createCuteKawaiiLoadingArt(card, width, height);
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
