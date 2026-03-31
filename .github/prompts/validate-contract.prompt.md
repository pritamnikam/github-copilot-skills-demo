---
mode: agent
tools: [codebase, file, terminal]
description: Validate running service conforms to OpenAPI contract
---

# Validate Contract Conformance

## Step 1 — Start the service
Java:
  mvn spring-boot:test-run &
  Wait for "Started Application" in output.

Python:
  uvicorn app.main:app --port 8000 --env-file .env.test &
  Sleep 2 seconds.

## Step 2 — Run contract validator skill
Service: ${input:service name}
Contract: contracts/${input:service name}/openapi.yaml

Execute contract-validator skill completely.
Do not skip any endpoint.

## Step 3 — Report drift
If drift found:
  - Show exact field missing or type mismatch
  - Locate the source: which exception handler, serializer,
    or DTO is producing the wrong shape
  - Suggest the minimal fix

If no drift:
  Print: "✅ Service conforms to contract — no drift detected"

## Step 4 — Stop service
Java:   kill the spring-boot:test-run process
Python: kill the uvicorn process