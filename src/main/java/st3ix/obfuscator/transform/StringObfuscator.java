package st3ix.obfuscator.transform;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.nio.charset.StandardCharsets;
import java.util.Random;

/**
 * Obfuscates string literals in bytecode using XOR encryption.
 * Replaces LDC "string" with inline decryption bytecode at each call site.
 * No central decoder class – no single point to hook and dump all strings.
 * Key per class (owner.hashCode) and per string (index in method) for stronger obfuscation.
 */
public final class StringObfuscator {

    /** Local variable indices for inline decrypt (high to avoid clashes with method params). */
    private static final int LOCAL_ENC = 100;
    private static final int LOCAL_KEY = 101;
    private static final int LOCAL_OUT = 102;
    private static final int LOCAL_I = 103;
    private static final int LOCAL_BYTE = 104;

    private static final int DEFAULT_KEY = 0x7B3C9E2F;

    private final int key;

    /**
     * Creates an obfuscator with default (fixed) key.
     */
    public StringObfuscator() {
        this.key = DEFAULT_KEY;
    }

    /**
     * Creates an obfuscator with random key (different per instance).
     */
    public static StringObfuscator withRandomKey() {
        return new StringObfuscator(new Random().nextInt());
    }

    private StringObfuscator(int key) {
        this.key = key;
    }

    /**
     * Encrypts a string to a byte array using XOR.
     */
    public byte[] encrypt(String s) {
        return encrypt(s, key);
    }

    private static byte[] encrypt(String s, int key) {
        byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
        byte[] out = new byte[bytes.length];
        for (int i = 0; i < bytes.length; i++) {
            out[i] = (byte) ((bytes[i] & 0xFF) ^ ((key + i) & 0xFF));
        }
        return out;
    }

    /**
     * Transforms the class bytes, obfuscating string literals.
     */
    public byte[] transform(byte[] classBytes) {
        ClassReader reader = new ClassReader(classBytes);
        ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_FRAMES);
        reader.accept(new StringObfuscatorClassVisitor(writer, key), ClassReader.EXPAND_FRAMES);
        return writer.toByteArray();
    }

    private static final class StringObfuscatorClassVisitor extends ClassVisitor {

        private final int key;
        private String owner;

        StringObfuscatorClassVisitor(ClassVisitor cv, int key) {
            super(Opcodes.ASM9, cv);
            this.key = key;
        }

        @Override
        public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
            this.owner = name;
            super.visit(version, access, name, signature, superName, interfaces);
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor,
                                         String signature, String[] exceptions) {
            MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
            return new StringObfuscatorMethodVisitor(mv, key, owner);
        }
    }

    private static final class StringObfuscatorMethodVisitor extends MethodVisitor {

        private final int key;
        private final String owner;
        private int stringIndex;

        StringObfuscatorMethodVisitor(MethodVisitor mv, int key, String owner) {
            super(Opcodes.ASM9, mv);
            this.key = key;
            this.owner = owner;
        }

        @Override
        public void visitLdcInsn(Object value) {
            if (value instanceof String s) {
                emitEncryptedString(s);
            } else {
                super.visitLdcInsn(value);
            }
        }

        private void emitEncryptedString(String s) {
            int idx = stringIndex++;
            int actualKey = key ^ owner.hashCode() ^ idx;
            byte[] encrypted = encrypt(s, actualKey);
            emitByteArray(encrypted);
            emitRuntimeKey(idx);
            emitInlineDecrypt();
        }

        /** Inline XOR decrypt: stack [byte[] encrypted, int key] -> stack [String]. No central decoder. */
        private void emitInlineDecrypt() {
            org.objectweb.asm.Label loopStart = new org.objectweb.asm.Label();
            org.objectweb.asm.Label loopEnd = new org.objectweb.asm.Label();
            super.visitVarInsn(Opcodes.ASTORE, LOCAL_ENC);
            super.visitVarInsn(Opcodes.ISTORE, LOCAL_KEY);
            super.visitVarInsn(Opcodes.ALOAD, LOCAL_ENC);
            super.visitInsn(Opcodes.ARRAYLENGTH);
            super.visitIntInsn(Opcodes.NEWARRAY, Opcodes.T_BYTE);
            super.visitVarInsn(Opcodes.ASTORE, LOCAL_OUT);
            super.visitInsn(Opcodes.ICONST_0);
            super.visitVarInsn(Opcodes.ISTORE, LOCAL_I);
            super.visitLabel(loopStart);
            super.visitVarInsn(Opcodes.ILOAD, LOCAL_I);
            super.visitVarInsn(Opcodes.ALOAD, LOCAL_ENC);
            super.visitInsn(Opcodes.ARRAYLENGTH);
            super.visitJumpInsn(Opcodes.IF_ICMPGE, loopEnd);
            super.visitVarInsn(Opcodes.ALOAD, LOCAL_ENC);
            super.visitVarInsn(Opcodes.ILOAD, LOCAL_I);
            super.visitInsn(Opcodes.BALOAD);
            super.visitVarInsn(Opcodes.ILOAD, LOCAL_KEY);
            super.visitVarInsn(Opcodes.ILOAD, LOCAL_I);
            super.visitInsn(Opcodes.IADD);
            super.visitIntInsn(Opcodes.SIPUSH, 255);
            super.visitInsn(Opcodes.IAND);
            super.visitInsn(Opcodes.IXOR);
            super.visitInsn(Opcodes.I2B);
            super.visitVarInsn(Opcodes.ISTORE, LOCAL_BYTE);
            super.visitVarInsn(Opcodes.ALOAD, LOCAL_OUT);
            super.visitVarInsn(Opcodes.ILOAD, LOCAL_I);
            super.visitVarInsn(Opcodes.ILOAD, LOCAL_BYTE);
            super.visitInsn(Opcodes.BASTORE);
            super.visitIincInsn(LOCAL_I, 1);
            super.visitJumpInsn(Opcodes.GOTO, loopStart);
            super.visitLabel(loopEnd);
            super.visitTypeInsn(Opcodes.NEW, "java/lang/String");
            super.visitInsn(Opcodes.DUP);
            super.visitVarInsn(Opcodes.ALOAD, LOCAL_OUT);
            super.visitFieldInsn(Opcodes.GETSTATIC, "java/nio/charset/StandardCharsets", "UTF_8", "Ljava/nio/charset/Charset;");
            super.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/String", "<init>", "([BLjava/nio/charset/Charset;)V", false);
        }

        /** Emits: (key ^ stringIndex) ^ thisClass.getName().hashCode() = actualKey per class and per string */
        private void emitRuntimeKey(int stringIndex) {
            super.visitLdcInsn(Type.getObjectType(owner));
            super.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Class", "getName", "()Ljava/lang/String;", false);
            super.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/String", "hashCode", "()I", false);
            super.visitLdcInsn(key ^ stringIndex);
            super.visitInsn(Opcodes.IXOR);
        }

        private void emitByteArray(byte[] bytes) {
            super.visitLdcInsn(bytes.length);
            super.visitIntInsn(Opcodes.NEWARRAY, Opcodes.T_BYTE);
            for (int i = 0; i < bytes.length; i++) {
                super.visitInsn(Opcodes.DUP);
                super.visitLdcInsn(i);
                int b = bytes[i] & 0xFF;
                if (b <= 127) {
                    super.visitIntInsn(Opcodes.BIPUSH, b);
                } else {
                    super.visitIntInsn(Opcodes.SIPUSH, b);
                }
                super.visitInsn(Opcodes.BASTORE);
            }
        }
    }
}
