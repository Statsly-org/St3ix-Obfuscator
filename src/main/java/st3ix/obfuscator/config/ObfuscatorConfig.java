package st3ix.obfuscator.config;

import java.util.Collections;
import java.util.List;

/**
 * Configuration for obfuscation runs. Loaded from config.yml next to the JAR.
 */
public record ObfuscatorConfig(
    boolean classRenamingEnabled,
    List<String> excludeClasses
) {
    public ObfuscatorConfig {
        excludeClasses = excludeClasses != null ? List.copyOf(excludeClasses) : Collections.emptyList();
    }

    public static ObfuscatorConfig defaults() {
        return new ObfuscatorConfig(true, List.of());
    }
}
