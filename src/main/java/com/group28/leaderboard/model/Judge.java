package com.group28.leaderboard.model;

public class Judge extends Person {

    public Judge(String id, String name) {
        super(id, name);
    }

    @Override
    public String getRole() {
        return "Judge";
    }
}
