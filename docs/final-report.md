# Final Project Report

## Overview

This project turns the midterm UNO CLI into a more complete, maintainable UNO
application. It carries the work through three branches, each built on the
previous one:

- **Assignment 4** — Maven build, JUnit-integrated tests, event logging, Docker.
- **Assignment 5** — Hibernate/JPA + H2 persistence with history reports.
- **Final** — fuller UNO rules (UNO call + penalty, multi-round match), more
  tests, documentation, and this report.

A guiding principle throughout: the midterm's behavior is locked by byte-for-byte
**golden transcripts**, and the default game keeps passing them. New rules that
change gameplay are added as opt-in modes with their own tests, so nothing
silently changed — or hid a regression in — the original behavior.

## UNO rules implemented

Deck composition, legal-play validation, Skip, Reverse (2-player = Skip), Draw
Two, Wild, Wild Draw Four, draw/pass, round scoring, **UNO call + missed-UNO
penalty**, and a **multi-round match to a target score**. The full feature list,
with the chosen variants and simplifications, is in
[`rules-supported.md`](rules-supported.md).

## Playing from the CLI

```bash
mvn package                                   # build target/uno.jar

# bot game:
java -jar target/uno.jar --bots 3 --games 1

# interactive game:
java -jar target/uno.jar --human --bots 2 --games 1

# full match to 500, with the UNO-call penalty rule:
java -jar target/uno.jar --human --bots 3 --target 500 --uno-penalty
```

Options: `--bots N`, `--games N`, `--human`, `--quiet`, `--seed N`,
`--target [N]` (multi-round match, default target 500), `--uno-penalty`,
`--save` (store the result), `--report [recent|wins|highscores]`.

During a human turn, enter a card by index or code (`R5`, `YS`, `BR`, `G+2`,
`W`, `W4`) or `draw`. After a wild, choose a color `R/Y/G/B`. With
`--uno-penalty`, a human reduced to one card is asked to "Call UNO?"; forgetting
draws a 2-card penalty. Invalid input is rejected and re-prompted rather than
crashing.

## Architecture: game logic vs. CLI

Rule and state logic is kept separate from console interaction so it can be
tested without input:

- `Card`, `Rank` — immutable card model.
- `Deck` — deck construction (`buildStandardDeck()`), shuffle, draw/reshuffle.
- `Rules` — legal-play validation, hand scoring, and turn advancement
  (`nextPlayer`) — pure functions, no console.
- `UnoCall` — UNO-call detection and penalty — pure functions.
- `Scoreboard` — match target/winner logic — pure functions.
- `GameLog` — file-only event logging (never touches the CLI output).
- `uno.persistence.*` — JPA entities, `Database`, `GameRepository` (DAO, JPQL
  only), `Reports`, and the `GameResult` DTO that keeps the game layer ORM-free.
- `Main` — the CLI: argument parsing, the turn loop, prompts, and output. It
  delegates rule decisions to the classes above.

Because the rule logic lives in `Rules`, `Deck`, `UnoCall`, and `Scoreboard`,
each is unit tested directly, with no console driving required.

## Tests added

`mvn test` runs **40 tests** with no manual classpath setup:

- `CharacterizationTest` (13) — unit checks for legal play, card parsing,
  scoring, and bot strategy, **plus six end-to-end golden transcripts** that run
  the real CLI in a child process and compare output byte-for-byte. Five lock the
  original gameplay (Skip, Reverse incl. 2-player, Draw Two, Wild, Wild Draw
  Four, draw/pass, scoring); one locks the new `--target` match output.
- `DeckCompositionTest` (5) — the 108-card composition.
- `TurnOrderTest` (5) — Skip, Reverse, and 2-player Reverse-as-Skip.
- `UnoCallTest` (4) — UNO call detection and the missed-call penalty.
- `ScoreboardTest` (6) — target detection and final-winner selection.
- `GameRepositoryTest` (8) — persistence: `save` + the three report queries on
  isolated in-memory H2.

## Limitations

- **Bot strategy is intentionally simple** and keeps two documented midterm
  quirks: a bot never plays a Reverse from its hand, and weak 2-bot games can
  stall to a 3000-turn safety limit. A `--target` match guards against this with
  a round-count cap (a stalled round scores 0).
- **No Wild Draw Four challenge** and **no card stacking** (accepted
  simplifications).
- **UNO penalty and target match are opt-in flags.** This is deliberate (it
  protects the golden transcripts), but it means the penalty rule is only
  observed in play when `--uno-penalty` is passed; bots always declare UNO, so
  the missed-call penalty is exercised by a human player and by `UnoCallTest`.
- The embedded H2 database uses a development connection pool (it prints a
  "not for production" notice, kept off the CLI) and is intended for local/course
  use, not production.
