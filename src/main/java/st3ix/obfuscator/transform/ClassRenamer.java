package st3ix.obfuscator.transform;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.SimpleRemapper;

import java.util.Map;

/**
 * Transforms class bytecode by renaming classes according to the given mapping.
 */
public final class ClassRenamer {

    private final ClassMapping mapping;

    public ClassRenamer(ClassMapping mapping) {
        this.mapping = mapping;
    }

    /**
     * Transforms the given class bytes. Returns the transformed bytes.
     */
    public byte[] transform(byte[] classBytes) {
        Map<String, String> map = mapping.asRemapperMap();
        SimpleRemapper remapper = new SimpleRemapper(map);
        ClassReader reader = new ClassReader(classBytes);
        ClassWriter writer = new ClassWriter(reader, 0);
        ClassRemapper adapter = new ClassRemapper(writer, remapper);
        reader.accept(adapter, ClassReader.EXPAND_FRAMES);
        return writer.toByteArray();
    }
}
