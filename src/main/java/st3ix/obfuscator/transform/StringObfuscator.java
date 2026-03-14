package st3ix.obfuscator.transform;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Obfuscates string literals using XOR encryption.
 * Replaces LDC "string" with inline decryption at each use site (no central decoder).
 * Also handles static final String fields: removes ConstantValue and initializes via
 * obfuscated code in &lt;clinit&gt;, so API keys and secrets are never readable.
 * Key per class and per string/field for stronger obfuscation.
 */
public final class StringObfuscator {

    private static final int LOCAL_ENC = 100;
    private static final int LOCAL_KEY = 101;
    private static final int LOCAL_OUT = 102;
    private static final int LOCAL_I = 103;
    private static final int LOCAL_BYTE = 104;

    private static final int DEFAULT_KEY = 0x7B3C9E2F;

    private final int key;

    public StringObfuscator() {
        this.key = DEFAULT_KEY;
    }

    public static StringObfuscator withRandomKey() {
        return new StringObfuscator(new Random().nextInt());
    }

    private StringObfuscator(int key) {
        this.key = key;
    }

    public byte[] transform(byte[] classBytes) {
        ClassReader reader = new ClassReader(classBytes);
        ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_FRAMES);
        reader.accept(new StringObfuscatorClassVisitor(writer, key), ClassReader.EXPAND_FRAMES);
        return writer.toByteArray();
    }

    private static byte[] encrypt(String s, int key) {
        byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
        byte[] out = new byte[bytes.length];
        for (int i = 0; i < bytes.length; i++) {
            out[i] = (byte) ((bytes[i] & 0xFF) ^ ((key + i) & 0xFF));
        }
        return out;
    }

    private static final class StringObfuscatorClassVisitor extends ClassVisitor {

        private final int key;
        private String owner;
        private final List<StaticStringField> staticStringFields = new ArrayList<>();
        private boolean hasClinit;

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
        public org.objectweb.asm.FieldVisitor visitField(int access, String name, String descriptor,
                                                          String signature, Object value) {
            if ("Ljava/lang/String;".equals(descriptor)
                    && (access & Opcodes.ACC_STATIC) != 0
                    && (access & Opcodes.ACC_FINAL) != 0
                    && value instanceof String s) {
                staticStringFields.add(new StaticStringField(name, s));
                return super.visitField(access, name, descriptor, signature, null);
            }
            return super.visitField(access, name, descriptor, signature, value);
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor,
                                         String signature, String[] exceptions) {
            MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
            if ("<clinit>".equals(name) && !staticStringFields.isEmpty()) {
                hasClinit = true;
                return new ClinitPrepender(mv, key, owner, staticStringFields);
            }
            return new StringObfuscatorMethodVisitor(mv, key, owner);
        }

        @Override
        public void visitEnd() {
            if (!staticStringFields.isEmpty() && !hasClinit) {
                MethodVisitor mv = super.visitMethod(Opcodes.ACC_STATIC, "<clinit>", "()V", null, null);
                if (mv != null) {
                    mv.visitCode();
                    emitStaticFieldInits(mv, key, owner, staticStringFields);
                    mv.visitInsn(Opcodes.RETURN);
                    mv.visitMaxs(0, 0);
                    mv.visitEnd();
                }
            }
            super.visitEnd();
        }

        private record StaticStringField(String name, String value) {}
    }

    private static final class ClinitPrepender extends MethodVisitor {

        private final int key;
        private final String owner;
        private final List<StringObfuscatorClassVisitor.StaticStringField> fields;

        ClinitPrepender(MethodVisitor mv, int key, String owner,
                        List<StringObfuscatorClassVisitor.StaticStringField> fields) {
            super(Opcodes.ASM9, mv);
            this.key = key;
            this.owner = owner;
            this.fields = fields;
        }

        @Override
        public void visitCode() {
            emitStaticFieldInits(this, key, owner, fields);
            super.visitCode();
        }
    }

    private static void emitStaticFieldInits(MethodVisitor mv, int key, String owner,
                                            List<StringObfuscatorClassVisitor.StaticStringField> fields) {
        for (int i = 0; i < fields.size(); i++) {
            var f = fields.get(i);
            int actualKey = key ^ owner.hashCode() ^ i;
            byte[] enc = encrypt(f.value(), actualKey);
            emitRuntimeKey(mv, owner, key ^ i);
            emitByteArray(mv, enc);
            emitInlineDecrypt(mv);
            mv.visitFieldInsn(Opcodes.PUTSTATIC, owner, f.name(), "Ljava/lang/String;");
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
                int idx = stringIndex++;
                int actualKey = key ^ owner.hashCode() ^ idx;
                byte[] encrypted = encrypt(s, actualKey);
                emitRuntimeKey(idx);
                emitByteArray(encrypted);
                emitInlineDecrypt();
            } else {
                super.visitLdcInsn(value);
            }
        }

        private void emitInlineDecrypt() {
            Label loopStart = new Label();
            Label loopEnd = new Label();
            // Stack: [key, byte[]] - top is byte[], store it first (ASTORE), then key (ISTORE)
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

    private static void emitByteArray(MethodVisitor mv, byte[] bytes) {
        mv.visitLdcInsn(bytes.length);
        mv.visitIntInsn(Opcodes.NEWARRAY, Opcodes.T_BYTE);
        for (int i = 0; i < bytes.length; i++) {
            mv.visitInsn(Opcodes.DUP);
            mv.visitLdcInsn(i);
            int b = bytes[i] & 0xFF;
            if (b <= 127) {
                mv.visitIntInsn(Opcodes.BIPUSH, b);
            } else {
                mv.visitIntInsn(Opcodes.SIPUSH, b);
            }
            mv.visitInsn(Opcodes.BASTORE);
        }
    }

    private static void emitRuntimeKey(MethodVisitor mv, String owner, int actualKey) {
        mv.visitLdcInsn(Type.getObjectType(owner));
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Class", "getName", "()Ljava/lang/String;", false);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/String", "hashCode", "()I", false);
        mv.visitLdcInsn(actualKey);
        mv.visitInsn(Opcodes.IXOR);
    }

    private static void emitInlineDecrypt(MethodVisitor mv) {
        Label loopStart = new Label();
        Label loopEnd = new Label();
        mv.visitVarInsn(Opcodes.ASTORE, LOCAL_ENC);
        mv.visitVarInsn(Opcodes.ISTORE, LOCAL_KEY);
        mv.visitVarInsn(Opcodes.ALOAD, LOCAL_ENC);
        mv.visitInsn(Opcodes.ARRAYLENGTH);
        mv.visitIntInsn(Opcodes.NEWARRAY, Opcodes.T_BYTE);
        mv.visitVarInsn(Opcodes.ASTORE, LOCAL_OUT);
        mv.visitInsn(Opcodes.ICONST_0);
        mv.visitVarInsn(Opcodes.ISTORE, LOCAL_I);
        mv.visitLabel(loopStart);
        mv.visitVarInsn(Opcodes.ILOAD, LOCAL_I);
        mv.visitVarInsn(Opcodes.ALOAD, LOCAL_ENC);
        mv.visitInsn(Opcodes.ARRAYLENGTH);
        mv.visitJumpInsn(Opcodes.IF_ICMPGE, loopEnd);
        mv.visitVarInsn(Opcodes.ALOAD, LOCAL_ENC);
        mv.visitVarInsn(Opcodes.ILOAD, LOCAL_I);
        mv.visitInsn(Opcodes.BALOAD);
        mv.visitVarInsn(Opcodes.ILOAD, LOCAL_KEY);
        mv.visitVarInsn(Opcodes.ILOAD, LOCAL_I);
        mv.visitInsn(Opcodes.IADD);
        mv.visitIntInsn(Opcodes.SIPUSH, 255);
        mv.visitInsn(Opcodes.IAND);
        mv.visitInsn(Opcodes.IXOR);
        mv.visitInsn(Opcodes.I2B);
        mv.visitVarInsn(Opcodes.ISTORE, LOCAL_BYTE);
        mv.visitVarInsn(Opcodes.ALOAD, LOCAL_OUT);
        mv.visitVarInsn(Opcodes.ILOAD, LOCAL_I);
        mv.visitVarInsn(Opcodes.ILOAD, LOCAL_BYTE);
        mv.visitInsn(Opcodes.BASTORE);
        mv.visitIincInsn(LOCAL_I, 1);
        mv.visitJumpInsn(Opcodes.GOTO, loopStart);
        mv.visitLabel(loopEnd);
        mv.visitTypeInsn(Opcodes.NEW, "java/lang/String");
        mv.visitInsn(Opcodes.DUP);
        mv.visitVarInsn(Opcodes.ALOAD, LOCAL_OUT);
        mv.visitFieldInsn(Opcodes.GETSTATIC, "java/nio/charset/StandardCharsets", "UTF_8", "Ljava/nio/charset/Charset;");
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/String", "<init>", "([BLjava/nio/charset/Charset;)V", false);
    }
}
