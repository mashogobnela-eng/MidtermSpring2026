package uno;

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

    /**
     * Build a fresh, unshuffled standard 108-card UNO deck: four colors, one 0
     * and two each of 1-9 per color, two each of Skip/Reverse/Draw-Two per
     * color, and four Wild plus four Wild Draw Four. Exposed (and side-effect
     * free) so deck composition can be unit tested directly.
     */
    public static List<Card> buildStandardDeck() {
        List<Card> cards = new ArrayList<Card>();
        String[] colors = {"R", "Y", "G", "B"};
        for (int c = 0; c < colors.length; c++) {
            cards.add(new Card(colors[c] + "0"));
            for (int n = 1; n <= 9; n++) {
                cards.add(new Card(colors[c] + n));
                cards.add(new Card(colors[c] + n));
            }
            cards.add(new Card(colors[c] + "S"));
            cards.add(new Card(colors[c] + "S"));
            cards.add(new Card(colors[c] + "R"));
            cards.add(new Card(colors[c] + "R"));
            cards.add(new Card(colors[c] + "+2"));
            cards.add(new Card(colors[c] + "+2"));
        }
        for (int i = 0; i < 4; i++) {
            cards.add(new Card("W"));
            cards.add(new Card("W4"));
        }
        return cards;
    }

    /** Build a fresh shuffled 108-card deck and empty the discard pile. */
    public void startNewDeck() {
        drawPile.clear();
        drawPile.addAll(buildStandardDeck());
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
