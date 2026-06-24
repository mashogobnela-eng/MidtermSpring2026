package uno;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * UNO call and missed-call penalty (rubric 1.9).
 */
class UnoCallTest {

    @Test
    void mustDeclareOnlyAtOneCard() {
        assertTrue(UnoCall.mustDeclare(1), "one card is the UNO state");
        assertFalse(UnoCall.mustDeclare(2));
        assertFalse(UnoCall.mustDeclare(0));
    }

    @Test
    void missedCallDrawsTwoCards() {
        assertEquals(2, UnoCall.penalty(false, 1));
        assertEquals(UnoCall.PENALTY_CARDS, UnoCall.penalty(false, 1));
    }

    @Test
    void declaringUnoAvoidsPenalty() {
        assertEquals(0, UnoCall.penalty(true, 1));
    }

    @Test
    void noPenaltyWhenNotAtOneCard() {
        assertEquals(0, UnoCall.penalty(false, 2));
        assertEquals(0, UnoCall.penalty(false, 7));
    }
}
