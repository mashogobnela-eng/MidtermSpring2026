package uno.persistence;

import jakarta.persistence.EntityManager;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Data-access object for game history. All database access goes through here
 * using JPQL; the game and CLI code never sees the EntityManager or any SQL.
 *
 * <p>Provides one write path ({@link #save}) and the three required report
 * queries: {@link #recentGames}, {@link #playerWinCounts}, {@link #highestScores}.
 */
public class GameRepository {

    private final Database db;

    public GameRepository(Database db) {
        this.db = db;
    }

    /**
     * Persist one finished game (players, game, rounds, scores, winner,
     * timestamp) in a single transaction. Returns the new game id.
     */
    public Long save(GameResult result) {
        EntityManager em = db.em();
        try {
            em.getTransaction().begin();

            PlayerEntity overallWinner =
                    result.getWinner() == null ? null : findOrCreatePlayer(em, result.getWinner());

            GameEntity game = new GameEntity(result.getPlayedAt(), result.getRounds().size(), overallWinner);

            for (GameResult.RoundResult r : result.getRounds()) {
                PlayerEntity roundWinner =
                        r.getWinner() == null ? null : findOrCreatePlayer(em, r.getWinner());
                game.addRound(new RoundEntity(r.getRoundNumber(), roundWinner, r.getPoints()));
            }

            for (GameResult.PlayerScore ps : result.getFinalScores()) {
                PlayerEntity player = findOrCreatePlayer(em, ps.getPlayer());
                game.addScore(new ScoreEntity(player, ps.getPoints()));
            }

            em.persist(game);
            em.getTransaction().commit();
            return game.getId();
        } catch (RuntimeException ex) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw ex;
        } finally {
            em.close();
        }
    }

    private PlayerEntity findOrCreatePlayer(EntityManager em, String name) {
        List<PlayerEntity> found = em.createQuery(
                        "select p from PlayerEntity p where p.name = :name", PlayerEntity.class)
                .setParameter("name", name)
                .getResultList();
        if (!found.isEmpty()) {
            return found.get(0);
        }
        PlayerEntity created = new PlayerEntity(name);
        em.persist(created);
        return created;
    }

    /** Report query 1: the most recent games, newest first. */
    public List<RecentGame> recentGames(int limit) {
        EntityManager em = db.em();
        try {
            List<GameEntity> games = em.createQuery(
                            "select g from GameEntity g order by g.playedAt desc, g.id desc", GameEntity.class)
                    .setMaxResults(limit)
                    .getResultList();

            List<RecentGame> out = new ArrayList<>();
            for (GameEntity g : games) {
                String winner = g.getWinner() == null ? "-" : g.getWinner().getName();
                // Build a compact "name=points" summary while the EM is still open.
                List<ScoreEntity> scores = new ArrayList<>(g.getScores());
                scores.sort((a, b) -> Integer.compare(b.getPoints(), a.getPoints()));
                StringBuilder summary = new StringBuilder();
                for (int i = 0; i < scores.size(); i++) {
                    if (i > 0) {
                        summary.append(", ");
                    }
                    summary.append(scores.get(i).getPlayer().getName())
                            .append("=").append(scores.get(i).getPoints());
                }
                out.add(new RecentGame(g.getId(), g.getPlayedAt(), winner,
                        g.getRoundsPlayed(), summary.toString()));
            }
            return out;
        } finally {
            em.close();
        }
    }

    /** Report query 2: how many games each player has won, most wins first. */
    public List<PlayerWins> playerWinCounts() {
        EntityManager em = db.em();
        try {
            List<Object[]> rows = em.createQuery(
                            "select g.winner.name, count(g) from GameEntity g "
                                    + "where g.winner is not null "
                                    + "group by g.winner.name "
                                    + "order by count(g) desc, g.winner.name asc", Object[].class)
                    .getResultList();
            List<PlayerWins> out = new ArrayList<>();
            for (Object[] row : rows) {
                out.add(new PlayerWins((String) row[0], (Long) row[1]));
            }
            return out;
        } finally {
            em.close();
        }
    }

    /** Report query 3: the highest single-game scores, biggest first. */
    public List<HighScore> highestScores(int limit) {
        EntityManager em = db.em();
        try {
            List<Object[]> rows = em.createQuery(
                            "select s.player.name, s.points, s.game.id, s.game.playedAt "
                                    + "from ScoreEntity s "
                                    + "order by s.points desc, s.game.id asc", Object[].class)
                    .setMaxResults(limit)
                    .getResultList();
            List<HighScore> out = new ArrayList<>();
            for (Object[] row : rows) {
                out.add(new HighScore((String) row[0], (Integer) row[1],
                        (Long) row[2], (Instant) row[3]));
            }
            return out;
        } finally {
            em.close();
        }
    }

    // --- report result DTOs ---------------------------------------------------

    public static final class RecentGame {
        private final Long id;
        private final Instant playedAt;
        private final String winner;
        private final int roundsPlayed;
        private final String scoreSummary;

        public RecentGame(Long id, Instant playedAt, String winner, int roundsPlayed, String scoreSummary) {
            this.id = id;
            this.playedAt = playedAt;
            this.winner = winner;
            this.roundsPlayed = roundsPlayed;
            this.scoreSummary = scoreSummary;
        }

        public Long getId() {
            return id;
        }

        public Instant getPlayedAt() {
            return playedAt;
        }

        public String getWinner() {
            return winner;
        }

        public int getRoundsPlayed() {
            return roundsPlayed;
        }

        public String getScoreSummary() {
            return scoreSummary;
        }
    }

    public static final class PlayerWins {
        private final String player;
        private final long wins;

        public PlayerWins(String player, long wins) {
            this.player = player;
            this.wins = wins;
        }

        public String getPlayer() {
            return player;
        }

        public long getWins() {
            return wins;
        }
    }

    public static final class HighScore {
        private final String player;
        private final int points;
        private final Long gameId;
        private final Instant playedAt;

        public HighScore(String player, int points, Long gameId, Instant playedAt) {
            this.player = player;
            this.points = points;
            this.gameId = gameId;
            this.playedAt = playedAt;
        }

        public String getPlayer() {
            return player;
        }

        public int getPoints() {
            return points;
        }

        public Long getGameId() {
            return gameId;
        }

        public Instant getPlayedAt() {
            return playedAt;
        }
    }
}
