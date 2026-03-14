package example;

/**
 * Demo service with various obfuscation targets.
 */
public final class DemoService {

    private static final String SECRET_KEY = "my-secret-key-12345";
    private int counter;

    public void run() {
        counter++;
        String message = buildMessage("Hello", "World");
        System.out.println(message);
        int hash = computeHash(42);
        System.out.println("Hash(42)=" + hash);
        processData(42, true);
        printMagicNumbers();
        printArrayDemo();
        printBooleanDemo();
    }

    /** Boolean obfuscation target: true/false literals – obfuscated in bytecode */
    private void printBooleanDemo() {
        boolean a = true;
        boolean b = false;
        if (a && !b) {
            System.out.println("Boolean obfuscation OK: " + a + ", " + b);
        }
    }

    private String buildMessage(String a, String b) {
        return a + " " + b + " (count: " + counter + ")";
    }

    private void processData(int value, boolean flag) {
        if (flag) {
            System.out.println("Processed: " + (value * 2));
        }
    }

    /** Obfuscation targets: 100, 256, 1337 - visible in decompiled bytecode before obfuscation */
    private void printMagicNumbers() {
        int port = 25565;
        int seed = 12345;
        int threshold = 1000;
        System.out.println("Numbers (obfuscated in bytecode): port=" + port + ", seed=" + seed + ", threshold=" + threshold);
    }

    /** Array obfuscation target: new int[8], new String[3] - dimensions obfuscated in bytecode */
    public void printArrayDemo() {
        int[] sizes = new int[8];
        String[] labels = new String[3];
        for (int i = 0; i < sizes.length; i++) sizes[i] = i * 10;
        labels[0] = "a";
        labels[1] = "b";
        labels[2] = "c";
        System.out.println("Array (obfuscated dims): len=" + sizes.length + ", labels=" + labels.length);
    }

    public String getSecretKey() {
        return SECRET_KEY;
    }

    /**
     * Linear method for flow obfuscation demo: pure arithmetic chain.
     */
    public int computeHash(int x) {
        int a = x * 31;
        int b = a + 17;
        int c = b / 7;
        int d = c - 3;
        return d * 11;
    }
}
