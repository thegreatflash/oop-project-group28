package com.group28.leaderboard.model;

/**
 * One score submission from a judge. Immutable, so it can be passed
 * between threads safely.
 */
public record Score(String competitionId, String participantId, String judgeName, double value) {
}
