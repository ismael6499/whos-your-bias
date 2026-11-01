package com.nitsutech.whosyourbias.data;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class IdolCard implements Serializable {
    private String id;
    private String stageName;
    private String fullName;
    private String nativeName; // Native script name (Korean Hangul, Japanese Kanji/Kana, etc.)
    private String groupName;
    private boolean isSoloist;
    private String country;
    private String birthDate; // YYYY-MM-DD
    private String role;
    private String photoUri;
    private List<String> photocards = new ArrayList<>(); // Multiple favorite photocards per idol!
    private List<String> unlockedPhotocards = new ArrayList<>(); // Photocards unlocked via Gacha or default
    private boolean useRandomPhotocard = false; // Default: Solo Portada!
    private String accentColor;
    private String bio;
    private String collections; // Comma-separated collection names
    private int tournamentsWon;
    private int matchesWon;
    private int matchesPlayed;
    private boolean isFavorite;

    public IdolCard() {
        this.id = UUID.randomUUID().toString();
        this.accentColor = "#7B61FF";
        this.collections = "K-Pop";
    }

    public IdolCard(String id, String stageName, String fullName, String groupName, boolean isSoloist,
                    String country, String birthDate, String role, String photoUri,
                    String accentColor, String bio, String collections) {
        this.id = (id == null || id.isEmpty()) ? UUID.randomUUID().toString() : id;
        this.stageName = stageName != null ? stageName : "";
        this.fullName = fullName != null ? fullName : "";
        this.groupName = groupName != null ? groupName : "Soloist";
        this.isSoloist = isSoloist;
        this.country = country != null ? country : "South Korea";
        this.birthDate = birthDate != null ? birthDate : "2004-01-01";
        this.role = role != null ? role : "Vocalist, Dancer";
        this.photoUri = photoUri != null ? photoUri : "";
        this.accentColor = (accentColor != null && !accentColor.isEmpty()) ? accentColor : "#7B61FF";
        this.bio = bio != null ? bio : "";
        this.collections = collections != null ? collections : "K-Pop";
    }

    public int getCalculatedAge() {
        if (birthDate == null || birthDate.length() < 4) return 20;
        try {
            String[] parts = birthDate.trim().split("[-/]");
            int year = Integer.parseInt(parts[0]);
            int month = parts.length > 1 ? Integer.parseInt(parts[1]) : 1;
            int day = parts.length > 2 ? Integer.parseInt(parts[2]) : 1;
            Calendar now = Calendar.getInstance();
            int curYear = now.get(Calendar.YEAR);
            int curMonth = now.get(Calendar.MONTH) + 1;
            int curDay = now.get(Calendar.DAY_OF_MONTH);
            int age = curYear - year;
            if (curMonth < month || (curMonth == month && curDay < day)) {
                age--;
            }
            return Math.max(10, Math.min(99, age));
        } catch (Exception e) {
            return 21;
        }
    }

    public String getZodiacSign() {
        if (birthDate == null || !birthDate.contains("-")) return "✨ Star";
        try {
            String[] parts = birthDate.trim().split("[-/]");
            if (parts.length < 3) return "✨ Star";
            int month = Integer.parseInt(parts[1]);
            int day = Integer.parseInt(parts[2]);
            switch (month) {
                case 1: return (day < 20) ? "♑ Capricorn" : "♒ Aquarius";
                case 2: return (day < 19) ? "♒ Aquarius" : "♓ Pisces";
                case 3: return (day < 21) ? "♓ Pisces" : "♈ Aries";
                case 4: return (day < 20) ? "♈ Aries" : "♉ Taurus";
                case 5: return (day < 21) ? "♉ Taurus" : "♊ Gemini";
                case 6: return (day < 21) ? "♊ Gemini" : "♋ Cancer";
                case 7: return (day < 23) ? "♋ Cancer" : "♌ Leo";
                case 8: return (day < 23) ? "♌ Leo" : "♍ Virgo";
                case 9: return (day < 23) ? "♍ Virgo" : "♎ Libra";
                case 10: return (day < 23) ? "♎ Libra" : "♏ Scorpio";
                case 11: return (day < 22) ? "♏ Scorpio" : "♐ Sagittarius";
                case 12: return (day < 22) ? "♐ Sagittarius" : "♑ Capricorn";
                default: return "✨ Star";
            }
        } catch (Exception e) {
            return "✨ Star";
        }
    }

    public String getCountryWithFlag() {
        if (country == null || country.isEmpty()) return "🌍 Global";
        String low = country.toLowerCase(Locale.ROOT);
        if (low.contains("korea") || low.contains("corea")) return "🇰🇷 " + country;
        if (low.contains("japan") || low.contains("japon") || low.contains("japón")) return "🇯🇵 " + country;
        if (low.contains("australia")) return "🇦🇺 " + country;
        if (low.contains("vietnam")) return "🇻🇳 " + country;
        if (low.contains("thailand") || low.contains("tailandia")) return "🇹🇭 " + country;
        if (low.contains("china")) return "🇨🇳 " + country;
        if (low.contains("taiwan") || low.contains("taiwán")) return "🇹🇼 " + country;
        if (low.contains("usa") || low.contains("united states") || low.contains("eeuu")) return "🇺🇸 " + country;
        if (low.contains("canada") || low.contains("canadá")) return "🇨🇦 " + country;
        if (low.contains("argentina")) return "🇦🇷 " + country;
        return "🌏 " + country;
    }

    public int getWinRatePercent() {
        if (matchesPlayed <= 0) return 0;
        return Math.round((matchesWon * 100f) / matchesPlayed);
    }

    public JSONObject toJson() throws JSONException {
        JSONObject obj = new JSONObject();
        obj.put("id", id);
        obj.put("stageName", stageName);
        obj.put("fullName", fullName);
        obj.put("nativeName", nativeName != null ? nativeName : "");
        obj.put("groupName", groupName);
        obj.put("isSoloist", isSoloist);
        obj.put("country", country);
        obj.put("birthDate", birthDate);
        obj.put("role", role);
        obj.put("photoUri", photoUri);
        JSONArray pcArr = new JSONArray();
        if (photocards != null) {
            for (String pc : photocards) {
                if (pc != null && !pc.trim().isEmpty()) pcArr.put(pc.trim());
            }
        }
        obj.put("photocards", pcArr);
        JSONArray upcArr = new JSONArray();
        if (unlockedPhotocards != null) {
            for (String u : unlockedPhotocards) {
                if (u != null && !u.trim().isEmpty()) upcArr.put(u.trim());
            }
        }
        obj.put("unlockedPhotocards", upcArr);
        obj.put("useRandomPhotocard", useRandomPhotocard);
        obj.put("accentColor", accentColor);
        obj.put("bio", bio);
        obj.put("collections", collections);
        obj.put("tournamentsWon", tournamentsWon);
        obj.put("matchesWon", matchesWon);
        obj.put("matchesPlayed", matchesPlayed);
        obj.put("isFavorite", isFavorite);
        return obj;
    }

    public static IdolCard fromJson(JSONObject obj) {
        IdolCard c = new IdolCard(
                obj.optString("id", UUID.randomUUID().toString()),
                obj.optString("stageName", "Unknown"),
                obj.optString("fullName", ""),
                obj.optString("groupName", "Soloist"),
                obj.optBoolean("isSoloist", false),
                obj.optString("country", "South Korea"),
                obj.optString("birthDate", "2004-01-01"),
                obj.optString("role", "Vocalist"),
                obj.optString("photoUri", ""),
                obj.optString("accentColor", "#7B61FF"),
                obj.optString("bio", ""),
                obj.optString("collections", "K-Pop")
        );
        c.setTournamentsWon(obj.optInt("tournamentsWon", 0));
        c.setMatchesWon(obj.optInt("matchesWon", 0));
        c.setMatchesPlayed(obj.optInt("matchesPlayed", 0));
        c.setFavorite(obj.optBoolean("isFavorite", false));
        JSONArray pcArr = obj.optJSONArray("photocards");
        List<String> pcs = new ArrayList<>();
        if (pcArr != null) {
            for (int i = 0; i < pcArr.length(); i++) {
                String p = pcArr.optString(i, "").trim();
                if (!p.isEmpty()) pcs.add(p);
            }
        }
        if (pcs.isEmpty() && c.getPhotoUri() != null && !c.getPhotoUri().trim().isEmpty()) {
            pcs.add(c.getPhotoUri().trim());
        }
        c.setPhotocards(pcs);
        JSONArray upcArr = obj.optJSONArray("unlockedPhotocards");
        List<String> upcs = new ArrayList<>();
        if (upcArr != null) {
            for (int i = 0; i < upcArr.length(); i++) {
                String u = upcArr.optString(i, "").trim();
                if (!u.isEmpty()) upcs.add(u);
            }
        }
        c.setUnlockedPhotocards(upcs);
        c.setNativeName(obj.optString("nativeName", ""));
        c.setUseRandomPhotocard(obj.optBoolean("useRandomPhotocard", false));
        return c;
    }

    public List<String> getPhotocards() {
        if (photocards == null) photocards = new ArrayList<>();
        return photocards;
    }

    public void setPhotocards(List<String> photocards) {
        this.photocards = photocards != null ? photocards : new ArrayList<>();
    }

    public static String normalizePhotoIdentity(String uri) {
        if (uri == null) return "";
        String s = uri.trim();
        int revIdx = s.indexOf("/revision/");
        if (revIdx > 0) {
            s = s.substring(0, revIdx);
        } else {
            int qIdx = s.indexOf('?');
            if (qIdx > 0) s = s.substring(0, qIdx);
        }
        return s.toLowerCase(java.util.Locale.ROOT);
    }

    public static boolean isSamePhoto(String a, String b) {
        if (a == null || b == null) return false;
        String na = normalizePhotoIdentity(a);
        String nb = normalizePhotoIdentity(b);
        return !na.isEmpty() && na.equals(nb);
    }

    public boolean containsPhotocardIdentity(String uri) {
        if (uri == null || uri.trim().isEmpty() || photocards == null) return false;
        String target = normalizePhotoIdentity(uri);
        for (String existing : photocards) {
            if (target.equals(normalizePhotoIdentity(existing))) {
                return true;
            }
        }
        return false;
    }

    public boolean deduplicatePhotocards() {
        if (photocards == null || photocards.size() <= 1) return false;
        List<String> unique = new ArrayList<>();
        java.util.Set<String> seen = new java.util.HashSet<>();
        boolean changed = false;
        for (String pc : photocards) {
            if (pc == null || pc.trim().isEmpty()) {
                changed = true;
                continue;
            }
            String norm = normalizePhotoIdentity(pc);
            if (!seen.contains(norm)) {
                seen.add(norm);
                unique.add(pc.trim());
            } else {
                changed = true;
            }
        }
        if (changed) {
            photocards = unique;
        }
        if (unlockedPhotocards != null && unlockedPhotocards.size() > 1) {
            List<String> uniqueUnl = new ArrayList<>();
            java.util.Set<String> seenUnl = new java.util.HashSet<>();
            for (String u : unlockedPhotocards) {
                if (u == null || u.trim().isEmpty()) continue;
                String norm = normalizePhotoIdentity(u);
                if (!seenUnl.contains(norm)) {
                    seenUnl.add(norm);
                    uniqueUnl.add(u.trim());
                }
            }
            unlockedPhotocards = uniqueUnl;
        }
        return changed;
    }

    public void addPhotocard(String uri) {
        if (uri == null || uri.trim().isEmpty()) return;
        String trimmed = uri.trim();
        if (photocards == null) photocards = new ArrayList<>();
        if (!containsPhotocardIdentity(trimmed)) {
            photocards.add(trimmed);
        }
        if (this.photoUri == null || this.photoUri.trim().isEmpty()) {
            this.photoUri = trimmed;
        }
    }

    public synchronized boolean swapPhotocards(int indexA, int indexB) {
        if (photocards == null || indexA < 0 || indexB < 0 || indexA >= photocards.size() || indexB >= photocards.size()) {
            return false;
        }
        String temp = photocards.get(indexA);
        photocards.set(indexA, photocards.get(indexB));
        photocards.set(indexB, temp);
        return true;
    }

    public synchronized boolean movePhotocard(int fromIndex, int toIndex) {
        if (photocards == null || fromIndex < 0 || toIndex < 0 || fromIndex >= photocards.size() || toIndex >= photocards.size()) {
            return false;
        }
        if (fromIndex == toIndex) return true;
        String item = photocards.remove(fromIndex);
        photocards.add(toIndex, item);
        return true;
    }

    public synchronized boolean removePhotocard(String uri) {
        if (uri == null || photocards == null || photocards.size() <= 1) return false;
        String norm = normalizePhotoIdentity(uri);
        boolean removed = false;
        for (int i = 0; i < photocards.size(); i++) {
            if (norm.equals(normalizePhotoIdentity(photocards.get(i)))) {
                photocards.remove(i);
                removed = true;
                break;
            }
        }
        if (unlockedPhotocards != null) {
            for (int i = 0; i < unlockedPhotocards.size(); i++) {
                if (norm.equals(normalizePhotoIdentity(unlockedPhotocards.get(i)))) {
                    unlockedPhotocards.remove(i);
                    break;
                }
            }
        }
        if (removed && norm.equals(normalizePhotoIdentity(this.photoUri))) {
            this.photoUri = (!photocards.isEmpty()) ? photocards.get(0) : "";
        }
        return removed;
    }

    public boolean isUseRandomPhotocard() {
        return useRandomPhotocard;
    }

    public void setUseRandomPhotocard(boolean useRandomPhotocard) {
        this.useRandomPhotocard = useRandomPhotocard;
    }

    public List<String> getUnlockedPhotocards() {
        if ("idol_illit_moka".equals(id)) {
            return getPhotocards(); // Moka has 100% of her photocards unlocked!
        }
        if (unlockedPhotocards == null) {
            unlockedPhotocards = new ArrayList<>();
        }
        List<String> all = getPhotocards();
        // Remove stale virtual URIs
        List<String> valid = new ArrayList<>();
        for (String u : unlockedPhotocards) {
            if (u != null && !u.startsWith("fandom_pc://") && containsPhotocardIdentity(all, u)) {
                valid.add(u);
            }
        }
        unlockedPhotocards = valid;

        // Guarantee at least the first 3 default photocards are always unlocked!
        int minDefault = Math.min(3, all.size());
        for (int i = 0; i < minDefault; i++) {
            String candidate = all.get(i);
            if (candidate != null && !containsPhotocardIdentity(unlockedPhotocards, candidate)) {
                unlockedPhotocards.add(candidate);
            }
        }
        if (photoUri != null && !photoUri.isEmpty() && !containsPhotocardIdentity(unlockedPhotocards, photoUri)) {
            unlockedPhotocards.add(photoUri);
        }
        return unlockedPhotocards;
    }

    public void setUnlockedPhotocards(List<String> unlocked) {
        this.unlockedPhotocards = unlocked != null ? unlocked : new ArrayList<>();
    }

    public boolean isPhotocardUnlocked(String uri) {
        if ("idol_illit_moka".equals(id)) return true;
        if (uri == null || uri.trim().isEmpty()) return false;
        if (isSamePhoto(uri, photoUri)) return true;
        // Starter slots 0, 1, 2 are ALWAYS unlocked for every idol!
        if (photocards != null) {
            int starterLimit = Math.min(3, photocards.size());
            for (int i = 0; i < starterLimit; i++) {
                if (isSamePhoto(uri, photocards.get(i))) return true;
            }
        }
        return containsPhotocardIdentity(getUnlockedPhotocards(), uri);
    }

    private static boolean containsPhotocardIdentity(List<String> list, String targetUri) {
        if (list == null || targetUri == null) return false;
        String normTarget = normalizePhotoIdentity(targetUri);
        for (String item : list) {
            if (item != null && normTarget.equals(normalizePhotoIdentity(item))) {
                return true;
            }
        }
        return false;
    }

    public boolean unlockPhotocard(String uri) {
        if (uri == null || uri.trim().isEmpty()) return false;
        String trimmed = uri.trim();
        List<String> unlocked = getUnlockedPhotocards();
        if (!containsPhotocardIdentity(unlocked, trimmed)) {
            unlocked.add(trimmed);
            return true;
        }
        return false;
    }

    public List<String> getLockedPhotocards() {
        if ("idol_illit_moka".equals(id)) return new ArrayList<>();
        List<String> locked = new ArrayList<>();
        for (String pc : getPhotocards()) {
            if (!isPhotocardUnlocked(pc)) {
                locked.add(pc);
            }
        }
        return locked;
    }

    public String getRandomOrPrimaryPhotocardUri() {
        List<String> pool = getUnlockedPhotocards();
        if (!useRandomPhotocard || pool == null || pool.isEmpty()) {
            return (photoUri != null && !photoUri.isEmpty()) ? photoUri : "";
        }
        int idx = (int) (Math.random() * pool.size());
        return pool.get(idx);
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getStageName() { return stageName; }
    public void setStageName(String stageName) { this.stageName = stageName; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getGroupName() { return isSoloist ? "Soloist" : groupName; }
    public void setGroupName(String groupName) { this.groupName = groupName; }
    public boolean isSoloist() { return isSoloist; }
    public void setSoloist(boolean soloist) { isSoloist = soloist; }
    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }
    public String getBirthDate() { return birthDate; }
    public void setBirthDate(String birthDate) { this.birthDate = birthDate; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getPhotoUri() { return photoUri; }
    public void setPhotoUri(String photoUri) { this.photoUri = photoUri; }
    public String getAccentColor() { return accentColor; }
    public void setAccentColor(String accentColor) { this.accentColor = accentColor; }
    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }
    public String getCollections() { return collections; }
    public void setCollections(String collections) { this.collections = collections; }
    public int getTournamentsWon() { return tournamentsWon; }
    public void setTournamentsWon(int tournamentsWon) { this.tournamentsWon = tournamentsWon; }
    public int getMatchesWon() { return matchesWon; }
    public void setMatchesWon(int matchesWon) { this.matchesWon = matchesWon; }
    public int getMatchesPlayed() { return matchesPlayed; }
    public void setMatchesPlayed(int matchesPlayed) { this.matchesPlayed = matchesPlayed; }
    public boolean isFavorite() { return isFavorite; }
    public void setFavorite(boolean favorite) { isFavorite = favorite; }

    public String getNativeName() {
        return nativeName != null ? nativeName : "";
    }

    public void setNativeName(String nativeName) {
        this.nativeName = nativeName;
    }

    public void unlockAllPhotocards() {
        if (unlockedPhotocards == null) unlockedPhotocards = new ArrayList<>();
        unlockedPhotocards.clear();
        unlockedPhotocards.addAll(getPhotocards());
    }

    public void unlockInitialPhotocards() {
        if (unlockedPhotocards == null) unlockedPhotocards = new ArrayList<>();
        List<String> all = getPhotocards();
        int count = Math.min(3, all.size());
        for (int i = 0; i < count; i++) {
            String u = all.get(i);
            if (!unlockedPhotocards.contains(u)) {
                unlockedPhotocards.add(u);
            }
        }
    }

    public synchronized void replacePhotocardSlot(String oldUri, String newUri) {
        if (oldUri == null || newUri == null || newUri.trim().isEmpty()) return;
        String cleanNew = newUri.trim();
        if (photocards != null) {
            int idx = photocards.indexOf(oldUri);
            if (idx >= 0) {
                if (!photocards.contains(cleanNew)) {
                    photocards.set(idx, cleanNew);
                } else {
                    photocards.remove(idx);
                }
            }
        }
        if (unlockedPhotocards != null) {
            int uIdx = unlockedPhotocards.indexOf(oldUri);
            if (uIdx >= 0) {
                if (!unlockedPhotocards.contains(cleanNew)) {
                    unlockedPhotocards.set(uIdx, cleanNew);
                } else {
                    unlockedPhotocards.remove(uIdx);
                }
            }
        }
        if (oldUri.equals(photoUri)) {
            photoUri = cleanNew;
        }
    }
}
