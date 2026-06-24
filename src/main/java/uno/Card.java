package uno;

/**
 * An immutable UNO card.
 *
 * Cards are still identified by their compact code ("R5", "G+2", "W4", ...).
 * This value object centralizes the color/rank/number/points parsing that used
 * to be scattered as primitive String handling, while {@link #toString()}
 * preserves the exact code so console output is unchanged.
 */
public final class Card {

    private final String code;

    public Card(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }

    /** Color letter: "R", "Y", "G", "B", or "" for wild cards. */
    public String color() {
        if (code.startsWith("R")) {
            return "R";
        }
        if (code.startsWith("Y")) {
            return "Y";
        }
        if (code.startsWith("G")) {
            return "G";
        }
        if (code.startsWith("B")) {
            return "B";
        }
        return "";
    }

    public Rank rank() {
        if (code.equals("W")) {
            return Rank.WILD;
        }
        if (code.equals("W4")) {
            return Rank.WILD_DRAW_FOUR;
        }
        if (code.endsWith("S")) {
            return Rank.SKIP;
        }
        if (code.endsWith("R")) {
            return Rank.REVERSE;
        }
        if (code.endsWith("+2")) {
            return Rank.DRAW_TWO;
        }
        return Rank.NUMBER;
    }

    /** True for both wild ("W") and wild draw four ("W4"). */
    public boolean isWild() {
        return code.startsWith("W");
    }

    /** Numeric value for number cards, or -1 for action/wild cards. */
    public int number() {
        if (rank() == Rank.NUMBER) {
            return Integer.parseInt(code.substring(1));
        }
        return -1;
    }

    /** Scoring value held against a losing player. */
    public int points() {
        Rank r = rank();
        if (r == Rank.NUMBER) {
            return number();
        }
        if (r == Rank.SKIP || r == Rank.REVERSE || r == Rank.DRAW_TWO) {
            return 20;
        }
        if (r == Rank.WILD || r == Rank.WILD_DRAW_FOUR) {
            return 50;
        }
        return 0;
    }

    @Override
    public String toString() {
        return code;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Card)) {
            return false;
        }
        return code.equals(((Card) o).code);
    }

    @Override
    public int hashCode() {
        return code.hashCode();
    }
}
