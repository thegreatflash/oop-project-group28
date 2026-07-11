package com.group28.leaderboard.core;

import com.group28.leaderboard.model.Participant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LeaderboardTest {

    private Leaderboard<Participant> leaderboard;
    private Participant alice;
    private Participant bob;
    private Participant carol;

    @BeforeEach
    void setUp() {
        leaderboard = new Leaderboard<>();
        alice = new Participant("p1", "Alice");
        bob = new Participant("p2", "Bob");
        carol = new Participant("p3", "Carol");
    }

    @Test
    void scoreIsTheAverageOfTheJudges() {
        leaderboard.addScore(alice, "Judge A", 80);
        leaderboard.addScore(alice, "Judge B", 60);

        assertEquals(70.0, leaderboard.getScore(alice));
    }

    @Test
    void sameJudgeSubmittingAgainReplacesTheOldScore() {
        leaderboard.addScore(alice, "Judge A", 40);
        leaderboard.addScore(alice, "Judge A", 90);

        // replaced, not added: still one judge, score is the latest value
        assertEquals(90.0, leaderboard.getScore(alice));
        assertEquals(1, leaderboard.getJudgeScores(alice).size());
    }

    @Test
    void averageNeverGoesAbove100() {
        leaderboard.addScore(alice, "Judge A", 100);
        leaderboard.addScore(alice, "Judge B", 90);
        leaderboard.addScore(alice, "Judge C", 95);

        assertTrue(leaderboard.getScore(alice) <= 100.0);
        assertEquals(95.0, leaderboard.getScore(alice));
    }

    @Test
    void ranksParticipantsByAverage() {
        leaderboard.addScore(alice, "Judge A", 50);
        leaderboard.addScore(bob, "Judge A", 80);
        leaderboard.addScore(carol, "Judge A", 65);

        List<Ranking<Participant>> rankings = leaderboard.getRankings();

        assertEquals(bob, rankings.get(0).entrant());
        assertEquals(1, rankings.get(0).rank());
        assertEquals(carol, rankings.get(1).entrant());
        assertEquals(alice, rankings.get(2).entrant());
    }

    @Test
    void rankingUpdatesWhenANewJudgeScores() {
        leaderboard.addScore(alice, "Judge A", 60);
        leaderboard.addScore(bob, "Judge A", 50);
        assertEquals(alice, leaderboard.getRankings().get(0).entrant());

        // a second judge gives Bob 90, lifting his average to 70 and past Alice
        leaderboard.addScore(bob, "Judge B", 90);
        assertEquals(bob, leaderboard.getRankings().get(0).entrant());
    }

    @Test
    void topKReturnsOnlyTheBestK() {
        leaderboard.addScore(alice, "Judge A", 50);
        leaderboard.addScore(bob, "Judge A", 80);
        leaderboard.addScore(carol, "Judge A", 65);

        List<Ranking<Participant>> top2 = leaderboard.getTopK(2);

        assertEquals(2, top2.size());
        assertEquals(bob, top2.get(0).entrant());
        assertEquals(carol, top2.get(1).entrant());
    }

    @Test
    void tiesAreOrderedByName() {
        leaderboard.addScore(carol, "Judge A", 70);
        leaderboard.addScore(alice, "Judge A", 70);

        List<Ranking<Participant>> rankings = leaderboard.getRankings();
        assertEquals(alice, rankings.get(0).entrant());
        assertEquals(carol, rankings.get(1).entrant());
    }

    @Test
    void resetClearsScoresButKeepsParticipants() {
        leaderboard.addScore(alice, "Judge A", 90);
        leaderboard.addScore(bob, "Judge A", 45);

        leaderboard.reset();

        assertEquals(2, leaderboard.getRankings().size());
        assertEquals(0.0, leaderboard.getScore(alice));
        assertEquals(0.0, leaderboard.getScore(bob));
    }
}
