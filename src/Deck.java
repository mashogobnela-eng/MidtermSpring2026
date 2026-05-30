import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * The draw and discard piles. Owns deck construction, shuffling, drawing, and
 * the reshuffle-from-discard behavior, so randomness lives behind one boundary
 * instead of being scattered through the turn loop.
 */
public final class Deck {

    private final List<Card> drawPile = new ArrayList<Card>();
    private final List<Card> discardPile = new ArrayList<Card>();
    private final Random random;

    public Deck(Random random) {
        this.random = random;
    }

    /** Build a fresh shuffled 108-card deck and empty the discard pile. */
    public void startNewDeck() {
        drawPile.clear();
        String[] colors = {"R", "Y", "G", "B"};
        for (int c = 0; c < colors.length; c++) {
            drawPile.add(new Card(colors[c] + "0"));
            for (int n = 1; n <= 9; n++) {
                drawPile.add(new Card(colors[c] + n));
                drawPile.add(new Card(colors[c] + n));
            }
            drawPile.add(new Card(colors[c] + "S"));
            drawPile.add(new Card(colors[c] + "S"));
            drawPile.add(new Card(colors[c] + "R"));
            drawPile.add(new Card(colors[c] + "R"));
            drawPile.add(new Card(colors[c] + "+2"));
            drawPile.add(new Card(colors[c] + "+2"));
        }
        for (int i = 0; i < 4; i++) {
            drawPile.add(new Card("W"));
            drawPile.add(new Card("W4"));
        }
        Collections.shuffle(drawPile, random);
        discardPile.clear();
    }

    /** Draw the top card, reshuffling the discard pile in when the deck runs out. */
    public Card draw() {
        if (drawPile.isEmpty()) {
            drawPile.addAll(discardPile);
            discardPile.clear();
            Collections.shuffle(drawPile, random);
        }
        if (drawPile.isEmpty()) {
            return new Card("W");
        }
        return drawPile.remove(0);
    }

    public void discard(Card card) {
        discardPile.add(card);
    }
}
