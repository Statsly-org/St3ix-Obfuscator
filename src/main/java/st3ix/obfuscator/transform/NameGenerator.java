package st3ix.obfuscator.transform;

import java.util.ArrayList;
import java.util.List;

/**
 * Generates short, unique identifiers for obfuscation (a, b, ..., z, aa, ab, ...).
 */
public final class NameGenerator {

    private static final String CHARS = "abcdefghijklmnopqrstuvwxyz";
    private final List<String> generated = new ArrayList<>();
    private int index;

    /**
     * Returns the next unique short name.
     */
    public String next() {
        String name = generate(index);
        generated.add(name);
        index++;
        return name;
    }

    private String generate(int n) {
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
}
