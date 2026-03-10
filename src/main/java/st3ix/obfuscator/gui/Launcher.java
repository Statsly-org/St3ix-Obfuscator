package st3ix.obfuscator.gui;

import st3ix.obfuscator.cli.CliRunner;

/**
 * Entry point for the obfuscator JAR.
 * When launched by double-click (no args) → GUI. When launched with CLI args → CLI.
 */
public final class Launcher {

    public static void main(String[] args) {
        if (isCliMode(args)) {
            CliRunner.main(args);
        } else {
            ObfuscatorGui.show();
        }
    }

    private static boolean isCliMode(String[] args) {
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if ("-i".equals(arg) || "--input".equals(arg)) return true;
            if ("-o".equals(arg) || "--output".equals(arg)) return true;
            if ("-h".equals(arg) || "--help".equals(arg)) return true;
        }
        return false;
    }
}
