package com.group28.leaderboard.core;

import com.group28.leaderboard.model.Competition;
import com.group28.leaderboard.model.Judge;
import com.group28.leaderboard.model.Participant;
import com.group28.leaderboard.model.Score;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Central class that holds all competitions and applies score updates.
 * Fires PropertyChangeEvents so the UI can react when standings change
 * (observer pattern via java.beans).
 */
public class LeaderboardManager {

    public static final String SCORE_APPLIED = "scoreApplied";
    public static final String SCORES_RESET = "scoresReset";

    private final Map<String, Competition> competitions = new ConcurrentHashMap<>();
    private final List<Judge> judges = new ArrayList<>();
    private final PropertyChangeSupport changes = new PropertyChangeSupport(this);

    public void addCompetition(Competition competition) {
        competitions.put(competition.getId(), competition);
    }

    public Competition getCompetition(String id) {
        return competitions.get(id);
    }

    public List<Competition> getCompetitions() {
        List<Competition> list = new ArrayList<>(competitions.values());
        list.sort(Comparator.comparing(Competition::getName));
        return list;
    }

    public void setJudges(List<Judge> loaded) {
        judges.clear();
        judges.addAll(loaded);
    }

    public List<Judge> getJudges() {
        return judges;
    }

    /**
     * Applies one score to the right competition. Called from the
     * ScoreProcessor's worker threads.
     */
    public void applyScore(Score score) {
        Competition competition = competitions.get(score.competitionId());
        if (competition == null) {
            return; // unknown competition, nothing to do
        }
        Participant participant = competition.getParticipant(score.participantId());
        if (participant == null) {
            return;
        }
        competition.getLeaderboard().addScore(participant, score.judgeName(), score.value());
        changes.firePropertyChange(SCORE_APPLIED, null, score);
    }

    public void resetScores(Competition competition) {
        competition.getLeaderboard().reset();
        changes.firePropertyChange(SCORES_RESET, null, competition.getId());
    }

    public void addListener(PropertyChangeListener listener) {
        changes.addPropertyChangeListener(listener);
    }

    public void removeListener(PropertyChangeListener listener) {
        changes.removePropertyChangeListener(listener);
    }
}
