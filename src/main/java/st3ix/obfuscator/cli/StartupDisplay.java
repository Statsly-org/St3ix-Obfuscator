package st3ix.obfuscator.cli;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;

/**
 * Prints the startup banner and system information to stdout.
 */
public final class StartupDisplay {

    private static final String BANNER = """
         ____  _   _____ _      
        / ___|| |_|___ /(_)_  __
        \\___ \\| __| |_ \\| \\ \\/ /
         ___) | |_ ___) | |>  < 
        |____/ \\__|____/|_/_/\\_\\
        """;

    private StartupDisplay() {}

    /**
     * Prints the ASCII banner followed by system info.
     */
    public static void print() {
        System.out.println(BANNER);
        System.out.println("  Version: " + getVersion());
        printSystemInfo();
    }

    private static String getVersion() {
        String v = StartupDisplay.class.getPackage().getImplementationVersion();
        return v != null ? v : "unknown";
    }

    private static void printSystemInfo() {
        String os = System.getProperty("os.name") + " " + System.getProperty("os.version", "");
        String arch = System.getProperty("os.arch");
        String java = System.getProperty("java.version");
        String vendor = System.getProperty("java.vendor", "");
        long maxMem = Runtime.getRuntime().maxMemory() / (1024 * 1024);
        int processors = Runtime.getRuntime().availableProcessors();

        System.out.println("  OS: " + os.trim());
        System.out.println("  Arch: " + arch);
        System.out.println("  Java: " + java + (vendor.isEmpty() ? "" : " (" + vendor + ")"));
        System.out.println("  Heap: " + maxMem + " MB max");
        System.out.println("  CPUs: " + processors);
        System.out.println();
    }
}
