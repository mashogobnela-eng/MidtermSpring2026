package uno;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

/**
 * Deck composition (rubric 1.1): the standard 108-card UNO deck.
 */
class DeckCompositionTest {

    private static final String[] COLORS = {"R", "Y", "G", "B"};

    private Map<String, Integer> counts() {
        Map<String, Integer> m = new HashMap<>();
        for (Card c : Deck.buildStandardDeck()) {
            m.merge(c.code(), 1, Integer::sum);
        }
        return m;
    }

    @Test
    void deckHas108Cards() {
        assertEquals(108, Deck.buildStandardDeck().size());
    }

    @Test
    void eachColorHas25Cards() {
        List<Card> deck = Deck.buildStandardDeck();
        for (String color : COLORS) {
            long n = deck.stream().filter(c -> c.color().equals(color)).count();
            assertEquals(25, n, "color " + color + " card count");
        }
    }

    @Test
    void oneZeroAndTwoOfEachNumberPerColor() {
        Map<String, Integer> m = counts();
        for (String color : COLORS) {
            assertEquals(1, m.get(color + "0"), color + " zero count");
            for (int n = 1; n <= 9; n++) {
                assertEquals(2, m.get(color + n), color + n + " count");
            }
        }
    }

    @Test
    void twoOfEachActionCardPerColor() {
        Map<String, Integer> m = counts();
        for (String color : COLORS) {
            assertEquals(2, m.get(color + "S"), color + " skip count");
            assertEquals(2, m.get(color + "R"), color + " reverse count");
            assertEquals(2, m.get(color + "+2"), color + " draw-two count");
        }
    }

    @Test
    void fourWildAndFourWildDrawFour() {
        Map<String, Integer> m = counts();
        assertEquals(4, m.get("W"), "wild count");
        assertEquals(4, m.get("W4"), "wild draw four count");
    }
}
