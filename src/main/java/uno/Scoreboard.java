package uno;

/**
 * Multi-round match scoring helpers: deciding when a match to a target score is
 * over and who the final winner is. Pure functions over a scores array, with no
 * console or game-state coupling, so the match-end logic can be unit tested.
 *
 * <p>A standard UNO match commonly runs to {@value #DEFAULT_TARGET} points.
 */
public final class Scoreboard {

    /** Common target score for a full match. */
    public static final int DEFAULT_TARGET = 500;

    private Scoreboard() {
    }

    /** True once any of the first {@code count} players has reached the target. */
    public static boolean reachedTarget(int[] scores, int count, int target) {
        for (int i = 0; i < count; i++) {
            if (scores[i] >= target) {
                return true;
            }
        }
        return false;
    }

    /**
     * Index of the leading player among the first {@code count}. Ties resolve to
     * the lowest index. Returns -1 when {@code count <= 0}.
     */
    public static int leader(int[] scores, int count) {
        int best = -1;
        int bestScore = Integer.MIN_VALUE;
        for (int i = 0; i < count; i++) {
            if (scores[i] > bestScore) {
                bestScore = scores[i];
                best = i;
            }
        }
        return best;
    }
}
