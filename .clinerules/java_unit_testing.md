---
paths:
  - "papersage_backend/src/test/**"    # All test files under src/
---

# Java Unit Testing Best Practices

## 1. Framework & Stack
- Use **JUnit 5** (`@Test`, `@BeforeEach`, `@AfterEach`, `@ExtendWith`) — never JUnit 4.
- Use **Mockito** for mocking dependencies (`@Mock`, `@InjectMocks`, `@ExtendWith(MockitoExtension.class)`).
- Use **AssertJ** (`assertThat(...)`) for fluent, readable assertions — prefer it over raw JUnit `assertEquals`.

## 2. Test Class Structure
- **Naming:** Test classes must be named `<ClassUnderTest>Test.java` (e.g., `TextChunkingServiceTest.java`).
- **Location:** Mirror the source package structure under `src/test/java/`.
- **One test class per production class** — do not group unrelated tests together.
- Annotate with `@ExtendWith(MockitoExtension.class)` for any class that uses mocks.

## 3. Test Method Naming
- Use the `should_<expectedBehavior>_when_<condition>` pattern:
  ```java
  @Test
  void should_returnEmptyList_when_noChunksAreIndexed() { }

  @Test
  void should_throwIllegalArgumentException_when_questionIsBlank() { }
  ```
- Names must read as complete sentences describing behavior — never generic names like `test1()` or `testMethod()`.

## 4. AAA Structure (Arrange / Act / Assert)
Every test body must follow this exact three-section pattern with blank lines separating each section:
```java
@Test
void should_returnTopChunks_when_validQuestionIsProvided() {
    // Arrange
    String question = "What is the main contribution?";
    List<RetrievalResult> expected = List.of(...);
    when(mockService.retrieveTopChunks(question)).thenReturn(expected);

    // Act
    QueryResponse result = paperController.queryPaper(question).getBody();

    // Assert
    assertThat(result).isNotNull();
    assertThat(result.topChunks()).hasSize(1);
    assertThat(result.topChunks().get(0).similarityScore()).isGreaterThan(0.5);
}
```

## 5. Scope & Isolation
- **Unit tests only test one class.** All dependencies must be mocked — no real I/O, no real HTTP, no real Gemini calls.
- **Never use `@SpringBootTest` in unit tests** — that's an integration test and is slow.
- Keep each test focused on **one behavior**. If a method has 3 branches, write 3 tests.
- Do not share mutable state between tests — reset state in `@BeforeEach`.

## 6. Mocking Rules
- Only mock **direct dependencies** of the class under test (constructor-injected services).
- Use `verify(mock).methodName(...)` to assert interactions when the return value alone doesn't prove correctness (e.g., verifying a downstream service was called).
- Avoid over-mocking — if a collaborator is a simple value object or record, use the real thing.
- Use `ArgumentCaptor` to inspect arguments passed to mocks when the input matters more than the output.

## 7. Assertions
- **Never use bare `assertTrue(condition)`** — always use a descriptive assertion:
  ```java
  // Bad
  assertTrue(result.isPresent());

  // Good
  assertThat(result).isPresent();
  ```
- Assert on **specific values**, not just nullity or emptiness, wherever possible.
- For exception testing, use `assertThatThrownBy`:
  ```java
  assertThatThrownBy(() -> service.chunkText(null))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("text must not be null");
  ```

## 8. Edge Cases to Always Cover
For every method under test, include tests for:
- **Happy path** — valid input, expected output
- **Empty/blank input** — null strings, empty lists, zero-length files
- **Boundary conditions** — min/max sizes, single-element lists
- **Error/exception paths** — verify exceptions are thrown with correct types and messages

## 9. Test Data
- Keep test data **inline and minimal** — only create the fields the test actually uses.
- Use `List.of(...)` and record constructors directly in tests for simplicity.
- Do not create shared test fixture constants unless the same data is used in 3+ tests.

## 10. What NOT to Test
- Do **not** write tests for Java `record` field accessors — they are compiler-generated.
- Do **not** test `@JsonProperty` mapping or serialization — that is framework behavior.
- Do **not** test private methods directly — test them through the public API that exercises them.
- Do **not** test logging output.
