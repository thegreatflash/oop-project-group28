package com.group28.leaderboard.web;

import com.group28.leaderboard.core.InvalidScoreException;
import com.group28.leaderboard.core.LeaderboardManager;
import com.group28.leaderboard.core.Ranking;
import com.group28.leaderboard.core.ScoreProcessor;
import com.group28.leaderboard.core.ScoreSimulator;
import com.group28.leaderboard.io.CSVManager;
import com.group28.leaderboard.model.Competition;
import com.group28.leaderboard.model.Judge;
import com.group28.leaderboard.model.Participant;
import com.group28.leaderboard.model.Score;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thin Spring layer around the plain-Java backend. It loads the CSV data
 * on startup, holds the manager/processor/simulator, and saves scores on
 * shutdown. Controllers call this instead of touching the engine directly.
 */
@Service
public class LeaderboardService {

    private static final Path PARTICIPANTS_FILE = Path.of("data", "participants.csv");
    private static final Path JUDGES_FILE = Path.of("data", "judges.csv");
    private static final Path SAVED_SCORES_FILE = Path.of("data", "saved_scores.csv");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final LeaderboardManager manager = new LeaderboardManager();
    private final Map<String, String> lastUpdated = new ConcurrentHashMap<>();
    private ScoreProcessor processor;
    private ScoreSimulator simulator;

    @PostConstruct
    void init() throws IOException {
        for (Competition competition : CSVManager.loadCompetitions(PARTICIPANTS_FILE)) {
            manager.addCompetition(competition);
        }
        manager.setJudges(CSVManager.loadJudges(JUDGES_FILE));
        if (Files.exists(SAVED_SCORES_FILE)) {
            CSVManager.loadScores(manager, SAVED_SCORES_FILE);
        }

        processor = new ScoreProcessor(manager);
        simulator = new ScoreSimulator(manager, processor);

        // Observer pattern: whenever a score is applied, remember when this
        // competition last changed, so the page can show a "last updated" time.
        manager.addListener(event -> {
            if (LeaderboardManager.SCORE_APPLIED.equals(event.getPropertyName())) {
                Score score = (Score) event.getNewValue();
                lastUpdated.put(score.competitionId(), LocalTime.now().format(TIME));
            }
        });
    }

    @PreDestroy
    void shutdown() throws IOException {
        simulator.stop();
        processor.shutdown();
        CSVManager.saveScores(manager.getCompetitions(), SAVED_SCORES_FILE);
    }

    public List<Competition> getCompetitions() {
        return manager.getCompetitions();
    }

    public List<Judge> getJudges() {
        return manager.getJudges();
    }

    public Competition getCompetition(String id) {
        return manager.getCompetition(id);
    }

    public List<Ranking<Participant>> getRankings(String competitionId) {
        Competition competition = manager.getCompetition(competitionId);
        return competition.getLeaderboard().getRankings();
    }

    /** The best k participants, read in O(k) from the skip-list index. */
    public List<Ranking<Participant>> getTopRankings(String competitionId, int k) {
        Competition competition = manager.getCompetition(competitionId);
        return competition.getLeaderboard().getTopK(k);
    }

    public String getLastUpdated(String competitionId) {
        return lastUpdated.getOrDefault(competitionId, "not yet");
    }

    /** Submits a score. Throws InvalidScoreException for out-of-range values. */
    public void submitScore(String competitionId, String participantId, String judgeName, double value)
            throws InvalidScoreException {
        processor.submit(new Score(competitionId, participantId, judgeName, value));
    }

    public void reset(String competitionId) {
        manager.resetScores(manager.getCompetition(competitionId));
    }

    public Path export(String competitionId) throws IOException {
        Competition competition = manager.getCompetition(competitionId);
        Path file = Path.of("data", competition.getId() + "_leaderboard.csv");
        CSVManager.exportLeaderboard(competition, file);
        return file;
    }

    /** Starts the simulator if stopped, stops it if running. Returns the new state. */
    public boolean toggleSimulator() {
        if (simulator.isRunning()) {
            simulator.stop();
        } else {
            simulator.start();
        }
        return simulator.isRunning();
    }

    public boolean isSimulatorRunning() {
        return simulator.isRunning();
    }
}
