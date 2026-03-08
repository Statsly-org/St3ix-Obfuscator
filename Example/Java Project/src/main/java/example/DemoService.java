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
        processData(42, true);
        printMagicNumbers();
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

    public String getSecretKey() {
        return SECRET_KEY;
    }
}
