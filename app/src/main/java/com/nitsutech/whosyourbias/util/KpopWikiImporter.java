package com.nitsutech.whosyourbias.util;

import android.os.Handler;
import android.os.Looper;

import com.nitsutech.whosyourbias.data.IdolCard;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Connects directly to K-Pop Fandom Wiki (MediaWiki Open API)
 * to search and auto-import idol profiles, Korean/Japanese native names,
 * official concept photos, birthday, nationality, group, and roles.
 */
public class KpopWikiImporter {

    private static final ExecutorService executor = Executors.newFixedThreadPool(2);
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());
    private static final String USER_AGENT = "Mozilla/5.0 (Linux; Android 14; Mobile) WhosYourBias/1.1";

    public static class WikiResult {
        public final String title;
        public final String pageUrl;
        public String thumbnailUrl;

        public WikiResult(String title, String pageUrl) {
            this.title = title;
            this.pageUrl = pageUrl;
        }
    }

    public interface SearchCallback {
        void onSuccess(List<WikiResult> results);
        void onError(String message);
    }

    public interface IdolImportCallback {
        void onSuccess(IdolCard card);
        void onError(String message);
    }

    public static void searchWiki(String query, SearchCallback callback) {
        if (query == null || query.trim().isEmpty()) {
            callback.onSuccess(new ArrayList<>());
            return;
        }

        executor.execute(() -> {
            try {
                String encoded = URLEncoder.encode(query.trim(), "UTF-8");
                String urlStr = "https://kpop.fandom.com/api.php?action=opensearch&search="
                        + encoded + "&limit=8&format=json";

                String raw = httpGet(urlStr);
                JSONArray array = new JSONArray(raw);
                JSONArray titles = array.getJSONArray(1);
                JSONArray urls = array.getJSONArray(3);

                List<WikiResult> results = new ArrayList<>();
                for (int i = 0; i < titles.length(); i++) {
                    String title = titles.getString(i);
                    String pUrl = urls.getString(i);
                    if (title.endsWith("/Gallery") || title.endsWith("/Discography")) continue;
                    results.add(new WikiResult(title, pUrl));
                }

                mainHandler.post(() -> callback.onSuccess(results));
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError("Error buscando en Wiki: " + e.getMessage()));
            }
        });
    }

    public static void fetchIdolCard(String pageTitle, IdolImportCallback callback) {
        executor.execute(() -> {
            try {
                String encoded = URLEncoder.encode(pageTitle.trim(), "UTF-8");
                String urlStr = "https://kpop.fandom.com/api.php?action=query&titles="
                        + encoded + "&prop=revisions&rvprop=content&format=json";

                String raw = httpGet(urlStr);
                JSONObject root = new JSONObject(raw);
                JSONObject queryObj = root.optJSONObject("query");
                if (queryObj == null) {
                    mainHandler.post(() -> callback.onError("No se encontró información"));
                    return;
                }

                JSONObject pages = queryObj.getJSONObject("pages");
                Iterator<String> keys = pages.keys();
                if (!keys.hasNext()) {
                    mainHandler.post(() -> callback.onError("Página no encontrada"));
                    return;
                }

                JSONObject page = pages.getJSONObject(keys.next());
                JSONArray revs = page.optJSONArray("revisions");
                if (revs == null || revs.length() == 0) {
                    mainHandler.post(() -> callback.onError("Sin contenido disponible"));
                    return;
                }

                String content = revs.getJSONObject(0).getString("*");
                IdolCard card = parseInfoboxToIdolCard(pageTitle, content);

                mainHandler.post(() -> callback.onSuccess(card));
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError("Error al importar: " + e.getMessage()));
            }
        });
    }

    private static IdolCard parseInfoboxToIdolCard(String pageTitle, String wikiText) {
        IdolCard card = new IdolCard();

        // 1. Stage name
        String stageName = extractParam(wikiText, "name");
        if (stageName.isEmpty()) {
            stageName = pageTitle.replaceAll("\\s*\\(.*?\\)", "").trim();
        }
        card.setStageName(cleanWikiFormatting(stageName));

        // 2. Birth / Real Name & Native Name
        String birthName = extractParam(wikiText, "birth_name");
        if (birthName.isEmpty()) birthName = extractParam(wikiText, "real_name");

        String nativeName = "";
        String hangul = extractParam(wikiText, "hangul");
        String kanji = extractParam(wikiText, "kanji");
        String katakana = extractParam(wikiText, "katakana");

        if (!hangul.isEmpty() && !kanji.isEmpty()) {
            nativeName = hangul + " • " + kanji;
        } else if (!hangul.isEmpty() && !katakana.isEmpty()) {
            nativeName = hangul + " • " + katakana;
        } else if (!hangul.isEmpty()) {
            nativeName = hangul;
        } else if (!kanji.isEmpty()) {
            nativeName = kanji;
        }

        // Check if birth_name has (Hangul / Native) in parentheses
        if (birthName.contains("(") && birthName.contains(")")) {
            Pattern p = Pattern.compile("^(.*?)\\s*\\((.*?)\\)");
            Matcher m = p.matcher(birthName);
            if (m.find()) {
                String latinPart = m.group(1).trim();
                String parenthesized = m.group(2).trim();
                card.setFullName(cleanWikiFormatting(latinPart));
                if (nativeName.isEmpty()) {
                    nativeName = cleanWikiFormatting(parenthesized);
                }
            } else {
                card.setFullName(cleanWikiFormatting(birthName));
            }
        } else {
            card.setFullName(cleanWikiFormatting(birthName));
        }
        card.setNativeName(cleanWikiFormatting(nativeName));

        // 3. Group Name
        String associated = extractParam(wikiText, "associated");
        String group = "";
        if (!associated.isEmpty()) {
            Pattern gp = Pattern.compile("\\[\\[(.*?)\\]\\]");
            Matcher gm = gp.matcher(associated);
            if (gm.find()) {
                group = gm.group(1).split("\\|")[0].trim();
            } else {
                group = cleanWikiFormatting(associated);
            }
        }
        if (group.isEmpty() && pageTitle.contains("(") && pageTitle.contains(")")) {
            int open = pageTitle.indexOf('(');
            int close = pageTitle.indexOf(')');
            group = pageTitle.substring(open + 1, close).trim();
        }
        boolean isSolo = group.equalsIgnoreCase("Soloist") || group.isEmpty();
        card.setSoloist(isSolo);
        card.setGroupName(isSolo ? "Soloist" : group);

        // 4. Country / Birth Place
        String place = extractParam(wikiText, "birth_place");
        if (place.isEmpty()) place = extractParam(wikiText, "origin");
        if (place.isEmpty()) place = extractParam(wikiText, "nationality");
        card.setCountry(normalizeCountry(place));

        // 5. Birth Date: {{Birth date and age|YYYY|MM|DD}}
        String bDate = extractParam(wikiText, "birth_date");
        Pattern bp = Pattern.compile("(\\d{4})\\s*\\|\\s*(\\d{1,2})\\s*\\|\\s*(\\d{1,2})");
        Matcher bm = bp.matcher(bDate);
        if (bm.find()) {
            String y = bm.group(1);
            String m = String.format(Locale.ROOT, "%02d", Integer.parseInt(bm.group(2)));
            String d = String.format(Locale.ROOT, "%02d", Integer.parseInt(bm.group(3)));
            card.setBirthDate(y + "-" + m + "-" + d);
        } else {
            // Try standard YYYY-MM-DD
            Pattern std = Pattern.compile("(\\d{4}-\\d{2}-\\d{2})");
            Matcher sm = std.matcher(bDate);
            if (sm.find()) card.setBirthDate(sm.group(1));
            else card.setBirthDate(cleanWikiFormatting(bDate));
        }

        // 6. Role / Occupation
        String occ = extractParam(wikiText, "occupation");
        if (occ.isEmpty()) occ = extractParam(wikiText, "position");
        card.setRole(cleanWikiFormatting(occ));

        // 7. Official high-res image
        String imageFile = extractParam(wikiText, "image");
        if (!imageFile.isEmpty()) {
            String resolvedUrl = resolveWikiImageUrl(imageFile);
            if (resolvedUrl != null && !resolvedUrl.isEmpty()) {
                card.setPhotoUri(resolvedUrl);
                card.addPhotocard(resolvedUrl);
            }
        }

        // 8. Collections & Bio
        if (!card.isSoloist()) {
            card.setCollections(card.getGroupName() + ", 5th Gen");
        } else {
            card.setCollections("Soloists, 5th Gen");
        }
        card.setBio("Official member of " + card.getGroupName() + " imported from K-Pop Wiki.");

        // 9. Preload up to 30 concept photos from K-Pop Wiki
        List<String> extraPcs = fetchUpTo30PhotocardsSync(card);
        for (String pcUrl : extraPcs) {
            if (card.getPhotocards().size() < 30) {
                card.addPhotocard(pcUrl);
            }
        }
        while (card.getPhotocards().size() < 30) {
            int slotIdx = card.getPhotocards().size() + 1;
            card.addPhotocard("fandom_pc://" + card.getId() + "/" + slotIdx);
        }
        card.unlockInitialPhotocards();

        return card;
    }

    private static final java.util.concurrent.ConcurrentHashMap<String, List<String>> idolGalleryCache = new java.util.concurrent.ConcurrentHashMap<>();

    public static List<String> fetchUpTo30PhotocardsSync(IdolCard card) {
        if (card == null) return new ArrayList<>();
        String cacheKey = card.getId() != null ? card.getId() : (card.getGroupName() + "_" + card.getStageName());
        List<String> cached = idolGalleryCache.get(cacheKey);
        if (cached != null && !cached.isEmpty()) return cached;

        List<String> urls = new ArrayList<>();
        String cleanStage = card.getStageName() != null ? card.getStageName().replaceAll("\\s*\\([^)]*\\)", "").trim().replace(" ", "_") : "";
        String cleanGroup = card.getGroupName() != null ? card.getGroupName().trim().replace(" ", "_") : "";

        List<String> prefixes = new ArrayList<>();
        if (!card.isSoloist() && !cleanGroup.isEmpty() && !cleanStage.isEmpty()) {
            prefixes.add(capitalizeFirst(cleanGroup + "_" + cleanStage));
        }
        // Also extract prefix from existing photoUri filename if present
        String pUri = card.getPhotoUri();
        if (pUri != null && pUri.contains("/images/")) {
            try {
                int idx = pUri.indexOf("/images/");
                String after = pUri.substring(idx + 8); // e.g. 5/56/ILLIT_Yunah_Break_Even.png/...
                String[] parts = after.split("/");
                if (parts.length >= 3) {
                    String fileName = java.net.URLDecoder.decode(parts[2], "UTF-8");
                    String[] words = fileName.split("_");
                    if (words.length >= 2) {
                        String p2 = capitalizeFirst(words[0] + "_" + words[1]);
                        if (!prefixes.contains(p2)) prefixes.add(p2);
                    }
                    if (words.length >= 3) {
                        String p3 = capitalizeFirst(words[0] + "_" + words[1] + "_" + words[2]);
                        if (!prefixes.contains(p3)) prefixes.add(p3);
                    }
                }
            } catch (Exception ignored) {}
        }
        if (!cleanStage.isEmpty()) {
            String pStage = capitalizeFirst(cleanStage + "_");
            if (!prefixes.contains(pStage)) prefixes.add(pStage);
        }

        for (String prefix : prefixes) {
            if (urls.size() >= 30) break;
            try {
                String enc = URLEncoder.encode(prefix, "UTF-8");
                String apiUrl = "https://kpop.fandom.com/api.php?action=query&list=allimages&aiprefix="
                        + enc + "&ailimit=40&aiprop=url&format=json";
                String raw = httpGet(apiUrl);
                JSONObject root = new JSONObject(raw);
                JSONObject qObj = root.optJSONObject("query");
                if (qObj != null) {
                    JSONArray allImages = qObj.optJSONArray("allimages");
                    if (allImages != null) {
                        for (int i = 0; i < allImages.length() && urls.size() < 30; i++) {
                            JSONObject im = allImages.optJSONObject(i);
                            if (im == null) continue;
                            String name = im.optString("name", "").toLowerCase(Locale.ROOT);
                            if (name.contains("signature") || name.contains("logo") || name.contains("autograph")
                                    || name.contains("lightstick") || name.contains("banner")) {
                                continue;
                            }
                            String imgUrl = im.optString("url", "");
                            if (!imgUrl.isEmpty() && !urls.contains(imgUrl)) {
                                urls.add(imgUrl);
                            }
                        }
                    }
                }
            } catch (Exception ignored) {}
        }

        if (!urls.isEmpty()) {
            idolGalleryCache.put(cacheKey, urls);
        }
        return urls;
    }

    public static String resolvePhotocardSlotUrl(IdolCard card, String slotUri) {
        if (card == null || slotUri == null) return "";
        if (!slotUri.startsWith("fandom_pc://")) return slotUri;
        int slotIdx = 1;
        try {
            int lastSlash = slotUri.lastIndexOf('/');
            if (lastSlash >= 0) {
                slotIdx = Integer.parseInt(slotUri.substring(lastSlash + 1));
            }
        } catch (Exception ignored) {}
        List<String> gallery = fetchUpTo30PhotocardsSync(card);
        if (!gallery.isEmpty()) {
            int pick = Math.max(0, Math.min(gallery.size() - 1, slotIdx - 1));
            String resolved = gallery.get(pick);
            card.replacePhotocardSlot(slotUri, resolved);
            return resolved;
        }
        return card.getPhotoUri() != null ? card.getPhotoUri() : "";
    }

    public static void resolveAllPhotocardSlotsAsync(IdolCard card, com.nitsutech.whosyourbias.data.BiasRepository repo, Runnable onUpdatedUi) {
        if (card == null) return;
        boolean hasVirtualSlots = false;
        for (String pc : card.getPhotocards()) {
            if (pc != null && pc.startsWith("fandom_pc://")) {
                hasVirtualSlots = true;
                break;
            }
        }
        if (!hasVirtualSlots && card.getPhotocards().size() >= 30) return;

        executor.execute(() -> {
            List<String> gallery = fetchUpTo30PhotocardsSync(card);
            if (gallery.isEmpty()) return;
            boolean changed = false;
            List<String> current = new ArrayList<>(card.getPhotocards());
            int gIdx = 0;
            for (String pc : current) {
                if (pc != null && pc.startsWith("fandom_pc://")) {
                    while (gIdx < gallery.size() && card.getPhotocards().contains(gallery.get(gIdx))) {
                        gIdx++;
                    }
                    if (gIdx < gallery.size()) {
                        card.replacePhotocardSlot(pc, gallery.get(gIdx));
                        gIdx++;
                        changed = true;
                    }
                }
            }
            if (changed) {
                if (repo != null) repo.saveOrUpdateIdol(card);
                if (onUpdatedUi != null) mainHandler.post(onUpdatedUi);
            }
        });
    }

    private static String capitalizeFirst(String s) {
        if (s == null || s.isEmpty()) return "";
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private static String resolveWikiImageUrl(String filename) {
        try {
            String clean = filename.trim().replace(" ", "_");
            String enc = URLEncoder.encode("File:" + clean, "UTF-8");
            String url = "https://kpop.fandom.com/api.php?action=query&titles="
                    + enc + "&prop=imageinfo&iiprop=url&format=json";
            String raw = httpGet(url);
            JSONObject root = new JSONObject(raw);
            JSONObject pages = root.getJSONObject("query").getJSONObject("pages");
            Iterator<String> it = pages.keys();
            if (it.hasNext()) {
                JSONObject page = pages.getJSONObject(it.next());
                JSONArray ii = page.optJSONArray("imageinfo");
                if (ii != null && ii.length() > 0) {
                    return ii.getJSONObject(0).optString("url");
                }
            }
        } catch (Exception ignored) {}
        return "";
    }

    private static String extractParam(String text, String key) {
        Pattern p = Pattern.compile("\\|\\s*" + Pattern.quote(key) + "\\s*=\\s*([^\\n\\|]+)", Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(text);
        if (m.find()) {
            return m.group(1).trim();
        }
        return "";
    }

    private static String cleanWikiFormatting(String raw) {
        if (raw == null) return "";
        return raw.replaceAll("\\[\\[(.*?)\\|(.*?)\\]\\]", "$2")
                .replaceAll("\\[\\[(.*?)\\]\\]", "$1")
                .replaceAll("\\{\\{.*?\\}\\}", "")
                .replaceAll("<ref.*?>.*?</ref>", "")
                .replaceAll("<.*?>", "")
                .replaceAll("'{2,5}", "")
                .trim();
    }

    private static String normalizeCountry(String raw) {
        String c = cleanWikiFormatting(raw).toLowerCase(Locale.ROOT);
        if (c.contains("korea")) return "South Korea";
        if (c.contains("japan")) return "Japan";
        if (c.contains("china")) return "China";
        if (c.contains("taiwan")) return "Taiwan";
        if (c.contains("thailand")) return "Thailand";
        if (c.contains("united states") || c.contains("usa") || c.contains("america")) return "USA";
        if (c.contains("australia")) return "Australia";
        if (c.contains("canada")) return "Canada";
        if (c.contains("philippines")) return "Philippines";
        return cleanWikiFormatting(raw);
    }

    private static String httpGet(String urlStr) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("User-Agent", USER_AGENT);
        conn.setConnectTimeout(8000);
        conn.setReadTimeout(8000);

        int code = conn.getResponseCode();
        if (code < 200 || code >= 300) {
            throw new Exception("HTTP " + code);
        }

        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line).append("\n");
        }
        reader.close();
        conn.disconnect();
        return sb.toString();
    }
}
