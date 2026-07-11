package com.group28.leaderboard.core;

/**
 * Thrown when a judge submits a score outside the allowed range.
 */
public class InvalidScoreException extends Exception {

    public InvalidScoreException(String message) {
        super(message);
    }
}
