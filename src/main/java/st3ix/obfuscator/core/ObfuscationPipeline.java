package st3ix.obfuscator.core;

import st3ix.obfuscator.config.ObfuscatorConfig;
import st3ix.obfuscator.io.JarProcessor;
import st3ix.obfuscator.io.JarProcessor.ClassEntry;
import st3ix.obfuscator.io.JarProcessor.JarContents;
import st3ix.obfuscator.io.JarProcessor.ResourceEntry;
import st3ix.obfuscator.log.Logger;
import st3ix.obfuscator.transform.ArrayObfuscator;
import st3ix.obfuscator.transform.BooleanObfuscator;
import st3ix.obfuscator.transform.ClassMapping;
import st3ix.obfuscator.transform.FlowObfuscator;
import st3ix.obfuscator.transform.ClassRenamer;
import st3ix.obfuscator.transform.DebugInfoStripper;
import st3ix.obfuscator.transform.FieldMapping;
import st3ix.obfuscator.transform.FieldRenamer;
import st3ix.obfuscator.transform.MethodMapping;
import st3ix.obfuscator.transform.MethodRenamer;
import st3ix.obfuscator.transform.NumberObfuscator;
import st3ix.obfuscator.transform.StringObfuscator;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

/**
 * Orchestrates the obfuscation transformation pipeline.
 */
public final class ObfuscationPipeline {

