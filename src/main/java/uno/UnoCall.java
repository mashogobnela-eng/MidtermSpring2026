package uno;

/**
 * The "UNO" call rule and its missed-call penalty, kept as pure logic with no
 * game state or console coupling so it can be unit tested directly.
 *
 * <p>A player must declare "UNO" at the moment they are reduced to a single
 * card. If they fail to declare before the next relevant action, they draw a
 * penalty of {@value #PENALTY_CARDS} cards.
 */
public final class UnoCall {

    /** Cards drawn for a missed UNO call. */
    public static final int PENALTY_CARDS = 2;

    private UnoCall() {
    }

    /** True when a hand of this size (after playing) is the one-card UNO state. */
    public static boolean mustDeclare(int handSizeAfterPlay) {
        return handSizeAfterPlay == 1;
    }

    /**
     * Number of penalty cards to draw: {@value #PENALTY_CARDS} when the player
     * is at the one-card state and did not declare UNO, otherwise zero.
     */
    public static int penalty(boolean declared, int handSizeAfterPlay) {
        if (mustDeclare(handSizeAfterPlay) && !declared) {
            return PENALTY_CARDS;
        }
        return 0;
    }
}
