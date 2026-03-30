# Skill: Test Quality Reviewer

## When to invoke
User asks to review existing tests.
User uses: /review-unit-tests
Agent runs after generation to self-verify quality.

## Review checklist — run through every test method

### Structure
[ ] Follows AAA pattern (Arrange / Act / Assert clearly separated)
[ ] Tests exactly ONE behaviour (not multiple assertions on different things)
[ ] Test name describes: method + condition + expected result
[ ] No logic in tests (no if/else, no loops, no try/catch)
[ ] No commented-out code

### Mocking quality
[ ] Only direct dependencies are mocked
[ ] No over-mocking (mocking things not needed for THIS test)
[ ] Argument matchers are specific, not blanket any()
[ ] verify() calls check meaningful interactions
[ ] No unnecessary verify() on queries that return values
    (the return value assertion IS the verification)

### Assertion quality
[ ] Asserts the right thing (not just "not null")
[ ] Exception tests assert the message, not just the type
[ ] Side effects verified (DB save called, event published, cache set)
[ ] No multiple unrelated assertions in one test

### Coverage quality
[ ] Happy path covered
[ ] At least one null input test (where applicable)
[ ] Each exception path has its own test
[ ] Boundary values tested (0, -1, max, empty string)

### Common anti-patterns to flag
- assertTrue(result != null)         → use assertThat(result).isNotNull()
- assertEquals(expected, actual)     → use assertThat(actual).isEqualTo(expected)
- catch(Exception e) { fail() }      → use assertThrows
- @Test void test1()                 → rename with meaningful name
- when(any()).thenReturn(anything())  → too broad, use specific matcher
- Thread.sleep(100)                  → use Awaitility or mock time
- @SpringBootTest in unit test       → remove, use @ExtendWith(Mockito)

## Output format
For each issue found:
  FILE: OrderServiceTest.java
  TEST: placeOrder_happyPath
  ISSUE: Missing verify() — kafkaTemplate.send() is a side effect, needs verification
  FIX:   verify(kafkaTemplate).send(eq("orders.created"), any(), any())

End with:
  Score: X/10
  Critical issues (must fix): N
  Warnings (should fix): N
  Suggestions (nice to have): N