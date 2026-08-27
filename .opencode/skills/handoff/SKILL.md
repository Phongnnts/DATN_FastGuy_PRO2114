---
name: handoff
description: Use when transferring work to a fresh session or agent, switching roles, compacting context, pausing complex work, or preparing a zero-context continuation.
metadata:
  source: mattpocock/dictionary-of-ai-coding
  scope: project
---

# Handoff

Transfer enough context for a stateless receiving session to continue without asking the old session. Assume there is no return path.

## Choose the Carry

| Mechanism | Use when |
| --- | --- |
| Handoff artifact | Work must be inspectable, correctable, reusable, or resumed later |
| Compaction summary | One immediate successor needs a cheap in-context continuation |

Prefer an artifact for multi-step engineering, database work, security-sensitive changes, or decisions that must survive multiple sessions.

## Required Artifact Shape

Write these sections in this order:

1. **Objective** — requested outcome and explicit non-goals.
2. **Current state** — branch, commit, working tree, environment, deployed/runtime state.
3. **Decisions and reasons** — what was settled, alternatives rejected, why.
4. **Source of truth** — contracts, schema, files, runtime identities, authoritative docs.
5. **Completed work** — exact changes, commits, migrations, pushes.
6. **Verification evidence** — commands, counts, pass/fail, warnings, cleanup evidence.
7. **Open work and blockers** — unfinished items, risks, missing approvals.
8. **Next action** — one concrete first step for the receiver.
9. **Relevant files** — exact paths and important symbols/lines.
10. **Safety boundaries** — forbidden targets, retained/disposable DB rules, secrets, destructive actions.

## Quality Gate

Before handing off, verify:

- A zero-context agent can identify the next command/action.
- Decisions include rationale, not only conclusions.
- Status distinguishes local, verified, committed, pushed, migrated, and deployed.
- Runtime facts are fresh; stale assumptions are labeled.
- No secret values appear.
- Unrelated user changes are identified and protected.
- Failed checks and partial results remain visible.

## Failure Signal

A bad handoff causes **relitigation**: the receiver reopens settled decisions because rationale, evidence, or constraints were omitted. If likely, rewrite the artifact before ending the session.

## Minimal Example

```markdown
## Objective
Add shift ownership; no SLA work.

## Decisions and reasons
Use nullable `staff_shift_id` on Orders: smallest queryable current-owner model. Old rows stay NULL to avoid invented audit history.

## Verification evidence
- `mvn test`: 474/474
- Playwright: desktop 1/1, mobile 1/1

## Next action
Review staged diff, then commit only Backend/database/frontend.
```

Adapted from Matt Pocock's Dictionary of AI Coding entry “Handoff”.
