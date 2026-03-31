---
mode: agent
tools: [codebase, file, terminal]
description: Generate RestAssured or httpx API tests from OpenAPI contract
---

# Generate API Tests

## Step 1 — Load standards
Read before generating:
- #file:.github/copilot-instructions.md
- #file:.github/skills/api-test-generator.md

## Step 2 — Load the contract
Endpoint to test: ${input:operationId or path+method (e.g. placeOrder or POST /api/orders)}
Service: ${input:service name (e.g. order-service)}

Read the OpenAPI spec:
  contracts/${input:service name}/openapi.yaml

Extract all request/response schemas for the target endpoint.

## Step 3 — Load existing base class
Java:   read BaseApiTest.java
Python: read tests/api/conftest.py

Match the exact patterns used there.

## Step 4 — Execute skill
Run api-test-generator skill step by step.
Output the test plan BEFORE generating any code.
Get implicit approval from the developer by showing the plan.

## Step 5 — Generate schemas
Extract JSON schema for the response body.
Save to correct location before writing tests.

## Step 6 — Run and verify
Java:   mvn test -Dtest="${input:operationId}ApiTest"
Python: pytest tests/api/test_${input:operationId}_api.py -v

Fix any failures before finishing.
Report final pass/fail count.