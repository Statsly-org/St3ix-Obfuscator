package st3ix.obfuscator.transform;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.Random;

/**
 * Obfuscates numeric constants in bytecode using math expression obfuscation.
 * Replaces simple constants like 123 with expressions such as (50 * 3) - 27,
 * so decompilers show the expression instead of the original value and compilers
 * cannot constant-fold as easily. Float/double/long still use XOR for now.
 */
public final class NumberObfuscator {

    private static final int DEFAULT_INT_KEY = 0x5A5A5A5A;
    private static final long DEFAULT_LONG_KEY = 0x5A5A5A5A5A5A5A5AL;

    private final int intKey;
    private final long longKey;
    private final Random rng;

    /**
     * Creates an obfuscator with fixed expression pattern (deterministic per value).
     */
    public NumberObfuscator() {
        this.intKey = DEFAULT_INT_KEY;
        this.longKey = DEFAULT_LONG_KEY;
        this.rng = null;
    }

    /**
     * Creates an obfuscator that randomizes expression patterns per value.
     */
    public static NumberObfuscator withRandomKey() {
        Random r = new Random();
        int ik = r.nextInt();
        long lk = ((long) r.nextInt() << 32) | (r.nextInt() & 0xFFFFFFFFL);
        return new NumberObfuscator(ik, lk, r);
    }

    private NumberObfuscator(int intKey, long longKey, Random rng) {
        this.intKey = intKey;
        this.longKey = longKey;
        this.rng = rng;
    }

