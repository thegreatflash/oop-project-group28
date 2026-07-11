package com.group28.leaderboard.core;

import com.group28.leaderboard.model.Score;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Takes score submissions and applies them on a small thread pool,
 * so the UI thread never does the ranking work itself. Validation
 * happens up front on the caller's thread so the judge gets an
 * immediate error for a bad score.
 */
public class ScoreProcessor {

    public static final double MIN_SCORE = 0;
    public static final double MAX_SCORE = 100;

    private final ExecutorService pool = Executors.newFixedThreadPool(3);
    private final LeaderboardManager manager;

    public ScoreProcessor(LeaderboardManager manager) {
        this.manager = manager;
    }

    public void submit(Score score) throws InvalidScoreException {
        validate(score);
        pool.submit(() -> manager.applyScore(score));
    }

    public static void validate(Score score) throws InvalidScoreException {
        double v = score.value();
        if (Double.isNaN(v) || v < MIN_SCORE || v > MAX_SCORE) {
            throw new InvalidScoreException(
                    "Score must be between " + MIN_SCORE + " and " + MAX_SCORE + " (got " + v + ")");
        }
        if (v != Math.floor(v)) {
            throw new InvalidScoreException("Score must be a whole number (got " + v + ")");
        }
    }

    /** Shuts down the pool and waits for queued scores to finish. */
    public void shutdown() {
        pool.shutdown();
        try {
            pool.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
