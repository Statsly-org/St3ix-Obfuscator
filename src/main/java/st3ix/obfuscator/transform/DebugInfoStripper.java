package st3ix.obfuscator.transform;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * Strips debug information from bytecode: source file name, line number table,
 * and local variable table. Decompilers will show generic variable names (var0, var1, ...)
 * and lose source-to-bytecode mappings.
 */
public final class DebugInfoStripper {

    /**
     * Transforms the class bytes, removing all debug info.
     */
    public byte[] transform(byte[] classBytes) {
        ClassReader reader = new ClassReader(classBytes);
        ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS);
        reader.accept(new DebugInfoStripperVisitor(writer), 0);
        return writer.toByteArray();
    }

    private static final class DebugInfoStripperVisitor extends ClassVisitor {

        DebugInfoStripperVisitor(ClassVisitor cv) {
            super(Opcodes.ASM9, cv);
        }

        @Override
        public void visitSource(String source, String debug) {
            // Don't delegate – strips source file and debug extension
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor,
                                         String signature, String[] exceptions) {
            MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
            return new MethodVisitor(Opcodes.ASM9, mv) {
                @Override
                public void visitLineNumber(int line, org.objectweb.asm.Label start) {
                    // Don't delegate – strips line number table
                }

                @Override
                public void visitLocalVariable(String name, String descriptor, String signature,
                                               org.objectweb.asm.Label start, org.objectweb.asm.Label end, int index) {
                    // Don't delegate – strips local variable table
                }
            };
        }
    }
}
