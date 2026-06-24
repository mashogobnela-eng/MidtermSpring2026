package uno.persistence;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * One completed game session: when it was played, how many rounds it ran, the
 * overall winner, and the per-player final scores and per-round results.
 */
@Entity
@Table(name = "games")
public class GameEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "played_at", nullable = false)
    private Instant playedAt;

    @Column(name = "rounds_played", nullable = false)
    private int roundsPlayed;

    @ManyToOne
    @JoinColumn(name = "winner_id")
    private PlayerEntity winner;

    @OneToMany(mappedBy = "game", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RoundEntity> rounds = new ArrayList<>();

    @OneToMany(mappedBy = "game", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ScoreEntity> scores = new ArrayList<>();

    protected GameEntity() {
        // required by JPA
    }

    public GameEntity(Instant playedAt, int roundsPlayed, PlayerEntity winner) {
        this.playedAt = playedAt;
        this.roundsPlayed = roundsPlayed;
        this.winner = winner;
    }

    public void addRound(RoundEntity round) {
        round.setGame(this);
        rounds.add(round);
    }

    public void addScore(ScoreEntity score) {
        score.setGame(this);
        scores.add(score);
    }

    public Long getId() {
        return id;
    }

    public Instant getPlayedAt() {
        return playedAt;
    }

    public int getRoundsPlayed() {
        return roundsPlayed;
    }

    public PlayerEntity getWinner() {
        return winner;
    }

    public List<RoundEntity> getRounds() {
        return rounds;
    }

    public List<ScoreEntity> getScores() {
        return scores;
    }
}
