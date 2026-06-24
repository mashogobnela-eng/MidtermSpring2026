# UNO CLI

A command-line UNO-like game in Java. This repository continues the midterm UNO
project; **Assignment 4** turns it into a standard Maven project with automated
tests, event logging, and Docker support.

## Requirements

- Java 17 (JDK)
- Maven 3.9+
- Docker (optional, for the container commands)

## Commands

All commands run from the repository root.

### Local build (compile)

```bash
mvn compile
```

### Local test

Runs the full JUnit 5 suite (unit checks **and** the end-to-end golden
transcripts) through Maven — no manual classpath setup:

```bash
mvn test
```

### Package (create the runnable jar)

Produces a self-contained executable jar at `target/uno.jar`:

```bash
mvn package
```

### Local run

```bash
# after packaging:
java -jar target/uno.jar --bots 3 --games 5 --quiet

# or compile-and-run in one step:
mvn -q exec:java -Dexec.args="--bots 3 --games 5 --quiet"
```

### Docker build

Builds the application entirely from repository contents:

```bash
docker build -t uno .
```

### Docker run

```bash
# bot game (overrides the default CMD):
docker run --rm uno --bots 3 --games 1 --quiet

# interactive game (note the -it for stdin):
docker run --rm -it uno --human --bots 2 --games 1
```

## Helper scripts

Thin wrappers around the Maven commands above:

```bash
scripts/compile.sh                 # mvn compile
scripts/test.sh                    # mvn test
scripts/run.sh --bots 3 --quiet    # package + java -jar
```

## Command-line options

| Option        | Meaning                                   |
| ------------- | ----------------------------------------- |
| `--bots N`    | number of bot players                     |
| `--games N`   | number of games to play                   |
| `--human`     | add a human player ("You")                |
| `--quiet`     | suppress per-turn output, show final score |
| `--seed N`    | fixed RNG seed (deterministic games)      |
| `--help`      | print usage                               |

## Interactive play

When playing with `--human`, enter a card by index or by code, or `draw`:

```text
R5    red 5
YS    yellow skip
BR    blue reverse
G+2   green draw two
W     wild
W4    wild draw four
draw  draw a card
```

## Logging

Important game events — game start, player turn, card played, card drawn,
invalid input, and round/game end — are written to `logs/uno.log` via
`java.util.logging`. Logging goes **only to the file**, never to the console, so
it never replaces the player-facing CLI output. Override the directory with
`-Duno.log.dir=<path>`.

## Project layout

```text
pom.xml                          Maven build
Dockerfile                       multi-stage build + run image
src/main/java/uno/               game source (Card, Deck, Rank, Rules, Main, GameLog)
src/test/java/uno/               JUnit 5 tests (CharacterizationTest)
src/test/resources/golden/       recorded golden transcripts
scripts/                         convenience wrappers
docs/                            rules and midterm materials
```

## Rules

See `docs/rules.html` for the implemented game rules.

## Submission

Work is delivered through GitHub branches (`assignment-4`, `assignment-5`,
`final`), each branched from the previous one, with a pull request per branch.
