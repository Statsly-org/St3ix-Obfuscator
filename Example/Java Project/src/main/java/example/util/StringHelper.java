package example.util;

/**
 * Utility for string operations.
 */
public final class StringHelper {

    private StringHelper() {}

    public static String reverse(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        return new StringBuilder(input).reverse().toString();
    }

    public static boolean containsDigit(String s) {
        for (int i = 0; i < s.length(); i++) {
            if (Character.isDigit(s.charAt(i))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Linear method for flow obfuscation demo: formats id as "id=N;" without branches.
     */
    public static String formatIdLinear(int id) {
        String prefix = "id=";
        String num = String.valueOf(id);
        String suffix = ";";
        return prefix + num + suffix;
    }
}
