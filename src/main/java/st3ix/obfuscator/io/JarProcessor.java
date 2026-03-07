package st3ix.obfuscator.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

/**
 * Reads a JAR file and provides access to class bytes and resources.
 */
public final class JarProcessor {

    /**
     * Reads all entries from a JAR. Returns classes and resources for processing.
     */
    public static JarContents read(Path jarPath) throws IOException {
        List<ClassEntry> classes = new ArrayList<>();
        List<ResourceEntry> resources = new ArrayList<>();
        Manifest manifest = null;

        try (JarFile jar = new JarFile(jarPath.toFile())) {
            manifest = jar.getManifest();
            var entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String name = entry.getName();
                if (name.endsWith("/")) continue;
                byte[] bytes = readAll(jar.getInputStream(entry));
                if (name.endsWith(".class")) {
                    String internalName = name.substring(0, name.length() - 6);
                    classes.add(new ClassEntry(name, internalName, bytes));
                } else {
                    resources.add(new ResourceEntry(name, bytes));
                }
            }
        }
        return new JarContents(classes, resources, manifest);
    }

    /**
     * Writes classes and resources to a JAR file.
     *
     * @param outputPath   target JAR path
     * @param classes      list of (path, bytes) – path e.g. "a.class"
     * @param resources    resources to copy
     * @param manifest    manifest (may be null)
     */
    public static void write(Path outputPath, List<ClassEntry> classes,
                            List<ResourceEntry> resources, Manifest manifest) throws IOException {
        if (outputPath == null) {
            throw new IllegalArgumentException("Output path cannot be null");
        }
        Path parent = outputPath.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        try (OutputStream out = Files.newOutputStream(outputPath);
             JarOutputStream jar = manifest != null
                 ? new JarOutputStream(out, manifest)
                 : new JarOutputStream(out)) {
            for (ClassEntry ce : classes) {
                JarEntry entry = new JarEntry(ce.path());
                entry.setSize(ce.bytes().length);
                jar.putNextEntry(entry);
                jar.write(ce.bytes());
                jar.closeEntry();
            }
            for (ResourceEntry re : resources) {
                JarEntry entry = new JarEntry(re.path());
                entry.setSize(re.bytes().length);
                jar.putNextEntry(entry);
                jar.write(re.bytes());
                jar.closeEntry();
            }
        }
    }

    private static byte[] readAll(InputStream in) throws IOException {
        return in.readAllBytes();
    }

    public record ClassEntry(String path, String internalName, byte[] bytes) {}
    public record ResourceEntry(String path, byte[] bytes) {}
    public record JarContents(List<ClassEntry> classes, List<ResourceEntry> resources, Manifest manifest) {}
}
