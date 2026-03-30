# Skill: WireMock Stub Generator from OpenAPI

## Purpose
Generate accurate, up-to-date WireMock stub JSON files
directly from an OpenAPI spec. Run this whenever the
contract changes to keep stubs in sync.

## Inputs
- Path to OpenAPI spec: contracts/{service}/openapi.yaml
- Target service consuming the stubs (for output path)

## Rules for stub generation

### URL matching
Use urlPathEqualTo for paths without query params.
Use urlPathMatching (regex) for paths with path variables:
  /api/users/([a-zA-Z0-9-]+) matches /api/users/{id}

### Request body matching
For POST/PUT: use equalToJson with ignoreArrayOrder: true
Extract the example from the OpenAPI spec's request body example.

### Response body
Use the example from the OpenAPI spec's response example exactly.
For dynamic fields (IDs, timestamps): use response templating:
  "id": "{{randomValue type='UUID'}}"
  "createdAt": "{{now format='yyyy-MM-ddTHH:mm:ssZ'}}"

### Status codes
Map OpenAPI responses directly:
  200 → 200, 201 → 201, 400 → 400
  404 → 404, 409 → 409, 503 → 503

## Output format per stub file
{
  "request": {
    "method": "GET|POST|PUT|DELETE",
    "urlPathEqualTo": "/api/path",
    "headers": { "Content-Type": { "contains": "application/json" } },
    "bodyPatterns": [{ "equalToJson": "{...}", "ignoreArrayOrder": true }]
  },
  "response": {
    "status": 200,
    "headers": { "Content-Type": "application/json" },
    "jsonBody": { ...from OpenAPI example... },
    "transformers": ["response-template"]
  }
}