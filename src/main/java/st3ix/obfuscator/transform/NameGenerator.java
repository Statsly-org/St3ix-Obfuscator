package st3ix.obfuscator.transform;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Generates unique identifiers for obfuscation.
 * Sequential mode: a, b, ..., z, aa, ab, ... (optionally with minimum length).
 * Random mode: random strings of configured length (e.g. xK9mP2).
 * Optional: homoglyphs (lookalike Unicode chars) and invisible (zero-width) chars for stronger obfuscation.
 */
public final class NameGenerator {

    private static final String CHARS = "abcdefghijklmnopqrstuvwxyz";
    /** Latin a-z -> Cyrillic lookalike (а,с,е,о,р,х,у); others unchanged */
    private static final String HOMOGLYPH_CHARS = "\u0430b\u0441d\u0435fghijklmn\u043E\u043Fqrstuvw\u0445\u0443z";
    /** Zero-width & narrow invisible chars: 200B–200F, 2060, 200A, 202F */
    private static final char[] INVISIBLE_CHARS = {
        '\u200B', /* Zero-Width Space */
        '\u200C', /* Zero Width Non-Joiner */
        '\u200D', /* Zero Width Joiner */
        '\u200E', /* Left-To-Right Mark */
        '\u200F', /* Right-To-Left Mark */
        '\u2060', /* Word Joiner */
        '\u200A', /* Hair Space */
        '\u202F', /* Narrow No-Break Space */
    };

    private final int minLength;
    private final boolean random;
    private final boolean useHomoglyph;
    private final boolean useInvisibleChars;
    private final Random rng;
    private final Set<String> usedRandom = new HashSet<>();
    private final List<String> generated = new ArrayList<>();
    private int index;

    /**
     * Creates a generator with default behavior (sequential, length 1, no homoglyph/invisible).
     */
    public NameGenerator() {
        this(false, 1, false, false);
    }

    /**
     * Creates a generator with the given mode and minimum length.
     *
     * @param random true for random names, false for sequential
     * @param minLength minimum name length (1–32). Sequential uses this as minimum; random uses exact length.
     */
    public NameGenerator(boolean random, int minLength) {
        this(random, minLength, false, false);
    }

    /**
     * Creates a generator with optional homoglyph and invisible-char obfuscation.
     *
     * @param random true for random names, false for sequential
     * @param minLength minimum name length (1–32)
     * @param useHomoglyph use lookalike chars (a→а, e→е, etc.). Sequential: fixed mapping; Random: varies per char
     * @param useInvisibleChars inject zero-width chars. Sequential: fixed append; Random: random position
     */
    public NameGenerator(boolean random, int minLength, boolean useHomoglyph, boolean useInvisibleChars) {
        this.random = random;
        this.minLength = Math.max(1, Math.min(32, minLength));
        this.useHomoglyph = useHomoglyph;
        this.useInvisibleChars = useInvisibleChars;
        this.rng = new Random();
    }

    /**
     * Returns the next unique name.
     */
    public String next() {
        String base = random ? nextRandom() : nextSequential();
        String name = applyHomoglyph(base);
        name = applyInvisibleChars(name);
        generated.add(name);
        return name;
    }

    private String nextSequential() {
        String name = generateSequential(index);
        index++;
        return name;
    }

    private String generateSequential(int n) {
        if (minLength == 1) {
            if (n < CHARS.length()) {
                return String.valueOf(CHARS.charAt(n));
            }
            int remainder = n;
            StringBuilder sb = new StringBuilder();
            while (remainder >= 0) {
                sb.insert(0, CHARS.charAt(remainder % CHARS.length()));
                remainder = remainder / CHARS.length() - 1;
            }
            return sb.toString();
        }
        long maxVal = (long) Math.pow(26, minLength);
        int digits = minLength;
        long num = n & 0xFFFFFFFFL;
        while (num >= maxVal) {
            num -= maxVal;
            digits++;
            long nextMax = (long) Math.pow(26, digits);
            if (nextMax > Integer.MAX_VALUE) break;
            maxVal = nextMax;
        }
        StringBuilder sb = new StringBuilder(digits);
        for (int i = 0; i < digits; i++) {
            sb.insert(0, CHARS.charAt((int) (num % 26)));
            num /= 26;
        }
        return sb.toString();
    }

    private String nextRandom() {
        for (int attempt = 0; attempt < 1000; attempt++) {
            StringBuilder sb = new StringBuilder(minLength);
            for (int i = 0; i < minLength; i++) {
                sb.append(CHARS.charAt(rng.nextInt(CHARS.length())));
            }
            String name = sb.toString();
            if (usedRandom.add(name)) {
                return name;
            }
        }
        String fallback = "n" + rng.nextInt(1_000_000);
        usedRandom.add(fallback);
        return fallback;
    }

    private String applyHomoglyph(String s) {
        if (!useHomoglyph) return s;
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            int idx = c - 'a';
            if (random && idx >= 0 && idx < HOMOGLYPH_CHARS.length()) {
                char homoglyph = HOMOGLYPH_CHARS.charAt(idx);
                if (homoglyph != c) {
                    sb.append(rng.nextBoolean() ? homoglyph : c);
                } else {
                    sb.append(c);
                }
            } else if (!random && idx >= 0 && idx < HOMOGLYPH_CHARS.length()) {
                sb.append(HOMOGLYPH_CHARS.charAt(idx));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private String applyInvisibleChars(String s) {
        if (!useInvisibleChars) return s;
        if (random) {
            int pos = rng.nextInt(s.length() + 1);
            char inv = INVISIBLE_CHARS[rng.nextInt(INVISIBLE_CHARS.length)];
            return s.substring(0, pos) + inv + s.substring(pos);
        } else {
            return s + INVISIBLE_CHARS[0];
        }
    }
}
