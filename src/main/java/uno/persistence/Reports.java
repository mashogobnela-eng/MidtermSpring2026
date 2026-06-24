package uno.persistence;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Renders the report queries as readable text for the CLI report mode. Keeps
 * formatting out of {@link GameRepository} so the DAO stays presentation-free.
 */
public class Reports {

    private static final DateTimeFormatter TS =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

    private final GameRepository repo;

    public Reports(GameRepository repo) {
        this.repo = repo;
    }

    public void printAll(int limit) {
        printRecentGames(limit);
        System.out.println();
        printWinCounts();
        System.out.println();
        printHighScores(limit);
    }

    public void printRecentGames(int limit) {
        List<GameRepository.RecentGame> games = repo.recentGames(limit);
        System.out.println("Recent games:");
        if (games.isEmpty()) {
            System.out.println("  (none yet - play with --save to record a game)");
            return;
        }
        for (GameRepository.RecentGame g : games) {
            System.out.println("  #" + g.getId() + "  " + TS.format(g.getPlayedAt())
                    + "  winner=" + g.getWinner()
                    + "  rounds=" + g.getRoundsPlayed()
                    + "  [" + g.getScoreSummary() + "]");
        }
    }

    public void printWinCounts() {
        List<GameRepository.PlayerWins> wins = repo.playerWinCounts();
        System.out.println("Player win counts:");
        if (wins.isEmpty()) {
            System.out.println("  (none yet)");
            return;
        }
        for (GameRepository.PlayerWins w : wins) {
            System.out.println("  " + w.getPlayer() + ": " + w.getWins());
        }
    }

    public void printHighScores(int limit) {
        List<GameRepository.HighScore> scores = repo.highestScores(limit);
        System.out.println("Highest scores:");
        if (scores.isEmpty()) {
            System.out.println("  (none yet)");
            return;
        }
        for (GameRepository.HighScore s : scores) {
            System.out.println("  " + s.getPoints() + "  " + s.getPlayer()
                    + "  (game #" + s.getGameId() + ", " + TS.format(s.getPlayedAt()) + ")");
        }
    }
}
