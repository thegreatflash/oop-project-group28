package com.group28.leaderboard.core;

import com.group28.leaderboard.model.Competition;
import com.group28.leaderboard.model.Participant;
import com.group28.leaderboard.model.Score;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks that no score updates get lost when many judges submit at once.
 * Each judge is a distinct name, so every submission must be recorded; if any
 * were lost, the recorded judge count and the average would both be wrong.
 */
class ConcurrentSubmissionTest {

    @Test
    void concurrentScoresFromDistinctJudgesAreNeverLost() throws Exception {
        Leaderboard<Participant> leaderboard = new Leaderboard<>();
        Participant alice = new Participant("p1", "Alice");

        int judges = 50;
        double expectedSum = 0;
        for (int i = 0; i < judges; i++) {
            expectedSum += 60 + (i % 41); // each value is 60..100
        }

        ExecutorService pool = Executors.newFixedThreadPool(8);
        CountDownLatch done = new CountDownLatch(judges);
        for (int i = 0; i < judges; i++) {
            int judgeIndex = i;
            pool.submit(() -> {
                leaderboard.addScore(alice, "Judge " + judgeIndex, 60 + (judgeIndex % 41));
                done.countDown();
            });
        }

        assertTrue(done.await(10, TimeUnit.SECONDS), "workers did not finish in time");
        pool.shutdown();

        assertEquals(judges, leaderboard.getJudgeScores(alice).size());
        assertEquals(Math.floor(expectedSum / judges), leaderboard.getScore(alice));
        // the ranking index must hold Alice exactly once, at her final score
        assertEquals(1, leaderboard.getRankings().size());
        assertEquals(leaderboard.getScore(alice), leaderboard.getRankings().get(0).score());
    }

    @Test
    void rankingIndexStaysConsistentUnderConcurrentRepositioning() throws Exception {
        // Two participants being re-scored from many threads at once: every
        // update repositions someone in the skip list. Afterwards the board
        // must contain each participant exactly once, in the right order.
        Leaderboard<Participant> leaderboard = new Leaderboard<>();
        Participant alice = new Participant("p1", "Alice");
        Participant bob = new Participant("p2", "Bob");
        leaderboard.addEntrant(alice);
        leaderboard.addEntrant(bob);

        int rounds = 200;
        ExecutorService pool = Executors.newFixedThreadPool(8);
        CountDownLatch done = new CountDownLatch(rounds);
        for (int i = 0; i < rounds; i++) {
            int round = i;
            pool.submit(() -> {
                Participant target = (round % 2 == 0) ? alice : bob;
                leaderboard.addScore(target, "Judge " + (round % 3), round % 101);
                done.countDown();
            });
        }

        assertTrue(done.await(10, TimeUnit.SECONDS), "workers did not finish in time");
        pool.shutdown();

        List<Ranking<Participant>> rankings = leaderboard.getRankings();
        assertEquals(2, rankings.size());
        assertEquals(1, rankings.get(0).rank());
        assertEquals(2, rankings.get(1).rank());
        // positions must match the final scores
        assertTrue(rankings.get(0).score() >= rankings.get(1).score());
    }

    @Test
    void concurrentSubmissionsThroughProcessorAllArrive() throws Exception {
        LeaderboardManager manager = new LeaderboardManager();
        Competition competition = new Competition("c1", "Test Competition");
        Participant alice = new Participant("p1", "Alice");
        competition.addParticipant(alice);
        manager.addCompetition(competition);

        ScoreProcessor processor = new ScoreProcessor(manager);

        int judges = 20;
        double expectedSum = 0;
        for (int i = 0; i < judges; i++) {
            expectedSum += 55 + (i % 46); // each value is 55..100
        }

        ExecutorService judgePool = Executors.newFixedThreadPool(judges);
        CountDownLatch done = new CountDownLatch(judges);
        for (int i = 0; i < judges; i++) {
            int judgeIndex = i;
            judgePool.submit(() -> {
                try {
                    processor.submit(new Score("c1", "p1", "Judge " + judgeIndex, 55 + (judgeIndex % 46)));
                } catch (InvalidScoreException e) {
                    throw new RuntimeException(e);
                } finally {
                    done.countDown();
                }
            });
        }

        assertTrue(done.await(10, TimeUnit.SECONDS), "judges did not finish in time");
        judgePool.shutdown();
        processor.shutdown(); // waits until every queued score is applied

        assertEquals(judges, competition.getLeaderboard().getJudgeScores(alice).size());
        assertEquals(Math.floor(expectedSum / judges),
                competition.getLeaderboard().getScore(alice));
    }
}
