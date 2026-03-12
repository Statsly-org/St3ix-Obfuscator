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

**Windows:** Use `gradlew.bat` for building. For the GUI, run `run.bat` from `build/dist/` or double-click the JAR.

Bei jedem Release gibt es ein ZIP-Archiv mit allem Nötigen: JAR, Batch-Datei zum Starten, Config-Beispiel, Images.

## Features

- **Class renaming** – Short or random names; configurable length
- **Method renaming** – Obfuscate method names; handles override chains
- **Field renaming** – Obfuscate field names; excludes serialVersionUID and enum constants
- **Homoglyph & invisible chars** – Unicode lookalikes (a→а) and zero-width chars; copy-paste fails
- **Number obfuscation** – Hides `int` via math expressions (`123` → `(50*3)-27`); `long`/`float`/`double` via XOR
- **Array obfuscation** – Hides array dimensions
- **Boolean obfuscation** – Hides `true`/`false` literals
- **String obfuscation** – Encrypts string literals (XOR); inline decrypt at each use; key per class and per string (no central decoder, no dump point)
- **Debug info stripping** – Removes source names, line numbers, local variable names
- **Local variable renaming** *(planned)* – Obfuscate local variable names when debug kept
- **Random options** – Optional random keys and class/method/field names per build
- **Exclude patterns** – Skip JDK, Bukkit, Minecraft, and custom packages
- **GUI** – Graphical interface for obfuscation (`run.bat`)
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
methodRenamingEnabled: true
fieldRenamingEnabled: true
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
package example;

public final class Main {
    public static void main(String[] args) {
        System.out.println("License validation active.");
        LicenseValidator validator = new LicenseValidator();
        validator.validate();
    }
}

// example/LicenseValidator.java
public final class LicenseValidator {
    private static final String API_KEY = "sk-live-a7f3b9c2e1d4";
    private int validationCount;
    
    public void validate() {
        validationCount++;
        int port = 443;           // HTTPS
        int maxRetries = 3;
        boolean strictMode = true;
        System.out.println("port=" + port + ", retries=" + maxRetries);
    }
}
```

**After obfuscation** (class, method, number, string, debug stripping, homoglyph):

```java

public final class b {
    public static void main(String[] args) {
        byte[] enc = new byte[]{...};
        byte[] out = new byte[enc.length];
        for (int i = 0; i < enc.length; i++)
            out[i] = (byte)(enc[i] ^ ((key + i) & 0xFF));
        System.out.println(new String(out, StandardCharsets.UTF_8));
        ь var0 = new ь();
        var0.a();
    }
}

public final class ь {
    private static final String a = /* inline decrypt */;
    private int b;
    
    public void a() {
        this.b++;
        int var0 = 431 + 12;      // 443 via expression
        int var1 = 2 * 2 - 1;    // 3 via expression
        boolean var2 = 0x... ^ 0x...;  // boolean XOR
        System.out.println("port=" + var0 + ", retries=" + var1);
    }
}
```

| Transform         | Effect                                                                 |
|-------------------|-----------------------------------------------------------------------|
| Class renaming    | `Main` → `b`, `LicenseValidator` → `ь` (short names; homoglyph: `а`, `ь`) |
| Method renaming   | `validate()` → `a()` (excludes main, constructors, native)            |
| Field renaming    | `API_KEY` → `a`, `validationCount` → `b`                              |
| Homoglyph         | Latin `a` becomes Cyrillic `а` (U+0430) – copy-paste fails           |
| Invisible chars   | Zero-width chars in names – appear normal but differ                  |
| Number obfuscation  | `443` → `431+12`, `3` → `2*2-1` (math expressions)                 |
| Boolean obfuscation | `true` → `(value ^ key) ^ key`                                    |
| String obfuscation  | Inline XOR decrypt at each use; key per class and per string; no central decoder → no dump point |
| Debug stripping   | Local vars become `var0`, `var1`; line numbers removed                |

### Strength

Obfuscation raises the bar for casual and automated reverse engineering. Numbers are hidden as math expressions at compile time; decompilers show the expression, not the literal. Strings use **runtime-computed keys** (`key ^ class.hashCode() ^ index` per string), decrypted inline at each use site—no central decoder, no single breakpoint to dump all strings.

We chose XOR over AES for string encryption: AES would be overkill for typical JAR obfuscation and adds complexity (IV handling, block size). XOR with per-class/per-string keys is lightweight and effective against `strings`-tools and casual inspection. **Issues and PRs are open**—if you want AES or stronger encryption, contributions are welcome.

Realistic caveats: determined reversers can still trace decryption logic; obfuscation is a deterrent, not unbreakable protection.

## Documentation

- **Support Server** – [Discord](https://discord.gg/bfqFGqFa99)
- [Features.md](Features.md) – Current and planned features
- [docs/HOMOGLYPH_INFO.md](docs/HOMOGLYPH_INFO.md) – Homoglyph & invisible char obfuscation
- [CONTRIBUTING.md](CONTRIBUTING.md) – Contribution guidelines

## License

**All Rights Reserved.** This project is proprietary software. Unauthorized copying, modification, distribution, or use is not permitted without explicit permission from the author.