    /**
     * Transforms the class bytes, obfuscating numeric constants.
     */
    public byte[] transform(byte[] classBytes) {
        ClassReader reader = new ClassReader(classBytes);
        ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_FRAMES);
        reader.accept(new NumberObfuscatorClassVisitor(writer, intKey, longKey, rng), ClassReader.EXPAND_FRAMES);
        return writer.toByteArray();
    }

    private static final class NumberObfuscatorClassVisitor extends ClassVisitor {

        private final int intKey;
        private final long longKey;
        private final Random rng;

        NumberObfuscatorClassVisitor(ClassVisitor cv, int intKey, long longKey, Random rng) {
            super(Opcodes.ASM9, cv);
            this.intKey = intKey;
            this.longKey = longKey;
            this.rng = rng;
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor,
                                         String signature, String[] exceptions) {
            MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
            return new NumberObfuscatorMethodVisitor(mv, intKey, longKey, rng);
        }
    }

    private static final class NumberObfuscatorMethodVisitor extends MethodVisitor {

        private final int intKey;
        private final long longKey;
        private final Random rng;

        NumberObfuscatorMethodVisitor(MethodVisitor mv, int intKey, long longKey, Random rng) {
            super(Opcodes.ASM9, mv);
            this.intKey = intKey;
            this.longKey = longKey;
            this.rng = rng;
        }

        @Override
        public void visitIntInsn(int opcode, int operand) {
            if (opcode == Opcodes.BIPUSH || opcode == Opcodes.SIPUSH) {
                emitIntExpression(operand);
            } else {
                super.visitIntInsn(opcode, operand);
            }
        }

        @Override
        public void visitLdcInsn(Object value) {
            if (value instanceof Integer i) {
                emitIntExpression(i);
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

        private void emitIntExpression(int value) {
            if (tryEmitExpression(value)) {
                return;
            }
            emitXorInt(value);
        }

        private boolean tryEmitExpression(int value) {
            int strategy = rng != null ? rng.nextInt(5) : (Math.abs(value) % 5);
            return switch (strategy) {
                case 0 -> emitAdd(value);
                case 1 -> emitMulAdd(value);
                case 2 -> emitSub(value);
                case 3 -> emitShiftAdd(value);
                case 4 -> emitMulSub(value);
                default -> emitAdd(value);
            };
        }

        private void emitLdcInt(int v) {
            if (v >= -1 && v <= 5) {
                int opcode = switch (v) {
                    case -1 -> Opcodes.ICONST_M1;
                    case 0 -> Opcodes.ICONST_0;
                    case 1 -> Opcodes.ICONST_1;
                    case 2 -> Opcodes.ICONST_2;
                    case 3 -> Opcodes.ICONST_3;
                    case 4 -> Opcodes.ICONST_4;
                    case 5 -> Opcodes.ICONST_5;
                    default -> -1;
                };
                if (opcode != -1) {
                    super.visitInsn(opcode);
                    return;
                }
            }
            if (v >= Byte.MIN_VALUE && v <= Byte.MAX_VALUE) {
                super.visitIntInsn(Opcodes.BIPUSH, v);
            } else if (v >= Short.MIN_VALUE && v <= Short.MAX_VALUE) {
                super.visitIntInsn(Opcodes.SIPUSH, v);
            } else {
                super.visitLdcInsn(v);
            }
        }

        private boolean emitAdd(int value) {
            int a;
            if (rng != null) {
                a = rng.nextInt(2001) - 1000;
                if (value >= 0 && a > value) a = value;
                if (value < 0 && a < value) a = value;
            } else {
                a = (Math.abs(value) % 500) + 1;
                if (value < 0) a = -a;
                if ((long) a + (long) (value - a) != value) a = value / 2;
            }
            long b = (long) value - (long) a;
            if (b < Integer.MIN_VALUE || b > Integer.MAX_VALUE) return false;
            emitLdcInt(a);
            emitLdcInt((int) b);
            super.visitInsn(Opcodes.IADD);
            return true;
        }

        private boolean emitMulAdd(int value) {
            if (value == 0) {
                emitLdcInt(1);
                emitLdcInt(0);
                super.visitInsn(Opcodes.IMUL);
                return true;
            }
            int a = (rng != null ? rng.nextInt(49) + 2 : 17) & 0xFF;
            if (a <= 0) a = 2;
            int b = value / a;
            int c = value - a * b;
            long prod = (long) a * (long) b;
            if (prod != (int) prod) return false;
            if ((long) a * b + c != value) return false;
            emitLdcInt(a);
            emitLdcInt(b);
            super.visitInsn(Opcodes.IMUL);
            if (c != 0) {
                emitLdcInt(c);
                super.visitInsn(Opcodes.IADD);
            }
            return true;
        }

        private boolean emitSub(int value) {
            int k = rng != null ? rng.nextInt(500) + 1 : Math.abs(value % 100) + 1;
            long a = (long) value + (long) k;
            if (a < Integer.MIN_VALUE || a > Integer.MAX_VALUE) return false;
            emitLdcInt((int) a);
            emitLdcInt(k);
            super.visitInsn(Opcodes.ISUB);
            return true;
        }

        private boolean emitShiftAdd(int value) {
            if (value < 0) return false;
            int n = rng != null ? rng.nextInt(4) + 1 : (value % 4) + 1;
            int shift = 1 << n;
            int a = value / shift;
            int b = value % shift;
            if (value == Integer.MAX_VALUE && n == 1) return false;
            emitLdcInt(a);
            emitLdcInt(n);
            super.visitInsn(Opcodes.ISHL);
            if (b != 0) {
                emitLdcInt(b);
                super.visitInsn(Opcodes.IADD);
            }
            return true;
        }

        private boolean emitMulSub(int value) {
            if (value == Integer.MIN_VALUE) {
                emitLdcInt(1);
                emitLdcInt(31);
                super.visitInsn(Opcodes.ISHL);
                return true;
            }
            if (value == Integer.MAX_VALUE) {
                emitLdcInt(1);
                emitLdcInt(31);
                super.visitInsn(Opcodes.ISHL);
                emitLdcInt(1);
                super.visitInsn(Opcodes.ISUB);
                return true;
            }
            int a = (rng != null ? rng.nextInt(30) + 2 : 7) & 0xFF;
            int b = (rng != null ? rng.nextInt(30) + 2 : 11) & 0xFF;
            long prod = (long) a * (long) b;
            if (prod < Integer.MIN_VALUE || prod > Integer.MAX_VALUE) return false;
            int c = (int) prod - value;
            if (c < 0) return false;
            emitLdcInt(a);
            emitLdcInt(b);
            super.visitInsn(Opcodes.IMUL);
            emitLdcInt(c);
            super.visitInsn(Opcodes.ISUB);
            return true;
        }

        private void emitXorInt(int value) {
            int obfuscated = value ^ intKey;
            super.visitLdcInsn(obfuscated);
            super.visitLdcInsn(intKey);
            super.visitInsn(Opcodes.IXOR);
        }
    }
}
