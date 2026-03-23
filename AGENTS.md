# AGENTS.md

## Purpose

This repository maintains reusable internal skills for AI coding agents.

Agents working in this repository should optimize for:

- Clear trigger conditions
- Concise `SKILL.md` files
- Progressive disclosure through `references/`
- Stable project installation and upgrade paths

## Repository Rules

- Treat each folder in `skills/` as an independently installable unit
- Do not duplicate detailed guidance across multiple skills unless the workflow truly diverges
- Keep skill metadata accurate and human-readable
- Keep `meta.json` aligned with `SKILL.md` and catalog expectations
- Prefer versioned rollouts over implicit breaking changes

## Skill Authoring Checklist

1. Write a short, precise `name` and `description` in `SKILL.md`.
2. Keep the body focused on workflow, not background theory.
3. Move long examples and standards into `references/`.
4. Add scripts only when deterministic execution matters.
5. Update `meta.json` so the web catalog stays accurate.
6. Update governance and rollout docs if the new skill changes operating expectations.

## Available Skills In This Repository

- `backend-api`
- `bitbucket-code-review`
- `frontend-react`
- `release-triage`

See [docs/GOVERNANCE.md](docs/GOVERNANCE.md) for ownership and change policy.
