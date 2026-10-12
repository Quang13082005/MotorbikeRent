# Copy-ready Gemini prompts

## 1. Read-only takeover

```text
You are taking over D:/Motorbike-rental, an existing motorcycle rental project. First understand it; do not edit application code or mutate the database in this step.
Read AGENTS.md, GEMINI.md, PROJECT_CONTEXT.md, docs/gemini-handoff/CURRENT_TASK.md, BUSINESS_RULES.md, VERIFICATION.md and USER_REQUIREMENTS_ORIGINAL.txt in the handoff folder. The approved plan is docs/continuation-20260928/DOI-CHIEU-VA-KE-HOACH.html.
Verify the actual stack, entities/relationships, auth/ownership/CSRF, APIs and caller contracts, payment and availability rules, current tests, recent logs, incomplete phases and integration risks against source. Do not read or echo secret values or customer document contents into the report.
Distinguish VERIFIED IMPLEMENTATION, APPROVED REQUIREMENT, MISSING/WRONG IMPLEMENTATION and UNVERIFIED ASSUMPTION. Update PROJECT_CONTEXT.md only with evidence and preserve uncertainty. Read source by module; don't waste context on binaries, target, backups or uploaded identity documents.
Explain the current checkpoint and exact next action in Vietnamese. Do not mark the whole project complete or claim tests passed unless you ran them. Do not restart from Phase 1 or follow obsolete HANDOFF paragraphs. Stop after the read-only report; I will then give the continuation instruction.
```

## 2. Continue the authorized phase

```text
Continue the next unfinished task identified in docs/gemini-handoff/CURRENT_TASK.md. Follow AGENTS.md and the approved nine-phase plan. Finish the current phase before starting another. Read the actual affected source/DTOs/callers/tests before editing; explain the change briefly.
Implement using the existing architecture. Preserve simple Staff final actions without physical-task checkboxes or new intermediate Rental states. Reuse Payment for money and history; protect schedule overlap, authenticated actor audit and idempotency. Do not bypass validation/security to obtain passing tests.
Run the relevant build and existing/new meaningful tests, negative cases, role/ownership checks and MySQL concurrency tests for money/scheduling changes. Test the actual UI if tooling is available. Review the diff against the available baseline. Fix root causes and rerun affected checks.
Do not perform real bank transactions. Use the clone, backups, reviewed migration and existing local launcher according to VERIFICATION.md. Do not change real customer data or deploy externally without explicit authorization. Existing approval covers the local work described in the checkpoint; do not repeatedly ask approval for the same action.
If a business policy is missing, ask a concrete question and continue independent authorized work. Never infer consent from no reply. At each completed phase report changed files/APIs/migration, actual test results and limitations. Update CURRENT_TASK.md and PROJECT_CONTEXT.md before ending or context exhaustion.
```

## 3. New bounded feature or bug fix

```text
TASK: [Describe one specific feature or bug and expected outcome.]
Read AGENTS.md, PROJECT_CONTEXT.md, current checkpoint and relevant business rules. Investigate source, schema, callers and tests first. Briefly explain affected modules and regression risks.
Implement only this authorized scope with the existing architecture. Preserve API contracts and business validation except for explicitly approved changes. Do not introduce extra Staff workflow steps.
Build, run relevant automated tests, add meaningful behavior/boundary/negative/authorization tests and verify related UI/data consistency. For money or availability, include duplicate/concurrent/stale operations on the isolated MySQL clone. Fix and re-test actual failures; don't weaken validation/tests.
Review the final diff. Report exact files, behavior, tests actually executed/results, remaining risks and manual verification. Save a checkpoint. Do not claim production readiness from compilation alone. Ask only for genuinely missing business decisions or actions beyond current authorization.
```

## 4. Save context before switching agent/model

```text
Before context is exhausted, stop starting new edits and write an exact checkpoint to docs/gemini-handoff/CURRENT_TASK.md. Include current phase, completed and unfinished files, last test/build with log path, running app/JAR/ports, applied/pending DB migrations, baseline counts, backups, temporary fixtures and cleanup, unresolved decisions and next exact command/action. Do not include secrets. Update PROJECT_CONTEXT.md if needed. Report honestly whether the app is running and whether current source equals the running build. Leave the workspace coherent for another agent.
```

Use only one coding agent at a time. Checkpoints are needed before the last token: an agent cannot reliably predict an abrupt context/credit stop. Prefer a checkpoint after each verified milestone.
