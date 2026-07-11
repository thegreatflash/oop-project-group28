package com.group28.leaderboard.web;

import com.group28.leaderboard.core.InvalidScoreException;
import com.group28.leaderboard.core.Ranking;
import com.group28.leaderboard.model.Competition;
import com.group28.leaderboard.model.Participant;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.util.List;

/**
 * Handles all the pages. Thymeleaf renders the HTML; the one @ResponseBody
 * method feeds JSON to the leaderboard page's auto-refresh script.
 * Access rules (who can see which page) live in SecurityConfig.
 */
@Controller
public class PageController {

    private final LeaderboardService service;

    public PageController(LeaderboardService service) {
        this.service = service;
    }

    @GetMapping("/login")
    public String login() { return "login"; }

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("competitions", service.getCompetitions());
        model.addAttribute("judgeCount", service.getJudges().size());
        return "home";
    }

    @GetMapping("/leaderboard")
    public String leaderboard(@RequestParam(required = false) String competition, Model model) {
        String competitionId = resolveCompetition(competition);
        model.addAttribute("competitions", service.getCompetitions());
        model.addAttribute("selected", service.getCompetition(competitionId));
        model.addAttribute("rankings", service.getRankings(competitionId));
        model.addAttribute("lastUpdated", service.getLastUpdated(competitionId));
        return "leaderboard";
    }

    /**
     * JSON used by leaderboard.js to refresh the table every few seconds.
     * Pass top=k (e.g. /api/leaderboard?competition=singing&top=5) to get
     * just the Top-K, served in O(k) from the skip-list index.
     */
    @GetMapping("/api/leaderboard")
    @ResponseBody
    public LeaderboardData leaderboardData(@RequestParam String competition,
                                           @RequestParam(required = false) Integer top) {
        List<Ranking<Participant>> rankings = (top != null && top > 0)
                ? service.getTopRankings(competition, top)
                : service.getRankings(competition);
        List<RankRow> rows = rankings.stream()
                .map(PageController::toRow)
                .toList();
        return new LeaderboardData(service.getLastUpdated(competition), rows);
    }

    @GetMapping("/judge")
    public String judge(@RequestParam(required = false) String competition, Model model) {
        String competitionId = resolveCompetition(competition);
        model.addAttribute("judges", service.getJudges());
        model.addAttribute("competitions", service.getCompetitions());
        model.addAttribute("selected", service.getCompetition(competitionId));
        model.addAttribute("participants", service.getCompetition(competitionId).getParticipants());
        return "judge";
    }

    @PostMapping("/judge")
    public String submitScore(@RequestParam String competition,
                              @RequestParam String participant,
                              @RequestParam String judge,
                              @RequestParam String score,
                              RedirectAttributes redirect) {
        double value;
        try {
            value = Double.parseDouble(score.trim());
        } catch (NumberFormatException e) {
            redirect.addFlashAttribute("error", "Score must be a number.");
            return redirectToJudge(competition);
        }

        try {
            service.submitScore(competition, participant, judge, value);
            redirect.addFlashAttribute("message", value + " points submitted.");
        } catch (InvalidScoreException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return redirectToJudge(competition);
    }

    @GetMapping("/admin")
    public String admin(Model model) {
        model.addAttribute("competitions", service.getCompetitions());
        model.addAttribute("simulatorRunning", service.isSimulatorRunning());
        return "admin";
    }

    @PostMapping("/admin/reset")
    public String reset(@RequestParam String competition, RedirectAttributes redirect) {
        service.reset(competition);
        redirect.addFlashAttribute("message", "Leaderboard reset.");
        return "redirect:/admin";
    }

    @PostMapping("/admin/export")
    public String export(@RequestParam String competition, RedirectAttributes redirect) {
        try {
            redirect.addFlashAttribute("message", "Exported to " + service.export(competition));
        } catch (IOException e) {
            redirect.addFlashAttribute("error", "Export failed: " + e.getMessage());
        }
        return "redirect:/admin";
    }

    @PostMapping("/admin/simulator")
    public String simulator(RedirectAttributes redirect) {
        boolean running = service.toggleSimulator();
        redirect.addFlashAttribute("message", running ? "Simulator started." : "Simulator stopped.");
        return "redirect:/admin";
    }

    /** Falls back to the first competition when none is given in the URL. */
    private String resolveCompetition(String competitionId) {
        if (competitionId != null && service.getCompetition(competitionId) != null) {
            return competitionId;
        }
        return service.getCompetitions().get(0).getId();
    }

    private String redirectToJudge(String competitionId) {
        return "redirect:/judge?competition=" + competitionId;
    }

    private static RankRow toRow(Ranking<Participant> ranking) {
        return new RankRow(ranking.rank(), ranking.entrant().getName(), ranking.score());
    }

    public record RankRow(int rank, String name, double score) {}
    public record LeaderboardData(String lastUpdated, List<RankRow> rows) {}
}
