package com.group28.leaderboard.core;

import com.group28.leaderboard.model.Person;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;

/**
 * Keeps each judge's latest score per entrant and maintains a live ranking.
 *
 * An entrant's score is the AVERAGE of their judges' scores with the decimal
 * dropped. Each judge has one score per entrant: if the same judge scores
 * again, the new value replaces the old one.
 *
 * Ranking is maintained incrementally in a ConcurrentSkipListMap ordered by
 * (score desc, name, id). When a score arrives we reposition only that one
 * entrant: one remove + one insert, each O(log n) — never a full re-sort.
 * getRankings() just walks the already-ordered map, and getTopK(k) reads the
 * first k entries in O(k).
 *
 * Thread safety: judge scores live in a ConcurrentHashMap and every update
 * goes through compute(), which locks only that entrant's map entry. So two
 * judges scoring the SAME entrant apply one after the other (no lost update
 * or stale skip-list key), while updates to DIFFERENT entrants run fully in
 * parallel.
 */
public class Leaderboard<T extends Person> {

    // entrant -> (judge name -> that judge's latest score)
    private final ConcurrentHashMap<T, Map<String, Double>> judgeScores = new ConcurrentHashMap<>();

    // live ranking index, always kept in leaderboard order
    private final ConcurrentSkipListMap<RankKey, T> ordered = new ConcurrentSkipListMap<>();

    /**
     * One entrant's position on the board. The id makes keys unique even if
     * two entrants share a name and a score.
     */
    record RankKey(double score, String name, String id) implements Comparable<RankKey> {
        @Override
        public int compareTo(RankKey other) {
            int byScore = Double.compare(other.score, this.score); // higher score first
            if (byScore != 0) {
                return byScore;
            }
            int byName = this.name.compareTo(other.name);
            return byName != 0 ? byName : this.id.compareTo(other.id);
        }
    }

    /** Registers an entrant so they appear on the board (at 0) before any scores. */
    public void addEntrant(T entrant) {
        judgeScores.computeIfAbsent(entrant, e -> {
            ordered.put(keyFor(e, 0), e);
            return new ConcurrentHashMap<>();
        });
    }

    /**
     * Records a judge's score for an entrant (replacing that judge's earlier
     * score, if any) and repositions the entrant in the ranking: one skip-list
     * remove + one insert, O(log n).
     */
    public void addScore(T entrant, String judgeName, double value) {
        judgeScores.compute(entrant, (e, scores) -> {
            if (scores == null) {
                scores = new ConcurrentHashMap<>();
            }
            ordered.remove(keyFor(e, averageOf(scores)));
            scores.put(judgeName, value);
            ordered.put(keyFor(e, averageOf(scores)), e);
            return scores;
        });
    }

    /** The entrant's score: average of their judges, decimal dropped (0 if none yet). */
    public double getScore(T entrant) {
        return averageOf(judgeScores.get(entrant));
    }

    /** A copy of one entrant's judge -> score map (used when saving to CSV). */
    public Map<String, Double> getJudgeScores(T entrant) {
        Map<String, Double> scores = judgeScores.get(entrant);
        return scores == null ? Map.of() : new HashMap<>(scores);
    }

    /**
     * Current standings. The skip list is already in ranking order, so this
     * is a plain walk — no sorting. (Iteration is weakly consistent: a refresh
     * that overlaps an in-flight update simply shows the board a moment later.)
     */
    public List<Ranking<T>> getRankings() {
        List<Ranking<T>> rankings = new ArrayList<>();
        int rank = 1;
        for (Map.Entry<RankKey, T> entry : ordered.entrySet()) {
            rankings.add(new Ranking<>(rank++, entry.getValue(), entry.getKey().score()));
        }
        return rankings;
    }

    /** The best k entrants in O(k) — just the first k entries of the skip list. */
    public List<Ranking<T>> getTopK(int k) {
        List<Ranking<T>> top = new ArrayList<>();
        int rank = 1;
        for (Map.Entry<RankKey, T> entry : ordered.entrySet()) {
            if (rank > k) {
                break;
            }
            top.add(new Ranking<>(rank++, entry.getValue(), entry.getKey().score()));
        }
        return top;
    }

    /** Clears every entrant's scores (they stay on the board, back at 0). */
    public void reset() {
        for (T entrant : judgeScores.keySet()) {
            judgeScores.compute(entrant, (e, scores) -> {
                ordered.remove(keyFor(e, averageOf(scores)));
                ordered.put(keyFor(e, 0), e);
                return new ConcurrentHashMap<>();
            });
        }
    }

    private RankKey keyFor(T entrant, double score) {
        return new RankKey(score, entrant.getName(), entrant.getId());
    }

    /** Average with the decimal dropped; 0 when no judge has scored yet. */
    private static double averageOf(Map<String, Double> scores) {
        if (scores == null || scores.isEmpty()) {
            return 0.0;
        }
        double sum = 0;
        for (double value : scores.values()) {
            sum += value;
        }
        return Math.floor(sum / scores.size());
    }
}
