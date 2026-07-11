package com.group28.leaderboard.io;

import com.group28.leaderboard.core.LeaderboardManager;
import com.group28.leaderboard.model.Competition;
import com.group28.leaderboard.model.Judge;
import com.group28.leaderboard.model.Participant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CSVManagerTest {

    @TempDir
    Path tempDir;

    @Test
    void loadsCompetitionsAndParticipants() throws Exception {
        Path file = tempDir.resolve("participants.csv");
        Files.writeString(file, """
                # comment line
                c1,Singing,p1,Alice
                c1,Singing,p2,Bob

                c2,Dance,p3,Carol
                this line is malformed and should be skipped
                """);

        List<Competition> competitions = CSVManager.loadCompetitions(file);

        assertEquals(2, competitions.size());
        Competition singing = competitions.get(0);
        assertEquals("Singing", singing.getName());
        assertEquals(2, singing.getParticipants().size());
        assertEquals("Alice", singing.getParticipant("p1").getName());
        assertEquals(1, competitions.get(1).getParticipants().size());
    }

    @Test
    void loadsJudges() throws Exception {
        Path file = tempDir.resolve("judges.csv");
        Files.writeString(file, """
                # judgeId,judgeName
                j1,Prof. Rao
                j2,Dr. Khan
                """);

        List<Judge> judges = CSVManager.loadJudges(file);

        assertEquals(2, judges.size());
        assertEquals("Prof. Rao", judges.get(0).getName());
    }

    @Test
    void exportsLeaderboardInRankOrder() throws Exception {
        Competition competition = new Competition("c1", "Singing");
        Participant alice = new Participant("p1", "Alice");
        Participant bob = new Participant("p2", "Bob");
        competition.addParticipant(alice);
        competition.addParticipant(bob);
        competition.getLeaderboard().addScore(bob, "Judge A", 75);
        competition.getLeaderboard().addScore(alice, "Judge A", 60);

        Path file = tempDir.resolve("export.csv");
        CSVManager.exportLeaderboard(competition, file);

        List<String> lines = Files.readAllLines(file);
        assertEquals("rank,participant,average", lines.get(0));
        assertEquals("1,Bob,75", lines.get(1));
        assertEquals("2,Alice,60", lines.get(2));
    }

    @Test
    void savedScoresSurviveARestart() throws Exception {
        // first "run" of the app: two judges score Alice, average 50
        LeaderboardManager manager = new LeaderboardManager();
        Competition competition = new Competition("c1", "Singing");
        competition.addParticipant(new Participant("p1", "Alice"));
        manager.addCompetition(competition);
        competition.getLeaderboard().addScore(competition.getParticipant("p1"), "Judge A", 40);
        competition.getLeaderboard().addScore(competition.getParticipant("p1"), "Judge B", 60);

        Path file = tempDir.resolve("saved_scores.csv");
        CSVManager.saveScores(manager.getCompetitions(), file);

        // second "run": fresh objects, same data files
        LeaderboardManager restarted = new LeaderboardManager();
        Competition freshCompetition = new Competition("c1", "Singing");
        freshCompetition.addParticipant(new Participant("p1", "Alice"));
        restarted.addCompetition(freshCompetition);
        CSVManager.loadScores(restarted, file);

        // both judge scores restored, so the average is back to 50
        assertEquals(2, freshCompetition.getLeaderboard()
                .getJudgeScores(freshCompetition.getParticipant("p1")).size());
        assertEquals(50.0, freshCompetition.getLeaderboard()
                .getScore(freshCompetition.getParticipant("p1")));
    }
}
