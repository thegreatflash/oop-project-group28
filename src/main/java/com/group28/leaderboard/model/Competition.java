package com.group28.leaderboard.model;

import com.group28.leaderboard.core.Leaderboard;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A single competition (e.g. Solo Singing) with its participants
 * and its own leaderboard.
 */
public class Competition {

    private final String id;
    private final String name;
    private final Map<String, Participant> participants = new ConcurrentHashMap<>();
    private final Leaderboard<Participant> leaderboard = new Leaderboard<>();

    public Competition(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public void addParticipant(Participant p) {
        participants.put(p.getId(), p);
        // register them so they show up on the leaderboard right away (score 0)
        leaderboard.addEntrant(p);
    }

    public Participant getParticipant(String participantId) {
        return participants.get(participantId);
    }

    public List<Participant> getParticipants() {
        List<Participant> list = new ArrayList<>(participants.values());
        list.sort(Comparator.comparing(Participant::getName));
        return list;
    }

    public Leaderboard<Participant> getLeaderboard() {
        return leaderboard;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return name;
    }
}
