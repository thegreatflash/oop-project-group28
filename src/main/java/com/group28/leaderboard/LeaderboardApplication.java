package com.group28.leaderboard;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point. Start with:  mvn spring-boot:run
 * Then open http://localhost:8080 in a browser.
 *
 * Run from the project root so the data/ CSV files are found.
 */
@SpringBootApplication
public class LeaderboardApplication {

    public static void main(String[] args) {
        SpringApplication.run(LeaderboardApplication.class, args);
    }
}
