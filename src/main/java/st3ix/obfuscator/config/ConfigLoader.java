package st3ix.obfuscator.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Loads config.yml from the directory containing the running JAR.
 */
public final class ConfigLoader {

    private static final String CONFIG_FILE = "config.yml";

    /**
     * Loads config from the JAR directory, or returns defaults if not found.
     */
    @SuppressWarnings("unchecked")
    public static ObfuscatorConfig load() {
        return loadWithPath().config();
    }

    /**
     * Loads config and returns it with the source path (null if defaults).
     */
    @SuppressWarnings("unchecked")
    public static LoadResult loadWithPath() {
        Path configPath = findConfigPath();
        if (configPath == null || !Files.isRegularFile(configPath)) {
            return new LoadResult(ObfuscatorConfig.defaults(), null);
        }
        try {
            Map<String, Object> raw = loadYaml(configPath);
            if (raw == null) return new LoadResult(ObfuscatorConfig.defaults(), null);

            boolean classRenamingEnabled = getBoolean(raw, "classRenamingEnabled", true);
            boolean methodRenamingEnabled = getBoolean(raw, "methodRenamingEnabled", true);
            boolean numberObfuscationEnabled = getBoolean(raw, "numberObfuscationEnabled", true);
            boolean arrayObfuscationEnabled = getBoolean(raw, "arrayObfuscationEnabled", true);
            boolean booleanObfuscationEnabled = getBoolean(raw, "booleanObfuscationEnabled", true);
            boolean stringObfuscationEnabled = getBoolean(raw, "stringObfuscationEnabled", true);
            boolean debugInfoStrippingEnabled = getBoolean(raw, "debugInfoStrippingEnabled", true);
            boolean classNamesRandom = getBoolean(raw, "classNamesRandom", false);
            int classNameLength = getInt(raw, "classNameLength", 6);
            boolean classNamesHomoglyph = getBoolean(raw, "classNamesHomoglyph", false);
            boolean classNamesInvisibleChars = getBoolean(raw, "classNamesInvisibleChars", false);
            boolean methodNamesRandom = getBoolean(raw, "methodNamesRandom", false);
            int methodNameLength = getInt(raw, "methodNameLength", 4);
            boolean methodNamesHomoglyph = getBoolean(raw, "methodNamesHomoglyph", false);
            boolean methodNamesInvisibleChars = getBoolean(raw, "methodNamesInvisibleChars", false);
            boolean numberKeyRandom = getBoolean(raw, "numberKeyRandom", false);
            boolean arrayKeyRandom = getBoolean(raw, "arrayKeyRandom", false);
            boolean booleanKeyRandom = getBoolean(raw, "booleanKeyRandom", false);
            boolean stringKeyRandom = getBoolean(raw, "stringKeyRandom", false);
            List<String> excludeClasses = getStringList(raw, "excludeClasses");

            return new LoadResult(new ObfuscatorConfig(
                classRenamingEnabled, methodRenamingEnabled, numberObfuscationEnabled, arrayObfuscationEnabled,
                booleanObfuscationEnabled, stringObfuscationEnabled, debugInfoStrippingEnabled,
                classNamesRandom, classNameLength, classNamesHomoglyph, classNamesInvisibleChars,
                methodNamesRandom, methodNameLength, methodNamesHomoglyph, methodNamesInvisibleChars,
                numberKeyRandom, arrayKeyRandom, booleanKeyRandom, stringKeyRandom, excludeClasses
            ), configPath);
        } catch (Exception e) {
            return new LoadResult(ObfuscatorConfig.defaults(), null);
        }
    }

    public record LoadResult(ObfuscatorConfig config, Path configPath) {}

