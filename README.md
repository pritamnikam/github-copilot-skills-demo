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