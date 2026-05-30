import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Scanner;

public class Main {
    static ArrayList<String> playerNames = new ArrayList<String>();
    static ArrayList<Boolean> humanPlayers = new ArrayList<Boolean>();
    static ArrayList<ArrayList<Card>> hands = new ArrayList<ArrayList<Card>>();
    static ArrayList<Card> deck = new ArrayList<Card>();
    static ArrayList<Card> discard = new ArrayList<Card>();
    static int[] scores = new int[10];
    static int currentPlayer = 0;
    static int direction = 1;
    static Card upCard = null;
    static String calledColor = "";
    static boolean quiet = false;
    static Random random = new Random();
    static Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {
        int bots = 3;
        int games = 1;
        boolean human = false;
        long seed = System.currentTimeMillis();

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
            } else if (args[i].equals("--self-test")) {
                selfTest();
                return;
            } else if (args[i].equals("--help")) {
                System.out.println("Usage: scripts/run.sh [--bots N] [--games N] [--human] [--quiet] [--seed N]");
                return;
            }
        }

        random = new Random(seed);
        setupPlayers(bots, human);

        if (playerNames.size() < 2 || playerNames.size() > 4) {
            System.out.println("UNO needs 2 to 4 players.");
            return;
        }

        for (int g = 1; g <= games; g++) {
            if (!quiet) {
                System.out.println("\n=== Game " + g + " ===");
            }
            playGame();
        }

        System.out.println("\nFinal scores:");
        for (int i = 0; i < playerNames.size(); i++) {
            System.out.println(playerNames.get(i) + ": " + scores[i]);
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
        deck.clear();
        String[] colors = {"R", "Y", "G", "B"};
        for (int c = 0; c < colors.length; c++) {
            deck.add(new Card(colors[c] + "0"));
            for (int n = 1; n <= 9; n++) {
                deck.add(new Card(colors[c] + n));
                deck.add(new Card(colors[c] + n));
            }
            deck.add(new Card(colors[c] + "S"));
            deck.add(new Card(colors[c] + "S"));
            deck.add(new Card(colors[c] + "R"));
            deck.add(new Card(colors[c] + "R"));
            deck.add(new Card(colors[c] + "+2"));
            deck.add(new Card(colors[c] + "+2"));
        }
        for (int i = 0; i < 4; i++) {
            deck.add(new Card("W"));
            deck.add(new Card("W4"));
        }
        Collections.shuffle(deck, random);
        discard.clear();
        for (int i = 0; i < hands.size(); i++) {
            hands.get(i).clear();
        }
        for (int i = 0; i < playerNames.size(); i++) {
            for (int j = 0; j < 7; j++) {
                hands.get(i).add(draw());
            }
        }
        upCard = draw();
        while (upCard.isWild()) {
            discard.add(upCard);
            upCard = draw();
        }
        calledColor = "";
        direction = 1;
        currentPlayer = random.nextInt(playerNames.size());

        int guard = 0;
        while (guard < 3000) {
            guard++;
            String name = playerNames.get(currentPlayer);
            ArrayList<Card> hand = hands.get(currentPlayer);

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
                Card drawn = draw();
                hand.add(drawn);
                if (!quiet) {
                    System.out.println(name + " draws " + drawn);
                }
                if (isLegal(drawn, upCard, calledColor)) {
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
                    if (!quiet) {
                        System.out.println(name + " selected an invalid index and draws a penalty card.");
                    }
                    hand.add(draw());
                    next();
                    continue;
                }

                Card card = hand.get(chosen);
                boolean ok = false;
                String cardColor = card.color();
                String upColor = upCard.color();
                Rank cardRank = card.rank();
                Rank upRank = upCard.rank();

                if (card.isWild()) {
                    ok = true;
                } else if (cardColor.equals(upColor)) {
                    ok = true;
                } else if (!calledColor.equals("") && cardColor.equals(calledColor)) {
                    ok = true;
                } else if (cardRank == upRank && cardRank != Rank.NUMBER) {
                    ok = true;
                } else if (cardRank == Rank.NUMBER && upRank == Rank.NUMBER && card.number() == upCard.number()) {
                    ok = true;
                }

                if (!ok) {
                    if (!quiet) {
                        System.out.println(name + " tried illegal card " + card + " and draws a penalty card.");
                    }
                    hand.add(draw());
                    next();
                    continue;
                }

                hand.remove(chosen);
                discard.add(upCard);
                upCard = card;
                calledColor = "";
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

                if (hand.size() == 1 && !quiet) {
                    System.out.println(name + " says UNO!");
                }

                if (hand.size() == 0) {
                    int points = 0;
                    for (int i = 0; i < hands.size(); i++) {
                        if (i != currentPlayer) {
                            for (int j = 0; j < hands.get(i).size(); j++) {
                                points += hands.get(i).get(j).points();
                            }
                        }
                    }
                    scores[currentPlayer] += points;
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
                    hands.get(currentPlayer).add(draw());
                    hands.get(currentPlayer).add(draw());
                    if (!quiet) {
                        System.out.println(playerNames.get(currentPlayer) + " draws two.");
                    }
                    next();
                } else if (card.rank() == Rank.WILD_DRAW_FOUR) {
                    next();
                    for (int i = 0; i < 4; i++) {
                        hands.get(currentPlayer).add(draw());
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
        if (!quiet) {
            System.out.println("Game stopped at safety limit.");
        }
    }

    static Card draw() {
        if (deck.size() == 0) {
            deck.addAll(discard);
            discard.clear();
            Collections.shuffle(deck, random);
        }
        if (deck.size() == 0) {
            return new Card("W");
        }
        return deck.remove(0);
    }

    static int chooseBotCard(ArrayList<Card> hand) {
        for (int i = 0; i < hand.size(); i++) {
            Card card = hand.get(i);
            boolean ok = false;
            if (card.isWild()) ok = true;
            else if (card.color().equals(upCard.color())) ok = true;
            else if (!calledColor.equals("") && card.color().equals(calledColor)) ok = true;
            else if (card.rank() == upCard.rank() && card.rank() != Rank.NUMBER) ok = true;
            else if (card.rank() == Rank.NUMBER && upCard.rank() == Rank.NUMBER && card.number() == upCard.number()) ok = true;
            if (card.rank() == Rank.DRAW_TWO && ok) {
                return i;
            }
        }
        for (int i = 0; i < hand.size(); i++) {
            Card card = hand.get(i);
            boolean ok = false;
            if (card.isWild()) ok = true;
            else if (card.color().equals(upCard.color())) ok = true;
            else if (!calledColor.equals("") && card.color().equals(calledColor)) ok = true;
            else if (card.rank() == upCard.rank() && card.rank() != Rank.NUMBER) ok = true;
            else if (card.rank() == Rank.NUMBER && upCard.rank() == Rank.NUMBER && card.number() == upCard.number()) ok = true;
            if (card.rank() == Rank.SKIP && ok) {
                return i;
            }
        }
        for (int i = 0; i < hand.size(); i++) {
            Card card = hand.get(i);
            boolean ok = false;
            if (card.isWild()) ok = true;
            else if (card.color().equals(upCard.color())) ok = true;
            else if (!calledColor.equals("") && card.color().equals(calledColor)) ok = true;
            else if (card.rank() == upCard.rank() && card.rank() != Rank.NUMBER) ok = true;
            else if (card.rank() == Rank.NUMBER && upCard.rank() == Rank.NUMBER && card.number() == upCard.number()) ok = true;
            if (card.rank() == Rank.NUMBER && ok) {
                return i;
            }
        }
        for (int i = 0; i < hand.size(); i++) {
            if (hand.get(i).isWild()) {
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
                    if (isLegal(hand.get(i), upCard, calledColor)) {
                        return i;
                    }
                    System.out.println("That card is not legal.");
                }
            }
            System.out.println("Card not found.");
        }
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

    static boolean isLegal(Card card, Card up, String call) {
        if (card.isWild()) {
            return true;
        }
        if (card.color().equals(up.color())) {
            return true;
        }
        if (!call.equals("") && card.color().equals(call)) {
            return true;
        }
        if (card.rank() == up.rank() && card.rank() != Rank.NUMBER) {
            return true;
        }
        if (card.rank() == Rank.NUMBER && up.rank() == Rank.NUMBER && card.number() == up.number()) {
            return true;
        }
        return false;
    }

    static void next() {
        currentPlayer += direction;
        if (currentPlayer >= playerNames.size()) {
            currentPlayer = 0;
        }
        if (currentPlayer < 0) {
            currentPlayer = playerNames.size() - 1;
        }
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

    static void selfTest() {
        int passed = 0;
        if (new Card("R5").color().equals("R")) passed++; else fail("color R5");
        if (new Card("G+2").rank() == Rank.DRAW_TWO) passed++; else fail("rank +2");
        if (new Card("W4").points() == 50) passed++; else fail("wild points");
        if (isLegal(new Card("R2"), new Card("R9"), "")) passed++; else fail("same color");
        if (isLegal(new Card("G9"), new Card("R9"), "")) passed++; else fail("same number");
        if (isLegal(new Card("B3"), new Card("W"), "B")) passed++; else fail("called color");
        if (!isLegal(new Card("B3"), new Card("R9"), "")) passed++; else fail("illegal mismatch");

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
