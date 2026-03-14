package example;

/**
 * Demo class specifically designed to test flow obfuscation.
 *
 * Flow obfuscation flattens the control flow into a switch-dispatcher structure.
 * To be eligible, methods must be:
 * - Linear (no if/else, for, while, switch)
 * - Have at least 8 instructions (after other obfuscations)
 * - No try-catch blocks
 *
 * After obfuscation with flow enabled, decompile this class and look for
 * switch statements in the method bodies.
 */
public final class FlowObfuscationDemo {

    /**
     * Linear method: pure arithmetic, no branches.
     * Should produce a switch-based flattened structure when flow obfuscation is applied.
     */
    public int linearCompute(int a, int b) {
        int step1 = a * 2;
        int step2 = step1 + b;
        int step3 = step2 / 3;
        int step4 = step3 - 7;
        int step5 = step4 * 11;
        return step5;
    }

    /**
     * Another linear method with more instructions.
     */
    public String linearBuildString(int id) {
        String part1 = "id=";
        String part2 = String.valueOf(id);
        String part3 = "; processed";
        String result = part1 + part2 + part3;
        return result;
    }

    /**
     * Linear method that returns a computed value.
     */
    public long linearTimestamp(int base) {
        long t1 = base * 1000L;
        long t2 = t1 + 500L;
        long t3 = t2 / 2L;
        return t3;
    }

    /**
     * Entry point - call from Main to exercise these methods.
     */
    public void run() {
        int x = linearCompute(10, 20);
        String s = linearBuildString(x);
        long t = linearTimestamp(x);
        System.out.println("FlowObfuscationDemo: " + s + " ts=" + t);
    }
}
