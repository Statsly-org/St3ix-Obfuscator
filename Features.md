# St3ix Obfuscator – Features

## Current Features

### Name Obfuscation
- **Class renaming** – Renames classes to short or random identifiers (e.g. `a`, `b`, `c` or `xk9m2p`)
- **Configurable name length** – Minimum length 1–32 characters
- **Random vs sequential** – Optional random names per build for stronger obfuscation
- **Exclude patterns** – Skip classes/packages from renaming. Use `*` to exclude all classes; prefix matches (e.g. `com.example` excludes `com.example.*`). Built-in: java.*, javax.*, Bukkit, Minecraft, etc.
- **Package flattening** – Inner classes get short names; outer package hierarchy preserved
- **Homoglyph obfuscation** – Use Unicode lookalikes (e.g. Cyrillic а instead of Latin a). Names appear familiar but copy-paste and search fail. Sequential: fixed mapping; random: varies per char.
- **Invisible character injection** – Zero-width chars (U+200B, etc.) in names. Safe for JVM; harder to detect and remove.

### Data Obfuscation
- **Number obfuscation** – Hides `int`, `long`, `float` and `double` constants using XOR (`42` → `(42 ^ key) ^ key`; floats/doubles via bit representation)
- **Array dimension obfuscation** – Hides array sizes (`new int[8]` → `new int[(8 ^ key) ^ key]`)
- **Boolean obfuscation** – Hides `true`/`false` literals using XOR (`ICONST_0`/`ICONST_1` → `(value ^ key) ^ key`)
- **String obfuscation** – Encrypts string literals using XOR; decrypts at runtime via injected helper class. Runtime-derived keys for `static final` fields hinder simple decompilers. Strong against `strings`-tools, casual inspection, and basic decompilation. **Note:** Advanced decompilers (e.g. Recaf) can still reconstruct strings via constant propagation and symbolic execution – this cannot be fully prevented with bytecode obfuscation alone.
- **Optional random keys** – `numberKeyRandom`, `arrayKeyRandom`, `booleanKeyRandom`, `stringKeyRandom` for different XOR keys per build
- **Debug info stripping** – Removes source file names, line number table, and local variable table. Decompilers show `var0`, `var1`, etc., and lose source mappings. Configurable via `debugInfoStrippingEnabled`.

### Configuration
- **YAML config** – `config.yml` next to JAR
- **Per-feature toggles** – Enable/disable class renaming, number, array, boolean, and string obfuscation
- **Random options** – Toggle randomness for class names and obfuscation keys

### Output & CLI
- **CLI** – `-i` input, `-o` output filename
- **Obfuscate folder** – Output written to `Obfuscate/` in the obfuscator JAR directory
- **Overwrite** – Replaces existing output file if present

### Developer Experience
- **Colored logging** – INFO, STEP, OK levels with colors (disable via `NO_COLOR=1`)
- **Version display** – Shows version in startup banner
- **Gradle integration** – Example project with `obfuscate` task
- **Test script** – `test-obfuscate.bat` for quick end-to-end testing

---

## Planned Features

### Name Obfuscation
- **Method renaming** – Rename methods (except overrides, main, constructors)
- **Field renaming** – Rename fields; respect reflection and serialization
- **Overload induction** – Multiple methods with same name, different signatures

### Control Flow Obfuscation
- **Opaque predicates** – Conditions that always evaluate true/false but are hard to analyze
- **Bogus control flow** – Fake branches, dead code, unreachable blocks
- **Control flow flattening** – Replace linear flow with switch/dispatch structure
- **Indirection** – Wrap logic in extra method calls or trampolines
- **Loop transformation** – Modify loop structure to obscure intent

### String Obfuscation
- ~~**String encryption**~~ ✓ (XOR-based, runtime decoder)
- **String splitting** – Split strings across multiple concatenations
- **Encoding** – Base64, XOR, custom encodings
- **Reflection hiding** – Obfuscate strings used in reflection

### Data Obfuscation
- *(none remaining)*

### Structural Obfuscation
- **Class splitting** – Split large classes into smaller ones
- **Class merging** – Merge unrelated classes
- **Fake classes** – Add dummy classes/methods to mislead analysis
- **Annotation removal** – Strip or obfuscate annotations (configurable)

### Anti-Tampering & Anti-Debugging
- **Integrity checks** – Detect modification via checksums/hashes
- **Anti-debugging** – Detect debugger attachment, timing checks
- **Anti-dump** – Hinder memory dumps or runtime inspection

### Compatibility & Usability
- **Mapping file** – Output deobfuscation mapping for stack traces
- **Keep rules** – ProGuard-style keep rules for classes/methods/fields
- **Gradle/Maven plugins** – Easy integration into build pipelines
- **Incremental obfuscation** – Support incremental builds where possible

