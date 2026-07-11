// Refreshes the leaderboard table every 3 seconds by polling the JSON
// endpoint, so the page updates without the judge reloading it.

const table = document.getElementById("board");
const competitionId = table.dataset.competition;
const tbody = table.querySelector("tbody");
const updatedLabel = document.getElementById("updated");
const topSelect = document.getElementById("topSelect");

// switching between All / Top 5 / Top 10 refreshes right away
topSelect.addEventListener("change", refresh);

async function refresh() {
    try {
        let url = "/api/leaderboard?competition=" + competitionId;
        if (topSelect.value) {
            url += "&top=" + topSelect.value; // O(k) top-k read on the server
        }
        const response = await fetch(url);
        const data = await response.json();

        tbody.innerHTML = "";
        for (const row of data.rows) {
            const tr = document.createElement("tr");
            tr.innerHTML = "<td>" + row.rank + "</td>"
                + "<td>" + row.name + "</td>"
                + "<td>" + Math.trunc(row.score) + "</td>";
            tbody.appendChild(tr);
        }
        updatedLabel.textContent = data.lastUpdated;
    } catch (e) {
        // if a request fails we just try again on the next tick
    }
}

setInterval(refresh, 3000);
refresh();
