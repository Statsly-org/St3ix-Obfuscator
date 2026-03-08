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
 */
public final class NameGenerator {

    private static final String CHARS = "abcdefghijklmnopqrstuvwxyz";
    private final int minLength;
    private final boolean random;
    private final Random rng;
    private final Set<String> usedRandom = new HashSet<>();
    private final List<String> generated = new ArrayList<>();
    private int index;

    /**
     * Creates a generator with default behavior (sequential, length 1).
     */
    public NameGenerator() {
        this(false, 1);
    }

    /**
     * Creates a generator with the given mode and minimum length.
     *
     * @param random true for random names, false for sequential
     * @param minLength minimum name length (1–32). Sequential uses this as minimum; random uses exact length.
     */
    public NameGenerator(boolean random, int minLength) {
        this.random = random;
        this.minLength = Math.max(1, Math.min(32, minLength));
        this.rng = new Random();
    }

    /**
     * Returns the next unique name.
     */
    public String next() {
        String name = random ? nextRandom() : nextSequential();
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
}
