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
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.net.Uri;
import android.util.Base64;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import com.nitsutech.whosyourbias.R;
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
                    } else {
                        // Stripping extra photocards keeps text codes/URLs ultra-compact (under 300 chars)
                        // so WhatsApp, SMS, and Android deep-link parsers NEVER truncate or break the link!
                        cJson.remove("photocards");
                        cJson.remove("unlockedPhotocards");
                        if (c.getPhotoUri() != null && !c.getPhotoUri().startsWith("http")) {
                            cJson.put("photoUri", "");
                        }
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
     * Reads a .wyb file URI (from WhatsApp attachment or file manager) into a SharedPayload,
     * or parses deep link schemes like wiyb://import?code=WYB1-... or wyb://...
     */
    public static SharedPayload decodeFromUri(Context context, Uri uri) {
        if (uri == null) return null;
        try {
            // 1. Check if it's a deep link or web link with ?code=WYB1-... or ?c=WYB1-...
            String codeParam = null;
            try {
                codeParam = uri.getQueryParameter("code");
                if (codeParam == null || codeParam.isEmpty()) {
                    codeParam = uri.getQueryParameter("c");
                }
            } catch (Exception ignored) {}
            if (codeParam != null && !codeParam.isEmpty()) {
                SharedPayload payload = decodeFromTextOrCode(context, codeParam);
                if (payload != null && (!payload.cards.isEmpty() || payload.customMatch != null)) {
                    return payload;
                }
            }

            // 2. Check if the full URI string itself contains a valid WYB1-... code (e.g. wiyb://import?code=... or wiyb://WYB1-...)
            String uriString = uri.toString();
            SharedPayload fromUri = decodeFromTextOrCode(context, uriString);
            if (fromUri != null && (!fromUri.cards.isEmpty() || fromUri.customMatch != null)) {
                return fromUri;
            }

            // 3. Fall back to reading as a file stream (.wyb)
            if (context != null && context.getContentResolver() != null) {
                try (InputStream is = context.getContentResolver().openInputStream(uri)) {
                    if (is != null) {
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        byte[] buf = new byte[4096];
                        int n;
                        while ((n = is.read(buf)) > 0) {
                            baos.write(buf, 0, n);
                        }
                        String content = new String(baos.toByteArray(), StandardCharsets.UTF_8).trim();
                        return decodeFromTextOrCode(context, content);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return null;
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
        // If match references cards, ensure full roster is resolved using repo when available
        if (payload.customMatch != null && context != null) {
            BiasRepository repo = BiasRepository.getInstance(context);
            List<IdolCard> resolved = new ArrayList<>();
            for (String cid : payload.customMatch.getCardIds()) {
                IdolCard found = null;
                for (IdolCard inc : payload.cards) {
                    if (inc.getId().equals(cid)) {
                        found = inc;
                        break;
                    }
                }
                if (found == null) {
                    found = repo.getIdolById(cid);
                }
                if (found != null) {
                    resolved.add(found);
                }
            }
            if (!resolved.isEmpty()) {
                payload.cards = resolved;
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
            String caption = activity.getString(R.string.share_msg_caption, title, (cards != null ? cards.size() : 0));
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
            String clickableLink = "https://ismael6499.github.io/whos-your-bias/?code=" + code;

            String message = activity.getString(R.string.share_msg_code, title, (cards != null ? cards.size() : 0))
                    + "🔗 " + clickableLink + "\n\n"
                    + "📥 *Download App / Descargar APK:*\nhttps://is.gd/whosyourbias";

            ClipboardManager cb = (ClipboardManager) activity.getSystemService(Context.CLIPBOARD_SERVICE);
            if (cb != null) {
                cb.setPrimaryClip(ClipData.newPlainText("Who's your Bias Code", clickableLink));
            }

            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_TEXT, message);
            activity.startActivity(Intent.createChooser(intent, activity.getString(R.string.action_share_whatsapp)));
        } catch (Exception e) {
            Toast.makeText(activity, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Generates a high-resolution Victory Poster PNG styled with the active App Theme colors
     * and shares it via WhatsApp / Instagram Stories.
     */
    public static void shareVictoryPoster(Activity activity, IdolCard champion, IdolCard runnerUp,
                                          List<IdolCard> semiFinalists, String tournamentTitle, String gameMode) {
        try {
            int w = 1080;
            int h = 1560;
            Bitmap poster = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(poster);

            ThemeManager.AppTheme theme = ThemeManager.getActiveTheme(activity);
            boolean isDark = ThemeManager.isColorDark(theme.backgroundColor);

            Paint bg = new Paint(Paint.ANTI_ALIAS_FLAG);
            bg.setShader(new LinearGradient(0, 0, w, h,
                    new int[]{theme.backgroundColor, theme.surfaceColor, theme.backgroundColor},
                    null, Shader.TileMode.CLAMP));
            canvas.drawRect(0, 0, w, h, bg);

            // Soft Decorative Pastel Circles in corners
            Paint decoPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            decoPaint.setColor(Color.argb(28, Color.red(theme.primaryColor), Color.green(theme.primaryColor), Color.blue(theme.primaryColor)));
            canvas.drawCircle(120, 140, 240, decoPaint);
            decoPaint.setColor(Color.argb(28, Color.red(theme.secondaryColor), Color.green(theme.secondaryColor), Color.blue(theme.secondaryColor)));
            canvas.drawCircle(w - 120, h - 160, 260, decoPaint);

            // Outer Theme Frame Border
            Paint border = new Paint(Paint.ANTI_ALIAS_FLAG);
            border.setStyle(Paint.Style.STROKE);
            border.setStrokeWidth(10f);
            border.setColor(theme.primaryColor);
            canvas.drawRoundRect(new RectF(42, 42, w - 42, h - 42), 52, 52, border);

            Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
            text.setTextAlign(Paint.Align.CENTER);

            // 1. Top Pill Slogan
            text.setColor(theme.primaryColor);
            text.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
            drawFittedCenteredText(canvas, "✨ WHO IS YOUR BIAS? I AM YOUR BIAS, IT'S ME! ✨", w / 2f, 118f, text, 30f, w - 140f);

            text.setColor(theme.textColor);
            text.setTypeface(Typeface.create("sans-serif-black", Typeface.BOLD));
            drawFittedCenteredText(canvas, activity.getString(R.string.poster_supreme_bias), w / 2f, 182f, text, 48f, w - 140f);

            String friendlyMode = formatFriendlyGameMode(activity, gameMode);
            String cleanTourneyTitle = (tournamentTitle != null ? tournamentTitle.trim() : activity.getString(R.string.poster_default_title));
            text.setColor(theme.getMutedTextColor());
            text.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
            drawFittedCenteredText(canvas, cleanTourneyTitle + "  •  " + friendlyMode, w / 2f, 232f, text, 28f, w - 150f);

            // 2. Champion Portrait Card (Rounded Clipped so corners NEVER poke out & Full-Fit so face is NEVER cropped!)
            Bitmap champBmp = CardImageLoader.getCardBitmapSync(activity, champion, 580, 650);
            if (champBmp != null) {
                RectF dst = new RectF(w / 2f - 290, 268, w / 2f + 290, 918);

                // White/Surface Polaroid Backing Shadow
                Paint cardBg = new Paint(Paint.ANTI_ALIAS_FLAG);
                cardBg.setColor(isDark ? Color.parseColor("#2A2438") : Color.WHITE);
                canvas.drawRoundRect(new RectF(dst.left - 12, dst.top - 12, dst.right + 12, dst.bottom + 12), 44, 44, cardBg);

                canvas.save();
                Path clipPath = new Path();
                clipPath.addRoundRect(dst, 36f, 36f, Path.Direction.CW);
                canvas.clipPath(clipPath);
                Paint bmpPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
                canvas.drawBitmap(champBmp, null, dst, bmpPaint);
                canvas.restore();

                border.setStrokeWidth(7f);
                border.setColor(theme.secondaryColor);
                canvas.drawRoundRect(dst, 36f, 36f, border);
            }

            // 3. Champion Name & Info
            text.setColor(theme.textColor);
            text.setTypeface(Typeface.create("sans-serif-black", Typeface.BOLD));
            drawFittedCenteredText(canvas, champion.getStageName(), w / 2f, 1005f, text, 68f, w - 140f);

            text.setColor(theme.primaryColor);
            text.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
            String grpLine = (champion.isSoloist() ? activity.getString(R.string.label_soloist) : champion.getGroupName()) + "  •  " + champion.getCountryWithFlag();
            drawFittedCenteredText(canvas, grpLine, w / 2f, 1062f, text, 38f, w - 140f);

            text.setColor(theme.getMutedTextColor());
            text.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
            String bioLine = champion.getFullName() + " • " + activity.getString(R.string.label_years_with_zodiac, champion.getCalculatedAge(), champion.getZodiacSign());
            drawFittedCenteredText(canvas, bioLine, w / 2f, 1112f, text, 29f, w - 140f);

            // 4. Clean Podium Box
            RectF podRect = new RectF(78, 1152, w - 78, 1412);
            Paint boxPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            boxPaint.setColor(isDark ? Color.parseColor("#241E33") : Color.WHITE);
            canvas.drawRoundRect(podRect, 34, 34, boxPaint);

            Paint boxStroke = new Paint(Paint.ANTI_ALIAS_FLAG);
            boxStroke.setStyle(Paint.Style.STROKE);
            boxStroke.setStrokeWidth(3f);
            boxStroke.setColor(Color.argb(90, Color.red(theme.primaryColor), Color.green(theme.primaryColor), Color.blue(theme.primaryColor)));
            canvas.drawRoundRect(podRect, 34, 34, boxStroke);

            text.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
            text.setColor(theme.textColor);
            String firstStr = activity.getString(R.string.podium_1st) + ": " + champion.getStageName() + " (" + champion.getGroupName() + ")";
            drawFittedCenteredText(canvas, firstStr, w / 2f, 1214f, text, 31f, w - 200f);

            String rUp = runnerUp != null ? runnerUp.getStageName() + " (" + runnerUp.getGroupName() + ")" : "—";
            text.setColor(theme.textColor);
            drawFittedCenteredText(canvas, activity.getString(R.string.podium_2nd) + ": " + rUp, w / 2f, 1272f, text, 30f, w - 200f);

            if (semiFinalists != null && !semiFinalists.isEmpty()) {
                StringBuilder sb = new StringBuilder(activity.getString(R.string.podium_3rd)).append(": ");
                for (int i = 0; i < semiFinalists.size(); i++) {
                    if (i > 0) sb.append(" • ");
                    IdolCard sf = semiFinalists.get(i);
                    sb.append(sf.getStageName()).append(" (").append(sf.getGroupName()).append(")");
                }
                text.setColor(theme.getMutedTextColor());
                drawFittedCenteredText(canvas, sb.toString(), w / 2f, 1330f, text, 27f, w - 200f);
            }

            text.setColor(theme.primaryColor);
            drawFittedCenteredText(canvas, "✨ Who's your Bias • K-Pop Edition ✨", w / 2f, 1384f, text, 24f, w - 200f);

            File sharedDir = new File(activity.getCacheDir(), "shared");
            if (!sharedDir.exists()) sharedDir.mkdirs();
            File imgFile = new File(sharedDir, "Bias_Champion_" + champion.getStageName().replaceAll("\\W+", "") + ".jpg");
            CardImageLoader.saveBitmapToFile(poster, imgFile, 92);

            Uri uri = FileProvider.getUriForFile(activity, activity.getPackageName() + ".fileprovider", imgFile);
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("image/jpeg");
            intent.putExtra(Intent.EXTRA_STREAM, uri);
            intent.putExtra(Intent.EXTRA_TEXT, activity.getString(R.string.share_victory_text, cleanTourneyTitle, champion.getStageName(), champion.getGroupName()));
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            activity.startActivity(Intent.createChooser(intent, activity.getString(R.string.share_victory_chooser)));
        } catch (Exception e) {
            Toast.makeText(activity, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private static String formatFriendlyGameMode(Context context, String mode) {
        if (mode == null) return context.getString(R.string.poster_default_title);
        switch (mode) {
            case "KING_OF_THE_HILL":
                return context.getString(R.string.game_mode_koth);
            case "ROUND_ROBIN":
                return context.getString(R.string.game_mode_round_robin);
            case "DOUBLE_ELIMINATION":
                return context.getString(R.string.game_mode_double_elim);
            default:
                return context.getString(R.string.game_mode_bracket);
        }
    }

    private static void drawFittedCenteredText(Canvas canvas, String str, float cx, float y, Paint paint, float idealSize, float maxWidth) {
        if (str == null) return;
        float size = idealSize;
        paint.setTextSize(size);
        while (size > 18f && paint.measureText(str) > maxWidth) {
            size -= 1.5f;
            paint.setTextSize(size);
        }
        if (paint.measureText(str) > maxWidth) {
            while (str.length() > 6 && paint.measureText(str + "...") > maxWidth) {
                str = str.substring(0, str.length() - 1);
            }
            str = str + "...";
        }
        canvas.drawText(str, cx, y, paint);
    }
}
