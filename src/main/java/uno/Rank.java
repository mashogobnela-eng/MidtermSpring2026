package uno;

/**
 * The kind of a card. Enum names match the string ranks the original code used
 * ("NUMBER", "SKIP", ...), so behavior and any rank-name output are unchanged.
 */
public enum Rank {
    NUMBER,
    SKIP,
    REVERSE,
    DRAW_TWO,
    WILD,
    WILD_DRAW_FOUR
}
