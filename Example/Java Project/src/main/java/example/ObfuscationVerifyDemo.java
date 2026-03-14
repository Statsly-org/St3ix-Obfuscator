package example;

/**
 * Verification demo for obfuscation. Use this to confirm that obfuscation works:
 *
 * 1. Build & obfuscate the Example project
 * 2. Decompile the output JAR (e.g. with CFR, Fernflower, JD-GUI)
 * 3. Open this class and check:
 *
 * NUMBER OBFUSCATION:     port=25565, seed=12345, threshold=1000
 *    → Should become:      int n = 0x???? ^ 0x????; etc.
 *
 * DEBUG STRIPPING:        Local vars port, seed, threshold
 *    → Should become:     n, n2, n3 (or var0, var1, var2)
 *
 * FIELD RENAMING:         apiKey field
 *    → Should become:     Short name like a, b, f
 *
 * Run with: java -jar .../example-java-project-obfuscated.jar ObfuscationVerifyDemo
 */
public final class ObfuscationVerifyDemo {

    private static final String apiKey = "verify-field-renaming";

    public static void main(String[] args) {
        ObfuscationVerifyDemo demo = new ObfuscationVerifyDemo();
        demo.verify();
    }

    public void verify() {
        int port = 25565;
        int seed = 12345;
        int threshold = 1000;
        int sum = port + seed + threshold;

        System.out.println("ObfuscationVerifyDemo: port=" + port + ", seed=" + seed + ", threshold=" + threshold);
        System.out.println("Sum=" + sum);
        System.out.println("Field (apiKey): " + apiKey);
    }
}
