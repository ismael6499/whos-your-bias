package com.nitsutech.whosyourbias.data;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class BiasRepository {
    private static final String FILE_IDOLS = "idols_db_v1.json";
    private static final String FILE_MATCHES = "custom_matches_v1.json";
    private static final String PREFS_NAME = "whos_your_bias_prefs";

    private static BiasRepository instance;
    private final Context appContext;
    private final List<IdolCard> idols = new ArrayList<>();
    private final List<CustomMatch> customMatches = new ArrayList<>();

    private BiasRepository(Context context) {
        this.appContext = context.getApplicationContext();
        loadAll();
    }

    public static synchronized BiasRepository getInstance(Context context) {
        if (instance == null) {
            instance = new BiasRepository(context);
        }
        return instance;
    }

    private synchronized void loadAll() {
        idols.clear();
        File idolFile = new File(appContext.getFilesDir(), FILE_IDOLS);
        if (idolFile.exists()) {
            try {
                String json = readFile(idolFile);
                JSONArray arr = new JSONArray(json);
                for (int i = 0; i < arr.length(); i++) {
                    idols.add(IdolCard.fromJson(arr.getJSONObject(i)));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (idols.isEmpty()) {
            idols.addAll(StarterPackSeeder.getInitialIdols());
            applyBundledPhotocardsDatabase(idols);
            saveIdols();
        } else {
            // Ensure any newly added starter groups exist AND backfill new Kpop Wiki photo URLs
            java.util.Map<String, IdolCard> existingMap = new java.util.HashMap<>();
            for (IdolCard c : idols) existingMap.put(c.getId(), c);
            boolean updatedAny = false;
            for (IdolCard seed : StarterPackSeeder.getInitialIdols()) {
                IdolCard ex = existingMap.get(seed.getId());
                if (ex == null) {
                    idols.add(seed);
                    updatedAny = true;
                } else {
                    if ("idol_illit_moka".equals(ex.getId()) && seed.getPhotoUri() != null && !seed.getPhotoUri().isEmpty()) {
                        ex.setPhotoUri(seed.getPhotoUri());
                        updatedAny = true;
                    } else if (("idol_nexz_sogeon".equals(ex.getId()) || "idol_nexz_hyui".equals(ex.getId()))
                            && seed.getPhotoUri() != null && !seed.getPhotoUri().isEmpty()) {
                        ex.setPhotoUri(seed.getPhotoUri());
                        updatedAny = true;
                    } else if ((ex.getPhotoUri() == null || ex.getPhotoUri().isEmpty()
                            || ex.getPhotoUri().startsWith("fandom_pc://")
                            || ex.getPhotoUri().contains("wikimedia.org")
                            || ex.getPhotoUri().contains("I_Got_Your_Back_concept_photo_4"))
                            && seed.getPhotoUri() != null && !seed.getPhotoUri().isEmpty()) {
                        ex.setPhotoUri(seed.getPhotoUri());
                        updatedAny = true;
                    }
                    if (ex.deduplicatePhotocards()) {
                        updatedAny = true;
                    }
                    for (String pc : seed.getPhotocards()) {
                        if (ex.getPhotocards().size() < 30 && !ex.containsPhotocardIdentity(pc)) {
                            ex.addPhotocard(pc);
                            updatedAny = true;
                        }
                    }
                    if (seed.getNativeName() != null && !seed.getNativeName().isEmpty()
                            && (ex.getNativeName() == null || ex.getNativeName().isEmpty())) {
                        ex.setNativeName(seed.getNativeName());
                        updatedAny = true;
                    }
                }
            }
            if (applyBundledPhotocardsDatabase(idols)) {
                updatedAny = true;
            }
            if (updatedAny) saveIdols();
        }

        customMatches.clear();
        File matchFile = new File(appContext.getFilesDir(), FILE_MATCHES);
        if (matchFile.exists()) {
            try {
                String json = readFile(matchFile);
                JSONArray arr = new JSONArray(json);
                for (int i = 0; i < arr.length(); i++) {
                    customMatches.add(CustomMatch.fromJson(arr.getJSONObject(i)));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (customMatches.isEmpty()) {
            seedStarterMatches();
            saveCustomMatches();
        }
    }

    private boolean applyBundledPhotocardsDatabase(List<IdolCard> targetList) {
        boolean changed = false;
        try (java.io.InputStream is = appContext.getAssets().open("idol_photocards_db.json");
             BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            String rawJson = sb.toString();
            if (rawJson.startsWith("\uFEFF")) {
                rawJson = rawJson.substring(1);
            }
            JSONObject db = new JSONObject(rawJson);
            for (IdolCard c : targetList) {
                if (c.deduplicatePhotocards()) {
                    changed = true;
                }
                JSONArray arr = db.optJSONArray(c.getId());
                if (arr != null && arr.length() > 0) {
                    List<String> bundledList = new ArrayList<>();
                    for (int i = 0; i < arr.length(); i++) {
                        String url = arr.optString(i, "").trim();
                        if (!url.isEmpty() && !bundledList.contains(url)) {
                            bundledList.add(url);
                        }
                    }

                    // Preserve any user-added custom local photos (e.g. file:// or content://)
                    List<String> customUserPhotos = new ArrayList<>();
                    for (String pc : c.getPhotocards()) {
                        if (pc != null && (pc.startsWith("file://") || pc.startsWith("content://"))) {
                            customUserPhotos.add(pc);
                        }
                    }

                    // Check if current list differs from bundledList + customUserPhotos
                    List<String> current = c.getPhotocards();
                    boolean needsSync = false;
                    if (current.size() != (bundledList.size() + customUserPhotos.size())) {
                        needsSync = true;
                    } else {
                        for (int i = 0; i < bundledList.size(); i++) {
                            if (!bundledList.get(i).equalsIgnoreCase(current.get(i))) {
                                needsSync = true;
                                break;
                            }
                        }
                    }

                    int prevUnlCount = c.getUnlockedPhotocards().size();

                    if (needsSync) {
                        List<String> updated = new ArrayList<>(bundledList);
                        for (String cp : customUserPhotos) {
                            if (!updated.contains(cp)) updated.add(cp);
                        }
                        c.setPhotocards(updated);
                        changed = true;
                    }

                    if (c.getPhotoUri() == null || c.getPhotoUri().isEmpty() || c.getPhotoUri().startsWith("fandom_pc://")) {
                        if (!c.getPhotocards().isEmpty()) {
                            c.setPhotoUri(c.getPhotocards().get(0));
                            changed = true;
                        }
                    }
                    // Restore unlocked count if any virtual unlocked slots were replaced
                    int targetUnl = Math.max(3, prevUnlCount);
                    List<String> allPcs = c.getPhotocards();
                    for (int i = 0; i < allPcs.size() && c.getUnlockedPhotocards().size() < targetUnl; i++) {
                        c.unlockPhotocard(allPcs.get(i));
                    }
                }
                if ("idol_illit_moka".equals(c.getId())) {
                    c.unlockAllPhotocards();
                } else if (c.getUnlockedPhotocards().size() < 3) {
                    c.unlockInitialPhotocards();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return changed;
    }

    private void seedStarterMatches() {
        // 1. ILLIT Official "Who is your bias? It's Me!" Match
        List<String> illitIds = Arrays.asList(
                "idol_illit_yunah", "idol_illit_minju", "idol_illit_moka", "idol_illit_wonhee", "idol_illit_iroha"
        );
        customMatches.add(new CustomMatch(
                "match_illit_special",
                "✨ ILLIT: Who is your bias? (It's Me!)",
                "Starter Challenge",
                "KING_OF_THE_HILL",
                illitIds,
                System.currentTimeMillis()
        ));

        // 2. 5th Gen Super Rookie Battle (16 Cards Bracket)
        List<String> gen5Ids = Arrays.asList(
                "idol_illit_minju", "idol_illit_wonhee", "idol_illit_moka", "idol_illit_iroha",
                "idol_bm_ahyeon", "idol_bm_asa", "idol_bm_chiquita", "idol_bm_ruka",
                "idol_bnd_jaehyun", "idol_bnd_taesan", "idol_bnd_woonhak", "idol_bnd_leehan",
                "idol_cortis_martin", "idol_cortis_james", "idol_cortis_juhoon", "idol_cortis_seonghyeon"
        );
        customMatches.add(new CustomMatch(
                "match_5th_gen_worldcup",
                "🔥 5th Gen World Cup (ILLIT • BABYMONSTER • BND • CORTIS)",
                "Starter Challenge",
                "BRACKET",
                gen5Ids,
                System.currentTimeMillis() - 60000
        ));

        // 3. All-Star K-Pop Icons (16 Cards Bracket)
        List<String> iconsIds = Arrays.asList(
                "idol_nj_hanni", "idol_nj_haerin", "idol_lsf_chaewon", "idol_lsf_yunjin",
                "idol_aespa_karina", "idol_aespa_winter", "idol_ive_wonyoung", "idol_ive_yujin",
                "idol_skz_felix", "idol_skz_hyunjin", "idol_bts_jungkook", "idol_bts_v",
                "idol_bp_jennie", "idol_bp_lisa", "idol_ateez_san", "idol_bb_gd"
        );
        customMatches.add(new CustomMatch(
                "match_kpop_titans",
                "👑 K-Pop Titans 16-Idol Showdown",
                "Starter Challenge",
                "BRACKET",
                iconsIds,
                System.currentTimeMillis() - 120000
        ));
    }

    public synchronized void saveIdols() {
        try {
            JSONArray arr = new JSONArray();
            for (IdolCard c : idols) {
                arr.put(c.toJson());
            }
            writeFile(new File(appContext.getFilesDir(), FILE_IDOLS), arr.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public synchronized void saveCustomMatches() {
        try {
            JSONArray arr = new JSONArray();
            for (CustomMatch m : customMatches) {
                arr.put(m.toJson());
            }
            writeFile(new File(appContext.getFilesDir(), FILE_MATCHES), arr.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public synchronized List<IdolCard> getAllIdols() {
        return new ArrayList<>(idols);
    }

    public synchronized IdolCard getIdolById(String id) {
        if (id == null) return null;
        for (IdolCard c : idols) {
            if (id.equals(c.getId())) return c;
        }
        return null;
    }

    public synchronized IdolCard findByStageAndGroup(String stageName, String groupName) {
        if (stageName == null) return null;
        for (IdolCard c : idols) {
            if (c.getStageName().equalsIgnoreCase(stageName.trim())
                    && c.getGroupName().equalsIgnoreCase(groupName != null ? groupName.trim() : "")) {
                return c;
            }
        }
        return null;
    }

    public synchronized void saveOrUpdateIdol(IdolCard card) {
        for (int i = 0; i < idols.size(); i++) {
            if (idols.get(i).getId().equals(card.getId())) {
                idols.set(i, card);
                saveIdols();
                return;
            }
        }
        idols.add(0, card);
        saveIdols();
    }

    public synchronized void deleteIdol(String id) {
        for (int i = 0; i < idols.size(); i++) {
            if (idols.get(i).getId().equals(id)) {
                idols.remove(i);
                saveIdols();
                return;
            }
        }
    }

    /**
     * Automatic Group Member Linking: returns all other idols belonging to the same group.
     */
    public synchronized List<IdolCard> getFellowGroupMembers(IdolCard card) {
        List<IdolCard> mates = new ArrayList<>();
        if (card == null || card.isSoloist() || "Soloist".equalsIgnoreCase(card.getGroupName())) {
            return mates;
        }
        String grp = card.getGroupName().trim();
        for (IdolCard c : idols) {
            if (!c.getId().equals(card.getId()) && !c.isSoloist() && c.getGroupName().equalsIgnoreCase(grp)) {
                mates.add(c);
            }
        }
        return mates;
    }

    public synchronized List<String> getAllGroups() {
        Set<String> groups = new LinkedHashSet<>();
        for (IdolCard c : idols) {
            if (!c.isSoloist() && c.getGroupName() != null && !c.getGroupName().trim().isEmpty()) {
                groups.add(c.getGroupName().trim());
            }
        }
        return new ArrayList<>(groups);
    }

    public synchronized List<String> getAllCollections() {
        Set<String> cols = new LinkedHashSet<>();
        cols.add("Girl Groups");
        cols.add("Boy Groups");
        cols.add("Soloists");
        cols.add("5th Gen");
        cols.add("4th Gen");
        cols.add("3rd Gen");
        cols.add("HYBE");
        cols.add("JYP Entertainment");
        cols.add("SM Entertainment");
        cols.add("THEBLACKLABEL");
        cols.add("YG Entertainment");
        cols.add("Legends");
        for (IdolCard c : idols) {
            if (c.getCollections() != null && !c.getCollections().isEmpty()) {
                String[] parts = c.getCollections().split(",");
                for (String p : parts) {
                    String trimmed = p.trim();
                    if (!trimmed.isEmpty()) cols.add(trimmed);
                }
            }
        }
        return new ArrayList<>(cols);
    }

    public synchronized List<IdolCard> getIdolsByFilter(String filter) {
        if (filter == null || filter.isEmpty() || "ALL".equalsIgnoreCase(filter) || "All Idols".equalsIgnoreCase(filter)) {
            return getAllIdols();
        }
        List<IdolCard> result = new ArrayList<>();
        String lowFilter = filter.toLowerCase(Locale.ROOT).trim();
        for (IdolCard c : idols) {
            if ("soloists".equals(lowFilter) || "soloist".equals(lowFilter)) {
                if (c.isSoloist() || "Soloist".equalsIgnoreCase(c.getGroupName())
                        || (c.getCollections() != null && c.getCollections().toLowerCase(Locale.ROOT).contains("soloist"))
                        || (c.getRole() != null && c.getRole().toLowerCase(Locale.ROOT).contains("soloist"))) {
                    result.add(c);
                }
            } else if (c.getGroupName() != null && c.getGroupName().toLowerCase(Locale.ROOT).equals(lowFilter)) {
                result.add(c);
            } else if (c.getCollections() != null && c.getCollections().toLowerCase(Locale.ROOT).contains(lowFilter)) {
                result.add(c);
            } else if ("favorites".equals(lowFilter) && c.isFavorite()) {
                result.add(c);
            }
        }
        return result;
    }

    public synchronized void recordMatchOutcome(String winnerId, String loserId) {
        IdolCard w = getIdolById(winnerId);
        if (w != null) {
            w.setMatchesPlayed(w.getMatchesPlayed() + 1);
            w.setMatchesWon(w.getMatchesWon() + 1);
        }
        IdolCard l = getIdolById(loserId);
        if (l != null) {
            l.setMatchesPlayed(l.getMatchesPlayed() + 1);
        }
        saveIdols();
    }

    public synchronized void recordTournamentChampion(String championId) {
        IdolCard c = getIdolById(championId);
        if (c != null) {
            c.setTournamentsWon(c.getTournamentsWon() + 1);
            saveIdols();
        }
    }

    public synchronized List<CustomMatch> getCustomMatches() {
        return new ArrayList<>(customMatches);
    }

    public synchronized void saveOrUpdateCustomMatch(CustomMatch match) {
        for (int i = 0; i < customMatches.size(); i++) {
            if (customMatches.get(i).getId().equals(match.getId())) {
                customMatches.set(i, match);
                saveCustomMatches();
                return;
            }
        }
        customMatches.add(0, match);
        saveCustomMatches();
    }

    public synchronized void deleteCustomMatch(String matchId) {
        for (int i = 0; i < customMatches.size(); i++) {
            if (customMatches.get(i).getId().equals(matchId)) {
                customMatches.remove(i);
                saveCustomMatches();
                return;
            }
        }
    }

    public SharedPreferences getPrefs() {
        return appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public String getCustomProfileAvatarUri() {
        return getPrefs().getString("custom_user_profile_avatar_uri", "");
    }

    public void setCustomProfileAvatarUri(String uri) {
        getPrefs().edit().putString("custom_user_profile_avatar_uri", uri != null ? uri : "").apply();
    }

    private String readFile(File file) throws Exception {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
        }
        return sb.toString();
    }

    private void writeFile(File file, String content) throws Exception {
        try (FileOutputStream fos = new FileOutputStream(file, false)) {
            fos.write(content.getBytes(StandardCharsets.UTF_8));
        }
    }
}
