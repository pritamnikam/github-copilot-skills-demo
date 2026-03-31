# Skill: Contract Validator

## Purpose
Validate that the running service actually conforms to its
OpenAPI contract. Run this in CI to catch contract drift
before it reaches consumers.

## When to invoke
"validate contract for order-service"
"/validate-contract"
Agent runs this after every API test generation.

## Step 1 — Extract JSON schemas from OpenAPI
For each schema in components/schemas:
  Convert to JSON Schema draft-07 format.
  Save to schemas/ directory.

Use this mapping:
  string             → { "type": "string" }
  integer            → { "type": "integer" }
  number             → { "type": "number" }
  boolean            → { "type": "boolean" }
  array              → { "type": "array", "items": {...} }
  object             → { "type": "object", "properties": {...} }
  required: [fields] → { "required": [...] }
  enum: [values]     → { "enum": [...] }
  format: date-time  → { "format": "date-time" }

## Step 2 — Validate each documented response
For every response code in the OpenAPI spec:
  - Make a real HTTP call that should trigger that code
  - Assert status code matches
  - Validate response body against extracted JSON schema
  - Assert Content-Type header is application/json

## Step 3 — Check undocumented responses
Make requests that are NOT in the spec:
  - DELETE on a GET-only endpoint → expect 405
  - POST with XML body → expect 415
  - Request with no Accept header → ensure response still valid

## Step 4 — Report
## Contract Validation Report — order-service

| Endpoint | Method | Status | Schema Valid | Notes |
|----------|--------|--------|--------------|-------|
| /api/orders | POST | ✅ 201 | ✅ | |
| /api/orders | POST | ✅ 400 | ✅ | |
| /api/orders | POST | ✅ 401 | ✅ | |
| /api/orders/{id} | GET | ✅ 200 | ✅ | |
| /api/orders/{id} | GET | ❌ 404 | ⚠️ | Missing 'code' field in response |

Drift detected: 1 issue
Fix required in: GlobalExceptionHandler.java line 47