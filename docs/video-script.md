# Demo Video Script — Group 28
### Concurrent Real-Time Competition Leaderboard System (CS F213, Part B)

**Target length:** 15–20 minutes ·
**File name when uploading:** `28_OOP_Summer_2026_Project`
**Speakers:** Aayush Rastogi (Speaker A) and Shaan Sharma (Speaker B)

---

## Before you press record (checklist)

1. `cd` into the project and delete old state so the demo starts clean:
   `rm -f data/saved_scores.csv data/*_leaderboard.csv`
2. Start the app: `mvn spring-boot:run` (or `./mvnw spring-boot:run` in Codespaces).
3. Open **two browser windows side by side** — you will log in as a judge in one
   and keep the leaderboard open in the other. This is how you *show* real-time
   updates instead of describing them.
4. Have the IDE open with these files in tabs, in this order:
   `Leaderboard.java`, `ScoreProcessor.java`, `LeaderboardManager.java`,
   `SecurityConfig.java`, `CSVManager.java`, `ConcurrentSubmissionTest.java`.
5. Keep a terminal visible for `mvn test` near the end.
6. Do one silent dry run — the timings below assume you don't stop to debug.

---

## Part 1 — Introduction (both on screen) — 0:00–1:30

**A:** "Hello, I am Aayush Rastogi, 2023B2A30888H, from Group 28."

**B:** "And I am Shaan Sharma, 2023B1AD0949H."

**A:** "Our project is a Concurrent Real-Time Competition Leaderboard System for
the college festival platform, under the Data Structures and Algorithms track.
The problem: during multi-stage competitions, judges submit scores from tablets
at the same time, and the existing system recalculates every rank from scratch
on each score, which lags during finals."

**B:** "Our solution processes concurrent score submissions on a thread pool and
maintains the ranking incrementally in a skip list, so one score update costs
O(log n) instead of a full re-sort. I will show the security, admin and
integration modules later; first Aayush walks through the core."

---

## Part 2 — Architecture in one minute (Speaker A) — 1:30–2:45

*[SCREEN: README project-structure section, or the UML diagram docs/uml.png]*

**A:** "The code has four layers. `model` holds the domain classes — an abstract
`Person` with `Participant` and `Judge` subclasses, `Competition`, and an
immutable `Score` record. `core` is the engine — `Leaderboard`,
`LeaderboardManager`, `ScoreProcessor` and the simulator; it is plain Java with
no framework code, so it could be reused outside the web app. `io` does all CSV
file exchange, which is our integration interface with the festival backend.
And `web` is a thin Spring Boot layer — one service, one controller, and the
security configuration."

---

## Part 3 — Live demo: core flow (Speaker A) — 2:45–7:30

*[SCREEN: browser window 1]*

1. **Login page** — "Every page requires signing in. We have three roles —
   participant, judge, admin — with different permissions." Log in as
   `judge1 / 1234`.
2. **Home page** — "Three competitions, twenty participants each, loaded from
   `participants.csv` at startup." Open **Solo Singing** leaderboard.
3. **Leaderboard page** — "Everyone starts at rank order with zero points. The
   page polls a JSON endpoint every three seconds, so it refreshes by itself —
   watch the 'last updated' time tick."
4. *[Arrange window 2 next to it, logged in as judge1, on the Judge page]*
   **Submit a score:** competition Solo Singing, any participant, score **90**.
   "Within a refresh cycle the leaderboard window reorders — no manual reload."
5. **Invalid input:** submit **150** → red error. "Validation throws our custom
   checked `InvalidScoreException` before anything reaches the ranking engine —
   one bad input can never corrupt the board." Also show **88.5** rejected —
   whole numbers only.
6. **Scoring rule:** submit **40** for the same participant as the same judge,
   then **90** again. "Each judge holds exactly one score per participant —
   resubmitting replaces it. The displayed score is the average across judges,
   so it always stays between 0 and 100." Log window 2 in as `judge2`, give the
   same participant **60**, and point at the new average on the board.

---

## Part 4 — Code walkthrough: concurrency & the skip list (Speaker A) — 7:30–10:30

*[SCREEN: IDE, `Leaderboard.java`]*

**A:** "This class is the heart of the project. Scores live in a
`ConcurrentHashMap` mapping participant to a map of judge name to score. The
ranking itself lives in a `ConcurrentSkipListMap` keyed by score, name and id,
ordered best-first. When a score arrives, we do not sort anything — we remove
the participant's old key and insert the new one. Both operations are
O(log n), which is the incremental update we promised in Part A. Reading the
standings is a plain walk of the already-ordered map, and `getTopK` reads just
the first k entries in O(k)."

