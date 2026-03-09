package st3ix.obfuscator.config;

import java.util.Collections;
import java.util.List;

/**
 * Configuration for obfuscation runs. Loaded from config.yml next to the JAR.
 */
public record ObfuscatorConfig(
    boolean classRenamingEnabled,
    boolean numberObfuscationEnabled,
    boolean arrayObfuscationEnabled,
    boolean classNamesRandom,
    int classNameLength,
    boolean numberKeyRandom,
    boolean arrayKeyRandom,
    List<String> excludeClasses
) {
    private static final int DEFAULT_CLASS_NAME_LENGTH = 6;
    private static final int MIN_CLASS_NAME_LENGTH = 1;
    private static final int MAX_CLASS_NAME_LENGTH = 32;

    public ObfuscatorConfig {
        excludeClasses = excludeClasses != null ? List.copyOf(excludeClasses) : Collections.emptyList();
        classNameLength = Math.max(MIN_CLASS_NAME_LENGTH, Math.min(MAX_CLASS_NAME_LENGTH, classNameLength));
    }

    public static ObfuscatorConfig defaults() {
        return new ObfuscatorConfig(true, true, true, false, DEFAULT_CLASS_NAME_LENGTH, false, false, List.of());
    }
}
