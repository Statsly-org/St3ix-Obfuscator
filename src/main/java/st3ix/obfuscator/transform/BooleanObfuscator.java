package st3ix.obfuscator.transform;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.Random;

/**
 * Obfuscates boolean constants (true/false) in bytecode using XOR.
 * Replaces ICONST_0 and ICONST_1 with (value ^ key) ^ key to hide the original value.
 */
public final class BooleanObfuscator {

    private static final int DEFAULT_KEY = 0x9E37A2C1;

    private final int key;

    /**
     * Creates an obfuscator with default (fixed) key.
     */
    public BooleanObfuscator() {
        this.key = DEFAULT_KEY;
    }

    /**
     * Creates an obfuscator with random key (different per instance).
     */
    public static BooleanObfuscator withRandomKey() {
        return new BooleanObfuscator(new Random().nextInt());
    }

    private BooleanObfuscator(int key) {
        this.key = key;
    }

    /**
     * Transforms the class bytes, obfuscating boolean constants.
     */
    public byte[] transform(byte[] classBytes) {
        ClassReader reader = new ClassReader(classBytes);
        ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_FRAMES);
        reader.accept(new BooleanObfuscatorClassVisitor(writer, key), ClassReader.EXPAND_FRAMES);
        return writer.toByteArray();
    }

    private static final class BooleanObfuscatorClassVisitor extends ClassVisitor {

        private final int key;

        BooleanObfuscatorClassVisitor(ClassVisitor cv, int key) {
            super(Opcodes.ASM9, cv);
            this.key = key;
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor,
                                         String signature, String[] exceptions) {
            MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
            return new BooleanObfuscatorMethodVisitor(mv, key);
        }
    }

    private static final class BooleanObfuscatorMethodVisitor extends MethodVisitor {

        private final int key;

        BooleanObfuscatorMethodVisitor(MethodVisitor mv, int key) {
            super(Opcodes.ASM9, mv);
            this.key = key;
        }

        @Override
        public void visitInsn(int opcode) {
            switch (opcode) {
                case Opcodes.ICONST_0 -> emitXorInt(0);
                case Opcodes.ICONST_1 -> emitXorInt(1);
                default -> super.visitInsn(opcode);
            }
        }

        private void emitXorInt(int value) {
            int obfuscated = value ^ key;
            super.visitLdcInsn(obfuscated);
            super.visitLdcInsn(key);
            super.visitInsn(Opcodes.IXOR);
        }
    }
}
