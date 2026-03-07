package st3ix.obfuscator.transform;

import java.util.HashMap;
import java.util.Map;

/**
 * Maps original class internal names to obfuscated names.
 */
public final class ClassMapping {

    private static final String[] EXCLUDED_PREFIXES = {
        "java/", "javax/", "jdk/", "sun/", "com/sun/", "org/xml/", "org/w3c/"
    };

    private final Map<String, String> oldToNew = new HashMap<>();
    private final NameGenerator nameGen = new NameGenerator();

    /**
     * Registers a class for renaming. JDK and library classes are skipped.
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
        for (String prefix : EXCLUDED_PREFIXES) {
            if (internalName.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the mapping as a map for ASM SimpleRemapper (old internal -> new internal).
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
