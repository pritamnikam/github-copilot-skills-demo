---
mode: agent
tools: [codebase, file, terminal]
description: Generate unit tests for the selected class or method
---

# Generate Unit Tests

## Step 1 — Load standards
Read before generating anything:
- #file:.github/copilot-instructions.md

## Step 2 — Load existing patterns
Search the codebase for existing *Test.java files.
Read the first 2 examples to understand the current style.
Match that style exactly in the generated output.

## Step 3 — Identify target
Class or method: ${input:Class or method name (e.g. OrderService or placeOrder)}

Find the source file using @codebase.
Read it completely before generating.

## Step 4 — Run skill
Execute the unit-test-generator skill step by step.
Do not skip the test plan step — output the plan first,
then generate the code.

## Step 5 — Run and verify
Java:  mvn test -Dtest="${input:Class or method name}Test"
Python: pytest tests/unit/test_${input:Class or method name}.py -v

If any tests fail:
  - Read the error
  - Fix the test (not the production code)
  - Re-run
  - Report final result

## Reminder
The developer will review every generated test.
Write tests that are READABLE first — they are documentation.