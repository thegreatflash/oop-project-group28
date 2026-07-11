package com.group28.leaderboard.core;

import com.group28.leaderboard.model.Competition;
import com.group28.leaderboard.model.Participant;
import com.group28.leaderboard.model.Score;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ScoreProcessorTest {

    private LeaderboardManager manager;
    private ScoreProcessor processor;

    @BeforeEach
    void setUp() {
        manager = new LeaderboardManager();
        Competition competition = new Competition("c1", "Test Competition");
        competition.addParticipant(new Participant("p1", "Alice"));
        manager.addCompetition(competition);
        processor = new ScoreProcessor(manager);
    }

    @AfterEach
    void tearDown() {
        processor.shutdown();
    }

    @Test
    void negativeScoreIsRejected() {
        Score score = new Score("c1", "p1", "Judge", -5);
        assertThrows(InvalidScoreException.class, () -> processor.submit(score));
    }

    @Test
    void scoreAbove100IsRejected() {
        Score score = new Score("c1", "p1", "Judge", 100.5);
        assertThrows(InvalidScoreException.class, () -> processor.submit(score));
    }

    @Test
    void decimalScoreIsRejected() {
        Score score = new Score("c1", "p1", "Judge", 90.5);
        assertThrows(InvalidScoreException.class, () -> processor.submit(score));
    }

    @Test
    void boundaryScoresAreAccepted() {
        assertDoesNotThrow(() -> processor.submit(new Score("c1", "p1", "Judge", 0)));
        assertDoesNotThrow(() -> processor.submit(new Score("c1", "p1", "Judge", 100)));
    }

    @Test
    void rejectedScoreDoesNotChangeTheLeaderboard() {
        Competition competition = manager.getCompetition("c1");
        Participant alice = competition.getParticipant("p1");

        assertThrows(InvalidScoreException.class,
                () -> processor.submit(new Score("c1", "p1", "Judge", 999)));
        processor.shutdown(); // make sure nothing is still queued

        assertEquals(0.0, competition.getLeaderboard().getScore(alice));
    }
}
