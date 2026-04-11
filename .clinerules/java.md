# Java 21 Coding Standards & Guidelines

You are an expert Java 21 developer. Follow these rules strictly to ensure code quality, maintainability, and modern performance standards.

## 1. Language Level & Features
- **Java 21:** Always use Java 21 syntax and features.
- **Records:** Use `record` for immutable data carriers instead of traditional classes with getters/equals/hashCode.
- **Switch Expressions:** Use `switch` as an expression, not a statement, when returning values or assigning variables.
- **Pattern Matching (Switch & `instanceof`):**
  - Use pattern matching in `switch` (JEP 441) to eliminate casting.
  - Use pattern matching with `instanceof` (JEP 394).
- **String Templates (JEP 430 - Preview):** Use `STR."..."` for string interpolation where appropriate.
- **Virtual Threads:** Leverage `Executors.newVirtualThreadPerTaskExecutor()` for blocking I/O tasks.
- **Sequenced Collections:** Use the new `SequencedCollection` interface (e.g., `.reversed()`) instead of manually reversing lists.

## 2. Coding Practices
- **Immutability:** Favor immutability (`final` fields, records) whenever possible.
- **Null Safety:** Use `Optional<T>` instead of returning `null`. Use `@NonNull`/`@Nullable` annotations if available.
- **Streams API:** Use Streams for functional-style collection processing, but favor traditional loops for simple, performance-critical loops.
- **Validation:** Use `Objects.requireNonNull()` or Validation API for input sanitization.
- **Formatting:** Use 4 spaces for indentation. Never use wildcard imports (e.g., `import java.util.*;`).
- **Type Safety:** Avoid `Object` type or raw types. Always use Generics (e.g., `List<String>`).

## 3. Project Structure & Standards
- **Naming:** 
  - Classes: `PascalCase` (Nouns)
  - Methods: `camelCase` (Verbs)
  - Constants: `UPPER_SNAKE_CASE`
- **Annotations:** Use Lombok for boilerplate reduction if it exists in `pom.xml`/`build.gradle`. If not, use standard `Record` structures.
- **Error Handling:** Catch specific exceptions, never `catch (Exception e)`. Use try-with-resources for all `AutoCloseable` resources.

## 4. Performance
- Avoid unnecessary object creation.
- Prefer `String.format` or `StringBuilder` for heavy concatenation.
- Use `ArrayList` by default, only use `LinkedList` if necessary.

## 5. Error Handling
- **Exceptions:** Never catch `Exception` or `Throwable`. Catch specific checked exceptions.
- **Logging:** Use SLF4J/Logback. Never use `System.out.println` or `printStackTrace()`.

## 6. Build & Validation
- **Command:** Always verify changes with `mvn clean compile`.
- **Dependencies:** Do not add new libraries to `pom.xml` or `build.gradle` without permission.

## 7. Documentation
- Use Javadoc for public API methods and classes.
- Explain *why* a complex algorithm is used, not *what* it does.
