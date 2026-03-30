---
mode: agent
tools: [codebase, file]
description: Review unit test quality against org standards
---

# Review Unit Test Quality

## Step 1 — Load standards
Read: #file:.github/copilot-instructions.md

## Step 2 — Identify target
Test file to review: ${input:Test class name (e.g. OrderServiceTest)}

Find the file using @codebase and read it completely.

## Step 3 — Run reviewer skill
Execute the test-quality-reviewer skill completely.
Do not skip any checklist item.

## Step 4 — Output
Format findings as a table with columns:
  Test Method | Issue Type | Description | Suggested Fix

Then provide the corrected test methods for all CRITICAL issues.

## Step 5 — Apply fixes (if approved)
Ask: "Should I apply these fixes directly to the file?"
If yes: apply only the CRITICAL and HIGH severity fixes.
Leave LOW and SUGGESTION items as comments for the developer.