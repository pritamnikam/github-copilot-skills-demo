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


# Functional / API Testing Standards

## What API tests verify
API tests call real HTTP endpoints against a running service instance.
They validate: status codes, response body schema, headers,
error messages, auth enforcement, and content negotiation.
They do NOT test business logic — that belongs in unit tests.
They do NOT test cross-service workflows — that belongs in E2E tests.

## Scope
- One service at a time
- Dependencies stubbed via WireMock (same as integration tests)
- Real DB via TestContainers
- Service started on a random port
- Tests call HTTP endpoints directly

## Java stack
Framework:   RestAssured 5.x
Base class:  BaseApiTest (extends BaseIntegrationTest)
JSON schema: rest-assured json-schema-validator
Auth helper: JwtTestHelper (generates test JWT tokens)
File suffix: *ApiTest.java

## Python stack
HTTP client: httpx (sync TestClient wrapping FastAPI app)
Base:        ApiTestBase class in tests/api/conftest.py
JSON schema: jsonschema library
Auth helper: create_test_token() fixture
File naming: test_*_api.py

## Non-negotiable rules
- ALWAYS test with a valid auth token (unless testing 401 explicitly)
- ALWAYS assert the response status code first
- ALWAYS assert the Content-Type header
- ALWAYS validate response body against OpenAPI schema
- ALWAYS test the 401 case for every protected endpoint
- NEVER assert internal implementation details (DB IDs from mock, etc.)
- NEVER use hardcoded ports — always use random port from test context

## What to test per endpoint (mandatory)
Every endpoint must have tests for:
  [ ] 2xx happy path — valid request, correct response body and schema
  [ ] 400 — invalid request body (missing fields, wrong types)
  [ ] 401 — missing token
  [ ] 401 — expired token
  [ ] 401 — malformed token
  [ ] 404 — resource not found (where applicable)
  [ ] 405 — wrong HTTP method
  [ ] 415 — wrong Content-Type (for POST/PUT)
  [ ] Each documented error code from OpenAPI spec

## Response schema validation
Java:
  .body(matchesJsonSchemaInClasspath(
      "schemas/order-response.json"))

Python:
  from jsonschema import validate
  validate(response.json(), load_schema("order-response.json"))

Schemas live at:
  Java:   src/test/resources/schemas/
  Python: tests/schemas/
Generate schemas from OpenAPI components using the
contract-validator skill.

## Auth token generation for tests
Java:
  String token = JwtTestHelper.validToken("user-1", "ROLE_USER");
  String expired = JwtTestHelper.expiredToken("user-1");
  String malformed = "not.a.real.jwt";

Python:
  token = create_test_token(user_id="user-1", role="ROLE_USER")
  expired_token = create_expired_token(user_id="user-1")

## Test data lifecycle
- Seed via @Sql (Java) or sql fixture (Python) before each test
- Rollback via @Transactional or explicit DELETE after each test
- Never depend on test execution order