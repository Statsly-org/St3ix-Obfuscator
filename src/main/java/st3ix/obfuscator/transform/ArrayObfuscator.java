package st3ix.obfuscator.transform;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;

import java.util.Random;

/**
 * Obfuscates array creation by hiding the dimension (size) with XOR.
 * Transforms new Type[n] so that n is stored as (n ^ key) ^ key in bytecode.
 * Uses tree API to avoid ASM frame computation issues.
 */
public final class ArrayObfuscator {

    private static final int DEFAULT_INT_KEY = 0x7B3C9E1A;

    private final int intKey;

    /**
     * Creates an obfuscator with default (fixed) key.
     */
    public ArrayObfuscator() {
        this.intKey = DEFAULT_INT_KEY;
    }

    /**
     * Creates an obfuscator with random key (different per instance).
     */
    public static ArrayObfuscator withRandomKey() {
        return new ArrayObfuscator(new Random().nextInt());
    }

    private ArrayObfuscator(int intKey) {
        this.intKey = intKey;
    }

    /**
     * Transforms the class bytes, obfuscating array dimensions.
     */
    public byte[] transform(byte[] classBytes) {
        ClassNode classNode = new ClassNode();
        new ClassReader(classBytes).accept(classNode, ClassReader.SKIP_FRAMES);

        for (MethodNode method : classNode.methods) {
            transformMethod(method);
        }

        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        classNode.accept(writer);
        return writer.toByteArray();
    }

    private void transformMethod(MethodNode method) {
        InsnList insns = method.instructions;
        if (insns.size() < 2) return;

        AbstractInsnNode cur = insns.getFirst();
        while (cur != null) {
            AbstractInsnNode next = cur.getNext();
            if (next != null) {
                int value = getIntValue(cur);
                if (value != Integer.MIN_VALUE && getArrayType(next) != 0) {
                    InsnList replacement = createXorSequence(value);
                    insns.insert(cur, replacement);
                    insns.remove(cur);
                    cur = next;
                }
            }
            cur = cur.getNext();
        }
    }

    private int getIntValue(AbstractInsnNode insn) {
        if (insn instanceof InsnNode in) {
            return switch (in.getOpcode()) {
                case Opcodes.ICONST_M1 -> -1;
                case Opcodes.ICONST_0 -> 0;
                case Opcodes.ICONST_1 -> 1;
                case Opcodes.ICONST_2 -> 2;
                case Opcodes.ICONST_3 -> 3;
                case Opcodes.ICONST_4 -> 4;
                case Opcodes.ICONST_5 -> 5;
                default -> Integer.MIN_VALUE;
            };
        }
        if (insn instanceof IntInsnNode in) {
            if (in.getOpcode() == Opcodes.BIPUSH || in.getOpcode() == Opcodes.SIPUSH) {
                return in.operand;
            }
        }
        if (insn instanceof LdcInsnNode in) {
            if (in.cst instanceof Integer i) return i;
        }
        return Integer.MIN_VALUE;
    }

    private int getArrayType(AbstractInsnNode insn) {
        if (insn instanceof IntInsnNode in && in.getOpcode() == Opcodes.NEWARRAY) {
            return 1;
        }
        if (insn instanceof TypeInsnNode in && in.getOpcode() == Opcodes.ANEWARRAY) {
            return 1;
        }
        return 0;
    }

    private InsnList createXorSequence(int value) {
        int obfuscated = value ^ intKey;
        InsnList list = new InsnList();
        list.add(new LdcInsnNode(obfuscated));
        list.add(new LdcInsnNode(intKey));
        list.add(new InsnNode(Opcodes.IXOR));
        return list;
    }
}
