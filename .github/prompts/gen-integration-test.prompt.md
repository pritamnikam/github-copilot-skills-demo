---
mode: agent
tools: [codebase, file, terminal]
description: Generate Strategy A integration test for selected method
---

# Generate Integration Test

## Step 1 — Load context
Read these files before generating anything:
- #file:.github/copilot-instructions.md
- #file:.github/skills/integration-test-generator.md
- #file:.github/skills/wiremock-stub-generator.md
- #file:src/test/java/**/support/BaseIntegrationTest.java

## Step 2 — Identify target
Method to test: ${input:Enter method name (e.g. placeOrder)}
Class:          ${input:Enter class name (e.g. OrderService)}

## Step 3 — Execute skill
Follow integration-test-generator skill exactly.
Do not skip the WireMock stub generation step.
Do not skip the SQL seed file step.

## Step 4 — Verify
After generating, check:
- [ ] Stub files saved to correct path
- [ ] Test class extends BaseIntegrationTest
- [ ] @Sql annotation references generated seed file
- [ ] At least one test per documented error response in OpenAPI

## Step 5 — Run and confirm
Run: mvn test -Dtest="${input:Enter method name}IT"
Report result. If failing, diagnose and fix before finishing.