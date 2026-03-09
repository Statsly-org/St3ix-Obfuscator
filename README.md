# St3ix Obfuscator

A Java bytecode obfuscator that transforms JAR files to make decompilation harder. Class names, numeric constants, and array dimensions are obfuscated while preserving runtime behavior.

## Requirements

- **Java 17** or later
- **Gradle** (wrapper included)

## Quick Start

```bash
# Build the obfuscator
./gradlew dist

# Obfuscate a JAR (output goes to build/dist/Obfuscate/)
java -jar build/dist/st3ix-obfuscator.jar -i myapp.jar -o myapp-obfuscated.jar

# Run the result
java -jar build/dist/Obfuscate/myapp-obfuscated.jar
```

**Windows:** Use `gradlew.bat` and `build\dist\st3ix.bat` for the launcher script.

## Features

- **Class renaming** – Short or random names; configurable length
- **Number obfuscation** – Hides `int`/`long` constants with XOR
- **Array obfuscation** – Hides array dimensions
- **Random options** – Optional random keys and class names per build
- **Exclude patterns** – Skip JDK, Bukkit, Minecraft, and custom packages
- **YAML config** – `config.yml` next to the JAR

See [Features.md](Features.md) for the full list of current and planned features.

## Usage

| Option | Description |
|--------|-------------|
| `-i`, `--input <path>` | Input JAR file |
| `-o`, `--output <path>` | Output filename (saved to `Obfuscate/` next to the obfuscator JAR) |
| `--max-ram <size>` | Max heap hint (e.g. `512m`, `2g`) – use the launcher script |
| `-h`, `--help` | Show usage |

Output files are written to the `Obfuscate/` folder in the directory containing the obfuscator JAR. If a file with the same name exists, it is overwritten.

## Configuration

Copy `config.yml.example` to `config.yml` and place it next to `st3ix-obfuscator.jar`.

```yaml
classRenamingEnabled: true
numberObfuscationEnabled: true
arrayObfuscationEnabled: true
classNamesRandom: false
classNameLength: 6
numberKeyRandom: false
arrayKeyRandom: false
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

## Documentation

- [Features.md](Features.md) – Current and planned features
- [CONTRIBUTING.md](CONTRIBUTING.md) – Contribution guidelines

## License

**All Rights Reserved.** This project is proprietary software. Unauthorized copying, modification, distribution, or use is not permitted without explicit permission from the author.
