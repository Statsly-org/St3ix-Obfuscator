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
 * Replaces LDC "string" with a call to the decoder with encrypted byte array.
 */
public final class StringObfuscator {

    /** Internal name of the decoder class (package o, class a). */
    public static final String DECODER_CLASS = "o/a";
    public static final String DECODER_METHOD = "d";
    public static final String DECODER_DESCRIPTOR = "([BI)Ljava/lang/String;";

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

    /**
     * Generates the decoder helper class bytecode. Must be added to the output JAR when string obfuscation is used.
     */
    public static byte[] generateDecoderClass() {
        org.objectweb.asm.ClassWriter cw = new org.objectweb.asm.ClassWriter(org.objectweb.asm.ClassWriter.COMPUTE_FRAMES);
        cw.visit(Opcodes.V17, Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL | Opcodes.ACC_SUPER,
            DECODER_CLASS, null, "java/lang/Object", null);

        org.objectweb.asm.MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
        mv.visitCode();
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();

        mv = cw.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, DECODER_METHOD, DECODER_DESCRIPTOR, null, null);
        mv.visitCode();
        // byte[] b = param0, int k = param1
        // byte[] out = new byte[b.length]
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitInsn(Opcodes.ARRAYLENGTH);
        mv.visitIntInsn(Opcodes.NEWARRAY, Opcodes.T_BYTE);
        mv.visitVarInsn(Opcodes.ASTORE, 2);
        // for (int i = 0; i < b.length; i++) out[i] = (byte)(b[i] ^ ((k+i) & 0xFF))
        mv.visitInsn(Opcodes.ICONST_0);
        mv.visitVarInsn(Opcodes.ISTORE, 3);  // i
        org.objectweb.asm.Label loopStart = new org.objectweb.asm.Label();
        org.objectweb.asm.Label loopEnd = new org.objectweb.asm.Label();
        mv.visitLabel(loopStart);
        mv.visitVarInsn(Opcodes.ILOAD, 3);
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitInsn(Opcodes.ARRAYLENGTH);
        mv.visitJumpInsn(Opcodes.IF_ICMPGE, loopEnd);
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitVarInsn(Opcodes.ILOAD, 3);
        mv.visitInsn(Opcodes.BALOAD);
        mv.visitVarInsn(Opcodes.ILOAD, 1);
        mv.visitVarInsn(Opcodes.ILOAD, 3);
        mv.visitInsn(Opcodes.IADD);
        mv.visitIntInsn(Opcodes.SIPUSH, 255);
        mv.visitInsn(Opcodes.IAND);
        mv.visitInsn(Opcodes.IXOR);
        mv.visitInsn(Opcodes.I2B);
        mv.visitVarInsn(Opcodes.ISTORE, 4);
        mv.visitVarInsn(Opcodes.ALOAD, 2);
        mv.visitVarInsn(Opcodes.ILOAD, 3);
        mv.visitVarInsn(Opcodes.ILOAD, 4);
        mv.visitInsn(Opcodes.BASTORE);
        mv.visitIincInsn(3, 1);
        mv.visitJumpInsn(Opcodes.GOTO, loopStart);
        mv.visitLabel(loopEnd);
        mv.visitTypeInsn(Opcodes.NEW, "java/lang/String");
        mv.visitInsn(Opcodes.DUP);
        mv.visitVarInsn(Opcodes.ALOAD, 2);
        mv.visitFieldInsn(Opcodes.GETSTATIC, "java/nio/charset/StandardCharsets", "UTF_8", "Ljava/nio/charset/Charset;");
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/String", "<init>", "([BLjava/nio/charset/Charset;)V", false);
        mv.visitInsn(Opcodes.ARETURN);
        mv.visitMaxs(0, 0);  // COMPUTE_FRAMES computes these
        mv.visitEnd();

        cw.visitEnd();
        return cw.toByteArray();
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
            boolean isClinit = "<clinit>".equals(name);
            return new StringObfuscatorMethodVisitor(mv, key, owner, isClinit);
        }
    }

    private static final class StringObfuscatorMethodVisitor extends MethodVisitor {

        private final int key;
        private final String owner;
        private final boolean useRuntimeKey;

        StringObfuscatorMethodVisitor(MethodVisitor mv, int key, String owner, boolean useRuntimeKey) {
            super(Opcodes.ASM9, mv);
            this.key = key;
            this.owner = owner;
            this.useRuntimeKey = useRuntimeKey;
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
            int actualKey = useRuntimeKey ? key ^ owner.hashCode() : key;
            byte[] encrypted = encrypt(s, actualKey);
            emitByteArray(encrypted);
            if (useRuntimeKey) {
                emitRuntimeKey();
            } else {
                super.visitLdcInsn(key);
            }
            super.visitMethodInsn(Opcodes.INVOKESTATIC, DECODER_CLASS, DECODER_METHOD, DECODER_DESCRIPTOR, false);
        }

        /** Emits: key ^ thisClass.getName().hashCode() - prevents static evaluation by decompilers */
        private void emitRuntimeKey() {
            super.visitLdcInsn(Type.getObjectType(owner));
            super.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Class", "getName", "()Ljava/lang/String;", false);
            super.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/String", "hashCode", "()I", false);
            super.visitLdcInsn(key);
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
