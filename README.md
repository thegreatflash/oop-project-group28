# Competition Leaderboard System

CS F213 (Object Oriented Programming) — Part B implementation, Group 28.

A Spring Boot web app for running live competition leaderboards at a college fest.
Judges submit scores through a web page, worker threads apply them, and the
leaderboard page updates automatically. This is a simplified version of the system
proposed in Part A: instead of thousands of users we simulate 3 competitions with
20 participants each and 3 judges, which keeps the concurrency easy to follow.

## How to run

Full step-by-step execution instructions (including cloud setup and demo flow)
are in **[HOW_TO_RUN.md](HOW_TO_RUN.md)**. Quick version below.

Requirements: JDK 21+. Maven is **not** required — the bundled wrapper
(`./mvnw`) downloads it automatically.

```
./mvnw spring-boot:run
```

Then open **http://localhost:8080** in a browser. Run it from the project root so
the app can find the `data/` folder. Stop the server with Ctrl+C.
(On Windows use `mvnw.cmd spring-boot:run`.)

### Run it on GitHub (Codespaces, no local setup)

You can also run the whole app in the cloud straight from the repo:

1. On the GitHub repo page, click the green **Code** button → **Codespaces** tab →
   **Create codespace on main**.
2. Wait for it to build (VS Code opens in the browser with Java 21 + Maven ready).
3. In its terminal run `./mvnw spring-boot:run`.
4. A popup offers to open the forwarded port 8080 — click it to use the app.

(GitHub Pages cannot host this app because it only serves static files and cannot
run a Java backend; Codespaces runs a real server, so it works.)

Tests:

```
./mvnw test
```

## Login and roles

The app is protected by Spring Security (form login, BCrypt-hashed in-memory
users). Every page requires signing in, and what you can open depends on your
role — participants can watch the leaderboard but cannot score, judges can
score but cannot administer:

| Username | Password | Role | Can open |
|---|---|---|---|
| `participant1` … `participant3` | `1234` | PARTICIPANT | Home, Leaderboard |
| `judge1` … `judge3` | `1234` | JUDGE | + Judge (submit scores) |
| `admin` | `1234` | ADMIN | + Admin (reset, export, simulator) |

There is a Sign out button in the navigation bar.

## Pages

- **Home** (`/`) — lists the competitions, click to open a leaderboard.
- **Leaderboard** (`/leaderboard`) — live rankings (rank, participant, average score).
  A small JavaScript polls a JSON endpoint every 3 seconds, so the table refreshes
  without reloading the page. Pick a different competition from the dropdown, and
  use the **Show** selector (All / Top 5 / Top 10) for the O(k) Top-K view.
- **Judge** (`/judge`) — pick judge, competition and participant, enter a score
  (0–100), submit. Invalid scores show an error message and are rejected.
- **Admin** (`/admin`) — reset a competition's scores, export the standings to CSV,
  and start/stop the score simulator (submits random judge scores every 0.4 s,
  useful for watching the leaderboard reorder itself live).

### How scores combine

Judges enter **whole numbers** from 0 to 100 (decimals are rejected). Each judge
has **one score per participant**: if the same judge submits again for that
participant, the new value **replaces** their old one. A participant's leaderboard
score is the **average** of their judges' scores with the decimal dropped (e.g.
an average of 73.9 shows as 73). Because every judge score is 0–100, the average
is always 0–100 too — it never runs past 100.

Example: Judge A gives Alice 90 and Judge B gives Alice 70 → Alice's score is 80.
If Judge A then changes their score to 100, Alice becomes (100 + 70) / 2 = 85.
Add a third judge who gives 65 → (100 + 70 + 65) / 3 = 78.3, shown as 78.

Scores are saved to `data/saved_scores.csv` when the server stops and loaded back
on the next start.

## Project structure

```
data/
  participants.csv        input: competitionId,competitionName,participantId,participantName
  judges.csv              input: judgeId,judgeName
  saved_scores.csv        written on shutdown, loaded on start
docs/
  uml.puml                PlantUML class diagram
screenshots/              screenshots for the report
src/main/java/com/group28/leaderboard/
  LeaderboardApplication.java   Spring Boot entry point
  model/                  Person (abstract), Participant, Judge, Competition, Score
  core/                   Leaderboard, LeaderboardManager, ScoreProcessor,
                          ScoreSimulator, Ranking, InvalidScoreException
  io/                     CSVManager (all file reading/writing)
  web/                    LeaderboardService (wires the backend to Spring),
                          PageController (serves the pages + JSON endpoint),
                          SecurityConfig (login and role-based access)
src/main/resources/
  templates/              Thymeleaf HTML pages (login, home, leaderboard, judge, admin)
  static/                 style.css and leaderboard.js
src/test/java/            JUnit 5 tests
```

