package com.group28.leaderboard.core;

import com.group28.leaderboard.model.Competition;
import com.group28.leaderboard.model.Judge;
import com.group28.leaderboard.model.Participant;
import com.group28.leaderboard.model.Score;

import java.util.List;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Submits random scores every few hundred milliseconds. Handy for
 * demos: start it and watch the leaderboard reorder itself live.
 */
public class ScoreSimulator {

    private final LeaderboardManager manager;
    private final ScoreProcessor processor;
    private final Random random = new Random();
    private ScheduledExecutorService timer;

    public ScoreSimulator(LeaderboardManager manager, ScoreProcessor processor) {
        this.manager = manager;
        this.processor = processor;
    }

    public void start() {
        if (isRunning()) {
            return;
        }
        timer = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "score-simulator");
            t.setDaemon(true);
            return t;
        });
        timer.scheduleAtFixedRate(this::submitRandomScore, 0, 400, TimeUnit.MILLISECONDS);
    }

    public void stop() {
        if (timer != null) {
            timer.shutdownNow();
            timer = null;
        }
    }

    public boolean isRunning() {
        return timer != null;
    }

    private void submitRandomScore() {
        List<Competition> competitions = manager.getCompetitions();
        List<Judge> judges = manager.getJudges();
        if (competitions.isEmpty() || judges.isEmpty()) {
            return;
        }
        Competition competition = competitions.get(random.nextInt(competitions.size()));
        List<Participant> participants = competition.getParticipants();
        if (participants.isEmpty()) {
            return;
        }
        Participant participant = participants.get(random.nextInt(participants.size()));
        Judge judge = judges.get(random.nextInt(judges.size()));
        double value = 50 + random.nextInt(51); // 50 to 100, like a real judge score

        try {
            processor.submit(new Score(competition.getId(), participant.getId(), judge.getName(), value));
        } catch (InvalidScoreException e) {
            // can't happen, we only generate valid values
        }
    }
}
