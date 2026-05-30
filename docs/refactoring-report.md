# Refactoring Report

**Student:** Maria Ghobnelishvili

## What behavior did you characterize before refactoring?

Before any structural changes, 52 characterization tests were added covering:

- **Matching by color** — a card of the same color as the top card is always legal.
- **Matching by number** — a number card of the same face value (different color) is legal.
- **Matching by action type** — skip matches skip, reverse matches reverse, draw-two matches draw-two, regardless of color.
- **Wild and wild draw four** — both are always legal to play.
- **Called color after a wild** — the called color acts as the active color constraint on the next play.
- **Illegal mismatches** — a card that shares neither color nor rank with the top card is rejected.
- **Card parsing** — color, rank, number, and points values derived from compact card codes (`R5`, `YS`, `W4`, etc.).
- **Scoring** — number cards score face value; skip/reverse/draw-two score 20; wilds score 50.
- **Bot card priority** — the bot prefers draw-two over skip over number over wild.
- **Bot never plays reverse (documented surprise)** — `chooseBotCard` scans only draw-two, skip, number, and wild by rank; reverse is never selected, even when a same-color reverse is perfectly legal. The card is legal by the rules, but the bot ignores it. This quirk is captured explicitly.
- **Bot color choice** — the bot calls the color it holds the most of; ties resolve in the order R > Y > G > B.
- **Five end-to-end golden transcripts** — full deterministic game runs (seeded) comparing byte-for-byte output against recordings made from the original code. These cover skip, reverse, the two-player "reverse acts as skip" case, draw-two, wild, wild draw four, drawing from the deck, bot auto-play of drawn cards, reaching the 3000-turn safety limit, and final scoring.

## What were the worst design problems you found?

The starting `Main.java` had several tangled responsibilities:

1. **Primitive card representation** — cards were raw strings. Color, rank, legality, and points were re-derived from the string by if-chains scattered through the turn loop, the bot, the human prompt, and the standalone `selfTest` method.
2. **Duplicated legal-play logic** — `isLegal` decisions appeared in at least four places with no single authoritative source.
3. **Deck management buried in the loop** — shuffling, drawing, reshuffling from the discard pile, and the empty-deck fallback were inline in `playGame` and `draw()`, making them invisible to tests.
4. **Console output mixed with rule execution** — the same turn-loop body evaluates card legality, mutates game state, and calls `System.out.println`, making any one piece hard to test in isolation.
5. **Global mutable state** — `upCard`, `calledColor`, `direction`, `currentPlayer`, `hands`, and `scores` are static fields, coupling every method to the full runtime state.

## Which refactorings did you perform?

All changes were incremental; characterization tests were rerun after each step.

### Step 1 — Extract `Rank` enum

Replaced the scattered string-based rank comparisons (`endsWith("S")`, `endsWith("+2")`, etc.) with a proper enum. This was the smallest possible change and eliminated the first category of string magic.

### Step 2 — Extract `Card` value object

Moved all color/rank/number/points derivation from raw strings into an immutable `Card` class. `Card.color()`, `Card.rank()`, `Card.number()`, and `Card.points()` are now the single definitions of those properties. The compact code string (`R5`, `W4`, …) is still the identity and the display form, so `toString()` and console output are unchanged.

### Step 3 — Extract `Rules` class

Pulled `isLegal` and `handPoints` into a stateless `Rules` class. This eliminated the duplication of legal-play logic and made it testable without the CLI. The `Rules` class has no console dependency and no game-state fields.

### Step 4 — Extract `Deck` class

Moved deck construction, shuffling, drawing, and the reshuffle-from-discard behavior into a `Deck` class. The `Main` class now calls `deck.draw()` and `deck.discard()` instead of managing two raw `ArrayList<Card>` fields inline. Randomness is fully behind this boundary.

## What behavior did you intentionally preserve?

All behavior documented in `docs/rules.html` and captured by characterization tests was preserved unchanged:

- All hands are visible to all players in the terminal.
- A human can type `draw` even when holding a legal card.
- An illegal card index triggers a penalty draw and ends the player's turn.
- A bot automatically plays a drawn card when it is legal.
- A two-player reverse acts as a skip (implemented by calling `next()` twice).
- The game stops at the 3000-turn safety limit and prints "Game stopped at safety limit."
- The bot never plays a reverse from its hand (the documented surprise quirk).

## What risks remain?

- **`Main` is still a god class** — turn orchestration, console rendering, human prompts, bot strategy, and player state all live together. A change to any one area still requires reading the whole loop.
- **Static global state** — `upCard`, `calledColor`, `direction`, `currentPlayer`, and `scores` are static fields. Two simultaneous games would corrupt each other, and test isolation is limited to resetting these fields by hand (as `selfTest` and `CharacterizationTest` do).
- **Bot logic is not independently testable** — `chooseBotCard` reads `Main.upCard` and `Main.calledColor` directly, so tests must mutate static state to exercise it.
- **Console and game logic remain coupled** — the turn loop interleaves `System.out.println` calls with state mutations. Replacing the CLI view requires editing the same loop that enforces the rules.
