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
        Path configPath = findConfigPath();
        if (configPath == null || !Files.isRegularFile(configPath)) {
            return ObfuscatorConfig.defaults();
        }
        try {
            Map<String, Object> raw = loadYaml(configPath);
            if (raw == null) return ObfuscatorConfig.defaults();

            boolean classRenamingEnabled = getBoolean(raw, "classRenamingEnabled", true);
            List<String> excludeClasses = getStringList(raw, "excludeClasses");

            return new ObfuscatorConfig(classRenamingEnabled, excludeClasses);
        } catch (Exception e) {
            return ObfuscatorConfig.defaults();
        }
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
