package st3ix.obfuscator.transform;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Opcodes;
import st3ix.obfuscator.io.JarProcessor.ClassEntry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Maps field names for obfuscation. Excludes serialVersionUID, enum constants,
 * and fields in excluded packages.
 */
public final class FieldMapping {

    private static final String[] LIBRARY_PREFIXES = {
        "java/", "javax/", "jdk/", "sun/", "com/sun/",
        "org/xml/", "org/w3c/",
        "org/bukkit/", "org/spigotmc/", "net/minecraft/",
        "io/papermc/", "com/destroystokyo/",
        "org/apache/", "com/google/", "org/springframework/"
    };

    private static final String SERIAL_VERSION_UID = "serialVersionUID";
    private static final String ENUM_SUPER = "java/lang/Enum";

    private final List<String> excludePrefixes = new ArrayList<>();
    private final Map<String, String> oldToNew = new HashMap<>();
    private final List<FieldRenameEntry> renameEntries = new ArrayList<>();
    private final NameGenerator nameGen;
    private boolean excludeAll;
    private final Map<String, ClassInfo> classInfos = new HashMap<>();

    public FieldMapping(boolean namesRandom, int nameLength, boolean useHomoglyph, boolean useInvisibleChars) {
        this.nameGen = new NameGenerator(namesRandom, nameLength, useHomoglyph, useInvisibleChars);
        for (String p : LIBRARY_PREFIXES) {
            excludePrefixes.add(p);
        }
    }

    public void addExcludes(List<String> patterns) {
        if (patterns == null) return;
        for (String p : patterns) {
            if (p == null || p.isBlank()) continue;
            String trimmed = p.trim();
            if ("*".equals(trimmed)) {
                excludeAll = true;
                return;
            }
            String internal = trimmed.replace('.', '/');
            if (internal.endsWith("/*")) internal = internal.substring(0, internal.length() - 1);
            if (!internal.endsWith("/")) internal += "/";
            excludePrefixes.add(internal);
        }
    }

    /**
     * Builds the field mapping from all class bytes. Must be called before getNewName.
     */
    public void buildFrom(List<ClassEntry> classes) {
        for (ClassEntry ce : classes) {
            if (excludeAll || isExcluded(ce.internalName())) continue;
            ClassReader cr = new ClassReader(ce.bytes());
            ClassInfo info = new ClassInfo(ce.internalName());
            cr.accept(new ClassVisitor(Opcodes.ASM9) {
                @Override
                public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
                    info.superName = superName;
                    super.visit(version, access, name, signature, superName, interfaces);
                }

                @Override
                public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
                    info.fields.add(new FieldDef(name, descriptor, access));
                    return super.visitField(access, name, descriptor, signature, value);
                }
            }, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG);
            classInfos.put(ce.internalName(), info);
        }

        for (ClassEntry ce : classes) {
            if (excludeAll || isExcluded(ce.internalName())) continue;
            ClassInfo info = classInfos.get(ce.internalName());
            if (info == null) continue;
            for (FieldDef f : info.fields) {
                if (!canRename(ce.internalName(), info.superName, f.name, f.desc, f.access)) continue;
                String key = ce.internalName() + '.' + f.name + f.desc;
                String newName = oldToNew.computeIfAbsent(key, k -> nameGen.next());
                if (!f.name.equals(newName)) {
                    renameEntries.add(new FieldRenameEntry(ce.internalName().replace('/', '.'), f.name, newName));
                }
            }
        }
    }

    private boolean canRename(String owner, String superName, String name, String desc, int access) {
        if (SERIAL_VERSION_UID.equals(name)) return false;
        if (ENUM_SUPER.equals(superName)) {
            String ownerType = "L" + owner + ";";
            if ((access & Opcodes.ACC_STATIC) != 0 && (access & Opcodes.ACC_FINAL) != 0
                && ownerType.equals(desc)) {
                return false;
            }
        }
        return true;
    }

    public String getNewName(String owner, String name, String descriptor) {
        String key = owner + '.' + name + descriptor;
        return oldToNew.getOrDefault(key, name);
    }

    /**
     * Returns all field renames for logging.
     */
    public List<FieldRenameEntry> getRenameEntries() {
        return new ArrayList<>(renameEntries);
    }

    public record FieldRenameEntry(String className, String oldName, String newName) {}

    private boolean isExcluded(String internalName) {
        for (String prefix : excludePrefixes) {
            String base = prefix.endsWith("/") ? prefix.substring(0, prefix.length() - 1) : prefix;
            if (internalName.equals(base) || internalName.startsWith(base + "/") || internalName.startsWith(base + "$")) {
                return true;
            }
        }
        return false;
    }

    private static final class ClassInfo {
        final String name;
        String superName;
        final List<FieldDef> fields = new ArrayList<>();

        ClassInfo(String name) {
            this.name = name;
        }
    }

    private static final class FieldDef {
        final String name;
        final String desc;
        final int access;

        FieldDef(String name, String desc, int access) {
            this.name = name;
            this.desc = desc;
            this.access = access;
        }
    }
}
