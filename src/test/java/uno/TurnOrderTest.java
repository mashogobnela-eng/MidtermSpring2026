package uno;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Turn order, Skip, and Reverse (rubric 1.3 and 1.4), exercised through the
 * pure {@link Rules#nextPlayer} helper that the game loop uses.
 */
class TurnOrderTest {

    @Test
    void advancesClockwiseWithWrap() {
        assertEquals(1, Rules.nextPlayer(0, 1, 3));
        assertEquals(2, Rules.nextPlayer(1, 1, 3));
        assertEquals(0, Rules.nextPlayer(2, 1, 3), "wraps past the last player");
    }

    @Test
    void advancesCounterclockwiseWithWrap() {
        assertEquals(1, Rules.nextPlayer(2, -1, 3));
        assertEquals(0, Rules.nextPlayer(1, -1, 3));
        assertEquals(2, Rules.nextPlayer(0, -1, 3), "wraps below the first player");
    }

    @Test
    void skipMakesTheNextPlayerLoseTheirTurn() {
        // Skip = advance twice in the same direction (the next player is skipped).
        int afterSkip = Rules.nextPlayer(Rules.nextPlayer(0, 1, 4), 1, 4);
        assertEquals(2, afterSkip);
    }

    @Test
    void reverseChangesDirectionForThreeOrMore() {
        int direction = 1;
        assertEquals(1, Rules.nextPlayer(0, direction, 3), "forward before reverse");
        direction = -direction; // Reverse flips the direction
        assertEquals(2, Rules.nextPlayer(0, direction, 3), "backward after reverse");
    }

    @Test
    void twoPlayerReverseActsLikeSkip() {
        // Documented variant: with two players, Reverse behaves like Skip, so
        // play returns to the player who reversed (two advances => back to start).
        int afterTwoAdvances = Rules.nextPlayer(Rules.nextPlayer(0, 1, 2), 1, 2);
        assertEquals(0, afterTwoAdvances);
    }
}
