package com.group28.leaderboard.io;

import com.group28.leaderboard.core.LeaderboardManager;
import com.group28.leaderboard.core.Ranking;
import com.group28.leaderboard.model.Competition;
import com.group28.leaderboard.model.Judge;
import com.group28.leaderboard.model.Participant;
import com.group28.leaderboard.model.Score;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * All the CSV reading/writing lives here.
 *
 * File formats (lines starting with # are ignored):
 *   participants.csv : competitionId,competitionName,participantId,participantName
 *   judges.csv       : judgeId,judgeName
 *   saved_scores.csv : competitionId,participantId,total
 */
public final class CSVManager {

    private CSVManager() {
        // static methods only
    }

    public static List<Competition> loadCompetitions(Path file) throws IOException {
        Map<String, Competition> byId = new LinkedHashMap<>();
        for (String line : Files.readAllLines(file)) {
            String[] parts = splitLine(line, 4);
            if (parts == null) {
                continue;
            }
            Competition competition = byId.computeIfAbsent(parts[0],
                    id -> new Competition(id, parts[1]));
            competition.addParticipant(new Participant(parts[2], parts[3]));
        }
        return new ArrayList<>(byId.values());
    }

    public static List<Judge> loadJudges(Path file) throws IOException {
        List<Judge> judges = new ArrayList<>();
        for (String line : Files.readAllLines(file)) {
            String[] parts = splitLine(line, 2);
            if (parts == null) {
                continue;
            }
            judges.add(new Judge(parts[0], parts[1]));
        }
        return judges;
    }

    /** Writes the current standings of one competition, nicely formatted. */
    public static void exportLeaderboard(Competition competition, Path file) throws IOException {
        List<String> lines = new ArrayList<>();
        lines.add("rank,participant,average");
        for (Ranking<Participant> row : competition.getLeaderboard().getRankings()) {
            lines.add(row.rank() + "," + row.entrant().getName() + ","
                    + String.format("%.0f", row.score()));
        }
        Files.write(file, lines);
    }

    /**
     * Saves every judge's individual score so the averages can be rebuilt
     * exactly on the next start. One line per (competition, participant, judge).
     */
    public static void saveScores(List<Competition> competitions, Path file) throws IOException {
        List<String> lines = new ArrayList<>();
        for (Competition competition : competitions) {
            for (Ranking<Participant> row : competition.getLeaderboard().getRankings()) {
                Participant participant = row.entrant();
                Map<String, Double> judgeScores =
                        competition.getLeaderboard().getJudgeScores(participant);
                for (Map.Entry<String, Double> entry : judgeScores.entrySet()) {
                    lines.add(competition.getId() + "," + participant.getId() + ","
                            + entry.getKey() + "," + entry.getValue());
                }
            }
        }
        Files.write(file, lines);
    }

    /** Restores the judge scores saved by saveScores(). */
    public static void loadScores(LeaderboardManager manager, Path file) throws IOException {
        for (String line : Files.readAllLines(file)) {
            String[] parts = splitLine(line, 4);
            if (parts == null) {
                continue;
            }
            try {
                double value = Double.parseDouble(parts[3]);
                manager.applyScore(new Score(parts[0], parts[1], parts[2], value));
            } catch (NumberFormatException e) {
                // skip malformed row instead of crashing on startup
            }
        }
    }

    /** Returns the trimmed fields, or null if the line is blank/comment/malformed. */
    private static String[] splitLine(String line, int expectedFields) {
        line = line.trim();
        if (line.isEmpty() || line.startsWith("#")) {
            return null;
        }
        String[] parts = line.split(",");
        if (parts.length != expectedFields) {
            return null;
        }
        for (int i = 0; i < parts.length; i++) {
            parts[i] = parts[i].trim();
        }
        return parts;
    }
}
