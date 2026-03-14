package example;

/**
 * Entry point for the example project.
 */
public final class Main {

    public static void main(String[] args) {
        System.out.println("Example project running.");
        DemoService service = new DemoService();
        service.run();
        new DebugStrippingDemo().demonstrateLocalVariables();
        new ObfuscationVerifyDemo().verify();
        new FlowObfuscationDemo().run();
        CrossRefTest crossRef = new CrossRefTest();
        crossRef.run();
    }
}
