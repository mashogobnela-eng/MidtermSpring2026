# Extension Readiness

**Student:** Maria Ghobnelishvili

## Which extension would your design support best?

**Adding a smarter bot strategy** is the most natural extension given the current design.

The refactoring extracted `Rules.isLegal` and `Rules.handPoints` into a stateless, console-free class. A bot strategy needs exactly this: a way to evaluate whether a card is playable and how much it is worth. A new strategy class can call `Rules.isLegal` freely without any dependency on the CLI or on game-loop state.

## Where would that change be implemented?

Currently, `chooseBotCard` and `chooseBotColor` are static methods in `Main` that read `Main.upCard` and `Main.calledColor` directly. To add a second strategy (e.g., one that plays the highest-scoring legal card rather than the first draw-two found), the change would be:

1. Define a `BotStrategy` interface with a `chooseCard(List<Card> hand, Card upCard, String calledColor)` method and a `chooseColor(List<Card> hand)` method.
2. Move the current logic into a `DefaultBotStrategy` implementing that interface.
3. Write the new strategy as a second implementation of the same interface.
4. Pass the strategy to each bot player at setup time.

Because `Rules` is already decoupled from `Main`, neither strategy implementation needs to import or reference the CLI at all.

## What part of your design still makes change difficult?

Two things resist this extension:

- **Static state coupling in `chooseBotCard`** — the method reads `Main.upCard` and `Main.calledColor` as static fields instead of receiving them as parameters. Before the interface above can be cleanly introduced, these reads need to become parameters (a one-step Extract Parameter refactoring).
- **No `Player` abstraction** — human and bot players are distinguished by a parallel `ArrayList<Boolean> humanPlayers` rather than by separate types. Introducing per-player strategy selection would be cleaner if each player were an object that carried its own `isHuman` flag and, for bots, its own `BotStrategy` instance. That abstraction does not yet exist.
