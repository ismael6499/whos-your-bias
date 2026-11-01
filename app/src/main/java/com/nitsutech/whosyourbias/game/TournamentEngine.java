package com.nitsutech.whosyourbias.game;

import android.content.Context;
import com.nitsutech.whosyourbias.R;
import com.nitsutech.whosyourbias.data.IdolCard;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TournamentEngine implements Serializable {

    public static class MatchHistoryItem implements Serializable {
        public final String roundLabel;
        public final IdolCard winner;
        public final IdolCard loser;

        public MatchHistoryItem(String roundLabel, IdolCard winner, IdolCard loser) {
            this.roundLabel = roundLabel;
            this.winner = winner;
            this.loser = loser;
        }
    }

    public static class Snapshot implements Serializable {
        final List<MatchHistoryItem> history;
        final List<IdolCard> currentRoundPool;
        final List<IdolCard> nextRoundWinners;
        final List<IdolCard> losersBracketPool;
        final boolean inLosersBracketPhase;
        final boolean inGrandFinalPhase;
        final IdolCard winnersBracketChamp;
        final int matchIndexInRound;
        final int roundNumber;
        final IdolCard reigningKing;
        final int currentKingStreak;
        final int maxKingStreak;
        final IdolCard longestStreakHolder;
        final int kothQueueIndex;
        final Map<String, Integer> pointsMap;
        final int rrIndex;
        final IdolCard champion;
        final IdolCard runnerUp;
        final List<IdolCard> semiFinalists;
        final boolean finished;
        final IdolCard currentLeft;
        final IdolCard currentRight;

        Snapshot(TournamentEngine e) {
            this.history = new ArrayList<>(e.history);
            this.currentRoundPool = new ArrayList<>(e.currentRoundPool);
            this.nextRoundWinners = new ArrayList<>(e.nextRoundWinners);
            this.losersBracketPool = new ArrayList<>(e.losersBracketPool);
            this.inLosersBracketPhase = e.inLosersBracketPhase;
            this.inGrandFinalPhase = e.inGrandFinalPhase;
            this.winnersBracketChamp = e.winnersBracketChamp;
            this.matchIndexInRound = e.matchIndexInRound;
            this.roundNumber = e.roundNumber;
            this.reigningKing = e.reigningKing;
            this.currentKingStreak = e.currentKingStreak;
            this.maxKingStreak = e.maxKingStreak;
            this.longestStreakHolder = e.longestStreakHolder;
            this.kothQueueIndex = e.kothQueueIndex;
            this.pointsMap = new HashMap<>(e.pointsMap);
            this.rrIndex = e.rrIndex;
            this.champion = e.champion;
            this.runnerUp = e.runnerUp;
            this.semiFinalists = new ArrayList<>(e.semiFinalists);
            this.finished = e.finished;
            this.currentLeft = e.currentLeft;
            this.currentRight = e.currentRight;
        }

        void restore(TournamentEngine e) {
            e.history.clear();
            e.history.addAll(this.history);
            e.currentRoundPool = new ArrayList<>(this.currentRoundPool);
            e.nextRoundWinners = new ArrayList<>(this.nextRoundWinners);
            e.losersBracketPool = new ArrayList<>(this.losersBracketPool);
            e.inLosersBracketPhase = this.inLosersBracketPhase;
            e.inGrandFinalPhase = this.inGrandFinalPhase;
            e.winnersBracketChamp = this.winnersBracketChamp;
            e.matchIndexInRound = this.matchIndexInRound;
            e.roundNumber = this.roundNumber;
            e.reigningKing = this.reigningKing;
            e.currentKingStreak = this.currentKingStreak;
            e.maxKingStreak = this.maxKingStreak;
            e.longestStreakHolder = this.longestStreakHolder;
            e.kothQueueIndex = this.kothQueueIndex;
            e.pointsMap.clear();
            e.pointsMap.putAll(this.pointsMap);
            e.rrIndex = this.rrIndex;
            e.champion = this.champion;
            e.runnerUp = this.runnerUp;
            e.semiFinalists.clear();
            e.semiFinalists.addAll(this.semiFinalists);
            e.finished = this.finished;
            e.currentLeft = this.currentLeft;
            e.currentRight = this.currentRight;
        }
    }

    private final List<Snapshot> snapshots = new ArrayList<>();

    private final String gameMode; // BRACKET, KING_OF_THE_HILL, ROUND_ROBIN, DOUBLE_ELIMINATION
    private final String tournamentTitle;
    private final List<IdolCard> initialRoster;
    private final List<MatchHistoryItem> history = new ArrayList<>();

    // Bracket & Double Elim State
    private List<IdolCard> currentRoundPool = new ArrayList<>();
    private List<IdolCard> nextRoundWinners = new ArrayList<>();
    private List<IdolCard> losersBracketPool = new ArrayList<>();
    private boolean inLosersBracketPhase = false;
    private boolean inGrandFinalPhase = false;
    private IdolCard winnersBracketChamp = null;
    private int matchIndexInRound = 0;
    private int roundNumber = 1;

    // King of the Hill State
    private IdolCard reigningKing = null;
    private int currentKingStreak = 0;
    private int maxKingStreak = 0;
    private IdolCard longestStreakHolder = null;
    private int kothQueueIndex = 0;

    // Round Robin State
    private final List<IdolCard[]> roundRobinPairs = new ArrayList<>();
    private final Map<String, Integer> pointsMap = new HashMap<>();
    private int rrIndex = 0;

    // Final Standings
    private IdolCard champion = null;
    private IdolCard runnerUp = null;
    private final List<IdolCard> semiFinalists = new ArrayList<>();
    private boolean finished = false;

    private IdolCard currentLeft = null;
    private IdolCard currentRight = null;

    public TournamentEngine(String tournamentTitle, String gameMode, List<IdolCard> cards) {
        this.tournamentTitle = tournamentTitle != null ? tournamentTitle : "Who's your Bias";
        this.gameMode = gameMode != null ? gameMode : "KING_OF_THE_HILL";
        this.initialRoster = new ArrayList<>(cards);
        Collections.shuffle(this.initialRoster);
        initMode();
    }

    private void initMode() {
        if (initialRoster.size() < 2) {
            if (!initialRoster.isEmpty()) champion = initialRoster.get(0);
            finished = true;
            return;
        }

        switch (gameMode) {
            case "KING_OF_THE_HILL":
                reigningKing = initialRoster.get(0);
                kothQueueIndex = 1;
                currentKingStreak = 0;
                currentLeft = reigningKing;
                currentRight = initialRoster.get(kothQueueIndex);
                break;

            case "ROUND_ROBIN":
                for (IdolCard c : initialRoster) pointsMap.put(c.getId(), 0);
                buildRoundRobinSchedule();
                rrIndex = 0;
                if (!roundRobinPairs.isEmpty()) {
                    currentLeft = roundRobinPairs.get(0)[0];
                    currentRight = roundRobinPairs.get(0)[1];
                }
                break;

            case "DOUBLE_ELIMINATION":
            case "BRACKET":
            default:
                currentRoundPool = new ArrayList<>(initialRoster);
                nextRoundWinners = new ArrayList<>();
                losersBracketPool = new ArrayList<>();
                matchIndexInRound = 0;
                roundNumber = 1;
                advanceToNextPairBracket();
                break;
        }
    }

    private void buildRoundRobinSchedule() {
        roundRobinPairs.clear();
        int n = initialRoster.size();
        // If small pool (<= 8), full all-play-all; if larger, 3 Swiss-style rounds per card
        if (n <= 8) {
            for (int i = 0; i < n; i++) {
                for (int j = i + 1; j < n; j++) {
                    roundRobinPairs.add(new IdolCard[]{initialRoster.get(i), initialRoster.get(j)});
                }
            }
            Collections.shuffle(roundRobinPairs);
        } else {
            List<IdolCard> copy = new ArrayList<>(initialRoster);
            for (int cycle = 0; cycle < 3; cycle++) {
                Collections.shuffle(copy);
                for (int i = 0; i + 1 < copy.size(); i += 2) {
                    roundRobinPairs.add(new IdolCard[]{copy.get(i), copy.get(i + 1)});
                }
            }
        }
    }

    public boolean canUndo() {
        return !snapshots.isEmpty();
    }

    public boolean undoLastPick() {
        if (snapshots.isEmpty()) return false;
        Snapshot s = snapshots.remove(snapshots.size() - 1);
        s.restore(this);
        return true;
    }

    public void pickWinner(boolean pickedLeft) {
        if (finished || currentLeft == null || currentRight == null) return;

        snapshots.add(new Snapshot(this));

        IdolCard winner = pickedLeft ? currentLeft : currentRight;
        IdolCard loser = pickedLeft ? currentRight : currentLeft;
        String rLabel = getCurrentRoundTitle();
        history.add(new MatchHistoryItem(rLabel, winner, loser));

        switch (gameMode) {
            case "KING_OF_THE_HILL":
                handleKingOfTheHillPick(winner, loser);
                break;
            case "ROUND_ROBIN":
                handleRoundRobinPick(winner, loser);
                break;
            case "DOUBLE_ELIMINATION":
                handleDoubleElimPick(winner, loser);
                break;
            case "BRACKET":
            default:
                handleBracketPick(winner, loser);
                break;
        }
    }

    private void handleKingOfTheHillPick(IdolCard winner, IdolCard loser) {
        if (winner.getId().equals(reigningKing.getId())) {
            currentKingStreak++;
        } else {
            reigningKing = winner;
            currentKingStreak = 1;
        }
        if (currentKingStreak > maxKingStreak) {
            maxKingStreak = currentKingStreak;
            longestStreakHolder = reigningKing;
        }

        kothQueueIndex++;
        if (kothQueueIndex >= initialRoster.size()) {
            champion = reigningKing;
            runnerUp = loser;
            if (longestStreakHolder != null && !longestStreakHolder.getId().equals(champion.getId())) {
                semiFinalists.add(longestStreakHolder);
            }
            finished = true;
        } else {
            currentLeft = reigningKing;
            currentRight = initialRoster.get(kothQueueIndex);
        }
    }

    private void handleRoundRobinPick(IdolCard winner, IdolCard loser) {
        int pts = pointsMap.containsKey(winner.getId()) ? pointsMap.get(winner.getId()) : 0;
        pointsMap.put(winner.getId(), pts + 3);

        rrIndex++;
        if (rrIndex >= roundRobinPairs.size()) {
            List<IdolCard> ranked = new ArrayList<>(initialRoster);
            ranked.sort((a, b) -> {
                int pa = pointsMap.containsKey(a.getId()) ? pointsMap.get(a.getId()) : 0;
                int pb = pointsMap.containsKey(b.getId()) ? pointsMap.get(b.getId()) : 0;
                return Integer.compare(pb, pa);
            });
            champion = ranked.get(0);
            if (ranked.size() > 1) runnerUp = ranked.get(1);
            for (int i = 2; i < Math.min(4, ranked.size()); i++) {
                semiFinalists.add(ranked.get(i));
            }
            finished = true;
        } else {
            currentLeft = roundRobinPairs.get(rrIndex)[0];
            currentRight = roundRobinPairs.get(rrIndex)[1];
        }
    }

    private void handleBracketPick(IdolCard winner, IdolCard loser) {
        nextRoundWinners.add(winner);
        if (currentRoundPool.size() <= 4 && currentRoundPool.size() > 2) {
            semiFinalists.add(loser);
        }
        matchIndexInRound += 2;
        advanceToNextPairBracket();
    }

    private void advanceToNextPairBracket() {
        // Handle odd card out (automatic bye to next round)
        if (matchIndexInRound == currentRoundPool.size() - 1) {
            nextRoundWinners.add(currentRoundPool.get(matchIndexInRound));
            matchIndexInRound++;
        }

        if (matchIndexInRound >= currentRoundPool.size()) {
            if (nextRoundWinners.size() == 1) {
                champion = nextRoundWinners.get(0);
                if (!history.isEmpty()) {
                    runnerUp = history.get(history.size() - 1).loser;
                }
                finished = true;
                return;
            }
            currentRoundPool = new ArrayList<>(nextRoundWinners);
            nextRoundWinners.clear();
            matchIndexInRound = 0;
            roundNumber++;
        }

        currentLeft = currentRoundPool.get(matchIndexInRound);
        currentRight = currentRoundPool.get(matchIndexInRound + 1);
    }

    private void handleDoubleElimPick(IdolCard winner, IdolCard loser) {
        if (inGrandFinalPhase) {
            champion = winner;
            runnerUp = loser;
            finished = true;
            return;
        }

        nextRoundWinners.add(winner);
        if (!inLosersBracketPhase) {
            // First loss sends card to Redemption Bracket
            losersBracketPool.add(loser);
        } else {
            if (currentRoundPool.size() <= 3) {
                semiFinalists.add(loser);
            }
        }
        matchIndexInRound += 2;

        if (matchIndexInRound == currentRoundPool.size() - 1) {
            nextRoundWinners.add(currentRoundPool.get(matchIndexInRound));
            matchIndexInRound++;
        }

        if (matchIndexInRound >= currentRoundPool.size()) {
            if (!inLosersBracketPhase) {
                if (nextRoundWinners.size() == 1) {
                    winnersBracketChamp = nextRoundWinners.get(0);
                    inLosersBracketPhase = true;
                    currentRoundPool = new ArrayList<>(losersBracketPool);
                    nextRoundWinners.clear();
                    matchIndexInRound = 0;
                    if (currentRoundPool.size() == 1) {
                        inGrandFinalPhase = true;
                        currentLeft = winnersBracketChamp;
                        currentRight = currentRoundPool.get(0);
                        return;
                    }
                } else {
                    currentRoundPool = new ArrayList<>(nextRoundWinners);
                    nextRoundWinners.clear();
                    matchIndexInRound = 0;
                    roundNumber++;
                }
            } else {
                if (nextRoundWinners.size() == 1) {
                    inGrandFinalPhase = true;
                    currentLeft = winnersBracketChamp;
                    currentRight = nextRoundWinners.get(0);
                    return;
                } else {
                    currentRoundPool = new ArrayList<>(nextRoundWinners);
                    nextRoundWinners.clear();
                    matchIndexInRound = 0;
                    roundNumber++;
                }
            }
        }

        currentLeft = currentRoundPool.get(matchIndexInRound);
        currentRight = currentRoundPool.get(matchIndexInRound + 1);
    }

    public String getCurrentRoundTitle() {
        return getCurrentRoundTitle(null);
    }

    public String getCurrentRoundTitle(Context context) {
        switch (gameMode) {
            case "KING_OF_THE_HILL":
                int totalCh = initialRoster.size() - 1;
                if (context != null) {
                    return context.getString(R.string.koth_round_badge, kothQueueIndex, totalCh, currentKingStreak);
                }
                return "👑 KING OF THE HILL • CHALLENGER " + kothQueueIndex + "/" + totalCh
                        + " • STREAK: " + currentKingStreak + "🔥";

            case "ROUND_ROBIN":
                return "📊 POINTS LEAGUE • MATCH " + (rrIndex + 1) + "/" + roundRobinPairs.size();

            case "DOUBLE_ELIMINATION":
                if (inGrandFinalPhase) return "🏆 GRAND FINAL (WINNERS vs REDEMPTION)";
                int p = (matchIndexInRound / 2) + 1;
                int tot = Math.max(1, currentRoundPool.size() / 2);
                if (inLosersBracketPhase) {
                    return "♻️ REDEMPTION BRACKET • MATCH " + p + "/" + tot;
                }
                return "⚔️ WINNERS BRACKET R" + roundNumber + " • MATCH " + p + "/" + tot;

            case "BRACKET":
            default:
                int size = currentRoundPool.size();
                int pairNum = (matchIndexInRound / 2) + 1;
                int totalPairs = Math.max(1, size / 2);
                String stage;
                if (size == 2) stage = "🏆 GRAND FINAL";
                else if (size <= 4) stage = "⚡ SEMIFINALS";
                else if (size <= 8) stage = "🔥 QUARTERFINALS";
                else if (size <= 16) stage = "✨ ROUND OF 16";
                else if (size <= 32) stage = "🌟 ROUND OF 32";
                else stage = "🚀 ROUND OF " + size;
                return stage + " • MATCH " + pairNum + "/" + totalPairs;
        }
    }

    public int getProgressPercent() {
        switch (gameMode) {
            case "KING_OF_THE_HILL":
                return Math.round((kothQueueIndex * 100f) / Math.max(1, initialRoster.size()));
            case "ROUND_ROBIN":
                return Math.round(((rrIndex + 1) * 100f) / Math.max(1, roundRobinPairs.size()));
            default:
                int totalMatches = Math.max(1, initialRoster.size() - 1);
                return Math.min(100, Math.round(((history.size() + 1) * 100f) / totalMatches));
        }
    }

    public IdolCard getCurrentLeft() { return currentLeft; }
    public IdolCard getCurrentRight() { return currentRight; }
    public boolean isFinished() { return finished; }
    public IdolCard getChampion() { return champion; }
    public IdolCard getRunnerUp() { return runnerUp; }
    public List<IdolCard> getSemiFinalists() { return semiFinalists; }
    public List<MatchHistoryItem> getHistory() { return history; }
    public String getTournamentTitle() { return tournamentTitle; }
    public String getGameMode() { return gameMode; }
    public List<IdolCard> getInitialRoster() { return initialRoster; }
}
