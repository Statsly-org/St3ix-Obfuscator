# Contributing to St3ix Obfuscator

Thank you for your interest in contributing. Please follow these guidelines to keep the codebase organized and consistent.

## Before You Start

- Check [Features.md](Features.md) for planned work
- Open an issue or discuss changes before large refactors

## Issues & Pull Requests (Non-Maintainers)

**Issues** and **pull requests** from non-maintainers must be **thoroughly described** and **correctly labeled**:

- **Issues:** Include steps to reproduce, expected vs. actual behavior, environment (OS, Java version), and any relevant context. Use the appropriate labels (e.g. `bug`, `enhancement`, `question`).
- **Pull requests:** Describe what changed, why, and how it was tested. Use the correct PR label (e.g. `feat`, `fix`, `docs`). Follow the commit message format below.

Incomplete or unlabeled issues/PRs may be **closed** or **rejected**.

## Code Organization

### Structure

- **Single responsibility** – One clear purpose per class or file
- **Short files** – Aim for ~150–200 lines max; split if it grows
- **Package layout** – Keep domains separate (`transform`, `io`, `config`, etc.)
- **No god classes** – Avoid classes that do too much; delegate instead

### Naming

- **Classes**: PascalCase, descriptive (e.g. `ClassRenamer`, `NumberObfuscator`)
- **Methods**: camelCase, verb-based (e.g. `transform()`, `loadConfig()`)
- **Constants**: UPPER_SNAKE_CASE
- **Packages**: lowercase, no underscores

### Quality

- Prefer immutable objects where feasible
- Avoid magic numbers and strings; use named constants
- Write Javadoc for public APIs
- Comments should explain *why*, not *what*

## Randomization Rule

When a feature can use random values (keys, names, seeds, etc.), there **must** be a config option to toggle it. Default to deterministic behavior for reproducibility. Example: `numberKeyRandom`, `arrayKeyRandom`, `classNamesRandom`.

## Testing

- Run the obfuscator on the example project: `./test-obfuscate.bat` (Windows) or equivalent
- Verify the obfuscated JAR runs correctly: `java -jar build/dist/Obfuscate/example-java-project-obfuscated.jar`
- Add unit tests for new transforms or isolated logic

## Pull Request Process

### Commit Message Format

All commits and PR titles **must** follow [Conventional Commits](https://www.conventionalcommits.org/), e.g.:

- `feat:` – new feature
- `fix:` – bug fix
- `docs:` – documentation
- `refactor:` – code refactoring
- `chore:` – maintenance, build, config
- `test:` – tests

Example: `feat: add array dimension obfuscation`

PRs or commits without this format will be **rejected**.

### Steps

1. Branch from the main development branch
2. Keep commits focused and logically grouped
3. Ensure the project builds: `./gradlew dist`
4. Run the test script and confirm the example project still works
5. Update documentation (README, Features.md, sidenotes) if needed
6. Summarize changes in the PR description

## Code Style

- Java 17+ features allowed
- English only in code, comments, and user-facing messages
- Avoid empty catch blocks; log or handle exceptions appropriately
- Prefer explicit error handling over silent failures

## Questions

If something is unclear, open an issue or reach out to the maintainers.
