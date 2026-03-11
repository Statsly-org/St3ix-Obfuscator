package example;

/**
 * Demo class to visually verify debug info stripping.
 *
 * Before obfuscation with debug stripping:
 *   - Decompiler shows this source file name (e.g. DebugStrippingDemo.java:42)
 *   - Decompiler shows real line numbers
 *   - Local variables: userName, userId, processedCount, result, etc.
 *
 * After obfuscation WITH debug stripping enabled:
 *   - Source file / line numbers gone or generic
 *   - Local variables become: var0, var1, var2, var3...
 */
public final class DebugStrippingDemo {

    public void demonstrateLocalVariables() {
        String userName = "Alice";
        int userId = 10042;
        long processedCount = 999_999L;
        boolean isActive = true;
        double score = 3.14159;

        String result = userName + " (#" + userId + ") processed=" + processedCount;
        if (isActive && score > 0) {
            System.out.println(result);
        }
    }

    public int calculateWithManyLocals(int input) {
        int firstStep = input * 2;
        int secondStep = firstStep + 100;
        int thirdStep = secondStep / 3;
        int finalResult = thirdStep - 7;

        return finalResult;
    }
}
