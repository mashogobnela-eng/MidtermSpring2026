import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;

/**
 * Characterization tests for the UNO CLI.
 *
 * These tests describe what the CURRENT implementation does, quirks included.
 * They are not tests for an ideal version of UNO. Their job is to lock the
 * observable behavior so the refactoring can be verified as behavior-preserving.
 *
 * Two layers:
 *   1. Unit-level checks against the rule/card/bot logic.
 *   2. End-to-end golden checks that run the real CLI (seeded, deterministic)
 *      in a child process and compare the full transcript byte-for-byte against
 *      a recording captured from the original code (tests/golden/*.txt).
 *
 * Run with: scripts/test.sh
 */
public class CharacterizationTest {

    static int passed = 0;
    static int failed = 0;

    public static void main(String[] args) throws Exception {
        matchByColor();
        matchByNumber();
        matchByActionType();
        wildAndWildDrawFour();
        calledColorAfterWild();
        illegalMismatch();
        cardParsing();
        scoringPoints();
        botCardPriority();
        botNeverPlaysReverseFromHand_surprise();
        botColorChoice();
        endToEndGoldens();

        System.out.println("Characterization: " + passed + " passed, " + failed + " failed.");
        if (failed > 0) {
            System.exit(1);
        }
    }

    // --- Required behavior: matching by color ---------------------------------
    static void matchByColor() {
        eq("color match: R2 on R9", true, Main.isLegal(c("R2"), c("R9"), ""));
        eq("color match: B8 on B3", true, Main.isLegal(c("B8"), c("B3"), ""));
        eq("color mismatch alone is not enough: B3 on R9", false, Main.isLegal(c("B3"), c("R9"), ""));
    }

    // --- Required behavior: matching by number --------------------------------
    static void matchByNumber() {
        eq("number match across colors: G9 on R9", true, Main.isLegal(c("G9"), c("R9"), ""));
        eq("number match: B0 on G0", true, Main.isLegal(c("B0"), c("G0"), ""));
        eq("different number, different color is illegal: G8 on R9", false, Main.isLegal(c("G8"), c("R9"), ""));
    }

    // --- Required behavior: matching by action type ---------------------------
    static void matchByActionType() {
        eq("skip on skip (diff color): BS on RS", true, Main.isLegal(c("BS"), c("RS"), ""));
        eq("reverse on reverse (diff color): YR on GR", true, Main.isLegal(c("YR"), c("GR"), ""));
        eq("draw-two on draw-two (diff color): R+2 on G+2", true, Main.isLegal(c("R+2"), c("G+2"), ""));
        // Quirk: an action card does NOT match a number card, even of "matching" value.
        eq("skip does not match a number: BS on R9", false, Main.isLegal(c("BS"), c("R9"), ""));
    }

    // --- Required behavior: wild and wild draw four ---------------------------
    static void wildAndWildDrawFour() {
        eq("wild is always legal: W on R9", true, Main.isLegal(c("W"), c("R9"), ""));
        eq("wild draw four is always legal: W4 on G+2", true, Main.isLegal(c("W4"), c("G+2"), ""));
        eq("wild is legal even over a called color: W on (W called B)", true, Main.isLegal(c("W"), c("W"), "B"));
    }

    // --- Required behavior: color called after a wild -------------------------
    static void calledColorAfterWild() {
        eq("called color matches: B3 on wild called B", true, Main.isLegal(c("B3"), c("W"), "B"));
        eq("wrong color vs called color is illegal: R3 on wild called B", false, Main.isLegal(c("R3"), c("W"), "B"));
    }

    static void illegalMismatch() {
        eq("plain mismatch is illegal: B3 on R9", false, Main.isLegal(c("B3"), c("R9"), ""));
    }

    // --- Card parsing characterization (color / rank / number) ----------------
    static void cardParsing() {
        eq("color of R5", "R", c("R5").color());
        eq("color of YS", "Y", c("YS").color());
        eq("color of G+2", "G", c("G+2").color());
        eq("color of BR", "B", c("BR").color());
        eq("color of wild is empty", "", c("W").color());
        eq("color of wild four is empty", "", c("W4").color());

        eq("rank of R5 is NUMBER", Rank.NUMBER, c("R5").rank());
        eq("rank of YS is SKIP", Rank.SKIP, c("YS").rank());
        eq("rank of BR is REVERSE", Rank.REVERSE, c("BR").rank());
        eq("rank of G+2 is DRAW_TWO", Rank.DRAW_TWO, c("G+2").rank());
        eq("rank of W is WILD", Rank.WILD, c("W").rank());
        eq("rank of W4 is WILD_DRAW_FOUR", Rank.WILD_DRAW_FOUR, c("W4").rank());

        eq("number of R5", 5, c("R5").number());
        eq("number of B0", 0, c("B0").number());
        eq("number of non-number is -1", -1, c("YS").number());
    }

    // --- Required behavior: scoring -------------------------------------------
    static void scoringPoints() {
        eq("number card scores face value", 5, c("R5").points());
        eq("zero card scores zero", 0, c("B0").points());
        eq("skip scores 20", 20, c("YS").points());
        eq("reverse scores 20", 20, c("BR").points());
        eq("draw-two scores 20", 20, c("G+2").points());
        eq("wild scores 50", 50, c("W").points());
        eq("wild draw four scores 50", 50, c("W4").points());
    }

