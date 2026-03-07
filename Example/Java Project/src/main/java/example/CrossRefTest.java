package example;

import example.model.User;
import example.util.StringHelper;

/**
 * Explicitly uses multiple other classes to verify cross-file reference obfuscation.
 * Main -> CrossRefTest -> DemoService, StringHelper, User
 */
public final class CrossRefTest {

    private final DemoService service;
    private final User user;

    public CrossRefTest() {
        this.service = new DemoService();
        this.user = new User("test-user", 42);
    }

    public void run() {
        service.run();
        String reversed = StringHelper.reverse("cross-ref");
        System.out.println("Reversed: " + reversed);
        System.out.println("User: " + user);
    }
}
