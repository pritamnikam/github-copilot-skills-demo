# Skill: Coverage Gap Analyser

## When to invoke
User asks: "what am I missing in tests?"
User uses: /analyse-coverage
Agent runs during Phase 1 discovery.

## Inputs
- Source class or package path
- Existing test class (if any)
- JaCoCo XML report (if available at: target/site/jacoco/jacoco.xml)

## Step 1 — Parse JaCoCo report (Java)
If jacoco.xml exists:
- Find the target class entry
- Extract: line coverage %, branch coverage %, missed lines
- List each missed line number

If no report: run coverage first:
  mvn test jacoco:report -Dtest="*Test"

## Step 2 — Parse pytest-cov report (Python)
If coverage.xml exists (from: pytest --cov --cov-report=xml):
- Find the target module
- Extract missed line numbers

If no report: run:
  pytest --cov=app --cov-report=xml tests/unit/

## Step 3 — Map missed lines to scenarios
For each missed line:
- Identify which method it belongs to
- Identify what condition causes that branch
  (look at surrounding if/else, try/catch, switch)
- Name the missing test scenario:
  "Line 47: null check for userId — missing test: placeOrder_nullUserId_throwsException"

## Step 4 — Priority ranking
Rank gaps by risk:
  HIGH:   Exception paths not tested (silent failures)
  HIGH:   Null checks not tested (NPE risk)
  MEDIUM: Boundary conditions not tested
  MEDIUM: Else branches not tested
  LOW:    Logging statements
  LOW:    Simple getters

## Output format
## Coverage Gap Report — OrderService

Current coverage: 62% lines / 54% branches

### HIGH priority gaps
| Method | Line | Missing Scenario |
|--------|------|-----------------|
| placeOrder | 47 | userId is null → should throw IllegalArgumentException |
| placeOrder | 63 | inventoryClient throws 503 → should propagate ServiceUnavailableException |
| cancelOrder | 89 | order status is SHIPPED → should throw OrderAlreadyShippedException |

### MEDIUM priority gaps
| Method | Line | Missing Scenario |
|--------|------|-----------------|
| placeOrder | 71 | quantity is 0 → should throw InvalidQuantityException |

### Estimated coverage after fixing HIGH gaps: ~85%
### Estimated coverage after fixing ALL gaps: ~94%