*[Point at `addScore`]* "Note the update runs inside `compute()`, which locks
only this participant's entry. Two judges scoring the same participant apply
one after the other, so no update is lost — but judges scoring different
participants proceed fully in parallel. That is the fine-grained locking
strategy from our proposal, with no global synchronized block anywhere."

*[SCREEN: `ScoreProcessor.java`]* "Submissions are validated on the caller's
thread for instant feedback, then handed to a fixed pool of three worker
threads — an `ExecutorService`. The pool's queue acts as the buffer of a
producer-consumer pipeline."

*[SCREEN: `LeaderboardManager.java`, the `firePropertyChange` line]* "After a
score is applied we fire a `PropertyChangeEvent` — the observer pattern via
`java.beans` — which is how the rest of the app learns that standings changed
without the engine knowing who is listening."

**A:** "Now Shaan takes over for security, administration and integration."

---

## Part 5 — Security & roles (Speaker B) — 10:30–13:00

*[SCREEN: IDE, `SecurityConfig.java`]*

**B:** "Access control is Spring Security with three roles. The filter chain
maps URLs to roles: the leaderboard needs any signed-in user, the judge page
needs the JUDGE or ADMIN role, and the admin page needs ADMIN. Users are held
in memory with BCrypt-hashed passwords."

*[SCREEN: browser]* Demonstrate, quickly:
1. Log in as `participant1 / 1234` → open Leaderboard fine → manually visit
   `/judge` → **403 denied**. "Participants can watch, not score."
2. Log in as `judge1` → `/judge` works → visit `/admin` → **403 denied**.
3. Log in as `admin` → everything opens, and the navigation shows Sign out.

---

## Part 6 — Admin, simulator & Top-K (Speaker B) — 13:00–15:30

*[SCREEN: browser, Admin page as admin]*

1. **Simulator:** "For load demonstration we built a simulator that submits a
   random valid judge score roughly every 0.4 seconds across all competitions —
   this mimics many judges pressing submit during finals." Start it, switch to
   the Leaderboard tab, let ranks visibly churn for ~20 seconds.
2. **Top-K:** flip the Show selector to **Top 5**. "This is served by the O(k)
   read — the server returns the first five entries of the skip list; it never
   sorts or scans the full set."
3. **Export:** back on Admin, click **Export to CSV**, then open
   `data/singing_leaderboard.csv` in the IDE. "Rank, participant, average —
   a file the festival backend can consume directly."
4. **Reset:** reset one competition, confirm the dialog, show zeros on the board.

---

## Part 7 — Integration & persistence (Speaker B) — 15:30–17:00

*[SCREEN: IDE, `CSVManager.java`, then the `data/` folder]*

**B:** "Integration with the festival ecosystem is file-based, one of the modes
the specification allows. Inputs: `participants.csv` and `judges.csv`, which
the event-management module would produce. Outputs: exported standings, plus
`saved_scores.csv` — every judge's individual score, written on shutdown and
reloaded on start, so a restart loses nothing. Malformed rows are skipped
rather than crashing the app."

*[Optional if time allows: stop the app (Ctrl+C), start it again, show the
board came back with the same scores.]*

---

## Part 8 — Tests & wrap-up (both) — 17:00–18:30

*[SCREEN: terminal]* Run `mvn test`.

**B:** "Twenty JUnit tests: ranking order and ties, Top-K, the averaging and
same-judge-replacement rules, invalid and non-integer rejection, CSV loading
and the save-restore round trip — and two concurrency tests that fire hundreds
of submissions from multiple threads and assert that not a single update is
lost and the skip-list index stays consistent."

**A:** "To summarise: the module solves the leaderboard bottleneck with
O(log n) incremental ranking, safe concurrency with per-participant locking,
observer-based updates, role-based security, and file-based integration —
built with Java 21, Spring Boot, and the concurrent collections framework.
Thank you."

---

## Timing summary

| Part | Content | Time |
|---|---|---|
| 1 | Introductions | 1:30 |
| 2 | Architecture | 1:15 |
| 3 | Core demo (login → scores → averaging) | 4:45 |
| 4 | Code: skip list + concurrency | 3:00 |
| 5 | Security & roles | 2:30 |
| 6 | Admin, simulator, Top-K | 2:30 |
| 7 | Integration & persistence | 1:30 |
| 8 | Tests & wrap-up | 1:30 |
| | **Total** | **≈ 18:30** |

If you run long, trim Part 7's restart demo and shorten Part 2.
