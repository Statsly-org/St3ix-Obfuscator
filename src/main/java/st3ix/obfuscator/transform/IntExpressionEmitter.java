package st3ix.obfuscator.transform;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.Random;

/**
 * Emits bytecode for an int value as a math expression (e.g. (50+59) for 109)
 * so decompilers show expressions instead of literals.
 * Used by NumberObfuscator and StringObfuscator (char-code construction).
 */
public final class IntExpressionEmitter {

    private final MethodVisitor mv;
    private final Random rng;

    public IntExpressionEmitter(MethodVisitor mv, Random rng) {
        this.mv = mv;
        this.rng = rng;
    }

    /**
     * Emits bytecode that leaves the given int value on the stack.
     * Uses math expressions where possible, falls back to XOR for edge cases.
     */
    public void emit(int value) {
        if (tryEmitExpression(value)) {
            return;
        }
        emitXor(value);
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

    private static final int XOR_KEY = 0x5A5A5A5A;

    private void emitXor(int value) {
        int obfuscated = value ^ XOR_KEY;
        mv.visitLdcInsn(obfuscated);
        mv.visitLdcInsn(XOR_KEY);
        mv.visitInsn(Opcodes.IXOR);
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
                mv.visitInsn(opcode);
                return;
            }
        }
        if (v >= Byte.MIN_VALUE && v <= Byte.MAX_VALUE) {
            mv.visitIntInsn(Opcodes.BIPUSH, v);
        } else if (v >= Short.MIN_VALUE && v <= Short.MAX_VALUE) {
            mv.visitIntInsn(Opcodes.SIPUSH, v);
        } else {
            mv.visitLdcInsn(v);
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
        mv.visitInsn(Opcodes.IADD);
        return true;
    }

    private boolean emitMulAdd(int value) {
        if (value == 0) {
            emitLdcInt(1);
            emitLdcInt(0);
            mv.visitInsn(Opcodes.IMUL);
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
        mv.visitInsn(Opcodes.IMUL);
        if (c != 0) {
            emitLdcInt(c);
            mv.visitInsn(Opcodes.IADD);
        }
        return true;
    }

    private boolean emitSub(int value) {
        int k = rng != null ? rng.nextInt(500) + 1 : Math.abs(value % 100) + 1;
        long a = (long) value + (long) k;
        if (a < Integer.MIN_VALUE || a > Integer.MAX_VALUE) return false;
        emitLdcInt((int) a);
        emitLdcInt(k);
        mv.visitInsn(Opcodes.ISUB);
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
        mv.visitInsn(Opcodes.ISHL);
        if (b != 0) {
            emitLdcInt(b);
            mv.visitInsn(Opcodes.IADD);
        }
        return true;
    }

    private boolean emitMulSub(int value) {
        if (value == Integer.MIN_VALUE) {
            emitLdcInt(1);
            emitLdcInt(31);
            mv.visitInsn(Opcodes.ISHL);
            return true;
        }
        if (value == Integer.MAX_VALUE) {
            emitLdcInt(1);
            emitLdcInt(31);
            mv.visitInsn(Opcodes.ISHL);
            emitLdcInt(1);
            mv.visitInsn(Opcodes.ISUB);
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
        mv.visitInsn(Opcodes.IMUL);
        emitLdcInt(c);
        mv.visitInsn(Opcodes.ISUB);
        return true;
    }
}
