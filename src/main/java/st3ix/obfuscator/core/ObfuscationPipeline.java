package st3ix.obfuscator.core;

import st3ix.obfuscator.config.ObfuscatorConfig;
import st3ix.obfuscator.io.JarProcessor;
import st3ix.obfuscator.io.JarProcessor.ClassEntry;
import st3ix.obfuscator.io.JarProcessor.JarContents;
import st3ix.obfuscator.io.JarProcessor.ResourceEntry;
import st3ix.obfuscator.log.Logger;
import st3ix.obfuscator.transform.ClassMapping;
import st3ix.obfuscator.transform.ClassRenamer;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
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
            Logger.info("Class renaming disabled by config. Copying JAR without renaming.");
            List<ResourceEntry> resources = contents.manifest() != null
                ? contents.resources().stream()
                    .filter(r -> !"META-INF/MANIFEST.MF".equalsIgnoreCase(r.path()))
                    .toList()
                : contents.resources();
            JarProcessor.write(outputPath, contents.classes(), resources, contents.manifest());
            Logger.info("Obfuscation complete.");
            return;
        }

        List<String> classNames = contents.classes().stream()
            .map(ClassEntry::internalName)
            .sorted(Comparator.comparing(s -> s.replace('$', '\uFFFF')))
            .toList();

        Logger.info("Building class mapping (%d classes)", classNames.size());
        ClassMapping mapping = new ClassMapping();
        mapping.addExcludes(config.excludeClasses());
        for (String name : classNames) {
            mapping.map(name);
        }

        Logger.step("Step 1/2: Class renaming");
        ClassRenamer renamer = new ClassRenamer(mapping);
        List<ClassEntry> transformed = new ArrayList<>();
        int renamedCount = 0;
        for (ClassEntry ce : contents.classes()) {
            String newInternal = mapping.getNewName(ce.internalName());
            boolean renamed = !ce.internalName().equals(newInternal);
            if (renamed) {
                Logger.info("  Class renamed: %s -> %s", ce.internalName().replace('/', '.'), newInternal.replace('/', '.'));
                renamedCount++;
            }
            byte[] transformedBytes = renamer.transform(ce.bytes());
            String newPath = newInternal + ".class";
            transformed.add(new ClassEntry(newPath, newInternal, transformedBytes));
        }
        Logger.info("Class renaming successful. Renamed a total of %d class(es)", renamedCount);

        Logger.step("Step 2/2: Writing output JAR: %s", outputPath);
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
