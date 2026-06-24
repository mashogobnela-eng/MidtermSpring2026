# Database & Persistence (Assignment 5)

Assignment 5 adds game-history persistence to the UNO project using an ORM, so
finished games are stored and useful player statistics can be queried.

## Selected database

**H2**, embedded.

- Normal use: a **file** database at `./data/uno.mv.db`
  (`jdbc:h2:file:./data/uno;AUTO_SERVER=TRUE`). History survives between runs,
  and `AUTO_SERVER=TRUE` lets a game and a report command access it at the same
  time.
- Tests: an **in-memory** database with a unique name per test and `create-drop`
  schema, so tests never touch the file database or any developer-specific state.

No credentials are stored in source beyond H2's default empty password, and no
machine-specific paths are hard-coded.

## Selected ORM / persistence framework

**Hibernate ORM 6 (Jakarta Persistence / JPA)**.

- Configuration: `src/main/resources/META-INF/persistence.xml`, persistence unit
  `uno-pu`, `RESOURCE_LOCAL` transactions.
- The schema is generated from the JPA entity mappings
  (`hibernate.hbm2ddl.auto=update`), so no manual SQL DDL is required.

## Schema

Four tables, mapped from JPA entities in `uno.persistence`:

| Table     | Entity         | Key columns                                              |
| --------- | -------------- | ------------------------------------------------------- |
| `players` | `PlayerEntity` | `id`, `name` (unique)                                    |
| `games`   | `GameEntity`   | `id`, `played_at` (timestamp), `rounds_played`, `winner_id` → players |
| `rounds`  | `RoundEntity`  | `id`, `game_id` → games, `round_number`, `winner_id` → players, `points` |
| `scores`  | `ScoreEntity`  | `id`, `game_id` → games, `player_id` → players, `points` |

This covers all the required data: **players, games, rounds, scores, winner, and
a timestamp.**

A game = one program session. Each round (one hand played to completion) is
stored in `rounds`; each player's final cumulative score is stored in `scores`;
the overall winner (highest final score) is stored on the game.

## Architecture

- **Entities** (`uno.persistence.*Entity`) — JPA-mapped tables.
- **`Database`** — owns the `EntityManagerFactory`; the single place the ORM is
  bootstrapped. `open()` uses the file database; `open(overrides)` lets tests
  inject the in-memory URL.
- **`GameRepository`** — the DAO. All database access (one write path + the three
  report queries) is JPQL here; the game and CLI code contain **no SQL**.
- **`GameResult`** — a plain DTO the game hands to the repository, so the game
  logic has no ORM dependency.
- **`Reports`** — formats query results for the CLI report mode.

## How schema setup happens

Automatic. On first save the entity mappings create/upgrade the four tables in
`./data/uno.mv.db`. There is no manual init step.

## Persisting a game

Pass `--save` to record the finished session:

```bash
java -jar target/uno.jar --bots 3 --games 3 --seed 7 --save
# ... game output ...
# Saved game #1 to the database.
```

Persistence is **opt-in**: without `--save` no database is opened, so normal play
is unchanged.

## Viewing history / statistics

```bash
java -jar target/uno.jar --report            # all three reports
java -jar target/uno.jar --report recent     # recent games
java -jar target/uno.jar --report wins       # player win counts
java -jar target/uno.jar --report highscores # highest single-game scores
```

Example:

```text
Recent games:
  #2  2026-06-24 22:30:09  winner=Bot2  rounds=2  [Bot2=93, Bot1=0]
  #1  2026-06-24 22:30:07  winner=Bot2  rounds=3  [Bot2=87, Bot1=69, Bot3=0]

Player win counts:
  Bot2: 2

Highest scores:
  93  Bot2  (game #2, 2026-06-24 22:30:09)
  87  Bot2  (game #1, 2026-06-24 22:30:07)
```

## Running the persistence tests

The repository tests run with the rest of the suite, against isolated in-memory
H2 — no setup, no manual classpath, no external database:

```bash
mvn test
# uno.persistence.GameRepositoryTest covers save + the three report queries
```

## Notes

- H2's built-in connection pool prints a "not intended for production" notice;
  that is expected for a course/dev project and is kept off the CLI.
- To reset history, delete the `data/` directory.
