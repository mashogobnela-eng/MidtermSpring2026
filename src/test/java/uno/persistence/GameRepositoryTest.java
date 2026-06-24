package uno.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Persistence-layer tests for {@link GameRepository}.
 *
 * Each test runs against its own isolated in-memory H2 database (a unique
 * database name plus {@code create-drop} schema), so the suite never touches
 * the developer's file database or any machine-specific state and tests cannot
 * interfere with one another.
 */
class GameRepositoryTest {

    private static int counter = 0;

    private Database db;
    private GameRepository repo;

    @BeforeEach
    void setUp() {
        counter++;
        Map<String, Object> overrides = new HashMap<>();
        overrides.put("jakarta.persistence.jdbc.url",
                "jdbc:h2:mem:unotest" + counter + ";DB_CLOSE_DELAY=-1");
        overrides.put("hibernate.hbm2ddl.auto", "create-drop");
        db = Database.open(overrides);
        repo = new GameRepository(db);
    }

    @AfterEach
    void tearDown() {
        db.close();
    }

    /** Two-player game where {@code winner} takes one round worth max(a,b) points. */
    private GameResult sampleGame(Instant when, String winner, int alicePoints, int bobPoints) {
        List<String> players = List.of("Alice", "Bob");
        List<GameResult.RoundResult> rounds = List.of(
                new GameResult.RoundResult(1, winner, Math.max(alicePoints, bobPoints)));
        List<GameResult.PlayerScore> scores = List.of(
                new GameResult.PlayerScore("Alice", alicePoints),
                new GameResult.PlayerScore("Bob", bobPoints));
        return new GameResult(when, players, rounds, scores, winner);
    }

    @Test
    void saveReturnsIdAndPersistsGame() {
        Long id = repo.save(sampleGame(Instant.now(), "Alice", 40, 0));
        assertNotNull(id, "save should return a generated id");
        assertEquals(1, repo.recentGames(10).size());
    }

    @Test
    void savePersistsRoundsScoresAndWinner() {
        repo.save(sampleGame(Instant.now(), "Alice", 40, 0));
        GameRepository.RecentGame game = repo.recentGames(10).get(0);
        assertEquals("Alice", game.getWinner());
        assertEquals(1, game.getRoundsPlayed());
        // Score summary is "name=points", sorted by points desc.
        assertEquals("Alice=40, Bob=0", game.getScoreSummary());
    }

    @Test
    void recentGamesAreNewestFirst() {
        repo.save(sampleGame(Instant.parse("2026-01-01T10:00:00Z"), "Alice", 30, 0));
        repo.save(sampleGame(Instant.parse("2026-02-01T10:00:00Z"), "Bob", 0, 50));
        List<GameRepository.RecentGame> recent = repo.recentGames(10);
        assertEquals(2, recent.size());
        assertEquals("Bob", recent.get(0).getWinner(), "most recent first");
        assertEquals("Alice", recent.get(1).getWinner());
    }

    @Test
    void recentGamesRespectLimit() {
        for (int i = 0; i < 5; i++) {
            repo.save(sampleGame(Instant.now(), "Alice", 10 + i, 0));
        }
        assertEquals(3, repo.recentGames(3).size());
    }

    @Test
    void playerWinCountsAggregateAcrossGames() {
        repo.save(sampleGame(Instant.now(), "Alice", 40, 0));
        repo.save(sampleGame(Instant.now(), "Alice", 35, 0));
        repo.save(sampleGame(Instant.now(), "Bob", 0, 22));
        List<GameRepository.PlayerWins> wins = repo.playerWinCounts();
        assertEquals("Alice", wins.get(0).getPlayer(), "most wins first");
        assertEquals(2L, wins.get(0).getWins());
        assertTrue(wins.stream().anyMatch(w -> w.getPlayer().equals("Bob") && w.getWins() == 1L));
    }

    @Test
    void highestScoresAreDescending() {
        repo.save(sampleGame(Instant.now(), "Alice", 40, 10));
        repo.save(sampleGame(Instant.now(), "Bob", 5, 99));
        List<GameRepository.HighScore> high = repo.highestScores(3);
        assertEquals(99, high.get(0).getPoints());
        assertEquals("Bob", high.get(0).getPlayer());
        assertTrue(high.get(0).getPoints() >= high.get(1).getPoints(), "scores sorted descending");
    }

    @Test
    void playersAreReusedNotDuplicated() {
        repo.save(sampleGame(Instant.now(), "Alice", 40, 0));
        repo.save(sampleGame(Instant.now(), "Bob", 0, 30));
        // Alice plays in both games but must be a single player row, so she
        // appears at most once in any per-player aggregate.
        long aliceWinRows = repo.playerWinCounts().stream()
                .filter(w -> w.getPlayer().equals("Alice")).count();
        assertEquals(1, aliceWinRows);
    }

    @Test
    void emptyDatabaseReportsAreEmpty() {
        assertTrue(repo.recentGames(10).isEmpty());
        assertTrue(repo.playerWinCounts().isEmpty());
        assertTrue(repo.highestScores(10).isEmpty());
    }
}
