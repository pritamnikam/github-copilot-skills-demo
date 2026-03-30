# Skill: Integration Test Generator (Strategy A)

## When to invoke this skill
User asks to generate integration tests for a method,
or uses the /gen-integration-test prompt.

## Inputs required
- Target method name and class
- Service name (e.g. order-service)
- Path to the method's source file

## Execution steps

### Step 1 — Analyse the method
Scan the method body for:
- FeignClient / RestTemplate / WebClient calls  → external REST
- KafkaTemplate.send() calls                   → event publishing
- @KafkaListener methods                        → event consuming
- @Autowired *Repository calls                  → DB interactions
- RedisTemplate / @Cacheable                    → cache interactions

### Step 2 — Load contracts
For each external REST call identified:
- Locate: contracts/{dependency-service}/openapi.yaml
- Extract: operationId, request schema, ALL response schemas
  including 400, 404, 409, 503 error shapes

### Step 3 — Generate WireMock stubs
For each endpoint + scenario, generate JSON stub file.
Save to: src/test/resources/wiremock/{service-name}/mappings/

File naming pattern:
  {operationId}-success.json
  {operationId}-not-found.json
  {operationId}-conflict.json
  {operationId}-server-error.json

### Step 4 — Generate test class
- Extend BaseIntegrationTest
- One @Test method per scenario
- Happy path + each error case + Kafka event verification
- Use @Sql for seed data, @Transactional for rollback
- Follow AssertJ patterns from copilot-instructions.md

### Step 5 — Generate SQL seed file
- Table names from @Entity classes in the service
- Minimum data needed for the test — no more
- Save to: src/test/resources/test-data/{TestClassName}.sql

## Output order
Always output files in this sequence:
1. WireMock stub JSON files (one per scenario)
2. SQL seed file
3. Test class
4. Brief summary: what was generated and coverage added