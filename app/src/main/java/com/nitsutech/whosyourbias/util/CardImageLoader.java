package com.nitsutech.whosyourbias.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.util.LruCache;
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
    private static final LruCache<String, Bitmap> memoryCache = new LruCache<>(80);
    private static final ExecutorService executor = Executors.newFixedThreadPool(4);
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    public static void loadCardImage(Context context, IdolCard card, ImageView imageView) {
        if (card == null || imageView == null) return;
        final String tag = card.getId() + "_" + card.getPhotoUri();
        imageView.setTag(tag);

        Bitmap cached = memoryCache.get(tag);
        if (cached != null && !cached.isRecycled()) {
            imageView.setImageBitmap(cached);
            return;
        }

        // Set instant stylized collectible portrait first so UI never flickers
        Bitmap fallback = createStylizedPortrait(card, 480, 600);
        imageView.setImageBitmap(fallback);

        final Context appCtx = context.getApplicationContext();
        executor.execute(() -> {
            Bitmap loaded = fetchOrResolveBitmap(appCtx, card);
            if (loaded != null) {
                memoryCache.put(tag, loaded);
                mainHandler.post(() -> {
                    if (tag.equals(imageView.getTag())) {
                        imageView.setImageBitmap(loaded);
                    }
                });
            } else {
                memoryCache.put(tag, fallback);
            }
        });
    }

    public static Bitmap getCardBitmapSync(Context context, IdolCard card, int width, int height) {
        if (card == null) return null;
        String tag = card.getId() + "_" + card.getPhotoUri();
        Bitmap cached = memoryCache.get(tag);
        if (cached != null && !cached.isRecycled()) return cached;
        Bitmap loaded = fetchOrResolveBitmap(context.getApplicationContext(), card);
        if (loaded != null) return loaded;
        return createStylizedPortrait(card, width, height);
    }

    private static Bitmap fetchOrResolveBitmap(Context context, IdolCard card) {
        try {
            String uri = card.getPhotoUri();
            File cacheDir = new File(context.getCacheDir(), "idol_img_cache");
            if (!cacheDir.exists()) cacheDir.mkdirs();

            if (uri != null && !uri.trim().isEmpty()) {
                uri = uri.trim();
                // 1. Base64 embedded data URI
                if (uri.startsWith("data:image")) {
                    int comma = uri.indexOf(',');
                    if (comma > 0) {
                        byte[] bytes = Base64.decode(uri.substring(comma + 1), Base64.DEFAULT);
                        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                    }
                }
                // 2. Local file path
                if (uri.startsWith("/") || uri.startsWith("file://")) {
                    String path = uri.startsWith("file://") ? uri.substring(7) : uri;
                    File f = new File(path);
                    if (f.exists()) {
                        return decodeSampledFile(f, 600, 600);
                    }
                }
                // 3. Content URI
                if (uri.startsWith("content://")) {
                    try (InputStream is = context.getContentResolver().openInputStream(Uri.parse(uri))) {
                        if (is != null) {
                            return BitmapFactory.decodeStream(is);
                        }
                    }
                }
                // 4. HTTP / HTTPS URL
                if (uri.startsWith("http://") || uri.startsWith("https://")) {
                    File cachedFile = new File(cacheDir, "url_" + Math.abs(uri.hashCode()) + ".jpg");
                    if (cachedFile.exists() && cachedFile.length() > 500) {
                        return decodeSampledFile(cachedFile, 600, 600);
                    }
                    Bitmap dl = downloadBitmap(uri);
                    if (dl != null) {
                        saveBitmapToFile(dl, cachedFile, 85);
                        return dl;
                    }
                }
            }

            // 5. Automatic Wikimedia Commons / Wikipedia portrait lookup for starter idols
            File wikiCache = new File(cacheDir, "wiki_" + card.getId() + ".jpg");
            if (wikiCache.exists() && wikiCache.length() > 500) {
                return decodeSampledFile(wikiCache, 600, 600);
            }
            Bitmap wikiBmp = tryFetchWikipediaPortrait(card);
            if (wikiBmp != null) {
                saveBitmapToFile(wikiBmp, wikiCache, 85);
                return wikiBmp;
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private static Bitmap tryFetchWikipediaPortrait(IdolCard card) {
        String[] candidates = new String[]{
                card.getStageName() + " (" + card.getGroupName() + ")",
                card.getStageName() + " (singer)",
                card.getFullName(),
                card.getStageName(),
                card.getGroupName()
        };
        for (String query : candidates) {
            if (query == null || query.trim().isEmpty() || "Soloist".equalsIgnoreCase(query.trim())) continue;
            try {
                String encoded = URLEncoder.encode(query.trim().replace(" ", "_"), "UTF-8");
                String apiUrl = "https://en.wikipedia.org/api/rest_v1/page/summary/" + encoded;
                HttpURLConnection conn = (HttpURLConnection) new URL(apiUrl).openConnection();
                conn.setConnectTimeout(3500);
                conn.setReadTimeout(3500);
                conn.setRequestProperty("User-Agent", "WhosYourBiasApp/1.0 (Android)");
                if (conn.getResponseCode() == 200) {
                    StringBuilder sb = new StringBuilder();
                    try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                        String line;
                        while ((line = br.readLine()) != null) sb.append(line);
                    }
                    JSONObject json = new JSONObject(sb.toString());
                    JSONObject thumb = json.optJSONObject("thumbnail");
                    if (thumb != null) {
                        String src = thumb.optString("source", "");
                        if (!src.isEmpty()) {
                            Bitmap b = downloadBitmap(src);
                            if (b != null) return b;
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
            conn.setConnectTimeout(4500);
            conn.setReadTimeout(4500);
            conn.setInstanceFollowRedirects(true);
            conn.setRequestProperty("User-Agent", "WhosYourBiasApp/1.0 (Android)");
            if (conn.getResponseCode() == 200) {
                try (InputStream is = conn.getInputStream()) {
                    return BitmapFactory.decodeStream(is);
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    /**
     * Copies a picked gallery/file URI into internal app storage so permissions never expire.
     */
    public static String copyUriToInternalStorage(Context context, Uri sourceUri) {
        try {
            File dir = new File(context.getFilesDir(), "idol_photos");
            if (!dir.exists()) dir.mkdirs();
            File dest = new File(dir, "photo_" + UUID.randomUUID().toString() + ".jpg");
            try (InputStream is = context.getContentResolver().openInputStream(sourceUri)) {
                Bitmap bmp = BitmapFactory.decodeStream(is);
                if (bmp != null) {
                    int maxDim = 720;
                    int w = bmp.getWidth();
                    int h = bmp.getHeight();
                    if (w > maxDim || h > maxDim) {
                        float ratio = Math.min((float) maxDim / w, (float) maxDim / h);
                        bmp = Bitmap.createScaledBitmap(bmp, Math.round(w * ratio), Math.round(h * ratio), true);
                    }
                    saveBitmapToFile(bmp, dest, 85);
                    return dest.getAbsolutePath();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return sourceUri.toString();
    }

    /**
     * Encodes a local photo file as a compact Base64 data URI so it can be embedded inside a shared .wyb file!
     */
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
     * Renders a sleek holographic collectible portrait when offline or before photo loads.
     */
    public static Bitmap createStylizedPortrait(IdolCard card, int width, int height) {
        Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bmp);

        int accent = parseColorSafe(card.getAccentColor(), Color.parseColor("#7B61FF"));
        int darkTop = Color.parseColor("#0F1526");
        int darkMid = blendColors(darkTop, accent, 0.28f);
        int darkBot = Color.parseColor("#0B0E18");

        Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bgPaint.setShader(new LinearGradient(0, 0, width, height,
                new int[]{darkTop, darkMid, darkBot}, null, Shader.TileMode.CLAMP));
        canvas.drawRect(0, 0, width, height, bgPaint);

        // Ambient neon orbs
        Paint orbPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        orbPaint.setColor(withAlpha(accent, 48));
        canvas.drawCircle(width * 0.82f, height * 0.20f, width * 0.36f, orbPaint);
        orbPaint.setColor(withAlpha(Color.parseColor("#00F0FF"), 32));
        canvas.drawCircle(width * 0.18f, height * 0.78f, width * 0.32f, orbPaint);

        // Center Avatar Crest Ring
        float cx = width / 2f;
        float cy = height * 0.43f;
        float radius = Math.min(width, height) * 0.25f;

        Paint ringFill = new Paint(Paint.ANTI_ALIAS_FLAG);
        ringFill.setColor(Color.parseColor("#161E35"));
        canvas.drawCircle(cx, cy, radius, ringFill);

        Paint ringStroke = new Paint(Paint.ANTI_ALIAS_FLAG);
        ringStroke.setStyle(Paint.Style.STROKE);
        ringStroke.setStrokeWidth(6f);
        ringStroke.setColor(accent);
        canvas.drawCircle(cx, cy, radius, ringStroke);

        // Outer dashed/glow ring
        ringStroke.setStrokeWidth(2f);
        ringStroke.setColor(withAlpha(accent, 110));
        canvas.drawCircle(cx, cy, radius + 14f, ringStroke);

        // Stage Name Initials inside Crest
        String initials = getInitials(card.getStageName());
        Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.WHITE);
        textPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTextSize(radius * 0.78f);
        Paint.FontMetrics fm = textPaint.getFontMetrics();
        float textY = cy - (fm.ascent + fm.descent) / 2f;
        canvas.drawText(initials, cx, textY, textPaint);

        // Top Group Pill
        String grp = card.isSoloist() ? "★ SOLOIST" : "✦ " + card.getGroupName().toUpperCase();
        textPaint.setTextSize(width * 0.052f);
        textPaint.setColor(accent);
        canvas.drawText(grp, cx, height * 0.12f, textPaint);

        // Bottom Stage Name & Role Banner inside Portrait
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(width * 0.085f);
        canvas.drawText(card.getStageName(), cx, height * 0.78f, textPaint);

        textPaint.setColor(Color.parseColor("#94A3B8"));
        textPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        textPaint.setTextSize(width * 0.044f);
        String sub = card.getFullName() != null && !card.getFullName().isEmpty() ? card.getFullName() : card.getRole();
        canvas.drawText(sub, cx, height * 0.85f, textPaint);

        return bmp;
    }

    private static String getInitials(String name) {
        if (name == null || name.trim().isEmpty()) return "★";
        String n = name.trim();
        String[] parts = n.split("\\s+");
        if (parts.length >= 2) {
            return ("" + parts[0].charAt(0) + parts[1].charAt(0)).toUpperCase();
        }
        return n.substring(0, Math.min(2, n.length())).toUpperCase();
    }

    public static int parseColorSafe(String hex, int fallback) {
        if (hex == null || hex.trim().isEmpty()) return fallback;
        try {
            return Color.parseColor(hex.trim());
        } catch (Exception e) {
            return fallback;
        }
    }

    private static int withAlpha(int color, int alpha) {
        return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color));
    }

    private static int blendColors(int c1, int c2, float ratio) {
        float ir = 1f - ratio;
        int r = Math.round(Color.red(c1) * ir + Color.red(c2) * ratio);
        int g = Math.round(Color.green(c1) * ir + Color.green(c2) * ratio);
        int b = Math.round(Color.blue(c1) * ir + Color.blue(c2) * ratio);
        return Color.rgb(r, g, b);
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
