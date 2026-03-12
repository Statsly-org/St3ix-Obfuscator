<p align="center">
  <img src="./Images/logo.png" alt="St3ix Obfuscator" width="200"/>
</p>

# St3ix Obfuscator

A Java bytecode obfuscator that transforms JAR files to make decompilation harder. Class names (including homoglyphs and invisible chars), numeric constants, booleans, strings, and array dimensions are obfuscated while preserving runtime behavior.

## Requirements

- **Java 17** or later
- **Gradle** (wrapper included)

## Quick Start

```bash
# Build the obfuscator (JAR + scripts)
./gradlew dist    # → build/dist/

# Obfuscate a JAR (output goes to build/dist/Obfuscate/)
java -jar build/dist/st3ix-obfuscator.jar -i myapp.jar -o myapp-obfuscated.jar

# Run the result
java -jar build/dist/Obfuscate/myapp-obfuscated.jar
```

**Windows:** Use `gradlew.bat` for building. For the GUI, run `run-gui.bat` from `build/dist/` or double-click the JAR.

Bei jedem Release gibt es ein ZIP-Archiv mit allem Nötigen: JAR, Batch-Datei zum Starten, Config-Beispiel, Images.

## Features

- **Class renaming** – Short or random names; configurable length
- **Method renaming** – Obfuscate method names; handles override chains
- **Field renaming** – Obfuscate field names; excludes serialVersionUID and enum constants
- **Homoglyph & invisible chars** – Unicode lookalikes (a→а) and zero-width chars; copy-paste fails
- **Number obfuscation** – Hides `int`, `long`, `float`, `double` with XOR
- **Array obfuscation** – Hides array dimensions
- **Boolean obfuscation** – Hides `true`/`false` literals
- **String obfuscation** – Encrypts string literals (XOR), decrypts at runtime
- **Debug info stripping** – Removes source names, line numbers, local variable names
- **Local variable renaming** *(planned)* – Obfuscate local variable names when debug kept
- **Random options** – Optional random keys and class/method names per build
- **Exclude patterns** – Skip JDK, Bukkit, Minecraft, and custom packages
- **YAML config** – `config.yml` next to the JAR

See [Features.md](Features.md) for the full list of current and planned features.

## Usage


| Option                  | Description                                                        |
| ----------------------- | ------------------------------------------------------------------ |
| `-i`, `--input <path>`  | Input JAR file                                                     |
| `-o`, `--output <path>` | Output filename (saved to `Obfuscate/` next to the obfuscator JAR) |
| `--max-ram <size>`      | Max heap hint (e.g. `512m`, `2g`) – use the launcher script        |
| `-h`, `--help`          | Show usage                                                         |


Output files are written to the `Obfuscate/` folder in the directory containing the obfuscator JAR. If a file with the same name exists, it is overwritten.

## Configuration

Copy `config.yml.example` to `config.yml` and place it next to `st3ix-obfuscator.jar`.

```yaml
classRenamingEnabled: true
numberObfuscationEnabled: true
arrayObfuscationEnabled: true
booleanObfuscationEnabled: true
stringObfuscationEnabled: true
debugInfoStrippingEnabled: true
classNamesRandom: false
classNameLength: 6
classNamesHomoglyph: false      # Cyrillic lookalikes (a→а)
classNamesInvisibleChars: false # Zero-width chars in names
numberKeyRandom: false
arrayKeyRandom: false
booleanKeyRandom: false
stringKeyRandom: false
excludeClasses:
  - com.myapp.sensitive
```

See `config.yml.example` for all options.

## Project Structure

```
src/main/java/st3ix/obfuscator/
├── api/          – Public API (if exposed)
├── cli/          – Command-line interface
├── config/       – Configuration loading
├── core/         – Obfuscation pipeline
├── io/           – JAR reading and writing
├── log/          – Logging
└── transform/    – Bytecode transformers
```

## Example Project

The `Example/Java Project` folder contains a demo with an `obfuscate` Gradle task.

```bash
cd "Example/Java Project"
./gradlew obfuscate
java -jar ../../build/dist/Obfuscate/example-java-project-obfuscated.jar
```

Or run `test-obfuscate.bat` from the project root for a full build and test.

## Before & After Example

**Before obfuscation** (decompiled):

```java
// example/Main.java
package example;

public final class Main {
    public static void main(String[] args) {
        System.out.println("Example project running.");
        DemoService service = new DemoService();
        service.run();
    }
}

// example/DemoService.java
public final class DemoService {
    private static final String SECRET_KEY = "my-secret-key-12345";
    private int counter;
    
    public void run() {
        counter++;
        int port = 25565;
        int seed = 12345;
        boolean flag = true;
        System.out.println("port=" + port + ", seed=" + seed);
    }
}
```

**After obfuscation** (class, method, number, string, debug stripping, homoglyph):

```java
// а/b.java  (Cyrillic а – looks like "a" but isn't)
public final class b {
    public static void main(String[] args) {
        System.out.println(o.a.d(new byte[]{...}, 12345));
        ь var0 = new ь();
        var0.a();   // run() → a()
    }
}

// а/ь.java  (Cyrillic ь)
public final class ь {
    private static final String a = o.a.d(new byte[]{...}, 98765);  // SECRET_KEY → a
    private int b;   // counter → b
    
    public void a() {   // run() → a()
        this.b++;
        int var0 = 25565 ^ 0x5A5A5A5A ^ 0x5A5A5A5A;
        int var1 = 12345 ^ 0x5A5A5A5A ^ 0x5A5A5A5A;
        boolean var2 = (1 ^ 0x5A5A5A5A) ^ 0x5A5A5A5A;
        System.out.println("port=" + var0 + ", seed=" + var1);
    }
}
```

| Transform        | Effect                                                                 |
|------------------|-----------------------------------------------------------------------|
| Class renaming   | `Main` → `b`, `DemoService` → `ь` (short names; homoglyph: `а`, `ь`)  |
| Method renaming  | `run()` → `a()` (excludes main, constructors, native)                 |
| Field renaming   | `SECRET_KEY` → `f`, `counter` → `g`                                  |
| Homoglyph        | Latin `a` becomes Cyrillic `а` (U+0430) – copy-paste fails           |
| Invisible chars  | Zero-width chars in names – appear normal but differ                 |
| Number obfuscation | `25565` → `(x ^ key) ^ key`; floats/doubles via bit XOR             |
| Boolean obfuscation | `true` → `(value ^ key) ^ key`                                    |
| String obfuscation | `"my-secret-key"` → encrypted bytes, decoded at runtime           |
| Debug stripping  | Local vars become `var0`, `var1`; line numbers removed               |

## Documentation

- [Features.md](Features.md) – Current and planned features
- [docs/HOMOGLYPH_INFO.md](docs/HOMOGLYPH_INFO.md) – Homoglyph & invisible char obfuscation
- [CONTRIBUTING.md](CONTRIBUTING.md) – Contribution guidelines

## License

**All Rights Reserved.** This project is proprietary software. Unauthorized copying, modification, distribution, or use is not permitted without explicit permission from the author.