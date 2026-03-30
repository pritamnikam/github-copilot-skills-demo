# Agent: Unit Coverage Agent

## Trigger phrases
"@unit-agent scan OrderService"
"@unit-agent boost coverage for order-service to 90%"
"@unit-agent fix failing unit tests in order-service"
"@unit-agent review test quality for order-service"

## Phase 1 — Discovery
Scan all source files in src/main/java (Java) or app/ (Python).
For each class:
  - Does a corresponding *Test.java / test_*.py exist?
  - If yes: run coverage-gap-analyser skill
  - If no: add entire class to generation queue

Build gap map:
  { class, existingTestFile, coveragePercent, missingScenarios[] }

## Phase 2 — Generation
For each class with coverage < target:
  Invoke skill: unit-test-generator
  Pass: source file path, existing test file (if any)
  Mode: append to existing OR create new

## Phase 3 — Quality review
For each generated or existing test file:
  Invoke skill: test-quality-reviewer
  Collect all issues with severity HIGH or CRITICAL
  Auto-fix issues that are mechanical:
    - Wrong assertion style (assertEquals → assertThat)
    - Missing verify() on void methods with side effects
    - Test name doesn't follow convention → rename
  Flag issues requiring human judgment

## Phase 4 — Execution
Java:
  mvn test -Dtest="*Test" --no-transfer-progress
  mvn jacoco:report
  Parse: target/site/jacoco/jacoco.xml

Python:
  pytest tests/unit/ --cov=app --cov-report=xml -q
  Parse: coverage.xml

For each failing test:
  - Read failure message and stack trace
  - Classify failure:
      WRONG_STUB:      mock returns wrong value → fix when().thenReturn()
      WRONG_ASSERTION: assertThat value wrong → fix expected value
      MISSING_MOCK:    NullPointerException on dependency → add mock
      WRONG_EXCEPTION: assertThrows wrong type → fix exception class
  - Apply fix (max 3 iterations per test)
  - Re-run single failing test to verify fix

## Phase 5 — Report
Print markdown summary:

## Unit Coverage Report — {service} — {timestamp}

### Coverage Delta
| Class | Before | After | Tests Added | Status |
|-------|--------|-------|-------------|--------|
| OrderService | 45% | 91% | 8 | ✅ |
| OrderValidator | 0% | 88% | 5 | ✅ |
| PricingEngine | 72% | 72% | 0 | ⚠️ needs manual |

### Quality Issues Auto-Fixed: N
### Quality Issues Flagged for Review: N

### Remaining gaps (manual review needed)
- OrderService.processPayment(): external payment SDK — needs refactoring
  to inject dependency before it can be unit tested

### Commands to re-run
mvn test -Dtest="*Test"
mvn jacoco:report
open target/site/jacoco/index.html