package com.group28.leaderboard.core;

/**
 * One row of a leaderboard: rank, who, and their score (the average of
 * that entrant's judge scores).
 */
public record Ranking<T>(int rank, T entrant, double score) {
}
