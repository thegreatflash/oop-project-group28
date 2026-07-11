# How to Run

Instructions to execute the Competition Leaderboard System (Group 28, CS F213).

## Requirements

- JDK 21 or newer (`java -version` to check)
- Maven is **not** required — the included Maven wrapper (`mvnw`) downloads it
  automatically on first use

## Run locally

```
git clone https://github.com/thegreatflash/oop-project-group28
cd oop-project-group28
./mvnw spring-boot:run
```

Wait for `Started LeaderboardApplication`, then open **http://localhost:8080**
in a browser. Run the command from the project root so the `data/` folder is
found. Stop the server with Ctrl+C (scores are saved on shutdown and restored
on the next start).

On Windows, use `mvnw.cmd spring-boot:run` instead of `./mvnw`.

## Sign in

| Username | Password | Role | Can open |
|---|---|---|---|
| `participant1` … `participant3` | `1234` | Participant | Home, Leaderboard |
| `judge1` … `judge3` | `1234` | Judge | + Judge page (submit scores) |
| `admin` | `1234` | Admin | + Admin page (reset, export, simulator) |

## Run in the cloud (no local setup)

1. On the GitHub repository page click **Code → Codespaces → Create codespace
   on main** (Java 21 is provisioned automatically).
2. In the Codespace terminal run `./mvnw spring-boot:run`.
3. Click the popup for forwarded port **8080** to open the app in a browser tab.

## Run the unit tests

```
./mvnw test
```

Expected result: `Tests run: 20, Failures: 0, Errors: 0` and `BUILD SUCCESS`.

## Suggested demo flow

1. Sign in as `admin` → **Admin** tab → **Start Simulator** (submits a random
   judge score about every 0.4 s across all competitions).
2. Open the **Leaderboard** tab and watch the rankings reorder live; the table
   refreshes automatically every 3 seconds.
3. Switch the **Show** selector to *Top 5* to see the O(k) Top-K view.
4. Sign in as `judge1` in another window and submit a score manually — try an
   invalid one (e.g. 150) to see validation reject it.
5. On the Admin tab, **Export to CSV** writes the standings to
   `data/<competition>_leaderboard.csv`.
