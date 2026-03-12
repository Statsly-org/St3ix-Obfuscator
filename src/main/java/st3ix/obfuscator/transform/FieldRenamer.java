package st3ix.obfuscator.transform;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * Transforms class bytecode by renaming fields according to the given mapping.
 */
public final class FieldRenamer {

    private final FieldMapping mapping;

    public FieldRenamer(FieldMapping mapping) {
        this.mapping = mapping;
    }

    /**
     * Transforms the given class bytes. Returns the transformed bytes.
     */
    public byte[] transform(byte[] classBytes) {
        ClassReader reader = new ClassReader(classBytes);
        ClassWriter writer = new ClassWriter(reader, 0);
        reader.accept(new FieldRenamerVisitor(writer), ClassReader.EXPAND_FRAMES);
        return writer.toByteArray();
    }

    private final class FieldRenamerVisitor extends ClassVisitor {
        private String ownerInternalName;

        FieldRenamerVisitor(ClassVisitor cv) {
            super(Opcodes.ASM9, cv);
        }

        @Override
        public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
            ownerInternalName = name;
            super.visit(version, access, name, signature, superName, interfaces);
        }

        @Override
        public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
            String newName = mapping.getNewName(ownerInternalName, name, descriptor);
            return super.visitField(access, newName, descriptor, signature, value);
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
            return new MethodVisitor(Opcodes.ASM9, super.visitMethod(access, name, descriptor, signature, exceptions)) {
                @Override
                public void visitFieldInsn(int opcode, String owner, String fieldName, String fieldDesc) {
                    String mappedName = mapping.getNewName(owner, fieldName, fieldDesc);
                    super.visitFieldInsn(opcode, owner, mappedName, fieldDesc);
                }
            };
        }
    }
}
