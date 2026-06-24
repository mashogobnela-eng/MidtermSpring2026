package uno.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * One round within a game: its number, who emptied their hand to win it, and
 * how many points that round was worth.
 */
@Entity
@Table(name = "rounds")
public class RoundEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "game_id", nullable = false)
    private GameEntity game;

    @Column(name = "round_number", nullable = false)
    private int roundNumber;

    @ManyToOne
    @JoinColumn(name = "winner_id")
    private PlayerEntity winner;

    @Column(nullable = false)
    private int points;

    protected RoundEntity() {
        // required by JPA
    }

    public RoundEntity(int roundNumber, PlayerEntity winner, int points) {
        this.roundNumber = roundNumber;
        this.winner = winner;
        this.points = points;
    }

    void setGame(GameEntity game) {
        this.game = game;
    }

    public Long getId() {
        return id;
    }

    public GameEntity getGame() {
        return game;
    }

    public int getRoundNumber() {
        return roundNumber;
    }

    public PlayerEntity getWinner() {
        return winner;
    }

    public int getPoints() {
        return points;
    }
}
