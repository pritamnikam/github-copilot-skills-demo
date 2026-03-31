# How to Use This Day-to-Day

## Repository structure

```bash
# What we're building
org/
├── .github/
│   ├── copilot-instructions.md          # ← extend with unit test rules
│   ├── skills/
│   │   ├── unit-test-generator.md       # ← core generation skill
│   │   ├── test-quality-reviewer.md     # ← reviews generated tests
│   │   └── coverage-gap-analyser.md     # ← finds what's missing
│   ├── agents/
│   │   └── unit-coverage-agent.md       # ← autonomous sweep agent
│   └── prompts/
│       ├── gen-unit-test.prompt.md      # ← developer daily driver
│       ├── review-test-quality.prompt.md
│       └── fix-coverage-gap.prompt.md
│
├── order-service/                        # Java Spring Boot
│   ├── .github/
│   │   └── copilot-instructions.md      # ← service-level overrides
│   └── src/test/java/com/example/order/
│       ├── support/
│       │   └── UnitTestSupport.java     # ← shared base + factories
│       └── service/
│           └── OrderServiceTest.java    # ← generated example
│
└── inventory-service/                   # Python FastAPI
    ├── .github/
    │   └── copilot-instructions.md
    └── tests/
        ├── unit/
        │   └── test_order_service.py    # ← generated example
        └── conftest.py
```

## How to Run Unit tests

```bash
# Generate unit tests for a new method
# Open OrderService.java in VS Code, select the method, then:
/gen-unit-test

# Review quality of existing tests
/review-unit-tests OrderServiceTest

# Find what coverage you're missing
/fix-coverage-gap OrderService

# Autonomous sweep — boost whole service to 90%
@unit-agent boost coverage for order-service to 90%

# Run coverage report
# Java:
mvn test jacoco:report
open target/site/jacoco/index.html

# Python:
pytest tests/unit/ --cov=app --cov-report=html
open htmlcov/index.html
```


## How to Run Everything

```bash
# From org/ root

# 1. Run order-service integration tests only
cd order-service
mvn test -Dtest="*IT"

# 2. Run with Copilot agent
# Open VS Code in order-service/
# In Copilot Chat:
@integration-agent scan order-service

# 3. Generate tests for a new method
# Select the method in the editor, then in Copilot Chat:
/gen-integration-test

# 4. Regenerate stubs when contracts change
# In Copilot Chat:
@integration-agent fix stale stubs for order-service
```


### Functiona tests
```bash
# The functional/API test layer sits here:

External caller
      │
      ▼
[API Test] ──HTTP──► [Your Service] ──WireMock──► [Dependencies]
                            │
                      [Real DB via TestContainers]
```

#### Structure
```bash
org/
├── contracts/
│   ├── user-service/openapi.yaml         # already exists
│   ├── inventory-service/openapi.yaml    # already exists
│   └── order-service/openapi.yaml        # NEW — we add this now
│
├── .github/
│   ├── copilot-instructions.md           # extend with API test rules
│   ├── skills/
│   │   ├── api-test-generator.md         # NEW
│   │   └── contract-validator.md         # NEW
│   ├── agents/
│   │   └── api-coverage-agent.md         # NEW
│   └── prompts/
│       ├── gen-api-test.prompt.md        # NEW
│       └── validate-contract.prompt.md  # NEW
│
├── order-service/                         # Java — RestAssured
│   ├── .github/copilot-instructions.md   # extend
│   └── src/test/java/com/example/order/
│       ├── support/
│       │   └── BaseApiTest.java          # NEW — RestAssured base
│       └── api/
│           ├── PlaceOrderApiTest.java    # NEW
│           └── CancelOrderApiTest.java   # NEW
│
└── inventory-service/                    # Python — httpx
    ├── .github/copilot-instructions.md  # extend
    └── tests/
        ├── api/
        │   ├── conftest.py              # NEW — httpx client setup
        │   └── test_reserve_stock_api.py # NEW
        └── conftest.py                  # already exists
```

```bash
# Java — run all API tests
mvn test -Dtest="*ApiTest" -pl order-service

# Java — run one endpoint's tests
mvn test -Dtest="PlaceOrderApiTest" -pl order-service

# Python — run all API tests
pytest tests/api/ -v

# Python — run one endpoint's tests
pytest tests/api/test_reserve_stock_api.py -v

# Generate API tests for a new endpoint (Copilot)
/gen-api-test

# Validate contract conformance (Copilot)
/validate-contract

# Autonomous sweep — find and fill all missing API test scenarios
@api-agent scan order-service
```