    /**
     * Loads config from a specific path. Throws on I/O or parse errors.
     */
    @SuppressWarnings("unchecked")
    public static LoadResult loadFrom(Path configPath) throws IOException {
        if (configPath == null || !Files.isRegularFile(configPath)) {
            throw new IOException("Config file not found: " + configPath);
        }
        Map<String, Object> raw = loadYaml(configPath);
        if (raw == null) {
            throw new IOException("Config file is empty or invalid YAML format.");
        }
        try {
            boolean classRenamingEnabled = getBoolean(raw, "classRenamingEnabled", true);
            boolean methodRenamingEnabled = getBoolean(raw, "methodRenamingEnabled", true);
            boolean numberObfuscationEnabled = getBoolean(raw, "numberObfuscationEnabled", true);
            boolean arrayObfuscationEnabled = getBoolean(raw, "arrayObfuscationEnabled", true);
            boolean booleanObfuscationEnabled = getBoolean(raw, "booleanObfuscationEnabled", true);
            boolean stringObfuscationEnabled = getBoolean(raw, "stringObfuscationEnabled", true);
            boolean debugInfoStrippingEnabled = getBoolean(raw, "debugInfoStrippingEnabled", true);
            boolean classNamesRandom = getBoolean(raw, "classNamesRandom", false);
            int classNameLength = getInt(raw, "classNameLength", 6);
            boolean classNamesHomoglyph = getBoolean(raw, "classNamesHomoglyph", false);
            boolean classNamesInvisibleChars = getBoolean(raw, "classNamesInvisibleChars", false);
            boolean methodNamesRandom = getBoolean(raw, "methodNamesRandom", false);
            int methodNameLength = getInt(raw, "methodNameLength", 4);
            boolean methodNamesHomoglyph = getBoolean(raw, "methodNamesHomoglyph", false);
            boolean methodNamesInvisibleChars = getBoolean(raw, "methodNamesInvisibleChars", false);
            boolean numberKeyRandom = getBoolean(raw, "numberKeyRandom", false);
            boolean arrayKeyRandom = getBoolean(raw, "arrayKeyRandom", false);
            boolean booleanKeyRandom = getBoolean(raw, "booleanKeyRandom", false);
            boolean stringKeyRandom = getBoolean(raw, "stringKeyRandom", false);
            List<String> excludeClasses = getStringList(raw, "excludeClasses");
            return new LoadResult(new ObfuscatorConfig(
                classRenamingEnabled, methodRenamingEnabled, numberObfuscationEnabled, arrayObfuscationEnabled,
                booleanObfuscationEnabled, stringObfuscationEnabled, debugInfoStrippingEnabled,
                classNamesRandom, classNameLength, classNamesHomoglyph, classNamesInvisibleChars,
                methodNamesRandom, methodNameLength, methodNamesHomoglyph, methodNamesInvisibleChars,
                numberKeyRandom, arrayKeyRandom, booleanKeyRandom, stringKeyRandom, excludeClasses
            ), configPath);
        } catch (Exception e) {
            throw new IOException("Config format invalid: " + e.getMessage());
        }
    }

    /**
     * Returns the directory containing the running JAR, or current directory as fallback.
     */
    public static Path getJarDirectory() {
        try {
            var uri = ConfigLoader.class.getProtectionDomain()
                .getCodeSource().getLocation().toURI();
            Path jarPath = Path.of(uri);
            if (jarPath.toString().endsWith(".jar")) {
                Path parent = jarPath.getParent();
                if (parent != null) return parent;
            }
        } catch (Exception ignored) {}
        return Path.of(".");
    }

    private static Path findConfigPath() {
        try {
            var uri = ConfigLoader.class.getProtectionDomain()
                .getCodeSource().getLocation().toURI();
            Path jarPath = Path.of(uri);
            if (jarPath.toString().endsWith(".jar")) {
                Path parent = jarPath.getParent();
                if (parent != null) {
                    return parent.resolve(CONFIG_FILE);
                }
            }
        } catch (Exception ignored) {}
        return Path.of(CONFIG_FILE);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> loadYaml(Path path) throws IOException {
        try (InputStream in = Files.newInputStream(path)) {
            return new org.yaml.snakeyaml.Yaml().load(in);
        }
    }

    @SuppressWarnings("unchecked")
    private static boolean getBoolean(Map<String, Object> map, String key, boolean def) {
        Object v = map.get(key);
        if (v == null) return def;
        if (v instanceof Boolean b) return b;
        if (v instanceof String s) return Boolean.parseBoolean(s);
        return def;
    }

    @SuppressWarnings("unchecked")
    private static int getInt(Map<String, Object> map, String key, int def) {
        Object v = map.get(key);
        if (v == null) return def;
        if (v instanceof Number n) return n.intValue();
        if (v instanceof String s) {
            try { return Integer.parseInt(s.trim()); } catch (NumberFormatException e) { return def; }
        }
        return def;
    }

    @SuppressWarnings("unchecked")
    private static List<String> getStringList(Map<String, Object> map, String key) {
        Object v = map.get(key);
        if (v == null) return List.of();
        if (v instanceof List<?> list) {
            List<String> result = new ArrayList<>();
            for (Object item : list) {
                if (item != null) result.add(item.toString());
            }
            return result;
        }
        return List.of();
    }
}
