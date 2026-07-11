package com.group28.leaderboard.model;

public class Participant extends Person {

    public Participant(String id, String name) {
        super(id, name);
    }

    @Override
    public String getRole() {
        return "Participant";
    }
}
