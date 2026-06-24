package uno;

import java.util.List;

/**
 * The legal-play and scoring rules of this UNO variant, with no console or
 * game-state coupling. This is the single home for the "is this card playable"
 * decision that the original code duplicated across the turn loop, the bot, the
 * human prompt, and a standalone helper.
 */
public final class Rules {

    private Rules() {
    }

    /**
     * Whether {@code card} may be played on {@code up} given the color called
     * after the last wild ({@code calledColor} is "" when none is in effect).
     */
    public static boolean isLegal(Card card, Card up, String calledColor) {
        if (card.isWild()) {
            return true;
        }
        if (card.color().equals(up.color())) {
            return true;
        }
        if (!calledColor.equals("") && card.color().equals(calledColor)) {
            return true;
        }
        if (card.rank() == up.rank() && card.rank() != Rank.NUMBER) {
            return true;
        }
        if (card.rank() == Rank.NUMBER && up.rank() == Rank.NUMBER && card.number() == up.number()) {
            return true;
        }
        return false;
    }

    /** Total points held in a hand, used to score the winner against losers. */
    public static int handPoints(List<Card> hand) {
        int total = 0;
        for (Card card : hand) {
            total += card.points();
        }
        return total;
    }

    /**
     * Index of the next player in turn order, wrapping around the table.
     * {@code direction} is +1 for clockwise and -1 for counterclockwise. Skip is
     * two advances in the same direction; Reverse flips the direction first.
     */
    public static int nextPlayer(int current, int direction, int playerCount) {
        int next = current + direction;
        if (next >= playerCount) {
            next = 0;
        }
        if (next < 0) {
            next = playerCount - 1;
        }
        return next;
    }
}
