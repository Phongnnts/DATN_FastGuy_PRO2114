---
name: find-skills
description: Use when the user asks to find, compare, install, update, or discover an agent skill for a specialized task.
license: MIT
metadata:
  source: vercel-labs/skills
  scope: project
---

# Find Skills

Discover existing skills before creating new ones.

## Workflow

1. Identify the domain, exact task, platform, and search synonyms.
2. Search installed project skills first.
3. Search the ecosystem with a specific query:

```bash
npx skills find <query>
```

4. Vet candidates before recommending:
   - read `SKILL.md` fully;
   - inspect shell commands, writes, network calls, credentials, dependencies, and permissions;
   - prefer maintained official/reputable sources;
   - use installs/stars only as signals, never proof of safety.
5. Present at most three ranked options: purpose, source, maintenance/security caveats, project install command.
6. Install only after explicit user approval.

## Project Installation

Default to project scope. Never use `-g` unless the user explicitly requests a global install.

```bash
npx skills add <owner/repo> --skill <skill-name> --copy -y
```

After installation, inspect every created path. Remove unexpected multi-agent copies and retain only the project skill paths intentionally used by this repository.

## Commands

```bash
npx skills find <query> [--owner <owner>]
npx skills add <package> --list
npx skills list --json
npx skills check
npx skills update -p
```

## No Match

State that no vetted match was found. Offer direct help or skill creation; use `skill-scout`/`writing-skills` before creating a new skill.

## Safety

- Never install from search output alone.
- Never skip source review because a directory labels a skill safe.
- Never print or persist credentials.
- Never modify global agent configuration without explicit approval.

Source adapted from `vercel-labs/skills/skills/find-skills/SKILL.md`.
