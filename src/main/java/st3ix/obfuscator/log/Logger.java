package st3ix.obfuscator.log;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;

/**
 * Colored console logger with timestamp and level.
 * Supports optional GUI output redirection.
 */
public final class Logger {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final boolean COLOR_ENABLED = System.getenv("NO_COLOR") == null;

    private static volatile Consumer<String> guiOutput;

    private Logger() {}

    /** Redirect log lines to GUI. Call with null to restore console only. */
    public static void setGuiOutput(Consumer<String> consumer) {
        guiOutput = consumer;
    }

    public static void info(String message) {
        log("INFO", message, "\033[32m");
    }

    public static void info(String format, Object... args) {
        log("INFO", String.format(format, args), "\033[32m");
    }

    public static void step(String message) {
        log("STEP", message, "\033[36m");
    }

    public static void step(String format, Object... args) {
        log("STEP", String.format(format, args), "\033[36m");
    }

    public static void success(String message) {
        log("OK", message, "\033[92m");
    }

    public static void success(String format, Object... args) {
        log("OK", String.format(format, args), "\033[92m");
    }

    public static void warn(String message) {
        log("WARN", message, "\033[33m");
    }

    public static void error(String message) {
        log("ERROR", message, "\033[31m");
    }

    private static void log(String level, String message, String color) {
        String time = LocalDateTime.now().format(TIME_FMT);
        String plainLine = "[" + level + "] " + time + " " + message;
        if (guiOutput != null) {
            guiOutput.accept(plainLine);
        }
        String reset = COLOR_ENABLED ? "\033[0m" : "";
        String c = COLOR_ENABLED ? color : "";
        System.out.println(c + plainLine + reset);
    }
}