The `model/`, `core/` and `io/` packages are plain Java with no Spring code — the
whole engine could run outside a web app. The `web/` package is the only part that
knows about Spring.

## How a score flows through the system

1. A judge submits the form on `/judge` (or the simulator generates a score).
2. `ScoreProcessor.submit()` validates the value and throws `InvalidScoreException`
   if it is out of range, so the judge sees the error immediately.
3. Valid scores are handed to a fixed thread pool (`ExecutorService`, 3 workers),
   which calls `LeaderboardManager.applyScore()`.
4. The competition's `Leaderboard` records the value under that judge's name for
   the participant (in a nested `ConcurrentHashMap`), replacing any earlier score
   from the same judge, and repositions only that participant in a
   `ConcurrentSkipListMap` ranking index — one remove + one insert, O(log n),
   never a full re-sort. The participant's shown score is the average of their judges.
5. The manager fires a `PropertyChangeEvent`; a listener records the "last updated"
   time, and the browser picks up the new standings on its next 3-second poll.

Because Spring Boot's server is itself multi-threaded (one thread per request),
several judges submitting at the same time really do hit the leaderboard
concurrently — which is exactly what the `ConcurrentHashMap` handles safely.

## Advanced Java features used

- **Collections / concurrency** — scores live in a nested `ConcurrentHashMap`
  (participant → judge → score). Every update goes through `compute()`, which
  locks only that participant's entry: two judges scoring the *same* participant
  apply one after the other (no lost update), while different participants update
  fully in parallel. The live ranking is a `ConcurrentSkipListMap` keyed by
  (score desc, name, id): a score update repositions one participant in O(log n)
  instead of re-sorting everyone, reading the standings is a plain walk of the
  already-ordered map, and Top-K retrieval is O(k)
  (`/api/leaderboard?competition=singing&top=5`). Readers never block writers.
- **ExecutorService** — a fixed pool of 3 worker threads processes score
  submissions off the request thread; a `ScheduledExecutorService` drives the simulator.
- **Observer pattern** — `LeaderboardManager` uses `PropertyChangeSupport`
  (java.beans) to notify a listener whenever standings change.
- **Generics** — `Leaderboard<T extends Person>` and `Ranking<T>` can rank any
  kind of person; the app instantiates `Leaderboard<Participant>`.
- **Inheritance & polymorphism** — `Participant` and `Judge` extend the abstract
  `Person` class and override `getRole()`/`toString()`.
- **Custom checked exception** — `InvalidScoreException` for out-of-range scores.
- **Records** — `Score` and `Ranking` are immutable records, safe to pass between threads.
- **File I/O** — `CSVManager` handles all reading/writing with `java.nio.file`;
  malformed CSV rows are skipped instead of crashing the app.
- **Spring Security** — role-based access control (PARTICIPANT / JUDGE / ADMIN)
  with form login and BCrypt password hashing.

## Simplifications compared to the Part A proposal

The proposal targeted 90+ events and 1000+ submissions/sec. We kept the core
algorithmic idea — `ConcurrentHashMap` for participant data plus a
`ConcurrentSkipListMap` for incremental O(log n) re-ranking and O(k) Top-K —
but scaled the simulation down to 3 competitions × 20 participants and 3 judges,
which is enough to demonstrate the design. We also left out WebSockets and a
distributed deployment — the leaderboard refreshes by simple JavaScript polling,
and the whole system runs in one process with file-based persistence, which
matches the course scope.

## Tests

`mvn test` runs 20 tests covering:

- averaging, same-judge replacement, ranking order, Top-K, ties and reset (`LeaderboardTest`)
- invalid and non-integer score rejection (`ScoreProcessorTest`)
- concurrent submissions losing no updates, and the skip-list ranking index
  staying consistent under concurrent repositioning (`ConcurrentSubmissionTest`)
- CSV loading, exporting and save/restore of judge scores (`CSVManagerTest`)
