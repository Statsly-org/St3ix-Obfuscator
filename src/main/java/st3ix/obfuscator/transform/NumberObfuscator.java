package st3ix.obfuscator.transform;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.Random;

/**
 * Obfuscates integer, long, float and double constants in bytecode using XOR.
 * Replaces ldc N with (N ^ key) ^ key to hide the original value.
 * For float/double, the bit representation is XORed and reconstructed at runtime.
 */
public final class NumberObfuscator {

    private static final int DEFAULT_INT_KEY = 0x5A5A5A5A;
    private static final long DEFAULT_LONG_KEY = 0x5A5A5A5A5A5A5A5AL;

    private final int intKey;
    private final long longKey;

    /**
     * Creates an obfuscator with default (fixed) keys.
     */
    public NumberObfuscator() {
        this.intKey = DEFAULT_INT_KEY;
        this.longKey = DEFAULT_LONG_KEY;
    }

    /**
     * Creates an obfuscator with random keys (different per instance).
     */
    public static NumberObfuscator withRandomKey() {
        Random r = new Random();
        int ik = r.nextInt();
        long lk = ((long) r.nextInt() << 32) | (r.nextInt() & 0xFFFFFFFFL);
        return new NumberObfuscator(ik, lk);
    }

    private NumberObfuscator(int intKey, long longKey) {
        this.intKey = intKey;
        this.longKey = longKey;
    }

    /**
     * Transforms the class bytes, obfuscating numeric constants.
     */
    public byte[] transform(byte[] classBytes) {
        ClassReader reader = new ClassReader(classBytes);
        ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_FRAMES);
        reader.accept(new NumberObfuscatorClassVisitor(writer, intKey, longKey), ClassReader.EXPAND_FRAMES);
        return writer.toByteArray();
    }

    private static final class NumberObfuscatorClassVisitor extends ClassVisitor {

        private final int intKey;
        private final long longKey;

        NumberObfuscatorClassVisitor(ClassVisitor cv, int intKey, long longKey) {
            super(Opcodes.ASM9, cv);
            this.intKey = intKey;
            this.longKey = longKey;
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor,
                                         String signature, String[] exceptions) {
            MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
            return new NumberObfuscatorMethodVisitor(mv, intKey, longKey);
        }
    }

    private static final class NumberObfuscatorMethodVisitor extends MethodVisitor {

        private final int intKey;
        private final long longKey;

        NumberObfuscatorMethodVisitor(MethodVisitor mv, int intKey, long longKey) {
            super(Opcodes.ASM9, mv);
            this.intKey = intKey;
            this.longKey = longKey;
        }

        @Override
        public void visitIntInsn(int opcode, int operand) {
            if (opcode == Opcodes.BIPUSH || opcode == Opcodes.SIPUSH) {
                emitXorInt(operand);
            } else {
                super.visitIntInsn(opcode, operand);
            }
        }

        @Override
        public void visitLdcInsn(Object value) {
            if (value instanceof Integer i) {
                emitXorInt(i);
            } else if (value instanceof Long l) {
                emitXorLong(l);
            } else if (value instanceof Float f) {
                int bits = Float.floatToRawIntBits(f);
                int obfuscated = bits ^ intKey;
                super.visitLdcInsn(obfuscated);
                super.visitLdcInsn(intKey);
                super.visitInsn(Opcodes.IXOR);
                super.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Float", "intBitsToFloat", "(I)F", false);
            } else if (value instanceof Double d) {
                long bits = Double.doubleToRawLongBits(d);
                long obfuscated = bits ^ longKey;
                super.visitLdcInsn(obfuscated);
                super.visitLdcInsn(longKey);
                super.visitInsn(Opcodes.LXOR);
                super.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Double", "longBitsToDouble", "(J)D", false);
            } else {
                super.visitLdcInsn(value);
            }
        }

        private void emitXorLong(long value) {
            long obfuscated = value ^ longKey;
            super.visitLdcInsn(obfuscated);
            super.visitLdcInsn(longKey);
            super.visitInsn(Opcodes.LXOR);
        }

        private void emitXorInt(int value) {
            int obfuscated = value ^ intKey;
            super.visitLdcInsn(obfuscated);
            super.visitLdcInsn(intKey);
            super.visitInsn(Opcodes.IXOR);
        }
    }
}
