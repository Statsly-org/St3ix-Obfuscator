package st3ix.obfuscator.transform;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import st3ix.obfuscator.io.JarProcessor.ClassEntry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Maps method names for obfuscation. Handles override chains so overridden methods
 * get the same new name. Excludes constructors, main, native methods, and enum boilerplate.
 */
public final class MethodMapping {

    private static final String[] LIBRARY_PREFIXES = {
        "java/", "javax/", "jdk/", "sun/", "com/sun/",
        "org/xml/", "org/w3c/",
        "org/bukkit/", "org/spigotmc/", "net/minecraft/",
        "io/papermc/", "com/destroystokyo/",
        "org/apache/", "com/google/", "org/springframework/"
    };

    private static final String MAIN_DESC = "([Ljava/lang/String;)V";

    private final List<String> excludePrefixes = new ArrayList<>();
    private final Map<String, String> oldToNew = new HashMap<>();
    private final NameGenerator nameGen;
    private boolean excludeAll;
    private final Set<String> classesInJar = new HashSet<>();
    private final Map<String, ClassInfo> classInfos = new HashMap<>();

    public MethodMapping(boolean namesRandom, int nameLength, boolean useHomoglyph, boolean useInvisibleChars) {
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
     * Builds the method mapping from all class bytes. Must be called before getNewName.
     */
    public void buildFrom(List<ClassEntry> classes) {
        for (ClassEntry ce : classes) {
            classesInJar.add(ce.internalName());
        }

        for (ClassEntry ce : classes) {
            if (excludeAll || isExcluded(ce.internalName())) continue;
            ClassReader cr = new ClassReader(ce.bytes());
            ClassInfo info = new ClassInfo(ce.internalName());
            cr.accept(new ClassVisitor(Opcodes.ASM9) {
                @Override
                public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
                    info.superName = superName;
                    info.interfaces = interfaces != null ? interfaces : new String[0];
                    super.visit(version, access, name, signature, superName, interfaces);
                }

                @Override
                public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                    info.methods.add(new MethodDef(name, descriptor, access));
                    return super.visitMethod(access, name, descriptor, signature, exceptions);
                }
            }, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG);
            classInfos.put(ce.internalName(), info);
        }

        for (ClassEntry ce : classes) {
            if (excludeAll || isExcluded(ce.internalName())) continue;
            ClassInfo info = classInfos.get(ce.internalName());
            if (info == null) continue;
            for (MethodDef m : info.methods) {
                if (!canRename(m.name, m.desc, m.access)) continue;
                String root = findRoot(ce.internalName(), m.name, m.desc);
                String key = root + '.' + m.name + m.desc;
                oldToNew.computeIfAbsent(key, k -> nameGen.next());
            }
        }

        for (ClassEntry ce : classes) {
            if (excludeAll || isExcluded(ce.internalName())) continue;
            ClassInfo info = classInfos.get(ce.internalName());
            if (info == null) continue;
            for (MethodDef m : info.methods) {
                if (!canRename(m.name, m.desc, m.access)) continue;
                String root = findRoot(ce.internalName(), m.name, m.desc);
                String key = root + '.' + m.name + m.desc;
                String newName = oldToNew.get(key);
                if (newName != null) {
                    oldToNew.put(ce.internalName() + '.' + m.name + m.desc, newName);
                }
            }
        }
    }

    private boolean canRename(String name, String desc, int access) {
        if ("<init>".equals(name) || "<clinit>".equals(name)) return false;
        if ("main".equals(name) && MAIN_DESC.equals(desc)) return false;
        if ((access & Opcodes.ACC_NATIVE) != 0) return false;
        if ("valueOf".equals(name) && desc.startsWith("(Ljava/lang/String;)")) return false;
        if ("values".equals(name) && desc.startsWith("()[L")) return false;  // enum values()
        return true;
    }

    private String findRoot(String owner, String name, String desc) {
        ClassInfo info = classInfos.get(owner);
        if (info == null) return owner;

        String superName = info.superName;
        if (superName != null && classesInJar.contains(superName) && hasMethod(superName, name, desc)) {
            return findRoot(superName, name, desc);
        }
        for (String iface : info.interfaces) {
            if (classesInJar.contains(iface) && hasMethod(iface, name, desc)) {
                return findRoot(iface, name, desc);
            }
        }
        return owner;
    }

    private boolean hasMethod(String internalName, String name, String desc) {
        ClassInfo info = classInfos.get(internalName);
        if (info == null) return false;
        for (MethodDef m : info.methods) {
            if (m.name.equals(name) && m.desc.equals(desc)) return true;
        }
        return false;
    }

    public String getNewName(String owner, String name, String descriptor) {
        if ("<init>".equals(name) || "<clinit>".equals(name)) return name;
        String key = owner + '.' + name + descriptor;
        return oldToNew.getOrDefault(key, name);
    }

    /**
     * Returns all method renames for logging. Each entry: (className.methodName, newName).
     */
    public List<MethodRenameEntry> getRenameEntries() {
        List<MethodRenameEntry> result = new ArrayList<>();
        for (Map.Entry<String, String> e : oldToNew.entrySet()) {
            String key = e.getKey();
            String newName = e.getValue();
            int parenIdx = key.indexOf('(');
            if (parenIdx <= 0) continue;
            String beforeParen = key.substring(0, parenIdx);
            int dotIdx = beforeParen.lastIndexOf('.');
            if (dotIdx < 0) continue;
            String owner = beforeParen.substring(0, dotIdx);
            String oldName = beforeParen.substring(dotIdx + 1);
            if (!oldName.equals(newName)) {
                result.add(new MethodRenameEntry(owner.replace('/', '.'), oldName, newName));
            }
        }
        return result;
    }

    public record MethodRenameEntry(String className, String oldName, String newName) {}

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
        String[] interfaces;
        final List<MethodDef> methods = new ArrayList<>();

        ClassInfo(String name) {
            this.name = name;
        }
    }

    private static final class MethodDef {
        final String name;
        final String desc;
        final int access;

        MethodDef(String name, String desc, int access) {
            this.name = name;
            this.desc = desc;
            this.access = access;
        }
    }
}
