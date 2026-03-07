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
    }

    private String buildMessage(String a, String b) {
        return a + " " + b + " (count: " + counter + ")";
    }

    private void processData(int value, boolean flag) {
        if (flag) {
            System.out.println("Processed: " + (value * 2));
        }
    }

    public String getSecretKey() {
        return SECRET_KEY;
    }
}
