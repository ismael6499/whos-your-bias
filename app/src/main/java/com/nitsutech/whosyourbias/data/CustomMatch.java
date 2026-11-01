package com.nitsutech.whosyourbias.data;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class CustomMatch implements Serializable {
    private String id;
    private String title;
    private String creatorName;
    private String gameMode; // BRACKET, KING_OF_THE_HILL, ROUND_ROBIN, DOUBLE_ELIMINATION
    private List<String> cardIds;
    private long createdAt;

    public CustomMatch() {
        this.id = UUID.randomUUID().toString();
        this.title = "Custom Bias Battle";
        this.creatorName = "Player";
        this.gameMode = "BRACKET";
        this.cardIds = new ArrayList<>();
        this.createdAt = System.currentTimeMillis();
    }

    public CustomMatch(String id, String title, String creatorName, String gameMode, List<String> cardIds, long createdAt) {
        this.id = (id == null || id.isEmpty()) ? UUID.randomUUID().toString() : id;
        this.title = title != null ? title : "Custom Bias Battle";
        this.creatorName = creatorName != null ? creatorName : "Player";
        this.gameMode = gameMode != null ? gameMode : "BRACKET";
        this.cardIds = cardIds != null ? cardIds : new ArrayList<>();
        this.createdAt = createdAt > 0 ? createdAt : System.currentTimeMillis();
    }

    public JSONObject toJson() throws JSONException {
        JSONObject obj = new JSONObject();
        obj.put("id", id);
        obj.put("title", title);
        obj.put("creatorName", creatorName);
        obj.put("gameMode", gameMode);
        JSONArray arr = new JSONArray();
        if (cardIds != null) {
            for (String cid : cardIds) {
                arr.put(cid);
            }
        }
        obj.put("cardIds", arr);
        obj.put("createdAt", createdAt);
        return obj;
    }

    public static CustomMatch fromJson(JSONObject obj) {
        List<String> ids = new ArrayList<>();
        JSONArray arr = obj.optJSONArray("cardIds");
        if (arr != null) {
            for (int i = 0; i < arr.length(); i++) {
                ids.add(arr.optString(i));
            }
        }
        return new CustomMatch(
                obj.optString("id", UUID.randomUUID().toString()),
                obj.optString("title", "Custom Match"),
                obj.optString("creatorName", "Friend"),
                obj.optString("gameMode", "BRACKET"),
                ids,
                obj.optLong("createdAt", System.currentTimeMillis())
        );
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getShortId() {
        if (id == null || id.isEmpty()) return "#0000";
        String clean = id.replace("-", "").replace("match_", "");
        if (clean.length() >= 4) {
            return "#" + clean.substring(0, 4).toUpperCase(java.util.Locale.ROOT);
        }
        return "#" + clean.toUpperCase(java.util.Locale.ROOT);
    }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getCreatorName() { return creatorName; }
    public void setCreatorName(String creatorName) { this.creatorName = creatorName; }
    public String getGameMode() { return gameMode; }
    public void setGameMode(String gameMode) { this.gameMode = gameMode; }
    public List<String> getCardIds() { return cardIds; }
    public void setCardIds(List<String> cardIds) { this.cardIds = cardIds; }
    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}
