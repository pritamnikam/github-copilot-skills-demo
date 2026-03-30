---
mode: agent
tools: [codebase, file, terminal]
description: Analyse coverage and generate missing test scenarios
---

# Fix Coverage Gap

## Step 1 — Run coverage
Java:
  mvn test jacoco:report -Dtest="*Test" --no-transfer-progress
  Read: target/site/jacoco/jacoco.xml

Python:
  pytest tests/unit/ --cov=app --cov-report=xml -q
  Read: coverage.xml

## Step 2 — Identify target
Class to analyse: ${input:Class name (e.g. OrderService)}

## Step 3 — Run gap analyser skill
Execute coverage-gap-analyser skill completely.
Show the full gap report before generating any code.

## Step 4 — Confirm with developer
Show gap report and ask:
"I found N gaps. Shall I generate tests for HIGH priority
gaps first, or all gaps at once?"

## Step 5 — Generate missing tests
For each approved gap:
  - Add the test method to the existing test class
  - Do NOT regenerate the whole file — append only
  - Maintain the existing style and @Nested structure

## Step 6 — Verify
Re-run coverage after generation.
Show before vs after comparison.