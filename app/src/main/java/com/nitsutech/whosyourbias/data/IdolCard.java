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
    private String groupName;
    private boolean isSoloist;
    private String country;
    private String birthDate; // YYYY-MM-DD
    private String role;
    private String photoUri;
    private List<String> photocards = new ArrayList<>(); // Multiple favorite photocards per idol!
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
        return c;
    }

    public List<String> getPhotocards() {
        if (photocards == null) photocards = new ArrayList<>();
        return photocards;
    }

    public void setPhotocards(List<String> photocards) {
        this.photocards = photocards != null ? photocards : new ArrayList<>();
    }

    public void addPhotocard(String uri) {
        if (uri == null || uri.trim().isEmpty()) return;
        String trimmed = uri.trim();
        if (photocards == null) photocards = new ArrayList<>();
        if (!photocards.contains(trimmed)) {
            photocards.add(trimmed);
        }
        if (this.photoUri == null || this.photoUri.trim().isEmpty()) {
            this.photoUri = trimmed;
        }
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
}
