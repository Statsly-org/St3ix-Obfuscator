package st3ix.obfuscator.transform;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Maps original class internal names to obfuscated names.
 * Excludes JDK, common libraries (Paper, Spigot, Minecraft), and config-defined patterns.
 */
public final class ClassMapping {

    /**
     * Library/API packages that must never be renamed. External APIs expect these names.
     */
    private static final String[] LIBRARY_PREFIXES = {
        "java/", "javax/", "jdk/", "sun/", "com/sun/",
        "org/xml/", "org/w3c/",
        "org/bukkit/", "org/spigotmc/", "net/minecraft/",
        "io/papermc/", "com/destroystokyo/",
        "org/apache/", "com/google/", "org/springframework/"
    };

    private final List<String> excludePrefixes = new ArrayList<>();
    private final Map<String, String> oldToNew = new HashMap<>();
    private final NameGenerator nameGen;

    public ClassMapping() {
        this(false, 1);
    }

    /**
     * Creates a mapping with configurable name generation.
     *
     * @param namesRandom true for random names, false for sequential
     * @param nameLength minimum length of generated names (1–32)
     */
    public ClassMapping(boolean namesRandom, int nameLength) {
        this.nameGen = new NameGenerator(namesRandom, nameLength);
        for (String p : LIBRARY_PREFIXES) {
            excludePrefixes.add(p);
        }
    }

    /**
     * Adds exclude patterns from config. Accepts dot or slash notation (e.g. org.bukkit or org/bukkit).
     */
    public void addExcludes(List<String> patterns) {
        if (patterns == null) return;
        for (String p : patterns) {
            if (p == null || p.isBlank()) continue;
            String internal = p.trim().replace('.', '/');
            if (!internal.endsWith("/")) internal += "/";
            excludePrefixes.add(internal);
        }
    }

    /**
     * Registers a class for renaming. Excluded classes are skipped.
     *
     * @param internalName ASM internal name (e.g. example/Main)
     * @return the new internal name, or the original if excluded
     */
    public String map(String internalName) {
        if (oldToNew.containsKey(internalName)) {
            return oldToNew.get(internalName);
        }
        if (isExcluded(internalName)) {
            oldToNew.put(internalName, internalName);
            return internalName;
        }
        int dollar = internalName.lastIndexOf('$');
        String newName;
        if (dollar > 0) {
            String outer = internalName.substring(0, dollar);
            String newOuter = map(outer);
            newName = newOuter + "$" + nameGen.next();
        } else {
            newName = nameGen.next();
        }
        oldToNew.put(internalName, newName);
        return newName;
    }

    /**
     * Returns the obfuscated name for a class, or the original if not mapped.
     */
    public String getNewName(String internalName) {
        return oldToNew.getOrDefault(internalName, internalName);
    }

    /**
     * Converts a binary class name (e.g. example.Main) to the new name for manifest.
     */
    public String toBinaryName(String binaryName) {
        String internal = binaryName.replace('.', '/');
        String newInternal = getNewName(internal);
        return newInternal.replace('/', '.');
    }

    private boolean isExcluded(String internalName) {
        for (String prefix : excludePrefixes) {
            String base = prefix.endsWith("/") ? prefix.substring(0, prefix.length() - 1) : prefix;
            if (internalName.equals(base) || internalName.startsWith(base + "/") || internalName.startsWith(base + "$")) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the mapping for ASM SimpleRemapper (old internal -> new internal).
     */
    public Map<String, String> asRemapperMap() {
        Map<String, String> result = new HashMap<>();
        for (Map.Entry<String, String> e : oldToNew.entrySet()) {
            if (!e.getKey().equals(e.getValue())) {
                result.put(e.getKey(), e.getValue());
            }
        }
        return result;
    }
}
