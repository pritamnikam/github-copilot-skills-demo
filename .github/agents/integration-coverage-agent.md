# Agent: Integration Coverage Agent

## Trigger
"@integration-agent scan order-service"
"@integration-agent boost coverage for OrderService"
"@integration-agent fix failing integration tests"

## Phase 1 — Discovery
Scan src/main/java for all @Service classes.
For each public method:
  - Does an *IT.java test exist for it? If no → gap found
  - Does the existing IT test cover error scenarios? If no → partial gap

Build gap report:
  { class, method, gaps: [missing-scenarios], dependencies: [...] }

## Phase 2 — Contract validation
For each dependency found:
  - Check contracts/{service}/openapi.yaml exists
  - Check stub files exist in wiremock/{service}/mappings/
  - Compare stub response schema to current OpenAPI schema
  - Flag stale stubs (schema mismatch)

## Phase 3 — Generation
For each gap:
  - Invoke skill: integration-test-generator
  - Invoke skill: wiremock-stub-generator (if stubs stale or missing)

## Phase 4 — Execution
Run: mvn test -Dtest="*IT" -pl {service}
Parse surefire XML output from target/surefire-reports/
For each failure:
  - Read stack trace
  - Classify: stub mismatch | missing test data | wrong assertion | infra
  - Apply fix (max 3 attempts)
  - Re-run the single failing test

## Phase 5 — Report
Print markdown report:
  ## Integration Coverage Report — {service} — {date}
  | Method | Before | After | Tests Added | Status |
  |--------|--------|-------|-------------|--------|
  | placeOrder | 0% | 85% | 4 | ✅ |
  | cancelOrder | 40% | 90% | 2 | ✅ |
  
  ### Remaining gaps (manual review needed)
  - processPayment: requires payment gateway contract (not in /contracts)