    /**
     * Runs obfuscation on the input JAR and writes to the output path.
     */
    public void run(Path inputPath, Path outputPath, ObfuscatorConfig config) throws IOException {
        Logger.info("Reading input JAR: %s", inputPath);
        JarContents contents = JarProcessor.read(inputPath);

        if (!config.classRenamingEnabled()) {
            Logger.info("Class renaming disabled by config.");
            List<ClassEntry> classesToWrite = contents.classes();
            boolean hasTransforms = config.methodRenamingEnabled() || config.fieldRenamingEnabled() || config.numberObfuscationEnabled()
                || config.arrayObfuscationEnabled() || config.booleanObfuscationEnabled()
                || config.stringObfuscationEnabled() || config.debugInfoStrippingEnabled();
            if (hasTransforms) {
                int stepNum = 1;
                int totalSteps = (config.methodRenamingEnabled() ? 1 : 0) + (config.fieldRenamingEnabled() ? 1 : 0)
                    + (config.numberObfuscationEnabled() ? 1 : 0) + (config.arrayObfuscationEnabled() ? 1 : 0)
                    + (config.booleanObfuscationEnabled() ? 1 : 0) + (config.stringObfuscationEnabled() ? 1 : 0)
                    + (config.flowObfuscationEnabled() ? 1 : 0) + (config.debugInfoStrippingEnabled() ? 1 : 0) + 1;
                if (config.methodRenamingEnabled()) {
                    Logger.step("Step %d/%d: Method renaming", stepNum++, totalSteps);
                    MethodMapping methodMapping = new MethodMapping(config.methodNamesRandom(), config.methodNameLength(),
                        config.methodNamesHomoglyph(), config.methodNamesInvisibleChars());
                    methodMapping.addExcludes(config.excludeClasses());
                    methodMapping.buildFrom(contents.classes());
                    for (var e : methodMapping.getRenameEntries()) {
                        Logger.info("  Method renamed: %s.%s -> %s", e.className(), e.oldName(), e.newName());
                    }
                    MethodRenamer methodRenamer = new MethodRenamer(methodMapping);
                    classesToWrite = contents.classes().stream()
                        .map(ce -> new ClassEntry(ce.path(), ce.internalName(), methodRenamer.transform(ce.bytes())))
                        .toList();
                    Logger.success("Method renaming applied (%d method(s))", methodMapping.getRenameEntries().size());
                }
                if (config.fieldRenamingEnabled()) {
                    Logger.step("Step %d/%d: Field renaming", stepNum++, totalSteps);
                    FieldMapping fieldMapping = new FieldMapping(config.fieldNamesRandom(), config.fieldNameLength(),
                        config.fieldNamesHomoglyph(), config.fieldNamesInvisibleChars());
                    fieldMapping.addExcludes(config.excludeClasses());
                    fieldMapping.buildFrom(classesToWrite);
                    for (var e : fieldMapping.getRenameEntries()) {
                        Logger.info("  Field renamed: %s.%s -> %s", e.className(), e.oldName(), e.newName());
                    }
                    FieldRenamer fieldRenamer = new FieldRenamer(fieldMapping);
                    classesToWrite = classesToWrite.stream()
                        .map(ce -> new ClassEntry(ce.path(), ce.internalName(), fieldRenamer.transform(ce.bytes())))
                        .toList();
                    Logger.success("Field renaming applied (%d field(s))", fieldMapping.getRenameEntries().size());
                }
                if (config.numberObfuscationEnabled()) {
                    Logger.step("Step %d/%d: Number obfuscation", stepNum++, totalSteps);
                    NumberObfuscator no = config.numberKeyRandom() ? NumberObfuscator.withRandomKey() : new NumberObfuscator();
                    classesToWrite = classesToWrite.stream()
                        .map(ce -> new ClassEntry(ce.path(), ce.internalName(), no.transform(ce.bytes())))
                        .toList();
                    Logger.success("Number obfuscation applied");
                }
                if (config.arrayObfuscationEnabled()) {
                    Logger.step("Step %d/%d: Array obfuscation", stepNum++, totalSteps);
                    ArrayObfuscator ao = config.arrayKeyRandom() ? ArrayObfuscator.withRandomKey() : new ArrayObfuscator();
                    classesToWrite = classesToWrite.stream()
                        .map(ce -> new ClassEntry(ce.path(), ce.internalName(), ao.transform(ce.bytes())))
                        .toList();
                    Logger.success("Array obfuscation applied");
                }
                if (config.booleanObfuscationEnabled()) {
                    Logger.step("Step %d/%d: Boolean obfuscation", stepNum++, totalSteps);
                    BooleanObfuscator bo = config.booleanKeyRandom() ? BooleanObfuscator.withRandomKey() : new BooleanObfuscator();
                    classesToWrite = classesToWrite.stream()
                        .map(ce -> new ClassEntry(ce.path(), ce.internalName(), bo.transform(ce.bytes())))
                        .toList();
                    Logger.success("Boolean obfuscation applied");
                }
                if (config.stringObfuscationEnabled()) {
                    Logger.step("Step %d/%d: String obfuscation", stepNum++, totalSteps);
                    StringObfuscator so = config.stringKeyRandom() ? StringObfuscator.withRandomKey() : new StringObfuscator();
                    classesToWrite = classesToWrite.stream()
                        .map(ce -> new ClassEntry(ce.path(), ce.internalName(), so.transform(ce.bytes())))
                        .toList();
                    Logger.success("String obfuscation applied");
                }
                if (config.flowObfuscationEnabled()) {
                    Logger.step("Step %d/%d: Flow obfuscation", stepNum++, totalSteps);
                    FlowObfuscator fo = config.flowKeyRandom() ? FlowObfuscator.withRandomKey() : new FlowObfuscator();
                    classesToWrite = classesToWrite.stream()
                        .map(ce -> new ClassEntry(ce.path(), ce.internalName(), fo.transform(ce.bytes())))
                        .toList();
                    Logger.success("Flow obfuscation applied");
                }
                if (config.debugInfoStrippingEnabled()) {
                    Logger.step("Step %d/%d: Debug info stripping", stepNum++, totalSteps);
                    DebugInfoStripper dis = new DebugInfoStripper();
                    classesToWrite = classesToWrite.stream()
                        .map(ce -> new ClassEntry(ce.path(), ce.internalName(), dis.transform(ce.bytes())))
                        .toList();
                    Logger.success("Debug info stripping applied");
                }
                Logger.step("Step %d/%d: Writing output JAR: %s", stepNum, totalSteps, outputPath);
            } else {
                Logger.info("Copying JAR without transformations.");
            }
            List<ResourceEntry> resources = contents.manifest() != null
                ? contents.resources().stream()
                    .filter(r -> !"META-INF/MANIFEST.MF".equalsIgnoreCase(r.path()))
                    .toList()
                : contents.resources();
            JarProcessor.write(outputPath, classesToWrite, resources, contents.manifest());
            Logger.info("Obfuscation complete.");
            return;
        }

        List<String> classNames = contents.classes().stream()
            .map(ClassEntry::internalName)
            .sorted(Comparator.comparing(s -> s.replace('$', '\uFFFF')))
            .toList();

        Logger.info("Building class mapping (%d classes)", classNames.size());
        ClassMapping mapping = new ClassMapping(config.classNamesRandom(), config.classNameLength(),
            config.classNamesHomoglyph(), config.classNamesInvisibleChars());
        mapping.addExcludes(config.excludeClasses());
        for (String name : classNames) {
            mapping.map(name);
        }

        boolean methodObfEnabled = config.methodRenamingEnabled();
        boolean fieldObfEnabled = config.fieldRenamingEnabled();
        boolean numberObfEnabled = config.numberObfuscationEnabled();
        boolean arrayObfEnabled = config.arrayObfuscationEnabled();
        boolean booleanObfEnabled = config.booleanObfuscationEnabled();
        boolean stringObfEnabled = config.stringObfuscationEnabled();
        boolean flowObfEnabled = config.flowObfuscationEnabled();
        boolean debugInfoStrippingEnabled = config.debugInfoStrippingEnabled();
        int totalSteps = 2 + (methodObfEnabled ? 1 : 0) + (fieldObfEnabled ? 1 : 0) + (numberObfEnabled ? 1 : 0) + (arrayObfEnabled ? 1 : 0)
            + (booleanObfEnabled ? 1 : 0) + (stringObfEnabled ? 1 : 0) + (flowObfEnabled ? 1 : 0) + (debugInfoStrippingEnabled ? 1 : 0);
        int stepNum = 1;

        MethodMapping methodMapping = null;
        MethodRenamer methodRenamer = null;
        FieldMapping fieldMapping = null;
        FieldRenamer fieldRenamer = null;
        if (methodObfEnabled) {
            Logger.step("Step %d/%d: Method renaming", stepNum++, totalSteps);
            methodMapping = new MethodMapping(config.methodNamesRandom(), config.methodNameLength(),
                config.methodNamesHomoglyph(), config.methodNamesInvisibleChars());
            methodMapping.addExcludes(config.excludeClasses());
            methodMapping.buildFrom(contents.classes());
            for (var e : methodMapping.getRenameEntries()) {
                Logger.info("  Method renamed: %s.%s -> %s", e.className(), e.oldName(), e.newName());
            }
            methodRenamer = new MethodRenamer(methodMapping);
            Logger.success("Method renaming applied (%d method(s))", methodMapping.getRenameEntries().size());
        }

        if (fieldObfEnabled) {
            Logger.step("Step %d/%d: Field renaming", stepNum++, totalSteps);
            fieldMapping = new FieldMapping(config.fieldNamesRandom(), config.fieldNameLength(),
                config.fieldNamesHomoglyph(), config.fieldNamesInvisibleChars());
            fieldMapping.addExcludes(config.excludeClasses());
            fieldMapping.buildFrom(contents.classes());
            for (var e : fieldMapping.getRenameEntries()) {
                Logger.info("  Field renamed: %s.%s -> %s", e.className(), e.oldName(), e.newName());
            }
            fieldRenamer = new FieldRenamer(fieldMapping);
            Logger.success("Field renaming applied (%d field(s))", fieldMapping.getRenameEntries().size());
        }

        Logger.step("Step %d/%d: Class renaming", stepNum++, totalSteps);
        ClassRenamer renamer = new ClassRenamer(mapping);
        NumberObfuscator numberObfuscator = numberObfEnabled
            ? (config.numberKeyRandom() ? NumberObfuscator.withRandomKey() : new NumberObfuscator())
            : null;
        ArrayObfuscator arrayObfuscator = arrayObfEnabled
            ? (config.arrayKeyRandom() ? ArrayObfuscator.withRandomKey() : new ArrayObfuscator())
            : null;
        BooleanObfuscator booleanObfuscator = booleanObfEnabled
            ? (config.booleanKeyRandom() ? BooleanObfuscator.withRandomKey() : new BooleanObfuscator())
            : null;
        StringObfuscator stringObfuscator = stringObfEnabled
            ? (config.stringKeyRandom() ? StringObfuscator.withRandomKey() : new StringObfuscator())
            : null;
        FlowObfuscator flowObfuscator = flowObfEnabled
            ? (config.flowKeyRandom() ? FlowObfuscator.withRandomKey() : new FlowObfuscator())
            : null;
        List<ClassEntry> transformed = new ArrayList<>();
        int renamedCount = 0;
        for (ClassEntry ce : contents.classes()) {
            String newInternal = mapping.getNewName(ce.internalName());
            boolean renamed = !ce.internalName().equals(newInternal);
            if (renamed) {
                Logger.info("  Class renamed: %s -> %s", ce.internalName().replace('/', '.'), newInternal.replace('/', '.'));
                renamedCount++;
            }
            byte[] bytes = ce.bytes();
            String cn = ce.internalName().replace('/', '.');
            Logger.info("  Transforming: %s", cn);
            if (methodObfEnabled) {
                bytes = methodRenamer.transform(bytes);
            }
            if (fieldObfEnabled) {
                bytes = fieldRenamer.transform(bytes);
            }
            bytes = renamer.transform(bytes);
            if (numberObfEnabled) {
                bytes = numberObfuscator.transform(bytes);
            }
            if (arrayObfEnabled) {
                bytes = arrayObfuscator.transform(bytes);
            }
            if (booleanObfEnabled) {
                bytes = booleanObfuscator.transform(bytes);
            }
            if (stringObfEnabled) {
                bytes = stringObfuscator.transform(bytes);
            }
            if (flowObfEnabled) {
                try {
                    Logger.flowTransform("  Flow transform: %s ...", cn);
                    ExecutorService exec = Executors.newSingleThreadExecutor();
                    byte[] inputBytes = bytes;
                    Future<byte[]> future = exec.submit(() -> flowObfuscator.transform(inputBytes));
                    try {
                        bytes = future.get(30, TimeUnit.SECONDS);
                        Logger.flowTransform("  Flow transform: %s OK", cn);
                    } catch (TimeoutException e) {
                        future.cancel(true);
                        Logger.warn(String.format("  Flow obfuscation timed out for %s (skipped)", cn));
                    } finally {
                        exec.shutdownNow();
                    }
                } catch (Throwable ex) {
                    Logger.warn(String.format("  Flow obfuscation skipped for %s: %s", cn, ex.getMessage()));
                    ex.printStackTrace(System.err);
                }
            }
            if (debugInfoStrippingEnabled) {
                bytes = new DebugInfoStripper().transform(bytes);
            }
            String newPath = newInternal + ".class";
            transformed.add(new ClassEntry(newPath, newInternal, bytes));
        }
        Logger.success("Class renaming successful. Renamed a total of %d class(es)", renamedCount);

        if (numberObfEnabled) {
            Logger.step("Step %d/%d: Number obfuscation", stepNum++, totalSteps);
            Logger.success("Number obfuscation applied");
        }
        if (arrayObfEnabled) {
            Logger.step("Step %d/%d: Array obfuscation", stepNum++, totalSteps);
            Logger.success("Array obfuscation applied");
        }
        if (booleanObfEnabled) {
            Logger.step("Step %d/%d: Boolean obfuscation", stepNum++, totalSteps);
            Logger.success("Boolean obfuscation applied");
        }
        if (stringObfEnabled) {
            Logger.step("Step %d/%d: String obfuscation", stepNum++, totalSteps);
            Logger.success("String obfuscation applied");
        }
        if (flowObfEnabled) {
            Logger.step("Step %d/%d: Flow obfuscation", stepNum++, totalSteps);
            Logger.success("Flow obfuscation applied");
        }
        if (debugInfoStrippingEnabled) {
            Logger.step("Step %d/%d: Debug info stripping", stepNum++, totalSteps);
            Logger.success("Debug info stripping applied");
        }

        Logger.step("Step %d/%d: Writing output JAR: %s", stepNum, totalSteps, outputPath);
        Manifest manifest = updateMainClass(contents.manifest(), mapping);
        List<ResourceEntry> resourcesToWrite = contents.resources().stream()
            .filter(r -> !"META-INF/MANIFEST.MF".equalsIgnoreCase(r.path()))
            .toList();
        JarProcessor.write(outputPath, transformed, resourcesToWrite, manifest);
        Logger.info("Obfuscation complete.");
    }

    private Manifest updateMainClass(Manifest manifest, ClassMapping mapping) {
        if (manifest == null) return null;
        String mainClass = manifest.getMainAttributes().getValue(Attributes.Name.MAIN_CLASS);
        if (mainClass == null) return manifest;
        Manifest copy = new Manifest(manifest);
        copy.getMainAttributes().put(Attributes.Name.MAIN_CLASS, mapping.toBinaryName(mainClass));
        return copy;
    }
}
