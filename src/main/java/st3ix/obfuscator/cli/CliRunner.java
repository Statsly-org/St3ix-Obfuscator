package st3ix.obfuscator.cli;

import st3ix.obfuscator.config.ConfigLoader;
import st3ix.obfuscator.config.ObfuscatorConfig;
import st3ix.obfuscator.core.ObfuscationPipeline;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Command-line interface entry point. Parses arguments and delegates to the obfuscation pipeline.
 */
public final class CliRunner {

    public static void main(String[] args) {
        CliRunner runner = new CliRunner();
        runner.run(args);
    }

    public void run(String[] args) {
        StartupDisplay.print();
        CliArgs parsed = parseArgs(args);
        if (parsed.help) {
            printUsage();
            return;
        }
        if (parsed.inputPath == null || parsed.outputPath == null) {
            System.err.println("Missing required arguments. Use --help for usage.");
            System.exit(1);
        }
        try {
            ObfuscatorConfig config = ConfigLoader.load();
            ObfuscationPipeline pipeline = new ObfuscationPipeline();
            pipeline.run(Path.of(parsed.inputPath), Path.of(parsed.outputPath), config);
            System.out.println("Done. Output: " + parsed.outputPath);
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            System.exit(1);
        }
    }

    private CliArgs parseArgs(String[] args) {
        CliArgs result = new CliArgs();
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            switch (arg) {
                case "--help", "-h" -> result.help = true;
                case "--input", "-i" -> result.inputPath = nextArg(args, i++);
                case "--output", "-o" -> result.outputPath = nextArg(args, i++);
                case "--max-ram" -> {
                    String val = nextArg(args, i++);
                    if (val != null) result.maxRamMb = parseRamMb(val);
                }
                default -> {
                    if (!arg.startsWith("-")) result.extra.add(arg);
                }
            }
        }
        return result;
    }

    private String nextArg(String[] args, int i) {
        return i + 1 < args.length ? args[i + 1] : null;
    }

    private int parseRamMb(String val) {
        val = val.trim().toLowerCase();
        int mult = 1;
        if (val.endsWith("g")) {
            mult = 1024;
            val = val.substring(0, val.length() - 1);
        } else if (val.endsWith("m") || val.endsWith("mb")) {
            val = val.replaceAll("mb$", "").replaceAll("m$", "");
        } else if (val.endsWith("k")) {
            mult = 1 / 1024;
            val = val.substring(0, val.length() - 1);
        }
        try {
            return Math.max(0, (int) (Integer.parseInt(val) * mult));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private void printUsage() {
        System.out.println("""
            St3ix Obfuscator - Java bytecode obfuscator

            Usage:
              java -Xmx512m -jar st3ix-obfuscator.jar [options] --input <path> --output <path>

            Options:
              -i, --input <path>    Input JAR or directory
              -o, --output <path>   Output JAR or directory
              --max-ram <size>      Max heap hint (e.g. 512m, 2g). Pass -Xmx to JVM for actual limit.
              -h, --help            Show this message

            JVM heap (actual limit):
              java -Xmx512m -jar st3ix-obfuscator.jar ...
              java -Xmx2g -jar st3ix-obfuscator.jar ...
            """);
    }

    private static final class CliArgs {
        boolean help;
        String inputPath;
        String outputPath;
        int maxRamMb;
        List<String> extra = new ArrayList<>();
    }
}
