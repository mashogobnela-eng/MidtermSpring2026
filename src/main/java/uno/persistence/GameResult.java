package uno.persistence;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * A plain, persistence-agnostic description of one finished game, handed from
 * the game layer to {@link GameRepository}. Carrying a DTO (rather than JPA
 * entities) keeps the game/CLI code free of any ORM details.
 */
public final class GameResult {

    /** Outcome of one round: who won it and how many points it was worth. */
    public static final class RoundResult {
        private final int roundNumber;
        private final String winner;
        private final int points;

        public RoundResult(int roundNumber, String winner, int points) {
            this.roundNumber = roundNumber;
            this.winner = winner;
            this.points = points;
        }

        public int getRoundNumber() {
            return roundNumber;
        }

        public String getWinner() {
            return winner;
        }

        public int getPoints() {
            return points;
        }
    }

    /** A player's final score in the game. */
    public static final class PlayerScore {
        private final String player;
        private final int points;

        public PlayerScore(String player, int points) {
            this.player = player;
            this.points = points;
        }

        public String getPlayer() {
            return player;
        }

        public int getPoints() {
            return points;
        }
    }

    private final Instant playedAt;
    private final List<String> players;
    private final List<RoundResult> rounds;
    private final List<PlayerScore> finalScores;
    private final String winner;

    public GameResult(Instant playedAt, List<String> players, List<RoundResult> rounds,
                      List<PlayerScore> finalScores, String winner) {
        this.playedAt = playedAt;
        this.players = new ArrayList<>(players);
        this.rounds = new ArrayList<>(rounds);
        this.finalScores = new ArrayList<>(finalScores);
        this.winner = winner;
    }

    public Instant getPlayedAt() {
        return playedAt;
    }

    public List<String> getPlayers() {
        return players;
    }

    public List<RoundResult> getRounds() {
        return rounds;
    }

    public List<PlayerScore> getFinalScores() {
        return finalScores;
    }

    public String getWinner() {
        return winner;
    }
}
