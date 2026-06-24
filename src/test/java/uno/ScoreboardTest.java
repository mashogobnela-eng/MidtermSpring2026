package uno;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Round scoring and the multi-round target match (rubric 1.10).
 */
class ScoreboardTest {

    @Test
    void defaultTargetIs500() {
        assertEquals(500, Scoreboard.DEFAULT_TARGET);
    }

    @Test
    void targetNotReachedBelowThreshold() {
        assertFalse(Scoreboard.reachedTarget(new int[]{100, 200, 499}, 3, 500));
    }

    @Test
    void targetReachedAtOrAboveThreshold() {
        assertTrue(Scoreboard.reachedTarget(new int[]{100, 500, 0}, 3, 500), "exactly the target");
        assertTrue(Scoreboard.reachedTarget(new int[]{0, 0, 512}, 3, 500), "above the target");
    }

    @Test
    void finalWinnerIsHighestScore() {
        assertEquals(2, Scoreboard.leader(new int[]{10, 20, 99, 5}, 4));
    }

    @Test
    void tieResolvesToLowestIndex() {
        assertEquals(0, Scoreboard.leader(new int[]{50, 50, 10}, 3));
    }

    @Test
    void onlyTheFirstCountPlayersAreConsidered() {
        // A trailing array slot beyond `count` must be ignored.
        assertFalse(Scoreboard.reachedTarget(new int[]{10, 20, 999}, 2, 500));
        assertEquals(1, Scoreboard.leader(new int[]{10, 20, 999}, 2));
    }
}
