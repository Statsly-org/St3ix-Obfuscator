package st3ix.obfuscator.transform;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * Transforms class bytecode by renaming methods according to the given mapping.
 */
public final class MethodRenamer {

    private final MethodMapping mapping;

    public MethodRenamer(MethodMapping mapping) {
        this.mapping = mapping;
    }

    /**
     * Transforms the given class bytes. Returns the transformed bytes.
     */
    public byte[] transform(byte[] classBytes) {
        ClassReader reader = new ClassReader(classBytes);
        ClassWriter writer = new ClassWriter(reader, 0);
        reader.accept(new MethodRenamerVisitor(writer), ClassReader.EXPAND_FRAMES);
        return writer.toByteArray();
    }

    private final class MethodRenamerVisitor extends ClassVisitor {
        private String ownerInternalName;

        MethodRenamerVisitor(ClassVisitor cv) {
            super(Opcodes.ASM9, cv);
        }

        @Override
        public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
            ownerInternalName = name;
            super.visit(version, access, name, signature, superName, interfaces);
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
            String newName = mapping.getNewName(ownerInternalName, name, descriptor);
            MethodVisitor mv = super.visitMethod(access, newName, descriptor, signature, exceptions);
            return new MethodVisitor(Opcodes.ASM9, mv) {
                @Override
                public void visitMethodInsn(int opcode, String owner, String methodName, String methodDesc, boolean isInterface) {
                    String mappedName = mapping.getNewName(owner, methodName, methodDesc);
                    super.visitMethodInsn(opcode, owner, mappedName, methodDesc, isInterface);
                }
            };
        }
    }
}