    // --- Bot strategy characterization ----------------------------------------
    static void botCardPriority() {
        // Up card R9, no called color.
        Main.upCard = c("R9");
        Main.calledColor = "";

        // Draw-two is preferred over a legal number.
        eq("bot prefers draw-two over number", 1,
                Main.chooseBotCard(hand("R4", "R+2", "R5")));

        // Skip is preferred over a legal number (no draw-two present).
        eq("bot prefers skip over number", 1,
                Main.chooseBotCard(hand("R4", "RS")));

        // With no draw-two/skip, a legal number is played before a wild.
        eq("bot plays number before wild", 1,
                Main.chooseBotCard(hand("B3", "R4", "W")));

        // Wild is the last resort when nothing else is legal.
        eq("bot plays wild as last resort", 1,
                Main.chooseBotCard(hand("B3", "W")));
    }

    // --- At least one edge case that surprised me -----------------------------
    // The bot's card selection scans only for draw-two, then skip, then number,
    // then any wild. A REVERSE is never selected from the hand: the bot draws
    // instead, even though a same-color reverse is a perfectly legal play.
    static void botNeverPlaysReverseFromHand_surprise() {
        Main.upCard = c("R9");   // RR (red reverse) is legal here by color.
        Main.calledColor = "";
        eq("SURPRISE: bot ignores a legal reverse and draws (-1)", -1,
                Main.chooseBotCard(hand("RR")));
        // Sanity: the same reverse IS considered legal by the rules.
        eq("the ignored reverse really is legal", true, Main.isLegal(c("RR"), c("R9"), ""));
    }

    static void botColorChoice() {
        eq("bot calls its majority color", "B", Main.chooseBotColor(hand("B1", "B2", "R3")));
        // Tie-break order is R >= Y >= G >= B, so a tie resolves to red.
        eq("bot color tie resolves to R", "R", Main.chooseBotColor(hand("R1", "B2")));
        eq("bot color with no colored cards resolves to R", "R", Main.chooseBotColor(hand("W", "W4")));
    }

    // --- End-to-end golden transcripts (deterministic, seeded) ----------------
    static void endToEndGoldens() throws Exception {
        // Full transcripts that end in a win and exercise skip, reverse
        // (including the 2-player reverse-as-skip case), draw two, wild,
        // wild draw four, drawing, bot auto-play of drawn cards, and scoring.
        assertGolden("tests/golden/g_2bots_seed17.txt",
                "--bots", "2", "--games", "1", "--seed", "17");
        assertGolden("tests/golden/g_2bots_seed42.txt",
                "--bots", "2", "--games", "1", "--seed", "42");

        // Quiet final-scores goldens, including stalls that reach the 3000-turn
        // safety limit (a real quirk of the current bot strategy).
        assertGolden("tests/golden/g_quiet_2bots_seed1.txt",
                "--bots", "2", "--games", "1", "--seed", "1", "--quiet");
        assertGolden("tests/golden/g_quiet_3bots_seed42.txt",
                "--bots", "3", "--games", "1", "--seed", "42", "--quiet");
        assertGolden("tests/golden/g_quiet_4bots_seed7.txt",
                "--bots", "4", "--games", "1", "--seed", "7", "--quiet");
    }

    // --- helpers --------------------------------------------------------------

    static Card c(String code) {
        return new Card(code);
    }

    static ArrayList<Card> hand(String... cards) {
        ArrayList<Card> h = new ArrayList<Card>();
        for (String code : cards) {
            h.add(new Card(code));
        }
        return h;
    }

    static void assertGolden(String goldenPath, String... gameArgs) throws Exception {
        String expected = new String(Files.readAllBytes(Paths.get(goldenPath)), StandardCharsets.UTF_8);
        String actual = runGame(gameArgs);
        if (expected.equals(actual)) {
            passed++;
        } else {
            failed++;
            System.out.println("FAIL golden: " + goldenPath);
            System.out.println(firstDifference(expected, actual));
        }
    }

    /** Runs the real CLI in a child process so the entry point is tested as the grader runs it. */
    static String runGame(String... gameArgs) throws Exception {
        ArrayList<String> cmd = new ArrayList<String>();
        cmd.add(javaBin());
        cmd.add("-cp");
        cmd.add("out");
        cmd.add("Main");
        for (String a : gameArgs) {
            cmd.add(a);
        }
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(true);
        Process p = pb.start();
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        try (InputStream in = p.getInputStream()) {
            byte[] chunk = new byte[8192];
            int n;
            while ((n = in.read(chunk)) != -1) {
                buf.write(chunk, 0, n);
            }
        }
        p.waitFor();
        return new String(buf.toByteArray(), StandardCharsets.UTF_8);
    }

    static String javaBin() {
        String home = System.getProperty("java.home");
        if (home != null && !home.isEmpty()) {
            return home + "/bin/java";
        }
        return "java";
    }

    static String firstDifference(String expected, String actual) {
        int min = Math.min(expected.length(), actual.length());
        int i = 0;
        while (i < min && expected.charAt(i) == actual.charAt(i)) {
            i++;
        }
        int from = Math.max(0, i - 40);
        int to = Math.min(min, i + 40);
        return "  first difference at index " + i + " (len exp=" + expected.length()
                + " act=" + actual.length() + ")\n"
                + "  expected: ..." + escape(expected.substring(from, Math.min(expected.length(), to))) + "...\n"
                + "  actual:   ..." + escape(actual.substring(from, Math.min(actual.length(), to))) + "...";
    }

    static String escape(String s) {
        return s.replace("\n", "\\n");
    }

    static void eq(String name, Object expected, Object actual) {
        boolean ok = expected == null ? actual == null : expected.equals(actual);
        if (ok) {
            passed++;
        } else {
            failed++;
            System.out.println("FAIL: " + name + " (expected=" + expected + " actual=" + actual + ")");
        }
    }
}
