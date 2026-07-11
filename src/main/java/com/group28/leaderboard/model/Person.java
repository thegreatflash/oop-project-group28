package com.group28.leaderboard.model;

/**
 * Base class for anyone in the system (participants, judges).
 */
public abstract class Person {

    private final String id;
    private final String name;

    protected Person(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public abstract String getRole();

    @Override
    public String toString() {
        return name;
    }
}
