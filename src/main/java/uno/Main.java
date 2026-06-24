package uno;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Random;
import java.util.Scanner;

import uno.persistence.Database;
import uno.persistence.GameRepository;
import uno.persistence.GameResult;
import uno.persistence.Reports;

public class Main {
    static ArrayList<String> playerNames = new ArrayList<String>();
    static ArrayList<Boolean> humanPlayers = new ArrayList<Boolean>();
    static ArrayList<ArrayList<Card>> hands = new ArrayList<ArrayList<Card>>();
    static Deck deck;
    static int[] scores = new int[10];
    static int currentPlayer = 0;
    static int direction = 1;
    static Card upCard = null;
    static String calledColor = "";
    static boolean quiet = false;
    static long seed = 0;
    static boolean save = false;
    static String reportMode = null;
    static int target = 0;
    static boolean unoPenalty = false;
    static final int MATCH_ROUND_CAP = 1000;
    static int roundCounter = 0;
    static ArrayList<GameResult.RoundResult> roundResults = new ArrayList<GameResult.RoundResult>();
    static Random random = new Random();
    static Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {
        int bots = 3;
        int games = 1;
        boolean human = false;
        seed = System.currentTimeMillis();

        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("--bots") && i + 1 < args.length) {
                bots = Integer.parseInt(args[++i]);
            } else if (args[i].equals("--games") && i + 1 < args.length) {
                games = Integer.parseInt(args[++i]);
            } else if (args[i].equals("--human")) {
                human = true;
            } else if (args[i].equals("--quiet")) {
                quiet = true;
            } else if (args[i].equals("--seed") && i + 1 < args.length) {
                seed = Long.parseLong(args[++i]);
            } else if (args[i].equals("--save")) {
                save = true;
            } else if (args[i].equals("--report")) {
                reportMode = "all";
                if (i + 1 < args.length && !args[i + 1].startsWith("--")) {
                    reportMode = args[++i];
                }
            } else if (args[i].equals("--target")) {
                target = Scoreboard.DEFAULT_TARGET;
                if (i + 1 < args.length && !args[i + 1].startsWith("--")) {
                    target = Integer.parseInt(args[++i]);
                }
            } else if (args[i].equals("--uno-penalty")) {
                unoPenalty = true;
            } else if (args[i].equals("--self-test")) {
                selfTest();
                return;
            } else if (args[i].equals("--help")) {
                System.out.println("Usage: scripts/run.sh [--bots N] [--games N] [--human] [--quiet] [--seed N] [--save] [--report [recent|wins|highscores]] [--target [N]] [--uno-penalty]");
                return;
            }
        }

        if (reportMode != null) {
            runReport(reportMode);
            return;
        }

        roundResults.clear();
        roundCounter = 0;
        random = new Random(seed);
        deck = new Deck(random);
        setupPlayers(bots, human);

        if (playerNames.size() < 2 || playerNames.size() > 4) {
            System.out.println("UNO needs 2 to 4 players.");
            return;
        }

        if (target > 0) {
            // Multi-round match: play rounds until a player reaches the target
            // score (with a safety cap in case weak bots keep stalling).
            int g = 0;
            while (!Scoreboard.reachedTarget(scores, playerNames.size(), target) && g < MATCH_ROUND_CAP) {
                g++;
                if (!quiet) {
                    System.out.println("\n=== Round " + g + " (target " + target + ") ===");
                }
                playGame();
            }
        } else {
            for (int g = 1; g <= games; g++) {
                if (!quiet) {
                    System.out.println("\n=== Game " + g + " ===");
                }
                playGame();
            }
        }

        System.out.println("\nFinal scores:");
        StringBuilder summary = new StringBuilder();
        for (int i = 0; i < playerNames.size(); i++) {
            System.out.println(playerNames.get(i) + ": " + scores[i]);
            if (i > 0) {
                summary.append(", ");
            }
            summary.append(playerNames.get(i)).append("=").append(scores[i]);
        }

        if (target > 0) {
            int w = Scoreboard.leader(scores, playerNames.size());
            System.out.println("\nMatch winner: " + playerNames.get(w)
                    + " with " + scores[w] + " points (target " + target + ").");
        }

        GameLog.gameEnd(summary.toString());

        if (save) {
            persistGame();
        }
    }

    static void setupPlayers(int bots, boolean human) {
        playerNames.clear();
        humanPlayers.clear();
        hands.clear();
        if (human) {
            playerNames.add("You");
            humanPlayers.add(Boolean.TRUE);
            hands.add(new ArrayList<Card>());
        }
        for (int i = 1; i <= bots; i++) {
            playerNames.add("Bot" + i);
            humanPlayers.add(Boolean.FALSE);
            hands.add(new ArrayList<Card>());
        }
    }

    static void playGame() {
        roundCounter++;
        deck.startNewDeck();
        for (int i = 0; i < hands.size(); i++) {
            hands.get(i).clear();
        }
        for (int i = 0; i < playerNames.size(); i++) {
            for (int j = 0; j < 7; j++) {
                hands.get(i).add(deck.draw());
            }
        }
        upCard = deck.draw();
        while (upCard.isWild()) {
            deck.discard(upCard);
            upCard = deck.draw();
        }
        calledColor = "";
        direction = 1;
        currentPlayer = random.nextInt(playerNames.size());
        GameLog.gameStart(playerNames.size(), seed);

        int guard = 0;
        while (guard < 3000) {
            guard++;
            String name = playerNames.get(currentPlayer);
            ArrayList<Card> hand = hands.get(currentPlayer);
            GameLog.playerTurn(name, hand.size(), upCard.toString(), calledColor);

            if (!quiet) {
                System.out.println("\nUp card: " + upCard + (calledColor.equals("") ? "" : " called " + calledColor));
                System.out.println(name + " hand: " + join(hand));
            }

            int chosen = -1;
            if (humanPlayers.get(currentPlayer).booleanValue()) {
                chosen = askHuman(hand);
            } else {
                chosen = chooseBotCard(hand);
            }

            if (chosen == -1) {
                Card drawn = deck.draw();
                hand.add(drawn);
                GameLog.cardDrawn(name, drawn.toString());
                if (!quiet) {
                    System.out.println(name + " draws " + drawn);
                }
                if (Rules.isLegal(drawn, upCard, calledColor)) {
                    if (!humanPlayers.get(currentPlayer).booleanValue()) {
                        chosen = hand.size() - 1;
                    } else {
                        System.out.print("Play drawn card " + drawn + "? y/n: ");
                        String answer = scanner.nextLine();
                        if (answer.equalsIgnoreCase("y") || answer.equalsIgnoreCase("yes")) {
                            chosen = hand.size() - 1;
                        }
                    }
                }
            }

            if (chosen >= 0) {
                if (chosen >= hand.size()) {
                    GameLog.invalidInput(name, "index " + chosen + " out of range");
                    if (!quiet) {
                        System.out.println(name + " selected an invalid index and draws a penalty card.");
                    }
                    Card penalty = deck.draw();
                    hand.add(penalty);
                    GameLog.cardDrawn(name, penalty.toString());
                    next();
                    continue;
                }

                Card card = hand.get(chosen);
                boolean ok = Rules.isLegal(card, upCard, calledColor);

                if (!ok) {
                    GameLog.invalidInput(name, "illegal card " + card);
                    if (!quiet) {
                        System.out.println(name + " tried illegal card " + card + " and draws a penalty card.");
                    }
                    Card penalty = deck.draw();
                    hand.add(penalty);
                    GameLog.cardDrawn(name, penalty.toString());
                    next();
                    continue;
                }

                hand.remove(chosen);
                deck.discard(upCard);
                upCard = card;
                calledColor = "";
                GameLog.cardPlayed(name, card.toString());
                if (!quiet) {
                    System.out.println(name + " plays " + card);
                }

                if (card.isWild()) {
                    if (humanPlayers.get(currentPlayer).booleanValue()) {
                        calledColor = askColor();
                    } else {
                        calledColor = chooseBotColor(hand);
                    }
                    if (!quiet) {
                        System.out.println(name + " calls " + calledColor);
                    }
                }

                if (hand.size() == 1) {
                    // One-card state. By default everyone declares UNO. With the
                    // --uno-penalty rule, a human must actively call it; failing
                    // to do so draws a penalty (bots always remember to call).
                    boolean declared = true;
                    if (unoPenalty && humanPlayers.get(currentPlayer).booleanValue()) {
                        declared = askUnoCall();
                    }
                    if (declared) {
                        if (!quiet) {
                            System.out.println(name + " says UNO!");
                        }
                    } else {
                        int pen = UnoCall.penalty(false, hand.size());
                        for (int i = 0; i < pen; i++) {
                            Card p = deck.draw();
                            hand.add(p);
                            GameLog.cardDrawn(name, p.toString());
                        }
                        GameLog.invalidInput(name, "missed UNO call, drew " + pen);
                        if (!quiet) {
                            System.out.println(name + " forgot to call UNO and draws " + pen + ".");
                        }
                    }
                }

                if (hand.size() == 0) {
                    int points = 0;
                    for (int i = 0; i < hands.size(); i++) {
                        if (i != currentPlayer) {
                            points += Rules.handPoints(hands.get(i));
                        }
                    }
                    scores[currentPlayer] += points;
                    GameLog.roundEnd(name, points);
                    roundResults.add(new GameResult.RoundResult(roundCounter, name, points));
                    if (!quiet) {
                        System.out.println(name + " wins and scores " + points);
                    }
                    return;
                }

                if (card.rank() == Rank.SKIP) {
                    next();
                    next();
                } else if (card.rank() == Rank.REVERSE) {
                    direction = direction * -1;
                    if (playerNames.size() == 2) {
                        next();
                        next();
                    } else {
                        next();
                    }
                } else if (card.rank() == Rank.DRAW_TWO) {
                    next();
                    Card d1 = deck.draw();
                    Card d2 = deck.draw();
                    hands.get(currentPlayer).add(d1);
                    hands.get(currentPlayer).add(d2);
                    GameLog.cardDrawn(playerNames.get(currentPlayer), d1.toString());
                    GameLog.cardDrawn(playerNames.get(currentPlayer), d2.toString());
                    if (!quiet) {
                        System.out.println(playerNames.get(currentPlayer) + " draws two.");
                    }
                    next();
                } else if (card.rank() == Rank.WILD_DRAW_FOUR) {
                    next();
                    for (int i = 0; i < 4; i++) {
                        Card d = deck.draw();
                        hands.get(currentPlayer).add(d);
                        GameLog.cardDrawn(playerNames.get(currentPlayer), d.toString());
                    }
                    if (!quiet) {
                        System.out.println(playerNames.get(currentPlayer) + " draws four.");
                    }
                    next();
                } else {
                    next();
                }
            } else {
                next();
            }
        }
        roundResults.add(new GameResult.RoundResult(roundCounter, null, 0));
        if (!quiet) {
            System.out.println("Game stopped at safety limit.");
        }
    }

    static int chooseBotCard(ArrayList<Card> hand) {
        int drawTwo = firstLegalWithRank(hand, Rank.DRAW_TWO);
        if (drawTwo >= 0) {
            return drawTwo;
        }
        int skip = firstLegalWithRank(hand, Rank.SKIP);
        if (skip >= 0) {
            return skip;
        }
        int number = firstLegalWithRank(hand, Rank.NUMBER);
        if (number >= 0) {
            return number;
        }
        for (int i = 0; i < hand.size(); i++) {
            if (hand.get(i).isWild()) {
                return i;
            }
        }
        return -1;
    }

    // Index of the first legal card of the given rank, or -1. Note this never
    // matches REVERSE, so a bot will not play a reverse from its hand.
    static int firstLegalWithRank(ArrayList<Card> hand, Rank rank) {
        for (int i = 0; i < hand.size(); i++) {
            Card card = hand.get(i);
            if (card.rank() == rank && Rules.isLegal(card, upCard, calledColor)) {
                return i;
            }
        }
        return -1;
    }

    static int askHuman(ArrayList<Card> hand) {
        while (true) {
            System.out.print("Choose card index/code or draw: ");
            String input = scanner.nextLine().trim().toUpperCase();
            if (input.equals("DRAW")) {
                return -1;
            }
            try {
                int index = Integer.parseInt(input);
                if (index >= 0 && index < hand.size()) {
                    return index;
                }
            } catch (Exception ignored) {
            }
            for (int i = 0; i < hand.size(); i++) {
                if (hand.get(i).code().equals(input)) {
                    if (Rules.isLegal(hand.get(i), upCard, calledColor)) {
                        return i;
                    }
                    GameLog.invalidInput("You", "illegal selection " + input);
                    System.out.println("That card is not legal.");
                }
            }
            GameLog.invalidInput("You", "card not found: " + input);
            System.out.println("Card not found.");
        }
    }

    static boolean askUnoCall() {
        System.out.print("Call UNO? y/n: ");
        String input = scanner.nextLine().trim().toUpperCase();
        return input.equals("Y") || input.equals("YES");
    }

    static String askColor() {
        while (true) {
            System.out.print("Call color R/Y/G/B: ");
            String input = scanner.nextLine().trim().toUpperCase();
            if (input.equals("R")) {
                return "R";
            }
            if (input.equals("Y")) {
                return "Y";
            }
            if (input.equals("G")) {
                return "G";
            }
            if (input.equals("B")) {
                return "B";
            }
            System.out.println("Bad color.");
        }
    }

    static String chooseBotColor(ArrayList<Card> hand) {
        int r = 0;
        int y = 0;
        int g = 0;
        int b = 0;
        for (int i = 0; i < hand.size(); i++) {
            String c = hand.get(i).color();
            if (c.equals("R")) {
                r++;
            } else if (c.equals("Y")) {
                y++;
            } else if (c.equals("G")) {
                g++;
            } else if (c.equals("B")) {
                b++;
            }
        }
        if (r >= y && r >= g && r >= b) {
            return "R";
        } else if (y >= r && y >= g && y >= b) {
            return "Y";
        } else if (g >= r && g >= y && g >= b) {
            return "G";
        } else {
            return "B";
        }
    }

    static void next() {
        currentPlayer = Rules.nextPlayer(currentPlayer, direction, playerNames.size());
    }

    static String join(ArrayList<Card> cards) {
        String out = "";
        for (int i = 0; i < cards.size(); i++) {
            out += i + ":" + cards.get(i);
            if (i < cards.size() - 1) {
                out += " ";
            }
        }
        return out;
    }

    /** Print a history/statistics report from the database, then exit. */
    static void runReport(String which) {
        try (Database db = Database.open()) {
            Reports reports = new Reports(new GameRepository(db));
            if (which.equals("recent")) {
                reports.printRecentGames(10);
            } else if (which.equals("wins")) {
                reports.printWinCounts();
            } else if (which.equals("highscores") || which.equals("high") || which.equals("scores")) {
                reports.printHighScores(10);
            } else {
                reports.printAll(10);
            }
        }
    }

    /** Persist the finished session (players, rounds, scores, winner, timestamp). */
    static void persistGame() {
        ArrayList<GameResult.PlayerScore> finalScores = new ArrayList<GameResult.PlayerScore>();
        int best = -1;
        String winner = null;
        for (int i = 0; i < playerNames.size(); i++) {
            finalScores.add(new GameResult.PlayerScore(playerNames.get(i), scores[i]));
            if (scores[i] > best) {
                best = scores[i];
                winner = playerNames.get(i);
            }
        }
        GameResult result = new GameResult(Instant.now(),
                new ArrayList<String>(playerNames), roundResults, finalScores, winner);
        try (Database db = Database.open()) {
            GameRepository repo = new GameRepository(db);
            Long id = repo.save(result);
            System.out.println("\nSaved game #" + id + " to the database.");
        }
    }

    static void selfTest() {
        int passed = 0;
        if (new Card("R5").color().equals("R")) passed++; else fail("color R5");
        if (new Card("G+2").rank() == Rank.DRAW_TWO) passed++; else fail("rank +2");
        if (new Card("W4").points() == 50) passed++; else fail("wild points");
        if (Rules.isLegal(new Card("R2"), new Card("R9"), "")) passed++; else fail("same color");
        if (Rules.isLegal(new Card("G9"), new Card("R9"), "")) passed++; else fail("same number");
        if (Rules.isLegal(new Card("B3"), new Card("W"), "B")) passed++; else fail("called color");
        if (!Rules.isLegal(new Card("B3"), new Card("R9"), "")) passed++; else fail("illegal mismatch");

        ArrayList<Card> h = new ArrayList<Card>();
        h.add(new Card("B3"));
        h.add(new Card("R4"));
        h.add(new Card("W"));
        upCard = new Card("R9");
        calledColor = "";
        if (chooseBotCard(h) == 1) passed++; else fail("bot normal before wild");

        ArrayList<Card> h2 = new ArrayList<Card>();
        h2.add(new Card("B1"));
        h2.add(new Card("B2"));
        h2.add(new Card("R3"));
        if (chooseBotColor(h2).equals("B")) passed++; else fail("bot color");

        System.out.println("Passed " + passed + " characterization checks.");
    }

    static void fail(String name) {
        throw new RuntimeException("Failed: " + name);
    }
}
