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
- **Number obfuscation** – Hides `int` constants via math expressions (`123` → `(50*3)-27`, `(a<<n)+b`, etc.); decompilers show expressions, not literals. `long`/`float`/`double` still use XOR. Less prone to constant folding than simple XOR.
- **Array dimension obfuscation** – Hides array sizes (`new int[8]` → `new int[(8 ^ key) ^ key]`)
- **Boolean obfuscation** – Hides `true`/`false` literals using XOR (`ICONST_0`/`ICONST_1` → `(value ^ key) ^ key`)
- **String obfuscation** – Encrypts string literals using XOR; **decryption inlined at each use site** (no central decoder class). **Key per class and per string**: each string uses `key ^ class.hashCode() ^ index`, so no global key and different strings get different keys. Strong against `strings`-tools and casual inspection. **Note:** Determined reversers can still trace decryption logic – obfuscation raises the bar, not unbreakable.
- **Optional random keys** – `numberKeyRandom`, `arrayKeyRandom`, `booleanKeyRandom`, `stringKeyRandom` for different keys/patterns per build
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
- ~~**String encryption**~~ ✓ (XOR-based, inline decrypt at each use site; no central decoder)
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

