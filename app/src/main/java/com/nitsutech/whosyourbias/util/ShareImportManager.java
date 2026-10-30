package com.nitsutech.whosyourbias.util;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.net.Uri;
import android.util.Base64;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import com.nitsutech.whosyourbias.data.BiasRepository;
import com.nitsutech.whosyourbias.data.CustomMatch;
import com.nitsutech.whosyourbias.data.IdolCard;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class ShareImportManager {
    private static final String CODE_PREFIX = "WYB1-";
    private static final Pattern CODE_PATTERN = Pattern.compile("WYB1-[A-Za-z0-9_\\-]+");

    public static class SharedPayload {
        public String type; // "MATCH" or "PACK"
        public String packTitle;
        public CustomMatch customMatch;
        public List<IdolCard> cards = new ArrayList<>();
    }

    /**
     * Builds a JSON payload containing either a CustomMatch + its IdolCards, or a Pack of IdolCards.
     */
    public static JSONObject createPayloadJson(String packTitle, CustomMatch match, List<IdolCard> cards, boolean embedLocalPhotos) {
        try {
            JSONObject root = new JSONObject();
            root.put("version", 1);
            root.put("type", match != null ? "MATCH" : "PACK");
            root.put("packTitle", packTitle != null ? packTitle : "Shared Idols Pack");
            if (match != null) {
                root.put("match", match.toJson());
            }
            JSONArray cardsArr = new JSONArray();
            if (cards != null) {
                for (IdolCard c : cards) {
                    JSONObject cJson = c.toJson();
                    if (embedLocalPhotos) {
                        cJson.put("photoUri", CardImageLoader.encodeLocalPhotoAsDataUri(c.getPhotoUri()));
                        JSONArray pcArr = new JSONArray();
                        for (String pc : c.getPhotocards()) {
                            pcArr.put(CardImageLoader.encodeLocalPhotoAsDataUri(pc));
                        }
                        cJson.put("photocards", pcArr);
                    } else if (c.getPhotoUri() != null && !c.getPhotoUri().startsWith("http")) {
                        // Keep text codes compact by omitting local file paths
                        cJson.put("photoUri", "");
                        JSONArray pcArr = new JSONArray();
                        for (String pc : c.getPhotocards()) {
                            if (pc != null && pc.startsWith("http")) pcArr.put(pc);
                        }
                        cJson.put("photocards", pcArr);
                    }
                    cardsArr.put(cJson);
                }
            }
            root.put("cards", cardsArr);
            return root;
        } catch (Exception e) {
            return new JSONObject();
        }
    }

    /**
     * Encodes payload into a compact GZIP + Base64 URL-safe code: WYB1-...
     */
    public static String encodeToCode(JSONObject payloadJson) {
        try {
            byte[] raw = payloadJson.toString().getBytes(StandardCharsets.UTF_8);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (GZIPOutputStream gzip = new GZIPOutputStream(baos)) {
                gzip.write(raw);
            }
            String b64 = Base64.encodeToString(baos.toByteArray(), Base64.URL_SAFE | Base64.NO_WRAP | Base64.NO_PADDING);
            return CODE_PREFIX + b64;
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Decodes either a WYB1-... code, a deep link URL, or raw JSON string into a SharedPayload.
     */
    public static SharedPayload decodeFromTextOrCode(Context context, String input) {
        if (input == null || input.trim().isEmpty()) return null;
        try {
            String trimmed = input.trim();
            String jsonStr = null;

            if (trimmed.startsWith("{")) {
                jsonStr = trimmed;
            } else {
                Matcher m = CODE_PATTERN.matcher(trimmed);
                String code = null;
                if (m.find()) {
                    code = m.group(0);
                } else if (trimmed.startsWith(CODE_PREFIX)) {
                    code = trimmed;
                }
                if (code != null && code.startsWith(CODE_PREFIX)) {
                    String b64 = code.substring(CODE_PREFIX.length());
                    byte[] compressed = Base64.decode(b64, Base64.URL_SAFE | Base64.NO_WRAP | Base64.NO_PADDING);
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    try (GZIPInputStream gis = new GZIPInputStream(new ByteArrayInputStream(compressed))) {
                        byte[] buf = new byte[2048];
                        int len;
                        while ((len = gis.read(buf)) > 0) {
                            out.write(buf, 0, len);
                        }
                    }
                    jsonStr = new String(out.toByteArray(), StandardCharsets.UTF_8);
                }
            }

            if (jsonStr == null) return null;
            return parsePayloadJson(context, new JSONObject(jsonStr));
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Reads a .wyb file URI (from WhatsApp attachment or file manager) into a SharedPayload.
     */
    public static SharedPayload decodeFromUri(Context context, Uri uri) {
        if (uri == null) return null;
        try {
            // Check if it's a deep link with ?code=WYB1-...
            String codeParam = uri.getQueryParameter("code");
            if (codeParam != null && !codeParam.isEmpty()) {
                return decodeFromTextOrCode(context, codeParam);
            }

            try (InputStream is = context.getContentResolver().openInputStream(uri)) {
                if (is == null) return null;
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                byte[] buf = new byte[4096];
                int n;
                while ((n = is.read(buf)) > 0) {
                    baos.write(buf, 0, n);
                }
                String content = new String(baos.toByteArray(), StandardCharsets.UTF_8).trim();
                return decodeFromTextOrCode(context, content);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static SharedPayload parsePayloadJson(Context context, JSONObject root) {
        SharedPayload payload = new SharedPayload();
        payload.type = root.optString("type", "PACK");
        payload.packTitle = root.optString("packTitle", "Shared Idols Pack");
        if (root.has("match")) {
            payload.customMatch = CustomMatch.fromJson(root.optJSONObject("match"));
        }
        JSONArray arr = root.optJSONArray("cards");
        if (arr != null) {
            for (int i = 0; i < arr.length(); i++) {
                JSONObject cObj = arr.optJSONObject(i);
                if (cObj != null) {
                    payload.cards.add(IdolCard.fromJson(cObj));
                }
            }
        }
        // If match references starter cards not explicitly in cards array, resolve them from repo
        if (payload.customMatch != null && payload.cards.isEmpty()) {
            BiasRepository repo = BiasRepository.getInstance(context);
            for (String cid : payload.customMatch.getCardIds()) {
                IdolCard existing = repo.getIdolById(cid);
                if (existing != null) payload.cards.add(existing);
            }
        }
        return payload;
    }

    /**
     * Shares a .wyb file directly via Android Share Sheet / WhatsApp so tapping the file opens the app.
     */
    public static void shareAsWybFile(Activity activity, String title, CustomMatch match, List<IdolCard> cards) {
        try {
            JSONObject json = createPayloadJson(title, match, cards, true);
            File sharedDir = new File(activity.getCacheDir(), "shared");
            if (!sharedDir.exists()) sharedDir.mkdirs();

            String safeName = (title != null ? title : "WhosYourBias")
                    .replaceAll("[^a-zA-Z0-9_\\-]", "_")
                    .replaceAll("_+", "_");
            if (safeName.length() > 32) safeName = safeName.substring(0, 32);
            File wybFile = new File(sharedDir, safeName + ".wyb");

            try (FileOutputStream fos = new FileOutputStream(wybFile)) {
                fos.write(json.toString().getBytes(StandardCharsets.UTF_8));
            }

            Uri uri = FileProvider.getUriForFile(activity, activity.getPackageName() + ".fileprovider", wybFile);
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("application/octet-stream");
            intent.putExtra(Intent.EXTRA_STREAM, uri);
            String caption = "🔥 Who's your Bias — \"" + title + "\" (" + (cards != null ? cards.size() : 0) + " Idols)\n"
                    + "👉 Tap the attached .wyb file to open and play in Who's your Bias!";
            intent.putExtra(Intent.EXTRA_TEXT, caption);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            activity.startActivity(Intent.createChooser(intent, "Share .wyb File"));
        } catch (Exception e) {
            Toast.makeText(activity, "Error sharing file: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Shares a WhatsApp text message containing both a clickable deep link AND the WYB1-... code.
     */
    public static void shareAsWhatsAppCode(Activity activity, String title, CustomMatch match, List<IdolCard> cards) {
        try {
            JSONObject json = createPayloadJson(title, match, cards, false);
            String code = encodeToCode(json);
            String deepLink = "https://whosyourbias.nitsutech.com/open?code=" + code;
            String message = "👑 *Who's your Bias* — *" + title + "* (" + (cards != null ? cards.size() : 0) + " Idols)\n\n"
                    + "📲 Tap link to open directly in app:\n" + deepLink + "\n\n"
                    + "📋 Or copy this message and tap 'Import' in the app:\n" + code;

            ClipboardManager cb = (ClipboardManager) activity.getSystemService(Context.CLIPBOARD_SERVICE);
            if (cb != null) {
                cb.setPrimaryClip(ClipData.newPlainText("Who's your Bias Code", code));
            }

            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_TEXT, message);
            activity.startActivity(Intent.createChooser(intent, "Share via WhatsApp"));
        } catch (Exception e) {
            Toast.makeText(activity, "Error creating share code: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Generates a high-resolution Victory Poster PNG and shares it via WhatsApp / Instagram Stories.
     */
    public static void shareVictoryPoster(Activity activity, IdolCard champion, IdolCard runnerUp,
                                          List<IdolCard> semiFinalists, String tournamentTitle, String gameMode) {
        try {
            int w = 1080;
            int h = 1520;
            Bitmap poster = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(poster);

            int accent = CardImageLoader.parseColorSafe(champion.getAccentColor(), Color.parseColor("#7B61FF"));
            Paint bg = new Paint(Paint.ANTI_ALIAS_FLAG);
            bg.setShader(new LinearGradient(0, 0, w, h,
                    new int[]{Color.parseColor("#0B0E17"), Color.parseColor("#1B1638"), Color.parseColor("#0D1528")},
                    null, Shader.TileMode.CLAMP));
            canvas.drawRect(0, 0, w, h, bg);

            // Decorative Neon Glow Border
            Paint border = new Paint(Paint.ANTI_ALIAS_FLAG);
            border.setStyle(Paint.Style.STROKE);
            border.setStrokeWidth(8f);
            border.setColor(Color.parseColor("#FFD166"));
            canvas.drawRoundRect(new RectF(36, 36, w - 36, h - 36), 44, 44, border);

            Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
            text.setTextAlign(Paint.Align.CENTER);

            // Header Slogan
            text.setColor(Color.parseColor("#00F0FF"));
            text.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
            text.setTextSize(34f);
            canvas.drawText("WHO IS YOUR BIAS? I AM YOUR BIAS, IT'S ME!", w / 2f, 115f, text);

            text.setColor(Color.parseColor("#FFD166"));
            text.setTextSize(48f);
            canvas.drawText("👑 SUPREME BIAS CHAMPION 👑", w / 2f, 185f, text);

            text.setColor(Color.parseColor("#94A3B8"));
            text.setTextSize(30f);
            canvas.drawText(tournamentTitle + " • " + gameMode, w / 2f, 240f, text);

            // Champion Portrait Card
            Bitmap champBmp = CardImageLoader.getCardBitmapSync(activity, champion, 560, 640);
            if (champBmp != null) {
                Bitmap scaled = Bitmap.createScaledBitmap(champBmp, 560, 640, true);
                RectF dst = new RectF(w / 2f - 280, 285, w / 2f + 280, 925);
                canvas.drawBitmap(scaled, null, dst, null);
                border.setStrokeWidth(6f);
                border.setColor(accent);
                canvas.drawRoundRect(dst, 28, 28, border);
            }

            // Champion Name & Info
            text.setColor(Color.WHITE);
            text.setTextSize(72f);
            canvas.drawText(champion.getStageName(), w / 2f, 1020f, text);

            text.setColor(accent);
            text.setTextSize(40f);
            String grpLine = (champion.isSoloist() ? "Soloist" : champion.getGroupName()) + "  •  " + champion.getCountryWithFlag();
            canvas.drawText(grpLine, w / 2f, 1080f, text);

            text.setColor(Color.parseColor("#CBD5E1"));
            text.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
            text.setTextSize(32f);
            canvas.drawText(champion.getFullName() + " • Age " + champion.getCalculatedAge() + " (" + champion.getZodiacSign() + ")", w / 2f, 1135f, text);

            // Podium Box
            Paint boxPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            boxPaint.setColor(Color.parseColor("#182039"));
            canvas.drawRoundRect(new RectF(80, 1190, w - 80, 1410), 28, 28, boxPaint);

            text.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
            text.setTextSize(32f);
            text.setColor(Color.parseColor("#FFD166"));
            String rUp = runnerUp != null ? runnerUp.getStageName() + " (" + runnerUp.getGroupName() + ")" : "N/A";
            canvas.drawText("🥈 2nd Place: " + rUp, w / 2f, 1255f, text);

            if (semiFinalists != null && !semiFinalists.isEmpty()) {
                StringBuilder sb = new StringBuilder("🏅 Top 4: ");
                for (int i = 0; i < semiFinalists.size(); i++) {
                    if (i > 0) sb.append("  •  ");
                    sb.append(semiFinalists.get(i).getStageName());
                }
                text.setColor(Color.parseColor("#00F0FF"));
                text.setTextSize(28f);
                canvas.drawText(sb.toString(), w / 2f, 1320f, text);
            }

            text.setColor(Color.parseColor("#64748B"));
            text.setTextSize(24f);
            canvas.drawText("Played on Who's your Bias • com.nitsutech.whosyourbias", w / 2f, 1380f, text);

            File sharedDir = new File(activity.getCacheDir(), "shared");
            if (!sharedDir.exists()) sharedDir.mkdirs();
            File imgFile = new File(sharedDir, "Bias_Champion_" + champion.getStageName().replaceAll("\\W+", "") + ".jpg");
            CardImageLoader.saveBitmapToFile(poster, imgFile, 92);

            Uri uri = FileProvider.getUriForFile(activity, activity.getPackageName() + ".fileprovider", imgFile);
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("image/jpeg");
            intent.putExtra(Intent.EXTRA_STREAM, uri);
            intent.putExtra(Intent.EXTRA_TEXT, "👑 My Supreme Bias in \"" + tournamentTitle + "\" is " + champion.getStageName() + " (" + champion.getGroupName() + ")! Who is your bias? It's me!");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            activity.startActivity(Intent.createChooser(intent, "Share Victory Poster"));
        } catch (Exception e) {
            Toast.makeText(activity, "Error sharing poster: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}
