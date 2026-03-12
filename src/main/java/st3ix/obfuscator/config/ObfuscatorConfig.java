package st3ix.obfuscator.config;

import java.util.Collections;
import java.util.List;

/**
 * Configuration for obfuscation runs. Loaded from config.yml next to the JAR.
 */
public record ObfuscatorConfig(
    boolean classRenamingEnabled,
    boolean methodRenamingEnabled,
    boolean fieldRenamingEnabled,
    boolean numberObfuscationEnabled,
    boolean arrayObfuscationEnabled,
    boolean booleanObfuscationEnabled,
    boolean stringObfuscationEnabled,
    boolean debugInfoStrippingEnabled,
    boolean classNamesRandom,
    int classNameLength,
    boolean classNamesHomoglyph,
    boolean classNamesInvisibleChars,
    boolean methodNamesRandom,
    int methodNameLength,
    boolean methodNamesHomoglyph,
    boolean methodNamesInvisibleChars,
    boolean fieldNamesRandom,
    int fieldNameLength,
    boolean fieldNamesHomoglyph,
    boolean fieldNamesInvisibleChars,
    boolean numberKeyRandom,
    boolean arrayKeyRandom,
    boolean booleanKeyRandom,
    boolean stringKeyRandom,
    List<String> excludeClasses
) {
    private static final int DEFAULT_CLASS_NAME_LENGTH = 6;
    private static final int DEFAULT_METHOD_NAME_LENGTH = 4;
    private static final int DEFAULT_FIELD_NAME_LENGTH = 4;
    private static final int MIN_NAME_LENGTH = 1;
    private static final int MAX_CLASS_NAME_LENGTH = 32;
    private static final int MAX_METHOD_NAME_LENGTH = 32;
    private static final int MAX_FIELD_NAME_LENGTH = 32;

    public ObfuscatorConfig {
        excludeClasses = excludeClasses != null ? List.copyOf(excludeClasses) : Collections.emptyList();
        classNameLength = Math.max(MIN_NAME_LENGTH, Math.min(MAX_CLASS_NAME_LENGTH, classNameLength));
        methodNameLength = Math.max(MIN_NAME_LENGTH, Math.min(MAX_METHOD_NAME_LENGTH, methodNameLength));
        fieldNameLength = Math.max(MIN_NAME_LENGTH, Math.min(MAX_FIELD_NAME_LENGTH, fieldNameLength));
    }

    public static ObfuscatorConfig defaults() {
        return new ObfuscatorConfig(true, true, true, true, true, true, true, true,
            false, DEFAULT_CLASS_NAME_LENGTH, false, false,
            false, DEFAULT_METHOD_NAME_LENGTH, false, false,
            false, DEFAULT_FIELD_NAME_LENGTH, false, false,
            false, false, false, false, List.of());
    }
}
