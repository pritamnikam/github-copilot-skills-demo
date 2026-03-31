# Skill: API Test Generator

## When to invoke
User asks to generate API/functional tests for an endpoint.
User uses: /gen-api-test
Agent identifies untested endpoints.

## Inputs required
- OpenAPI spec path: contracts/{service}/openapi.yaml
- Target operationId or HTTP path + method
- Language: Java (RestAssured) or Python (httpx)

## Execution steps

### Step 1 — Parse the OpenAPI spec
For the target endpoint extract:
- HTTP method and path
- Request body schema + example
- All response codes and their schemas
- Security requirements (is auth required?)
- Path parameters, query parameters
- Required vs optional fields

### Step 2 — Build test plan
Generate this plan as a comment before any code:

Endpoint: POST /api/orders
Tests to generate:
  1. placeOrder_validRequest_returns201WithOrderBody
  2. placeOrder_validRequest_locationHeaderPresent
  3. placeOrder_validRequest_responseMatchesSchema
  4. placeOrder_missingUserId_returns400
  5. placeOrder_missingProductId_returns400
  6. placeOrder_quantityZero_returns400WithFieldError
  7. placeOrder_quantityNegative_returns400WithFieldError
  8. placeOrder_missingAuthToken_returns401
  9. placeOrder_expiredToken_returns401
  10. placeOrder_malformedToken_returns401
  11. placeOrder_wrongContentType_returns415
  12. placeOrder_userNotFound_returns404
  13. placeOrder_insufficientStock_returns409
  14. placeOrder_upstreamDown_returns503

### Step 3 — Generate JSON schema file
Extract the response schema from OpenAPI components.
Convert to JSON Schema draft-07 format.
Save to:
  Java:   src/test/resources/schemas/{operationId}-response.json
  Python: tests/schemas/{operation_id}_response.json

### Step 4 — Generate test class
Java: extend BaseApiTest, use RestAssured DSL
Python: use ApiTestBase, httpx TestClient

Follow the structure:
  - Group by endpoint using @Nested (Java) or class (Python)
  - Happy path tests first
  - Validation error tests second
  - Auth tests third
  - Upstream error tests last

### Step 5 — Generate WireMock stubs
API tests still need WireMock for upstream dependencies.
Reuse stubs from integration tests if they exist.
Generate new stubs if needed (invoke wiremock-stub-generator skill).

## Output order
1. JSON schema file
2. WireMock stubs (if new ones needed)
3. Test class
4. Summary of tests generated per scenario type