# GitHub Copilot — Org-Wide Standards

## Integration Testing: Strategy A (Contract Isolation)

### Non-negotiable rules
- NEVER use H2 or in-memory databases in integration tests
- NEVER call real external services — always WireMock stubs
- ALWAYS use TestContainers for all infrastructure
- Stubs MUST be derived from OpenAPI specs in /contracts/
- Integration test classes end in IT.java — unit tests end in Test.java
- Every IT test must be independently runnable — no shared mutable state
- Python: use testcontainers-python, pytest fixtures, never mock the DB layer

### Infrastructure
All integration tests use TestContainers:
- Postgres: postgres:15-alpine
- Kafka:    confluentinc/cp-kafka:7.4.0
- Redis:    redis:7-alpine

### WireMock conventions
- Stub files: src/test/resources/wiremock/{service-name}/mappings/
- File naming: {operation-id}-{scenario}.json
  Examples: getUserById-success.json, getUserById-not-found.json
- Always stub: URL + HTTP method + request body matcher
- Always cover: success + each documented error response

### Assertion style
- Java: AssertJ only (assertThat) — never assertEquals
- Python: plain assert or pytest assertions
- Always assert BOTH return value AND side effects:
  - DB state (query after the call)
  - Kafka events (consume and verify)
  - Cache state (check Redis if applicable)

### Test data
- Java: @Sql("/test-data/{TestClassName}.sql") for seeding
- Python: fixtures in conftest.py, SQL in tests/fixtures/sql/
- Always roll back after each test

### BaseIntegrationTest
- Java services: extend BaseIntegrationTest
  Location: src/test/java/{package}/support/BaseIntegrationTest.java
- Python services: use conftest.py in tests/integration/

### Imports and dependencies
Java — always use:
  org.testcontainers:testcontainers:1.19.3
  org.testcontainers:postgresql:1.19.3
  org.testcontainers:kafka:1.19.3
  com.github.tomakehurst:wiremock-standalone:3.3.1
  org.springframework.boot:spring-boot-starter-test

Python — always use:
  testcontainers[postgres,kafka]==3.7.1
  pytest==8.1.1
  httpx==0.27.0
  pytest-asyncio==0.23.6


## Skill: Unit Test Generator

### When to invoke
User asks to generate unit tests for a class or method.
User uses: /gen-unit-test
Agent identifies an untested method.

### Inputs required
- Target class or method name
- Path to the source file
- Language (Java or Python — auto-detect from extension)

### Execution steps

#### Step 1 — Analyse the class
Read the source file completely. Identify:
- Class name and package / module
- All public methods (skip private, getters, setters)
- All constructor-injected dependencies
- All thrown exceptions (declared + runtime)
- Return types (void, Optional, List, primitive, domain object)
- Any time-dependent logic (LocalDateTime.now(), Instant.now())
- Any static calls (flag these — may need refactoring)

#### Step 2 — Build the test plan
For each public method, list:
  - Happy path scenario
  - Each null/empty input scenario
  - Each boundary value scenario
  - Each exception scenario (one per throw)
  - Side effect verification scenarios (verify mock calls)
Output this plan as a comment block before generating code.

#### Step 3 — Generate test class structure

##### Java
- Annotate class with @ExtendWith(MockitoExtension.class)
- Declare @InjectMocks for the class under test (name it: sut)
- Declare @Mock for each dependency
- Group tests by method using @Nested inner classes
- Name the sut field always "sut" for consistency

##### Python
- Create a pytest class: Test{ClassName}
- Group by method using nested classes or method prefix
- Use @pytest.fixture for the class under test and mocks
- Name the fixture: sut

#### Step 4 — Generate test methods
For each scenario in the plan:
- Follow strict AAA pattern
- Use descriptive names: method_condition_result
- For Java: use AssertJ assertions only
- For Python: use plain assert + pytest.raises
- Always verify mock interactions where side effects exist
- For exceptions: assert the message content, not just the type

#### Step 5 — Generate test data helpers
If the class uses complex domain objects:
- Java: create a static factory TestDataFactory with builder methods
- Python: create pytest fixtures in conftest.py

### Output format
Always output in this order:
1. Test plan (as inline comment)
2. Test class
3. Any test data factories needed
4. Summary: N tests generated, covering N methods, N scenarios