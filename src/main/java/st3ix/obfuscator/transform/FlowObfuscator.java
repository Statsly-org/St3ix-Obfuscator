package st3ix.obfuscator.transform;

import org.objectweb.asm.Label;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TableSwitchInsnNode;
import org.objectweb.asm.tree.LookupSwitchInsnNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import st3ix.obfuscator.log.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * Control flow obfuscation via flattening.
 * Converts method control flow into a switch-dispatcher structure so decompilers
 * show a flat switch instead of the original branching logic.
 * Skips constructors, static init, native/abstract methods, and methods with try-catch.
 */
public final class FlowObfuscator {

    private static final int DEFAULT_KEY = 0x3B9A7C2E;
    private static final int MIN_INSTRUCTIONS = 4;
    private static final int CHUNK_SIZE = 5;

    private final int key;

    public FlowObfuscator() {
        this.key = DEFAULT_KEY;
    }

    public static FlowObfuscator withRandomKey() {
        return new FlowObfuscator(new Random().nextInt());
    }

    private FlowObfuscator(int key) {
        this.key = key;
    }

    public byte[] transform(byte[] classBytes) {
        ClassNode classNode = new ClassNode();
        ClassReader reader = new ClassReader(classBytes);
        reader.accept(classNode, ClassReader.EXPAND_FRAMES);

        for (Object m : classNode.methods) {
            transformMethod(classNode.name, (MethodNode) m);
        }

        try {
            // ClassWriter with ClassReader for getCommonSuperClass - needed for correct frame merge with COMPUTE_FRAMES
            ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_FRAMES);
            classNode.accept(writer);
            return writer.toByteArray();
        } catch (Throwable t) {
            String msg = t.getMessage();
            if (msg == null) msg = t.getClass().getSimpleName();
            Logger.warn(String.format("  Flow ClassWriter failed, skipping: %s", msg));
            return classBytes;
        }
    }

    private void transformMethod(String className, MethodNode method) {
        if ("<init>".equals(method.name) || "<clinit>".equals(method.name)) return;
        if ((method.access & (Opcodes.ACC_NATIVE | Opcodes.ACC_ABSTRACT)) != 0) return;
        if (method.tryCatchBlocks != null && !method.tryCatchBlocks.isEmpty()) return;
        if (method.instructions == null || method.instructions.size() < MIN_INSTRUCTIONS) {
            Logger.flowSkipped("%s.%s (too few instructions)", className.replace('/', '.'), method.name);
            return;
        }

        List<BasicBlock> blocks = buildBasicBlocks(method);
        if (blocks.size() < 2) {
            Logger.flowSkipped("%s.%s (only %d block(s))", className.replace('/', '.'), method.name, blocks.size());
            return;
        }

        Map<LabelNode, Integer> labelToBlock = new HashMap<>();
        for (int i = 0; i < blocks.size(); i++) {
            if (blocks.get(i).startLabel != null) {
                labelToBlock.put(blocks.get(i).startLabel, i);
            }
        }

        Map<Integer, Integer> blockToNext = computeNextBlocks(blocks, labelToBlock);
        if (blockToNext == null) {
            Logger.flowSkipped("%s.%s (unsupported control flow)", className.replace('/', '.'), method.name);
            return;
        }

        int stateVar = method.maxLocals;
        if (stateVar <= 0) {
            stateVar = Type.getArgumentsAndReturnSizes(method.desc) >> 2;
        }
        method.maxLocals++;

        InsnList newInsns = new InsnList();

        org.objectweb.asm.tree.LabelNode dispatcherLabelNode = new org.objectweb.asm.tree.LabelNode();
        List<org.objectweb.asm.tree.LabelNode> blockLabelNodes = new ArrayList<>(blocks.size() + 1);
        for (int i = 0; i < blocks.size(); i++) {
            blockLabelNodes.add(new org.objectweb.asm.tree.LabelNode());
        }
        org.objectweb.asm.tree.LabelNode exitLabelNode = new org.objectweb.asm.tree.LabelNode();

        int firstObfState = obfuscateState(0);
        emitConst(newInsns, firstObfState);
        newInsns.add(new VarInsnNode(Opcodes.ISTORE, stateVar));

        newInsns.add(dispatcherLabelNode);
        newInsns.add(new VarInsnNode(Opcodes.ILOAD, stateVar));
        newInsns.add(new LdcInsnNode(key));
        newInsns.add(new InsnNode(Opcodes.IXOR));

        org.objectweb.asm.tree.LabelNode[] switchLabels = new org.objectweb.asm.tree.LabelNode[blocks.size() + 1];
        for (int i = 0; i < blocks.size(); i++) {
            switchLabels[i] = blockLabelNodes.get(i);
        }
        switchLabels[blocks.size()] = exitLabelNode;

        newInsns.add(new TableSwitchInsnNode(0, blocks.size(), exitLabelNode, switchLabels));

        for (int i = 0; i < blocks.size(); i++) {
            BasicBlock block = blocks.get(i);
            newInsns.add(blockLabelNodes.get(i));

            Integer next = blockToNext.get(i);
            AbstractInsnNode lastInBlock = block.instructions.isEmpty() ? null : block.instructions.get(block.instructions.size() - 1);
            boolean excludeLast = (next == null) || (next != null && lastInBlock instanceof JumpInsnNode jmp && jmp.getOpcode() == Opcodes.GOTO);
            copyBlockContent(newInsns, block, excludeLast);

            if (next != null) {
                int nextObf = obfuscateState(next);
                emitConst(newInsns, nextObf);
                newInsns.add(new VarInsnNode(Opcodes.ISTORE, stateVar));
                newInsns.add(new JumpInsnNode(Opcodes.GOTO, dispatcherLabelNode));
            } else {
                AbstractInsnNode term = block.instructions.get(block.instructions.size() - 1);
                // Skip cloning label-referencing instructions - clone(emptyMap) can return null or invalid node
                if (term instanceof JumpInsnNode || term instanceof TableSwitchInsnNode || term instanceof LookupSwitchInsnNode) continue;
                AbstractInsnNode cloned = term.clone(new HashMap<>());
                if (cloned != null) newInsns.add(cloned);
            }
        }

        newInsns.add(exitLabelNode);
        newInsns.add(new InsnNode(Opcodes.RETURN));

        method.instructions.clear();
        // Transfer node-by-node to avoid NPE in InsnList.add(InsnList) with complex structures
        AbstractInsnNode cursor = newInsns.getFirst();
        while (cursor != null) {
            AbstractInsnNode next = cursor.getNext();
            newInsns.remove(cursor);
            method.instructions.add(cursor);
            cursor = next;
        }
        // Clear stale debug info (references old labels) - avoids ClassWriter "Index 0 out of bounds"
        method.localVariables = null;
        method.parameters = null;
        Logger.flowFlattened("%s.%s (%d blocks)", className.replace('/', '.'), method.name, blocks.size());
    }

    private boolean returns(BasicBlock block) {
        if (block.instructions.isEmpty()) return false;
        AbstractInsnNode last = block.instructions.get(block.instructions.size() - 1);
        int op = last.getOpcode();
        return op == Opcodes.RETURN || op == Opcodes.ARETURN || op == Opcodes.IRETURN
            || op == Opcodes.LRETURN || op == Opcodes.FRETURN || op == Opcodes.DRETURN
            || op == Opcodes.ATHROW;
    }

    private void emitConst(InsnList list, int value) {
        if (value >= -1 && value <= 5) {
            int op = value == -1 ? Opcodes.ICONST_M1 :
                value == 0 ? Opcodes.ICONST_0 : value == 1 ? Opcodes.ICONST_1 :
                value == 2 ? Opcodes.ICONST_2 : value == 3 ? Opcodes.ICONST_3 :
                value == 4 ? Opcodes.ICONST_4 : Opcodes.ICONST_5;
            list.add(new InsnNode(op));
        } else if (value >= Byte.MIN_VALUE && value <= Byte.MAX_VALUE) {
            list.add(new IntInsnNode(Opcodes.BIPUSH, value));
        } else {
            list.add(new LdcInsnNode(value));
        }
    }

    private int obfuscateState(int state) {
        return state ^ key;
    }

    private static final Map<LabelNode, LabelNode> EMPTY_LABEL_MAP = Map.of();

    private void copyBlockContent(InsnList dest, BasicBlock block, boolean excludeLast) {
        int end = excludeLast ? block.instructions.size() - 1 : block.instructions.size();
        for (int i = 0; i < end; i++) {
            AbstractInsnNode insn = block.instructions.get(i);
            if (insn instanceof LabelNode || insn instanceof org.objectweb.asm.tree.LineNumberNode) continue;
            if (insn instanceof org.objectweb.asm.tree.FrameNode) continue;
            // Skip instructions that reference labels - ASM clone(map) needs map when labels are used
            if (insn instanceof JumpInsnNode || insn instanceof TableSwitchInsnNode || insn instanceof LookupSwitchInsnNode) continue;
            AbstractInsnNode cloned = insn.clone(EMPTY_LABEL_MAP);
            if (cloned != null) dest.add(cloned);
        }
    }

    private Map<Integer, Integer> computeNextBlocks(List<BasicBlock> blocks, Map<LabelNode, Integer> labelToBlock) {
        Map<Integer, Integer> next = new HashMap<>();
        for (int i = 0; i < blocks.size(); i++) {
            BasicBlock block = blocks.get(i);
            AbstractInsnNode last = block.instructions.get(block.instructions.size() - 1);
            if (isConditionalBranch(last)) {
                return null;
            }
            if (isTerminator(last)) {
                if (last instanceof JumpInsnNode) {
                    JumpInsnNode jmp = (JumpInsnNode) last;
                    if (jmp.getOpcode() != Opcodes.GOTO) return null;
                    LabelNode target = jmp.label;
                    Integer targetBlock = labelToBlock.get(target);
                    if (targetBlock != null) {
                        next.put(i, targetBlock);
                    }
                } else if (last instanceof TableSwitchInsnNode) {
                    return null;
                } else if (last instanceof LookupSwitchInsnNode) {
                    return null;
                }
            } else if (i + 1 < blocks.size()) {
                next.put(i, i + 1);
            }
        }
        // Reject methods with back-edges (loops) - COMPUTE_FRAMES produces invalid stack maps for them
        for (int i = 0; i < blocks.size(); i++) {
            Integer target = next.get(i);
            if (target != null && target <= i) {
                return null; // back-edge = loop, skip
            }
        }
        return next;
    }

    private boolean isConditionalBranch(AbstractInsnNode insn) {
        if (insn == null) return false;
        int op = insn.getOpcode();
        return op == Opcodes.IFEQ || op == Opcodes.IFNE || op == Opcodes.IFLT || op == Opcodes.IFGE
            || op == Opcodes.IFGT || op == Opcodes.IFLE || op == Opcodes.IF_ICMPEQ || op == Opcodes.IF_ICMPNE
            || op == Opcodes.IF_ICMPLT || op == Opcodes.IF_ICMPGE || op == Opcodes.IF_ICMPGT || op == Opcodes.IF_ICMPLE
            || op == Opcodes.IF_ACMPEQ || op == Opcodes.IF_ACMPNE || op == Opcodes.IFNULL || op == Opcodes.IFNONNULL
            || insn instanceof TableSwitchInsnNode || insn instanceof LookupSwitchInsnNode;
    }

    private boolean isTerminator(AbstractInsnNode insn) {
        if (insn == null) return false;
        switch (insn.getOpcode()) {
            case Opcodes.RETURN:
            case Opcodes.ARETURN:
            case Opcodes.IRETURN:
            case Opcodes.LRETURN:
            case Opcodes.FRETURN:
            case Opcodes.DRETURN:
            case Opcodes.ATHROW:
                return true;
            case Opcodes.GOTO:
            case Opcodes.IFEQ: case Opcodes.IFNE: case Opcodes.IFLT: case Opcodes.IFGE:
            case Opcodes.IFGT: case Opcodes.IFLE: case Opcodes.IF_ICMPEQ: case Opcodes.IF_ICMPNE:
            case Opcodes.IF_ICMPLT: case Opcodes.IF_ICMPGE: case Opcodes.IF_ICMPGT: case Opcodes.IF_ICMPLE:
            case Opcodes.IF_ACMPEQ: case Opcodes.IF_ACMPNE:
            case Opcodes.IFNULL: case Opcodes.IFNONNULL:
                return true;
            default:
                return insn instanceof TableSwitchInsnNode || insn instanceof LookupSwitchInsnNode;
        }
    }

    private List<BasicBlock> buildBasicBlocks(MethodNode method) {
        Set<LabelNode> branchTargets = new HashSet<>();
        AbstractInsnNode first = method.instructions.getFirst();
        if (first instanceof LabelNode) {
            branchTargets.add((LabelNode) first);
        }

        for (AbstractInsnNode insn : method.instructions) {
            if (insn instanceof JumpInsnNode) {
                branchTargets.add(((JumpInsnNode) insn).label);
            } else if (insn instanceof TableSwitchInsnNode) {
                TableSwitchInsnNode ts = (TableSwitchInsnNode) insn;
                branchTargets.add(ts.dflt);
                for (Object l : ts.labels) branchTargets.add((LabelNode) l);
            } else if (insn instanceof LookupSwitchInsnNode) {
                LookupSwitchInsnNode ls = (LookupSwitchInsnNode) insn;
                branchTargets.add(ls.dflt);
                for (Object l : ls.labels) branchTargets.add((LabelNode) l);
            }
        }

        List<BasicBlock> blocks = new ArrayList<>();
        BasicBlock current = null;

        for (AbstractInsnNode insn : method.instructions) {
            if (insn instanceof LabelNode && branchTargets.contains(insn)) {
                if (current != null && !current.instructions.isEmpty()) {
                    blocks.add(current);
                }
                current = new BasicBlock((LabelNode) insn);
            }
            if (current == null) {
                current = new BasicBlock(null);
            }
            current.instructions.add(insn);
        }
        if (current != null) blocks.add(current);

        // Linear methods (no branches) yield only 1 block - split artificially into chunks
        if (blocks.size() == 1) {
            blocks = splitLinearBlock(blocks.get(0));
        }

        return blocks;
    }

    private List<BasicBlock> splitLinearBlock(BasicBlock single) {
        List<AbstractInsnNode> real = new ArrayList<>();
        for (AbstractInsnNode insn : single.instructions) {
            if (insn instanceof LabelNode || insn instanceof org.objectweb.asm.tree.LineNumberNode)
                continue;
            if (insn instanceof org.objectweb.asm.tree.FrameNode) continue;
            real.add(insn);
        }
        if (real.size() < 4) return java.util.Collections.singletonList(single);

        // Find split points where stack is empty - required for valid bytecode (each block must
        // reach the dispatcher GOTO with empty stack, otherwise COMPUTE_FRAMES fails)
        List<Integer> splitIndices = new ArrayList<>();
        splitIndices.add(0);
        int stack = 0;
        for (int i = 0; i < real.size(); i++) {
            stack += stackDelta(real.get(i));
            if (stack == 0 && i + 1 < real.size() && splitIndices.size() < 32) {
                int lastSplit = splitIndices.get(splitIndices.size() - 1);
                if (i - lastSplit >= 1) splitIndices.add(i + 1);
            }
        }
        if (stack != 0 || splitIndices.size() < 2) return java.util.Collections.singletonList(single);
        splitIndices.add(real.size());

        List<BasicBlock> chunks = new ArrayList<>();
        for (int s = 0; s < splitIndices.size() - 1; s++) {
            BasicBlock chunk = new BasicBlock(null);
            for (int i = splitIndices.get(s); i < splitIndices.get(s + 1); i++) {
                chunk.instructions.add(real.get(i));
            }
            chunks.add(chunk);
        }
        return chunks.size() >= 2 ? chunks : java.util.Collections.singletonList(single);
    }

    /** Stack delta (pops negative, pushes positive) for finding stack-neutral split points. */
    private static int stackDelta(AbstractInsnNode insn) {
        if (insn == null) return 0;
        int op = insn.getOpcode();
        switch (op) {
            case Opcodes.NOP: case Opcodes.GOTO: case Opcodes.RETURN:
            case Opcodes.IFEQ: case Opcodes.IFNE: case Opcodes.IFLT: case Opcodes.IFGE:
            case Opcodes.IFGT: case Opcodes.IFLE: case Opcodes.IF_ICMPEQ: case Opcodes.IF_ICMPNE:
            case Opcodes.IF_ICMPLT: case Opcodes.IF_ICMPGE: case Opcodes.IF_ICMPGT: case Opcodes.IF_ICMPLE:
            case Opcodes.IF_ACMPEQ: case Opcodes.IF_ACMPNE: case Opcodes.IFNULL: case Opcodes.IFNONNULL:
            case Opcodes.MONITORENTER: case Opcodes.MONITOREXIT: case Opcodes.SWAP:
                return 0;
            case Opcodes.IRETURN: case Opcodes.FRETURN: case Opcodes.ARETURN:
            case Opcodes.ISTORE: case Opcodes.FSTORE: case Opcodes.ASTORE: case Opcodes.POP:
            case Opcodes.ATHROW:
                return -1;
            case Opcodes.LRETURN: case Opcodes.DRETURN:
            case Opcodes.LSTORE: case Opcodes.DSTORE: case Opcodes.POP2:
                return -2;
            case Opcodes.ICONST_M1: case Opcodes.ICONST_0: case Opcodes.ICONST_1: case Opcodes.ICONST_2:
            case Opcodes.ICONST_3: case Opcodes.ICONST_4: case Opcodes.ICONST_5:
            case Opcodes.FCONST_0: case Opcodes.FCONST_1: case Opcodes.FCONST_2:
            case Opcodes.ACONST_NULL: case Opcodes.ILOAD: case Opcodes.FLOAD: case Opcodes.ALOAD:
            case Opcodes.BIPUSH: case Opcodes.SIPUSH:
                return 1;
            case Opcodes.LCONST_0: case Opcodes.LCONST_1: case Opcodes.DCONST_0: case Opcodes.DCONST_1:
            case Opcodes.LLOAD: case Opcodes.DLOAD:
                return 2;
            case Opcodes.LDC:
                return (insn instanceof org.objectweb.asm.tree.LdcInsnNode ldc
                    && (ldc.cst instanceof Long || ldc.cst instanceof Double)) ? 2 : 1;
            case Opcodes.DUP: return 1;
            case Opcodes.DUP2: return 2;
            case Opcodes.IADD: case Opcodes.ISUB: case Opcodes.IMUL: case Opcodes.IDIV: case Opcodes.IREM:
            case Opcodes.IAND: case Opcodes.IOR: case Opcodes.IXOR: case Opcodes.ISHL: case Opcodes.ISHR:
            case Opcodes.IUSHR: case Opcodes.L2I: case Opcodes.I2L: case Opcodes.I2F: case Opcodes.I2D:
            case Opcodes.L2F: case Opcodes.L2D: case Opcodes.F2I: case Opcodes.F2L: case Opcodes.F2D:
            case Opcodes.D2I: case Opcodes.D2L: case Opcodes.D2F: case Opcodes.LCMP: case Opcodes.INEG:
            case Opcodes.I2B: case Opcodes.I2C: case Opcodes.I2S: case Opcodes.ARRAYLENGTH:
                return -1;
            case Opcodes.LADD: case Opcodes.LSUB: case Opcodes.LMUL: case Opcodes.LDIV: case Opcodes.LREM:
                return -2;
            case Opcodes.IALOAD: case Opcodes.LALOAD: case Opcodes.FALOAD: case Opcodes.DALOAD:
            case Opcodes.AALOAD: case Opcodes.BALOAD: case Opcodes.CALOAD: case Opcodes.SALOAD:
                return -1;
            default:
                if (op >= Opcodes.INVOKEVIRTUAL && op <= Opcodes.INVOKEDYNAMIC && insn instanceof org.objectweb.asm.tree.MethodInsnNode mi) {
                    int retSize = Type.getReturnType(mi.desc).getSize();
                    int argSize = Type.getArgumentTypes(mi.desc).length;
                    if (op != Opcodes.INVOKESTATIC && op != Opcodes.INVOKEDYNAMIC) argSize++;
                    return retSize - argSize;
                }
                if (op == Opcodes.NEW || op == Opcodes.GETSTATIC || op == Opcodes.GETFIELD) return 1;
                if (op == Opcodes.PUTSTATIC || op == Opcodes.PUTFIELD) return -1;
                return 0;
        }
    }

    private static final class BasicBlock {
        final LabelNode startLabel;
        final List<AbstractInsnNode> instructions = new ArrayList<>();

        BasicBlock(LabelNode startLabel) {
            this.startLabel = startLabel;
        }
    }
}
