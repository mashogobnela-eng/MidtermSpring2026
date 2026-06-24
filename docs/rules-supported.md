# Supported Rules

This lists which rules from `Final_Project_UNO_rules_reference.md` are
implemented, and the variants/simplifications chosen. ✅ = implemented,
⚠️ = implemented with a documented variant.

| Rule | Status | Notes |
| ---- | ------ | ----- |
| Deck composition (108 cards) | ✅ | Built by `Deck.buildStandardDeck()`; unit tested in `DeckCompositionTest`. |
| Basic turn flow | ✅ | `Main.playGame()` deals 7 cards each, flips a start card, then loops turns. |
| Starting card is an action/wild | ⚠️ | If the first up-card is a Wild it is redrawn until a non-wild starts (action cards are allowed as the start card and simply sit on the pile). |
| Legal play validation | ✅ | `Rules.isLegal(card, up, calledColor)`: match by color, number, action type, or any wild. Tested in `CharacterizationTest`. |
| Skip | ✅ | Next player loses their turn (advance twice). Tested in `TurnOrderTest` and the golden transcripts. |
| Reverse | ⚠️ | Flips direction for 3+ players; **with 2 players Reverse acts like Skip**. Tested in `TurnOrderTest` and goldens. |
| Draw Two | ✅ | Next player draws two and loses their turn. Exercised by goldens. No stacking. |
| Wild | ✅ | Player chooses the active color; chosen color drives later legal-play checks. Bots pick their majority color. |
| Wild Draw Four | ⚠️ | Player chooses color; next player draws four and loses their turn. **No challenge rule** (an accepted simplification). |
| Draw / pass behavior | ⚠️ | A player who cannot (or chooses not to) play draws one card; if that card is legal it may be played immediately (bots auto-play it, a human is prompted), otherwise the turn passes. |
| UNO call + missed-UNO penalty | ✅ | `UnoCall`: the one-card state is detected; missing the call draws a 2-card penalty. **Opt-in via `--uno-penalty`** (a human is prompted to call; bots always call). Tested in `UnoCallTest`. |
| Round end | ✅ | A round ends when a player empties their hand; that player wins the round. |
| Scoring | ✅ | Winner scores the points held in opponents' hands. Number = face value; Skip/Reverse/Draw Two = 20; Wild/Wild Draw Four = 50. Tested in `CharacterizationTest`. |
| Multi-round game to target | ✅ | **Opt-in via `--target N`** (default 500). Rounds repeat until a player reaches the target; the final winner is the highest score. Logic in `Scoreboard`, tested in `ScoreboardTest` and a golden. |

## Variants and simplifications (summary)

- **2-player Reverse = Skip** (standard UNO variant).
- **No Wild Draw Four challenge** (accepted simplification).
- **No Draw Two / Wild Draw Four stacking.**
- **Draw-then-play-if-legal** draw/pass variant.
- **UNO penalty and target match are opt-in flags**, so the default game keeps
  the exact midterm behavior that the characterization golden transcripts lock.
  This is a deliberate choice so that adding rules never silently changed —
  or hid a regression in — existing behavior.
- **Fixed UNO penalty of 2 cards.**
- **Deterministic deck** via `--seed` for reproducible tests.
- **Bot strategy is intentionally simple** and carries two documented quirks from
  the midterm: a bot never plays a Reverse from its hand, and weak 2-bot games
  can stall to a safety limit. In a `--target` match a stalled round scores 0 and
  the match has a round-count safety cap.
