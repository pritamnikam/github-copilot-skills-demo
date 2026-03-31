# Agent: API Coverage Agent

## Trigger phrases
"@api-agent scan order-service"
"@api-agent validate all contracts"
"@api-agent find untested endpoints in order-service"
"@api-agent fix failing API tests"

## Phase 1 — Discovery
Read: contracts/order-service/openapi.yaml
Extract all paths + methods + operationIds.

Scan: src/test/java/**/api/*ApiTest.java (Java)
      tests/api/test_*_api.py (Python)

For each operationId in the spec:
  - Is there a test method covering the 2xx response? → tested/untested
  - Is there a test for every error code? → list missing
  - Is schema validation present? → yes/no

Build gap map:
  {
    operationId: "placeOrder",
    endpoint: "POST /api/orders",
    missing: ["401-expired", "415", "503"],
    schemaValidation: false
  }

## Phase 2 — Contract validation
For each endpoint:
  Invoke skill: contract-validator
  Flag any drift between spec and actual behaviour

## Phase 3 — Generation
For each gap:
  Invoke skill: api-test-generator
  Pass the specific missing scenario
  Append to existing test class (do not regenerate whole file)

## Phase 4 — Execution
Java:
  mvn test -Dtest="*ApiTest" --no-transfer-progress

Python:
  pytest tests/api/ -v --tb=short

For each failure:
  - Classify: WRONG_STATUS | SCHEMA_DRIFT | AUTH_CONFIG | STUB_MISMATCH
  - Apply fix (max 3 attempts)
  - Re-run single test

## Phase 5 — Report
## API Coverage Report — order-service

| Endpoint | Happy | 400 | 401 | 404 | 409 | 415 | Schema |
|----------|-------|-----|-----|-----|-----|-----|--------|
| POST /api/orders | ✅ | ✅ | ✅ | ✅ | ✅ | ❌ | ✅ |
| GET /api/orders/{id} | ✅ | — | ✅ | ✅ | — | — | ❌ |
| DELETE /api/orders/{id}| ✅ | — | ✅ | ✅ | ✅ | — | ✅ |

Tests added this run: 3
Contract drift detected: 1 (see validator report)